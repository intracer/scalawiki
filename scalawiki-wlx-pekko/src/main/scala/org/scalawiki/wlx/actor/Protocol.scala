package org.scalawiki.wlx.actor

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.pattern.StatusReply
import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.scalawiki.wlx.stat.{ContestStat, StatConfig}
import org.scalawiki.wlx.{ImageDB, MonumentDB}

// ---- Commands ----
sealed trait Command

case class GatherData(
    total: Boolean,
    replyTo: ActorRef[StatusReply[Done]]
) extends Command

case class DataFetched(event: StatisticsEvent) extends Command

case object GenerateReports extends Command

// internal: sent when ReporterSupervisorActor finishes
private[actor] case object AllReportsDoneInternal extends Command

// ---- Events (persisted, one per item) ----
sealed trait StatisticsEvent

case class MonumentReceived(monument: Monument)   extends StatisticsEvent
case class ImageReceived(image: Image, year: Int) extends StatisticsEvent
case class TotalImageReceived(image: Image)       extends StatisticsEvent

case object MonumentsFetchCompleted               extends StatisticsEvent
case class YearImagesFetchCompleted(year: Int)    extends StatisticsEvent
case object TotalImagesFetchCompleted             extends StatisticsEvent

// ---- State ----
case class StatisticsState(
    monuments:     Vector[Monument]        = Vector.empty,
    imagesByYear:  Map[Int, Vector[Image]] = Map.empty,
    totalImages:   Vector[Image]           = Vector.empty,
    monumentsDone: Boolean                 = false,
    yearsComplete: Set[Int]                = Set.empty,
    totalDone:     Boolean                 = false
) {

  def isComplete(expectedYears: Set[Int]): Boolean =
    monumentsDone && yearsComplete == expectedYears && totalDone

  def applyEvent(event: StatisticsEvent): StatisticsState = event match {
    case MonumentReceived(m)         => copy(monuments = monuments :+ m)
    case ImageReceived(img, year)    =>
      copy(imagesByYear = imagesByYear.updated(
        year, imagesByYear.getOrElse(year, Vector.empty) :+ img))
    case TotalImageReceived(img)     => copy(totalImages = totalImages :+ img)
    case MonumentsFetchCompleted     => copy(monumentsDone = true)
    case YearImagesFetchCompleted(y) => copy(yearsComplete = yearsComplete + y)
    case TotalImagesFetchCompleted   => copy(totalDone = true)
  }

  def toContestStat(contest: Contest, startYear: Int, cfg: StatConfig): ContestStat = {
    val mDb = new MonumentDB(contest, monuments)
    val imageDbs: Seq[ImageDB] = imagesByYear.map { case (year, imgs) =>
      new ImageDB(contest.copy(year = year), imgs, Some(mDb))
    }.toSeq
    val totalDb = new ImageDB(contest, totalImages, Some(mDb))
    val currentYearDb = imageDbs
      .find(_.contest.year == contest.year)
      .getOrElse(new ImageDB(contest, Seq.empty, Some(mDb)))

    ContestStat(
      contest            = contest,
      startYear          = startYear,
      monumentDb         = Some(mDb),
      currentYearImageDb = currentYearDb,
      totalImageDb       = totalDb,
      dbsByYear          = imageDbs,
      config             = Some(cfg)
    )
  }
}
