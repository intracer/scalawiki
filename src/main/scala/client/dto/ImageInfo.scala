package client.dto

case class ImageInfo(timestamp: String,
                      user: String,
                      size: Int,
                      width: Int,
                      height: Int,
                      url: String,
                      descriptionUrl: String) {

  def pixels  = width * height


}
