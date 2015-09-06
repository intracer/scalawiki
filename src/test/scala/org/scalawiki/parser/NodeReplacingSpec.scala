package org.scalawiki.parser

import org.scalawiki.wikitext.SwebleParser
import org.specs2.mutable.Specification
import org.sweble.wikitext.engine.config.WikiConfig
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp
import org.sweble.wikitext.parser.nodes.WtTemplate

class NodeReplacingSpec extends Specification {
  val parser = new SwebleParser {
    val config: WikiConfig = DefaultConfigEnWp.generate
  }

  "parser" should {
    "parse" in {
      val replaced = parser.replace("a {{TemplateName}} b", { case t: WtTemplate => t}, (x:WtTemplate) => "+")

      replaced === "a + b"

    }
  }

}
