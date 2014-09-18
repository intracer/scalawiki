package client.wlx.dto

import client.dto.{Template, Page}


case class Image(pageId: Long, title: String,
                 url: String, pageUrl: String,
                 width: Int,
                 height: Int,
                 monumentId: Option[String],
                 author: Option[String]) extends Ordered[Image]{

  def compare(that: Image) =  (this.pageId - that.pageId).signum

//  def region: Option[String] = monumentId.map(_.split("-")(0))

}

object Image {

  def fromPageImageInfo(page: Page):Option[Image] = {
    for (imageInfo <- page.imageInfo.headOption)
    yield new Image(page.pageid, page.title, imageInfo.url, imageInfo.descriptionUrl, imageInfo.width, imageInfo.height, None, None)
  }

  def fromPageRevision(page: Page, monumentIdTemplate: String):Option[Image] = {
    page.revisions.headOption.map { revision =>

      val idRegex = """(\d\d)-(\d\d\d)-(\d\d\d\d)"""
      val id = Template.getDefaultParam(revision.content, monumentIdTemplate)
      val ipOpt = if (id.matches(idRegex)) Some(id) else None

      val authorValue = new Template(revision.content).getParam("author")

      val author = authorValue.split("\\|")(0).replace("[[User:", "")

      new Image(page.pageid, page.title, "", "", 0, 0, ipOpt, Some(author))
    }
  }


}
