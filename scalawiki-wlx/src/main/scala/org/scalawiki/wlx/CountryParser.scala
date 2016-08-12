package org.scalawiki.wlx

import org.scalawiki.dto.markup.Table
import org.scalawiki.wikitext.TableParser
import org.scalawiki.wlx.dto.{Contest, ContestType, Country, NoAdmDivision}

import scala.util.matching.Regex.Match

class CountryParser(val table: Table) {

  val headers = table.headers.toIndexedSeq
  val campaignIndex = headers.indexWhere(_.contains("Campaign"))
  val uploadsIndex = headers.indexWhere(_.contains("Uploads"))

  def rowToContest(data: Iterable[String]): Option[Contest] = {
    val indexed = data.toIndexedSeq

    val uploadsCell = indexed(uploadsIndex)

    CountryParser.fromCategoryName(uploadsCell)
  }
}

object CountryParser {

  val countries = Country.fromJavaLocales

  val contestCategory = "Category\\:Images from ([a-zA-Z ]+) (\\d+)"

  val contestRegex = contestCategory.r
  val contestWithCountryRegex = (contestCategory + " in ([a-zA-Z ]+)").r

  def parse(wiki: String): Seq[Contest] = {
    val table = TableParser.parse(wiki)
    val parser = new CountryParser(table)
    table.data.toSeq.flatMap(parser.rowToContest)
  }

  def fromCategoryName(uploadsCell: String): Option[Contest] = {
    contestWithCountryRegex.findFirstMatchIn(uploadsCell).map {
      m =>
        val contest = categoryToContest(m)

        val countryStr = m.group(3)
        val country = countries.find(_.name == countryStr).getOrElse(new Country("", countryStr))

        contest.copy(country = country)
    }.orElse {
      contestRegex.findFirstMatchIn(uploadsCell).map(categoryToContest)
    }
  }

  def categoryToContest(m: Match): Contest = {
    val typeStr = m.group(1)
    val year = m.group(2).toInt

    val _type = ContestType.byName(typeStr)
    new Contest(_type.get, NoAdmDivision(), year, uploadConfigs = Seq.empty)
  }
}
