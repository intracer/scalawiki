package client

import java.security.cert.X509Certificate
import javax.net.ssl.{KeyManager, X509TrustManager, SSLContext}

import akka.actor.{ActorRef, Props, ActorSystem}
import spray.http._
import spray.httpx.marshalling.Marshaller
import scala.concurrent.Future
import spray.client.pipelining._
import spray.httpx.encoding.Gzip
import akka.event.Logging
import spray.http.HttpHeaders.{Cookie, `User-Agent`, `Accept-Encoding`}
import scala.concurrent.duration._


trait HttpClient {
  val timeout: Duration = 30.minutes

  def setCookies(cookies: Seq[HttpCookie])

  def get(url: String): Future[HttpResponse]

  def get(url: Uri): Future[HttpResponse]

  def post(url: String, params: (String, String)*): Future[HttpResponse] = post(url, params.toMap)

  def post(url: String, params: Map[String, String]): Future[HttpResponse]

  def post(url: Uri, params: Map[String, String]): Future[HttpResponse]

  def postMultiPart(url: String, params: Map[String, String]): Future[HttpResponse]

}

class HttpClientImpl(val system: ActorSystem) extends HttpClient {

  implicit val sys = system

  import system.dispatcher

  var cookies: Seq[HttpCookie] = Seq.empty

  //  implicit def trustfulSslContext: SSLContext = {
  //    object BlindFaithX509TrustManager extends X509TrustManager {
  //      def checkClientTrusted(chain: Array[X509Certificate], authType: String) = ()
  //
  //      def checkServerTrusted(chain: Array[X509Certificate], authType: String) = ()
  //
  //      def getAcceptedIssuers = Array[X509Certificate]()
  //    }
  //
  //    val context = SSLContext.getInstance("TLS")
  //    context.init(Array[KeyManager](), Array(BlindFaithX509TrustManager), null)
  //    context
  //  }

  //  val httpClient = system.actorOf(Props(new HttpClient {
  //    implicit def sslContextProvider = new CustomContextProvider
  //    implicit def sslEngineProvider = new CustomClientSSLEngineProvider(sslContextProvider)
  //
  //    override def createConnector(host: String, port: Int, ssl: Boolean): ActorRef =
  //      context.actorOf(Props(
  //        new HttpHostConnector(host, port, hostConnectorSettingsFor(host, port), clientConnectionSettingsFor(host, port))(sslEngineProvider) {
  //          override def tagForConnection(index: Int): Any = connectionTagFor(host, port, index, ssl)
  //        }
  //      ))
  //
  //
  //  }), "http-client")


  override def setCookies(cookies: Seq[HttpCookie]): Unit = {
    this.cookies ++= cookies
  }

  // execution context for futures
  val sendAndDecode: HttpRequest => Future[HttpResponse] = sendReceive ~> decode(Gzip)
  val log = Logging(system, getClass)
  //  val TraceLevel = Logging.LogLevel(Logging.DebugLevel.asInt + 1)

 override implicit val timeout: Duration = 5.minutes

  def submit: HttpRequest => Future[HttpResponse] = (
    addHeaders(
      Cookie(cookies),
      `Accept-Encoding`(HttpEncodings.gzip),
      `User-Agent`("ScalaMwBot/0.1")) ~>
      logRequest(log, Logging.InfoLevel)
      //      logRequest(r =>
      //        log.info(s"HttpRequest: h: ${r.headers} d:${r.entity.data.asString}")
      //      )
      //~> ((_:HttpRequest).mapEntity(_.flatMap(entity => HttpEntity(entity.contentType.withoutDefinedCharset, entity.data))))
      ~> sendReceive
      ~> decode(Gzip)
      ~> logResponse(r => log.info(s"HttpResponse: ${r.status}, ${r.headers}"))
    )

  override def get(url: String) = submit(Get(url))

  override def get(url: Uri) = submit(Get(url))

  //  implicit val UTF8FormDataMarshaller =
  //    Marshaller.delegate[FormData, String](MediaTypes.`application/x-www-form-urlencoded`) { (formData, contentType) ⇒
  //      import java.net.URLEncoder.encode
  //      val charset = "UTF-8"
  //      formData.fields.map { case (key, value) ⇒ encode(key, charset) + '=' + encode(value, charset) }.mkString("&")
  //    }

  implicit val UTF8FormDataMarshaller =
    Marshaller.delegate[FormData, String](MediaTypes.`application/x-www-form-urlencoded`) { (formData, contentType) ⇒
      Uri.Query(formData.fields: _*).render(new StringRendering, HttpCharsets.`UTF-8`.nioCharset).get
    }

  override def post(url: String, params: Map[String, String]): Future[HttpResponse] = {
    submit(Post(url, FormData(params)))
  }

  override def post(url: Uri, params: Map[String, String]): Future[HttpResponse] = {
    submit(Post(url, FormData(params)))
  }

  override def postMultiPart(url: String, params: Map[String, String]): Future[HttpResponse] = {
    val bodyParts = params.map { case (key, value) =>
      (key,
        BodyPart(
          HttpEntity(
            value),
          key)
        )

    }
    submit(Post(Uri(url) withQuery("title" -> params("title")), new MultipartFormData(bodyParts.values.toSeq)))
  }

}

//     submit(Post(baseUrl + url, FormData(Map("username" -> user, "password" -> password)))) map cookiesAndBody
