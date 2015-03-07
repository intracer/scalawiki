package org.scalawiki.dto

import org.scalawiki.dto.history.Annotation
import org.specs2.mutable.Specification


class AnnotationSpec extends Specification {

  def fromRevs(revs: Revision*) =
    new Annotation(Page(1).copy(revisions = revs))

  "annotation" should {
    "not fail with no revisions" in {
      val annotation = new Annotation(Page(1))

      val elements = annotation.annotatedElements

      elements.size === 0
    }
  }

  "annotation" should {
    "annotate 1 revision" in {
      val r1 = Revision().withContent("a1", "b1")
      val annotation = fromRevs(r1)

      val elements = annotation.annotatedElements

      elements.size === 2
      elements.map(_.getRevision) == Seq(r1, r1)
      elements.map(_.getElement) == Seq("a1", "b1")
    }
  }

  "annotation" should {
    "annotate 2 revisions" in {
      val r2 = Revision().withContent("a2", "b1")
      val r1 = Revision().withContent("a1", "b1")
      val annotation = fromRevs(r2, r1)

      val elements = annotation.annotatedElements

      elements.size === 2
      elements.map(_.getRevision) == Seq(r2, r1)
      elements.map(_.getElement) == Seq("a2", "b1")
    }

  }
}
