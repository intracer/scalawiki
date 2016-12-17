package org.scalawiki.query

import java.nio.file.{Files, Paths}

import org.scalawiki.MwBot
import org.scalawiki.dto.{Namespace, Page}
import org.scalawiki.dto.cmd._
import org.scalawiki.dto.cmd.edit._
import org.scalawiki.dto.cmd.query._
import org.scalawiki.dto.cmd.query.list._
import org.scalawiki.dto.cmd.query.prop._
import org.scalawiki.dto.cmd.query.prop.rvprop.RvProp
import org.scalawiki.json.MwReads._

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

    val action = Action(Query(
      Prop(
        Info(),
        Revisions(RvProp(RvPropArgs.byNames(props.toSeq): _*))
      ),
      Generator(ListArgs.toDsl(generator, title, pageId, namespaces, Some(limit)).get)
    ))

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

    val listArg = ListArgs.toDsl(generator, title, pageId, namespaces, Some(limit))

    val generatorArg = listArg.getOrElse(new Images(ImLimit(limit)))
    val titlesParam = listArg match {
      case None => title.map(t => TitlesParam(Seq(t)))
      case _ => None
    }

    val queryParams = titlesParam.toSeq ++ Seq(
      Prop(
        ImageInfo(
          IiProp(IiPropArgs.byNames(props.toSeq): _*)
        )
      ),
      Generator(generatorArg)
    )

    val action = Action(Query(queryParams:_*))

    bot.run(action, context)
  }

  override def edit(text: String, summary: Option[String] = None, section: Option[String] = None, token: Option[String] = None, multi: Boolean = true) = {

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
        "bot" -> "x",
        "assert" -> "user",
        "assert" -> "bot") ++ section.map(s => "section" -> s).toSeq ++ summary.map(s => "summary" -> s).toSeq

    bot.log.info(s"${bot.host} edit page: $page, summary: $summary")

    if (multi)
      bot.postMultiPart(editResponseReads, params)
    else
      bot.post(editResponseReads, params)
  }

  override def upload(filename: String,
                      text: Option[String] = None,
                      comment: Option[String] = None,
                      ignoreWarnings: Boolean = false): Future[String] = {
    val page = query.right.toOption.fold(filename)(_.head)
    val token = bot.token
    val fileContents = Files.readAllBytes(Paths.get(filename))
    val params = Map(
      "action" -> "upload",
      "filename" -> page,
      "token" -> token,
      "format" -> "json",
      "comment" -> "update",
      "filesize" -> fileContents.size.toString,
      "assert" -> "user",
      "assert" -> "bot") ++
      text.map("text" -> _) ++
      comment.map("comment" -> _) ++
      (if (ignoreWarnings) Seq("ignorewarnings" -> "true") else Seq.empty)

    bot.postFile(uploadResponseReads, params, "file", filename)
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
