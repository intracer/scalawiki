package client.elastic

import com.sksamuel.elastic4s.ElasticClient
import org.elasticsearch.common.settings.ImmutableSettings

class ElasticMw {



}

object ElasticMw {


  def start(path: String): ElasticClient = {
    val settings = ImmutableSettings.settingsBuilder()
      .put("http.enabled", true)
      .put("path.home", path)
      .put("cluster.routing.allocation.disk.threshold_enabled", false)
      .put("script.groovy.sandbox.enabled", false)
      .put("script.disable_dynamic", true)
    ElasticClient.local(settings.build)
  }

  def connect(host: String = "localhost", port: Int = 9300) =
    ElasticClient.remote(host, port)

  def main(args: Array[String]) {
//    val path = Files.createTempDirectory("elastic").toString
//    start(path)

    connect()
  }

}
