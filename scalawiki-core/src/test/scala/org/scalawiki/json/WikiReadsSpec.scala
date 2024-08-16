package org.scalawiki.json

import java.time.ZonedDateTime
import org.scalawiki.dto._
import org.scalawiki.json.playjson.{CategoryInfoReads, GlobalUserInfoReads, ImageReads, PageReads, RevisionRead, UserContributorReads, UserReads, WikiReads}
import org.specs2.mutable.Specification
import play.api.libs.json._

/** Created by francisco on 03/11/16.
  */
class WikiReadsSpec extends Specification {

  "Wiki Reads" should {
    val pageRead: WikiReads[Page] = PageReads()
    val pageJson = """{"pageid": 123, "ns": 4, "title": "PageTitle" }"""
    "transform to Page object" in {
      val pageObject: Page = pageRead.reads(Json.parse(pageJson)).get
      pageObject.title === "PageTitle"
      pageObject.id === (Some(123))
    }

    val userRead: WikiReads[User] = UserReads()
    val userJson = """{"userid": 4, "name":"wikiUser", "editcount":56}"""
    "transform to User object" in {
      val userObject = userRead.reads(Json.parse(userJson)).get
      userObject.id === Some(4)
      userObject.blocked === None
      userObject.name === Some("wikiUser")
      userObject.emailable === None
      userObject.editCount === Some(56)
    }

    val revisionRead: WikiReads[Revision] = RevisionRead(Some(5))
    val revisionJson =
      """{"revid" : 23,
                         "parentid" : 5,
                         "user" : "wikiUser",
                         "timestamp" : "2016-11-04T08:20:40Z",
                         "comment" : "comment"}"""
    "transform to Revision object" in {
      val revisionObject = revisionRead.reads(Json.parse(revisionJson)).get
      println(s"${revisionObject.timestamp}")
      revisionObject.revId === Some(23)
      revisionObject.parentId === Some(5)
      revisionObject.user.get.name === Some("wikiUser")
      revisionObject.timestamp === Some(
        ZonedDateTime.parse("2016-11-04T08:20:40Z")
      )
    }

    val globalUserInfoRead: WikiReads[GlobalUserInfo] = GlobalUserInfoReads()
    val globalUserInfoJson =
      """{"home":"home",
         "name":"name",
        "id" : 5,
        "merged":[{
            "wiki" : "wiki",
            "timestamp" : "2016-11-07T19:27:40Z",
            "registration":"2016-11-07T19:30:40Z",
            "url" : "http://www.url.com",
            "method" : "post",
            "editcount": 5
        }],
        "registration":"2016-11-07T19:27:40Z",
        "editcount":5}"""
    "transform to GlobalUserInfo object" in {
      val globalUserInfo =
        globalUserInfoRead.reads(Json.parse(globalUserInfoJson)).get
      globalUserInfo.name === "name"
      globalUserInfo.merged(0).method === "post"
      globalUserInfo.merged(0).registration === ZonedDateTime.parse(
        "2016-11-07T19:30:40Z"
      )
    }

    val imageRead: WikiReads[Image] = ImageReads()
    val imageJson: String =
      """{"user":"user", "size":50, "width": 60, "height":60,"title":"page title"}"""
    "transform to Image without pageId either title to object" in {
      val image: Image = imageRead.reads(Json.parse(imageJson)).get
      image.author === None
      image.title === "page title"
      image.pageId === None
      image.height === Some(60)
    }

    val imageReadWithParameters: WikiReads[Image] =
      ImageReads(Some(1), Some("title"))
    "transform to Image with pageId and title to object" in {
      val image: Image =
        imageReadWithParameters.reads(Json.parse(imageJson)).get
      image.author === None
      image.title === "title"
      image.pageId === Some(1)
      image.height === Some(60)
    }

    val categoryInfoRead: WikiReads[CategoryInfo] = CategoryInfoReads()
    val categoryInfoJson =
      """{"size": 45, "pages": 5, "files": 7, "subcats": 3}"""
    "transform to CategoryInfo to object" in {
      val categoryInfo: CategoryInfo =
        categoryInfoRead.reads(Json.parse(categoryInfoJson)).get
      categoryInfo.pages === 5
      categoryInfo.files === 7
      categoryInfo.subCats === 3
    }

    val userContributorRead: WikiReads[UserContrib] = UserContributorReads()
    val userContribJson =
      """{"userid": 1,
         "user" : "wikiuser",
         "pageid": 34,
        "revid" : 5,
        "parentid": 4,
        "ns":3,
        "title": "page title",
        "timestamp":"2016-11-09T07:02:23Z"}"""

    "transform to UserContrib to object" in {
      val userContrib: UserContrib =
        userContributorRead.reads(Json.parse(userContribJson)).get
      userContrib.userId === 1
      userContrib.pageId === 34
      userContrib.revId === 5
      userContrib.title === "page title"
    }
  }
}
