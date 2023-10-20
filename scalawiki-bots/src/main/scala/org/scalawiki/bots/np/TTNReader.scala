package org.scalawiki.bots.np

import org.apache.poi.ss.usermodel.{Cell, CellType}

import java.io.File
import scala.jdk.CollectionConverters._
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import java.io.FileInputStream



object TTNReader {
  def main(args: Array[String]): Unit = {
    val dir = new File("c:\\wmua\\np")
    val files = dir.listFiles().filter(_.getName.endsWith(".xlsx"))
//    val file = new File(
//      "c:\\wmua\\np\\Специфікація до акту №НП-009788712 від 10.08.2023_14082023183857_3.xlsx")

    val ttns: Seq[TTN] = files.flatMap(readTtns)
    val byMonth =
      ttns.groupBy(_.month).view.mapValues(_.map(_.cost).sum).toSeq.sortBy(_._1)
    val byYear = byMonth.groupMap {
      case (month, ttns) => month.split("\\.").head
    } { case (month, ttns) => ttns }
    val byYearAvg = byYear.view.mapValues(ttns => ttns.sum / ttns.size).toSeq.sortBy(_._1)

    val year2023 = ttns.filter(_.year == "2022")
    val lastYear = year2023.groupBy(_.receiverContact).view.mapValues(x => x.size).map{
      case (contact, count) => (count, contact)
    }.groupBy(_._1).view.mapValues{x =>
      x.toSeq.map(_._2).distinct.sorted
    }.toSeq.sortBy(_._1)

    println(byMonth)
    println(byYearAvg)

    lastYear.filter(_._1 >= 5 )foreach{case (count, people) => println(s"$count: $people")}

//   ttns.filter(x => x.receiverContact.contains("Мамон") && x.year == "2023").sortBy(_.yyMmDd).foreach { t =>
//     println(s"${t.date}, ${t.description}, ${t.mass}, ${t.cost}")
//   }

    val distinct = year2023.map(_.receiverContact).distinct.size
    val all = year2023.size

    println(s"All: $all, distinct: $distinct")

  }

  private def readTtns(file: File): Seq[TTN] = {
    val fis = new FileInputStream(file)
    val workbook = new XSSFWorkbook(fis)
    val sheet = workbook.getSheetAt(0)
    val rowIterator = sheet.iterator.asScala
    rowIterator.toSeq.flatMap { row =>
      TTN.apply(row.cellIterator.asScala.toSeq)
    }
  }

  def readWlmPhoneNumbers() = {

  }
}
