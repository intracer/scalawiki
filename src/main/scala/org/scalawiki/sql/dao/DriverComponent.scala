package org.scalawiki.sql.dao

import scala.slick.driver.JdbcProfile

trait DriverComponent {
  val driver: JdbcProfile
}
