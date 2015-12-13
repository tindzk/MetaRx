package pl.metastack.metarx

import org.scalatest.FunSuite

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global

class MultithreadingSpec extends FunSuite {
  test("Multithreading") {
    val ch = Var(42)
    val collected = ArrayBuffer.empty[Int]
    ch.attach(collected += _)

    val tasks = (0 until 100).map(i => Future(ch := i))
    val aggregated = Future.sequence(tasks)
    Await.result(aggregated, 15.seconds)

    assert(collected.length == 101)
  }
}
