package spray

import spray.util.pimps.PimpedFuture

import scala.concurrent.Future

package object util {

  implicit def pimpFuture[T](fut: Future[T]): PimpedFuture[T] =
    new PimpedFuture[T](fut)
}
