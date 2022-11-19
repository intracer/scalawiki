package org.scalawiki.wlx

import scala.io.Source

object Checker {

  def main(args: Array[String]): Unit = {
    val jury = Source.fromFile("jury.txt", "UTF-8").getLines().toBuffer
    val winners = Source.fromFile("Winners.wiki", "UTF-8").getLines().toBuffer

    val missing = jury.filterNot { line =>
      val spaced = line.replace('_', ' ')
      val spacedNoFile = spaced.replace("File:", "")
      val underscored = line.replace(' ', '_')
      val underscoredNoFile = underscored.replace("File:", "")
      winners.exists(_.startsWith(spaced)) ||
        winners.exists(_.startsWith(underscored)) ||
        winners.exists(_.startsWith(spacedNoFile)) ||
        winners.exists(_.startsWith(underscoredNoFile))
    }

    println(s"Missing size: ${missing.size}")
    missing.foreach(println)

    val winnerFiles = winners.collect {
      case line if line.startsWith("File:") =>
        line.takeWhile(_ != '|').trim
    }
    println(s"\nWinners size: ${winnerFiles.size}")

    val missingWinners = winnerFiles.filterNot { line =>
      val spaced = line.replace('_', ' ')
      val underscored = line.replace(' ', '_')
      jury.contains(spaced) || jury.contains(underscored) || line == "File:INSERT FILE NAME"
    }
    println(s"missingWinners size: ${missingWinners.size}")
    missingWinners.foreach(println)

  }

}
