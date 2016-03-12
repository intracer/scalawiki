package org.scalawiki.cache

import org.scalawiki.dto.{Page, Revision}
import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

class CacheSpec extends Specification with ThrownMessages {

  "empty cache" should {
    "not have pages" in {
      val cache = new InMemoryPageStore

      val page = Page(Some(1L), 0, "page1")

      cache.hasPage(1) === false
      cache.getPage(1) === None
    }

    "not have revisions" in {
      val cache = new InMemoryPageStore

      val rev = Revision(Some(2L), Some(1L), content = Some("rev1"))
      val page = Page(Some(1L), 0, "page1", Seq(rev))

      cache.hasPage(1) === false
      cache.getPage(1) === None

      cache.hasRevision(2) === false
      cache.getRevision(2) === None
    }
  }

  "cache" should {
    "remember page" in {
      val cache = new InMemoryPageStore

      val page = Page(Some(1L), 0, "page1")

      cache.addPages(Seq(page))

      cache.hasPage(1) === true
      val cachedOpt = cache.getPage(1)
      cachedOpt.fold(fail("")) {
        _ === page
      }
    }

    "remember revision" in {
      val cache = new InMemoryPageStore

      val rev = Revision(Some(2L), Some(1L), content = Some("rev1"))
      val page = Page(Some(1L), 0, "page1", Seq(rev))

      cache.addPages(Seq(page))

      cache.hasPage(1) === true
      val cachedOpt = cache.getPage(1)
      cachedOpt.fold(fail("")) {
        _ === page
      }

      cache.hasRevision(2) === true
      val cachedRevOpt = cache.getRevision(2)
      cachedRevOpt.fold(fail("")) {
        _ === rev
      }
    }

    "add one revision" in {
      val cache = new InMemoryPageStore

      val rev1 = Revision(Some(2L), Some(1L), content = Some("rev1"))
      val page1 = Page(Some(1L), 0, "page1", Seq(rev1))

      cache.addPages(Seq(page1))

      val rev2 = Revision(Some(3L), Some(1L), content = Some("rev2"))
      val page2 = Page(Some(1L), 0, "page1", Seq(rev2))

      cache.addPages(Seq(page2))

      cache.hasPage(1) === true
      val cachedOpt = cache.getPage(1)
      cachedOpt.fold(fail("")) {
        _ === Page(Some(1L), 0, "page1", Seq(rev2, rev1))
      }

      cache.hasRevision(2) === true
      val cachedRevOpt1 = cache.getRevision(2)
      cachedRevOpt1.fold(fail("")) {
        _ === rev1
      }

      cache.hasRevision(3) === true
      val cachedRevOpt2 = cache.getRevision(3)
      cachedRevOpt2.fold(fail("")) {
        _ === rev2
      }
    }

    "add one revision from revision list" in {
      val cache = new InMemoryPageStore

      val rev1 = Revision(Some(1), Some(1), content = Some("rev1"))
      val page1 = Page(Some(1), 0, "page1", Seq(rev1))

      cache.addPages(Seq(page1))

      val rev2 = Revision(Some(2), Some(1), content = Some("rev2"))
      val page2 = Page(Some(1), 0, "page1", Seq(rev2, rev1))

      cache.addPages(Seq(page2))

      cache.hasPage(1) === true
      val cachedOpt = cache.getPage(1)
      cachedOpt.fold(fail("")) {
        _ === Page(Some(1), 0, "page1", Seq(rev2, rev1))
      }

      cache.hasRevision(1) === true
      val cachedRevOpt1 = cache.getRevision(1)
      cachedRevOpt1.fold(fail("")) {
        _ === rev1
      }

      cache.hasRevision(2) === true
      val cachedRevOpt2 = cache.getRevision(2)
      cachedRevOpt2.fold(fail("")) {
        _ === rev2
      }
    }
  }

}
