package org.scalawiki.dto.cmd.query.list

import org.scalawiki.dto.cmd.query.Module
import org.scalawiki.dto.cmd._

case class AllUsers(override val params: AuParam[Any]*)
  extends Module[ListArg]("au", "allusers", "Enumerate all registered users, ordered by username.")
  with ListArg
  with ArgWithParams[AuParam[Any], ListArg] {

}

trait AuParam[+T] extends Parameter[T]

case class AuFrom(override val arg: String) extends StringParameter("aufrom",
  "The user name to start enumerating from.") with AuParam[String]

case class AuTo(override val arg: String) extends StringParameter("auto",
  "The user name to stop enumerating at.") with AuParam[String]

// TODO enum
case class AuDir(override val arg: String) extends StringParameter("audir",
  "Direction to sort in.") with AuParam[String]

case class AuPrefix(override val arg: String) extends StringParameter("auprefix",
  "Search for all users that begin with this value.") with AuParam[String]

case class AuGroup(override val args: Seq[String]) extends StringListParameter("augroup",
  "Limit users to given group name(s). Possible values: bot, sysop, bureaucrat (+ any other group that is defined on the wiki)")
with AuParam[String]

case class AuExcludeGroup(override val args: Seq[String]) extends StringListParameter("auexcludegroup",
  "Exclude users in given group name (s).Can not be used together with 'group '.") with AuParam[String]

case class AuRights(override val args: Seq[String]) extends StringListParameter("aurights",
  "Limit users to given right (s)") with AuParam[String]

// TODO enum
//blockinfo: Adds the information about a current block on the user
//groups: Lists groups that the user is in.This uses more server resources and may return fewer results than the limit
//implicitgroups: Lists all the groups the user is automatically in
//rights: Lists rights that the user has
//editcount: Adds the edit count of the user
//registration: Adds the timestamp of when the user registered if available (may be blank) 1.12 +
case class AuProp(override val args: Seq[String]) extends StringListParameter("auprop",
  "What pieces of information to include.") with AuParam[String]

case class AuLimit(override val arg: String) extends StringParameter("aulimit",
  "How many total user names to return.") with AuParam[String]

case class AuWithEditsOnly(override val arg: Boolean) extends BooleanParameter("auwitheditsonly",
  "auwitheditsonly.") with AuParam[Boolean]

case class AuActiveUsers(override val arg: Boolean) extends BooleanParameter("auactiveusers",
  "Only list users active in the last 30 days.") with AuParam[Boolean]