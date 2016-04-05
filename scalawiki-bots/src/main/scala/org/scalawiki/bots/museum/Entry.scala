package org.scalawiki.bots.museum

/**
  * Upload entry
  *
  * @param dir          directory
  * @param article      wikipedia article
  * @param wlmId        Wiki Loves Monuments Id
  * @param images       images in the directory
  * @param descriptions image descriptions
  */
case class Entry(dir: String,
                 article: Option[String],
                 wlmId: Option[String],
                 images: Seq[String],
                 descriptions: Seq[String],
                 text: Option[String] = None)
