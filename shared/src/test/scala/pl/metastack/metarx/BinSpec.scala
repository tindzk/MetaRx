package pl.metastack.metarx

import org.scalatest.{Matchers, WordSpec}

import scala.collection.mutable.ArrayBuffer

class BinSpec extends WordSpec with Matchers {
  "Bin" when {
    val bin: Bin[Int] = Bin(0)

    val values = ArrayBuffer.empty[Int]
    val leftValues = ArrayBuffer.empty[Int]
    val rightValues = ArrayBuffer.empty[Int]

    def reset(): Unit = {
      values.clear()
      leftValues.clear()
      rightValues.clear()
    }

    "creating binary channels" should {
      "be initialised to zero" in {
        bin.get should be(0)
      }
      "subscribe to updates from main channel" in {
        bin.attach(values += _)
      }
      "subscribe to updates from `left`" in {
        bin.left.attach(leftValues += _)
      }
      "subscribe to updates from `right`" in {
        bin.right.attach(rightValues += _)
      }
      "update main channel" in {
        bin := 42
        values should be(Seq(0, 42))
        leftValues should be(Seq(0, 42))
        rightValues should be(Seq(0, 42))
      }
      "update `left`" in {
        reset()
        bin.left ! 42
        values should be(empty)
        leftValues should be(Seq(42))
        rightValues should be(empty)
        bin.get should be(42)
      }
      "update `right`" in {
        reset()
        bin.right ! 23
        values should be(empty)
        leftValues should be(empty)
        rightValues should be(Seq(23))
        bin.get should be(23)
      }
      "flush current value" in {
        reset()
        bin.attach(values += _)
        bin.left.attach(leftValues += _)
        bin.right.attach(rightValues += _)
        values should be(Seq(23))
        leftValues should be(Seq(23))
        rightValues should be(Seq(23))
      }
    }
  }
}
