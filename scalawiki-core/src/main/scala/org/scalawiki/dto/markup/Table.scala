package org.scalawiki.dto.markup


case class Table(
                  headers: Iterable[String],
                  data: Iterable[Iterable[String]],
                  title: String = "",
                  cssClass: String = "wikitable sortable") {


  def asWiki = {
    "{|" +
      (if (cssClass.nonEmpty) s" class='$cssClass'" else "") +
      (if (title.nonEmpty) "\n|+ " + title else "") +
      (if (headers.nonEmpty) headers.mkString("\n! ", " !! ", "") else "") +
      (if (data.nonEmpty)
        data.map {
          row =>
            row.mkString("| ", " || ", "")
        }.mkString("\n|-\n", "\n|-\n", "")
      else "") +
      "\n|}"
  }


  def asHtml = {
    "<table>" +
      (if (cssClass.nonEmpty) s" class='$cssClass'" else "") +
      (if (title.nonEmpty) s"\n<caption> $title </caption>" else "") +
      (if (headers.nonEmpty)
        headers.map(h => s"  <th> $h </th>").mkString("\n<thead>\n<tr>\n", "\n", "\n</tr>\n</thead>")
      else "") +
      (if (data.nonEmpty)
        data.map {
          row =>
            row.map(c => s"  <td> $c </td>").mkString("<tr>\n", "\n", "\n</tr>")
        }.mkString("\n<tbody>\n", "\n", "\n</tbody>")
      else "") +
      "\n</table>"
  }


  //    s"{| class=\"$cssClass\"\n" +
  //      s"|+ $title\n" +
  //      headers.mkString("! ", " !! ", "") +
  //      data.map(row => row.mkString("| ", " || ", "")).mkString("\n|-\n", "\n|-\n", "\n|}")
  //
  //
  //    val x = s"<table class=\"$cssClass\">"
  //      <thead>
  //        <tr>
  //          {}
  //          <th>@Messages("id")</th>
  //          <th>@Messages("image")</th>
  //          <th>@Messages("name")</th>
  //          <th>@Messages("year")</th>
  //          <th>@Messages("place")</th>
  //          <th>@Messages("city")</th>
  //          <th>@Messages("user")</th>
  //          <th>@Messages("coordinates")</th>
  //          <th>@Messages("page")</th>
  //          <th>@Messages("type")</th>
  //          <th>@Messages("commons")</th>
  //          <th>@Messages("resolution")</th>
  //        </tr>
  //      </thead>
  //      <tbody>
  //      </tbody>
  //    </table>

  // }


}
