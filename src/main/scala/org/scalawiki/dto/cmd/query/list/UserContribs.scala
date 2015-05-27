package org.scalawiki.dto.cmd.query.list

import org.joda.time.DateTime
import org.scalawiki.dto.cmd.query.Module
import org.scalawiki.dto.cmd._

case class UserContribs(override val params: UcParam[Any]*)
  extends Module[ListArg]("uc", "usercontribs",
    "Gets a list of contributions made by a given user, ordered by modification time.")
  with ListArg
  with ArgWithParams[UcParam[Any], ListArg] {
}

trait UcParam[+T] extends Parameter[T]

case class UcStart(override val arg: DateTime) extends DateTimeParameter("ucstart",
  "The timestamp to start listing from.") with UcParam[DateTime]

case class UcEnd(override val arg: DateTime) extends DateTimeParameter("ucend",
  "The timestamp to end listing at.") with UcParam[DateTime]

case class UcUser(override val args: Seq[String]) extends StringListParameter("ucuser",
  "Users to retrieve contributions for.") with UcParam[String]

case class UcUserPrefix(override val arg: String) extends StringParameter("ucuserprefix",
  "List contributions of all users whose name starts with this string.") with UcParam[String]

// TODO enum
case class UcDir(override val arg: String) extends StringParameter("audir",
  "Direction to sort in.") with UcParam[String]

case class UcLimit(override val arg: String) extends StringParameter("uclimit",
  "Maximum amount of contributions to list.") with UcParam[String]

case class UcNamespace(override val args: Seq[Int]) extends IntListParameter("ucnamespace",
  "Only list contributions in these namespaces.") with UcParam[Int]

// TODO enum
case class UcProp(override val args: Seq[String]) extends StringListParameter("ucprop",
  "Which properties to get.") with UcParam[String]

case class UcShow(override val args: Seq[String]) extends StringListParameter("ucshow",
  "Only list contributions that meet these criteria.") with UcParam[String]

case class UcTag(override val args: Seq[String]) extends StringListParameter("uctag",
  "Only list revisions tagged with this tag.") with UcParam[String]