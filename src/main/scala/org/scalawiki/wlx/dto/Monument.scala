package org.scalawiki.wlx.dto

import java.net.URLEncoder

import org.scalawiki.dto.Template

case class Monument(textParam: String, page: String,
                    id: String,
                    name: String,
                    year: Option[String] = None,
                    description: Option[String] = None,
                    article: Option[String] = None,
                    city: Option[String] = None,
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
                    resolution: Option[String] = None,
                    stateId: Option[String] = None,
                    contest: Option[Long] = None,
//                    otherParams: Map[String, String],
                      names: Map[String, String]
                     ) extends Template(textParam, page, names) {

  def toUrls = Monument.wikiLinkToUrl(name +" * "  + place, "uk.wikipedia.org")

  def galleryLink = gallery.fold("") { title => s" [[:Category:$title|$title]]"  }

  override def init(text: String, page:String, names: Map[String, String]):Monument = Monument.init(text, page, names)

  def regionId = Monument.getRegionId(id)

  val types = initTypes

  def initTypes: Set[String] = {
    val str = typ.getOrElse("").replaceAll("\\.", "")
    if (str.toLowerCase.contains("комплекс"))
      Set("комплекс")
    else str.split(",").map(_.trim).toSet
  }

}

object Monument {

  def init(text: String, page: String = "", names: Map[String, String]) = {
    val t = new Template(text, page, names)
    val name: String = t.getParam("name")
    new Monument(textParam = text,
      id = t.getParam("ID"),
      name = name,
      year =  t.getParamOpt("year"),
      description =  t.getParamOpt("description"),
      article = getArticle(name),
      city = t.getParamOpt("city"),
      place =  t.getParamOpt("place"),
      user = t.getParamOpt("user"),
      area = t.getParamOpt("area"),
      lat = t.getParamOpt("lat"),
      lon = t.getParamOpt("lon"),
      typ = t.getParamOpt("type"),
      subType =  t.getParamOpt("subType"),
      photo = t.getParamOpt("photo"),
      gallery = t.getParamOpt("gallery"),
      resolution = t.getParamOpt("resolution"),
      page = page,
      names = names
    )
  }

  def getArticle(s: String): Option[String] = {
    val start = s.indexOf("[[")
    val end = s.indexOf("]]")

    if (start >= 0 && end > start && end < s.length)
      Some(s.substring(start + 2, end))
    else
      None
  }

  def wikiLinkToUrl(wikiText: Option[String], host: String):String =
    wikiText.fold(""){t => wikiLinkToUrl(t, host)}

    def wikiLinkToUrl(wikiText: String, host: String):String = {
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

  def monumentsFromText(text: String, page: String, template: String, names: Map[String, String]): Set[Monument] =
    text.split("\\{\\{" + template).tail.map(text => init(text, page, names)).toSet
      //.filter(_.id.nonEmpty).toSet

  // test for "-" id
  def getRegionId(monumentId: String): String =  monumentId.split("\\-").headOption.getOrElse("")

  def getRegionId(monumentId: Option[String]): String =  monumentId.fold("")(getRegionId)

}

