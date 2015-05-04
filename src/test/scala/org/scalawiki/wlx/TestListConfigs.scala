package org.scalawiki.wlx

import org.scalawiki.wlx.dto.lists.ListConfig

object TestListConfigs {

}

object IdNameConfig extends ListConfig {
  override val namesMap = Map(
    "ID" -> "_ID",
    "name" -> "_name"
  )

  override val templateName = "templateName"
}

object NameConfig extends ListConfig {
  override val namesMap = Map(
    "name" -> "_name"
  )

  override val templateName = "templateName"

}

