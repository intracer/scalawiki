package org.scalawiki.wlx.dto

object SpecialNomination {
  val music = new SpecialNomination("Музичні пам'ятки в Україні", "WLM-рядок",
    Seq("Template:WLM-music-navbar"))

  val wooden = new SpecialNomination("Пам'ятки дерев'яної архітектури України", "WLM-рядок",
    Seq("Template:WLM Дерев'яна архітектура"))

  val fortification = new SpecialNomination("Замки і фортеці України", "WLM-рядок",
    Seq("Template:WLM замки і фортеці"))

  val tatars = new SpecialNomination("Кримськотатарські пам'ятки в Україні", "WLM-рядок",
    Seq("Вікіпедія:Вікі любить пам'ятки/Кримськотатарські пам'ятки в Україні"))

  val nationalLiberation = new SpecialNomination("Пам'ятки національно-визвольної боротьби", "WLM-рядок",
    Seq("Вікіпедія:Вікі любить пам'ятки/Пам'ятки національно-визвольної боротьби"))

  val greek = new SpecialNomination("Грецькі пам'ятки в Україні", "WLM-рядок",
    Seq("Вікіпедія:Вікі любить пам'ятки/Грецькі пам'ятки в Україні"))

  val armenian = new SpecialNomination("Вірменські пам'ятки в Україні", "WLM-рядок",
    Seq("Вікіпедія:Вікі любить пам'ятки/Вірменські пам'ятки в Україні"))

  val libraries = new SpecialNomination("Бібліотеки", "WLM-рядок",
    Seq("Вікіпедія:Вікі любить пам'ятки/Бібліотеки"))

  val worldWarOne = new SpecialNomination("Українські пам'ятки Першої світової війни", "WLM-рядок",
    Seq("Вікіпедія:Вікі любить пам'ятки/Українські пам'ятки Першої світової війни"))

  val nominations = Seq(music, nationalLiberation, greek, armenian, worldWarOne, wooden, fortification, tatars, libraries)
}

class SpecialNomination(val name: String, val listTemplate: String, val pages: Seq[String])