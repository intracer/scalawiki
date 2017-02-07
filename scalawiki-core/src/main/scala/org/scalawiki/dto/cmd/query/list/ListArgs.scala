package org.scalawiki.dto.cmd.query.list



object ListArgs {

  def toDsl(module: String, title: Option[String], pageId: Option[Long], namespaces:Set[Int], limit: Option[String]): Option[ListArg] = {

    val f = Seq(title.toSeq, pageId.toSeq, namespaces).flatten

    module match {
      case "categorymembers" =>
        Some(new CategoryMembers(title, pageId, namespaces, limit))
      case "embeddedin" =>
        Some(new EmbeddedIn(title, pageId, namespaces, limit))
      case _ => None
    }
  }
}
