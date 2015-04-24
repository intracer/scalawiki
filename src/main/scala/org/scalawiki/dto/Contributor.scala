package org.scalawiki.dto

trait Contributor {

  def name: Option[String]

}

object Contributor {

  def apply(id: Option[Long], name: Option[String]): Option[Contributor] = {
    // TODO detect IPs
      (id, name) match {
        case (None, None) => None
        case _ => Some(User(id, name))
      }
  }

}