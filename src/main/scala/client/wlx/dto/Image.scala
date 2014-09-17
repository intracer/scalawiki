package client.wlx.dto

import client.dto.Page


case class Image(pageId: Long, title: String,
                 url: String, pageUrl: String,
                 width: Int,
                 height: Int,
                 monumentId: Option[String]) extends Ordered[Image]{

  def compare(that: Image) =  (this.pageId - that.pageId).signum

//  def region: Option[String] = monumentId.map(_.split("-")(0))

}

object Image {

  def fromPage(page: Page):Option[Image] = {
    for (imageInfo <- page.imageInfo.headOption)
    yield new Image(page.pageid, page.title, imageInfo.url, imageInfo.descriptionUrl, imageInfo.width, imageInfo.height, None)
  }

}
