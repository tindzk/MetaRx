package pl.metastack.metarx

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global

import minitest._

object MultithreadingSpec extends SimpleTestSuite {

  test("Multithreading") {
    val ch = Var(42)
    val collected = ArrayBuffer.empty[Int]
    ch.attach(collected += _)

    val tasks = (0 until 100).map(i => Future(ch := i))
    val aggregated = Future.sequence(tasks)
    Await.result(aggregated, 15.seconds)

    assertEquals(collected.length, 101)
  }

}
