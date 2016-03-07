package org.scalawiki.wlx.dto

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
class Contest(
               val contestType: ContestType,
               val country: Country,
               val year: Int,
               val startDate: String,
               val endDate: String,
               val uploadConfigs: Seq[UploadConfig],
               val specialNominations: Seq[SpecialNomination] = Seq.empty) {

  /**
    * @return Name of category containing contest images
    */
  def category: String = s"Category:Images from ${contestType.name} $year in ${country.name}".replaceAll(" ", "_")

  /**
    * @return name of template that monument lists consist of
    */
  def listTemplate: Option[String] = uploadConfigs.headOption.map(_.listTemplate)

  /**
    * @return name of template that marks a contest image with monument id
    */
  def fileTemplate: Option[String] = uploadConfigs.headOption.map(_.fileTemplate)

}

/**
  * Contest definitions. Need to move them to config files
  */
object Contest {

  def ESPCUkraine(year: Int, startDate: String = "01-09", endDate: String = "30-09") =
    new Contest(ContestType.ESPC, Country.Ukraine, year, startDate, endDate, Seq.empty)

  def WLMUkraine(year: Int, startDate: String = "01-09", endDate: String = "30-09") =
    new Contest(ContestType.WLM, Country.Ukraine, year, startDate, endDate,
      Seq(UploadConfig("wlm-ua", "ВЛП-рядок", "Monument Ukraine", lists.WlmUa)))

  def WLEUkraine(year: Int, startDate: String, endDate: String) =
    new Contest(ContestType.WLE, Country.Ukraine, year, startDate, endDate,
      Seq(UploadConfig("wle-ua", "ВЛЗ-рядок", "UkrainianNaturalHeritageSite", lists.WleUa)))

  def WLEArmenia(year: Int, startDate: String, endDate: String) =
    new Contest(ContestType.WLE, Country.Armenia, year, startDate, endDate,
      Seq(UploadConfig("wle-am", "Բնության հուշարձան ցանկ", "Natural Heritage Armenia & Nagorno-Karabakh", lists.WleAm)))

  def WLECatalonia(year: Int, startDate: String, endDate: String) =
    new Contest(ContestType.WLE, Country.Catalonia, year, startDate, endDate,
      Seq(UploadConfig("wle-cat", "filera patrimoni natural", "WLE-AD-ES", lists.WleCat)))

  def WLEAustria(year: Int, startDate: String, endDate: String) =
    new Contest(ContestType.WLE, Country.Austria, year, startDate, endDate,
      Seq(
        UploadConfig("wle-at-nap", "Nationalpark Österreich Tabellenzeile", "Nationalpark Österreich", lists.WleAu), //National parks
        UploadConfig("wle-at-nsg", "Naturschutzgebiet Österreich Tabellenzeile", "Naturschutzgebiet Österreich", lists.WleAu), // Nature reserves
        UploadConfig("wle-at-glt", "Geschützter Landschaftsteil Österreich Tabellenzeile", "Geschützter Landschaftsteil Österreich", lists.WleAu), // Geschützter Landschaftsteil
        UploadConfig("wle-at-hoe", "Geschützter Landschaftsteil Österreich Tabellenzeile", "Geschützte Höhle Österreich", lists.WleAu), // Geschützte Höhle
        UploadConfig("wle-at-np", "Naturdenkmal Österreich Tabellenzeile", "Naturpark Österreich", lists.WleAu), // Nature parks
        UploadConfig("wle-at-nd", "Naturdenkmal Österreich Tabellenzeile", "Naturdenkmal Österreich", lists.WleAu) //Natural monuments
      ))

  def WLEEstonia(year: Int, startDate: String, endDate: String) =
    new Contest(ContestType.WLE, Country.Estonia, year, startDate, endDate,
      Seq(UploadConfig("wle-ee", "KKR rida", "Loodusmälestis", lists.WleEe)))


  def WLENepal(year: Int, startDate: String, endDate: String) =
    new Contest(ContestType.WLE, Country.Nepal, year, startDate, endDate,
      Seq(UploadConfig("wle-np", "Nepal Monument row WLE", "Wiki Loves Earth Nepal", lists.WleNp)))

  def WLERussia(year: Int, startDate: String, endDate: String) =
    new Contest(ContestType.WLE, Country.Russia, year, startDate, endDate,
      Seq(UploadConfig("wle-ru", "monument", "Protected Area Russia", lists.WleRu)))

  def WLESwitzerland(year: Int, startDate: String, endDate: String) =
    new Contest(ContestType.WLE, Country.Switzerland, year, startDate, endDate,
      Seq(UploadConfig("wle-ch", "Naturalistic heritage CH row", "", lists.WleCh)))

  def allWLE = {
    val year = 2015
    val (start, end) = ("01-05", "31-05")
    Seq(
      //       WLEAustria(year, start, end),
      WLECatalonia(year, start, end),
      WLEEstonia(year, start, end),
      WLENepal(year, start, end),
      WLERussia(year, start, end),
      WLESwitzerland(year, start, end),
      WLEUkraine(year, start, end)
    )
  }


}



