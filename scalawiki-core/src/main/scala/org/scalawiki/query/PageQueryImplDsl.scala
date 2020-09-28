package org.scalawiki.query

import java.nio.file.{Files, Paths}

import org.scalawiki.MwBot
import org.scalawiki.dto.cmd._
import org.scalawiki.dto.cmd.edit._
import org.scalawiki.dto.cmd.query._
import org.scalawiki.dto.cmd.query.list._
import org.scalawiki.dto.cmd.query.prop._
import org.scalawiki.dto.cmd.query.prop.rvprop.RvProp
import org.scalawiki.dto.{Namespace, Page}
import org.scalawiki.json.MwReads._
import retry.Success

import scala.concurrent.Future

class PageQueryImplDsl(query: Either[Set[Long], Set[String]],
                       bot: MwBot,
                       context: Map[String, String] = Map.empty) extends PageQuery with SinglePageQuery {

  override def withContext(context: Map[String, String]) =
    new PageQueryImplDsl(query, bot, context)

  override def revisions(namespaces: Set[Int], props: Set[String], continueParam: Option[(String, String)]): Future[Seq[Page]] = {

    import org.scalawiki.dto.cmd.query.prop.rvprop._

    val pages = query.fold(
      ids => PageIdsParam(ids.toSeq),
      titles => TitlesParam(titles.toSeq)
    )

    val action = Action(Query(
      pages,
      Prop(
        Info(),
        Revisions(
          RvProp(RvPropArgs.byNames(props.toSeq): _*),
          RvLimit("max")
        )
      )
    ))

    bot.run(action, context)
  }

  override def revisionsByGenerator(
                                     generator: String,
                                     generatorPrefix: String,
                                     namespaces: Set[Int],
                                     props: Set[String],
                                     continueParam: Option[(String, String)],
                                     limit: String,
                                     titlePrefix: Option[String]): Future[Seq[Page]] = {

    val pageId: Option[Long] = query.left.toOption.map(_.head)
    val title: Option[String] = query.right.toOption.map(_.head)

    val generatorArg = ListArgs.toDsl(generator, title, pageId, namespaces, Some(limit))
    val queryParams = pagesParam(pageId, title, generatorArg) ++ Seq(
      Prop(
        Info(),
        Revisions(RvProp(RvPropArgs.byNames(props.toSeq): _*))
      ),
      Generator(ListArgs.toDsl(generator, title, pageId, namespaces, Some(limit)).get)
    )

    val action = Action(Query(queryParams:_*))
    bot.run(action, context)
  }

  override def imageInfoByGenerator(
                                     generator: String,
                                     generatorPrefix: String,
                                     namespaces: Set[Int],
                                     props: Set[String],
                                     continueParam: Option[(String, String)],
                                     limit: String,
                                     titlePrefix: Option[String]): Future[Seq[Page]] = {
    import org.scalawiki.dto.cmd.query.prop.iiprop._

    val pageId: Option[Long] = query.left.toOption.map(_.head)
    val title: Option[String] = query.right.toOption.map(_.head)

    val generatorArg = ListArgs.toDsl(generator, title, pageId, namespaces, Some(limit))
    val queryParams = pagesParam(pageId, title, generatorArg) ++ Seq(
      Prop(
        ImageInfo(
          IiProp(IiPropArgs.byNames(props.toSeq): _*)
        )
      ),
      Generator(generatorArg.get)
    )

    val action = Action(Query(queryParams:_*))
    bot.run(action, context)
  }

  private def pagesParam(pageId: Option[Long], title: Option[String], generatorArg: Option[GeneratorArg]) = {
    val pagesInGenerator = generatorArg.exists(_.pairs.map(_._1).exists(p => p.endsWith("title") || p.endsWith("pageid")))
    if (pagesInGenerator) Seq.empty[QueryParam[String]] else {
      title.map(t => TitlesParam(Seq(t))).toSeq ++ pageId.map(id => PageIdsParam(Seq(id))).toSeq
    }
  }

  override def edit(text: String, summary: Option[String] = None, section: Option[String] = None, token: Option[String] = None, multi: Boolean = false) = {

    val page = query.fold(
      ids => PageId(ids.head),
      titles => Title(titles.head)
    )

    val action = Action(Edit(
      page,
      Text(text),
      Token(token.fold(bot.token)(identity))
    ))

    val params = action.pairs.toMap ++
      Map("action" -> "edit",
        "format" -> "json",
        "utf8" -> "",
        "bot" -> "x",
        "assert" -> "user",
        "assert" -> "bot") ++ section.map(s => "section" -> s).toSeq ++ summary.map(s => "summary" -> s).toSeq


    import scala.concurrent.ExecutionContext.Implicits.global
    def performEdit(): Future[String] = {
      bot.log.info(s"Request ${bot.host} edit page: $page, summary: $summary")
      if (multi)
        bot.postMultiPart(editResponseReads, params)
      else
        bot.post(editResponseReads, params)
    }.map { s =>
      bot.log.info(s"Response ${bot.host} edit page: $page: $s")
      s
    }

    implicit def stringSuccess: Success[String] = Success(_ == "Success")
    retry.Backoff()(odelay.Timer.default)(() => performEdit())
  }

  override def uploadFromFile(filename: String,
                              text: Option[String] = None,
                              comment: Option[String] = None,
                              ignoreWarnings: Boolean = false): Future[String] = {
    val fileContents = Files.readAllBytes(Paths.get(filename))
    upload(filename, fileContents,text, comment, ignoreWarnings)
  }

  override def upload(title: String, fileContents: Array[Byte],
                      text: Option[String] = None,
                      comment: Option[String] = None,
                      ignoreWarnings: Boolean = false): Future[String] = {
    val page = query.right.toOption.fold(title)(_.head)
    val token = bot.token
    val params = Map(
      "action" -> "upload",
      "filename" -> page,
      "token" -> token,
      "format" -> "json",
      "comment" -> comment.getOrElse("update"),
      "filesize" -> fileContents.length.toString,
      "assert" -> "user",
      "assert" -> "bot") ++
      text.map("text" -> _) ++
      (if (ignoreWarnings) Seq("ignorewarnings" -> "true") else Seq.empty)

    bot.postFile(uploadResponseReads, params, "file", title)
  }

  override def whatTranscludesHere(namespaces: Set[Int], continueParam: Option[(String, String)]): Future[Seq[Page]] = {
    val pages = query.fold(
      ids => EiPageId(ids.head),
      titles => EiTitle(titles.head)
    )

    val action = Action(Query(
      ListParam(
        EmbeddedIn(
          pages,
          EiLimit("max"),
          EiNamespace(namespaces.toSeq)
        )
      )
    ))

    bot.run(action, context)
  }

  override def categoryMembers(namespaces: Set[Int], continueParam: Option[(String, String)]): Future[Seq[Page]] = {
    val pages = query.fold(
      ids => CmPageId(ids.head),
      titles => CmTitle(titles.head)
    )

    val cmTypes = namespaces.filter(_ == Namespace.CATEGORY).map(_ => CmTypeSubCat) ++
      namespaces.filter(_ == Namespace.FILE).map(_ => CmTypeFile)

    val cmParams = Seq(pages,
      CmLimit("max"),
      CmNamespace(namespaces.toSeq)
    ) ++ (if (cmTypes.nonEmpty) Seq(CmType(cmTypes.toSeq: _*)) else Seq.empty)

    val action = Action(Query(
      ListParam(
        CategoryMembers(cmParams: _*)
      )
    ))

    bot.run(action, context)
  }
}
