package client.slick

import client.wlx.dto.Monument

import scala.slick.driver.H2Driver.simple._

class Slick {

//  val db = Database.forURL("jdbc:h2:mem:hello", driver = "org.h2.Driver")

  val db = Database.forURL("jdbc:h2:~/test", driver = "org.h2.Driver")

  db.withSession { implicit session =>


  }

}


abstract class Monuments(tag: Tag) extends Table[Monument](tag, "MONUMENTS") {
  // Auto Increment the id primary key column
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
  // The name can't be null
  def name = column[String]("NAME", O.NotNull)
  // the * projection (e.g. select * ...) auto-transforms the tupled
  // column values to / from a User
//  def * = (name, id.?) <> (Monument.tupled, Monument.unapply)
}
