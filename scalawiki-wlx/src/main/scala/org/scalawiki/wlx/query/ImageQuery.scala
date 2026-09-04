package org.scalawiki.wlx.query

import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.list._
import org.scalawiki.dto.cmd.query.{Generator, Query}
import org.scalawiki.dto.{Image, Namespace}
import org.scalawiki.query.QueryLibrary
import org.scalawiki.wlx.dto.{Contest, SpecialNomination}
import org.scalawiki.{ActionBot, MwBot}

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ImageQuery {

  def imagesFromCategory(contest: Contest): Future[Iterable[Image]]

  def imagesWithTemplate(contest: Contest): Future[Iterable[Image]]

  def imagesWithTemplateByIds(contest: Contest, pageIds: Set[Long]): Future[Iterable[Image]]

  def imageIdsWithTemplate(contest: Contest): Future[Iterable[Long]]

  /** Page ids of all files in the contest images category, without imageinfo.
    * Cheap (ids + titles only) — used to diff against a CSV cache.
    */
  def imageIdsFromCategory(contest: Contest): Future[Iterable[Long]]

}

class ImageQueryApi(bot: ActionBot) extends ImageQuery with QueryLibrary {

  /** File templates of every special nomination, regardless of contest year.
    *
    * Recognition must not depend on `contest.year`: a photo uploaded for the
    * 2024 interior nomination still carries `{{WLM2024-UA-interior}}` when it
    * turns up in an all-time / prior-year fetch, and downstream logic (e.g.
    * [[org.scalawiki.wlx.stat.rating.NumberOfInteriorImagesBonus]]) needs to see
    * it. Consumers that care about a specific year filter by exact template name.
    */
  private val allSpecialNominationTemplates: Set[String] =
    SpecialNomination.nominations.flatMap(_.fileTemplate).toSet

  private def categoryGenerator(contest: Contest): Generator = Generator(
    CategoryMembers(
      CmTitle(contest.imagesCategory),
      CmNamespace(Seq(Namespace.FILE)),
      CmLimit("max")
    )
  )

  override def imagesFromCategory(contest: Contest): Future[Iterable[Image]] =
    imagesByGenerator(contest, categoryGenerator(contest))

  override def imageIdsFromCategory(contest: Contest): Future[Iterable[Long]] =
    imageIdsByGenerator(categoryGenerator(contest))

  override def imagesWithTemplate(contest: Contest): Future[Iterable[Image]] =
    contest.fileTemplate
      .map { template =>
        imagesByGenerator(contest, generatorWithTemplate(template, Set(Namespace.FILE)))
      }
      .getOrElse(Future.successful(Nil))

  override def imageIdsWithTemplate(contest: Contest): Future[Iterable[Long]] =
    contest.fileTemplate
      .map { template =>
        imageIdsByGenerator(generatorWithTemplate(template, Set(Namespace.FILE)))
      }
      .getOrElse(Future.successful(Nil))

  override def imagesWithTemplateByIds(
      contest: Contest,
      pageIds: Set[Long]
  ): Future[Iterable[Image]] = {
    bot.log.info(s"imagesWithTemplateByIds pageIds size: ${pageIds.size}")
    val blockSize = 50
    val fetched = new AtomicInteger(0)
    val specialNominationTemplates = allSpecialNominationTemplates
    Future
      .sequence(pageIds.toSeq.sorted.grouped(blockSize).map { idsSlice =>
        imagesByIds(idsSlice, withMetadata = true)
        for (pages <- bot.run(imagesByIds(idsSlice, withMetadata = true)))
          yield {
            bot.log.info(s"Fetched ${fetched.addAndGet(pages.size)} of ${pageIds.size}")
            pages.flatMap(
              Image.fromPage(contest.fileTemplate, specialNominationTemplates)
            )
          }
      })
      .map(_.flatten.toIndexedSeq)
  }

  private def imagesByGenerator(contest: Contest, generator: Generator): Future[Iterable[Image]] = {
    val specialNominationTemplates = allSpecialNominationTemplates
    for (pages <- bot.run(imagesByGenerator(generator, withMetadata = true)))
      yield pages.flatMap(
        Image.fromPage(contest.fileTemplate, specialNominationTemplates)
      )
  }

  private def imageIdsByGenerator(generator: Generator): Future[Iterable[Long]] = {
    bot.run(Action(Query(generator))).map(_.flatMap(_.id))
  }

}

object ImageQuery {

  def create(implicit bot: ActionBot = MwBot.fromHost(MwBot.commons)): ImageQuery =
    new ImageQueryApi(bot)

}
