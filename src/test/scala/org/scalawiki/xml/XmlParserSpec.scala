package org.scalawiki.xml

import org.joda.time.DateTime
import org.scalawiki.Timestamp
import org.scalawiki.dto.Revision
import org.scalawiki.xml.XmlHelper._
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import com.github.nscala_time.time.Imports._

class XmlParserSpec extends Specification {

  "xml parser " should {

    "parse empty dump" in {
      val parser = XmlParser.parseString(mediawiki(""))
      val seq = parser.iterator.toSeq
      seq.isEmpty === true
    }

    "parse siteinfo" in {
      val s = mediawiki(siteInfoXml)
      val parser = XmlParser.parseString(s)
      val seq = parser.iterator.toSeq
      seq.isEmpty === true

      parser.siteInfo === Some(SiteInfo(
        Some("Вікіпедія"),
        Some("ukwiki"),
        Some("MediaWiki 1.24wmf22")
      ))

      val namespaces = parser.namespaces
      namespaces.size === 24

      namespaces(-2) === "Медіа"
      namespaces(0) === ""
    }

    "parse a page and siteinfo" in {
      val (title, ns, pageId) = ("Page title", 0, 123)

      val (revId, parentId, timestamp, user, userId, comment, text, sha1) =
          (345, 456, DateTime.now, "user", 567, "revision comment", "revision text", "sha1")

      val revsXml = revisionXml(revId, parentId, timestamp, user, userId, comment, text, sha1)

      val xml = mediawiki(siteInfoXml ++ pageXml(title, ns, pageId, revsXml))
      val parser = XmlParser.parseString(xml)

      val pages = parser.iterator.toSeq
      pages.size === 1

      val page = pages(0)
      (page.id, page.ns, page.title) === (pageId, ns, title)

      val revs = page.revisions
      revs.size === 1
      val rev = revs(0)

      checkRevision(revId, parentId, timestamp, user, userId, comment, text, rev)
    }

    "parse a page without siteinfo" in {
      val (title, ns, pageId) = ("Page title", 0, 123)

      val (revId, parentId, timestamp, user, userId, comment, text, sha1) =
        (345, 456, DateTime.now, "user", 567, "revision comment", "revision text", "sha1")

      val revsXml = revisionXml(revId, parentId, timestamp, user, userId, comment, text, sha1)

      val xml = mediawiki(pageXml(title, ns, pageId, revsXml))
      val parser = XmlParser.parseString(xml)

      val pages = parser.iterator.toSeq
      pages.size === 1

      val page = pages(0)
      (page.id, page.ns, page.title) === (pageId, ns, title)

      val revs = page.revisions
      revs.size === 1
      val rev = revs(0)

      checkRevision(revId, parentId, timestamp, user, userId, comment, text, rev)
    }

    "parse page with two revisions" in {
      val (title1, ns1, pageId1) = ("Page title1", 0, 123)

      val (revId1, parentId1, timestamp1, user1, userId1, comment1, text1, sha1) =
        (1345, 1456, DateTime.now - 1.month, "user", 1567, "revision comment1", "revision text1", "sha1")
      val (revId2, parentId2, timestamp2, user2, userId2, comment2, text2, sha2) =
        (2345, 2456, DateTime.now, "user2", 2567, "revision comment2", "revision text2", "sha2")

      val revsXml1 = revisionXml(revId1, parentId1, timestamp1, user1, userId1, comment1, text1, sha1)
      val revsXml2 = revisionXml(revId2, parentId2, timestamp2, user2, userId2, comment2, text2, sha2)

      val xml = mediawiki(siteInfoXml ++
        pageXml(title1, ns1, pageId1, revsXml1 ++ revsXml2))

      val parser = XmlParser.parseString(xml)

      val pages = parser.iterator.toSeq
      pages.size === 1

      val page1 = pages(0)
      (page1.id, page1.ns, page1.title) === (pageId1, ns1, title1)

      val revs1 = page1.revisions
      revs1.size === 2
      checkRevision(revId1, parentId1, timestamp1, user1, userId1, comment1, text1, revs1(0))
      checkRevision(revId2, parentId2, timestamp2, user2, userId2, comment2, text2, revs1(1))
    }

    "parse two pages" in {
      val (title1, ns1, pageId1) = ("Page title1", 0, 123)
      val (title2, ns2, pageId2) = ("Page title2", 1, 234)

      val (revId1, parentId1, timestamp1, user1, userId1, comment1, text1, sha1) =
        (1345, 1456, DateTime.now - 1.month, "user", 1567, "revision comment1", "revision text1", "sha1")
      val (revId2, parentId2, timestamp2, user2, userId2, comment2, text2, sha2) =
        (2345, 2456, DateTime.now - 2.month, "user2", 2567, "revision comment2", "revision text2", "sha2")

      val revsXml1 = revisionXml(revId1, parentId1, timestamp1, user1, userId1, comment1, text1, sha1)
      val revsXml2 = revisionXml(revId2, parentId2, timestamp2, user2, userId2, comment2, text2, sha2)

      val xml = mediawiki(siteInfoXml ++
        pageXml(title1, ns1, pageId1, revsXml1) ++
        pageXml(title2, ns2, pageId2, revsXml2))

      val parser = XmlParser.parseString(xml)

      val pages = parser.iterator.toSeq
      pages.size === 2

      val page1 = pages(0)
      (page1.id, page1.ns, page1.title) === (pageId1, ns1, title1)

      val revs1 = page1.revisions
      revs1.size === 1
      checkRevision(revId1, parentId1, timestamp1, user1, userId1, comment1, text1, revs1(0))

      val page2 = pages(1)
      (page2.id, page2.ns, page2.title) === (pageId2, ns2, title2)

      val revs2 = page2.revisions
      revs2.size === 1
      checkRevision(revId2, parentId2, timestamp2, user2, userId2, comment2, text2, revs2(0))
    }

  }

  def checkRevision(revId: Int, parentId: Int, timestamp: DateTime, user: String, userId: Int, comment: String, text: String, revision: Revision): MatchResult[Any] = {
    (revision.id, revision.parentId, revision.timestamp.map(Timestamp.format), revision.user, revision.userId, revision.comment, revision.content) ===
      (revId, Some(parentId), Some(Timestamp.format(timestamp)), Some(user), Some(userId), Some(comment), Some(text))
  }
}
