package org.scalawiki.dto

case class User(id: Option[Long], login: Option[String]) extends Contributor  {

  override def name  = login

}


