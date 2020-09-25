package org.scalawiki.bots

import org.scalawiki.dto.Image

object MoveFile {

  val title = "File:Пам'ятний знак на місці першої школи на Русі.JPG"
  def main(args: Array[String]): Unit = {
    val image = new Image(title)
    image.download()
  }
}