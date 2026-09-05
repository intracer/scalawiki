package org.scalawiki.util

import org.apache.pekko.event.LoggingAdapter

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.jdk.CollectionConverters._
import scala.util.Failure
import scala.util.control.NonFatal

/** Process-wide gate and registry for "outbound wiki write" operations (page
  * edits, file uploads).
  *
  * The WLX statistics engine fans out dozens of `bot.page(...).edit(...)` calls
  * whose `Future` results are simply discarded by their callers ("fire and
  * forget"). Three things then go wrong silently:
  *   - all the edits are launched at once; with only two connections to the host
  *     and Pekko's default `max-open-requests = 32`, the surplus requests fail
  *     immediately with `BufferOverflowException` and those reports never
  *     publish;
  *   - a failed edit is never observed, so nothing says a report didn't publish;
  *   - `main` has no idea when the writes are actually finished, so the process
  *     either exits too early (killing the in-flight edits) or never exits.
  *
  * Once [[enable]] has been called, [[submit]] runs each write through a small
  * concurrency gate, logs any failure together with its throwable, and
  * [[awaitQuiescence]] blocks until the queue has drained. Disabled by default,
  * so library and test users keep the old unthrottled fire-and-forget behaviour.
  */
object WriteWatcher {

  /** Max wiki writes in flight at once while enabled. Kept well under Pekko's
    * `max-open-requests` (32) so read requests still get through, and above the
    * 2-connection pool so it stays saturated. Override with
    * `-Dscalawiki.write.maxConcurrent=N`. */
  private val maxConcurrent: Int =
    sys.props.get("scalawiki.write.maxConcurrent").flatMap(s => scala.util.Try(s.toInt).toOption).filter(_ > 0).getOrElse(4)

  @volatile private var enabled = false
  @volatile private var logger: Option[LoggingAdapter] = None

  private val queue = new ConcurrentLinkedQueue[() => Unit]()
  private val running = new AtomicInteger(0)
  private val seq = new AtomicLong(0L)
  private val started = new AtomicLong(0L)
  private val completed = new AtomicLong(0L)
  private val failures = new ConcurrentHashMap[Long, (String, Throwable)]()

  /** Start gating and tracking wiki writes. `log`, if given, receives failure
    * and summary messages; otherwise they go to stderr. */
  def enable(log: LoggingAdapter = null): Unit = {
    enabled = true
    logger = Option(log)
  }

  def isEnabled: Boolean = enabled

  /** Disable tracking and forget all recorded state (drops anything queued).
    * Only useful for tests, to undo an [[enable]]. */
  def reset(): Unit = {
    enabled = false
    logger = None
    queue.clear()
    running.set(0)
    failures.clear()
    seq.set(0L)
    started.set(0L)
    completed.set(0L)
  }

  /** Run `op` as a tracked wiki write described by `desc`.
    *
    * When enabled the call is queued and only started once fewer than
    * [[maxConcurrent]] writes are in flight; the returned future still completes
    * with `op`'s result (or failure). When disabled `op` runs immediately, so
    * behaviour is unchanged for library/test callers.
    */
  def submit[T](
      desc: => String
  )(op: () => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    if (!enabled) {
      return try op()
      catch { case NonFatal(e) => Future.failed(e) }
    }

    val id = seq.incrementAndGet()
    val d =
      try desc
      catch { case _: Throwable => "wiki write" }
    val promise = Promise[T]()
    queue.add(() => runTask(id, d, op, promise))
    started.incrementAndGet()
    pump()
    promise.future
  }

  /** Back-compat helper for an already-started future. Prefer [[submit]], which
    * also throttles. */
  def register[T](
      desc: => String
  )(future: Future[T])(implicit ec: ExecutionContext): Future[T] =
    submit(desc)(() => future)

  private def pump()(implicit ec: ExecutionContext): Unit = {
    var continue = true
    while (continue) {
      val cur = running.get()
      if (cur >= maxConcurrent) {
        continue = false
      } else if (running.compareAndSet(cur, cur + 1)) {
        // slot acquired; hand it to the next queued task, or give it back
        val runnable = queue.poll()
        if (runnable == null) {
          running.decrementAndGet()
          continue = false
        } else {
          runnable()
        }
      }
      // CAS lost: another thread changed `running`, retry the loop
    }
  }

  private def runTask[T](
      id: Long,
      desc: String,
      op: () => Future[T],
      promise: Promise[T]
  )(implicit ec: ExecutionContext): Unit = {
    // the caller (pump) has already reserved the running slot
    val fut =
      try op()
      catch { case NonFatal(e) => Future.failed[T](e) }
    fut.onComplete { result =>
      running.decrementAndGet()
      completed.incrementAndGet()
      result match {
        case Failure(e) =>
          failures.put(id, (desc, e))
          logger match {
            case Some(l) => l.error(e, s"wiki write failed: $desc")
            case None =>
              Console.err.println(s"[WriteWatcher] wiki write failed: $desc")
              e.printStackTrace()
          }
        case _ =>
      }
      promise.complete(result)
      pump()
    }
  }

  /** (description, throwable) for every write that has failed so far. A write
    * that the caller later retries and lands (e.g.
    * [[org.scalawiki.edit.PageUpdater]] re-reading after an edit conflict) still
    * shows up here for its failed attempt. */
  def recordedFailures: Seq[(String, Throwable)] =
    failures.values().asScala.toVector

  def inFlightCount: Int = running.get() + queue.size()

  def completedCount: Long = completed.get()

  /** Block until the write queue has been empty and idle for `settle`, or until
    * `hardLimit` elapses, whichever comes first. Writes spawned from the
    * completion callback of an earlier write are still picked up as long as they
    * start within the `settle` window.
    *
    * @return the failures observed while waiting.
    */
  def awaitQuiescence(
      settle: FiniteDuration = 3.seconds,
      hardLimit: Duration = 3.hours,
      pollInterval: FiniteDuration = 200.millis
  ): Seq[(String, Throwable)] = {
    if (!enabled) return Nil

    val deadlineNanos = hardLimit match {
      case f: FiniteDuration => System.nanoTime() + f.toNanos
      case _                 => Long.MaxValue
    }

    var lastCompleted = completed.get()
    var quietSinceNanos = if (inFlightCount == 0) System.nanoTime() else Long.MinValue
    var done = false
    var timedOut = false

    while (!done) {
      Thread.sleep(pollInterval.toMillis)
      val now = System.nanoTime()
      val idle = inFlightCount == 0
      val doneNow = completed.get()

      if (idle && doneNow == lastCompleted) {
        if (quietSinceNanos == Long.MinValue) quietSinceNanos = now
        if (now - quietSinceNanos >= settle.toNanos) done = true
      } else {
        quietSinceNanos = Long.MinValue
        lastCompleted = doneNow
      }

      if (!done && now >= deadlineNanos) {
        done = true
        timedOut = true
      }
    }

    if (timedOut) {
      emit(
        warn = true,
        s"gave up waiting after $hardLimit with $inFlightCount write(s) still queued/in flight"
      )
    }

    val f = recordedFailures
    emit(
      warn = f.nonEmpty,
      s"wiki writes finished: ${completed.get()} completed, ${f.size} failed"
    )
    f
  }

  private def emit(warn: Boolean, msg: String): Unit =
    logger match {
      case Some(l) => if (warn) l.warning(msg) else l.info(msg)
      case None    => Console.err.println(s"[WriteWatcher] $msg")
    }
}
