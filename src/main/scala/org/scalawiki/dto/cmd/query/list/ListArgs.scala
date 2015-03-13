package org.scalawiki.dto.cmd.query.list

object ListArgs {

  def toDsl(module: String, title: Option[String], pageId: Option[Int], limit: Option[String]): ListArg = {
    module match {
      case "categorymembers" =>
        val params = Seq(title.map(CmTitle), pageId.map(CmPageId), limit.map(CmLimit)).flatten
        CategoryMembers(params: _*)
      case "embeddedin" =>
        val params = Seq(title.map(EiTitle), pageId.map(EiPageId), limit.map(EiLimit)).flatten
        EmbeddedIn(params: _*)
    }
  }
}
