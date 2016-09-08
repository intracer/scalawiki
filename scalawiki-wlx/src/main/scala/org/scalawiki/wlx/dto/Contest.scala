package org.scalawiki.wlx.dto

import com.typesafe.config.{Config, ConfigFactory}
import org.scalawiki.wlx.dto.lists.ListConfig

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
                    rating: Boolean = false) {

  def name = s"${contestType.name} $year in ${country.name}"

  /**
    * @return Name of category containing contest images
    */
  def category: String = s"Category:Images from $name".replaceAll(" ", "_")

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

/**
  * Contest definitions. Need to move them to config files
  */
object Contest {

  def load(name: String): Option[Contest] = {
    fromConfig(ConfigFactory.load(name))
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



