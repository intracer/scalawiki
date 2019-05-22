package org.scalawiki.cache

import java.io.File

import net.openhft.chronicle.map.{ChronicleMap, ChronicleMapBuilder}
import org.scalawiki.dto.cmd.Action
import org.scalawiki.{MwBot, MwBotImpl}
import org.scalawiki.dto.{MwException, Page, Site}
import org.scalawiki.http.HttpClient

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class Caller(fn: String => String) extends java.util.function.Function[String, String] {
  override def apply(t: String): String = {
    fn.apply(t)
  }
}

class Cache(name: String, entries: Int = 12 * 1024, valueSize: Int = 128 * 1024, persistent: Boolean = true) {

  private val builder: ChronicleMapBuilder[String, String] = ChronicleMap
    .of(classOf[String], classOf[String])
    .averageKeySize(1024)
    .averageValueSize(valueSize)
    .entries(entries)
    .name(name)

  val cache = if (persistent) {
    builder.createPersistedTo(new File(name))
  } else {
    builder.create()
  }

  def containsKey(key: String): Boolean = cache.containsKey(key)
  def remove(key: String): String = cache.remove(key)
  def computeIfAbsent(key: String, fn: String => String): String = cache.computeIfAbsent(key, new Caller(fn))

}

// TODO async compute, do not enter twice
class CachedBot(site: Site, name: String, persistent: Boolean, http: HttpClient = HttpClient.get(MwBot.system),
                entries: Int = 12 * 1024,
                valueSize: Int = 128 * 1024)
  extends MwBotImpl(site) {

  val cache = new Cache(name + ".cache", entries, valueSize, persistent)

  override def run(action: Action,
                   context: Map[String, String] = Map.empty,
                   limit: Option[Long] = None): Future[Seq[Page]] = {
    val future = super.run(action, context, limit)

    future recoverWith {
      case ex: MwException =>
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

    val fn = (_: String) => Await.result(super.post(params), 30.minute)
    val value = cache.computeIfAbsent(key, fn)

    Future.successful(value)
  }
}

object CachedBot {

  import scala.collection.JavaConverters._
  import com.concurrentthought.cla.{Args, Opt}

  val argsDefs = Args(
    "Cache [options]",
    Seq(
      Opt.string(name = "cache", flags = Seq("-c", "-cache"), help = "cache file", requiredFlag = true)
    )
  )

  def main(args: Array[String]): Unit = {
    val parsed = argsDefs.parse(args)

    if (parsed.handleErrors()) sys.exit(1)
    if (parsed.handleHelp()) sys.exit(0)

    val cacheFile = parsed.values("cache").asInstanceOf[String]
    val file = new File(cacheFile)
    if (!file.exists()) {
      throw new IllegalArgumentException(s"File $cacheFile is absent")
    }
    val cache = new Cache(cacheFile)
    val keys = cache.cache.keySet().asScala.toSeq.sorted
    val valueSizes = cache.cache.values().asScala.map(_.length).toSeq

    println("keys: " + keys.size)
    if (valueSizes.nonEmpty) {
      println(s"values: total size: ${valueSizes.sum / (1024 * 1024)} MB, avg size: ${valueSizes.sum / (valueSizes.size * 1024)} KB")
    }
  }
}
