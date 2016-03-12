spray-cookies
=============

A cookiejar exploration for spray-client

spray-cookies implements a cookiejar that can be plugged in to a spray-client pipeline. The cookiejar itself is mutable so that it remembers the current state

basic usage:

The pipeline example from spray-client at http://spray.io/documentation/1.2.1/spray-client/ is as follows:

```
import spray.http._
import spray.json.DefaultJsonProtocol
import spray.httpx.encoding.{Gzip, Deflate}
import spray.httpx.SprayJsonSupport._
import spray.client.pipelining._

case class Order(id: Int)
case class OrderConfirmation(id: Int)

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val orderFormat = jsonFormat1(Order)
  implicit val orderConfirmationFormat = jsonFormat1(OrderConfirmation)
}
import MyJsonProtocol._

implicit val system = ActorSystem()
import system.dispatcher // execution context for futures

val pipeline: HttpRequest => Future[OrderConfirmation] = (
  addHeader("X-My-Special-Header", "fancy-value")
  ~> addCredentials(BasicHttpCredentials("bob", "secret"))
  ~> encode(Gzip)
  ~> sendReceive
  ~> decode(Deflate)
  ~> unmarshal[OrderConfirmation]
)
val response: Future[OrderConfirmation] =
  pipeline(Post("http://example.com/orders", Order(42)))

```

To store cookies received from the http response on this pipeline you can use the withCookies function, which takes a cookiejar and the inner sendReive pipeline as arguments. The above example then becomes

```
  case class Order(id: Int)
  case class OrderConfirmation(id: Int)

  object MyJsonProtocol extends DefaultJsonProtocol {
    implicit val orderFormat = jsonFormat1(Order)
    implicit val orderConfirmationFormat = jsonFormat1(OrderConfirmation)
  }
  import MyJsonProtocol._

  implicit val system = ActorSystem()
  import system.dispatcher // execution context for futures

  val cookieJar = new CookieJar(DefaultEffectiveTldList)
  val cookied = CookieHandling.withCookies(Some(cookieJar), Some(cookieJar)) _

  val pipeline: HttpRequest ⇒ Future[OrderConfirmation] = (
    addHeader("X-My-Special-Header", "fancy-value")
    ~> addCredentials(BasicHttpCredentials("bob", "secret"))
    ~> cookied(encode(Gzip)
      ~> sendReceive
      ~> decode(Deflate))
    ~> unmarshal[OrderConfirmation]
  )
  val response: Future[OrderConfirmation] =
    pipeline(Post("http://example.com/orders", Order(42)))

```

[![Build Status](https://travis-ci.org/martijnhoekstra/spray-cookies.svg)](https://travis-ci.org/martijnhoekstra/spray-cookies)
