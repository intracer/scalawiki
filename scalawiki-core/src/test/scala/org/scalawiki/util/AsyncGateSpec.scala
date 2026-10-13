package org.scalawiki.util

import org.specs2.mutable.Specification
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class AsyncGateSpec extends Specification {
  "AsyncGate" should {
    "release its slot when starting an operation throws synchronously" in {
      val gate = new AsyncGate(1)
      Await.result(gate.submit[Int](throw new RuntimeException("start failed")), 5.seconds) must
        throwA[RuntimeException]("start failed")
      Await.result(gate.submit(Future.successful(42)), 5.seconds) must_== 42
    }
  }
}
