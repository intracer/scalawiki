package org.scalawiki.wlx.dto

import java.time.ZonedDateTime

import com.typesafe.config.{Config, ConfigFactory, ConfigParseOptions, ConfigResolveOptions}

import scala.util.Try

/**
  * Represents Wiki Loves X contest
  *
  * @param contestType
  * @param country
  * @param year
  * @param startDate
  * @param endDate
  * @param uploadConfigs
  * @param specialNominations
  */
case class Contest(
                    contestType: ContestType,
                    country: AdmDivision,
                    year: Int,
                    startDate: String = "",
                    endDate: String = "",
                    uploadConfigs: Seq[UploadConfig] = Seq.empty,
                    specialNominations: Seq[SpecialNomination] = Seq.empty,
                    newObjectRating: Option[Int] = None,
                    newAuthorObjectRating: Option[Int] = None
                  ) extends HasImagesCategory {

  def campaign = contestType.code + "-" + country.code

  def name = s"${contestType.name} $year" + countryName.fold("")(" in " + _)

  def countryName: Option[String] =
    if (country != NoAdmDivision())
      Some(country.name)
    else
      None

  /**
    * @return Name of category containing contest images
    */
  override def imagesCategory: String = s"Category:Images from $name".replaceAll(" ", "_")

  /**
    * @return name of template that monument lists consist of
    */
  def listTemplate: Option[String] = uploadConfigs.headOption.map(_.listTemplate)

  /**
    * @return name of template that marks a contest image with monument id
    */
  def fileTemplate: Option[String] = uploadConfigs.headOption.map(_.fileTemplate)

  def listsHost: Option[String] = {
    uploadConfigs.head.listsHost
      .orElse(
        country.languageCodes.headOption.map(_ + ".wikipedia.org")
      )
  }
}

object Contest {

  val opts = ConfigParseOptions.defaults.setAllowMissing(false)

  def load(name: String): Option[Contest] = {
    Try {
      ConfigFactory.load(name, opts, ConfigResolveOptions.defaults)
    }.map(fromConfig)
      .getOrElse {
        val Campaign = "(\\w+)_(\\w+).conf".r
        name match {
          case Campaign(typeCode, countryCode) =>
            for (contestType <- ContestType.byCode(typeCode);
                 country <- Country.byCode(countryCode)
            ) yield
              Contest(contestType, country, ZonedDateTime.now.getYear)
          case _ => None
        }
      }
  }

  def byCampaign(campaign: String): Option[Contest] = {
    load(campaign.replace("-", "_") + ".conf")
  }

  def fromConfig(config: Config): Option[Contest] = {
    val (typeStr, countryStr, year) = (
      config.getString("type"),
      config.getString("country"),
      config.getInt("year"))

    val uploadConfig = UploadConfig.fromConfig(config)

    for (contestType <- ContestType.byCode(typeStr.toLowerCase);
         country <- Country.fromJavaLocales.find(country => country.name == countryStr || country.code == countryStr))
      yield new Contest(contestType, country, year, uploadConfigs = Seq(uploadConfig))
  }

  def ESPCUkraine(year: Int, startDate: String = "01-09", endDate: String = "30-09") =
    new Contest(ContestType.ESPC, Country.Ukraine, year, startDate, endDate, Seq.empty)

  def WLMUkraine(year: Int) =
    load("wlm_ua.conf").get.copy(year = year)

  def WLEUkraine(year: Int) =
    load("wle_ua.conf").get.copy(year = year)

}



