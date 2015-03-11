package org.scalawiki.dto.history

import java.util

import org.scalawiki.dto.{Page, Revision}
import org.xwiki.blame.internal.DefaultBlameManager
import org.xwiki.blame.{AnnotatedContent, AnnotatedElement}

class Annotation(val page: Page) {

  def revisions = page.revisions

  val annotation = createAnnotation(revisions :+ Annotation.revision0)

  import scala.collection.JavaConverters._
  val annotatedElements: Seq[AnnotatedElement[Revision, String]] = annotation.toSeq.flatMap(_.iterator().asScala.toSeq)
    .filter(_.getRevision != null)

  val byRevisionContent: Map[Revision, Seq[String]] = annotatedElements.groupBy(_.getRevision).mapValues(_.map(_.getElement))
  val byUserContent: Map[String, Seq[String]] = annotatedElements.groupBy(_.getRevision.user.getOrElse("")).mapValues(_.map(_.getElement))

  val byRevisionSize =  byRevisionContent.mapValues(_.map(_.size).sum)
  val byUserSize =  byUserContent.mapValues(_.map(_.size).sum)

  def createAnnotation(revisions: Seq[Revision]): Option[AnnotatedContent[Revision, String]] = {
    val contentSize = revisions.count(_.content.isDefined)

    if (contentSize > 0) {

      val blameManager = new DefaultBlameManager()

      val annotatedContent = revisions.foldLeft(null.asInstanceOf[AnnotatedContent[Revision, String]]) {
        (annotation, revision) =>
          blameManager.blame(annotation, revision, splitByWords(revision.content))
      }
      Option(annotatedContent)
    } else {
      None
    }
  }

  def splitByWords(content: Option[String]): util.List[String] = {
    val array = content.fold(Array[String]())(_.split("[^\\pL_\\pN]+"))
    util.Arrays.asList(array:_*)
  }

}

object Annotation {

  val revision0: Revision = Revision(0).withContent("")

  def create(page: Page): Option[Annotation] = {
    val contentSize = page.revisions.count(_.content.isDefined)
    if (contentSize > 0) {
      Option(new Annotation(page))
    } else None
  }
}
