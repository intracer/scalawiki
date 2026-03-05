package org.scalawiki.wlx.actor

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.stream.scaladsl.Source
import org.scalawiki.MwBot
import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.{Contest, Monument}

import scala.concurrent.{ExecutionContext, Future}

object DataFetchingStream {

  /** Production entry point: builds real streaming sources from bot. */
  def run(
      contest: Contest,
      contests: Seq[Contest],
      bot: MwBot,
      total: Boolean,
      target: ActorRef[Command],
      state: StatisticsState
  )(implicit system: ActorSystem[_]): Future[Done] = {
    import org.apache.pekko.actor.typed.scaladsl.adapter._
    val classicSystem = system.toClassic

    val monumentSource = StreamingMonumentQuery.source(contest, bot)(classicSystem)
    val yearSources: Map[Int, Source[Image, _]] = contests
      .map(c => c.year -> StreamingImageQuery.source(c, bot)(classicSystem))
      .toMap
    val totalSource: Source[Image, _] =
      if (total) StreamingImageQuery.source(contest, bot)(classicSystem)
      else Source.empty[Image]

    runWithSources(monumentSource, yearSources, totalSource, total, target, state)
  }

  /** Testable entry point: accepts pre-built sources. */
  def runWithSources(
      monumentSource: Source[Monument, _],
      yearSources: Map[Int, Source[Image, _]],
      totalSource: Source[Image, _],
      total: Boolean,
      target: ActorRef[Command],
      state: StatisticsState
  )(implicit system: ActorSystem[_]): Future[Done] = {
    implicit val ec: ExecutionContext = system.executionContext

    val monumentFuture: Future[Done] =
      if (state.monumentsDone) Future.successful(Done)
      else
        monumentSource
          .runForeach(m => target ! DataFetched(MonumentReceived(m)))
          .map { _ => target ! DataFetched(MonumentsFetchCompleted); Done }

    val yearFutures: Iterable[Future[Done]] = yearSources.collect {
      case (year, src) if !state.yearsComplete.contains(year) =>
        src
          .runForeach(img => target ! DataFetched(ImageReceived(img, year)))
          .map { _ => target ! DataFetched(YearImagesFetchCompleted(year)); Done }
    }

    val totalFuture: Future[Done] =
      if (!total || state.totalDone) Future.successful(Done)
      else
        totalSource
          .runForeach(img => target ! DataFetched(TotalImageReceived(img)))
          .map { _ => target ! DataFetched(TotalImagesFetchCompleted); Done }

    Future
      .sequence(Seq(monumentFuture) ++ yearFutures ++ Seq(totalFuture))
      .map(_ => Done)
  }
}
