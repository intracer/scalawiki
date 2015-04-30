package org.scalawiki.parser

import org.scalawiki.dto.markup.Table
import org.specs2.mutable.Specification

class TableParserSpec extends Specification {

  "table parser" should {
    "parse empty table" in {
      val wiki = "{|\n|}"   //new Table("", Seq.empty, Seq.empty, "")
      TableParser.parse(wiki).asWiki === wiki
    }

    "parse table with 1 column header" in {
      val wiki = "{|\n! header1\n|}"
      TableParser.parse(wiki) === Table("", Seq("header1"), Seq.empty, "")
    }

    "parse table with 2 column headers" in {
      val wiki = "{|\n! header1 !! header2\n|}"
      TableParser.parse(wiki) === Table("", Seq("header1", "header2"), Seq.empty, "")
    }

    "parse table with 1 column header (other style)" in {
      val wiki = "{|\n|-\n! header1\n|}"
      TableParser.parse(wiki) === new Table("", Seq("header1"), Seq.empty, "")
    }

    "parse table with 2 column headers (other style)" in {
      val wiki = "{|\n|-\n! header1 !! header2\n|}"
      TableParser.parse(wiki) === Table("", Seq("header1", "header2"), Seq.empty, "")
    }

    "parse table with 1 data column" in {
      val wiki = "{|\n|-\n| data11\n|}"
      TableParser.parse(wiki) === new Table("", Seq.empty, Seq(Seq("data11")), "")
    }

    "parse table with 2 data columns" in {
      val wiki = "{|\n|-\n| data11 || data12\n|}"
      TableParser.parse(wiki) === new Table("", Seq.empty, Seq(Seq("data11", "data12")), "")
    }

    "parse table with empty columns" in {
      val wiki = "{|\n|-\n| data11 || || data13\n|}"
      TableParser.parse(wiki) === new Table("", Seq.empty, Seq(Seq("data11", "", "data13")), "")
    }

    "parse table with 1 data column and 2 rows" in {
      val wiki = "{|\n|-\n| data11\n|-\n| data21\n|}"
      TableParser.parse(wiki) === new Table("", Seq.empty, Seq(Seq("data11"), Seq("data21")), "")
    }

  }
}
