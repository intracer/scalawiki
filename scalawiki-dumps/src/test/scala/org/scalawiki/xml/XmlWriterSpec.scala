package org.scalawiki.xml

import java.io.StringWriter
import java.time.ZonedDateTime

import org.scalawiki.dto.{Page, Revision, User}
import org.scalawiki.xml.XmlHelper._
import org.specs2.matcher.XmlMatchers
import org.specs2.mutable.Specification

import scala.xml.Utility._
import scala.xml.XML

class XmlWriterSpec extends Specification with XmlMatchers {

  "xml writer" should {
    "serialize page" in {
      val (title, ns, pageId) = ("Page title", 0, 123)

      val (revId, parentId, timestamp, user, userId, comment, text, minor, sha1) =
        (345, 456, ZonedDateTime.now, "user", 567, "revision comment", "revision text", true, "sha1")

      val rev = Revision(Some(revId), Some(pageId), Some(parentId), Some(User(userId, user)), Some(timestamp), Some(comment), Some(text), sha1 = Some(sha1)/*, minor = Some(minor)*/)
      val page = Page(Some(pageId), ns, title, Seq(rev))

      val sw = new StringWriter()

      val writer = XmlWriter.create(sw)

      writer.write(Seq(page))

      val str = sw.toString

      val actualXml = XML.loadString(str)

      val revsXml = revisionXml(revId, parentId, timestamp, user, userId, comment, text, sha1)
      val expectXml = XML.loadString(mediawiki(pageXml(title, ns, pageId, revsXml)))

      trim(actualXml) === trim(expectXml)
    }
  }

}
