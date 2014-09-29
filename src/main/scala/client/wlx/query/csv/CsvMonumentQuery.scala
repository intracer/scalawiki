package client.wlx.query.csv

import java.io.File

import client.slick.Slick
import client.wlx.MonumentDB
import client.wlx.dto.{Contest, Monument}
import client.wlx.query.MonumentQuery
import com.github.tototoshi.csv.CSVReader
import com.rockymadden.stringmetric.similarity.LevenshteinMetric

class CsvMonumentQuery {

//  № п/п,Назва,,Дати,Адреса,,Охоронний номер,,Назва документу про взяття на облік


  def readCsv(path: String) = {
    val reader = CSVReader.open(new File("C:\\wm\\wlm\\kiev\\lists\\Shevchenkivskyi.csv"))
    val monumentsCsv = reader.all().zipWithIndex.map{
      case (line, n) => fromCsv(n, line)
    }

    //monumentsCsv.foreach(println)

    val slick = new Slick()

    val wlmContest = Contest.WLMUkraine(2014, "09-15", "10-15")

    val monumentQuery = MonumentQuery.create(wlmContest)
    val monumentsWiki = monumentQuery.byPage("Вікіпедія:Вікі любить пам'ятки/Київ/navbar", wlmContest.listTemplate, true).filter(_.id.startsWith("80-291"))
    val monumentDb = new MonumentDB(wlmContest, monumentsWiki)

    for(csv <- monumentsCsv;
      wiki <- monumentsWiki) {

      val nameD = LevenshteinMetric.compare(csv.name, wiki.name)
      val yearD = LevenshteinMetric.compare(csv.year.getOrElse(""), wiki.year.getOrElse(""))
      val addressD = LevenshteinMetric.compare(csv.place.getOrElse(""), wiki.place.getOrElse(""))
      val numD = LevenshteinMetric.compare(csv.stateId.getOrElse(""), wiki.stateId.getOrElse(""))

    }
  }

  def fromCsv(n: Int, line: Seq[String]) = {
    val name = line(1)
    val date = line(2) + line(3)
    val address = line(4)
    val number = line(5)
    val resolution = line(6) + line(7)
    new Monument("", "", n.toString, name, year = Some(date), place = Some(address), stateId = Some(number), resolution = Some(resolution))
  }

}


object CsvReader {
  def main(args: Array[String]) {
    new CsvMonumentQuery().readCsv("")
  }
}