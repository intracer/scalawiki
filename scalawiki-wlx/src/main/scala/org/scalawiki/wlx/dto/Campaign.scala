package org.scalawiki.wlx.dto

case class Campaign(enabled: Boolean,
                    title: String,
                    headerLabel: String,
                    thanksLabel: String,
                    defaultCategories: Seq[String],
                    fields: Seq[CampaignField])

case class CampaignField(wikitext: String, label: String)

object Campaign {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  def parse(json: String): Campaign = {
    Json.parse(json).validate(campaignReads).get
  }

  implicit val fieldReads: Reads[CampaignField] = (
    (__ \ "wikitext").read[String] and
      (__ \ "label").read[String]
    ) (CampaignField.apply _)

  val campaignReads: Reads[Campaign] = (
    (__ \ "enabled").read[Boolean] and
      (__ \ "title").read[String] and
      (__ \ "display" \ "headerLabel").read[String] and
      (__ \ "display" \ "thanksLabel").read[String] and
      (__ \ "defaults" \ "categories").read[Seq[String]] and
      (__ \ "fields").read[Seq[CampaignField]]
    ) (Campaign.apply _)

}