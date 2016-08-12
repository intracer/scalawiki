package org.scalawiki.wlx

import org.scalawiki.wikitext.TableParser
import org.scalawiki.wlx.dto.{Contest, ContestType, Country, NoAdmDivision}

import scala.util.Try
import scala.util.matching.Regex.Match

object CountryParser {

  val countries = Country.fromJavaLocales

  val contestCategory = "Category\\:Images from ([a-zA-Z ]+) (\\d+)"

  val contestRegex = contestCategory.r
  val contestWithCountryRegex = (contestCategory + " in ([a-zA-Z ]+)").r

  val contestLinkRegex = "\\[\\[Commons\\:([a-zA-Z ]+) (\\d+) in ([a-zA-Z\\& ]+)\\|".r

  def parseTable(wiki: String) = {
    val table = TableParser.parse(wiki)

    val headers = table.headers.toIndexedSeq
    val uploadsIndex = headers.indexWhere(_.contains("Uploads"))

    if (uploadsIndex < 0) throw
      new IllegalArgumentException("Could not find the country lists")

    def rowToContest(data: Iterable[String]): Option[Contest] = {
      CountryParser.fromCategoryName(data.toIndexedSeq(uploadsIndex))
    }

    table.data.toSeq.flatMap(rowToContest)
  }

  def parsePageLinks(wiki: String) = {
    contestLinkRegex.findAllMatchIn(wiki).map(matchToContestWithCountry).toSeq.distinct
  }

  def parse(wiki: String): Seq[Contest] = {
    (Try {
      parseTable(wiki)
    } recover {
      case _ =>
        parsePageLinks(wiki)
    }).get
  }

  def fromCategoryName(uploadsCell: String): Option[Contest] = {
    contestWithCountryRegex.findFirstMatchIn(uploadsCell).map(matchToContestWithCountry)
      .orElse {
        contestRegex.findFirstMatchIn(uploadsCell).map(categoryToContest)
      }
  }

  def matchToContestWithCountry(m: Match): Contest = {
    val contest = categoryToContest(m)

    val countryStr = m.group(3)
    val country = countries.find(_.name == countryStr).getOrElse(new Country("", countryStr))

    contest.copy(country = country)
  }

  def categoryToContest(m: Match): Contest = {
    val typeStr = m.group(1)
    val year = m.group(2).toInt

    val _type = ContestType.byName(typeStr)
    new Contest(_type.get, NoAdmDivision(), year, uploadConfigs = Seq.empty)
  }
}
