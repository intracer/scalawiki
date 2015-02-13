package client.wlx.metadata


case class Entity(name: String, properties: Seq[Property[_]]) {

}


case class Property[T](name: String, _type: Class[T], value: Option[T] = None) {

}

case class Value[T](code: String, value: T) {

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
