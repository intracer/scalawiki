package client

import java.util.concurrent.atomic.AtomicInteger

import client.dto.Namespace

import scala.concurrent.{Future, _}
import scala.concurrent.ExecutionContext.Implicits.global

object CatScan {

  import spray.caching.{Cache, LruCache}

  val cache: Cache[Int] = LruCache(maxCapacity = 650000, initialCapacity = 650000)

  val total = new AtomicInteger()
  val categories = new AtomicInteger()

  val wikipedia = "wikipedia.org"
  val enWikiHost = "en." + wikipedia

  def main(args: Array[String]) {
//    val title = "Category:Military"
//    val host = "en." + wikipedia

//    val title = "Категорія:Збройні_сили"
//    val host = "uk." + wikipedia

    val title = "Категория:Вооружённые_силы"
    val host = "ru." + wikipedia

    val enWiki = MwBot.get(host)

    val countFuture = getCountCached(enWiki, title)

    countFuture.map{
      count =>
        enWiki.system.log.info("Final count:" + count)
    }
  }

  def getCountCached(bot: MwBot, title: String): Future[Int] = {
    cache.get(title).getOrElse(getCount(bot, title))
  }

  def getCount(bot: MwBot, title: String): Future[Int] = {
    bot.system.log.info(s"Enter: $title, cache size: ${cache.size}")
    cache(title){future { 0 }}
    val subCats = bot.page(title).categoryMembers(namespaces = Set(Namespace.CATEGORY)).flatMap { pages =>
      bot.system.log.info(s"Category $title has ${pages.size} subcats")

      val futures = pages.map(page => getCountCached(bot, page.title))
      val intFuture = Future.reduce(futures)(_ + _)
      intFuture
    }

    val thisCat = bot.page(title).categoryMembers(namespaces = Set(Namespace.MAIN)).map { articles =>
      val size = articles.size
      val now = total.addAndGet(size)
      val processed = categories.addAndGet(1)

      bot.system.log.info(s"Total is $now, processed = $processed (${processed*100.0/cache.size} %) Category $title has $size articles")

      if (processed == cache.size) {
        println(cache.keys.mkString("All cats:\n","\n", "\n"))
      }
      size
    }
    val sum = Seq(subCats, thisCat)
    Future.reduce(sum)(_+_)
  }
}
