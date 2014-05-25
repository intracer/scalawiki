package client.util

class Command(queryParam: Map[String, String], val response: String, val path: String = "/w/api.php") {
   val query = queryParam + ("format" -> "json")
 }
