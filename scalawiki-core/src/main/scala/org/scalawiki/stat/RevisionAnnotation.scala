package org.scalawiki.stat

import org.scalawiki.dto.{Revision, Page}
import org.scalawiki.dto.filter.{AllRevisionsFilter, RevisionFilter}
import org.scalawiki.dto.history.Annotation
import org.xwiki.blame.AnnotatedElement

class RevisionAnnotation(val page: Page, revFilter: RevisionFilter = AllRevisionsFilter) {

  val revisions = revFilter(page.revisions)

  val annotation: Option[Annotation] = Annotation.create(page)

  def pageAnnotatedElements: Seq[AnnotatedElement[Revision, String]] =
    annotation.fold(Seq.empty[AnnotatedElement[Revision, String]])(_.annotatedElements)

  val annotatedElements = pageAnnotatedElements
    .filter(element => revFilter.predicate(element.getRevision))

  val byRevisionContent: Map[Revision, Seq[String]] = annotatedElements.groupBy(_.getRevision).mapValues(_.map(_.getElement))
  val byUserContent: Map[String, Seq[String]] = annotatedElements.groupBy(_.getRevision.user.flatMap(_.name) getOrElse "").mapValues(_.map(_.getElement))

}
