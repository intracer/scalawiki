package org.scalawiki.dto

// TODO overwhelming Options annoy, at some point need to decide what to do
case class User(id: Option[Long], login: Option[String]) extends Contributor  {

  override def name  = login

}


