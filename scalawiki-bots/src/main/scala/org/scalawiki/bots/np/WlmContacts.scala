package org.scalawiki.bots.np

import scala.io.Source
import scala.util.matching.Regex

object WlmContacts {

  private val Number = """[ +][\d\-()\s]{10,}""".r
  private val Lengths = Set(10, 12)

  def main(args: Array[String]): Unit = {
    println(getNumbers.size)
    getNumbers.foreach(println)
  }

  def getLines: Seq[String] = {
    val source = Source.fromFile("c:\\wmua\\np\\progress.txt")
    source.getLines().toSeq
  }

  def getNumbers: Seq[String] = {
    getLines.flatMap(getNumber)
  }

  def getNumber(line: String): Seq[String] = {
    Number
      .findAllIn(line)
      .toSeq
      .map(_.filter(_.isDigit))
      .collect {
        case n if n.length == 10 => "38" + n
        case n if n.length == 12 => n
      }
  }

}
