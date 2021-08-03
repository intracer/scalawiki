package org.scalawiki.query

import org.scalawiki.dto.{Image, Page}
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.prop.{ImageInfo, Images, Prop}
import org.scalawiki.dto.cmd.query.{Generator, Query, TitlesParam}
import org.scalawiki.util.{HttpStub, MockBotSpec}
import org.specs2.mutable.Specification
import spray.util.pimpFuture

class PropImagesSpec extends Specification with MockBotSpec {

  "get page images" should {
    "return page images" in {

      val response =
        """{ "query": {
          |        "pages": {
          |            "736": {
          |                "pageid": 736,
          |                "ns": 0,
          |                "title": "Albert Einstein",
          |                "images": [
          |                    {
          |                        "ns": 6,
          |                        "title": "File:1919 eclipse positive.jpg"
          |                    },
          |                    {
          |                        "ns": 6,
          |                        "title": "File:Albert Einstein's exam of maturity grades (color2).jpg"
          |                    }
          |                ]
          |            }
          |        }
          |    }
          |}""".stripMargin


      val commands = Seq(
        HttpStub(Map("action" -> "query", "titles" -> "Albert_Einstein", "prop" -> "images", "format" -> "json", "continue" -> ""), response)
      )

      val bot = getBot(commands: _*)

      val action = Action(Query(
        TitlesParam(Seq("Albert_Einstein")),
        Prop(Images())
      ))

      val result = bot.run(action).await
      result must have size 1
      val page = result(0)
      page === new Page(Some(736), Some(0), "Albert Einstein", images = Seq(
        new Image("File:1919 eclipse positive.jpg"),
        new Image("File:Albert Einstein's exam of maturity grades (color2).jpg")
      ))
    }
  }

  "return page images with imageinfo" in {

    val response =
      """{
        |    "query": {
        |        "pages": {
        |            "32228631": {
        |                "pageid": 32228631,
        |                "ns": 6,
        |                "title": "File:0 green.svg",
        |                "imagerepository": "local",
        |                "imageinfo": [
        |                    {
        |                        "timestamp": "2014-04-17T17:41:01Z",
        |                        "user": "Amakuha"
        |                    }
        |                ]
        |            },
        |            "20066016": {
        |                "pageid": 20066016,
        |                "ns": 6,
        |                "title": "File:10turquoise.png",
        |                "imagerepository": "local",
        |                "imageinfo": [
        |                    {
        |                        "timestamp": "2012-06-29T01:24:40Z",
        |                        "user": "Hanryjunior"
        |                    }
        |                ]
        |            }
        |        }
        |    }
        |}""".stripMargin


    val commands = Seq(
      HttpStub(Map("action" -> "query", "titles" -> "Commons:Wiki_Loves_Earth_2015/Winners",
        "prop" -> "imageinfo", "generator" -> "images", "format" -> "json", "continue" -> ""), response)
    )

    val bot = getBot(commands: _*)
    val action = Action(Query(
      TitlesParam(Seq("Commons:Wiki_Loves_Earth_2015/Winners")),
      Prop(ImageInfo()),
      Generator(Images())
    ))

    val result = bot.run(action).await
    result must have size 2
    //    val page = result(0)
    //    result(0) === new Page(Some(736), 0, "Albert Einstein", images = Seq(
    //      new Image("File:1919 eclipse positive.jpg"),
    //      new Image("File:Albert Einstein's exam of maturity grades (color2).jpg")
    //    ))
    //  }
  }

}
