package org.scalawiki.wlx.dto

import java.util.Locale

import scala.collection.immutable.SortedSet

trait AdmDivision {
  def code: String

  def name: String

  def regions: Seq[AdmDivision] = Nil

  def languageCodes: Seq[String] = Nil

  def withoutLangCodes = this

  val regionIds: SortedSet[String] = SortedSet(regions.map(_.code): _*)

  val regionNames: Seq[String] = regions.sortBy(_.code).map(_.name)

  val regionById: Map[String, AdmDivision] = regions.groupBy(_.code).mapValues(_.head)

  def regionName(regId: String) = regionById.get(regId).map(_.name).getOrElse("")

  def regionId(monumentId: String): String = monumentId.split("-").take(2).mkString

  def byId(monumentId: String): Option[AdmDivision] = {
    regionById.get(regionId(monumentId)).flatMap(region => region.byId(monumentId).orElse(Some(region)))
  }

  def byRegion(monumentIds: Set[String]): Map[AdmDivision, Set[String]] = {
    val entries = monumentIds.flatMap(id => byId(id).map(adm => id -> adm))

    entries
      .groupBy { case (id, adm) => adm }
      .mapValues(_.toMap.keySet)
  }
}

case class NoAdmDivision(code: String = "", name: String = "") extends AdmDivision

case class Country(code: String,
                   name: String,
                   override val languageCodes: Seq[String] = Nil,
                   override val regions: Seq[AdmDivision] = Nil
                  ) extends AdmDivision {

  override def withoutLangCodes = copy(languageCodes = Nil)

  override def regionId(monumentId: String): String = monumentId.split("-").head

}

object Country {

  val Azerbaijan = new Country("AZ", "Azerbaijan", Seq("az"))

  val Ukraine = new Country("UA", "Ukraine", Seq("uk"), Koatuu.regions)

  val customCountries = Seq(Ukraine)

  def langMap: Map[String, Seq[String]] = {
    Locale.getAvailableLocales
      .groupBy(_.getCountry)
      .map {
        case (countryCode, locales) =>

          val langs = locales.flatMap {
            locale =>
              Option(locale.getLanguage)
                .filter(_.nonEmpty)
          }
          countryCode -> langs.toSeq
      }
  }


  def fromJavaLocales: Seq[Country] = {

    val countryLangs = langMap

    val fromJava = Locale.getISOCountries.map { countryCode =>

      val langCodes = countryLangs.getOrElse(countryCode, Seq.empty)

      val locales = langCodes.map(langCode => new Locale(langCode, countryCode))

      val locale = locales.headOption.getOrElse(new Locale("", countryCode))

      val langs = locales.map(_.getDisplayLanguage(Locale.ENGLISH)).distinct

      new Country(locale.getCountry,
        locale.getDisplayCountry(Locale.ENGLISH),
        langCodes
      )
    }.filterNot(_.code == "ua")

    Seq(Ukraine) ++ fromJava
  }

  lazy val countryMap: Map[String, Country] =
    (fromJavaLocales ++ customCountries).groupBy(_.code.toLowerCase).mapValues(_.head)

  def byCode(code: String): Option[Country] = countryMap.get(code.toLowerCase)
}
