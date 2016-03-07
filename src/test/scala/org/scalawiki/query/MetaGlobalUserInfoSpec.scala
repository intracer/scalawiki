package org.scalawiki.query

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
      val users = result.flatMap(_.lastRevisionUser)
      users must have size 1
      val user = users.head

      user.name === Some("Ilya")
    }
  }


}
