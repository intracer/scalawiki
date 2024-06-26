package org.scalawiki.wlx.stat

import java.time.ZonedDateTime

case class StatConfig(
    campaign: String,
    years: Seq[Int] = Nil,
    regions: Seq[String] = Nil,
    exceptRegions: Seq[String] = Nil,
    cities: Seq[String] = Nil,
    exceptCities: Seq[String] = Nil,
    rateConfig: RateConfig = RateConfig(),
    gallery: Boolean = false,
    fillLists: Boolean = false,
    wrongIds: Boolean = false,
    missingIds: Boolean = false,
    multipleIds: Boolean = false,
    lowRes: Boolean = false,
    specialNominations: Boolean = false,
    regionalStat: Boolean = false,
    regionalDetails: Boolean = false,
    authorsStat: Boolean = false,
    regionalGallery: Boolean = false,
    missingGallery: Boolean = false,
    placeDetection: Boolean = false,
    newMonuments: Boolean = false,
    rateInputDistribution: Boolean = false,
    mostPopularMonuments: Boolean = false,
    minMpx: Option[Float] = None,
    previousYearsGallery: Boolean = false,
    numberOfMonumentsByNumberOfPictures: Boolean = false
)

import org.rogach.scallop._

class StatParams(arguments: Seq[String]) extends ScallopConf(arguments) {
  val years = opt[List[Int]]("year", 'y', "contest year.")
  val startYear = opt[Int]("start-year", 's', "contest year.")
  val campaign = opt[String](
    "campaign",
    'c',
    "upload campaign, like wlm-ua.",
    required = true
  )
  val regions = opt[List[String]]("region", 'r', "region code")
  val exceptRegions =
    opt[List[String]](name = "except-regions", descr = "except region codes")
  val cities = opt[List[String]](name = "cities", descr = "cities")
  val exceptCities =
    opt[List[String]](name = "except-cities", descr = "except cities")
  val newObjectRating =
    opt[Double](name = "new-object-rating", descr = "new object rating")
  val newAuthorObjectRating = opt[Double](
    name = "new-author-object-rating",
    descr = "new author object rating"
  )
  val numberOfAuthorsBonus = opt[Boolean](
    name = "number-of-authors-bonus",
    descr = "number of authors bonus"
  )
  val numberOfImagesBonus = opt[Boolean](
    name = "number-of-images-bonus",
    descr = "number of images bonus"
  )
  val baseRate = opt[Double](name = "base-rate", descr = "base rate")
  val gallery = opt[Boolean](name = "gallery", descr = "gallery")
  val fillLists = opt[Boolean](name = "fill-lists", descr = "fill lists")
  val wrongIds = opt[Boolean](name = "wrong-ids", descr = "report wrong ids")
  val missingIds =
    opt[Boolean](name = "missing-ids", descr = "report missing ids")
  val multipleIds =
    opt[Boolean](name = "multiple-ids", descr = "report multiple ids")
  val lowRes =
    opt[Boolean](name = "low-res", descr = "report low resolution photos")
  val specialNominations = opt[Boolean](
    name = "special-nominations",
    descr = "report special nominations"
  )
  val regionalStat =
    opt[Boolean](name = "regional-stat", descr = "report regional statistics")
  val regionalDetails = opt[Boolean](
    name = "regional-details",
    descr = "report regional detailed statistics"
  )
  val authorsStat =
    opt[Boolean](name = "authors-stat", descr = "report authors statistics")
  val regionalGallery =
    opt[Boolean](name = "regional-gallery", descr = "report regional gallery")
  val missingGallery =
    opt[Boolean](name = "missing-gallery", descr = "report missing galleries")
  val placeDetection =
    opt[Boolean](name = "place-detection", descr = "report place detection")
  val newMonuments =
    opt[Boolean](name = "new-monuments", descr = "new monuments")
  val rateInputDistribution = opt[Boolean](
    name = "rate-input-distribution",
    descr = "rate input distribution"
  )
  val mostPopularMonuments = opt[Boolean](
    name = "most-popular-monuments",
    descr = "most popular monuments"
  )
  val minMpx = opt[Float](name = "min-mpx", descr = "minimum megapixels")
  val previousYearsGallery =
    opt[Boolean](name = "prev-years-gallery", descr = "previous years gallery")
  verify()
}

object StatParams {

  def parse(args: Seq[String]): StatConfig = {

    val conf = new StatParams(args)

    val year = conf.years.getOrElse(List(ZonedDateTime.now.getYear)).sorted
    val startYear = conf.startYear.getOrElse(year.head)

    val years = (startYear to year.last).toList

    StatConfig(
      campaign = conf.campaign(),
      years = years,
      regions = conf.regions.getOrElse(Nil),
      exceptRegions = conf.exceptRegions.getOrElse(Nil),
      cities = conf.cities.getOrElse(Nil),
      exceptCities = conf.exceptCities.getOrElse(Nil),
      rateConfig = RateConfig(conf),
      gallery = conf.gallery.getOrElse(false),
      fillLists = conf.fillLists.getOrElse(false),
      wrongIds = conf.wrongIds.getOrElse(false),
      missingIds = conf.missingIds.getOrElse(false),
      multipleIds = conf.multipleIds.getOrElse(false),
      lowRes = conf.lowRes.getOrElse(false),
      specialNominations = conf.specialNominations.getOrElse(false),
      regionalStat = conf.regionalStat.getOrElse(false),
      regionalDetails = conf.regionalDetails.getOrElse(false),
      authorsStat = conf.authorsStat.getOrElse(false),
      regionalGallery = conf.regionalGallery.getOrElse(false),
      missingGallery = conf.missingGallery.getOrElse(false),
      placeDetection = conf.placeDetection.getOrElse(false),
      newMonuments = conf.newMonuments.getOrElse(false),
      rateInputDistribution = conf.rateInputDistribution.getOrElse(false),
      mostPopularMonuments = conf.mostPopularMonuments.getOrElse(false),
      minMpx = conf.minMpx.toOption,
      previousYearsGallery = conf.previousYearsGallery.getOrElse(false)
    )
  }
}
