package org.scalawiki.wlx.dto

case class Region(code: String, name: String, override val regions: Seq[Region] = Nil)
  extends AdmDivision



