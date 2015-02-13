package client.board

class Votes(
             val support: UserGroup,
             val oppose: UserGroup,
             val abstain: UserGroup) {

  def passed = support.users.size > 3

  def unanimous = support.users.size == 7

  override def toString: String = {
    if (!unanimous)
      Seq(support, oppose, abstain).mkString("", "; ", "")
    else "Одностайно"
  }
}

object Votes {
  def apply(userGroups: Seq[UserGroup]): Votes = {
    val byName = userGroups.groupBy(_.name.toLowerCase.trim)
    //    val support = user
    new Votes(
      byName.get("«за»").fold(UserGroup.empty("«За»"))(_.head),
      byName.get("«проти»").fold(UserGroup.empty("«Проти»"))(_.head),
      byName.get("«утримались»").fold(UserGroup.empty("«Утримались»"))(_.head))
  }

}