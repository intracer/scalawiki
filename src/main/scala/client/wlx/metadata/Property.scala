package client.wlx.metadata


case class Entity(val name: String, properties: Seq[Property[_]]) {

}


case class Property[T](val name: String, val _type: Class[T], val value: Option[T] = None) {

}

case class Value[T](val code: String, value: T) {

}


object Entity {

  val country = new Entity("Country", Seq(
    Property("code", classOf[String]),
    Property("name", classOf[String]),
    Property("regions", classOf[Entity], Some(region))
  )
  )

  val region = new Entity("Region", Seq(
    Property("code", classOf[String]),
    Property("name", classOf[String])
  )
  )

  val monument = new Entity("Monument", Seq(
    Property("code", classOf[String]),
    Property("name", classOf[String])
  )
  )

  val image = new Entity("Image", Seq(
    Property("id", classOf[Long]),
    Property("name", classOf[String])
  )
  )




}
