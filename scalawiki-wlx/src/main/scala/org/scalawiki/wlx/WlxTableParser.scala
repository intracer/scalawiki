package org.scalawiki.wlx

import org.scalawiki.wikitext.TableParser
import org.scalawiki.wlx.dto.Monument
import org.scalawiki.wlx.dto.lists.ListConfig

/** Parses list of monuments in wiki table format
  *
  * @param config
  */
class WlxTableParser(val config: ListConfig) {

  def parse(wiki: String): Iterable[Monument] = {
    val table = TableParser.parse(wiki)

    val headers = table.headers.toIndexedSeq

    def getIndex(name: String) = {
      config.namesMap.get(name).flatMap { mappedName =>
        val index = headers.indexOf(mappedName)
        if (index >= 0)
          Some(index)
        else
          None
      }
    }

    val id = getIndex("ID")
    val name = getIndex("name")
    val year = getIndex("year")
    val area = getIndex("area")
    val lat = getIndex("lat")
    val lon = getIndex("lon")
    val image = getIndex("photo")
    val gallery = getIndex("gallery")

    val otherParamNames = table.headers.toSet -- config.namesMap.values

    val otherParamIndexes = otherParamNames.map { name =>
      name -> headers.indexOf(name)
    }.toMap

    table.data.zipWithIndex.map { case (row, index) =>
      val rowSeq = row.toSeq

      def byIndex(index: Option[Int]) = index.map(rowSeq.apply)

      val otherParams = otherParamNames.map { name =>
        name -> rowSeq(otherParamIndexes(name))
      }.toMap

      new Monument(
        page = "",
        id = byIndex(id).getOrElse((index + 1).toString),
        name = byIndex(name).get,
        year = byIndex(year),
        area = byIndex(area),
        lat = byIndex(lat),
        lon = byIndex(lon),
        photo = byIndex(image),
        gallery = byIndex(gallery),
        otherParams = otherParams,
        listConfig = Some(config)
      )
    }
  }
}
