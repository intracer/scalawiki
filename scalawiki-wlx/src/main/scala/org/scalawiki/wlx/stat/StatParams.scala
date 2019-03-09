package org.scalawiki.wlx.stat

import java.time.ZonedDateTime

import scala.util.Try

case class StatConfig(campaign: String,
                      years: Seq[Int] = Nil,
                      regions: Seq[String] = Nil,
                      exceptRegions: Seq[String] = Nil,
                      cities: Seq[String] = Nil,
                      exceptCities: Seq[String] = Nil,
                      newObjectRating: Option[Int] = None,
                      newAuthorObjectRating: Option[Int] = None,
                      gallery: Boolean = false)

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
      Opt.flag(name = "gallery", flags = Seq("-gallery"), help = "gallery")
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
      newObjectRating = parsed.values.get("new-object-rating").asInstanceOf[Option[Int]],
      newAuthorObjectRating = parsed.values.get("new-author-object-rating").asInstanceOf[Option[Int]],
      gallery = parsed.values.get("gallery").asInstanceOf[Option[Boolean]].getOrElse(false)
    )
  }
}
