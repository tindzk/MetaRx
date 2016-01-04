package pl.metastack.metarx

import org.scalatest.{Matchers, WordSpec}

class DepSpec extends WordSpec with Matchers {
  "Dep" when {
    val x: Sub[Double] = Sub(0.0)
    val width: Sub[Double] = Sub(0.0)

    "creating simple dependencies" should {
      val right = new Dep[Double](x, _ - width, x.get + width.get)

      "be initialized to zero" in {
        right.get should be(0.0)
      }
      "update when the width changes" in {
        width := 50.0
        x.get should be(0.0)
        width.get should be(50.0)
        right.get should be(50.0)
      }
      "update when x changes" in {
        x := 25.0
        right.get should be(75.0)
        width.get should be(50.0)
      }
      "update when right changes" in {
        right := 100.0
        x.get should be(50.0)
        width.get should be(50.0)
      }
      "update right properly when width changes" in {
        width := 25.0
        right.get should be(100.0)
        x.get should be(75.0)
      }
    }
  }
}