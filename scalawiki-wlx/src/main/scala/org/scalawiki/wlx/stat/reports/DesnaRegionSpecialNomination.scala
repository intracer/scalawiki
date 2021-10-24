package org.scalawiki.wlx.stat.reports

import com.github.tototoshi.csv.CSVReader
import org.scalawiki.wlx.dto.AdmDivision
import org.scalawiki.wlx.dto.Country.Ukraine

import java.io.InputStreamReader

class DesnaRegionSpecialNomination(data: Seq[List[String]]) {
  val places = data.map(_.filter(_.trim.nonEmpty)).filter(_.size == 4)
  val oblastNames = places.flatMap { case List(adm0, adm1, adm2, city) => Some(adm0)}
    .map(_.trim).distinct
  val oblasts = oblastNames.flatMap(name => Ukraine.byName(name + " область"))

  def getOblast(row: List[String]): Option[AdmDivision] = getOblast(row.head)
  def getOblast(name: String): Option[AdmDivision] = Ukraine.byName(name.trim + " область").headOption

  def getRaion(row: List[String]): Seq[AdmDivision] = {
    (for (oblast <- getOblast(row)) yield getRaion(oblast, row(1))).getOrElse(Nil)
  }

  def getRaion(oblast: AdmDivision, name: String): Seq[AdmDivision] = {
    val candidates = Ukraine.byIdAndName(oblast.code, name + " район")
    if (candidates.size != 1) {
      println(s"${oblast.name} $name, candidates: ${candidates.map(_.name)}")
    }
    candidates
  }

  def getPlace(row: List[String]): Seq[AdmDivision] = {
    (for {
      oblast <- getOblast(row);
      raion <- getRaion(oblast, row(1)).headOption
    } yield getPlace(raion, row(3))).getOrElse(Nil)
  }

  def getPlace(raion: AdmDivision, name: String): Seq[AdmDivision] = {
    val code = raion.code.take(2) + "-" + raion.code.drop(2)
    val candidates = Ukraine.byIdAndName(code, name)
    if (candidates.size != 1) {
      println(s"${raion.name} $code $name, candidates: ${candidates.map(_.name)}")
    }
    candidates
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
