package org.scalawiki

trait WithBot {

  def host: String

  def bot: MwBot = {
    MwBot.fromHost(host)
  }

}
