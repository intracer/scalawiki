package org.scalawiki.query

import java.util.concurrent.TimeUnit

import org.scalawiki.Timestamp
import org.scalawiki.copyvio.CopyVio._
import org.scalawiki.dto.User
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.dto.cmd.query.list.{AllUsers, AuProp, ListParam}
import org.scalawiki.util.{Command, MockBotSpec}
import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ListAllUsersSpec extends Specification with MockBotSpec {

  "get all users" should {
    "return users without continue" in {
      val queryType = "allusers"

      val response1 =
        """{ "query": {
          |        "allusers": [
          |            {
          |                "userid": 146308,
          |                "name": "!"
          |            },
          |            {
          |                "userid": 480659,
          |                "name": "! !"
          |            }
          |         ]
          |    }
          |}""".stripMargin

      val commands = Seq(
        new Command(Map("action" -> "query", "list" -> queryType, "continue" -> ""), response1)
      )

      val bot = getBot(commands: _*)

      val action =
        Action(
          Query(
            ListParam(
              AllUsers()
            )
          )
        )

      val future = bot.run(action)

      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 2
      val users = result.flatMap(_.lastRevisionUser)
      users(0) === User(146308, "!")
      users(1) === User(480659, "! !")
    }

    "return users with editcount, registration and blockinfo" in {
      val queryType = "allusers"

      val response1 =
        """{ "query": {
          |        "allusers": [
          |             {
          |                "userid": 3634417,
          |                "name": "Y",
          |                "editcount": 13892,
          |                "registration": "2007-02-22T03:19:08Z",
          |                "blockid": 278807,
          |                "blockedby": "Gurch",
          |                "blockedbyid": 241822,
          |                "blockedtimestamp": "2006-10-17T06:53:13Z",
          |                "blockreason": "username",
          |                "blockexpiry": "infinity"
          |            },
          |            {
          |                "userid": 53928,
          |                "name": "Y (usurped)",
          |                "editcount": 0,
          |                "registration": ""
          |            }
          |         ]
          |    }
          |}""".stripMargin

      val commands = Seq(
        new Command(Map("action" -> "query", "list" -> queryType, "auprop" -> "registration|editcount|blockinfo", "continue" -> ""), response1)
      )

      val bot = getBot(commands: _*)

      val action =
        Action(
          Query(
            ListParam(
              AllUsers(AuProp(Seq("registration", "editcount", "blockinfo")))
            )
          )
        )

      val future = bot.run(action)

      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 2
      val users = result.flatMap(_.lastRevisionUser)
      users(0) === new User(Some(3634417), Some("Y"), Some(13892), Some(Timestamp.parse("2007-02-22T03:19:08Z")), Some(true))
      users(1) === new User(Some(53928), Some("Y (usurped)"), Some(0), None, Some(false))
    }

    "return users with continue" in {
      val queryType = "allusers"

      val response1 =
        """{  "continue": {
          |        "aufrom": "! ! !",
          |        "continue": "-||"
          |    },
          |    "query": {
          |        "allusers": [
          |            {
          |                "userid": 146308,
          |                "name": "!"
          |            },
          |            {
          |                "userid": 480659,
          |                "name": "! !"
          |            }
          |         ]
          |    }
          |}""".stripMargin

      val response2 =
        """{ "query": {
          |        "allusers": [
          |             {
          |                "userid": 505506,
          |                "name": "! ! !"
          |            },
          |            {
          |                "userid": 553517,
          |                "name": "! ! ! !"
          |            }
          |         ]
          |    }
          |}""".stripMargin

      val commands = Seq(
        new Command(Map("action" -> "query", "list" -> queryType, "continue" -> ""), response1),
        new Command(Map("action" -> "query", "list" -> queryType,
          "aufrom" -> "! ! !", "continue" -> "-||"), response2)
      )

      val bot = getBot(commands: _*)

      val action =
        Action(
          Query(
            ListParam(
              AllUsers()
            )
          )
        )

      val future = bot.run(action)

      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 4
      val users = result.flatMap(_.lastRevisionUser)
      users(0) === User(146308, "!")
      users(1) === User(480659, "! !")
      users(2) === User(505506, "! ! !")
      users(3) === User(553517, "! ! ! !")
    }
  }
}
