package client.wlx

import client.{LoginInfo, MwBot}

trait WithBot {

  def host: String

  lazy val bot: MwBot = createBot()

  private def createBot() = {
    val bot = MwBot.create(host)
    bot.await(bot.login(LoginInfo.login, LoginInfo.password))
    bot
  }

}
