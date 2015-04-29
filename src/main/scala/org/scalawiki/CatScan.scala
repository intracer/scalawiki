package org.scalawiki

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import org.scalawiki.dto.Namespace

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, _}

object CatScan {

  import spray.caching.{Cache, LruCache}

  val cache: Cache[Set[String]] = LruCache(maxCapacity = 650000, initialCapacity = 650000)

  val total = Collections.newSetFromMap[String](new ConcurrentHashMap())
  val categories = new AtomicInteger()

  val wikipedia = "wikipedia.org"
  val enWikiHost = "en." + wikipedia

  def main(args: Array[String]) {
//    val title = "Category:Crimea"
//    val host = "en." + wikipedia




//    val title = "Catégorie:Crimée"
//    val host = "fr." + wikipedia

//    val title = "Categoria:Crimea"
//    val host = "it." + wikipedia

//    val title = "Κατηγορία:Κριμαία"
//    val host = "el." + wikipedia


        val title = "Categoría:Crimea"
        val host = "es." + wikipedia



//    val title = "Катэгорыя:Крым"
//    val host = "be-x-old." + wikipedia

//        val title = "Катэгорыя:Крым"
//        val host = "be." + wikipedia

//    val title = "Kategorie:Krim"
//    val host = "de." + wikipedia

//        val title = "Kategoria:Krym"
//        val host = "pl." + wikipedia

    //        val title = "Kategoriya:Qırım"
//        val host = "crh." + wikipedia


//    val title = "Категорія:Крим"
//    val host = "uk." + wikipedia

//    val title = "Категория:Крым"
//    val host = "ru." + wikipedia

    val enWiki = MwBot.get(host)

    val countFuture = getCountCached(enWiki, title)

    countFuture.map{
      count =>
        enWiki.system.log.info("Final count:" + count)
    }
  }

  def getCountCached(bot: MwBot, title: String): Future[Set[String]] = {
    cache.get(title).getOrElse(getCount(bot, title))
  }

  def getCount(bot: MwBot, title: String): Future[Set[String]] = {
    bot.system.log.info(s"Enter: $title, cache size: ${cache.size}")
    cache(title){future { Set.empty[String] }}
    val subCats = bot.page(title).categoryMembers(namespaces = Set(Namespace.CATEGORY)).flatMap { pages =>
      bot.system.log.info(s"Category $title has ${pages.size} subcats")

      val futures = pages.map(page => getCountCached(bot, page.title))
      val setFuture = Future.reduce(futures)((s1, s2) => s1 ++ s2)
      setFuture
    }

    val thisCat = bot.page(title).categoryMembers(namespaces = Set(Namespace.MAIN)).map { articles =>

      import scala.collection.JavaConverters._

      val size = articles.size
      val titles: Set[String] = articles.map(_.title).toSet
      total.addAll(titles.asJava)
      val processed = categories.addAndGet(1)

      bot.system.log.info(s"Total is ${total.size}, processed = $processed (${processed*100.0/cache.size} %) Category $title has $size articles")

      if (processed == cache.size) {
        println(cache.keys.mkString("All cats:\n","\n", "\n"))
      }
      titles
    }
    val sum = Seq(subCats, thisCat)
    Future.reduce(sum)((s1, s2) => s1 ++ s2)
  }
}
