package pl.metastack

import scala.math.{Fractional, Numeric}

package object metarx extends OptImplicits {
  implicit def FunctionToWriteChannel[T](f: T => Unit): WriteChannel[T] = {
    val ch = Channel[T]()
    ch.attach(f)
    ch
  }

  implicit class OptExtensions[T](opt: Opt[T]) {
    def :=(t: T) {
      opt := Some(t)
    }
  }

  implicit class ReadChannelBooleanExtensions(rch: ReadChannel[Boolean]) {
    def &&(other: ReadChannel[Boolean]): ReadChannel[Boolean] = {
      rch.zipWith(other)(_ && _)
    }

    def &&(argument: Boolean): ReadChannel[Boolean] = {
      rch.map(_ && argument)
    }

    def ||(other: ReadChannel[Boolean]): ReadChannel[Boolean] = {
      rch.zipWith(other)(_ || _)
    }

    def ||(argument: Boolean): ReadChannel[Boolean] = {
      rch.map(_ || argument)
    }

    def isFalse: ReadChannel[Boolean] = rch.map(!_)
    def unary_! = isFalse
    def onTrue: ReadChannel[Unit] = rch.collect { case true => Unit }
    def onFalse: ReadChannel[Unit] = rch.collect { case false => Unit }
  }

  implicit class BooleanExtensions(boolVal: Boolean) {
    def &&(rch: ReadChannel[Boolean]): ReadChannel[Boolean] = rch && boolVal
    def ||(rch: ReadChannel[Boolean]): ReadChannel[Boolean] = rch || boolVal
  }

  implicit class ReadChannelNumericExtensions[T: Numeric](rch: ReadChannel[T])(implicit num: Numeric[T]) {
    import num._

    def +(other: ReadChannel[T]): ReadChannel[T] = rch.zipWith(other)(plus)
    def +(argument: T): ReadChannel[T] = rch.map(_ + argument)
    def -(other: ReadChannel[T]): ReadChannel[T] = rch.zipWith(other)(minus)
    def -(argument: T): ReadChannel[T] = rch.map(_- argument)
    def *(other: ReadChannel[T]): ReadChannel[T] = rch.zipWith(other)(times)
    def *(argument: T): ReadChannel[T] = rch.map(_ * argument)

    def unary_-(other: ReadChannel[T]): ReadChannel[T] = rch.map(-_)

    def toInt: ReadChannel[Int] = rch.map(_.toInt)
    def toLong: ReadChannel[Long] = rch.map(_.toLong)
    def toFloat: ReadChannel[Float] = rch.map(_.toFloat)
    def toDouble: ReadChannel[Double] = rch.map(_.toDouble)
  }

  implicit class NumericExtensions[T: Numeric](value: T)(implicit num: Numeric[T]) {
    import num._

    def +(other: ReadChannel[T]): ReadChannel[T] = other.map(plus(value, _))
    def -(other: ReadChannel[T]): ReadChannel[T] = other.map(minus(value, _))
    def *(other: ReadChannel[T]): ReadChannel[T] = other.map(times(value, _))
  }

  implicit class ReadChannelIntegralExtensions[T: Integral](rch: ReadChannel[T])(implicit num: Integral[T]) {
    import num._

    def /(other: ReadChannel[T]): ReadChannel[T] = rch.zipWith(other)(quot)
    def /(arg: T): ReadChannel[T] = rch.map(_ / arg)
    def %(other: ReadChannel[T]): ReadChannel[T] = rch.zipWith(other)(rem)
  }

  implicit class IntegralExtensions[T: Integral](value: T)(implicit num: Integral[T]) {
    import num._

    def /(other: ReadChannel[T]): ReadChannel[T] = other.map(quot(value, _))
    def %(other: ReadChannel[T]): ReadChannel[T] = other.map(rem(value, _))
  }

  implicit class ReadChannelFractionalExtensions[T: Fractional](rch: ReadChannel[T])(implicit num: Fractional[T]) {
    import num._

    def /(other: ReadChannel[T]): ReadChannel[T] = rch.zipWith(other)(div)
    def /(arg: T): ReadChannel[T] = rch.map(_ / arg)
  }

  implicit class FractionalExtensions[T: Fractional](value: T)(implicit num: Fractional[T]) {
    import num._

    def /(other: ReadChannel[T]): ReadChannel[T] = other.map(div(value, _))
  }

  implicit class ReadChannelOrderingExtensions[T: Ordering](rch: ReadChannel[T])(implicit ord: Ordering[T]) {
    import ord._

    def <(other: ReadChannel[T]): ReadChannel[Boolean] = rch.zipWith(other)(_ < _)
    def <=(other: ReadChannel[T]): ReadChannel[Boolean] = rch.zipWith(other)(_ <= _)
    def <(arg: T): ReadChannel[Boolean] = rch.map(_ < arg)
    def <=(arg: T): ReadChannel[Boolean] = rch.map(_ <= arg)

    def >(other: ReadChannel[T]): ReadChannel[Boolean] = rch.zipWith(other)(_ > _)
    def >=(other: ReadChannel[T]): ReadChannel[Boolean] = rch.zipWith(other)(_ >= _)
    def >(arg: T): ReadChannel[Boolean] = rch.map(_ > arg)
    def >=(arg: T): ReadChannel[Boolean] = rch.map(_ >= arg)
  }

  implicit class OrderingExtensions[T: Ordering](value: T)(implicit ord: Ordering[T]) {
    import ord._

    def >(other: ReadChannel[T]): ReadChannel[Boolean] = other.map(value > _)
    def >=(other: ReadChannel[T]): ReadChannel[Boolean] = other.map(value >= _)
    def <(other: ReadChannel[T]): ReadChannel[Boolean] = other.map(value < _)
    def <=(other: ReadChannel[T]): ReadChannel[Boolean] = other.map(value <= _)
  }

  implicit class ReadChannelStringExtensions(rch: ReadChannel[String]) {
    def +(other: ReadChannel[String]): ReadChannel[String] = rch.zipWith(other)(_ + _)
    def +(value: String): ReadChannel[String] = rch.map(_ + value)
  }
}
