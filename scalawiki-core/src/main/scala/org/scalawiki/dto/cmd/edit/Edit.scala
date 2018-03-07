package org.scalawiki.dto.cmd.edit

import java.time.ZonedDateTime

import org.scalawiki.dto.cmd._

case class Edit(override val params: EditParam[Any]*)
  extends  EnumArgument[ActionArg]("edit", "Edit and create pages.")
  with ActionArg
  with ArgWithParams[EditParam[Any], ActionArg] {

  def watch(params: WatchParam[Any]*) = ???

  }

trait EditParam[+T] extends Parameter[T]

case class Title(override val arg: String) extends StringParameter("title",
  "Title of the page you want to edit. Cannot be used together with pageid.") with EditParam[String]

case class PageId(override val arg: Long) extends IdParameter("pageid",
  "Page ID of the page you want to edit. Cannot be used together with title") with EditParam[Long]

case class Section(override val arg: String) extends StringParameter("section",
  "Section number. 0 for the top section, 'new' for a new section. Omit to act on the entire page") with EditParam[String]

case class SectionTitle(override val arg: String) extends StringParameter("sectiontitle",
  "Title to use if creating a new section. If not specified, summary will be used instead") with EditParam[String]

case class Text(override val arg: String) extends StringParameter("text",
  "New page (or section) content") with EditParam[String]

case class Token(override val arg: String) extends StringParameter("token", "Edit token") with EditParam[String]

case class Summary(override val arg: String) extends StringParameter("summary",
  "Edit summary. Also section title when section=new and sectiontitle is not set") with EditParam[String]

case class Minor(override val arg: Boolean = true) extends BooleanParameter("minor",
  "If set, mark the edit as minor") with EditParam[Boolean]

case class NotMinor(override val arg: Boolean = true) extends BooleanParameter("notminor",
  "If set, don't mark the edit as minor, even if you have the 'Mark all my edits minor by default' preference enabled") with EditParam[Boolean]

case class Bot(override val arg: Boolean = true) extends BooleanParameter("bot",
  "If set, mark the edit as bot; even if you are using a bot account the edits will not be marked unless you set this flag") with EditParam[Boolean]

case class BaseTimestamp(override val arg: ZonedDateTime) extends DateTimeParameter("basetimestamp",
    "Timestamp of the base revision (obtained through prop=revisions&rvprop=timestamp). " +
      "Used to detect edit conflicts; leave unset to ignore conflicts") with EditParam[ZonedDateTime]

case class StartTimestamp(override val arg: ZonedDateTime) extends DateTimeParameter("starttimestamp",
  "Timestamp when you started editing the page (e.g., when you fetched the current revision's text to begin editing it or checked the (non-)existence of the page). " +
    "Used to detect if the page has been deleted since you started editing; leave unset to ignore conflicts") with EditParam[ZonedDateTime]

case class Recreate(override val arg: Boolean = true) extends BooleanParameter("recreate",
  "Override any errors about the article having been deleted in the meantime") with EditParam[Boolean]

case class CreateOnly(override val arg: Boolean = true) extends BooleanParameter("createonly",
  "Don't edit the page if it exists already") with EditParam[Boolean]

case class NoCreate(override val arg: Boolean = true) extends BooleanParameter("nocreate",
  "Throw an error if the page doesn't exist") with EditParam[Boolean]

case class Md5(override val arg: String) extends StringParameter("md5",
  "MD5 hash (hex) of the text parameter or the prependtext and appendtext parameters concatenated. " +
    "If this parameter is set and the hashes don't match, the edit is rejected.") with EditParam[String]

case class PrependText(override val arg: String) extends StringParameter("prependtext",
  "Add this text to the beginning of the page. Overrides text") with EditParam[String]

case class AppendText(override val arg: String) extends StringParameter("appendtext",
  "Add this text to the end of the page. Overrides text. Use section=new to append a new section") with EditParam[String]

case class Undo(override val arg: Long) extends IdParameter("appendtext",
  "Revision ID to undo. Overrides text, prependtext and appendtext") with EditParam[Long]

case class UndoAfter(override val arg: Long) extends IdParameter("undoafter",
  "Undo all revisions from undo up to but not including this one. If not set, just undo one revision") with EditParam[Long]

case class Redirect(override val arg: Boolean = true) extends BooleanParameter("redirect",
  "Automatically resolve redirects") with EditParam[Boolean]

