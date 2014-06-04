package client.dto

case class Page(pageid: Integer, ns: Integer, title: String, revisions: Seq[Revision] = Seq.empty, imageInfo: Seq[ImageInfo] = Seq.empty) {

  def withText(text: String) = copy(revisions = Page.revisionsFromText(Some(text)))

  def text = revisions.headOption.map(_.content)
}

object Page {
  def noText(pageid: Int, ns: Int, title: String) = new Page(pageid, ns, title)

  def withText(pageid: Int, ns: Int, title: String, text: Option[String]) = new Page(pageid, ns, title, revisionsFromText(text))

  def withRevisionsText(pageid: Int, ns: Int, title: String, texts: Seq[String])
  = new Page(pageid, ns, title, texts.map(text => new Revision(text)))

  def withRevisions(pageid: Int, ns: Int, title: String, revisions: Seq[Revision])  = new Page(pageid, ns, title, revisions)

  def withImageInfo(pageid: Int, ns: Int, title: String, imageInfo: Seq[ImageInfo])  = new Page(pageid, ns, title, Seq.empty, imageInfo)

  def apply(title: String) = new Page(null, null, title)

  def apply(id: Int) = new Page(id, null, null)

  def revisionsFromText(text: Option[String]) = text.fold(Seq.empty[Revision])(content => Seq(new Revision(content)))
}





