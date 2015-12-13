package pl.metastack.metarx

import org.scalatest.FunSuite

class UpickleTest extends FunSuite {
  import upickle.default._
  import Upickle._

  test("Var") {
    val x = Var(23)
    assert(read[Var[Int]](write(x)).get == x.get)
  }

  test("Opt") {
    val x = Opt(23)
    assert(read[Opt[Int]](write(x)).get == x.get)
  }

  test("Opt (2)") {
    val x = Opt[Int]()
    assert(read[Opt[Int]](write(x)).get == x.get)
  }

  test("Buffer") {
    val x = Buffer(1, 2, 3)
    assert(read[Buffer[Int]](write(x)).get == x.get)
  }
}
