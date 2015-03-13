package org.scalawiki.dto.cmd.query.prop

object RvPropArgs {
  val args = Seq(Ids, Flags, Timestamp, User, UserId, Size, Sha1, ContentModel, Comment, ParsedComment, Content, Tags)
  val argsByName: Map[String, RvPropArg] = args.groupBy(_.name).mapValues(_.head)

  def byNames(names: Seq[String]): Seq[RvPropArg] = {
    names.flatMap(argsByName.get)
  }
}
