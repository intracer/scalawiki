package org.scalawiki.query

import java.util.concurrent.TimeUnit

import org.scalawiki.Timestamp
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.dto.cmd.query.list.{ListParam, UcUser, UserContribs}
import org.scalawiki.dto.{Page, Revision, User}
import org.scalawiki.util.{Command, MockBotSpec}
import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ListUserContribsSpec extends Specification with MockBotSpec {

  "get all users without continue" should {
    "return users in" in {
      val queryType = "usercontribs"

      val response1 =
        """{ "query": {
          |        "usercontribs": [
          |            {
          |                "userid": 4587601,
          |                "user": "Catrope",
          |                "pageid": 11650099,
          |                "revid": 136629050,
          |                "parentid": 0,
          |                "ns": 3,
          |                "title": "User talk:Catrope",
          |                "timestamp": "2007-06-07T16:45:30Z",
          |                "new": "",
          |                "minor": "",
          |                "comment": "Creation; directing to BW",
          |                "size": 119
          |            }
          |          ]
          |        }
          |}""".stripMargin

      val commands = Seq(
        new Command(Map("action" -> "query", "list" -> queryType, "ucuser" -> "Catrope" , "continue" -> ""), response1)
      )

      val bot = getBot(commands: _*)

      val action =
        Action(
          Query(
            ListParam(
              UserContribs(UcUser(Seq("Catrope")))
            )
          )
        )

      val future = bot.run(action)

      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 1
      val user = User(4587601, "Catrope")

      val rev = new Revision(Some(136629050), Some(11650099), Some(0), Some(user),
        Some(Timestamp.parse("2007-06-07T16:45:30Z")), Some("Creation; directing to BW"), None, Some(119))
      result(0) === new Page(Some(11650099), 3, "User talk:Catrope", revisions = Seq(rev))
    }
  }

//  "get all users with continue" should {
//    "return users in" in {
//      val queryType = "allusers"
//
//      val response1 =
//        """{  "continue": {
//          |        "aufrom": "! ! !",
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
//        new Command(Map("action" -> "query", "list" -> queryType, "continue" -> ""), response1),
//        new Command(Map("action" -> "query", "list" -> queryType,
//          "aufrom" -> "! ! !", "continue" -> "-||"), response2)
//      )
//
//      val bot = getBot(commands: _*)
//
//      val action =
//        Action(
//          Query(
//            ListParam(
//              AllUsers()
//            )
//          )
//        )
//
//      val future = new DslQuery(action, bot).run()
//
//      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
//      result must have size 4
//      val users = result.flatMap(_.revisions.head.user)
//      users(0) === User(146308, "!")
//      users(1) === User(480659, "! !")
//      users(2) === User(505506, "! ! !")
//      users(3) === User(553517, "! ! ! !")
//    }
//  }
}
