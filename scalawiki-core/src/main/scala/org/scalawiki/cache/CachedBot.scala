package org.scalawiki.cache

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, StandardCopyOption}
import java.security.MessageDigest

import org.rogach.scallop.ScallopConf
import org.scalawiki.dto.cmd.Action
import org.scalawiki.{MwBot, MwBotImpl}
import org.scalawiki.dto.{MwException, Page, Site}
import org.scalawiki.http.HttpClient

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object Cache {

  /** Root directory for all persistent caches, relative to the working dir. */
  val defaultRoot: File = new File("http-cache")
}

/** A plain filesystem key -> file cache.
  *
  * Each entry is one file under `<root>/<name>/` (root defaults to `http-cache/`),
  * named by the SHA-256 hex of the key, holding the response body verbatim
  * (UTF-8). This replaced a ChronicleMap-backed store: the file cache is smaller,
  * faster to read, plain text, and needs no `--add-opens` / `--add-exports` JVM
  * flags to run on JDK 17+.
  */
class Cache(name: String, persistent: Boolean = true, root: File = Cache.defaultRoot) {

  private val dir: File = new File(root, name)

  private val memory: TrieMap[String, String] =
    if (persistent) null else TrieMap.empty[String, String]

  if (persistent) dir.mkdirs()

  private def fileFor(key: String): File = {
    val digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8))
    new File(dir, digest.map("%02x".format(_)).mkString)
  }

  def containsKey(key: String): Boolean =
    if (persistent) fileFor(key).isFile else memory.contains(key)

  def remove(key: String): Unit =
    if (persistent) Files.deleteIfExists(fileFor(key).toPath)
    else memory.remove(key)

  def computeIfAbsent(key: String, fn: String => String): String = {
    if (persistent) {
      val target = fileFor(key)
      if (target.isFile) {
        new String(Files.readAllBytes(target.toPath), StandardCharsets.UTF_8)
      } else {
        val value = fn(key)
        val tmp = File.createTempFile(target.getName, ".tmp", dir)
        Files.write(tmp.toPath, value.getBytes(StandardCharsets.UTF_8))
        try {
          Files.move(
            tmp.toPath,
            target.toPath,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
          )
        } catch {
          case _: java.nio.file.AtomicMoveNotSupportedException =>
            Files.move(tmp.toPath, target.toPath, StandardCopyOption.REPLACE_EXISTING)
        }
        value
      }
    } else {
      memory.getOrElseUpdate(key, fn(key))
    }
  }

}

// TODO async compute, do not enter twice
class CachedBot(
    site: Site,
    name: String,
    persistent: Boolean,
    http: HttpClient = HttpClient.get(MwBot.system)
) extends MwBotImpl(site) {

  val cache = new Cache(name, persistent)

  override def run(
      action: Action,
      context: Map[String, String] = Map.empty,
      limit: Option[Long] = None
  ): Future[Iterable[Page]] = {
    val future = super.run(action, context, limit)

    future recoverWith { case ex: MwException =>
      val key = paramsKey(ex.params)
      cache.remove(key)
      super.run(action, context, limit)
    }
  }

  def paramsKey(params: Map[String, String]) =
    params.toIndexedSeq.sortBy(_._1).toString()

  override def post(params: Map[String, String]): Future[String] = {
    val key = paramsKey(params)

    if (cache.containsKey(key)) {
      log.info(s"cached $host POST equivalent to: ${getUri(params)}")
    }

    try {
      val fn = (_: String) => Await.result(super.post(params), 30.minutes)
      val value = cache.computeIfAbsent(key, fn)

      // computeIfAbsent may return a previously cached error/rate-limit body (not
      // valid JSON) just as easily as a freshly fetched one. Don't let a non-JSON
      // response linger in the persistent cache: evict it so the next attempt
      // (this retry included) re-fetches from the network instead of replaying
      // the same failure forever.
      if (!CachedBot.looksLikeJson(value)) {
        cache.remove(key)
      }

      Future.successful(value)
    } catch {
      case t: Throwable =>
        Future.failed(t)
    }
  }
}

object CachedBot {

  def looksLikeJson(body: String): Boolean = {
    val trimmed = body.trim
    trimmed.startsWith("{") || trimmed.startsWith("[")
  }

  class CachedArgs(arguments: Seq[String]) extends ScallopConf(arguments) {
    val cache = opt[String](descr = "cache directory")
    verify()
  }

  def main(args: Array[String]): Unit = {
    val parsed = new CachedArgs(args)

    val cacheDir = parsed.cache()
    val dir = new File(cacheDir)
    if (!dir.isDirectory) {
      throw new IllegalArgumentException(s"Cache directory $cacheDir is absent")
    }
    val entries = Option(dir.listFiles()).getOrElse(Array.empty[File]).filter(_.isFile)
    val valueSizes = entries.map(_.length()).toSeq

    println("entries: " + entries.length)
    if (valueSizes.nonEmpty) {
      println(
        s"values: total size: ${valueSizes.sum / (1024 * 1024)} MB, avg size: ${valueSizes.sum / (valueSizes.size * 1024)} KB"
      )
    }
  }
}
