package org.scalawiki

import org.specs2.mutable.Specification
import org.scalawiki.util.{MockBotSpec, Command}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import org.scalawiki.dto.Page

class ListCategoryMembersSpec extends Specification with MockBotSpec {

  "get category members with continue" should {
    "return category members in" in {
      val queryType = "categorymembers"

      //      val response = {"query-continue":{"embeddedin":{"eicontinue":"10|Stub|6674690"}},"query":{"embeddedin":[{"pageid":569559,"ns":1,"title":"Talk:Welfare reform"},{"pageid":2581310,"ns":2,"title":"User:Rpyle731/sandbox/archive1"},{"pageid":3954860,"ns":3,"title":"User talk:PBS/Archive 6"},{"pageid":4571809,"ns":2,"title":"User:Formator"},{"pageid":5024711,"ns":3,"title":"User talk:Rauterkus"}]}}

      val response1 = """{"query":{"categorymembers":[{"pageid":569559,"ns":1,"title":"Talk:Welfare reform"}]}, "continue":{"continue":"-||","cmcontinue":"10|Stub|6674690"}}"""
      val response2 = """{"limits": {"categorymembers": 500}, "query":{"categorymembers":[ {"pageid":4571809,"ns":2,"title":"User:Formator"}]}}"""

      val commands = Seq(
        new Command(Map("action" -> "query", "list" -> queryType, "cmlimit" -> "max", "cmtitle" -> "Category:SomeCategory", "continue" -> ""), response1),
        new Command(Map("action" -> "query", "list" -> queryType, "cmlimit" -> "max", "cmtitle" -> "Category:SomeCategory", "continue" -> "-||", "cmcontinue" -> "10|Stub|6674690"), response2)
      )

      val bot = getBot(commands:_*)

      val future = bot.page("Category:SomeCategory").categoryMembers()
      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 2
      result(0) === Page(569559, 1, "Talk:Welfare reform")
      result(1) === Page(4571809, 2, "User:Formator")
    }
  }


}
