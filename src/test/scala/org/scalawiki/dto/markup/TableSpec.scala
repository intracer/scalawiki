package org.scalawiki.dto.markup

import org.specs2.mutable.Specification

class TableSpec extends Specification {

  "asWiki" should {
    "make empty table" in {
      val table = new Table("", Seq.empty, Seq.empty, "")
      table.asWiki === "{|\n|}"
    }

    "make table with css class" in {
      val table = new Table("", Seq.empty, Seq.empty, "wikitable")
      table.asWiki === "{| class='wikitable'\n|}"
    }

    "make table with title" in {
      val table = new Table("title", Seq.empty, Seq.empty, "")
      table.asWiki === "{|\n|+ title\n|}"
    }

    "make table with 1 column header" in {
      val table = new Table("", Seq("header1"), Seq.empty, "")
      table.asWiki === "{|\n! header1\n|}"
    }

    "make table with 2 columns header" in {
      val table = new Table("", Seq("header1", "header2"), Seq.empty, "")
      table.asWiki === "{|\n! header1 !! header2\n|}"
    }

    "make table with 3 columns headers" in {
      val table = new Table("", Seq("header1", "header2", "header3"), Seq.empty, "")
      table.asWiki === "{|\n! header1 !! header2 !! header3\n|}"
    }

    "make table with 1 data column" in {
      val table = new Table("", Seq.empty, Seq(Seq("data11")), "")
      table.asWiki === "{|\n|-\n| data11\n|}"
    }

    "make table with 2 data columns" in {
      val table = new Table("", Seq.empty, Seq(Seq("data11", "data12")), "")
      table.asWiki === "{|\n|-\n| data11 || data12\n|}"
    }

    "make table with 1 data column and 2 rows" in {
      val table = new Table("", Seq.empty, Seq(Seq("data11"), Seq("data21")), "")
      table.asWiki === "{|\n|-\n| data11\n|-\n| data21\n|}"
    }

    "make table with css class and title" in {
      val table = new Table("title", Seq.empty, Seq.empty, "wikitable")
      table.asWiki === "{| class='wikitable'\n|+ title\n|}"
    }

    "make table with css class and 1 column header" in {
      val table = new Table("", Seq("header1"), Seq.empty, "wikitable")
      table.asWiki === "{| class='wikitable'\n! header1\n|}"
    }

    "make table with css class and 1 data column" in {
      val table = new Table("", Seq.empty, Seq(Seq("data11")), "wikitable")
      table.asWiki === "{| class='wikitable'\n|-\n| data11\n|}"
    }

    "make table with title and 1 column header" in {
      val table = new Table("title", Seq("header1"), Seq.empty, "")
      table.asWiki === "{|\n|+ title\n! header1\n|}"
    }

    "make table with title and 1 data column" in {
      val table = new Table("title", Seq.empty, Seq(Seq("data11")), "")
      table.asWiki === "{|\n|+ title\n|-\n| data11\n|}"
    }

    "make table with 1 column header and 1 data row" in {
      val table = new Table("", Seq("header1"), Seq(Seq("data11")), "")
      table.asWiki === "{|\n! header1\n|-\n| data11\n|}"
    }

  }

}
