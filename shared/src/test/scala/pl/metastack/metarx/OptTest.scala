package pl.metastack.metarx

import scala.collection.mutable

import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global

class OptTest extends CompatTest {
  test("foldLeft()") {
    val elems = mutable.ArrayBuffer.empty[Int]

    val ch = Opt(0)
    val count = ch.foldLeft(0) { case (acc, cur) => acc + 1 }

    count.attach(elems += _)
    count.attach(elems += _)

    assertEquals(elems, mutable.ArrayBuffer(1, 1))
  }

  test("flatMap()") {
    val elems = mutable.ArrayBuffer.empty[String]

    val ch = Opt(0)
    val x = Channel[String]()
    val y = ch.values.flatMap(cur => x.map(_ + cur))

    y.attach(elems += _)

    x ! "a"

    ch := 1
    x ! "b"

    ch := 2
    x ! "c"

    assertEquals(elems, mutable.ArrayBuffer("a0", "b1", "c2"))

    /* flatMap() must work with multiple attaches */
    val elems2 = mutable.ArrayBuffer.empty[String]
    y.attach(elems2 += _)
    assertEquals(elems, mutable.ArrayBuffer("a0", "b1", "c2"))
    x ! "c"
    assertEquals(elems, mutable.ArrayBuffer("a0", "b1", "c2", "c2"))
    assertEquals(elems2, mutable.ArrayBuffer("c2"))
  }

  test("size()") {
    val elems = mutable.ArrayBuffer.empty[Int]

    val ch = Opt("a")

    val size = ch.count
    size.attach(elems += _)

    ch.clear()
    ch := "b"
    ch := "c"

    assertEquals(elems, mutable.ArrayBuffer(1, 0, 1, 2))
  }

  test("values()") {
    val elems = mutable.ArrayBuffer.empty[Option[Int]]

    val ch = Opt[Int]()
    ch.attach(elems += _)

    ch := 1
    ch.clear()
    ch := 2

    assertEquals(elems, mutable.ArrayBuffer(None, Some(1), None, Some(2)))
  }

  test("map()") {
    val elements = mutable.ArrayBuffer.empty[Boolean]

    val x = Opt[Int]()
    x.map(_.isDefined).attach(elements += _)
    x ! Some(42)
    x ! None

    assertEquals(elements, mutable.ArrayBuffer(false, true, false))
  }

  test("mapValues() ... orElse()") {
    val elements = mutable.ArrayBuffer.empty[Int]

    val x = Opt[String]()
    x.mapValues[String, Int](_.toInt)
     .mapValues[Int, Int](_ * 2)
     .orElse(Var(-1))
     .attach(elements += _)
    x ! Some("42")
    x ! None

    assertEquals(elements, mutable.ArrayBuffer(-1, 84, -1))
  }

  test("mapOrElse()") {
    val elements = mutable.ArrayBuffer.empty[Int]

    val x = Opt[Int]()
    x.mapOrElse[Int, Int](_ + 1, 42).attach(elements += _)
    x ! Some(23)
    x ! None

    assertEquals(elements, mutable.ArrayBuffer(42, 24, 42))
  }

  test("mapOrElse(): lazy evaluation") {
    val elements = mutable.ArrayBuffer.empty[Int]

    var i = 41
    def f: Int = { i += 1; i }

    val x = Opt[Int]()
    val map = x.mapOrElse[Int, Int](_ + 1, f)
    assertEquals(i, 41)
    map.attach(elements += _)
    assertEquals(i, 42)

    x ! Some(23)
    x ! None

    assertEquals(elements, mutable.ArrayBuffer(42, 24, 42))
  }

  test("contains()") {
    val elements = mutable.ArrayBuffer.empty[Boolean]

    val x = Opt[Int]()
    x.contains(5).attach(elements += _)
    x ! Some(23)
    x ! Some(5)
    x ! None

    assertEquals(elements, mutable.ArrayBuffer(false, false, true, false))
  }

  test("Conversion from Future[_]") {
    val p = Promise[Int]()
    val f = p.future
    val opt = Opt.from(f)
    assertEquals(opt.get, None)
    p.success(42)
    f.onComplete { v =>
      assertEquals(opt.get, Some(42))
    }
  }

  test("Conversion from Future[Option[_]]") {
    val p = Promise[Option[Int]]()
    val f = p.future
    val opt = Opt.fromOption(f)
    assertEquals(opt.get, None)
    p.success(Some(42))
    f.onComplete { v =>
      assertEquals(opt.get, Some(42))
    }
  }
}
