package org.scalawiki.parser

import org.scalawiki.dto.markup.Table
import org.scalawiki.wikitext.TableParser
import org.specs2.mutable.Specification

class TableParserSpec extends Specification {

  val parser = TableParser

  "table parser" should {
    "parse empty table" in {
      val wiki = "{|\n|}"
      parser.parse(wiki).asWiki === wiki
    }

    "parse table with 1 column header" in {
      val wiki = "{|\n! header1\n|}"
      parser.parse(wiki) === Table(Seq("header1"), Seq.empty, "", "")
    }

    "parse table with 2 column headers" in {
      val wiki = "{|\n! header1 !! header2\n|}"
      parser.parse(wiki) === Table(Seq("header1", "header2"), Seq.empty, "", "")
    }

    "parse table with 1 column header (other style)" in {
      val wiki = "{|\n|-\n! header1\n|}"
      parser.parse(wiki) === new Table(Seq("header1"), Seq.empty, "", "")
    }

    "parse table with 2 column headers (other style)" in {
      val wiki = "{|\n|-\n! header1 !! header2\n|}"
      parser.parse(wiki) === Table(Seq("header1", "header2"), Seq.empty, "", "")
    }

    "parse table with 1 data column" in {
      val wiki = "{|\n|-\n| data11\n|}"
      parser.parse(wiki) === new Table(Seq.empty, Seq(Seq("data11")), "", "")
    }

    "parse table with 2 data columns" in {
      val wiki = "{|\n|-\n| data11 || data12\n|}"
      parser.parse(wiki) === new Table(Seq.empty, Seq(Seq("data11", "data12")), "", "")
    }

    "parse table with formatted data" in {
      val data = Seq(
        "'''bold''' ''italic''",
        "[[article1]], [[article2|link2]]",
        "{{template|param}} {{template|name=param}}",
        "[http://link title] [http://link] http://link",
        "a<ref name=\"b\">c</ref> <ref name=\"d\"/> <br /> <small>sm</small>" // +  "<!-- comments -->"
      )

      val wiki = "{|\n|-\n|" + data.mkString(" || ") + "\n|}"

      parser.parse(wiki) === new Table(Seq.empty, Seq(data), "", "")
    }

    "parse table with empty columns" in {
      val wiki = "{|\n|-\n| data11 || || data13\n|}"
      parser.parse(wiki) === new Table(Seq.empty, Seq(Seq("data11", "", "data13")), "", "")
    }

    "parse table with 1 data column and 2 rows" in {
      val wiki = "{|\n|-\n| data11\n|-\n| data21\n|}"
      parser.parse(wiki) === new Table(Seq.empty, Seq(Seq("data11"), Seq("data21")), "", "")
    }

    "parse table with header and data" in {
      val wiki = "{|\n! header1\n|-\n| data11\n|}"
      parser.parse(wiki) === Table(Seq("header1"), Seq(Seq("data11")), "", "")
    }

    "parse table with 2 columns header and data" in {
      val wiki = "{|\n! header1 !! header2\n|-\n| data11 || data12\n|}"
      parser.parse(wiki) === Table(Seq("header1", "header2"), Seq(Seq("data11", "data12")), "", "")
    }

    "parse table with 2 columns header and 1 column data" in {
      val wiki = "{|\n! header1 !! header2\n|-\n| data11 ||\n|}"
      parser.parse(wiki) === Table(Seq("header1", "header2"), Seq(Seq("data11", "")), "", "")
    }

    "parse table with 2 rows header" in {
      val wiki = "{|\n! header1 !! header2\n|-\n! data11 || data12\n|}"
      parser.parse(wiki) === Table(Seq("header1", "header2"), Seq(Seq("data11", "data12")), "", "")
    }
  }
}
