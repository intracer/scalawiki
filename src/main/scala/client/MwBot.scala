package client

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import client.dto._
import client.json.MwReads._
import play.api.libs.json._
import spray.can.Http
import spray.http.HttpHeaders.`Set-Cookie`
import spray.http.{HttpResponse, _}
import spray.util._

import scala.collection.SortedSet
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class MwBot(val http: HttpClient, val system: ActorSystem, val host: String) {

  implicit val sys = system

  import system.dispatcher

  val baseUrl: String = "https://" + host + "/w/"

  val indexUrl = baseUrl + "index.php"

  val apiUrl = baseUrl + "api.php"

  def encodeTitle(title: String): String = MwUtils.normalize(title)

  def log = system.log

  def login(user: String, password: String) = {
    http.post(apiUrl, "action" -> "login", "lgname" -> user, "lgpassword" -> password, "format" -> "json") map cookiesAndBody map { cb =>
      http.setCookies(cb.cookies)
      val json = Json.parse(cb.body)
      json.validate(loginResponseReads).fold({ err =>
        log.error("Could not login" + err)
        err.toString()
      }, { resp =>
        val params = Map("action" -> "login", "lgname" -> user, "lgpassword" -> password, "lgtoken" -> resp.token.get, "format" -> "json")
        Await.result(http.post(apiUrl, params) map cookiesAndBody map { cb =>
          http.setCookies(cb.cookies)
          val json = Json.parse(cb.body)
          val l = json.validate(loginResponseReads) // {"login":{"result":"NotExists"}}
          l.fold(err => err.toString(),
            success => success.result
          )
        }, http.timeout)
      })
    }
  }

  lazy val token = await(getToken)

  def getToken = get(tokenReads, "action" -> "query", "meta" -> "tokens")

  def getTokens = get(tokensReads, "action" -> "tokens")


  def get[T](reads: Reads[T], params: (String, String)*): Future[T] =
    http.get(getUri(params:_*)) map getBody map {
      body =>
        Json.parse(body).validate(reads).get
    }

  def getByteArray(url: String): Future[Array[Byte]] =
    http.get(url) map {
      response => response.entity.data.toByteArray
    }


  def post[T](reads: Reads[T], params: (String, String)*): Future[T] =
    post(reads, params.toMap)

  def post[T](reads: Reads[T], params: Map[String, String]): Future[T] =
    http.post(apiUrl, params) map getBody map {
      body =>
        val result = Json.parse(body).validate(reads).get
        println(result)
        result
    }

  def postMultiPart[T](reads: Reads[T], params: Map[String, String]): Future[T] =
    http.postMultiPart(apiUrl, params) map getBody map {
      body =>
        val json = Json.parse(body)
        val response = json.validate(reads)
//        response.fold[T](err => {
//          json.validate(errorReads)
//        },
//          success => success
//        )
        val result = response.get
        println(result)
        result
    }

  def postFile[T](reads: Reads[T], params: Map[String, String], fileParam: String, filename: String): Future[T] =
    http.postFile(apiUrl, params, fileParam , filename) map getBody map {
      body =>
        val json = Json.parse(body)
        val response = json.validate(reads)
        //        response.fold[T](err => {
        //          json.validate(errorReads)
        //        },
        //          success => success
        //        )
        val result = response.get
        println(result)
        result
  }


  def pagesByTitle(titles: Set[String]) = PageQuery.byTitles(titles, this)

  def pagesById(ids: Set[Long]) = PageQuery.byIds(ids, this)

  def page(title: String) = PageQuery.byTitle(title, this)

  def page(id: Long) = PageQuery.byId(id, this)


  def cookiesAndBody(response: HttpResponse): CookiesAndBody =
    CookiesAndBody(getCookies(response), getBody(response))

  def getBody(response: HttpResponse): String =
    response.entity.asString(HttpCharsets.`UTF-8`)

  def getCookies(response: HttpResponse): List[HttpCookie] =
    response.headers.collect {
      case `Set-Cookie`(hc) => hc
    }

  case class CookiesAndBody(cookies: List[HttpCookie], body: String)

  def pageText(title: String): Future[String] = {
    val url = getIndexUri("title" -> encodeTitle(title), "action" -> "raw")
    http.get(url) map getBody
  }

  def getIndexUri(params: (String, String)*) =
    Uri(indexUrl) withQuery (params ++ Seq("format" -> "json"): _*)

  def getUri(params: (String, String)*) =
    Uri(apiUrl) withQuery (params ++ Seq("format" -> "json"): _*)

  def getUri(params: Map[String, String]) =
    Uri(apiUrl) withQuery (params ++ Map("format" -> "json"))

  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
  }

  def await[T](future: Future[T]) = Await.result(future, http.timeout)

}

object MwBot {

  import spray.caching.{Cache, LruCache}
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.{Future, _}

  val commons = "commons.wikimedia.org"
  val ukWiki = "uk.wikipedia.org"

  def create(host: String): MwBot = {
    val system = ActorSystem()
    val http = new HttpClientImpl(system)

    val bot = new MwBot(http, system, host)
    bot.await(bot.login(LoginInfo.login, LoginInfo.password))
    bot
  }

  val cache: Cache[MwBot] = LruCache()

  def get(host: String): MwBot = {
    Await.result(cache(host) {
      future { create(host) }
    }, 1.minute)
  }

  def main(args: Array[String]) {


//        val ukWiki = new MwBot(http, system, "uk.wikipedia.org")

  //   val result =   Await.result(ukWiki.login(args(0), args(1)), http.timeout)
    //    listsNew(system, http, ukWiki)
    //Monument.lists(ukWiki, "ВЛЗ-рядок")

    val commons = create("commons.wikimedia.org")

//    commons.await(commons.login(args(0), args(1)))
    //    images(commons)
//    imagesText(commons)
    //imagesInfo(commons)

   // contestImages(commons)

//    imagesIds(commons)
    imagesAuthors(commons)

  }

  def contestImages(commons: MwBot)(implicit dispatcher: ExecutionContext) {
    commons.page("Category:Images from Wiki Loves Earth 2014").categoryMembers(Set(Namespace.CATEGORY)) flatMap {
      categories =>
        val filtered = categories.filter(c => c.title.startsWith("Category:Images from Wiki Loves Earth 2014 in"))
        Future.traverse(filtered)(
          category => commons.page(category.pageid).categoryMembers(Set(Namespace.FILE))
        )
    } map {
      filesInCategories =>

        for (files1 <- filesInCategories;
             files2 <- filesInCategories) {
          if (files1 != files2) {
            val intersect = files1.intersect(files2)
            if (!intersect.isEmpty) {
              println(intersect)
            }
          }
        }

        commons.shutdown()

    }
  }

  def images(commons: MwBot)(implicit dispatcher: ExecutionContext) {
    commons.page("Category:Images from Wiki Loves Earth 2014 in Ukraine").categoryMembers() map {
      pages =>
        println("pages:" + pages.size)

        commons.shutdown()
    }
  }

  def imagesIds(commons: MwBot)(implicit dispatcher: ExecutionContext) {
    commons.page("Category:Images from Wiki Loves Earth 2014 in Ukraine").revisionsByGenerator("categorymembers", "cm",
      Set.empty, Set("content", "timestamp", "user", "comment")) map {
      pages =>

        val idRegex = """(\d\d)-(\d\d\d)-(\d\d\d\d)"""
       val ids:Seq[String] = pages.flatMap(_.text.map(Template.getDefaultParam(_, "UkrainianNaturalHeritageSite")))
        .filter(_.matches(idRegex))

        val byId = ids.groupBy(identity)
        println("pages:" + pages.size)
        println("ids:" + byId.size)

        commons.shutdown()
    }
  }

  def imagesAuthors(commons: MwBot)(implicit dispatcher: ExecutionContext) {
    val wle2014 = getAuthors(commons, "Category:Images from Wiki Loves Earth 2014 in Ukraine")
    val wle2013 = getAuthors(commons, "Category:Images from Wiki Loves Earth 2013 in Ukraine")

    val wlm2013 = getAuthors(commons, "Category:Images from Wiki Loves Monuments 2013 in Ukraine")
    val wlm2012 = getAuthors(commons, "Category:Images from Wiki Loves Monuments 2012 in Ukraine")

    val all = wlm2012 ++ wlm2013 ++ wle2013 ++ wle2014

    println(all.size)

    val mapped = all.map(u => s"{{#target:User talk:$u}}").mkString("\n")

    println(mapped)

  }

  def getAuthors(commons: MwBot, category: String)(implicit dispatcher: ExecutionContext): SortedSet[String] = {
    val result = Await.result(commons.page(category).revisionsByGenerator("categorymembers", "cm",
      Set.empty, Set("content", "timestamp", "user", "comment")) map {
      pages => getAuthors(pages)
    }, 15.minutes)

    Thread.sleep(1000)
    result
  }

  def getAuthors(pages: Seq[Page])(implicit dispatcher: ExecutionContext): SortedSet[String] = {
    val authors: Seq[String] = pages.flatMap(_.text.map(t => new Template(t).getParam("author")))

    val byAuthor = authors.groupBy(identity)
    println("pages:" + pages.size)
    println("authors:" + byAuthor.size)
    val authorsSet = SortedSet(byAuthor.keys.map(t => t.split("\\|")(0).replace("[[User:", "")).toSeq: _*)
    println("authors:" + authorsSet.size)
//    println(authorsSet.mkString("\n"))

    authorsSet
  }

  def imagesInfo(commons: MwBot)(implicit dispatcher: ExecutionContext) {
    commons.page("Category:Images from Wiki Loves Earth 2014 in Ukraine").imageInfoByGenerator("categorymembers", "cm") map {
      pages =>
        println("pages:" + pages.size)

        commons.shutdown()
    }
  }

  def listsOld(ukWiki: MwBot)(implicit dispatcher: ExecutionContext) {
    ukWiki.page("Template:ВЛЗ-рядок").whatTranscludesHere() flatMap {
      pages =>
        println("pages:" + pages.size)
        Future.traverse(pages)(page => ukWiki.pageText(page.title).map(page.withText))
    } map {
      pages =>
        pages.foreach { page =>
          val text: String = page.text.getOrElse("")
          val monuments = "\\{\\{ВЛЗ-рядок".r.findAllMatchIn(text).size
          println(s"${page.title} - $monuments monuments")
        }

        ukWiki.shutdown()
    }
  }

  def listsNew(ukWiki: MwBot)(implicit dispatcher: ExecutionContext) {
    ukWiki.page("Template:ВЛП-рядок").revisionsByGenerator("embeddedin", "ei", Set.empty, Set("content", "timestamp", "user", "comment")) map {
      pages =>
        println("pages2:" + pages.size)
        for (page <- pages) {
          val text: String = page.text.getOrElse("")
          val monuments = "\\{\\{ВЛП-рядок".r.findAllMatchIn(text).size
          println(s"${page.title} - $monuments monuments")
        }

        //          val commons = new MwBot(http, system, "commons.wikimedia.org")
        ukWiki.shutdown()
    }
    // commons.shutdown()
  }


}


