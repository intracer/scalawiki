package client.wlx

class Contest(
              val contestType: ContestType,
              val country: Country,
              val year: String,
              val startDate: String,
              val endDate: String,
              val listTemplate: String,
              val fileTemplate: String) {

  def category: String =  s"Images from ${contestType.name} in ${country.name}".replaceAll(" ", "_")

}

object Contest {

  def WLMUkraine(year: String, startDate: String, endDate: String) =
    new Contest(ContestType.WLM, Country.Ukraine, year, startDate, endDate, "ВЛП-рядок", "Monument Ukraine")

  def WLEUkraine(year: String, startDate: String, endDate: String) =
    new Contest(ContestType.WLE, Country.Ukraine, year, startDate, endDate, "ВЛЗ-рядок", "UkrainianNaturalHeritageSite")

}