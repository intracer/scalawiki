package org.scalawiki.bots.finance

import better.files.{File => SFile}
import org.scalawiki.dto.Page
import org.scalawiki.bots.html

case class Resolution(number: Int, year: Int, month: String, day: String,
                      description: String,
                      text: String,
                      votes: Votes,
                      board: Board) extends Ordered[Resolution] {

  def date: String = s"$day $month $year"

  override def compare(that: Resolution): Int = {
    val years = year.compare(that.year)
    if (years == 0) {
      number.compare(that.number)
    } else years
  }

  override def toString: String = {
    s"РІШЕННЯ № $number/$year\n" +
      "Правління Громадської організації «Вікімедіа Україна»" +
      s"м. Київ  «$day» $month $year року\n" +
      text
  }

  def substRealNames(text: String): String = {
    var replaced = text
    Resolution.realNames.foreach {
      case (nick, name) =>
        replaced = replaced.replace(nick, name)
    }
    replaced
  }

  def htmlText: String = {
    try {
      val catR = "\\[\\[Категорія:Рішення Правління\\|\\d+\\]\\]"
      val linkR = "\\[\\[[^\\|\\]]*\\|"

      val noCats = text.replaceAll(catR, "")
      val noLinks = noCats.replaceAll(linkR, "")
        .replace("]]", "")
        .replace("[[", "")
        .replace("{{u|", "")
        .replace("{{", "")
        .replace("}}", "")
        .replace("{{text|1=", "")
        .replaceAll("^\\s*$", "")

      val noEndingDate = noLinks.replaceAll("\\d+\\.\\d{2}\\.\\d{4}, м. Київ\\.?", "")

      val voteR = "''\\(«За»[^\\)]+\\)''".r
      val withVotes = voteR.replaceAllIn(noEndingDate, m => {
        "<p><i>" + m.matched.toString.replace("''", "") + "</i>"
      })

      val withRealNames = substRealNames(withVotes)

      withRealNames.replaceFirst("#", "<ol><li>")
        .replace("#:", "")
        .replace("#", "<li>")
    } catch {
      case e: Throwable =>
        println(e)
        e.toString
    }
  }
}

object Resolution {

  val realNames = Map.empty[String, String]

  def fromPage(page: Page): Option[Resolution] = {

    val title = page.title

    val regex = "Рішення Правління №(\\d+)\\/(\\d{4}) від (\\d+) (\\p{L}+) (\\d{4})"
      .r("num", "year1", "day", "month", "year2")

    regex.findFirstMatchIn(title).map {
      m =>
        val number = m.group("num").toInt
        val year = m.group("year1").toInt
        val month = m.group("month")
        val day = m.group("day")
        Resolution(number, year, month, day, "", page.text.getOrElse(""), null, null)
    }
  }

  def saveHtml(file: String, resolutions: Seq[Resolution]) = {
    val output = html.resolution(resolutions).toString()
    SFile(file).overwrite(output)
    println(s"written ${resolutions.size} resolutions")
  }
}