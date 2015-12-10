package pl.metastack.metarx

import minitest._

object UpickleTest extends SimpleTestSuite {
  import upickle.default._
  import Upickle._

  test("Var") {
    val x = Var(23)
    assertEquals(read[Var[Int]](write(x)).get, x.get)
  }

  test("Opt") {
    val x = Opt(23)
    assertEquals(read[Opt[Int]](write(x)).get, x.get)
  }

  test("Opt (2)") {
    val x = Opt[Int]()
    assertEquals(read[Opt[Int]](write(x)).get, x.get)
  }

  test("Buffer") {
    val x = Buffer(1, 2, 3)
    assertEquals(read[Buffer[Int]](write(x)).get, x.get)
  }
}
