package org.scalawiki.wlx

import org.scalawiki.wlx.dto.lists.ListConfig

import scala.collection.immutable.ListMap

object IdNameConfig extends ListConfig {
  override val namesMap = ListMap(
    "ID" -> "_ID",
    "name" -> "_name"
  )

  override val templateName = "templateName"
}

object NameConfig extends ListConfig {
  override val namesMap = ListMap(
    "name" -> "_name"
  )

  override val templateName = "templateName"

}
