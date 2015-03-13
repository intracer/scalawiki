package org.scalawiki.query

import org.scalawiki.MwBot
import org.scalawiki.dto.Page
import org.scalawiki.dto.cmd.ActionParam
import org.scalawiki.dto.cmd.query.list.ListArgs
import org.scalawiki.dto.cmd.query.prop._
import org.scalawiki.dto.cmd.query.{Generator, PageIdsParam, Query, TitlesParam}

import scala.concurrent.Future

class PageQueryImplDsl(query: Either[Set[Page.Id], Set[String]], bot: MwBot) extends PageQuery {

  override def revisions(namespaces: Set[Page.Id], props: Set[String], continueParam: Option[(String, String)]): Future[Seq[Page]] = {

    val pagesQueryParam = query.fold(
      ids => PageIdsParam(ids.toSeq),
      titles => TitlesParam(titles.toSeq)
    )

    val action = ActionParam(Query(
      pagesQueryParam,
      PropParam(
        Info(),
        Revisions(
          RvProp(RvPropArgs.byNames(props.toSeq):_*),
          RvLimit("max")
        )
      )
    ))

    new DslQuery(action, bot).run
  }


  override def imageInfoByGenerator(
                                     generator: String,
                                     generatorPrefix: String,
                                     namespaces: Set[Int],
                                     props: Set[String],
                                     continueParam: Option[(String, String)],
                                     limit: String,
                                     titlePrefix: Option[String]): Future[Seq[Page]] = ???

  override def revisionsByGenerator(
                                     generator: String,
                                     generatorPrefix: String,
                                     namespaces: Set[Int],
                                     props: Set[String],
                                     continueParam: Option[(String, String)],
                                     limit: String,
                                     titlePrefix: Option[String]): Future[Seq[Page]] = {

    val pageId: Option[Page.Id] = query.left.toOption.map(_.head)
    val title: Option[String] = query.right.toOption.map(_.head)

    val action = ActionParam(Query(
      PropParam(
        Info(),
        Revisions(RvProp(RvPropArgs.byNames(props.toSeq):_*))
      ),
      Generator(ListArgs.toDsl(generator, title, pageId, Some(limit)))
    ))

    new DslQuery(action, bot).run
  }
}
