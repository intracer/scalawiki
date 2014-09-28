package client.dto

case class Page(
                 pageid: java.lang.Long,
                 ns: Integer,
                 title: String,
                 revisions: Seq[Revision] = Seq.empty,
                 imageInfo: Seq[ImageInfo] = Seq.empty,
                 edittoken: Option[String] = None,
                 missing: Boolean = false) {

  def withText(text: String) = copy(revisions = Page.revisionsFromText(Some(text)))

  def text = revisions.headOption.map(_.content)
}

object Page {
  def noText(pageid: Int, ns: Int, title: String, missing: Option[String]) = new Page(pageid, ns, title, missing = missing.fold(false)(_ => true))

  def withText(pageid: Int, ns: Int, title: String, text: Option[String]) = new Page(pageid, ns, title, revisionsFromText(text))

  def withRevisionsText(pageid: Int, ns: Int, title: String, texts: Seq[String])
  = new Page(pageid, ns, title, texts.map(text => new Revision(text)))

  def withRevisions(pageid: Int, ns: Int, title: String, editToken: Option[String], revisions: Seq[Revision], missing: Option[String])
  = new Page(pageid, ns, title, revisions, Seq.empty, editToken, missing.fold(false)(_ => true))

  def withImageInfo(pageid: Int, ns: Int, title: String, imageInfo: Seq[ImageInfo])  = new Page(pageid, ns, title, Seq.empty, imageInfo)

  def apply(title: String) = new Page(null, null, title)

  def apply(id: Int) = new Page(id, null, null)

  def withEditToken(id: Option[Int], ns: Int, title: String, editToken:Option[String]) = new Page(id.getOrElse(0).toLong, ns, title, Seq.empty, Seq.empty, editToken)

  def revisionsFromText(text: Option[String]) = text.fold(Seq.empty[Revision])(content => Seq(new Revision(content)))
}





