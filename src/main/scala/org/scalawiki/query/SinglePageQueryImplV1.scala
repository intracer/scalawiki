package org.scalawiki.query

import java.nio.file.{Files, Paths}

import org.scalawiki.MwBot
import org.scalawiki.dto.Page
import org.scalawiki.json.MwReads._

import scala.concurrent.Future


class SinglePageQueryImplV1(query: Either[Long, String], site: MwBot)
  extends PageQueryImplV1(query.fold(id => Left(Set(id)), title => Right(Set(title))), site)
  with SinglePageQuery {

  def whatTranscludesHere(namespaces: Set[Int] = Set.empty, continueParam: Option[(String, String)] = None): Future[Seq[Page]] = {
    queryList(namespaces, continueParam, "embeddedin", "ei")
  }

  def categoryMembers(namespaces: Set[Int] = Set.empty, continueParam: Option[(String, String)] = None): Future[Seq[Page]] = {
    queryList(namespaces, continueParam, "categorymembers", "cm")
  }
  def edit(text: String, summary: String, token: Option[String] = None, multi:Boolean = true) = {
    val fold: String = token.fold(site.token)(identity)
    val params = Map("action" -> "edit",
      "text" -> text,
      "summary" -> summary,
      "format" -> "json",
      "bot" -> "x",
      "token" -> fold) ++ toMap("pageid", "title")

    if (multi)
      site.postMultiPart(editResponseReads, params)
    else
      site.post(editResponseReads, params)
  }

  def upload(filename: String) = {
    val pagename = query.right.toOption.fold(filename)(identity)
    val token = site.token
    val fileContents = Files.readAllBytes(Paths.get(filename))
    val params = Map(
      "action" -> "upload",
      "filename" -> pagename,
      "token" -> token,
      "format" -> "json",
      "comment" -> "update",
      "filesize" -> fileContents.size.toString,
      "ignorewarnings" -> "true")
    site.postFile(editResponseReads, params, "file", filename)
  }

}
