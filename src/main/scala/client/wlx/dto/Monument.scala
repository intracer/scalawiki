package client.wlx.dto

import java.net.URLEncoder

import client.dto.Template

case class Monument(textParam: String, pageParam: String,
                    id: String,
                    name: String,
                    description: Option[String] = None,
                    article: Option[String] = None,
                    place: Option[String] = None,
                    user: Option[String] = None,
                    area: Option[String] = None,
//                    coordinate: Option[Coordinate],
                    lat: Option[String] = None,
                    lon: Option[String] = None,
                    typ: Option[String] = None,
                    subType: Option[String] = None,
                    photo: Option[String] = None,
                    gallery: Option[String] = None,
                    resolution: Option[String] = None
//                    otherParams: Map[String, String]
                     ) extends Template(textParam, pageParam) {

  def toUrls = Monument.wikiLinkToUrl(name +" * "  + place, "uk.wikipedia.org")

  override def init(text: String, page:String):Monument = Monument.init(text, page)

  def regionId = Monument.getRegionId(id)

}

object Monument {

  def init(text: String, page: String = "") = {
    val t = new Template(text)
    val name: String = t.getParam("назва")
    new Monument(textParam = text,
      id = t.getParam("ID"),
      name = name,
      description =  t.getParamOpt("опис"),
      article = None,
      place =  t.getParamOpt("розташування"),
      user = t.getParamOpt("користувач"),
      area = t.getParamOpt("площа"),
      lat = t.getParamOpt("широта"),
      lon = t.getParamOpt("довгота"),
      typ = t.getParamOpt("тип"),
      subType =  t.getParamOpt("підтип"),
      photo = t.getParamOpt("фото"),
      gallery = t.getParamOpt("галерея"),
      resolution = t.getParamOpt("постанова"),
      pageParam = page
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

  // test for "-" id
  def getRegionId(monumentId: String): String =  monumentId.split("\\-").headOption.getOrElse("")

  def getRegionId(monumentId: Option[String]): String =  monumentId.fold("")(getRegionId)

}

