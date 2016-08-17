package org.scalawiki.wlx.stat

import org.scalawiki.wlx.{ImageDB, MonumentDB}

class ReporterRegistry(stat: ContestStat) {
  import org.scalawiki.wlx.stat.{ReporterRegistry => RR}

  def monumentDbStat: Option[String] =
    stat.monumentDb.map(RR.monumentDbStat)

  def authorsMonuments: String =
    RR.authorsMonuments(stat.currentYearImageDb)

  def authorsImages: String =
    RR.authorsImages(stat.currentYearImageDb, stat.monumentDb)

  def authorsContributed: String =
    RR.authorsContributed(stat.dbsByYear, stat.totalImageDb, stat.monumentDb)

  def specialNominations(): String =
    RR.specialNominations(stat.currentYearImageDb)

  def mostPopularMonuments: String =
    new MostPopularMonuments(stat).asText

  def monumentsPictured: String =
    new MonumentsPicturedByRegion(stat).asText

  def galleryByRegionAndId: Option[String] =
    RR.galleryByRegionAndId(stat.monumentDb, stat.currentYearImageDb)

  def withArticles: Option[String] =
    RR.withArticles(stat.monumentDb)
}

object ReporterRegistry {

  def monumentDbStat(db: MonumentDB): String =
    new MonumentDbStat().getStat(Seq(db))

  def authorsMonuments(imageDb: ImageDB, rating: Boolean = false): String =
    new AuthorsStat().authorsMonuments(imageDb, rating)

  def authorsImages(imageDb: ImageDB, monumentDb: Option[MonumentDB]): String =
    new AuthorsStat().authorsImages(imageDb._byAuthor, monumentDb)

  def authorsContributed(imageDbs: Seq[ImageDB], totalImageDb: Option[ImageDB], monumentDb: Option[MonumentDB]): String =
    new AuthorsStat().authorsContributed(imageDbs, totalImageDb, monumentDb)

  def specialNominations(imageDB: ImageDB): String =
    new SpecialNominations(imageDB.contest, imageDB).specialNomination()

  def galleryByRegionAndId(monumentDb: Option[MonumentDB], imageDb: ImageDB): Option[String] =
    monumentDb.map(db => new Output()galleryByRegionAndId(db, imageDb))

  def withArticles(monumentDb: Option[MonumentDB]): Option[String] =
    monumentDb.map(db => Stats.withArticles(db).asWiki("").asWiki)
}



