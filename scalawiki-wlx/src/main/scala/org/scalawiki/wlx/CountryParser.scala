package org.scalawiki.wlx

import org.scalawiki.dto.markup.Table
import org.scalawiki.wikitext.TableParser
import org.scalawiki.wlx.dto.{Contest, ContestType, Country}

class CountryParser(val table: Table) {

  val headers = table.headers.toIndexedSeq
  val campaignIndex = headers.indexWhere(_.contains("Campaign"))
  val uploadsIndex = headers.indexWhere(_.contains("Uploads"))

    val categoryLinkRegex = "\\[\\[\\:Category\\:Images from ([a-zA-Z ]+) (\\d+) in ([a-zA-Z ]+)".r

  def rowToContest(data: Iterable[String]): Option[Contest] = {
    val indexed = data.toIndexedSeq

    val campaignCell = indexed(campaignIndex)
    val uploadsCell = indexed(uploadsIndex)

    val countries = Country.fromJavaLocales

    categoryLinkRegex.findFirstMatchIn(uploadsCell).map {
      m =>
        val typeStr = m.group(1)
        val yearStr = m.group(2)
        val countryStr = m.group(3)

        val _type = ContestType.byName(typeStr)
        val country = countries.find(_.name == countryStr).get
        val year = yearStr.toInt

      new Contest(_type.get, country, year, null, null, Seq.empty)
    }

  }

}

object CountryParser {

  def parse(wiki: String): Seq[Contest] = {
    val table = TableParser.parse(wiki)
    val parser = new CountryParser(table)
    table.data.toSeq.flatMap(parser.rowToContest)
  }

}
