package org.scalawiki

// TODO better names
trait HasBot {
  def bot: MwBot
}

trait WithBot extends HasBot {

  def host: String

  implicit def bot: MwBot = {
    MwBot.fromHost(host)
  }

}
