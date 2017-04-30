package org.scalawiki.wlx

import org.scalawiki.wikitext.TableParser
import org.scalawiki.wlx.CountryParser.{contestRegex, countries}
import org.scalawiki.wlx.dto.{Contest, ContestType, Country, NoAdmDivision}

import scala.util.Try
import scala.util.matching.Regex
import scala.util.matching.Regex.Match

/**
  * Parses information about contest country
  */
object CountryParser {

  val countries = Country.fromJavaLocales

  val contestCategory = "Category\\:Images from ([a-zA-Z ]+) (\\d+)"

  val contestRegex = (contestCategory + "$").r
  val contestWithCountryRegex = (contestCategory + " in ([a-zA-Z\\&\\- ]+)$").r

  val contestLinkRegex = "\\[\\[Commons\\:([a-zA-Z ]+) (\\d+) in ([a-zA-Z\\& ]+)\\|".r

  def isContestCategory(s: String) = contestRegex.pattern.matcher(s).matches()

  val parser = new RegexCampaignParser(contestWithCountryRegex)

  /**
    * Parses information from wiki-table of participating countries like
    * https://commons.wikimedia.org/wiki/Commons:Wiki_Loves_Earth_2016
    *
    * @param wikiText participating countries table wiki-markup
    * @return list of contests from the table
    */
  def parse(wikiText: String): Seq[Contest] = {
    (Try {
      parseTable(wikiText)
    } recoverWith { case _ => Try {
      parsePageLinks(wikiText)
    }
    }).getOrElse(Seq.empty)
  }

  /**
    * Parses information from wiki-table of participating countries like
    * https://commons.wikimedia.org/wiki/Commons:Wiki_Loves_Earth_2016
    *
    * @param wikiText participating countries table wiki-markup
    * @return list of contests from the table
    */
  def parseTable(wikiText: String): Seq[Contest] = {
    val table = TableParser.parse(wikiText)

    val headers = table.headers.toIndexedSeq
    val uploadsIndex = headers.indexWhere(_.contains("Uploads"))

    if (uploadsIndex < 0) throw
      new IllegalArgumentException("Could not find the country lists")

    def rowToContest(data: Iterable[String]): Option[Contest] =
      fromCategoryName(data.toIndexedSeq(uploadsIndex))

    table.data.toSeq.flatMap(rowToContest)
  }

  def parsePageLinks(wikiText: String): Seq[Contest] =
    new RegexCampaignParser(contestLinkRegex).findAll(wikiText)

  def fromCategoryName(uploadsCell: String): Option[Contest] =
    parser.findWithBackup(uploadsCell)

}

class RegexCampaignParser(r: Regex, typeIndex: Int = 1, yearIndex: Int = 2, countryIndex: Int = 3) {

  def findWithBackup(cell: String): Option[Contest] = {
    r.findFirstMatchIn(cell).flatMap(matchToContestWithCountry)
      .orElse {
        contestRegex.findFirstMatchIn(cell).flatMap(categoryToContest)
      }
  }

  def findAll(wikiText: String): Seq[Contest] = {
    r.findAllMatchIn(wikiText).flatMap(matchToContestWithCountry).toSeq.distinct
  }

  def matchToContestWithCountry(m: Match): Option[Contest] = {
    for (contest <- categoryToContest(m)) yield {

      val countryStr = m.group(countryIndex)
      val country = countries.find(_.name == countryStr).getOrElse(new Country("", countryStr))
      contest.copy(country = country)
    }
  }

  def categoryToContest(m: Match): Option[Contest] = {
    val typeStr = m.group(typeIndex)
    val year = m.group(yearIndex).toInt

    for (typ <- ContestType.byName(typeStr))
      yield new Contest(typ, NoAdmDivision(), year, uploadConfigs = Seq.empty)
  }
}
