package client

trait WithBot {

  def host: String

  lazy val bot: MwBot = createBot()

  private def createBot() = {
    MwBot.get(host)
  }

}
