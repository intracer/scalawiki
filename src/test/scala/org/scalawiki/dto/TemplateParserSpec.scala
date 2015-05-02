package org.scalawiki.dto

import org.scalawiki.parser.TemplateParser
import org.specs2.mutable.Specification

class TemplateParserSpec extends Specification {

  val parser = TemplateParser

  "template" should {
    "parse empty" in {
      val template = parser.parse("{{TemplateName}}")
      template.templateName === "TemplateName"

      template.params === Map.empty
    }

    "parse empty trimmed" in {
      val template = parser.parse("{{ TemplateName }}")
      template.templateName === "TemplateName"

      template.params === Map.empty
    }

    "parse positional parameter" in {
      val template = parser.parse("{{TemplateName|param1}}")
      template.templateName === "TemplateName"

      template.params === Map("1" -> "param1")
    }

    "parse positional parameter article link" in {
      val template = parser.parse("{{TemplateName | [[ article | link ]] }}")
      template.templateName === "TemplateName"

      template.params === Map("1" -> "[[ article | link ]]")
    }

    "parse positional parameter article two article links" in {
      val template = parser.parse("{{TemplateName | [[ article1 | link1 ]], [[ article2 | link2 ]] }}")
      template.templateName === "TemplateName"

      template.params === Map("1" -> "[[ article1 | link1 ]], [[ article2 | link2 ]]")
    }


    "parse positional parameter template" in {
      val template = parser.parse("{{TemplateName | {{child}} }}")
      template.templateName === "TemplateName"

      template.params === Map("1" -> "{{child}}")
    }

    "parse positional parameter template with param" in {
      val template = parser.parse("{{TemplateName | {{child|param}} }}")
      template.templateName === "TemplateName"

      template.params === Map("1" -> "{{child|param}}")
    }

    "parse positional parameter newline" in {
      val template = parser.parse("{{TemplateName\n|param1\n}}")
      template.templateName === "TemplateName"

      template.params === Map("1" -> "param1")
    }

    "parse two positional parameters" in {
      val template = parser.parse("{{TemplateName|param1|param2}}")
      template.templateName === "TemplateName"

      template.params === Map("1" -> "param1", "2" -> "param2")
    }

    "parse two positional parameters newline" in {
      val template = parser.parse("{{TemplateName\n|param1\n|param2\n}}")
      template.templateName === "TemplateName"

      template.params === Map("1" -> "param1", "2" -> "param2")
    }

    "parse positional parameter trimmed" in {
      val template = parser.parse("{{ TemplateName | param1 }}")
      template.templateName === "TemplateName"

      template.params === Map("1" -> "param1")
    }

    "parse positional parameter trimmed newlines" in {
      val template = parser.parse("{{ TemplateName\n| param1\n}}")
      template.templateName === "TemplateName"

      template.params === Map("1" -> "param1")
    }

    "parse named positional parameter" in {
      val template = parser.parse("{{TemplateName|1=param1}}")
      template.templateName === "TemplateName"

      template.params === Map("1" -> "param1")
    }

    "parse named positional parameter newline" in {
      val template = parser.parse("{{TemplateName\n|1=param1\n}}")
      template.templateName === "TemplateName"

      template.params === Map("1" -> "param1")
    }

    "parse named positional parameter trimmed" in {
      val template = parser.parse("{{ TemplateName | 1 = param1 }}")
      template.templateName === "TemplateName"

      template.params === Map("1" -> "param1")
    }

    "parse named positional parameter trimmed newline" in {
      val template = parser.parse("{{ TemplateName\n| 1 = param1\n}}")
      template.templateName === "TemplateName"

      template.params === Map("1" -> "param1")
    }


    "parse two named positional parameters" in {
      val template = parser.parse("{{TemplateName|1=param1|2=param2}}")
      template.templateName === "TemplateName"

      template.params === Map("1" -> "param1", "2" -> "param2")
    }

    "parse two named positional parameters newline" in {
      val template = parser.parse("{{TemplateName\n|1=param1\n|2=param2\n}}")
      template.templateName === "TemplateName"

      template.params === Map("1" -> "param1", "2" -> "param2")
    }

    "parse named parameter" in {
      val template = parser.parse("{{TemplateName|name=value}}")
      template.templateName === "TemplateName"

      template.params === Map("name" -> "value")
    }

    "parse named parameter newline" in {
      val template = parser.parse("{{TemplateName\n|name=value\n}}")
      template.templateName === "TemplateName"

      template.params === Map("name" -> "value")
    }

    "parse named parameter trimmed" in {
      val template = parser.parse("{{ TemplateName | name = value }}")
      template.templateName === "TemplateName"

      template.params === Map("name" -> "value")
    }

    "parse named parameter trimmed newline" in {
      val template = parser.parse("{{ TemplateName\n| name = value\n}}")
      template.templateName === "TemplateName"

      template.params === Map("name" -> "value")
    }


  }


}
