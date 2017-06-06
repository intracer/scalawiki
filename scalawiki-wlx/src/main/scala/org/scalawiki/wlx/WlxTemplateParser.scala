package org.scalawiki.wlx

import org.scalawiki.dto.markup.Template
import org.scalawiki.wikitext.TemplateParser
import org.scalawiki.wlx.dto.Monument
import org.scalawiki.wlx.dto.lists.ListConfig

/**
  * Parses list of monuments in monument info templates format
  *
  * @param config
  */
class WlxTemplateParser(val config: ListConfig, val page: String) {

  def getMappedName(name: String) = config.namesMap.get(name)

  val id = getMappedName("ID")
  val name = getMappedName("name")
  val year = getMappedName("year")
  val description = getMappedName("description")
  val city = getMappedName("city")
  val place = getMappedName("place")
  val user = getMappedName("user")
  val area = getMappedName("area")
  val lat = getMappedName("lat")
  val lon = getMappedName("lon")
  val image = getMappedName("photo")
  val gallery = getMappedName("gallery")
  val stateId = getMappedName("stateId")
  val typ = getMappedName("type")
  val subType = getMappedName("subType")
  val resolution = getMappedName("resolution")

  def parse(wiki: String): Iterable[Monument] = {
    val templates = TemplateParser.parse(wiki, config.templateName)
    templates.map(templateToMonument)
  }

  def templateToMonument(template: Template): Monument = {
    def byName(name: Option[String]) = name.flatMap(template.getParamOpt).filter(_.trim.nonEmpty)

    val otherParamNames = template.params.keySet -- config.namesMap.values

    val otherParams = otherParamNames.map { name => name -> template.getParam(name) }.toMap

    new Monument(page = page,
      id = removeComments(byName(id).getOrElse(1.toString)).trim,
      name = byName(name).getOrElse(""),
      year = byName(year),
      description = byName(description),
      city = byName(city),
      place = byName(place),
      user = byName(user),
      area = byName(area),
      lat = byName(lat),
      lon = byName(lon),
      photo = byName(image),
      gallery = byName(gallery),
      stateId = byName(stateId),
      typ = byName(typ),
      subType = byName(subType),
      resolution = byName(resolution),
      otherParams = otherParams,
      listConfig = Some(config))
  }

  def removeComments(s: String): String = {
    val start = s.indexOf("<!--")

    if (start > 0) {
      val end = s.indexOf("-->", start + 4)
      if (end > 0) {
        removeComments(s.substring(0, start) + s.substring(end + 3, s.length))
      }
      else s.substring(0, start)
    } else {
      s
    }
  }
}
