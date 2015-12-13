package pl.metastack.metarx

import org.scalatest.{Assertion, FunSuite}

trait CompatTest extends FunSuite {
  def assertEquals[T](a: T, b: T): Assertion = assert(a == b)
}
