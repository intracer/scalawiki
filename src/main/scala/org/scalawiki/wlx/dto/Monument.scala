package org.scalawiki.wlx.dto

import java.net.URLEncoder

import org.scalawiki.dto.Template2
import org.scalawiki.wlx.WlxTemplateParser
import org.scalawiki.wlx.dto.lists.ListConfig

import scala.collection.immutable.ListMap

case class Monument(page: String,
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
                    source: Option[String] = None,
                    otherParams: Map[String, String] = Map.empty,
                    listConfig: ListConfig
                     ) {

  def toUrls = Monument.wikiLinkToUrl(name + " * " + place, "uk.wikipedia.org")

  def galleryLink = gallery.fold("") { title => s" [[:Category:$title|$title]]" }

  def regionId = Monument.getRegionId(id)

  val types = initTypes

  def initTypes: Set[String] = {
    val str = typ.getOrElse("").replaceAll("\\.", "")
    if (str.toLowerCase.contains("комплекс"))
      Set("комплекс")
    else str.split(",").map(_.trim).toSet
  }

  def asWiki = {

    val longest = listConfig.namesMap.values.map(_.length).max

    val names = listConfig.namesMap.mapValues(_.padTo(longest, ' '))

    val paramValues = Map("name" -> Option(name),
      "ID" -> Option(id),
      "year" -> year,
      "description" -> description,
      "article" -> article,
      "city" -> city,
      "place" -> place,
      "user" -> user,
      "area" -> area,
      "lat" -> lat,
      "lon" -> lon,
      "type" -> typ,
      "subType" -> subType,
      "photo" -> photo,
      "gallery" -> gallery,
      "resolution" -> resolution)

    val params =
      names.toSeq.map { case (englName, mappedName) => mappedName -> paramValues(englName).getOrElse("") } ++
        otherParams.toSeq

    val template = new Template2(listConfig.templateName, ListMap(params: _*))

    template.text + "\n"
  }

}

object Monument {

  def init(text: String, page: String = "", listConfig: ListConfig) = {
    new WlxTemplateParser(listConfig, page).parse(text)
    //    val name: String = t.getParam("name")
    //    new Monument(
    //      id = t.getParam("ID"),
    //      name = name,
    //      year =  t.getParamOpt("year"),
    //      description =  t.getParamOpt("description"),
    //      article = getArticle(name),
    //      city = t.getParamOpt("city"),
    //      place =  t.getParamOpt("place"),
    //      user = t.getParamOpt("user"),
    //      area = t.getParamOpt("area"),
    //      lat = t.getParamOpt("lat"),
    //      lon = t.getParamOpt("lon"),
    //      typ = t.getParamOpt("type"),
    //      subType =  t.getParamOpt("subType"),
    //      photo = t.getParamOpt("photo"),
    //      gallery = t.getParamOpt("gallery"),
    //      resolution = t.getParamOpt("resolution"),
    //      page = page,
    //    listConfig = listConfig)
  }

  def getArticle(s: String): Option[String] = {
    val start = s.indexOf("[[")
    val end = s.indexOf("]]")

    if (start >= 0 && end > start && end < s.length)
      Some(s.substring(start + 2, end))
    else
      None
  }

  def wikiLinkToUrl(wikiText: Option[String], host: String): String =
    wikiText.fold("") { t => wikiLinkToUrl(t, host) }

  def wikiLinkToUrl(wikiText: String, host: String): String = {
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

  def monumentsFromText(text: String, page: String, template: String, listConfig: ListConfig): Set[Monument] =
    init(text, page, listConfig).toSet

  //.filter(_.id.nonEmpty).toSet

  // test for "-" id
  def getRegionId(monumentId: String): String = monumentId.split("\\-").headOption.getOrElse("")

  def getRegionId(monumentId: Option[String]): String = monumentId.fold("")(getRegionId)

}

