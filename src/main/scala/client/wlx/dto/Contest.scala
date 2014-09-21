package client.wlx.dto

class Contest(
               val contestType: ContestType,
               val country: Country,
               val year: Int,
               val startDate: String,
               val endDate: String,
               val listTemplate: String,
               val fileTemplate: String,
               val specialNominations: Seq[SpecialNomination]) {

  def category: String = s"Category:Images from ${contestType.name} ${year} in ${country.name}".replaceAll(" ", "_")

}

object Contest {

  def WLMUkraine(year: Int, startDate: String = "01-09", endDate: String = "30-09") =
    new Contest(ContestType.WLM, Country.Ukraine, year, startDate, endDate, "ВЛП-рядок", "Monument Ukraine", Seq.empty)

  def WLEUkraine(year: Int, startDate: String, endDate: String) =
    new Contest(ContestType.WLE, Country.Ukraine, year, startDate, endDate, "ВЛЗ-рядок", "UkrainianNaturalHeritageSite", Seq.empty)

}

class SpecialNomination(val name: String, val listTemplate: String, val pages: Seq[String]) {

}

object SpecialNomination {
  val music = new SpecialNomination("Музичні пам'ятки в Україні", "WLM-рядок",
    Seq("Template:WLM-music-navbar"))

  val nationalLiberation = new SpecialNomination("Пам'ятки національно-визвольної боротьби", "WLM-рядок",
    Seq("Вікіпедія:Вікі любить пам'ятки/Пам'ятки національно-визвольної боротьби"))

  val greek = new SpecialNomination("Грецькі пам'ятки в Україні", "WLM-рядок",
    Seq("Вікіпедія:Вікі любить пам'ятки/Грецькі пам'ятки в Україні"))

  val armenian = new SpecialNomination("Вірменські пам'ятки в Україні", "WLM-рядок",
    Seq("Вікіпедія:Вікі любить пам'ятки/Вірменські пам'ятки в Україні"))

  val worldWarOne = new SpecialNomination("Українські пам'ятки Першої світової війни", "WLM-рядок",
    Seq("Вікіпедія:Вікі любить пам'ятки/Українські пам'ятки Першої світової війни"))

  val nominations = Seq(music, nationalLiberation, greek, armenian, worldWarOne)
}