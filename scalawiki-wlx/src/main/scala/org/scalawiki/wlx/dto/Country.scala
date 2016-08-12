package org.scalawiki.wlx.dto

import java.util.Locale

import scala.collection.immutable.SortedSet

trait AdmDivision {
  def code: String

  def name: String

  def regions: Seq[AdmDivision] = Seq.empty

  def languageCodes: Seq[String] = Seq.empty

  def withoutLangCodes = this

  val regionIds = SortedSet(regions.map(_.code): _*)

  val regionNames = regions.sortBy(_.code).map(_.name)

  val regionById = regions.groupBy(_.code).mapValues(_.head)

  def regionName(regId: String) = regionById.get(regId).map(_.name).getOrElse("")

}

case class NoAdmDivision(code: String = "", name: String = "") extends AdmDivision

case class Country(
                    code: String,
                    name: String,
                    override val languageCodes: Seq[String] = Seq.empty,
                    override val regions: Seq[AdmDivision] = Seq.empty
                  ) extends AdmDivision {

  override def withoutLangCodes = copy(languageCodes = Seq.empty)
}

object Country {

  val Armenia = new Country("am", "Armenia & Nagorno-Karabakh", Seq("hy"))

  val Austria = new Country("au", "Austria", Seq("de"))

  val Catalonia = new Country("ca", "Andorra & Catalan areas", Seq("ca"))

  val Azerbaijan = new Country("az", "Azerbaijan", Seq("az"))

  val Estonia = new Country("ee", "Estonia", Seq("et"))

  val Nepal = new Country("np", "Nepal", Seq("en"))

  val Russia = new Country("ru", "Russia", Seq("ru"))

  val Switzerland = new Country("ch", "Switzerland")

  val Ukraine = new Country("ua", "Ukraine", Seq("uk"),
    Map(
      "80" -> "Київ",
      "07" -> "Волинська область",
      "68" -> "Хмельницька область",
      "05" -> "Вінницька область",
      "35" -> "Кіровоградська область",
      "65" -> "Херсонська область",
      "63" -> "Харківська область",
      "01" -> "Автономна Республіка Крим",
      "32" -> "Київська область",
      "61" -> "Тернопільська область",
      "18" -> "Житомирська область",
      "48" -> "Миколаївська область",
      "46" -> "Львівська область",
      "14" -> "Донецька область",
      "44" -> "Луганська область",
      "74" -> "Чернігівська область",
      "12" -> "Дніпропетровська область",
      "73" -> "Чернівецька область",
      "71" -> "Черкаська область",
      "59" -> "Сумська область",
      "26" -> "Івано-Франківська область",
      "56" -> "Рівненська область",
      "85" -> "Севастополь",
      "23" -> "Запорізька область",
      "53" -> "Полтавська область",
      "21" -> "Закарпатська область",
      "51" -> "Одеська область"
    ).map { case (code, name) => Region(code, name) }.toSeq
  )

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

    Locale.getISOCountries.map { countryCode =>

      val langCodes = countryLangs.getOrElse(countryCode, Seq.empty)

      val locales = langCodes.map(langCode => new Locale(langCode, countryCode))

      val locale = locales.headOption.getOrElse(new Locale("", countryCode))

      val langs = locales.map(_.getDisplayLanguage(Locale.ENGLISH)).distinct

      new Country(locale.getCountry,
        locale.getDisplayCountry(Locale.ENGLISH),
        langCodes
      )
    }
  }
}
