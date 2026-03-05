package org.scalawiki.wlx.actor

import org.scalawiki.MwBot
import org.scalawiki.wlx.ImageFiller
import org.scalawiki.wlx.stat.{ContestStat, StatConfig}
import org.scalawiki.wlx.stat.reports._

import scala.concurrent.{ExecutionContext, Future}

object ReporterRegistry {

  def toNamedReporters(
      stat: ContestStat,
      cfg: StatConfig
  )(implicit ec: ExecutionContext): List[NamedReporter] = {
    val commons = MwBot.fromHost(MwBot.commons)

    List(
      Some(NamedReporter("RecentlyTaken",
        _ => new RecentlyTaken(stat).updateWiki(commons).map(_ => ()))),

      if (cfg.specialNominations) Some(NamedReporter("SpecialNominations",
        _ => new SpecialNominations(stat, stat.currentYearImageDb).statistics().map(_ => ())))
      else None,

      if (cfg.authorsStat) Some(NamedReporter("AuthorsStat",
        _ => new AuthorsStat().authorsStat(stat, commons, cfg.gallery).map(_ => ())))
      else None,

      if (cfg.regionalStat) Some(NamedReporter("RegionalStat",
        _ => Output.regionalStat(stat).map(_ => ())))
      else None,

      if (cfg.newMonuments) Some(NamedReporter("NewMonuments",
        _ => Output.newMonuments(stat).map(_ => ())))
      else None,

      if (cfg.mostPopularMonuments) Some(NamedReporter("MostPopularMonuments",
        _ => new MostPopularMonuments(stat).updateWiki(commons).map(_ => ())))
      else None,

      if (cfg.fillLists) Some(NamedReporter("FillLists", { _ =>
        stat.monumentDb.map(mDb =>
          ImageFiller.fillLists(mDb, stat.totalImageDb)
        ).getOrElse(Future.successful(()))
      }))
      else None,

    ).flatten
  }
}
