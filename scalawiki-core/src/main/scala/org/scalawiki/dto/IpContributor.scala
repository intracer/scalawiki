package org.scalawiki.dto

case class IpContributor(
    ip: String,
    editCount: Option[Long] = None,
    blocked: Option[Boolean] = None
) extends Contributor {
  override def name = Some(ip)
  override def id: Option[Long] = None

}
