package org.scalawiki.dto

case class User(id: Option[Long], login: Option[String]) extends Contributor  {

  override def name  = login

}

object User {
  def apply(id: Long, login: String) = new User(Some(id), Option(login))
}


