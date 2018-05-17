package org.scalawiki.query

import org.scalawiki.dto.User
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.dto.cmd.query.list._
import org.scalawiki.util.{HttpStub, MockBotSpec}
import org.specs2.mutable.Specification
import spray.util.pimpFuture

class ListUsersSpec extends Specification with MockBotSpec {

  "get users" should {
    "return no users" in {
      val queryType = "users"

      val response1 =
        """{ "query": {
          |        "users": []
          |    }
          |}""".stripMargin

      val commands = Seq(
        HttpStub(Map("action" -> "query", "list" -> queryType, "continue" -> ""), response1)
      )

      val bot = getBot(commands: _*)

      val action =
        Action(
          Query(
            ListParam(
              Users()
            )
          )
        )

      val result = bot.run(action).await
      result must have size 0
    }

    "only missing user" in {
      val queryType = "users"

      val response1 =
        """{ "query": {
          |        "users": [
          |             {
          |                "name": "MissingUser",
          |                "missing": ""
          |            }
          |         ]
          |    }
          |}""".stripMargin

      val expectedParams = Map(
        "action" -> "query", "list" -> queryType,
        "ususers" -> "MissingUser",
        "continue" -> "")

      val commands = Seq(HttpStub(expectedParams, response1))

      val bot = getBot(commands: _*)

      val action =
        Action(
          Query(
            ListParam(
              Users(UsUsers(Seq("MissingUser")))
            )
          )
        )

      val result = bot.run(action).await
      result must have size 1

      val users = result.flatMap(_.lastRevisionUser)
      users === Seq(
        new User(None, Some("MissingUser"), missing = true)
      )
    }


    "with missing user" in {
      val queryType = "users"

      val response1 =
        """{ "query": {
          |        "users": [
          |             {
          |                "name": "MissingUser",
          |                "missing": ""
          |            },
          |            {
          |                "userid": 53928,
          |                "name": "ExistingUser"
          |            }
          |         ]
          |    }
          |}""".stripMargin

      val expectedParams = Map("action" -> "query", "list" -> queryType,
        "ususers" -> "MissingUser|ExistingUser",
        "continue" -> "")

      val commands = Seq(HttpStub(expectedParams, response1))

      val bot = getBot(commands: _*)

      val action =
        Action(
          Query(
            ListParam(
              Users(UsUsers(Seq("MissingUser", "ExistingUser")))
            )
          )
        )

      val result = bot.run(action).await
      result must have size 2

      val users = result.flatMap(_.lastRevisionUser)
      users === Seq(
        new User(None, Some("MissingUser"), missing = true),
        new User(Some(53928), Some("ExistingUser"))
      )
    }

    "return users with editcount and emailable" in {
      val queryType = "users"

      val response1 =
        """{ "query": {
          |        "users": [
          |             {
          |                "userid": 3634417,
          |                "name": "Y",
          |                "editcount": 13892,
          |                "emailable": ""
          |            },
          |            {
          |                "userid": 53928,
          |                "name": "Y (usurped)",
          |                "editcount": 0
          |            }
          |         ]
          |    }
          |}""".stripMargin

      val expectedParams = Map("action" -> "query", "list" -> queryType,
        "usprop" -> "editcount|emailable",
        "ususers" -> "Y|Y (usurped)",
        "continue" -> "")

      val commands = Seq(
        HttpStub(expectedParams, response1)
      )

      val bot = getBot(commands: _*)

      val action =
        Action(
          Query(
            ListParam(
              Users(
                UsUsers(Seq("Y", "Y (usurped)")),
                UsProp(UsEditCount, UsEmailable))
            )
          )
        )

      action.pairs.toMap + ("continue" -> "") === expectedParams

      val result = bot.run(action).await
      result must have size 2
      val users = result.flatMap(_.lastRevisionUser)
      users(0) === new User(Some(3634417), Some("Y"), Some(13892), None, None, Some(true))
      users(1) === new User(Some(53928), Some("Y (usurped)"), Some(0), None, None, Some(false))
    }

    "return users with continue" in {
      // TODO compare with actual MediaWiki behaviour
      val queryType = "users"

      val response1 =
        """{  "continue": {
          |        "continue": "-||"
          |    },
          |    "query": {
          |        "users": [
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
          |        "users": [
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
        HttpStub(Map("action" -> "query", "ususers" -> "!|! !|! ! !|! ! ! !", "list" -> queryType, "continue" -> ""), response1),
        HttpStub(Map("action" -> "query", "ususers" -> "!|! !|! ! !|! ! ! !", "list" -> queryType, "continue" -> "-||"), response2)
      )

      val bot = getBot(commands: _*)

      val action =
        Action(
          Query(
            ListParam(
              Users(
                UsUsers(Seq("!", "! !", "! ! !", "! ! ! !"))
              )
            )
          )
        )

      val result = bot.run(action).await

      result must have size 4
      val users = result.flatMap(_.lastRevisionUser)
      users(0) === User(146308, "!")
      users(1) === User(480659, "! !")
      users(2) === User(505506, "! ! !")
      users(3) === User(553517, "! ! ! !")
    }
  }
}
