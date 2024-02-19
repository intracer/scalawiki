package org.scalawiki.dto.cmd.query.list

import org.scalawiki.dto.cmd._
import org.scalawiki.dto.cmd.query.Module

/** ?action=query&amp;prop=revisions
  */
case class EmbeddedIn(override val params: EiParam[Any]*)
    extends Module[ListArg](
      "ei",
      "embeddedin",
      "Find all pages that embed (transclude) the given page."
    )
    with ListArg
    with ArgWithParams[EiParam[Any], ListArg] {

  def this(
      title: Option[String],
      pageId: Option[Long],
      namespaces: Set[Int],
      limit: Option[String],
      params: EiParam[Any]*
  ) = {
    this(
      Seq(
        title.map(EiTitle).toSeq,
        pageId.map(EiPageId).toSeq,
        Seq(EiNamespace(namespaces.toSeq)),
        limit.map(EiLimit).toSeq
      ).flatten ++ params: _*
    )
  }

}

trait EiParam[+T] extends Parameter[T]

case class EiTitle(override val arg: String)
    extends StringParameter(
      "eititle",
      "Title to search. Cannot be used together with eipageid."
    )
    with EiParam[String]
case class EiPageId(override val arg: Long)
    extends IdParameter(
      "eipageid",
      "Page ID to search. Cannot be used together with eititle."
    )
    with EiParam[Long]

case class EiNamespace(override val args: Seq[Int])
    extends IntListParameter(
      "einamespace",
      "Only include pages in these namespaces."
    )
    with EiParam[Int]

case class EiLimit(override val arg: String)
    extends StringParameter("eilimit", "How many total pages to return.")
    with EiParam[String]

//eicontinue
//When more results are available, use this to continue.
//eidir
//The direction in which to list.
//eifilterredir
//How to filter for redirects.
//eilimit
//How many total pages to return.
