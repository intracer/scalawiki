package org.scalawiki

import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.email._

trait ActionLibrary {

  def bot: MwBot

  def message(user: String, section: String, text: String) = {
    bot.page("User_talk:" + user).edit(text,
      section = Some("new"),
      summary = Some(section)
    )
  }

  def email(user: String, subject: String, text: String) = {
    val cmd = Action(EmailUser(
      Target(user),
      Subject(subject),
      Text(text),
      Token(bot.token)
    ))
    bot.await(bot.post(cmd.pairs.toMap))
  }

}
