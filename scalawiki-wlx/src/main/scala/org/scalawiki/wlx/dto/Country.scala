package org.scalawiki.wlx.dto

import java.util.Locale

import scala.collection.immutable.SortedSet

trait AdmDivision {
  def code: String

  def name: String

  def regionType: Option[RegionType]

  override def toString = s"$name ($code)"

  def regions: Seq[AdmDivision]

  def parent: () => Option[AdmDivision]

  def regionId(monumentId: String): String = monumentId.split("-").take(2).mkString

  def regionName(regId: String): String = if (regId == code) name else ""

  def byId(monumentId: String): Option[AdmDivision] = if (regionId(monumentId) == code) Some(this) else None

  def byName(name: String): Seq[AdmDivision] = {
    Seq(this).filter(_.name.toLowerCase == name.toLowerCase) ++ regions.flatMap(_.byName(name))
  }

  def byRegion(monumentIds: Set[String]): Map[AdmDivision, Set[String]] = {
    val entries = monumentIds.flatMap(id => byId(id).map(adm => id -> adm))

    entries
      .groupBy { case (id, adm) => adm }
      .mapValues(_.toMap.keySet)
  }

  def byIdAndName(regionId: String, rawName: String): Seq[AdmDivision] = {
    val cleanName = AdmDivision.cleanName(rawName)

    val candidates = byId(regionId).map { region =>
      val here = region.byName(cleanName)
      if (here.isEmpty) {
        region.parent().map(_.byName(cleanName)).getOrElse(Nil)
      } else {
        here
      }
    }.getOrElse(Nil)

    val types = RegionTypes.nameToType(rawName).toSet

    if (candidates.size > 1 && types.nonEmpty) {
      candidates.filter(c => c.regionType.exists(types.contains))
    } else {
      candidates
    }
  }

  def withParents(parent: () => Option[AdmDivision] = () => None): AdmDivision

  def regionsWithParents(): Seq[AdmDivision] = {
    regions.map(_.withParents(() => Some(this)))
  }
}

trait AdmRegion extends AdmDivision {
  lazy val regionIds: SortedSet[String] = SortedSet(regions.map(_.code): _*)

  lazy val regionNames: Seq[String] = regions.sortBy(_.code).map(_.name)

  lazy val regionById: Map[String, AdmDivision] = regions.groupBy(_.code).mapValues(_.head)

  override def regionName(regId: String) = byId(regId).map(_.name).getOrElse("")

  override def byId(monumentId: String): Option[AdmDivision] = {
    regionById.get(regionId(monumentId)).flatMap(region => region.byId(monumentId).orElse(Some(region)))
  }
}


object NoAdmDivision extends Country("", "")

case class Country(code: String,
                   name: String,
                   val languageCodes: Seq[String] = Nil,
                   var regions: Seq[AdmDivision] = Nil
                  ) extends AdmRegion {

  val parent: () => Option[AdmDivision] = () => None

  def withoutLangCodes: Country = copy(languageCodes = Nil)

  override def regionId(monumentId: String): String = monumentId.split("-").head

  override def withParents(parent: () => Option[AdmDivision] = () => None): AdmDivision = {
    regions = regionsWithParents()
    this
  }

  override def regionType: Option[RegionType] = None
}

case class Region(code: String, name: String,
                  var regions: Seq[AdmDivision] = Nil,
                  var parent: () => Option[AdmDivision] = () => None,
                  regionType: Option[RegionType] = None)
  extends AdmRegion {

  override def withParents(parent: () => Option[AdmDivision] = () => None): AdmDivision = {
    this.parent = parent
    this.regions = regionsWithParents()
    this
  }
}

case class NoRegions(code: String, name: String,
                     var parent: () => Option[AdmDivision] = () => None,
                     regionType: Option[RegionType] = None)
  extends AdmDivision {

  val regions: Seq[AdmDivision] = Nil

  override def withParents(parent: () => Option[AdmDivision] = () => None): AdmDivision = {
    this.parent = parent
    this
  }

}

object AdmDivision {
  def apply(code: String, name: String,
            regions: Seq[AdmDivision],
            parent: () => Option[AdmDivision],
            regionType: Option[RegionType]): AdmDivision = {
    if (regions.isEmpty) {
      NoRegions(code, name, parent, regionType)
    } else {
      Region(code, name, regions, parent, regionType)
    }
  }

  def cleanName(raw: String): String = {
    raw
      .replace("р-н", "район")
      .replace("сільська рада", "")
      .replace("селищна рада", "")
      .replace("[[", "")
      .replace("]]", "")
      .replace("&nbsp;", " ")
      .replace('\u00A0',' ')
      .replace("м.", "")
      .replace("місто", "")
      .replace("с.", "")
      .replace("С.", "")
      .replace(".", "")
      .replace("село", "")
      .replace("сел.", "")
      .replace("смт", "")
      .replace("Смт", "")
      .replace("с-ще", "")
      .replace("'''", "")
      .replace("''", "")
      .replace(",", "")
      .replace("’", "'")
      .replace("”", "'")
      .split("\\(").head
      .split("\\|").head
      .trim
  }

}

object Country {

  val Azerbaijan = new Country("AZ", "Azerbaijan", Seq("az"))

  val Ukraine: Country = new Country("UA", "Ukraine", Seq("uk"), Koatuu.regions(() => Some(Ukraine)))

  val customCountries = Seq(Ukraine)

  def langMap: Map[String, Seq[String]] = {
    Locale.getAvailableLocales
      .groupBy(_.getCountry)
      .map {
        case (countryCode, locales) =>

          val langs = locales.flatMap {
            locale =>
              Option(locale.getLanguage)
                .filter(_.nonEmpty)
          }
          countryCode -> langs.toSeq
      }
  }

  def fromJavaLocales: Seq[Country] = {

    val countryLangs = langMap

    val fromJava = Locale.getISOCountries.map { countryCode =>

      val langCodes = countryLangs.getOrElse(countryCode, Seq.empty)

      val locales = langCodes.map(langCode => new Locale(langCode, countryCode))

      val locale = locales.headOption.getOrElse(new Locale("", countryCode))

      val langs = locales.map(_.getDisplayLanguage(Locale.ENGLISH)).distinct

      new Country(locale.getCountry,
        locale.getDisplayCountry(Locale.ENGLISH),
        langCodes
      )
    }.filterNot(_.code == "ua")

    Seq(Ukraine) ++ fromJava
  }

  lazy val countryMap: Map[String, Country] =
    (fromJavaLocales ++ customCountries).groupBy(_.code.toLowerCase).mapValues(_.head)

  def byCode(code: String): Option[Country] = countryMap.get(code.toLowerCase)
}
