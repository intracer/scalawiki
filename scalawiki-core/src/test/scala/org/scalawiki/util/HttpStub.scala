package org.scalawiki.util

import org.scalawiki.http.HttpClient
import spray.http.ContentType

class HttpStub(
               queryParam: Map[String, String],
               val response: String,
               val path: String = "/w/api.php",
               val contentType: ContentType = HttpClient.JSON_UTF8
             ) {
   val query = queryParam + ("format" -> "json")
 }
