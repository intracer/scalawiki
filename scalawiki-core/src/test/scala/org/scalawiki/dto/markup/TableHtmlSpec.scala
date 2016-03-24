package org.scalawiki.dto.markup

import org.specs2.mutable.Specification
import org.specs2.matcher.ContentMatchers._
import org.scalawiki.dto.markup.LineUtil._

class TableHtmlSpec extends Specification {

  "asHtml" should {
    "make empty table" in {
      val table = new Table(Seq.empty, Seq.empty, "", "")
      table.asHtml === "<table>\n</table>"
    }

    "make table with css class" in {
      val table = new Table(Seq.empty, Seq.empty, "", "wikitable")
      table.asHtml === "<table> class='wikitable'\n</table>"
    }

    "make table with title" in {
      val table = new Table(Seq.empty, Seq.empty, "title", "")
      table.asHtml === "<table>\n<caption> title </caption>\n</table>"
    }

    "make table with 1 column header" in {
      val table = new Table(Seq("header1"), Seq.empty, "", "")
      table.asHtml must haveSameLinesAs(
        """<table>
          |<thead>
          |<tr>
          |  <th> header1 </th>
          |</tr>
          |</thead>
          |</table>""".stripMargin)
    }

    "make table with 2 columns header" in {
      val table = new Table(Seq("header1", "header2"), Seq.empty, "", "")
      table.asHtml must haveSameLinesAs(
        """<table>
          |<thead>
          |<tr>
          |  <th> header1 </th>
          |  <th> header2 </th>
          |</tr>
          |</thead>
          |</table>""".stripMargin)
    }

    "make table with 3 columns headers" in {
      val table = new Table(Seq("header1", "header2", "header3"), Seq.empty, "", "")
      table.asHtml must haveSameLinesAs(
        """<table>
          |<thead>
          |<tr>
          |  <th> header1 </th>
          |  <th> header2 </th>
          |  <th> header3 </th>
          |</tr>
          |</thead>
          |</table>""".stripMargin)
    }

    "make table with 1 data column" in {
      val table = new Table(Seq.empty, Seq(Seq("data11")), "", "")
      table.asHtml must haveSameLinesAs(
        """<table>
          |<tbody>
          |<tr>
          |  <td> data11 </td>
          |</tr>
          |</tbody>
          |</table>""".stripMargin)
    }

    "make table with 2 data columns" in {
      val table = new Table(Seq.empty, Seq(Seq("data11", "data12")), "", "")
      table.asHtml must haveSameLinesAs(
        """<table>
          |<tbody>
          |<tr>
          |  <td> data11 </td>
          |  <td> data12 </td>
          |</tr>
          |</tbody>
          |</table>""".stripMargin)
    }

    "make table with 1 data column and 2 rows" in {
      val table = new Table(Seq.empty, Seq(Seq("data11"), Seq("data21")), "", "")
      table.asHtml must haveSameLinesAs(
        """<table>
          |<tbody>
          |<tr>
          |  <td> data11 </td>
          |</tr>
          |<tr>
          |  <td> data21 </td>
          |</tr>
          |</tbody>
          |</table>""".stripMargin)
    }

    "make table with css class and title" in {
      val table = new Table(Seq.empty, Seq.empty, "title", "wikitable")
      table.asHtml must haveSameLinesAs(
        """<table> class='wikitable'
          |<caption> title </caption>
          |</table>""".stripMargin)
    }

    "make table with css class and 1 column header" in {
      val table = new Table(Seq("header1"), Seq.empty, "", "wikitable")
      table.asHtml must haveSameLinesAs(
        """<table> class='wikitable'
          |<thead>
          |<tr>
          |  <th> header1 </th>
          |</tr>
          |</thead>
          |</table>""".stripMargin)
    }

    "make table with css class and 1 data column" in {
      val table = new Table(Seq.empty, Seq(Seq("data11")), "", "wikitable")
      table.asHtml must haveSameLinesAs(
        """<table> class='wikitable'
          |<tbody>
          |<tr>
          |  <td> data11 </td>
          |</tr>
          |</tbody>
          |</table>""".stripMargin)
    }

    "make table with title and 1 column header" in {
      val table = new Table(Seq("header1"), Seq.empty, "title", "")
      table.asHtml must haveSameLinesAs(
        """<table>
          |<caption> title </caption>
          |<thead>
          |<tr>
          |  <th> header1 </th>
          |</tr>
          |</thead>
          |</table>""".stripMargin)
    }

    "make table with title and 1 data column" in {
      val table = new Table(Seq.empty, Seq(Seq("data11")), "title", "")
      table.asHtml must haveSameLinesAs(
        """<table>
          |<caption> title </caption>
          |<tbody>
          |<tr>
          |  <td> data11 </td>
          |</tr>
          |</tbody>
          |</table>""".stripMargin)
    }

    "make table with 1 column header and 1 data row" in {
      val table = new Table(Seq("header1"), Seq(Seq("data11")), "", "")
      table.asHtml must haveSameLinesAs(
        """<table>
          |<thead>
          |<tr>
          |  <th> header1 </th>
          |</tr>
          |</thead>
          |<tbody>
          |<tr>
          |  <td> data11 </td>
          |</tr>
          |</tbody>
          |</table>""".stripMargin)
    }
  }
}
