package client.wlx.dto

import client.dto.{Template, Page}


case class Image(pageId: Long, title: String,
                 url: String, pageUrl: String,
                 size: Int,
                 width: Int,
                 height: Int,
                 monumentId: Option[String] = None,
                 author: Option[String] = None,
                 uploader: Option[String] = None,
                 date: Option[String] = None) extends Ordered[Image]{

  def compare(that: Image) =  (this.pageId - that.pageId).signum

//  def region: Option[String] = monumentId.map(_.split("-")(0))

}

object Image {

  def fromPageImageInfo(page: Page, monumentIdTemplate: String, date: String):Option[Image] = {
    page.imageInfo.headOption.map{ ii =>
          Image(
            pageId = page.pageid,
            title = page.title,
            url = ii.url,
            pageUrl = ii.descriptionUrl,
            size = ii.size,
            width = ii.width,
            height = ii.height,
            monumentId = None,
            author = None,
            Some(ii.uploader),
            Some(date)
          )
    }
  }

  def fromPageRevision(page: Page, monumentIdTemplate: String, date: String):Option[Image] = {
    page.revisions.headOption.map { revision =>

      val idRegex = """(\d\d)-(\d\d\d)-(\d\d\d\d)"""
      val id = Template.getDefaultParam(revision.content, monumentIdTemplate)
      val ipOpt = if (id.matches(idRegex)) Some(id) else None

      val authorValue = new Template(revision.content).getParam("author")

      val author = authorValue.split("\\|")(0).replace("[[User:", "")

      new Image(page.pageid, page.title, "", "", 0, 0, 0, ipOpt, Some(author), None, Some(date))
    }
  }


}
