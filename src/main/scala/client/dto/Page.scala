package client.dto

case class Page(
                 pageId: java.lang.Long,
                 ns: Integer,
                 title: String,
                 revisions: Seq[Revision] = Seq.empty,
                 imageInfo: Seq[ImageInfo] = Seq.empty,
                 edittoken: Option[String] = None,
                 missing: Boolean = false) {

  def withText(text: String) = copy(revisions = Page.revisionsFromText(Some(text)))

  def text = revisions.headOption.map(_.content)

  def isTalkPage = ns % 2 == 1
}

object Page {
  def noText(pageId: Int, ns: Int, title: String, missing: Option[String]) = new Page(pageId, ns, title, missing = missing.fold(false)(_ => true))

  def withText(pageId: Int, ns: Int, title: String, text: Option[String]) = new Page(pageId, ns, title, revisionsFromText(text))

  def withRevisionsText(pageId: Int, ns: Int, title: String, texts: Seq[String])
  = new Page(pageId, ns, title, texts.map(text => new Revision(text)))

  def withRevisions(pageId: Int, ns: Int, title: String, editToken: Option[String], revisions: Seq[Revision], missing: Option[String])
  = new Page(pageId, ns, title, revisions, Seq.empty, editToken, missing.fold(false)(_ => true))

  def withImageInfo(pageId: Int, ns: Int, title: String, imageInfo: Seq[ImageInfo])  = new Page(pageId, ns, title, Seq.empty, imageInfo)

  def apply(title: String) = new Page(null, null, title)

  def apply(id: Int) = new Page(id, null, null)

  def withEditToken(id: Option[Int], ns: Int, title: String, editToken:Option[String]) = new Page(id.getOrElse(0).toLong, ns, title, Seq.empty, Seq.empty, editToken)

  def revisionsFromText(text: Option[String]) = text.fold(Seq.empty[Revision])(content => Seq(new Revision(content)))
}





