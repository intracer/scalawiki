package org.scalawiki.dto.markup


class Table(
             val title: String,
             val headers: Seq[String],
             val data: Seq[Seq[String]],
             val cssClass: String = "wikitable sortable") {


  def asWiki = {
    s"{| class='$cssClass'\n" +
      s"|+ $title\n" +
      headers.mkString("! ", " !! ", "") +
      data.map(row => row.mkString("| ", " || ", "")).mkString("\n|-\n", "\n|-\n", "\n|}")
  }


//  def asHtml = {
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
