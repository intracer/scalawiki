package org.scalawiki.wlx

import org.scalawiki.wlx.dto.Monument
import org.scalawiki.wlx.dto.lists.WlmUa
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
      val m = Monument.init(text1, names = WlmUa.namesMap)
      "05-105-0001" === m.id
      "[[Козятин I|Козятинський залізничний вокзал]]" === m.name
      Some("Station2 2.jpg") ===  m.photo
      Some("Koziatyn-1 Railway Station") === m.gallery
    }

    "empty" in {
      val m = Monument.init(text2, names = WlmUa.namesMap)
      "05-105-0012" === m.id
      "Братська могила 6 радянських воїнів, загиблих при звільненні міста" === m.name
      None === m.photo
      None === m.gallery
    }

  }

  "set" should {
    "photo" in  {
          val m = Monument.init(text2, names = WlmUa.namesMap)
          val photo = "Photo.jpg"
          val m2 = m.setTemplateParam("фото", photo).asInstanceOf[Monument]
          "05-105-0012" === m2.id
          "Братська могила 6 радянських воїнів, загиблих при звільненні міста" === m2.name
          Some(photo) === m2.photo
          None === m2.gallery
        }

    "gallery" in  {
      val m = Monument.init(text2, names = WlmUa.namesMap)
      val m2 = m.setTemplateParam("галерея", "123").asInstanceOf[Monument]
      "05-105-0012" === m2.id
      "Братська могила 6 радянських воїнів, загиблих при звільненні міста" === m2.name
      None === m2.photo
      Some("123") === m2.gallery
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
      val list = Monument.monumentsFromText(listText, "page", "ВЛП-рядок", names = WlmUa.namesMap).toSeq
      2 === list.size
      list(0).id === "05-105-0001"
      list(1).id === "05-105-0012"
    }

    "parse with rubbish" in {
      val list = Monument.monumentsFromText("abcd\n" + listText + "\ndef", "page", "ВЛП-рядок", names = WlmUa.namesMap).toSeq
      2 === list.size
      list(0).id === "05-105-0001"
      list(1).id === "05-105-0012"
    }
  }

}
