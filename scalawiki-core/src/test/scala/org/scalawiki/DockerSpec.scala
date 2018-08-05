package org.scalawiki

import java.io.File

import org.scalawiki.dto.Site
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll

import scala.language.postfixOps
import scala.sys.process._

trait WithDocker extends BeforeAfterAll {

  val install = "docker exec scalawiki_mediawiki_1 " +
    "php maintenance/install.php SomeWiki admin --pass 123 " +
    "--dbserver database --dbuser wikiuser --dbpass example --installdbpass root_pass --installdbuser root " +
    "--server http://localhost:8080 --scriptpath="

  def checkMysql() = {
    Seq("docker", "exec", "scalawiki_database_1",
      "mysql", "--user=root", "--password=root_pass", "-s", "-e", "use my_wiki") ! ProcessLogger(_ => (), _ => ())
  }

  override def beforeAll: Unit = {
    s"docker-compose rm -fsv" !

    s"docker-compose up -d" !

    println(s"waiting for mysql to be alive")
    while (checkMysql() != 0) {
      Thread.sleep(1000)
    }

    install !
  }

  override def afterAll: Unit = {
    s"docker-compose down" !
  }
}

class DockerSpec extends Specification with WithDocker {
  "docker" should {
    "check mediawiki version" in {
      val bot = MwBot.fromSite(Site.localhost)
      bot.mediaWikiVersion.version === "1.31"
    }
  }
}
