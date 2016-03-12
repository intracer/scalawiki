package org.scalawiki.dto.cmd.query.prop

import org.scalawiki.dto.cmd._
import org.scalawiki.dto.cmd.query.Module

/**
 *  ?action=query&amp;prop=links
 *
 */
case class Links(override val params:LinksParam[Any]*)
  extends Module[PropArg]("pl", "links", "Gets a list of all links on the provided pages.")
  with PropArg with ArgWithParams[LinksParam[Any], PropArg]


/**
 * Marker trait for parameters used with prop=links
 */
trait LinksParam[+T] extends Parameter[T]

case class PlLimit(override val arg: String) extends StringParameter("pllimit",
  "How many links to return. Default: 10. No more than 500 (5000 for bots) allowed.") with LinksParam[String]

case class PlNamespace(override val args: Seq[Int]) extends IntListParameter("plnamespace",
  "Only include pages in these namespaces.") with LinksParam[Int]





