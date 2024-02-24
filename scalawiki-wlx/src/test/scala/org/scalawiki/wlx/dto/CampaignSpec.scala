package org.scalawiki.wlx.dto

import org.specs2.mutable.Specification
import org.scalawiki.util.TestUtils._

class CampaignSpec extends Specification {

  "Campaign" should {
    "parse json" in {
      val s = resourceAsString("/org/scalawiki/wlx/wle-at-nap.json")
      val campaign = Campaign.parse(s)

      campaign === Campaign(
        true,
        "Wiki Loves Earth in Austria",
        "{{Wiki Loves Earth is running|at|{{Upload campaign header wle-at}}}}",
        "{{Upload campaign use Wiki Loves Earth}}",
        Seq("National parks of Austria"),
        Seq(
          CampaignField(
            "{{Nationalpark Österreich|$1}}",
            "Name laut Liste"
          )
        )
      )
    }
  }

}
