package org.scalawiki.wlx.dto

import java.net.URLEncoder

import org.scalawiki.dto.markup.Template
import org.scalawiki.wlx.WlxTemplateParser
import org.scalawiki.wlx.dto.lists.ListConfig

import scala.collection.immutable.ListMap
import scala.util.Try

case class Monument(page: String = "",
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
                    listConfig: Option[ListConfig] = None
                   ) extends Ordered[Monument] {

  val cityName = AdmDivision.cleanName(city.getOrElse(""))

  def toUrls = Monument.wikiLinkToUrl(name + " * " + place, "uk.wikipedia.org")

  def galleryLink = gallery.fold("") { title => s" [[:Category:$title|$title]]" }

  def regionId = Monument.getRegionId(id)

  val types = initTypes

  def initTypes: Set[String] = {
    val str = typ.getOrElse("").replaceAll("\\.", "").replace("'", "")
    if (str.toLowerCase.contains("комплекс"))
      Set("комплекс")
    else str.split(",").map(_.trim.split("<").head).filter{ monumentType =>
      true // monumentType.contains("-")
    }.toSet.filterNot(_.isBlank)
  }

  def asWiki(templateName: Option[String] = None, pad: Boolean = true) = {

    val paramValues = ListMap(
      "ID" -> Option(id),
      "name" -> Option(name),
      "stateId" -> stateId,
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

    val names = listConfig.fold(paramValues.filter(_._2.nonEmpty).keys)(_.namesMap.values.toSeq)
    val namesMap = listConfig.map(_.namesMap).getOrElse(names.map(name => name -> name).toMap)
    val longest = names.map(_.length).max

    val namesMapPadded = if (pad) namesMap.mapValues(_.padTo(longest, ' ')) else namesMap

    val params =
      namesMapPadded.toSeq.map {
        case (englName, mappedName) => mappedName -> paramValues(englName).getOrElse("")
      } ++
        otherParams.toSeq

    val template = new Template(templateName.orElse(listConfig.map(_.templateName)).get, ListMap(params: _*))

    template.text + "\n"
  }

  override def compare(that: Monument): Int = id.compare(that.id)
}

object Monument {

  def init(text: String, page: String = "", listConfig: ListConfig) = {
    new WlxTemplateParser(listConfig, page).parse(text)
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

  def monumentsFromText(text: String, page: String, template: String, listConfig: ListConfig): Iterable[Monument] =
    init(text, page, listConfig) //.toSet

  def getRegionId(monumentId: String): String = {
    val parts = monumentId.split("\\-")
    parts.headOption
      .filter(Country.Ukraine.regionIds.contains)
      .orElse(Try(parts(1)).toOption.map(_.take(2)))
      .filter(Country.Ukraine.regionIds.contains)
      .getOrElse("")
  }

  def getRegionId(monumentId: Option[String]): String = monumentId.fold("")(getRegionId)

}

