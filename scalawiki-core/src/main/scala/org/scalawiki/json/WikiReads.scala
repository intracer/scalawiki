package org.scalawiki.json

import org.joda.time.DateTime
import org.scalawiki.dto._
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsNumber, JsPath, JsResult, JsString, JsSuccess, JsValue, Reads, _}

/**
  * Created by francisco on 03/11/16.
  */
trait WikiReads[T] extends Reads[T] {
}

abstract class WikiResponseReads[T]() {
  val TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"
  val df = org.joda.time.format.DateTimeFormat.forPattern(TIMESTAMP_PATTERN).withZoneUTC()

  lazy val jodaDateTimeReads = jodaDateReads(TIMESTAMP_PATTERN)

  def parseDate(input: String): DateTime = DateTime.parse(input, df)

  def parseDateOpt(input: String): Option[DateTime] =
    scala.util.control.Exception.allCatch[DateTime] opt parseDate(input)

  def jodaDateReads(pattern: String, corrector: String => String = identity): Reads[DateTime] =
    new Reads[DateTime] {
      def reads(json: JsValue): JsResult[DateTime] = json match {
        case JsNumber(d) => JsSuccess(new DateTime(d.toLong))
        case JsString(s) => parseDateOpt(corrector(s)) match {
          case Some(d) => JsSuccess(d)
          case None => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jodadate.format", pattern))))
        }
        case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.date"))))
      }
    }
}

trait PageIdParameter {
  val pageId: Option[Long] = None
}

trait TitleParameter {
  val title: Option[String] = None
}

case class PageRead() extends WikiResponseReads with WikiReads[Page] {

  import play.api.libs.functional.syntax._

  private val pageRead: Reads[Page] = (
    (__ \ "pageid").readNullable[Long] ~
      (__ \ "ns").read[Int] ~
      (__ \ "title").read[String] ~
      (__ \ "missing").readNullable[String] ~
      (__ \ "subjectid").readNullable[Long] ~
      (__ \ "talkid").readNullable[Long]
    ) (Page.full _)

  override def reads(json: JsValue): JsResult[Page] = pageRead.reads(json)
}

case class UserRead() extends WikiResponseReads with WikiReads[User] {

  import play.api.libs.functional.syntax._

  private val userRead: Reads[User] = (
    (__ \ "userid").readNullable[Long] ~
      (__ \ "name").readNullable[String] ~
      (__ \ "editcount").readNullable[Long] ~
      (__ \ "registration").readNullable[DateTime](jodaDateTimeReads) ~
      (__ \ "blockid").readNullable[Long].map(_.map(_ => true)) ~
      (__ \ "emailable").readNullable[String].map(_.map(_ => true)) ~
      (__ \ "missing").readNullable[String].map(_.isDefined)
    ) (User.apply(
    _: Option[Long],
    _: Option[String],
    _: Option[Long],
    _: Option[DateTime],
    _: Option[Boolean],
    _: Option[Boolean],
    _: Boolean
  )
  )

  override def reads(json: JsValue): JsResult[User] = userRead.reads(json)
}

case class RevisionRead(override val pageId: Option[Long])
  extends WikiResponseReads with WikiReads[Revision] with PageIdParameter {

  import play.api.libs.functional.syntax._

  private val revisionRead: Reads[Revision] = (
    (__ \ "revid").readNullable[Long] ~
      Reads.pure[Option[Long]](pageId) ~
      (__ \ "parentid").readNullable[Long] ~
      (
        (__ \ "userid").readNullable[Long] ~
          (__ \ "user").readNullable[String]
        ) (Contributor.apply _) ~
      (__ \ "timestamp").readNullable[DateTime](jodaDateTimeReads) ~
      (__ \ "comment").readNullable[String] ~
      (__ \ "*").readNullable[String] ~
      (__ \ "size").readNullable[Long] ~
      (__ \ "sha1").readNullable[String] //~
    //Reads.pure[Option[Long]](None) // textId
    ) (Revision.apply(_: Option[Long],
    _: Option[Long],
    _: Option[Long],
    _: Option[Contributor],
    _: Option[DateTime],
    _: Option[String],
    _: Option[String],
    _: Option[Long],
    _: Option[String]))

  override def reads(json: JsValue): JsResult[Revision] = revisionRead.reads(json)
}

case class GlobalUserInfoRead() extends WikiResponseReads with WikiReads[GlobalUserInfo] {

  import play.api.libs.functional.syntax._

  implicit val sulAccountReads: Reads[SulAccount] = (
    (__ \ "wiki").read[String] ~
      (__ \ "url").read[String] ~
      (__ \ "timestamp").read[DateTime](jodaDateTimeReads) ~
      (__ \ "method").read[String] ~
      (__ \ "editcount").read[Long] ~
      (__ \ "registration").read[DateTime](jodaDateTimeReads)
    ) (SulAccount.apply _)

  private val globalInfoReads: Reads[GlobalUserInfo] =
    (
      (__ \ "home").read[String] ~
        (__ \ "id").read[Long] ~
        (__ \ "registration").read[DateTime](jodaDateTimeReads) ~
        (__ \ "name").read[String] ~
        (__ \ "merged").read[Seq[SulAccount]] ~
        (__ \ "editcount").read[Long]
      ) (GlobalUserInfo.apply _)


  override def reads(json: JsValue): JsResult[GlobalUserInfo] = globalInfoReads.reads(json)
}

case class ImageRead(override val pageId: Option[Long] = None, override val title: Option[String] = None)
  extends WikiResponseReads
    with WikiReads[Image]
    with PageIdParameter with TitleParameter {

  import play.api.libs.functional.syntax._

  private val imagesRead: Reads[Image] = (
    (title match {
      case Some(t) => Reads.pure[String](t)
      case None => (__ \ "title").read[String]
    }) ~
      (__ \ "timestamp").readNullable[DateTime](jodaDateTimeReads) ~
      (__ \ "user").readNullable[String] ~
      (__ \ "size").readNullable[Long] ~
      (__ \ "width").readNullable[Int] ~
      (__ \ "height").readNullable[Int] ~
      (__ \ "url").readNullable[String] ~
      (__ \ "descriptionurl").readNullable[String] ~
      Reads.pure[Option[Long]](pageId)
    //      (__ \ "extmetadata" \ "ImageDescription" \ "value").readNullable[String] and
    //      (__ \ "extmetadata" \ "Artist" \ "value").readNullable[String]
    ) (Image.basic _)

  override def reads(json: JsValue): JsResult[Image] = imagesRead.reads(json)
}

case class CategoryInfoRead() extends WikiResponseReads with WikiReads[CategoryInfo] {

  import play.api.libs.functional.syntax._

  private val categoryRead: Reads[CategoryInfo] = (
    (__ \ "size").read[Long] ~
      (__ \ "pages").read[Long] ~
      (__ \ "files").read[Long] ~
      (__ \ "subcats").read[Long]
    ) (CategoryInfo.apply _)

  override def reads(json: JsValue): JsResult[CategoryInfo] = categoryRead.reads(json)
}


case class UserContributorRead() extends WikiResponseReads with WikiReads[UserContrib] {

  import play.api.libs.functional.syntax._

  private val userContribRead: Reads[UserContrib] = (
    (__ \ "userid").read[Long] ~
      (__ \ "user").read[String] ~
      (__ \ "pageid").read[Long] ~
      (__ \ "revid").read[Long] ~
      (__ \ "parentid").read[Long] ~
      (__ \ "ns").read[Int] ~
      (__ \ "title").read[String] ~
      (__ \ "timestamp").read[DateTime](jodaDateTimeReads) ~
      //      (__ \ "new").read[String] ~
      //      (__ \ "minor").read[Boolea] ~
      (__ \ "comment").readNullable[String] ~ // can be hidden
      (__ \ "size").readNullable[Long]
    ) (UserContrib.apply _)

  override def reads(json: JsValue): JsResult[UserContrib] = userContribRead.reads(json)
}