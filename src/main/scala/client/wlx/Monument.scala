package client.wlx

import java.net.URLEncoder

import client.MwBot
import client.dto.Template

import scala.concurrent.{ExecutionContext, Future}

case class Monument(textParam: String,
                    id: String,
                    name: String,
                    description: Option[String],
                    article: Option[String],
                    place: String,
                    user: String,
                    area: Option[String],
//                    coordinate: Option[Coordinate],
                    lat: Option[String],
                    lon: Option[String],
                    typ: String,
                    subType: String,
                    photo: Option[String],
                    gallery: Option[String],
                    resolution: Option[String],
                    pageParam: String//,
//                    otherParams: Map[String, String]
                     ) extends Template(textParam, pageParam) {

  def toUrls = Monument.wikiLinkToUrl(name +" * "  + place, "uk.wikipedia.org")

  override def init(text: String, page:String):Monument = Monument.init(text, page)

}

object Monument {

  def init(text: String, page: String = "") = {
    val t = new Template(text)
    val name: String = t.getParam("назва")
    new Monument(textParam = text,
      id = t.getParam("ID"),
      name = name,
      description =  t.getParamOpt("опис"),
      None,
      place =  t.getParam("розташування"),
      user = t.getParam("користувач"),
      area = t.getParamOpt("площа"),
      lat = t.getParamOpt("широта"),
      lon = t.getParamOpt("довгота"),
      typ = t.getParam("тип"),
      subType =  t.getParam("підтип"),
      photo = t.getParamOpt("фото"),
      gallery = t.getParamOpt("галерея"),
      resolution = t.getParamOpt("постанова"),
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
    wiki.page("Template:" + template).revisionsByGenerator("embeddedin", "ei", Set.empty, Set("content", "timestamp", "user", "comment")) map {
      pages =>
        val monuments = pages.flatMap(page => monumentsFromText(page.text.getOrElse(""), page.title, template))
        monuments
    }
  }

}

