package org.scalawiki.wlx.slick

import scala.slick.driver.H2Driver.simple._

class Slick {

//  val db = Database.forURL("jdbc:h2:mem:hello", driver = "org.h2.Driver")

  //  val db = Database.forURL("jdbc:h2:~/wlm_ua;AUTO_SERVER=TRUE", driver = "org.h2.Driver", user = "sa", password = "")
  val db = Database.forURL("jdbc:h2:tcp://localhost/~/wlm_ua", driver = "org.h2.Driver", user = "sa", password = "")

  val monuments = TableQuery[Monuments]
//  val images = TableQuery[Images]

  def createDdl {
    db.withSession { implicit session =>
      monuments.ddl.create
    }
  }

  def drop {
    db.withSession { implicit session =>
      monuments.ddl.drop
    }
  }

  def withSession[T](f:(Session) => T) =
    db.withSession { implicit session =>
      f(session)
    }

}


object Slick {

  def main(args: Array[String]) {
//    val slick: Slick = new Slick()
//    slick.db.withSession { implicit session =>
//      val images = slick.images.filter(_.date === "2014").list
//      val byRegion = images.groupBy(im => Monument.getRegionId(im.monumentId))
//      val regionIds = SortedSet(byRegion.keySet.toSeq:_*)
//
//      for (regionId <- regionIds)
//          println(s"${Country.Ukraine.regionById.getOrElse(regionId, "-")} ${byRegion(regionId).size}")
//
//    }
  }
}


