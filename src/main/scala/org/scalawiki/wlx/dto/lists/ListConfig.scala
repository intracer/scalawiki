package org.scalawiki.wlx.dto.lists

trait ListConfig {

  def namesMap: Map[String, String]

}

object WleUa extends ListConfig {
  override val namesMap = Map(
    "ID" -> "ID",
    "name" -> "назва",
    "year" -> "рік",
    "description" -> "опис",
    "city" -> "нас_пункт",
    "place" -> "розташування",
    "user" -> "користувач",
    "area" -> "площа",
    "lat" -> "широта",
    "lon" -> "довгота",
    "type" -> "тип",
    "subType" -> "підтип",
    "photo" -> "фото",
    "gallery" -> "галерея",
    "resolution" -> "постанова"
  )
}

object WlmUa extends ListConfig {
  override val namesMap = Map(
    "ID" -> "ID",
    "name" -> "назва",
    "year" -> "рік",
    "description" -> "опис",
    "city" -> "нас_пункт",
    "place" -> "адреса",
    "user" -> "користувач",
    "area" -> "площа",
    "lat" -> "широта",
    "lon" -> "довгота",
    "type" -> "тип",
    "subType" -> "підтип",
    "photo" -> "фото",
    "gallery" -> "галерея",
    "resolution" -> "постанова"
  )
}

object WleAm extends ListConfig {
  override val namesMap = Map(
    "ID" -> "համարանիշ",
    "photo" -> "պատկեր",
    "gallery" -> "կատեգորիա")
}

object WleNp extends ListConfig {
  override val namesMap = Map(
    "ID" -> "number",
    "photo" -> "image",
    "gallery" -> "gallery")
}

object WleRu extends ListConfig {
  override val namesMap = Map(
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
}

object WleCh extends ListConfig {
  override val namesMap = Map(
    "ID" -> "Nr",
    "name" -> "Object",
    "article" -> "enlink",
    "type" -> "Type",
    "photo" -> "Photo",
    "place" -> "Canton1",
    "gallery" -> "Commonscat")
}

object WleAu extends ListConfig {
  override val namesMap = Map(
    "ID" -> "number",
    "name" -> "Name",
    "date" -> "Datum",
    "place" ->  "Bundesland", // "Bezirk" , "Gemeinde", "Anzeige-Gemeinde"
    "description" -> "Beschreibung",
    "article" -> "Artikel",
    "area" -> "Fläche",
    "lat" -> "Längengrad",
    "lon" -> "Breitengrad",
    "type" -> "Typ",
    "photo" -> "Foto",
    "regionCode" -> "Region-ISO",
    "gallery" -> "Commonscat")
}

object WleEe extends ListConfig {
  override val namesMap = Map(
    "ID" -> "kood",
    "name" -> "nimi",
    "type" -> "tüüp",
    "photo" -> "pilt"
    )
}

object WleCat extends ListConfig {
  override val namesMap = Map(
    "ID" -> "codi",
    "name" -> "nom",
    "area" -> "dimensions",
    "lat" -> "lat",
    "lon" -> "lon",
    "photo" -> "imatge",
    "regionCode" -> "regió",
    "gallery" -> "commonscat")
}



