package org.scalawiki.wlx.stat

import java.time.ZonedDateTime

import scala.util.Try

case class StatConfig(campaign: String,
                      years: Seq[Int] = Nil,
                      regions: Seq[String] = Nil,
                      exceptRegions: Seq[String] = Nil,
                      cities: Seq[String] = Nil,
                      exceptCities: Seq[String] = Nil,
                      rateConfig: RateConfig = RateConfig(),
                      gallery: Boolean = false,
                      fillLists: Boolean = false,
                      wrongIds: Boolean = false,
                      lowRes: Boolean = false,
                      specialNominations: Boolean = false,
                      regionalStat: Boolean = false,
                      authorsStat: Boolean = false,
                      regionalGallery: Boolean = false,
                      missingGallery: Boolean = false)

object StatParams {

  import com.concurrentthought.cla.{Args, Opt}

  val argsDefs = Args(
    "Statistics [options]",
    Seq(
      Opt.seq[Int]("[,]")(name = "year", flags = Seq("-y", "-year"), help = "contest year.") { s: String => Try(s.toInt) },
      Opt.seq[Int]("[,]")(name = "startyear", flags = Seq("-sy", "-startyear"), help = "start year.") { s: String => Try(s.toInt) },
      Opt.string(name = "campaign", flags = Seq("-campaign"), help = "upload campaign, like wlm-ua", requiredFlag = true),
      Opt.seqString("[,]")(name = "region", flags = Seq("-region"), help = "region code"),
      Opt.seqString("[,]")(name = "except regions", flags = Seq("-exceptregion"), help = "except region codes"),
      Opt.seqString("[,]")(name = "cities", flags = Seq("-city"), help = "cities"),
      Opt.seqString("[,]")(name = "exceptcities", flags = Seq("-exceptcity"), help = "except cities"),
      Opt.int(name = "new-object-rating", flags = Seq("-new-object-rating"), help = "new object rating"),
      Opt.int(name = "new-author-object-rating", flags = Seq("-new-author-object-rating"), help = "new author object rating"),
      Opt.flag(name = "number-of-authors-bonus", flags = Seq("-number-of-authors-bonus"), help = "number of authors bonus"),
      Opt.flag(name = "number-of-images-bonus", flags = Seq("-number-of-images-bonus"), help = "number of images bonus"),
      Opt.flag(name = "gallery", flags = Seq("-gallery"), help = "gallery"),
      Opt.flag(name = "fill-lists", flags = Seq("-fill-lists"), help = "fill lists"),
      Opt.flag(name = "wrong-ids", flags = Seq("-wrong-ids"), help = "report wrong ids"),
      Opt.flag(name = "low-res", flags = Seq("-low-res"), help = "report low resolution photos"),
      Opt.flag(name = "special-nominations", flags = Seq("-special-nominations"), help = "report special nominations"),
      Opt.flag(name = "regional-stat", flags = Seq("-regional-stat"), help = "report regional statistics"),
      Opt.flag(name = "authors-stat", flags = Seq("-authors-stat"), help = "report authors statistics"),
      Opt.flag(name = "regional-gallery", flags = Seq("-regional-gallery"), help = "report regional gallery"),
      Opt.flag(name = "missing-gallery", flags = Seq("-missing-gallery"), help = "report missing galleries")
    )
  )

  def parse(args: Seq[String]): StatConfig = {
    val parsed = argsDefs.parse(args)

    if (parsed.handleErrors()) sys.exit(1)
    if (parsed.handleHelp()) sys.exit(0)

    val year = parsed.values.getOrElse("year", Seq(ZonedDateTime.now.getYear)).asInstanceOf[Seq[Int]].sorted
    val startYear = parsed.values.getOrElse("startyear", Seq(year.head)).asInstanceOf[Seq[Int]].sorted

    val years = startYear.head to year.last

    StatConfig(
      campaign = parsed.values("campaign").asInstanceOf[String],
      years = years,
      regions = parsed.values.getOrElse("region", Nil).asInstanceOf[Seq[String]],
      exceptRegions = parsed.values.getOrElse("exceptregion", Nil).asInstanceOf[Seq[String]],
      cities = parsed.values.getOrElse("city", Nil).asInstanceOf[Seq[String]],
      exceptCities = parsed.values.getOrElse("exceptcities", Nil).asInstanceOf[Seq[String]],
      rateConfig = RateConfig(parsed),
      gallery = parsed.values.get("gallery").asInstanceOf[Option[Boolean]].getOrElse(false),
      fillLists = parsed.values.get("fill-lists").asInstanceOf[Option[Boolean]].getOrElse(false),
      wrongIds = parsed.values.get("wrong-ids").asInstanceOf[Option[Boolean]].getOrElse(false),
      lowRes = parsed.values.get("low-res").asInstanceOf[Option[Boolean]].getOrElse(false),
      specialNominations = parsed.values.get("special-nominations").asInstanceOf[Option[Boolean]].getOrElse(false),
      regionalStat = parsed.values.get("regional-stat").asInstanceOf[Option[Boolean]].getOrElse(false),
      authorsStat = parsed.values.get("authors-stat").asInstanceOf[Option[Boolean]].getOrElse(false),
      regionalGallery = parsed.values.get("regional-gallery").asInstanceOf[Option[Boolean]].getOrElse(false),
      missingGallery = parsed.values.get("missing-gallery").asInstanceOf[Option[Boolean]].getOrElse(false)
    )
  }
}
