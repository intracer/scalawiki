package client.board

import org.specs2.mutable.Specification

import scala.io.Source

class SecretaryBotSpec extends Specification {

  val date =  "24 грудня 2014"
  val title = "Протокол засідання Правління" + " " + date

  val present = """
== Присутні ==
;Члени правління
# Amakuha
# Antanana
# Friend
# Ilya
# NickK
# Юрій Булка
# Ліонкінг (телефоном)
;Члени РК
# Ahonc
# Base
;Члени Організації
# Ата
# Kharkivian
# Учитель
"""

  "protocol" should {
    "contain present board members" in {

      val protocol = SecretaryBot.parseProtocolXmlDom(title, present, date)

      val board = protocol.present.head

      board.name === "Члени правління"
      board.users.size === 7
      board.users.map(_.login) === Set("Amakuha", "Antanana","Friend","Ilya","NickK","Юрій Булка", "Ліонкінг (телефоном)")
    }

    "contains resolutions" in {

      val source = Source.fromURL(getClass.getResource("/client/board/protocol2.wiki"))
      val text = source.mkString

      val protocol = SecretaryBot.parseProtocolXmlDom(title, text, date)

      protocol.resolutions.size === 6

    }
  }

}
