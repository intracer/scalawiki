package org.scalawiki.util

import akka.http.scaladsl.model.ContentType
import org.scalawiki.http.HttpClient

case class HttpStub(queryParam: Map[String, String],
                    response: String,
                    path: String = "/w/api.php",
                    contentType: ContentType = HttpClient.JSON_UTF8) {
  val query = queryParam + ("format" -> "json") + ("utf8" -> "")
}
