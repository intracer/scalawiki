package org.scalawiki.bots.np

import org.apache.poi.ss.usermodel.{Cell, CellType}

case class TTN(
    ttn: String,
    date: String,
    route: String,
    sender: String,
    senderContact: String,
    receiver: String,
    receiverContact: String,
    description: String,
    mass: Double,
    places: Int,
    value: Double,
    cost: Double
) {
  def month: String = date.split("\\.").tail.reverse.mkString(".")
  def year: String = date.split("\\.").last
  def yyMmDd: String = date.split("\\.").reverse.mkString(".")
}

object TTN {
  def apply(cells: Seq[Cell]): Option[TTN] = {
    if (cells.headOption.exists(_.getCellType == CellType.NUMERIC)) {
      Some(
        TTN(
          ttn = cells(1).getStringCellValue,
          date = cells(2).getStringCellValue,
          route = cells(3).getStringCellValue,
          sender = cells(4).getStringCellValue,
          senderContact = cells(5).getStringCellValue,
          receiver = cells(6).getStringCellValue,
          receiverContact = cells(7).getStringCellValue,
          description = cells(8).getStringCellValue,
          mass = cells(9).getNumericCellValue,
          places = cells(10).getNumericCellValue.toInt,
          value = cells(11).getNumericCellValue,
          cost = cells(12).getNumericCellValue
        )
      )
    } else None
  }
}
