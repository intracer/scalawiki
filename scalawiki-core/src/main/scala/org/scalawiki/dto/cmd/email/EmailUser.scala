package org.scalawiki.dto.cmd.email

import org.scalawiki.dto.cmd._

case class EmailUser(override val params: EmailParam[Any]*)
  extends  EnumArgument[ActionArg]("emailuser", "Email a user.")
  with ActionArg
  with ArgWithParams[EmailParam[Any], ActionArg]

trait EmailParam[+T] extends Parameter[T]

case class Target(override val arg: String) extends StringParameter("target",  "User to send email to.") with EmailParam[String]

case class Subject(override val arg: String) extends StringParameter("subject",  "The subject of the message.") with EmailParam[String]

case class Text(override val arg: String) extends StringParameter("text",  "The message.") with EmailParam[String]

case class Token(override val arg: String) extends StringParameter("token",  "The subject of the message.") with EmailParam[String]

case class CcMe(override val arg: Boolean) extends BooleanParameter("ccme",  "If set, a copy of the email will be sent to you.") with EmailParam[Boolean]

