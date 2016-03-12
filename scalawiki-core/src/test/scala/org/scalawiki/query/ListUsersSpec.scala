package org.scalawiki.query

import java.util.concurrent.TimeUnit

import org.scalawiki.dto.User
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.dto.cmd.query.list._
import org.scalawiki.util.{Command, MockBotSpec}
import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ListUsersSpec extends Specification with MockBotSpec {

  "get users" should {
    "return users without continue" in {
      val queryType = "users"

      val response1 =
        """{ "query": {
          |        "users": []
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
              Users()
            )
          )
        )

      val future = bot.run(action)

      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 0
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
        new Command(expectedParams, response1)
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

      val future = bot.run(action)

      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 2
      val users = result.flatMap(_.lastRevisionUser)
      users(0) === new User(Some(3634417), Some("Y"), Some(13892), None, None, Some(true))
      users(1) === new User(Some(53928), Some("Y (usurped)"), Some(0), None, None, Some(false))
    }

//    "return users with continue" in {
//      // TODO compare with actual MediaWiki behaviour
//      val queryType = "users"
//
//      val response1 =
//        """{  "continue": {
//          |        "continue": "-||"
//          |    },
//          |    "query": {
//          |        "allusers": [
//          |            {
//          |                "userid": 146308,
//          |                "name": "!"
//          |            },
//          |            {
//          |                "userid": 480659,
//          |                "name": "! !"
//          |            }
//          |         ]
//          |    }
//          |}""".stripMargin
//
//      val response2 =
//        """{ "query": {
//          |        "allusers": [
//          |             {
//          |                "userid": 505506,
//          |                "name": "! ! !"
//          |            },
//          |            {
//          |                "userid": 553517,
//          |                "name": "! ! ! !"
//          |            }
//          |         ]
//          |    }
//          |}""".stripMargin
//
//      val commands = Seq(
//        new Command(Map("action" -> "query", "ususers" -> "!|! !|! ! !|! ! ! !", "list" -> queryType, "continue" -> ""), response1),
//        new Command(Map("action" -> "query", "ususers" -> "!|! !|! ! !|! ! ! !", "list" -> queryType, "continue" -> "-||"), response2)
//      )
//
//      val bot = getBot(commands: _*)
//
//      val action =
//        Action(
//          Query(
//            ListParam(
//              Users(
//                UsUsers(Seq("!", "! !", "! ! !", "! ! ! !"))
//              )
//            )
//          )
//        )
//
//      val future = new DslQuery(action, bot).run()
//
//      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
//      result must have size 4
//      val users = result.flatMap(_.lastRevisionUser)
//      users(0) === User(146308, "!")
//      users(1) === User(480659, "! !")
//      users(2) === User(505506, "! ! !")
//      users(3) === User(553517, "! ! ! !")
//    }
  }
}
