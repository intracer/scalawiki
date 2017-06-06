package org.scalawiki.wlx.dto

trait HasImagesCategory {
  def imagesCategory: String
}

class ContestType(val code: String, val name: String) extends HasImagesCategory {
  override def imagesCategory: String = "Category:Images from " + name
}

object ContestType {
  val WLM = new ContestType("wlm", "Wiki Loves Monuments")
  val WLE = new ContestType("wle", "Wiki Loves Earth")
  val ESPC = new ContestType("espc", "European Science Photo Competition")

  val all = Seq(WLM, WLE, ESPC)

  def byName(name: String): Option[ContestType] =
    all.find(_.name == name)

  def byCode(code: String): Option[ContestType] =
    all.find(_.code == code)

}
