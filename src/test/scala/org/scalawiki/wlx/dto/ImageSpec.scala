package org.scalawiki.wlx.dto

import org.scalawiki.dto.Template2
import org.specs2.mutable.Specification

class ImageSpec extends Specification {

  def makeTemplate(author: String) =
    new Template2("Information",
      Map(
        "description" -> "",
        "date" -> "",
        "source" -> "{{own}}",
        "author" -> author,
        "permission" -> "",
        "other versions" -> ""
      )
    ).text

  "get Author" should {
    "get author from wiki link" in {

      val wiki = makeTemplate("[[User:Qammer Wazir|Qammer Wazir]]")

      Image.getAuthorFromPage(wiki) === "Qammer Wazir"
    }
  }

}
