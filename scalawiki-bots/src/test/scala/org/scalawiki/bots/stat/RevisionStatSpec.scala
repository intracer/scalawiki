package org.scalawiki.bots.stat

import org.scalawiki.dto.{Page, Revision, User}
import org.specs2.mutable.Specification

class RevisionStatSpec extends Specification {

  "RevisionStatSpec" should {

    "work with empty history" in {
      val page = Page("page")
      val stat = RevisionStat.fromPage(page)

      stat.page === page
      stat.history.revisions === Seq.empty
      stat.addedOrRewritten === 0

      val annotation = new RevisionAnnotation(page)
      annotation.annotatedElements === Seq.empty
      annotation.annotation === None

      annotation.byRevisionContent === Map.empty
      annotation.byUserContent === Map.empty

      stat.byRevisionSize === Map.empty
    }

    "work with one revision" in {
      val words = Seq("revision", "text")
      val text = words.mkString(" ")
      val user = "user"
      val rev = Revision(
        revId = Some(1),
        user = Some(User(12, user)),
        content = Some(text)
      )
      val page = new Page(Some(1), Some(0), "page", revisions = Seq(rev))
      val stat = RevisionStat.fromPage(page)

      stat.page === page.withoutContent
      stat.history.revisions === Seq(rev.withoutContent)
      stat.addedOrRewritten === text.length - 1

      val annotation = new RevisionAnnotation(page)
      annotation.byRevisionContent === Map(rev -> words)
      annotation.byUserContent === Map(user -> words)

      stat.byRevisionSize === Map(rev.withoutContent -> (text.length - 1))
    }

    "multibyte stat" in {
      val words = Seq("текст", "ревізії")
      val text = words.mkString(" ")
      val user = "user"
      val rev = Revision(
        revId = Some(1),
        user = Some(User(12, user)),
        content = Some(text)
      )
      val page = new Page(Some(1), Some(0), "page", revisions = Seq(rev))
      val stat = RevisionStat.fromPage(page)

      stat.page === page.withoutContent
      stat.history.revisions === Seq(rev.withoutContent)
      stat.addedOrRewritten === (text.length - 1) * 2

      val annotation = new RevisionAnnotation(page)
      annotation.byRevisionContent === Map(rev -> words)
      annotation.byUserContent === Map(user -> words)

      stat.byRevisionSize === Map(rev.withoutContent -> (text.length - 1) * 2)
    }

    "two revisions add text" in {

      val words1 = Seq("revision1", "text1")
      val words2 = Seq("before", "between", "after")
      val text2Words = Seq("before", "revision1", "between", "text1", "after")

      val text1 = words1.mkString(" ")
      val text2 = text2Words.mkString(" ")

      val user = "user"
      val rev1 = Revision(
        revId = Some(1),
        user = Some(User(12, user)),
        content = Some(text1)
      )
      val rev2 = Revision(
        revId = Some(2),
        user = Some(User(12, user)),
        content = Some(text2)
      )
      val page = new Page(Some(1), Some(0), "page", revisions = Seq(rev2, rev1))
      val stat = RevisionStat.fromPage(page)

      val annotation = new RevisionAnnotation(page)
      annotation.byRevisionContent === Map(rev1 -> words1, rev2 -> words2)
      annotation.byUserContent === Map(user -> text2Words)

      stat.page === page.withoutContent
      stat.history.revisions === Seq(rev2.withoutContent, rev1.withoutContent)
      stat.byRevisionSize === Map(
        rev1.withoutContent -> words1.mkString.length,
        rev2.withoutContent -> words2.mkString.length
      )
      stat.addedOrRewritten === (words1 ++ words2).mkString.length
    }

    "two revisions remove text" in {

      val text1Words = Seq("before", "revision1", "between", "text1", "after")
      val words2 = Seq("revision1", "text1")
      val words1 = Seq("before", "between", "after")

      val text1 = text1Words.mkString(" ")
      val text2 = words2.mkString(" ")

      val user = "user"
      val rev1 = Revision(
        revId = Some(1),
        user = Some(User(12, user)),
        content = Some(text1)
      )
      val rev2 = Revision(
        revId = Some(2),
        user = Some(User(12, user)),
        content = Some(text2)
      )
      val page = new Page(Some(1), Some(0), "page", revisions = Seq(rev2, rev1))
      val stat = RevisionStat.fromPage(page)

      stat.page === page.withoutContent
      stat.history.revisions === Seq(rev2.withoutContent, rev1.withoutContent)
      stat.addedOrRewritten === words2.mkString.length

      val annotation = new RevisionAnnotation(page)
      annotation.byRevisionContent === Map(rev1 -> words2)
      annotation.byUserContent === Map(user -> words2)

      stat.byRevisionSize === Map(rev1.withoutContent -> words2.mkString.length)
    }

    "two revisions replace text" in {

      val words1 = Seq("firstRevision", "text")
      val words2 = Seq("secondRevision", "text")

      val text1 = words1.mkString(" ")
      val text2 = words2.mkString(" ")

      val user = "user"
      val rev1 = Revision(
        revId = Some(1),
        user = Some(User(12, user)),
        content = Some(text1)
      )
      val rev2 = Revision(
        revId = Some(2),
        user = Some(User(12, user)),
        content = Some(text2)
      )
      val page = new Page(Some(1), Some(0), "page", revisions = Seq(rev2, rev1))
      val stat = RevisionStat.fromPage(page)

      val annotation = new RevisionAnnotation(page)
      annotation.byRevisionContent === Map(
        rev1 -> Seq("text"),
        rev2 -> Seq("secondRevision")
      )
      annotation.byUserContent === Map(user -> words2)

      stat.page === page.withoutContent
      stat.history.revisions === Seq(rev2.withoutContent, rev1.withoutContent)
      stat.byRevisionSize === Map(
        rev1.withoutContent -> "text".length,
        rev2.withoutContent -> "secondRevision".length
      )
      stat.addedOrRewritten === words2.mkString.length

    }

  }
}
