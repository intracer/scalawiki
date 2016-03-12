package org.scalawiki.dto.cmd.query.list



object ListArgs {

  def toDsl(module: String, title: Option[String], pageId: Option[Long], namespaces:Set[Int], limit: Option[String]): ListArg = {

    val f = Seq(title.toSeq, pageId.toSeq, namespaces).flatten

    module match {
      case "categorymembers" =>
        new CategoryMembers(title, pageId, namespaces, limit)
      case "embeddedin" =>
        new EmbeddedIn(title, pageId, namespaces, limit)
    }
  }
}
