package client.dto

import client.dto.history.History
import org.specs2.mutable.Specification

class HistorySpec extends Specification {


  def fromRevs(revs: Revision*) =
    new History(Page(1).copy(revisions = revs))

  "history" should {
    "123" in {
      val rev1 = Revision().withContent("a1", "b1", "c1")
      val history = fromRevs(rev1)

      val elements = history.annotation.map(_.annotatedElements)

      1 === 1
    }

  }
  "history" should {
    "123" in {
      val rev2 = Revision().withContent("a2", "b1", "c1")
      val rev1 = Revision().withContent("a1", "b1", "c1")
      val rev0 = Revision()
      val history = fromRevs(rev2, rev1, rev0)

      val elements = history.annotation.map(_.annotatedElements)
      1 === 1
    }

  }
}
