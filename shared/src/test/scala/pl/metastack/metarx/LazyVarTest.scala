package pl.metastack.metarx

import org.scalatest.FunSuite

import scala.collection.mutable.ArrayBuffer

class LazyVarTest extends FunSuite {
  test("apply()") {
    var v = 23
    val ch = LazyVar(v)

    val values = ArrayBuffer.empty[Int]
    ch.attach(values += _)

    assert(values == Seq(23))

    v = 24
    ch.produce()

    assert(values == Seq(23, 24))
  }

  test("map()") {
    var v = 23
    val ch = LazyVar(v)
    val map = ch.map(_ + 1)

    val values = ArrayBuffer.empty[Int]
    map.attach(values += _)
    assert(values == Seq(24))

    map.map(_ + 2).attach(values += _)
    assert(values == Seq(24, 26))

    v = 24
    ch.produce()
    assert(values == Seq(24, 26, 25, 27))
  }

  test("filter()") {
    val ch = LazyVar(42)
    val filter = ch.filter(_ % 2 == 0)

    var values = ArrayBuffer.empty[Int]
    filter.map(_ * 2).attach(value => values += value)
    assert(values == Seq(84))

    ch ! 1
    assert(values == Seq(84))

    ch ! 2
    assert(values == Seq(84, 4))
  }

  test("take()") {
    val ch = LazyVar(42)
    val take = ch.take(2)

    val values = ArrayBuffer.empty[Int]
    take.map(_ + 1).attach(values += _)
    assert(values == Seq(43))

    ch ! 1
    assert(values == Seq(43, 2))

    ch ! 1
    assert(values == Seq(43, 2))
  }

  /*
  test("+()") {
    val ch = LazyVar(42)
    val childCh = ch + ((_: Int) => ())

    var sum = 0
    childCh.attach(sum += _)
    expect(sum).toBe(42)
  }*/
}
