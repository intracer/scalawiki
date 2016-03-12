package org.scalawiki.dto

case class ImageInfo(timestamp: String,
                     uploader: String,
                     size: Long,
                     width: Int,
                     height: Int,
                     url: String,
                     descriptionUrl: String,
                     author: Option[String] = None) {

  def pixels = width * height

}


object ImageInfo {
  def basic(timestamp: String,
            uploader: String,
            size: Long,
            width: Int,
            height: Int,
            url: String,
            descriptionUrl: String)
  = new ImageInfo(timestamp, uploader, size, width, height, url, descriptionUrl)
}