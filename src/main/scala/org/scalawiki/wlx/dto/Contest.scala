package org.scalawiki.wlx.dto

class Contest(
               val contestType: ContestType,
               val country: Country,
               val year: Int,
               val startDate: String,
               val endDate: String,
               val listTemplate: String,
               val fileTemplate: String,
               val specialNominations: Seq[SpecialNomination]) {

  def category: String = s"Category:Images from ${contestType.name} $year in ${country.name}".replaceAll(" ", "_")

}

object Contest {

  def WLMUkraine(year: Int, startDate: String = "01-09", endDate: String = "30-09") =
    new Contest(ContestType.WLM, Country.Ukraine, year, startDate, endDate, "ВЛП-рядок", "Monument Ukraine", Seq.empty)

  def WLEUkraine(year: Int, startDate: String, endDate: String) =
    new Contest(ContestType.WLE, Country.Ukraine, year, startDate, endDate, "ВЛЗ-рядок", "UkrainianNaturalHeritageSite", Seq.empty)

}



