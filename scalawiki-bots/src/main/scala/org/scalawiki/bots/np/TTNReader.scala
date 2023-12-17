package org.scalawiki.bots.np

import org.apache.poi.ss.usermodel.{Cell, CellType}

import java.io.File
import scala.jdk.CollectionConverters._
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import java.io.FileInputStream

case class Person(contact: String, ttns: Seq[TTN])

object Person {
  def receivers(ttns: Seq[TTN]) = {
    ttns.groupBy(_.receiverContact).map {
      case (contact, personReceived) => Person(contact, personReceived)
    }

  }
}

case class TTNData(ttns: Seq[TTN]) {

  val byMonth =
    ttns.groupBy(_.month).view.mapValues(_.map(_.cost).sum).toSeq.sortBy(_._1)
  val byYear = byMonth.groupMap {
    case (month, ttns) => month.split("\\.").head
  } { case (month, ttns) => ttns }

  def variousStat(): Unit = {
    val byYearAvg =
      byYear.view.mapValues(ttns => ttns.sum / ttns.size).toSeq.sortBy(_._1)

    val year2023 = ttns.filter(_.year == "2022")
    val lastYear: Seq[(Int, Seq[String])] = year2023
      .groupBy(_.receiverContact)
      .view
      .mapValues(x => x.size)
      .map {
        case (contact, count) => (count, contact)
      }
      .groupBy(_._1)
      .view
      .mapValues { x =>
        x.toSeq.map(_._2).distinct.sorted
      }
      .toSeq
      .sortBy(_._1)

    println(byMonth)
    println(byYearAvg)

    lastYear.filter(_._1 >= 5) foreach {
      case (count, people) => println(s"$count: $people")
    }

    //   ttns.filter(x => x.receiverContact.contains("Мамон") && x.year == "2023").sortBy(_.yyMmDd).foreach { t =>
    //     println(s"${t.date}, ${t.description}, ${t.mass}, ${t.cost}")
    //   }

    val distinct = year2023.map(_.receiverContact).distinct.size
    val all = year2023.size

    println(s"All: $all, distinct: $distinct")
  }

}

object TTNReader {
  def main(args: Array[String]): Unit = {
    val wlmNumbers = WlmContacts.getNumbers

    val dir = new File("c:\\wmua\\np")
    val ttns2023 = TTNData(readDir(dir)).ttns.filter(_.year == "2023")
    val ttnsLastMonth = ttns2023.filter { ttn =>
      !ttn.receiverContact.contains("Корбут") &&
      ttn.month == "2023.08" // || ttn.month == "2023.09"
    }
    println("Ttns: " + ttnsLastMonth.size)

    val wlmTtns = ttnsLastMonth.filter { ttn =>
      wlmNumbers.exists(n => ttn.receiverContact.contains(n))
    }
    val wlmTtnsWithIndex = wlmTtns.sortBy(_.yyMmDd).zipWithIndex.map(_.swap)
    println("wlm ttns: " + wlmTtnsWithIndex.size)
//    wlmTtnsWithIndex.foreach(println)
    val wlmTtnsNumbers = wlmTtns.map(_.ttn).toSet

    val nonWlmTtns = ttnsLastMonth
      .filterNot(ttn => wlmTtnsNumbers.contains(ttn.ttn))
      .sortBy(_.yyMmDd)
      .zipWithIndex
      .map(_.swap)

    println("not wlm ttns: " + nonWlmTtns.size)
    //    nonWlmTtns.foreach(println)
//    val receivers = ttnsLastMonth.map(_.receiverContact).distinct.sorted
//    println("receivers: " + receivers.size)
//    receivers.foreach(println)

//    ttns2023
//      .filter(x => x.receiverContact.contains("380683381211") && x.year == "2023")
//      .sortBy(_.yyMmDd).zipWithIndex
//      .foreach { case (t, i) =>
//        println(s"${i+1}. ${t.date}, ${t.description}, ${t.mass}, ${t.cost}, ${t.receiverContact}")
//      }

  }

  private def readDir(dir: File): Seq[TTN] = {
    val files = dir.listFiles().filter(_.getName.endsWith(".xlsx"))
    files.flatMap(readFile)
  }

  private def readFile(file: File): Seq[TTN] = {
    val fis = new FileInputStream(file)
    val workbook = new XSSFWorkbook(fis)
    val sheet = workbook.getSheetAt(0)
    val rowIterator = sheet.iterator.asScala
    rowIterator.toSeq.flatMap { row =>
      TTN.apply(row.cellIterator.asScala.toSeq)
    }
  }

  def readWlmPhoneNumbers() = {}
}
