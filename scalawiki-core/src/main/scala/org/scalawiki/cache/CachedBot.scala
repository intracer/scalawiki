package org.scalawiki.cache

import java.io.File

import net.openhft.chronicle.map.{ChronicleMap, ChronicleMapBuilder}
import org.scalawiki.{MwBot, MwBotImpl}
import org.scalawiki.dto.Site
import org.scalawiki.http.HttpClient

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

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

  override def post(params: Map[String, String]): Future[String] = {
    val key = params.toIndexedSeq.sortBy(_._1).toString()

    val fn = (_: String) => Await.result(super.post(params), 1.minute)
    val value = cache.computeIfAbsent(key, new Caller(fn))

    Future.successful(value)
  }
}

class Caller(fn: String => String) extends java.util.function.Function[String, String] {
  override def apply(t: String): String = {
    fn.apply(t)
  }
}
