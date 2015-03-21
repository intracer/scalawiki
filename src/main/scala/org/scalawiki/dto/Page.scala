package org.scalawiki.dto

import org.scalawiki.dto.Page.Id
import org.scalawiki.dto.history.History

case class Page(
                 id: Id,
                 ns: Integer,
                 title: String,
                 revisions: Seq[Revision] = Seq.empty,
                 imageInfo: Seq[ImageInfo] = Seq.empty,
                 editToken: Option[String] = None,
                 missing: Boolean = false,
                 length: Option[Int] = None,
                 subjectId: Option[Id] = None,
                 talkId: Option[Id] = None
                 ) {
  val history = new History(this)

  def withText(text: String) = copy(revisions = Page.revisionsFromText(Some(text)))

  def text:Option[String] = revisions.headOption.flatMap(_.content)

  def isTalkPage = ns % 2 == 1


}

object Page {
  type Id = Long

  def full(
            id: Id,
            ns: Int,
            title: String,
            missing: Option[String],
            subjectId: Option[Id],
            talkId: Option[Id]) =
  {
    new Page(id, ns, title,
      missing = missing.fold(false)(_ => true),
      subjectId = subjectId,
      talkId = talkId)
  }
  
  def noText(id: Id, ns: Int, title: String, missing: Option[String]) = new Page(id, ns, title, missing = missing.fold(false)(_ => true))

  def withText(id: Id, ns: Int, title: String, text: Option[String]) = new Page(id, ns, title, revisionsFromText(text))

  def withRevisionsText(id: Id, ns: Int, title: String, texts: Seq[String])
  = new Page(id, ns, title, Revision.create(texts:_*))

  def withRevisions(id: Id, ns: Int, title: String, editToken: Option[String], revisions: Seq[Revision], missing: Option[String])
  = new Page(id, ns, title, revisions, Seq.empty, editToken, missing.fold(false)(_ => true))

  def withImageInfo(id: Id, ns: Int, title: String, imageInfo: Seq[ImageInfo])  = new Page(id, ns, title, Seq.empty, imageInfo)

  def apply(title: String) = new Page(0, null, title)

  def apply(id: Id) = new Page(0, null, null)

  def withEditToken(id: Option[Id], ns: Int, title: String, editToken:Option[String]) = new Page(id.getOrElse(0), ns, title, Seq.empty, Seq.empty, editToken)

  def revisionsFromText(text: Option[String]) = text.fold(Seq.empty[Revision])(content => Revision.create(content))
}





