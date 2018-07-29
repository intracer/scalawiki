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

// TODO async compute, do not enter twice
class CachedBot(site: Site, name: String, persistent: Boolean, http: HttpClient = HttpClient.get(MwBot.system))
  extends MwBotImpl(site) {

  private val builder: ChronicleMapBuilder[String, String] = ChronicleMap
    .of(classOf[String], classOf[String])
    .averageKeySize(256)
    .averageValueSize(1024 * 1024)
    .entries(1024)
    .name(name + ".cache")

  private val cache = if (persistent) {
    builder.createPersistedTo(new File(name + ".cache"))
  } else {
    builder.create()
  }

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

    val fn = (_: String) => Await.result(super.post(params), 30.minute)
    val value = cache.computeIfAbsent(key, new Caller(fn))

    Future.successful(value)
  }
}

class Caller(fn: String => String) extends java.util.function.Function[String, String] {
  override def apply(t: String): String = {
    fn.apply(t)
  }
}
