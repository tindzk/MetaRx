package pl.metastack.metarx

import scala.collection.mutable.ArrayBuffer

class SubTest extends CompatTest {
  test(":=") {
    val x = Var(23)
    val y = Var(404)

    val subscriber = Sub[Int](0)

    val values = ArrayBuffer.empty[Int]
    subscriber.attach(values += _)

    subscriber := x
    assertEquals(values, Seq(0, 23))

    x := 42
    assertEquals(values, Seq(0, 23, 42))

    subscriber := y
    assertEquals(values, Seq(0, 23, 42, 404))

    x := 21
    assertEquals(values, Seq(0, 23, 42, 404))

    subscriber.detach()

    y := 21
    assertEquals(values, Seq(0, 23, 42, 404))

    subscriber ! 200
    assertEquals(values, Seq(0, 23, 42, 404, 200))

    subscriber := x + y
    assertEquals(values, Seq(0, 23, 42, 404, 200, 42))

    y := 500
    assertEquals(values, Seq(0, 23, 42, 404, 200, 42, 521))

    x := 100
    assertEquals(values, Seq(0, 23, 42, 404, 200, 42, 521, 600))
  }

  test("get") {
    val x = Var(5.0)
    val y = Sub(6.0)

    y := x + 5.0
    assertEquals(y.get, x.get + 5.0)

    y := 8.0
    x := 10.0
    assertEquals(y.get, 8.0)
  }
}
