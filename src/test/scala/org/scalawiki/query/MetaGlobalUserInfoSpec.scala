package org.scalawiki.query

import org.scalawiki.dto.User
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.dto.cmd.query.meta._
import org.scalawiki.util.{Command, MockBotSpec}
import org.specs2.mutable.Specification
import spray.util.pimpFuture

import scala.io.Source

class MetaGlobalUserInfoSpec extends Specification with MockBotSpec {

  "globaluserinfo" should {
    "return properties" in {
      val is = getClass.getResourceAsStream("/org/scalawiki/query/globaluserinfo.json")
      is !== null
      val response = Source.fromInputStream(is).mkString

      val action = Action(Query(MetaParam(
        GlobalUserInfo(
          GuiProp(
            Merged, Unattached, EditCount
          ),
          GuiUser("Ilya")
        ))))

      val commands = Seq(new Command(
        Map("action" -> "query", "meta" -> "globaluserinfo",
          "guiuser" -> "Ilya", "guiprop" -> "merged|unattached|editcount", "continue" -> ""),
        response))

      val bot = getBot(commands: _*)

      val result = bot.run(action).await
      result must have size 1
      val users = result.flatMap(_.lastRevisionUser.map(_.asInstanceOf[User]))
      users must have size 1

      val user = users.head
      user.name === Some("Ilya")
      user.id === Some(527)
      user.editCount === Some(50208)
      // user.registration === Some(new DateTime("2008-03-25T17:19:03Z"))

      val accounts = user.sulAccounts

      accounts must have size 4

      accounts.map(a => (a.wiki, a.url, a.editCount)) === Seq(
        ("enwiki", "https://en.wikipedia.org", 1350),
        ("commonswiki", "https://commons.wikimedia.org", 14598),
        ("ukwiki", "https://uk.wikipedia.org", 29290),
        ("ukwiktionary", "https://uk.wiktionary.org", 1190)
      )
    }
  }

}
