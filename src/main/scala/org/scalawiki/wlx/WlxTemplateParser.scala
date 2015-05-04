package org.scalawiki.wlx

import org.scalawiki.parser.TemplateParser
import org.scalawiki.wlx.dto.Monument
import org.scalawiki.wlx.dto.lists.ListConfig

class WlxTemplateParser(val config: ListConfig) {

  def getMappedName(name: String) = config.namesMap.get(name)

  def parse(wiki: String): Iterable[Monument] = {

    val id = getMappedName("ID")
    val name = getMappedName("name")
    val year = getMappedName("year")
    val area = getMappedName("area")
    val lat = getMappedName("lat")
    val lon = getMappedName("lon")
    val image = getMappedName("photo")
    val gallery = getMappedName("gallery")

    val templates = TemplateParser.parse(wiki)


    templates.map {
      template =>

      def byName(name: Option[String]) = name.flatMap(template.getParamOpt).filter(_.trim.nonEmpty)

      val otherParamNames = template.params.keySet -- config.namesMap.values

      val otherParams = otherParamNames.map { name => name -> template.getParam(name) }.toMap

      new Monument(page = "",
        id = byName(id).getOrElse(1.toString),
        name = byName(name).get,
        year = byName(year),
        area = byName(area),
        lat = byName(lat),
        lon = byName(lon),
        photo = byName(image),
        gallery = byName(gallery),
        otherParams = otherParams,
        listConfig = config)
    }
  }
}
