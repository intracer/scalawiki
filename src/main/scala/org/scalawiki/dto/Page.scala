package org.scalawiki.dto

import org.scalawiki.dto.history.History
import org.scalawiki.wlx.dto.Image

case class Page(
                 id: Option[Long],
                 ns: Int,
                 title: String,
                 revisions: Seq[Revision] = Seq.empty,
                 images: Seq[Image] = Seq.empty,
                 editToken: Option[String] = None,
                 missing: Boolean = false,
                 length: Option[Int] = None,
                 subjectId: Option[Long] = None,
                 talkId: Option[Long] = None,
                 langLinks: Map[String, String] = Map.empty,
                  categoryInfo: Option[CategoryInfo] = None
                 ) /*extends HasId[Page]*/ {
  val history = new History(this)

  def withText(text: String) = copy(revisions = Page.revisionsFromText(Some(text)))

  def text:Option[String] = revisions.headOption.flatMap(_.content)

  def isTalkPage = ns % 2 == 1

  def withId(id: Long): Page = copy(id = Some(id))

  def lastRevisionUser: Option[Contributor] = revisions.headOption.flatMap(_.user)

  def appendLists(other: Page) = copy(
    revisions = this.revisions ++ other.revisions,
    langLinks = this.langLinks ++ other.langLinks
  )
}

object Page {

 def full(
            id: Long,
            ns: Int,
            title: String,
            missing: Option[String],
            subjectId: Option[Long],
            talkId: Option[Long]) =
  {
    new Page(Some(id), ns, title,
      missing = missing.fold(false)(_ => true),
      subjectId = subjectId,
      talkId = talkId)
  }
  
  def noText(id: Long, ns: Int, title: String, missing: Option[String] = None) = new Page(Some(id), ns, title, missing = missing.fold(false)(_ => true))

  def withText(id: Long, ns: Int, title: String, text: Option[String]) = new Page(Some(id), ns, title, revisionsFromText(text))

  def withRevisionsText(id: Long, ns: Int, title: String, texts: Seq[String])
  = new Page(Some(id), ns, title, Revision.many(texts:_*))

  def withRevisions(id: Long, ns: Int, title: String, editToken: Option[String], revisions: Seq[Revision], missing: Option[String])
  = new Page(Some(id), ns, title, revisions, Seq.empty, editToken, missing.fold(false)(_ => true))

  def withImages(id: Long, ns: Int, title: String, images: Seq[Image])  = new Page(Some(id), ns, title, Seq.empty, images)

  def apply(title: String) = new Page(Some(0L), 0, title)

  def apply(id: Long) = new Page(Some(0L), 0, null)

  def apply(id: Long, ns: Int, title: String) = new Page(Some(id), ns, title)

  def withEditToken(id: Option[Long], ns: Int, title: String, editToken:Option[String]) = {
    new Page(id, ns, title, Seq.empty, Seq.empty, editToken)
  }

  def revisionsFromText(text: Option[String]) = text.fold(Seq.empty[Revision])(content => Revision.many(content))
}





