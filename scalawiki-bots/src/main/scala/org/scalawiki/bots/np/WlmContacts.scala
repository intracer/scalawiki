package org.scalawiki.bots.np

object WlmContacts {

  private val Number = """[ +][\d\-()\s]{10,}""".r

  def main(args: Array[String]): Unit = {
    println(getNumbers.size)
    getNumbers.foreach(println)
  }

  def getLines: Seq[String] = {
    Seq(
      " 0931234567",
      " 380671234567",
      " 063-123-45-67",
      " 097 123-45-67",
      " 095 123 45 67",
      " 095-1234567",
      " (097)1234567",
      " 380 (63) 123 45 67",
      " 38 050 123 45 67"
    )
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
