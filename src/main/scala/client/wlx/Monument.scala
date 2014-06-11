package client.wlx

import client.dto.{PageQuery, Template}
import client.MwBot
import scala.concurrent.{Future, ExecutionContext}
import java.util.regex.{Pattern, Matcher}
import java.net.URLEncoder

case class Monument(textParam: String,
                    id: String,
                    name: String,
                    description: Option[String],
                    article: Option[String],
                    place: String,
                    typ: String,
                    subType: String,
                    photo: Option[String],
                    gallery: Option[String],
                    page: String
                     ) extends Template(textParam) {

  def toUrls = Monument.wikiLinkToUrl(name +" * "  + place, "uk.wikipedia.org")

}

object Monument {

  def init(text: String, page: String) = {
    val t = new Template(text)
    val name: String = t.getParam("назва")
    new Monument(text,
      t.getParam("ID"),
      name,
      t.getParamOpt("опис"),
      None,
      t.getParam("розташування"),
      t.getParam("тип"),
      t.getParam("підтип"),
      t.getParamOpt("фото"),
      t.getParamOpt("галерея"),
      page
    )
  }

  def wikiLinkToUrl(wikiText: String, host: String) = {
    val r1 = "\\[\\[([^|]*?)\\]\\]".r.replaceAllIn(wikiText, {
      m =>
        val url = URLEncoder.encode(m.group(1).replaceAll(" ", "_"), "UTF-8")
        val title = m.group(1)
        s"""<a href='https://$host/wiki/$url'>$title</a>"""
    })

    val r2 = "\\[\\[(.*?)\\|(.*?)\\]\\]".r.replaceAllIn(r1, {
      m =>
        val url = URLEncoder.encode(m.group(1).replaceAll(" ", "_"), "UTF-8")
        val title = m.group(2)
        s"""<a href='https://$host/wiki/$url'>$title</a>"""
    })


    r2
  }


  def monumentsFromText(text: String, page: String, template: String): Set[Monument] =
    text.split("\\{\\{" + template).map(text => init(text, page)).filter(_.id.nonEmpty).toSet

  def lists(wiki: MwBot, template: String)(implicit dispatcher: ExecutionContext): Future[Seq[Monument]] = {
    wiki.revisionsByGenerator("embeddedin", "ei", PageQuery.byTitle("Template:" + template), Set.empty, Set("content", "timestamp", "user", "comment")) map {
      pages =>
        val monuments = pages.flatMap(page => monumentsFromText(page.text.getOrElse(""), page.title, template))
        val urls = monuments.map(_.toUrls)
        monuments
    }
  }

}

