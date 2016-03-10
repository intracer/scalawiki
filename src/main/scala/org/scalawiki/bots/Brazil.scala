package org.scalawiki.bots

import akka.actor.ActorSystem
import org.jsoup.Jsoup
import org.scalawiki.MwBot
import org.scalawiki.http.HttpClientSpray
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

object Brazil {

  def msg2014(user:String) = s"""== Convite - Concurso Wiki Loves Earth Brasil 2015 ==
                  |{| width="100%" cellpadding="0" cellspacing="0" style="background-color:transparent; color:#A2B5CD; border:1px solid #A2B5CD; margin-top:0;"
                  ||-
                  || style="width:100px;" |[[Image:WLE Austria Logo (no text).svg|100px|Wiki Loves Earth Logo]]
                  || style="text-align:center;" | <div style="background-color:; border:solid 0px; font-size:150%; line-height:100%; padding:10px;"><span style="color:#000000;">Bem vindo ao<br />'''Wiki Loves Earth Brasil 2015'''<br />Patrimônio Natural</span></div>
                  || style="width:600px; text-align:right;" |[[Image:Amanhecer_no_Hercules_--.jpg|x100px|Parque Nacional da Serra dos Órgãos]] [[Image:Velejando_nas_nuvens.jpg|x100px]] [[Image:Baía_de_Guanabara_vista_do_alto_do_Corcovado.jpg|x100px]]
                  ||}
                  |
                  |Olá $user,
                  |
                  |No ano anterior você participou da primeira edição do concurso fotográfico Wiki Loves Earth Brasil enviando ótimas fotografias!
                  |
                  |O concurso foi um grande sucesso graça a sua participação e ajuda!
                  |
                  |Este ano estamos repetindo o sucesso do ano anterior, já são mais de 1.000 participantes e mais de 7.500 fotos recebidas até o momento.
                  |
                  |Você poderá participar do concurso [https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:Wiki_Loves_Earth_2015/Brasil Wiki Loves Earth Brasil 2015] até o dia 31 de Maio e concorrer a R$$9.000,00 em premiações!
                  |
                  |Esta ano, estamos premiando 2 categorias, categoria melhor foto onde premiaremos as 3 melhores fotos do concurso e a categoria melhores contribuições onde premiaremos os usuários que mandarem a maior quantidade de fotos úteis.
                  |Até o momento os 3 participantes que enviaram mais fotografias enviaram respectivamente 356, 297 e 93 fotografias.
                  |
                  |Além da premiação em dinheiro, as melhores fotografias serão publicadas na edição de Agosto da revista impressa [http://fotografemelhor.com.br/ Fotografe Melhor] da Editora Europa.
                  |
                  |Agradecemos a sua participação desde já e obrigado pelas fotos enviadas no ano anterior!
                  |
                  |Para maiores informações e para submeter suas novas fotografias, acesse o [https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:Wiki_Loves_Earth_2015/Brasil site do concurso] e participe!
                  |
                  |Contribua também divulgando para seus amigos e colegas e também curtindo nossa página no [https://www.facebook.com/wikilovesearthbrasil Facebook] e seguindo nossa conta no [https://twitter.com/WLMBRASIL Twitter].
                  |
                  |Em breve anunciaremos novidades sobre  os vencedores e a exposição das melhores fotografias do concurso.
                  |
                  |Atenciosamente,
                  |
                  |Comitê organizador do Wiki Loves Earth Brasil 2015
                  |
                  |Grupo Wikimedia Brasileiro de Educação e Pesquisa, [[User:Rodrigo Padula|Rodrigo Padula]] ([[User talk:Rodrigo Padula|talk]]). <small>message left by [[User:IlyaBot|]]</small>.""".stripMargin


  def msg2015(user:String) = s"""== Reta final - Concurso Wiki Loves Earth Brasil 2015 ==
                               |{| width="100%" cellpadding="0" cellspacing="0" style="background-color:transparent; color:#A2B5CD; border:1px solid #A2B5CD; margin-top:0;"
                               ||-
                               || style="width:100px;" |[[Image:WLE Austria Logo (no text).svg|100px|Wiki Loves Earth Logo]]
                               || style="text-align:center;" | <div style="background-color:; border:solid 0px; font-size:150%; line-height:100%; padding:10px;"><span style="color:#000000;">Bem vindo ao<br />'''Wiki Loves Earth Brasil 2015'''<br />Patrimônio Natural</span></div>
                               || style="width:600px; text-align:right;" |[[Image:Amanhecer_no_Hercules_--.jpg|x100px|Parque Nacional da Serra dos Órgãos]] [[Image:Velejando_nas_nuvens.jpg|x100px]] [[Image:Baía_de_Guanabara_vista_do_alto_do_Corcovado.jpg|x100px]]
                               ||}
                               |
                               |Olá $user,
                               |
                               |Já estamos chegando à reta final do concurso [https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:Wiki_Loves_Earth_2015/Brasil Wiki Loves Earth Brasil 2015 - Patrimônio Natural]!
                               |
                               |Você ainda poderá submeter mais fotos até o dia 31 de maio e ampliar suas chances na premiação de até R$$2.000,00!
                               |
                               |Estamos premiando 2 categorias, categoria melhor foto onde premiaremos as 3 melhores fotos do concurso e a categoria melhores contribuições onde premiaremos os usuários que mandarem a maior quantidade de fotos úteis.
                               |Até o momento os 3 participantes que enviaram mais fotografias enviaram respectivamente 356, 297 e 93 fotografias.
                               |
                               |Além da premiação em dinheiro, as melhores fotografias serão publicadas na edição de Agosto da revista impressa [http://fotografemelhor.com.br/ Fotografe Melhor] da Editora Europa.
                               |
                               |Agradecemos a sua participação desde já, obrigado  pelas fotos enviadas, elas serão usadas em  vários dos nossos projetos.
                               |
                               |Para melhorar suas chances envie mais fotos para o concurso. acesse o site do [https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:Wiki_Loves_Earth_2015/Brasil concurso] e submeta mais fotografias!
                               |
                               |Contribua também divulgando para seus amigos, contatos e curtindo nossa página no [https://www.facebook.com/wikilovesearthbrasil Facebook] e seguindo nossa conta no [https://twitter.com/WLMBRASIL Twitter].
                               |
                               |Em breve anunciaremos novidades sobre  os vencedores e a exposição das melhores fotografias do concurso.
                               |
                               |Atenciosamente,
                               |
                               |Comitê organizador do Wiki Loves Earth Brasil 2015
                               |
                               |Grupo Wikimedia Brasileiro de Educação e Pesquisa, [[User:Rodrigo Padula|Rodrigo Padula]] ([[User talk:Rodrigo Padula|talk]]). <small>message left by [[User:IlyaBot|]]</small>.""".stripMargin

  def main(args: Array[String]) {
    val system = ActorSystem()
    val http = new HttpClientSpray(system)
    val bot = MwBot.get(MwBot.commons)

    def message(user: String): Unit = {
      bot.page("User_talk:" + user).edit(msg2015(user), section = Some("new"))
    }

    http.get("https://tools.wmflabs.org/ptwikis/WLE2015:Brazil").foreach {
      html =>
        val doc = Jsoup.parse(html)
        val users = doc.select("table a").asScala.map(_.text())
//        val users = Seq("Otavioneto33", "Bevieira", "Nochita", "Fabiancm")
        users.foreach(message)
    }

        val user = "Ilya"
//    message(user)
  }

}
