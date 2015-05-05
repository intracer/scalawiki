package org.scalawiki.wlx

import org.scalawiki.parser.TemplateParser
import org.scalawiki.wlx.dto.Monument
import org.scalawiki.wlx.dto.lists.ListConfig

class WlxTemplateParser(val config: ListConfig, val page: String) {

  def getMappedName(name: String) = config.namesMap.get(name)

  def parse(wiki: String): Iterable[Monument] = {

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
    val typ = getMappedName("type")
    val subType = getMappedName("subType")
    val resolution = getMappedName("resolution")

    val templates = TemplateParser.parse(wiki, config.templateName)


    templates.map {
      template =>

        def byName(name: Option[String]) = name.flatMap(template.getParamOpt).filter(_.trim.nonEmpty)

        val otherParamNames = template.params.keySet -- config.namesMap.values

        val otherParams = otherParamNames.map { name => name -> template.getParam(name) }.toMap

        new Monument(page = page,
          id = byName(id).getOrElse(1.toString),
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
          typ = byName(typ),
          subType = byName(subType),
          resolution = byName(resolution),
          otherParams = otherParams,
          listConfig = config)
    }
  }
}
