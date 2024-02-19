package org.scalawiki.dto.markup

case class Table(
    headers: Iterable[String],
    data: Iterable[Iterable[String]],
    title: String = "",
    cssClass: String = "wikitable sortable"
) {

  def asWiki = {
    "{|" +
      (if (cssClass.nonEmpty) s" class='$cssClass'" else "") +
      (if (title.nonEmpty) "\n|+ " + title else "") +
      (if (headers.nonEmpty) headers.mkString("\n! ", " !! ", "") else "") +
      (if (data.nonEmpty)
         data
           .map { row =>
             row.mkString("| ", " || ", "")
           }
           .mkString("\n|-\n", "\n|-\n", "")
       else "") +
      "\n|}"
  }

  def asHtml = {
    "<table>" +
      (if (cssClass.nonEmpty) s" class='$cssClass'" else "") +
      (if (title.nonEmpty) s"\n<caption> $title </caption>" else "") +
      (if (headers.nonEmpty) {
         headers
           .map(h => s"  <th> $h </th>")
           .mkString("\n<thead>\n<tr>\n", "\n", "\n</tr>\n</thead>")
       } else "") +
      (if (data.nonEmpty)
         data
           .map { row =>
             row
               .map(c => s"  <td> $c </td>")
               .mkString("<tr>\n", "\n", "\n</tr>")
           }
           .mkString("\n<tbody>\n", "\n", "\n</tbody>")
       else "") +
      "\n</table>"
  }
}
