package org.scalawiki.dto.markup


case class Table(
                  headers: Iterable[String],
                  data: Iterable[Iterable[String]],
                  title: String = "",
                  cssClass: String = "wikitable sortable") {


  def asWiki = {
    "{|" +
      (if (cssClass.isEmpty) "" else s" class='$cssClass'") +
      (if (title.isEmpty) "" else s"\n|+ $title") +
      (if (headers.isEmpty) "" else headers.mkString("\n! ", " !! ", "")) +
      (if (data.isEmpty) ""
      else {
        data.map {
          row =>
            row.mkString("| ", " || ", "")
        }.mkString("\n|-\n", "\n|-\n", "")
      }) +
      "\n|}"


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
