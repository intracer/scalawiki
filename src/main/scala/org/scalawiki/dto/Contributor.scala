package org.scalawiki.dto

trait Contributor {

  def id: Option[Long]

  def name: Option[String]

  def editCount: Option[Long]

  def blocked: Option[Boolean]

}

object Contributor {

  def apply(idOpt: Option[Long], nameOpt: Option[String]): Option[Contributor] = {
    (idOpt, nameOpt) match {
      case (None, None) => None
      case (Some(id), Some(name)) => Some(User(idOpt, nameOpt))
      case (Some(id), None) => Some(User(idOpt, nameOpt))
      case (None, Some(name)) =>
        val chars = name.toSet
        if ((chars -- "123456789.".toSet).isEmpty)
          Some(IpContributor(name))
        else
          Some(User(idOpt, nameOpt))
    }
  }

}