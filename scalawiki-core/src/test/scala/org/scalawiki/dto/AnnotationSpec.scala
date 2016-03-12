package org.scalawiki.dto

import org.scalawiki.dto.history.Annotation
import org.specs2.mutable.Specification


class AnnotationSpec extends Specification {

  def fromRevs(revs: Revision*) =
    new Annotation(Page(1).copy(revisions = revs))

  "annotation" should {
    "not fail with no revisions" in {
      val annotation = new Annotation(Page(1))

      annotation.revisions.isEmpty === true
      annotation.annotatedElements.isEmpty === true
      annotation.byRevisionSize.isEmpty === true
    }
  }

  "annotation" should {
    "annotate 1 revision" in {
      val r1 = Revision(1, 1).withContent("a1", "b1").withUser(1, "u1")
      val annotation = fromRevs(r1)

      val elements = annotation.annotatedElements

      elements.size === 2
      elements.map(_.getRevision) == Seq(r1, r1)
      elements.map(_.getElement) == Seq("a1", "b1")

      val byRevisionSize = annotation.byRevisionSize
      byRevisionSize.size === 1
      byRevisionSize(r1) === 4

      val byRevisionContent = annotation.byRevisionContent
      byRevisionContent.size === 1
      byRevisionContent(r1) === Seq("a1", "b1")

      val byUserSize = annotation.byUserSize
      byUserSize.size === 1
      byUserSize("u1") === 4

      val byUserContent = annotation.byUserContent
      byUserContent.size === 1
      byUserContent("u1") === Seq("a1", "b1")
    }
  }

  "annotation" should {
    "annotate 2 revisions" in {
      val r2 = Revision(2, 1).withContent("a2", "b1", "c1").withUser(1, "u2")
      val r1 = Revision(1, 1).withContent("a1", "b1", "c1").withUser(1, "u1")
      val annotation = fromRevs(r2, r1)

      val elements = annotation.annotatedElements

      elements.size === 3
      elements.map(_.getRevision) == Seq(r2, r1, r1)
      elements.map(_.getElement) == Seq("a2", "b1", "c1")

      val byRevisionSize = annotation.byRevisionSize
      byRevisionSize.size === 2
      byRevisionSize(r1) === 4
      byRevisionSize(r2) === 2

      val byRevisionContent = annotation.byRevisionContent
      byRevisionContent.size === 2
      byRevisionContent(r1) === Seq("b1", "c1")
      byRevisionContent(r2) === Seq("a2")

      val byUserSize = annotation.byUserSize
      byUserSize.size === 2
      byUserSize("u1") === 4
      byUserSize("u2") === 2

      val byUserContent = annotation.byUserContent
      byUserContent.size === 2
      byUserContent("u1") === Seq("b1", "c1")
      byUserContent("u2") === Seq("a2")
    }

  }
}
