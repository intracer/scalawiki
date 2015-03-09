package org.scalawiki.xml

import org.scalawiki.dto.Revision
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

import scala.xml.{NodeSeq, Node}

class XmlParserSpec extends Specification {

  def mediawiki(content: AnyRef) =
    <mediawiki xmlns="http://www.mediawiki.org/xml/export-0.10/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.mediawiki.org/xml/export-0.10/ http://www.mediawiki.org/xml/export-0.10.xsd" version="0.10" xml:lang="uk">
      {content}
    </mediawiki>.toString()

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

    "parse a page" in {
      val (title, ns, pageId) = ("Page title", 0, 123)

      val (revId, parentId, user, userId, comment, text, sha1) =
          (345, 456, "user", 567, "revision comment", "revision text", "sha1")

      val revsXml = revisionXml(revId, parentId, user, userId, comment, text, sha1)

      val xml = mediawiki(siteInfoXml ++ pageXml(title, ns, pageId, revsXml))
      val parser = XmlParser.parseString(xml)

      val pages = parser.iterator.toSeq
      pages.size === 1

      val page = pages(0)
      (page.id, page.ns, page.title) === (pageId, ns, title)

      val revs = page.revisions
      revs.size === 1
      val rev = revs(0)

      checkRevision(revId, parentId, user, userId, comment, text, rev)
    }

    "parse page with two revisions" in {
      val (title1, ns1, pageId1) = ("Page title1", 0, 123)

      val (revId1, parentId1, user1, userId1, comment1, text1, sha1) =
        (1345, 1456, "user", 1567, "revision comment1", "revision text1", "sha1")
      val (revId2, parentId2, user2, userId2, comment2, text2, sha2) =
        (2345, 2456, "user2", 2567, "revision comment2", "revision text2", "sha2")

      val revsXml1 = revisionXml(revId1, parentId1, user1, userId1, comment1, text1, sha1)
      val revsXml2 = revisionXml(revId2, parentId2, user2, userId2, comment2, text2, sha2)

      val xml = mediawiki(siteInfoXml ++
        pageXml(title1, ns1, pageId1, revsXml1 ++ revsXml2))


      val parser = XmlParser.parseString(xml)

      val pages = parser.iterator.toSeq
      pages.size === 1

      val page1 = pages(0)
      (page1.id, page1.ns, page1.title) === (pageId1, ns1, title1)

      val revs1 = page1.revisions
      revs1.size === 2
      checkRevision(revId1, parentId1, user1, userId1, comment1, text1, revs1(0))
      checkRevision(revId2, parentId2, user2, userId2, comment2, text2, revs1(1))
    }

    "parse two pages" in {
      val (title1, ns1, pageId1) = ("Page title1", 0, 123)
      val (title2, ns2, pageId2) = ("Page title2", 1, 234)

      val (revId1, parentId1, user1, userId1, comment1, text1, sha1) =
        (1345, 1456, "user", 1567, "revision comment1", "revision text1", "sha1")
      val (revId2, parentId2, user2, userId2, comment2, text2, sha2) =
        (2345, 2456, "user2", 2567, "revision comment2", "revision text2", "sha2")

      val revsXml1 = revisionXml(revId1, parentId1, user1, userId1, comment1, text1, sha1)
      val revsXml2 = revisionXml(revId2, parentId2, user2, userId2, comment2, text2, sha2)

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
      checkRevision(revId1, parentId1, user1, userId1, comment1, text1, revs1(0))

      val page2 = pages(1)
      (page2.id, page2.ns, page2.title) === (pageId2, ns2, title2)

      val revs2 = page2.revisions
      revs2.size === 1
      checkRevision(revId2, parentId2, user2, userId2, comment2, text2, revs2(0))
    }

  }

  def checkRevision(revId: Int, parentId: Int, user: String, userId: Int, comment: String, text: String, revision: Revision): MatchResult[Any] = {
    (revision.id, revision.parentId, revision.user, revision.userId, revision.comment, revision.content) ===
      (Some(revId), Some(parentId), Some(user), Some(userId), Some(comment), Some(text))
  }

  def pageXml(title: String = "Page title",
             ns: Int = 0,
             id: Int = 1,
             revisions: NodeSeq): Node =
    <page>
      <title>{title}</title>
      <ns>{ns}</ns>
      <id>{id}</id>
      {revisions}
  </page>

  def revisionXml(revId: Int, parentId: Int, user: String, userId: Int, comment: String, text: String, sha1: String): Node =
    <revision>
    <id>{revId}</id>
    <parentid>{parentId}</parentid>
    <timestamp>2015-02-03T07:33:27Z</timestamp>
    <contributor>
      <username>{user}</username>
      <id>{userId}</id>
    </contributor>
    <minor/>
    <comment>{comment}</comment>
    <model>wikitext</model>
    <format>text/x-wiki</format>
    <text xml:space="preserve" bytes="19424">{text}</text>
    <sha1>{sha1}</sha1>
  </revision>

  val siteInfoXml =
    <siteinfo>
      <sitename>Вікіпедія</sitename>
      <dbname>ukwiki</dbname>
      <base>http://uk.wikipedia.org/wiki/%D0%93%D0%BE%D0%BB%D0%BE%D0%B2%D0%BD%D0%B0_%D1%81%D1%82%D0%BE%D1%80%D1%96%D0%BD%D0%BA%D0%B0</base>
      <generator>MediaWiki 1.24wmf22</generator>
      <case>first-letter</case>
      <namespaces>
        <namespace key="-2" case="first-letter">Медіа</namespace>
        <namespace key="-1" case="first-letter">Спеціальна</namespace>
        <namespace key="0" case="first-letter" />
        <namespace key="1" case="first-letter">Обговорення</namespace>
        <namespace key="2" case="first-letter">Користувач</namespace>
        <namespace key="3" case="first-letter">Обговорення користувача</namespace>
        <namespace key="4" case="first-letter">Вікіпедія</namespace>
        <namespace key="5" case="first-letter">Обговорення Вікіпедії</namespace>
        <namespace key="6" case="first-letter">Файл</namespace>
        <namespace key="7" case="first-letter">Обговорення файлу</namespace>
        <namespace key="8" case="first-letter">MediaWiki</namespace>
        <namespace key="9" case="first-letter">Обговорення MediaWiki</namespace>
        <namespace key="10" case="first-letter">Шаблон</namespace>
        <namespace key="11" case="first-letter">Обговорення шаблону</namespace>
        <namespace key="12" case="first-letter">Довідка</namespace>
        <namespace key="13" case="first-letter">Обговорення довідки</namespace>
        <namespace key="14" case="first-letter">Категорія</namespace>
        <namespace key="15" case="first-letter">Обговорення категорії</namespace>
        <namespace key="100" case="first-letter">Портал</namespace>
        <namespace key="101" case="first-letter">Обговорення порталу</namespace>
        <namespace key="446" case="first-letter">Education Program</namespace>
        <namespace key="447" case="first-letter">Education Program talk</namespace>
        <namespace key="828" case="first-letter">Модуль</namespace>
        <namespace key="829" case="first-letter">Обговорення модуля</namespace>
      </namespaces>
    </siteinfo>

  val redirect =  <page>
    <title>Esperanto</title>
    <ns>0</ns>
    <id>2</id>
    <redirect title="Есперанто"/>
    <revision>
      <id>7186485</id>
      <parentid>7186423</parentid>
      <timestamp>2011-07-06T06:56:28Z</timestamp>
      <contributor>
        <username>Aibot</username>
        <id>3755</id>
      </contributor>
      <minor/>
      <comment>Робот: видалення з Категорія:Перенаправлення з інших мов</comment>
      <text xml:space="preserve">#REDIRECT [[Есперанто]]</text>
      <sha1>5ii9st6e8jrix75gp9s4fqydxm7ar7s</sha1>
      <model>wikitext</model>
      <format>text/x-wiki</format>
    </revision>
  </page>






}
