package org.scalawiki.query

import org.scalawiki.Timestamp
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.dto.cmd.query.list.{ListParam, UcUser, UserContribs}
import org.scalawiki.dto.{Page, Revision, User}
import org.scalawiki.util.{HttpStub, MockBotSpec}
import org.specs2.mutable.Specification
import spray.util.pimpFuture

class ListUserContribsSpec extends Specification with MockBotSpec {

  "get usercontribs without continue" should {
    "return users in" in {
      val queryType = "usercontribs"

      val response1 =
        """{ "query": {
                "usercontribs": [
                    {
                        "userid": 4587601,
                        "user": "Catrope",
                        "pageid": 11650099,
                        "revid": 136629050,
                        "parentid": 0,
                        "ns": 3,
                        "title": "User talk:Catrope",
                        "timestamp": "2007-06-07T16:45:30Z",
                        "new": "",
                        "minor": "",
                        "comment": "Creation; directing to BW",
                        "size": 119
                    }
                  ]
                }
           }"""

      val query = Map("action" -> "query", "list" -> queryType, "ucuser" -> "Catrope", "continue" -> "")
      val commands = Seq(
        HttpStub(query, response1)
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

      val result = bot.run(action).await

      result must have size 1
      val user = User(4587601, "Catrope")

      val rev = new Revision(Some(136629050), Some(11650099), Some(0), Some(user),
        Some(Timestamp.parse("2007-06-07T16:45:30Z")), Some("Creation; directing to BW"), None, Some(119))
      result(0) === new Page(Some(11650099), Some(3), "User talk:Catrope", revisions = Seq(rev))
    }
  }

}
