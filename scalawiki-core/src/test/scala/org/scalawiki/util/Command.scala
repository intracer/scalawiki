package org.scalawiki.util

import akka.http.scaladsl.model.ContentType
import org.scalawiki.http.HttpClient

class Command(
               queryParam: Map[String, String],
               val response: String,
               val path: String = "/w/api.php",
               val contentType: ContentType = HttpClient.JSON_UTF8
             ) {
   val query = queryParam + ("format" -> "json")
 }
