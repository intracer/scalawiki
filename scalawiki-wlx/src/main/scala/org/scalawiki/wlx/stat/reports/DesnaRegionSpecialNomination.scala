package org.scalawiki.wlx.stat.reports

import com.github.tototoshi.csv.CSVReader
import org.scalawiki.wlx.dto.{AdmDivision, Country, Katotth}
import org.scalawiki.wlx.dto.Country.Ukraine

import java.io.InputStreamReader

class DesnaRegionSpecialNomination(data: Seq[List[String]]) {
  val UkraineKatotth: Country = new Country("UA", "Ukraine", Seq("uk"),
    Katotth.regions(() => Some(UkraineKatotth)))

  val places: Seq[List[String]] = data.map(_.filter(_.trim.nonEmpty)).filter(_.size == 4)

  def getOblast(row: List[String], country: Country, suffix: String): Option[AdmDivision] = {
    getOblast(row.head, country, suffix)
  }

  def getOblast(name: String, country: Country, suffix: String): Option[AdmDivision] = {
    country.byName(name.trim + suffix, 0, 1).headOption
  }

  def getRaion(row: List[String], country: Country, suffixes: Seq[String]): Seq[AdmDivision] = {
    (for (oblast <- getOblast(row, country: Country, suffixes.headOption.getOrElse("")))
      yield getRaion(oblast, row(1) + suffixes.lastOption.getOrElse(""), country: Country)).getOrElse(Nil)
  }

  def getRaion(oblast: AdmDivision, name: String, country: Country): Seq[AdmDivision] = {
    val candidates = country.byIdAndName(oblast.code.take(2), name)
    candidates.filter(_.regionType.exists(_.code == "P") || name.endsWith(" район"))
  }

  def getRaion(row: List[String]): Seq[AdmDivision] = {
    val candidates = getRaion(row, UkraineKatotth, Nil)
    val twoTries = if (candidates.isEmpty) {
      getRaion(row, Ukraine, Seq(" область", " район"))
    } else {
      candidates
    }

    if (twoTries.size != 1) {
      println(s"getRaion ${row}, candidates: $candidates")
    }
    twoTries
  }

  def getPlace(row: List[String]): Seq[AdmDivision] = {
    val candidates = getPlace(row, UkraineKatotth, Nil)
    val twoTries = if (candidates.size != 1) {
      getPlace(row, Ukraine, Seq(" область", " район"))
    } else {
      candidates
    }

    twoTries
  }

  def getPlace(row: List[String], country: Country, suffixes: Seq[String]): Seq[AdmDivision] = {
    (for {
      oblast <- getOblast(row, country, suffixes.headOption.getOrElse(""));
      raion <- getRaion(oblast, row(1) + suffixes.lastOption.getOrElse(""), country).headOption
    } yield getPlace(raion, row(3), country)).getOrElse(Nil)
  }

  def getPlace(raion: AdmDivision, name: String, country: Country): Seq[AdmDivision] = {
    val code = raion.code.take(2) + "-" + raion.code.drop(2)
    country.byIdAndName(code, name)
  }
}

object DesnaRegionSpecialNomination {
  def apply(): DesnaRegionSpecialNomination = {
    new DesnaRegionSpecialNomination(readCsv("DesnaRegion"))
  }

  private def readCsv(filename: String): Seq[List[String]] = {
    val reader = new InputStreamReader(getClass.getResourceAsStream(s"/$filename.csv"), "UTF-8")
    val csv = CSVReader.open(reader)
    csv.all().tail
  }
}
