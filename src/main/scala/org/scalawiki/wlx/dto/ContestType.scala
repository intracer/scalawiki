package org.scalawiki.wlx.dto

class ContestType(val code: String, val name: String) {


}

object ContestType {
  val WLM = new ContestType("wlm", "Wiki Loves Monuments")
  val WLE = new ContestType("wle", "Wiki Loves Earth")
  val ESPC = new ContestType("уізс", "European Science Photo Competition")
}
