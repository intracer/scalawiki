package org.scalawiki.query

import org.scalawiki.Timestamp
import org.scalawiki.dto.User
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.dto.cmd.query.list.{AllUsers, AuProp, ListParam}
import org.scalawiki.util.{HttpStub, MockBotSpec}
import org.specs2.mutable.Specification
import spray.util.pimpFuture

import scala.concurrent.ExecutionContext.Implicits.global

class ListAllUsersSpec extends Specification with MockBotSpec {

  "get all users" should {
    "return users without continue" in {
      val queryType = "allusers"

      val response1 =
        """{ "query": {
                "allusers": [
                    {
                      "userid": 146308,
                      "name": "!"
                    },
                    {
                       "userid": 480659,
                       "name": "! !"
                    }
                ]
          }}"""

      val commands = Seq(
        HttpStub(Map("action" -> "query",
                     "list" -> queryType,
                     "continue" -> "",
                     "format" -> "json",
                     "utf8" -> "",
//                     "auprop" -> "registration|editcount|blockinfo"
        ),
                 response1)
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

      val result = bot.run(action).map(_.toSeq).await

      result must have size 2
      val users = result.flatMap(_.lastRevisionUser)
      users(0) === User(146308, "!")
      users(1) === User(480659, "! !")
    }

    "return users with editcount, registration and blockinfo" in {
      val queryType = "allusers"

      val response1 =
        """{ "query": {
                "allusers": [
                   {
                      "userid": 3634417,
                      "name": "Y",
                      "editcount": 13892,
                      "registration": "2007-02-22T03:19:08Z",
                      "blockid": 278807,
                      "blockedby": "Gurch",
                      "blockedbyid": 241822,
                      "blockedtimestamp": "2006-10-17T06:53:13Z",
                      "blockreason": "username",
                      "blockexpiry": "infinity"
                  },
                  {
                      "userid": 53928,
                      "name": "Y (usurped)",
                      "editcount": 0,
                      "registration": ""
                   }
                ]
          }}"""

      val query = Map("action" -> "query",
                      "list" -> queryType,
                      "auprop" -> "registration|editcount|blockinfo",
                      "continue" -> "")

      val bot = getBot(Seq(HttpStub(query, response1)): _*)

      val action =
        Action(
          Query(
            ListParam(
              AllUsers(AuProp(Seq("registration", "editcount", "blockinfo")))
            )
          )
        )

      val result = bot.run(action).map(_.toSeq).await

      result must have size 2
      val users = result.flatMap(_.lastRevisionUser)
      users(0) === new User(Some(3634417),
                            Some("Y"),
                            Some(13892),
                            Some(Timestamp.parse("2007-02-22T03:19:08Z")),
                            Some(true))
      users(1) === new User(Some(53928),
                            Some("Y (usurped)"),
                            Some(0),
                            None,
                            Some(false))
    }

    "return users with continue" in {
      val queryType = "allusers"

      val response1 =
        """{ "continue": {
                "aufrom": "! ! !",
                "continue": "-||"
              },
              "query": {
                 "allusers": [
                    {
                      "userid": 146308,
                      "name": "!"
                    },
                    {
                        "userid": 480659,
                        "name": "! !"
                    }
                 ]
              }
           }"""

      val response2 =
        """{ "query": {
                "allusers": [
                    {
                      "userid": 505506,
                      "name": "! ! !"
                    },
                    {
                      "userid": 553517,
                      "name": "! ! ! !"
                    }
                ]
           }}"""

      val query = Map("action" -> "query", "list" -> queryType)

      val commands = Seq(
        HttpStub(query + ("continue" -> ""), response1),
        HttpStub(query ++ Map("aufrom" -> "! ! !", "continue" -> "-||"),
                 response2)
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

      val result = bot.run(action).map(_.toSeq).await

      result must have size 4
      val users = result.flatMap(_.lastRevisionUser)
      users(0) === User(146308, "!")
      users(1) === User(480659, "! !")
      users(2) === User(505506, "! ! !")
      users(3) === User(553517, "! ! ! !")
    }
  }
}
