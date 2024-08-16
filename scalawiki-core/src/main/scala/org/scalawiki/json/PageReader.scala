package org.scalawiki.json

import org.scalawiki.dto.Page
import upickle.default._
trait PageReader {

  implicit val pageReader: Reader[Page] = reader[ujson.Value].map { json =>
    val obj = json.obj
    Page.full(
      id = obj.get("pageid").map(_.num.toLong),
      ns = obj.get("ns").map(_.num.toInt),
      title = obj.get("title").str,
      missing = obj.contains("missing"),
      subjectId = obj.get("subjectid").map(_.num.toLong),
      talkId = obj.get("talkid").map(_.num.toLong),
      invalidReason = obj.get("invalidreason").map(_.str)
    )
  }

}
