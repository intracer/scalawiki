package org.scalawiki.wlx.dto.lists

import scala.collection.immutable.ListMap

trait ListConfig {
  def templateName: String

  def namesMap: Map[String, String]

}

object EmptyListConfig extends ListConfig {
  override def templateName: String = "???"

  override def namesMap: Map[String, String] = Map.empty
}

object WleUa extends ListConfig {


  override val namesMap = ListMap(
    "ID" -> "ID",
    "name" -> "назва",
    "resolution" -> "постанова",
    "place" -> "розташування",
    "user" -> "користувач",
    "area" -> "площа",
    "lat" -> "широта",
    "lon" -> "довгота",
    "type" -> "тип",
    "subType" -> "підтип",
    "photo" -> "фото",
    "gallery" -> "галерея"
  )

  override val templateName: String = "ВЛЗ-рядок"
}

object WlmUa extends ListConfig {
  override val namesMap = ListMap(
    "ID" -> "ID",
    "name" -> "назва",
    "year" -> "рік",
    "city" -> "нас_пункт",
    "place" -> "адреса",
    "lat" -> "широта",
    "lon" -> "довгота",
    "type" -> "тип",
    "stateId" -> "охоронний номер",
    "photo" -> "фото",
    "gallery" -> "галерея"
  )

  override val templateName: String = "ВЛП-рядок"

}

object WleAm extends ListConfig {
  override val namesMap = ListMap(
    "ID" -> "համարանիշ",
    "photo" -> "պատկեր",
    "gallery" -> "կատեգորիա")

  override val templateName: String = "Բնության հուշարձան ցանկ"

}

object WleNp extends ListConfig {
  override val namesMap = ListMap(
    "ID" -> "number",
    "photo" -> "image",
    "gallery" -> "gallery")

  override val templateName: String = "Nepal Monument row WLE"
}

object WleRu extends ListConfig {
  override val namesMap = ListMap(
    "ID" -> "knid",
    "name" -> "name",
    "description" -> "description",
    "article" -> "wiki",
    "lat" -> "lat",
    "lon" -> "long",
    "type" -> "type",
    "photo" -> "image",
    "regionCode" -> "region",
    "gallery" -> "commonscat")
  override val templateName: String = "monument"
}

object WleTh extends ListConfig {
  override val namesMap = ListMap(
    "ID" -> "ลำดับ",
    "name" -> "อุทยานแห่งชาติ",
    "area" -> "พื้นที่",
    "year" -> "ปีที่จัดตั้ง",
    "photo" -> "ไฟล์",
    "gallery" -> "commonscat")
  override val templateName: String = "อุทยานแห่งชาติในประเทศไทย WLE"
}


object WleCh extends ListConfig {
  override val namesMap = ListMap(
    "ID" -> "Nr",
    "name" -> "Object",
    "article" -> "enlink",
    "type" -> "Type",
    "photo" -> "Photo",
    "place" -> "Canton1",
    "gallery" -> "Commonscat")
  override val templateName: String = "Naturalistic heritage CH row"
}

object WleAu extends ListConfig {
  override val namesMap = ListMap(
    "ID" -> "number",
    "name" -> "Name",
    "date" -> "Datum",
    "place" -> "Bundesland", // "Bezirk" , "Gemeinde", "Anzeige-Gemeinde"
    "description" -> "Beschreibung",
    "article" -> "Artikel",
    "area" -> "Fläche",
    "lat" -> "Längengrad",
    "lon" -> "Breitengrad",
    "type" -> "Typ",
    "photo" -> "Foto",
    "regionCode" -> "Region-ISO",
    "gallery" -> "Commonscat")
  override val templateName: String = "Naturdenkmal Österreich Tabellenzeile"
}

object WleEe extends ListConfig {
  override val namesMap = ListMap(
    "ID" -> "kood",
    "name" -> "nimi",
    "type" -> "tüüp",
    "photo" -> "pilt"
  )
  override val templateName: String = "KKR rida"
}

object WleCat extends ListConfig {
  override val namesMap = ListMap(
    "ID" -> "codi",
    "name" -> "nom",
    "area" -> "dimensions",
    "lat" -> "lat",
    "lon" -> "lon",
    "photo" -> "imatge",
    "regionCode" -> "regió",
    "gallery" -> "commonscat")
  override val templateName: String = "filera patrimoni naturalк"
}



