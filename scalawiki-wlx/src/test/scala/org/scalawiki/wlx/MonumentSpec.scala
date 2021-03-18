package org.scalawiki.wlx

import org.scalawiki.wlx.dto.Monument
import org.scalawiki.wlx.dto.lists.ListConfig.{WleUa, WlmUa}
import org.specs2.mutable.Specification

class MonumentSpec extends Specification {
  var text1: String =
    "{{ВЛП-рядок\n" +
      "| ID = 05-105-0001\n" +
      "| назва =  [[Козятин I|Козятинський залізничний вокзал]]\n" +
      "| рік =  1888\n" +
      "| нас_пункт = [[Козятин]]\n" +
      "| адреса =  вул. Вокзальна, 1\n" +
      "| широта = 49.705589\n" +
      "| довгота = 28.830099\n" +
      "| охоронний номер = {{Фон|#cff|655}}, {{Фон|#cfc|17-Вн/1}}\n" +
      "| тип =  А-місц.\n" +
      "| фото =  Station2 2.jpg\n" +
      "| галерея =Koziatyn-1 Railway Station\n" +
      "}}"

  var text2: String = "{{ВЛП-рядок\n" +
    "| ID = 05-105-0012\n" +
    "| назва =  Братська могила 6 радянських воїнів, загиблих при звільненні міста\n" +
    "| рік =  1974\n" +
    "| нас_пункт = [[Козятин]]\n" +
    "| адреса =  Кладовище\n" +
    "| широта = 49.695243\n" +
    "| довгота = 28.839562\n" +
    "| охоронний номер = {{Фон|#cf0|37}}\n" +
    "| тип =  І-місц.\n" +
    "| фото = \n" +
    "| галерея =\n" +
    "}}"

  var withResolution1: String = "{{ВЛЗ-рядок\n" +
    "|ID           = Vinn-3\n" +
    "|назва        = [[Іллінецький заказник|Іллінецький]]<ref name=\"500n\"/>\n" +
    "|розташування = Іллінецький район Іллінецька міська рада, Васильківська сільська рада (кв.36,37), Іллінецьке лісництво, кв.35-37, кв.47-50\n" +
    "|користувач   = Іллінецький ДЛГ\n" +
    "|площа        = 432,0\n" +
    "|широта       = \n" +
    "|довгота      = \n" +
    "|тип          = Заказник\n" +
    "|підтип       = Ботанічний\n" +
    "|фото         = Иллинци (1).jpg\n" +
    "|галерея      = \n" +
    "}}"

  var withResolution2: String = "{{ВЛЗ-рядок\n" +
    "|ID           = Vinn-17\n" +
    "|назва        = [[Володимирська Дубина]]<ref name=\"500n\">{{РпоПЗФ|500}}</ref>\n" +
    "|розташування = Жмеринський район, Браїлівська сільська рада, Гніванське лісництво, кв. 32-35\n" +
    "|користувач   = Вінницький лісо-виробничий комплекс\n" +
    "|площа        = 133,0\n" +
    "|широта       = \n" +
    "|довгота      = \n" +
    "|тип          = Заказник\n" +
    "|підтип       = Ландшафтний\n" +
    "|фото         = \n" +
    "|галерея      = Volodymyrska Dubyna\n" +
    "}}"

  var listText: String = text1 + "\n" + text2

  "parse" should {
    "full" in {
      val m = Monument.init(text1, listConfig = WlmUa).head
      m.id === "05-105-0001"
      m.name === "[[Козятин I|Козятинський залізничний вокзал]]"
      m.year === Some("1888")
      m.city === Some("[[Козятин]]")
      m.place === Some("вул. Вокзальна, 1")
      m.lat === Some("49.705589")
      m.lon === Some("28.830099")
      m.stateId === Some("{{Фон|#cff|655}}, {{Фон|#cfc|17-Вн/1}}")
      m.typ === Some("А-місц.")
      m.photo === Some("Station2 2.jpg")
      m.gallery === Some("Koziatyn-1 Railway Station")
    }

    "empty" in {
      val m = Monument.init(text2, listConfig = WlmUa).head
      m.id === "05-105-0012"
      m.name === "Братська могила 6 радянських воїнів, загиблих при звільненні міста"
      m.photo === None
      m.gallery === None
    }

  }

  "set" should {
    "photo" in {
      val m = Monument.init(text2, listConfig = WlmUa).head

      val photo = "Photo.jpg"
      val m2 = m.copy(photo = Some(photo))
      m2.id === "05-105-0012"
      m2.name === "Братська могила 6 радянських воїнів, загиблих при звільненні міста"
      m2.photo === Some(photo)
      m2.gallery === None
    }

    "gallery" in {
      val m = Monument.init(text2, listConfig = WlmUa).head
      val m2 = m.copy(gallery = Some("123"))
      m2.id === "05-105-0012"
      m2.name === "Братська могила 6 радянських воїнів, загиблих при звільненні міста"
      m2.photo === None
      m2.gallery === Some("123")
    }
  }

  "asWiki" should {

    "output default fields" in {
      val m = new Monument("page1", "id1", "name1", photo = Some("Image.jpg"), gallery = Some("category1"), listConfig = None)

      val longest = WleUa.namesMap.values.map(_.length).max

      val names = WleUa.namesMap.mapValues(_.padTo(longest, ' '))
      val keys = names.filterKeys(Set("ID", "name", "year", "description", "photo", "gallery").contains)
      val text = m.asWiki(Some("WLE-row"), pad = false)
      val lines = text.split("\\n", -1).toSeq
      lines ===
        Seq(
          "{{WLE-row",
          "|ID = id1",
          "|name = name1",
          "|photo = Image.jpg",
          "|gallery = category1",
          "}}",
          ""
        )
    }

    "output WLE fields" in {
      val m = new Monument("page1", "id1", "name1", photo = Some("Image.jpg"), gallery = Some("category1"), listConfig = Some(WleUa))

      val longest = WleUa.namesMap.values.map(_.length).max

      val names = WleUa.namesMap.mapValues(_.padTo(longest, ' '))
      val keys = names.filterKeys(Set("ID", "name", "year", "description", "photo", "gallery").contains)
      val text = m.asWiki()
      val lines = text.split("\\n", -1).toSeq
      lines ===
        Seq(
          "{{ВЛЗ-рядок",
          s"|${names("ID")} = id1",
          s"|${names("name")} = name1",
          s"|${names("resolution")} = ",
          s"|${names("place")} = ",
          s"|${names("user")} = ",
          s"|${names("area")} = ",
          s"|${names("lat")} = ",
          s"|${names("lon")} = ",
          s"|${names("type")} = ",
          s"|${names("subType")} = ",
          s"|${names("photo")} = Image.jpg",
          s"|${names("gallery")} = category1",
          s"}}",
          ""
        )
    }

    "output WLM fields" in {
      val m = new Monument("page1", "id1", "name1", photo = Some("Image.jpg"), gallery = Some("category1"), listConfig = Some(WlmUa))

      val longest = WlmUa.namesMap.values.map(_.length).max

      val names = WlmUa.namesMap.mapValues(_.padTo(longest, ' '))
      val text = m.asWiki()
      val lines = text.split("\\n", -1).toSeq

      val expected = Seq(
        "{{ВЛП-рядок",
        s"|${names("ID")} = id1",
        s"|${names("name")} = name1",
        s"|${names("year")} = ",
        s"|${names("city")} = ",
        s"|${names("cityType")} = ",
        s"|${names("place")} = ",
        s"|${names("lat")} = ",
        s"|${names("lon")} = ",
        s"|${names("stateId")} = ",
        s"|${names("type")} = ",
        s"|${names("photo")} = Image.jpg",
        s"|${names("gallery")} = category1",
        s"}}",
        ""
      )
      lines === expected
    }

  }


  //  @Test def testSetResolution1 {
  //    val m: Monument = Monument.init(withResolution1)
  //    m.addResolution
  //    "Vinn-3", m.id)
  //    "[[Іллінецький заказник|Іллінецький]]", m.name)
  //    "<ref name=\"500n\"/>", m.getTemplateParam("постанова"))
  //  }

  "monuments" should {
    "parse" in {
      val list = Monument.monumentsFromText(listText, "page", "ВЛП-рядок", listConfig = WlmUa).toSeq
      list.size === 2
      list(0).id === "05-105-0001"
      list(1).id === "05-105-0012"
    }

    "parse with rubbish" in {
      val list = Monument.monumentsFromText("abcd\n" + listText + "\ndef", "page", "ВЛП-рядок", listConfig = WlmUa).toSeq
      list.size === 2
      list(0).id === "05-105-0001"
      list(1).id === "05-105-0012"
    }
  }

}
