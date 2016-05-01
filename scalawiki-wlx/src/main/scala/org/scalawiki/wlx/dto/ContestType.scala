package org.scalawiki.wlx.dto

class ContestType(val code: String, val name: String)

object ContestType {
  val WLM = new ContestType("wlm", "Wiki Loves Monuments")
  val WLE = new ContestType("wle", "Wiki Loves Earth")
  val ESPC = new ContestType("espc", "European Science Photo Competition")

  val all = Seq(WLM, WLE, ESPC)

  def byName(name: String): Option[ContestType] =
    all.find(_.name == name)
}
