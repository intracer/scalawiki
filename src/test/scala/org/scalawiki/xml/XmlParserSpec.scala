package org.scalawiki.xml

import org.joda.time.DateTime
import org.scalawiki.Timestamp
import org.scalawiki.dto.{IpContributor, User, Revision}
import org.scalawiki.xml.XmlHelper._
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import com.github.nscala_time.time.Imports._

import scala.io.Source

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

  "parse mediwiki export-demo" in {
    val is = getClass.getResourceAsStream("/org/scalawiki/xml/export-demo.xml")
    is !== null
    val s = Source.fromInputStream(is).mkString

    val parser = XmlParser.parseString(s)

    parser.siteInfo === Some(SiteInfo(
      Some("DemoWiki"),
      Some("demowiki"),
      Some("MediaWiki 1.24")
    ))

    val namespaces = parser.namespaces
    namespaces.size === 18

    namespaces(-2) === "Media"
    namespaces(-1) === "Special"
    namespaces(0) === ""
    namespaces(1) === "Talk"
    namespaces(15) === "Category talk"

    val pages = parser.iterator.toBuffer
    pages.size === 3

    val p1 = pages(0)
    (p1.title, p1.ns, p1.id) === ("Page title", 0, 1)

    val p1Revs = p1.revisions
    p1Revs.size === 2
    val p1r1 = p1Revs(0)
    (p1r1.revId, p1r1.parentId, p1r1.comment, p1r1.content) === (100, Some(99), Some("I have just one thing to say!"), Some("A bunch of [[text]] here."))
    p1r1.user === Some(User(Some(42), Some("Foobar")))

    val p1r2 = p1Revs(1)
    (p1r2.revId, p1r2.parentId, p1r2.comment, p1r2.content) === (99, None, Some("new!"), Some("An earlier [[revision]]."))
    p1r2.user === Some(IpContributor("10.0.0.2"))

    val p2 = pages(1)
    (p2.title, p2.ns, p2.id) === ("Talk:Page title", 1, 2)
    val p2Revs = p2.revisions
    p2Revs.size === 1
    val p2r1 = p2Revs(0)
    (p2r1.revId, p2r1.parentId, p2r1.comment, p2r1.content) === (101, None, Some("hey"), Some("WHYD YOU LOCK PAGE??!!! i was editing that jerk"))
    p2r1.user === Some(IpContributor("10.0.0.2"))

    val p3 = pages(2)
    (p3.title, p3.ns, p3.id) === ("File:Some image.jpg", 6, 3)
    val p3Revs = p3.revisions
    p3Revs.size === 1
    val p3r1 = p3Revs(0)
    (p3r1.revId, p3r1.parentId, p3r1.comment, p3r1.content) === (102, None, Some("My awesomeest image!"), Some("This is an awesome little imgae. I lurves it. {{PD}}"))
    p3r1.user === Some(User(Some(42), Some("Foobar")))

    val ii = p3.imageInfo
    ii.size === 0

  }

  def checkRevision(revId: Int, parentId: Int, timestamp: DateTime, user: String, userId: Int, comment: String, text: String, revision: Revision): MatchResult[Any] = {
    (revision.id, revision.parentId, revision.timestamp.map(Timestamp.format), revision.comment, revision.content) ===
      (revId, Some(parentId), Some(Timestamp.format(timestamp)), Some(comment), Some(text))

    revision.user === Some(User(Some(userId), Some(user)))
  }
}
