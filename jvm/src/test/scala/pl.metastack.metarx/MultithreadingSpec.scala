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

    val tasks = (0 until 100).map(i => Future(ch ! i))
    val aggregated = Future.sequence(tasks)
    Await.result(aggregated, 15.seconds)

    assert(collected.length == 101)
  }

  test("Thread-safe insertions") {
    val b = Buffer[Int]()
    (0 until 1000).par.foreach(b += _)
    (0 until 1000).foreach(x => assert(b.get.contains(x)))
  }

  test("Thread-safe removals") {
    val b = Buffer[Int]()
    (0 until 1000).par.foreach(b += _)
    (0 until 1000).par.foreach(b -= _)
    assert(b.get.isEmpty)
  }
}
