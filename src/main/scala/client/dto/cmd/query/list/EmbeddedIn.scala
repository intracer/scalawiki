package client.dto.cmd.query.list

import client.dto.cmd._
import client.dto.cmd.query.Module

/**
 *  ?action=query&amp;prop=revisions
 *
 */
case class EmbeddedIn(override val params: EiParam[Any]*)
  extends Module[ListArg]("ei", "embeddedin", "Find all pages that embed (transclude) the given page.")
  with ListArg
  with ArgWithParams[EiParam[Any], ListArg]

trait EiParam[+T] extends Parameter[T]

import client.dto.Page.Id

case class EiTitle(override val arg: String)
  extends StringParameter("eititle", "Title to search. Cannot be used together with eipageid.") with EiParam[String]
case class EiPageid(override val arg: Id)
  extends IntParameter("eipageid", "Page ID to search. Cannot be used together with eititle.") with EiParam[Int]
case class EiLimit(override val arg: String)
  extends StringParameter("eilimit", "How many total pages to return.") with EiParam[String]


//eititle
//Title to search. Cannot be used together with eipageid.
//eipageid
//Page ID to search. Cannot be used together with eititle.
//eicontinue
//When more results are available, use this to continue.
//einamespace
//The namespace to enumerate.
//eidir
//The direction in which to list.
//eifilterredir
//How to filter for redirects.
//eilimit
//How many total pages to return.
