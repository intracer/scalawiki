package client.wlx

import client.wlx.dto.{SpecialNomination, Contest}
import client.wlx.query.{ImageQuery, MonumentQuery}

class Statistics {

  def init(): Unit = {
    val wlmContest = Contest.WLMUkraine(2014, "09-15", "10-15")
    val allContests = (2012 to 2014).map(year =>  Contest.WLMUkraine(year, "09-01", "09-30"))
//    val allContests = Seq(wlmContest) ++ previousContests

    val monumentQuery = MonumentQuery.create(wlmContest)
    val allMonuments = monumentQuery.byMonumentTemplate(wlmContest.listTemplate)

    val monumentDb = new MonumentDB(wlmContest, allMonuments)

    val imageQuery = ImageQuery.create()

//    regionalStat(wlmContest, allContests, monumentDb, imageQuery)

    specialNominations(allContests.find(_.year == 2013).get, monumentDb, imageQuery, monumentQuery)
  }

  def specialNominations(contest: Contest, monumentDb: MonumentDB, imageQuery: ImageQuery, monumentQuery: MonumentQuery) {
    val imageDb = ImageDB.create(contest, imageQuery, monumentDb)

    val imageDbs = SpecialNomination.nominations.map { nomination =>
      val monuments = monumentQuery.byPage(nomination.pages.head, nomination.listTemplate)
      (nomination, imageDb.subSet(monuments))
    }.toMap

    val output = new Output()
    val stat = output.specialNomination(imageDbs)

    println(stat)
  }

  def regionalStat(wlmContest: Contest, allContests: Seq[Contest], monumentDb: MonumentDB, imageQuery: ImageQuery) {
    val imageDbs = allContests.map {
      contest =>
        ImageDB.create(contest, imageQuery, monumentDb)
    }

    val totalImages = imageQuery.imagesWithTemplate(wlmContest.fileTemplate, wlmContest)
    val totalImageDb = new ImageDB(wlmContest, totalImages, monumentDb)

    val output = new Output()

    val idsStat = output.monumentsPictured(imageDbs, totalImageDb, monumentDb)
    println(idsStat)

    val authorStat = output.authorsContributed(imageDbs, totalImageDb, monumentDb)
    println(authorStat)
  }


}

object Statistics {
  def main(args: Array[String]) {
    new Statistics().init()
  }
}
