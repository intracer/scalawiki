package org.scalawiki.dto.cmd.query.list

import org.scalawiki.dto.cmd.query.GeneratorArg
import org.scalawiki.dto.cmd.query.prop.{ImLimit, Images}

object ListArgs {

  def toDsl(
      module: String,
      title: Option[String],
      pageId: Option[Long],
      namespaces: Set[Int],
      limit: Option[String]
  ): Option[GeneratorArg] = {

    val f = Seq(title.toSeq, pageId.toSeq, namespaces).flatten

    module match {
      case "categorymembers" =>
        Some(new CategoryMembers(title, pageId, namespaces, limit))
      case "embeddedin" =>
        Some(new EmbeddedIn(title, pageId, namespaces, limit))
      case "images" =>
        Some(limit.map(max => Images(ImLimit(max))).getOrElse(Images()))
      case _ => None
    }
  }
}
