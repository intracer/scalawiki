package org.scalawiki.dto

case class IpContributor(ip: String) extends Contributor {
  override def name = Some(ip)
}
