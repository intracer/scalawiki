package org.scalawiki.xml

import java.time.ZonedDateTime

import org.scalawiki.Timestamp

import scala.xml.{Node, NodeSeq}

object XmlHelper {

  def parseExportDemo: XmlParser = {
    val is = getClass.getResourceAsStream("/org/scalawiki/xml/export-demo.xml")
    XmlParser.parseInputStream(is)
  }

  def mediawiki(content: AnyRef) =
    <mediawiki xmlns="http://www.mediawiki.org/xml/export-0.10/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.mediawiki.org/xml/export-0.10/ http://www.mediawiki.org/xml/export-0.10.xsd" version="0.10" xml:lang="en">
      {content}
    </mediawiki>.toString()

  def pageXml(
      title: String = "Page title",
      ns: Int = 0,
      id: Long = 1,
      revisions: NodeSeq
  ): Node =
    <page>
      <title>{title}</title>
      <ns>{ns}</ns>
      <id>{id}</id>
      {revisions}
    </page>

  def revisionXml(
      revId: Long,
      parentId: Long,
      timestamp: ZonedDateTime,
      user: String,
      userId: Int,
      comment: String,
      text: String,
      sha1: String
  ): Node =
    <revision>
      <id>{revId}</id>
      <parentid>{parentId}</parentid>
      <timestamp>{Timestamp.format(timestamp)}</timestamp>
      <contributor>
        <username>{user}</username>
        <id>{userId}</id>
      </contributor>
      <comment>{comment}</comment>
      <model>wikitext</model>
      <format>text/x-wiki</format>
      <text xml:space="preserve">{text}</text>
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

}
