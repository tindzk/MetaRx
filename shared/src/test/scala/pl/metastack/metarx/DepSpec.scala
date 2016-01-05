package pl.metastack.metarx

import org.scalatest.{Matchers, WordSpec}

class DepSpec extends WordSpec with Matchers {
  "Dep" when {
    val x: Sub[Double] = Sub(0.0)
    val width: Sub[Double] = Sub(0.0)

    "creating simple dependencies" should {
      val right = x.dep(_ + width.get, _ - width)

      "be initialized to zero" in {
        right.get should be(0.0)
      }
      "update `right` when `width` changes" in {
        width := 50.0
        width.get should be(50.0)
        right.get should be(50.0)
        x.get should be(0.0)
      }
      "update `right` when `x` changes" in {
        x := 25.0
        width.get should be(50.0)
        right.get should be(75.0)  // x + width
      }
      "update `x` when `right` changes" in {
        right := 100.0
        x.get should be(50.0)  // right - width
        width.get should be(50.0)
      }
      "update `x` and `right` when `width` changes" in {
        width := 25.0
        right.get should be(100.0)
        x.get should be(75.0)
      }
    }
  }
}