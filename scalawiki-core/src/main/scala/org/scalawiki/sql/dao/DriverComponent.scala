package org.scalawiki.sql.dao

import slick.driver.JdbcProfile

trait DriverComponent {
  val driver: JdbcProfile
}
