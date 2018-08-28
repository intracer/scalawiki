package org.scalawiki.wlx.stat

import org.scalacheck.{Gen, Properties}
import org.scalacheck.Prop.forAll
import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.{Contest, ContestType, Country, UploadConfig}

object AuthorsMonumentsSpecification {

  val country = Country.Ukraine

  val startYear = 2013

  val contest = new Contest(ContestType.WLE, country, startYear, uploadConfigs = Seq.empty[UploadConfig])

  def genName: Gen[String] = Gen.alphaNumStr

  def genRegionIds = Gen.someOf(country.regionIds).map(_.toSeq)

  def genMonumentIds(regionId: String) =
    for (number <- Gen.choose(0, 100);
         indexes <- Gen.someOf(1 to number);
         index <- indexes)
      yield
        regionId + "-" + index


  def genImages(user: String, monumentId: String, year: Int) = Gen.choose(0, 100).map { number =>
    (1 to number).map(i => Image(s"File:Year_$year User_$user Monument_$monumentId Number_$i"))
  }

  def genYears = Gen.choose(0, 10).map(years => startYear until startYear + years)

  def genUsers = Gen.listOf(genName)

  def genUserImages: Gen[Seq[Image]] = {
    for (years <- genYears;
         year <- years;
         user <- genName;
         regionIds <- genRegionIds;
         regionId <- regionIds;
         monumentIds <- genMonumentIds(regionId);
         monumentId <- monumentIds;
         images <- genImages(user, monumentId, year)
    ) yield images
  }
}
