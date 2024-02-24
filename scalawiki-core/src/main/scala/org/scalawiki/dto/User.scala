package org.scalawiki.dto

import java.time.ZonedDateTime

case class User(
    id: Option[Long],
    login: Option[String],
    editCount: Option[Long] = None,
    registration: Option[ZonedDateTime] = None,
    blocked: Option[Boolean] = None,
    emailable: Option[Boolean] = None,
    missing: Boolean = false,
    sulAccounts: Seq[SulAccount] = Seq.empty
) extends Contributor {

  override def name = login

}

object User {
  def apply(id: Long, login: String) =
    new User(Some(id), Option(login))
}
