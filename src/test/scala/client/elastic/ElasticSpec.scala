package client.elastic

import org.scalawiki.dto.Page
import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.databind.ser.impl.{SimpleBeanPropertyFilter, SimpleFilterProvider}
import com.fasterxml.jackson.databind.{ObjectMapper, ObjectWriter}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.DocumentSource
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ElasticSpec extends Specification with NoTimeConversions {

  def await[T](future: Future[T])(implicit timeout: Duration = 10.seconds) = Await.result(future, timeout)


  val client = ElasticMw.connect()
//  val mapper = new ObjectMapper()
//  mapper.registerModule(DefaultScalaModule)

  "Search page" should {
    "should find on page" in {

      val page = Page(123, 0, "TestPage")

      await(client.execute {
        index into "uk.wikipedia/pages" doc PageSource(page)
      })

        eventually {
          val result = await(client.execute { search("uk.wikipedia/pages") })

          val strings = result.getHits.getHits.map(_.getSourceAsString)
          val dtos =  strings.map(s => PageSource.mapper.readValue(s, classOf[Page]))

          dtos.size === 1
        }
      }

    }

}


class PageSource(any: Any) extends DocumentSource {
  def json: String = PageSource.writer.writeValueAsString(any)
}

object PageSource {
  val filters = new SimpleFilterProvider().addFilter("Page", SimpleBeanPropertyFilter.serializeAllExcept("history"))

  val mapper = new ObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.addMixIn(classOf[Page], classOf[PageMixIn])
  mapper.setConfig(mapper.getSerializationConfig.withFilters(filters))
//  mapper.setConfig(mapper.getDeserializationConfig.withView(classOf[Page]))

  val writer = mapper.writer[ObjectWriter](filters)

  def apply(any: Any) = new PageSource(any)
}

@JsonFilter("Page")
//@JsonIgnoreProperties( "history" )
trait PageMixIn