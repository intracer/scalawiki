package org.scalawiki.dto.cmd.query.prop

import org.scalawiki.dto.cmd._
import org.scalawiki.dto.cmd.query.Module

/** ?action=query&amp;prop=categories
  */
case class Categories(override val params: CategoriesParam[Any]*)
    extends Module[PropArg](
      "cl",
      "categories",
      "Gets a list of all categories used on the provided pages."
    )
    with PropArg
    with ArgWithParams[CategoriesParam[Any], PropArg]

/** Marker trait for parameters used with prop=langlinks
  */
trait CategoriesParam[+T] extends Parameter[T]

case class ClLimit(override val arg: String)
    extends StringParameter("cllimit", "How many categories to return")
    with CategoriesParam[String]
