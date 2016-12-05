package org.scalawiki

trait WithBot {

  def host: String

  implicit def bot: MwBot = {
    MwBot.fromHost(host)
  }

}
