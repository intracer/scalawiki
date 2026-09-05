package org.scalawiki.cache

import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll

class CacheFileStoreSpec extends Specification with AfterAll {

  private val root: File = Files.createTempDirectory("cache-spec").toFile

  def afterAll(): Unit = {
    def del(f: File): Unit = {
      Option(f.listFiles()).foreach(_.foreach(del))
      f.delete()
    }
    del(root)
  }

  "the filesystem Cache" should {

    "round-trip: compute, hit, not-recompute, remove" in {
      val c = new Cache("rt", root = root)
      val v1 = c.computeIfAbsent("k1", _ => """{"ok":1}""")
      v1 === """{"ok":1}"""
      c.containsKey("k1") === true
      c.computeIfAbsent("k1", _ => "SHOULD-NOT-RECOMPUTE") === v1
      c.remove("k1")
      c.containsKey("k1") === false
    }

    "keep distinct keys apart and survive a fresh Cache over the same dir" in {
      val a = new Cache("persist", root = root)
      a.computeIfAbsent("x", _ => "AAA")
      a.computeIfAbsent("y", _ => "BBB")

      val b = new Cache("persist", root = root)
      b.containsKey("x") === true
      b.computeIfAbsent("x", _ => "nope") === "AAA"
      b.computeIfAbsent("y", _ => "nope") === "BBB"
    }

    "non-persistent mode: in-memory only, no dir created" in {
      val c = new Cache("mem", persistent = false, root = root)
      c.computeIfAbsent("k", _ => "v") === "v"
      c.containsKey("k") === true
      new File(root, "mem").exists() === false
    }

    "run the value function once per key under concurrent access" in {
      val c = new Cache("single-flight", root = root)
      val calls = new AtomicInteger(0)
      val fn = { (_: String) =>
        calls.incrementAndGet()
        Thread.sleep(50)
        """{"v":1}"""
      }
      val results = Await.result(
        Future.sequence(Seq.fill(16)(Future(c.computeIfAbsent("k", fn)))),
        10.seconds
      )
      results.distinct === Seq("""{"v":1}""")
      calls.get() === 1
    }

    "recompute when the entry is evicted between the check and the read" in {
      val c = new Cache("evict-race", root = root)
      c.computeIfAbsent("k", _ => "first")
      // simulate a concurrent eviction landing just before this read
      c.remove("k")
      c.computeIfAbsent("k", _ => "second") === "second"
    }

    "leave no .tmp files behind after a write" in {
      val c = new Cache("no-tmp", root = root)
      c.computeIfAbsent("k", _ => "value")
      val leftovers = Option(new File(root, "no-tmp").listFiles())
        .getOrElse(Array.empty[File])
        .filter(_.getName.endsWith(".tmp"))
      leftovers.toSeq === Seq.empty
    }
  }
}
