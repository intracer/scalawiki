package org.scalawiki.bots.museum

import org.specs2.mutable.Specification

class ImageTemplateSpec extends Specification {

  val expected =
    """
      |== {{int:filedesc}} ==
      |{{Art Photo
      | |artist             =
      | |title              = {{uk|Archaeological Museum interior}}
      | |description        = Archaeological Museum interior description
      | |date               =
      | |medium             =
      | |dimensions         =
      | |institution        = {{Institution:NIEZ Museum}}
      | |location           =
      | |references         =
      | |object history     =
      | |exhibition history =
      | |credit line        =
      | |inscriptions       =
      | |notes              =
      | |accession number   =
      | |artwork license    =
      | |place of creation  =
      | |photo description  =
      | |photo date         =
      | |photographer       =
      | |source             = {{GLAM Ukraine - National Historic-Ethnographic Reserve "Pereyaslav"}}
      | |photo license      =
      | |other_versions     =
      |}}
      |
      |== {{int:license-header}} ==
      |{{OTRS Pending}}
      |[[Category:National Historic-Ethnographic Reserve "Pereyaslav"]]""".stripMargin

  "template" should {
    "should resolve config params from Map" in {
      val params = Map(
        "title" -> "Archaeological Museum interior",
        "description" -> "Archaeological Museum interior description",
      "location" -> "Archaeological Museum"
      )
      val resolved = ImageTemplate.resolve(params)
      resolved.isResolved === true
      resolved.getString("template") === expected
    }

    "should makeInfoPage" in {
      ImageTemplate.makeInfoPage(
        title = "Archaeological Museum interior",
        description =  "Archaeological Museum interior description",
        location = "Archaeological Museum"
      ) === expected
    }
  }

}
