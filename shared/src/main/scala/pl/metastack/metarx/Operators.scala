package pl.metastack.metarx

import scala.math.{Fractional, Numeric}

object Operators {
  trait Base[T, MapOut, MapResult, ZipOut] {
    def map(f: T => MapResult): ReadChannel[MapOut]
    def zipWith(other: ReadChannel[T])(f: (T, T) => MapResult): ReadChannel[ZipOut]
    def collect[U](f: PartialFunction[T, U]): ReadChannel[U]
  }

  trait DefaultBase[T, MapResult] extends Base[T, MapResult, MapResult, MapResult] {
    def ch: ReadChannel[T]

    override def map(f: T => MapResult): ReadChannel[MapResult] = ch.map(f)
    override def zipWith(other: ReadChannel[T])
                        (f: (T, T) => MapResult): ReadChannel[MapResult] =
      ch.zipWith(other)(f)
    override def collect[U](f: PartialFunction[T, U]): ReadChannel[U] =
      ch.collect(f)
  }

  trait OptionBase[T, MapResult] extends Base[T, Option[MapResult], MapResult, Option[MapResult]] {
    def ch: ReadChannel[Option[T]]

    override def map(f: T => MapResult): ReadChannel[Option[MapResult]] =
      ch.mapValues(f)

    override def zipWith(other: ReadChannel[T])
                        (f: (T, T) => MapResult): ReadChannel[Option[MapResult]] =
      ch.zipWith(other) {
        case (None, u) => None
        case (Some(t), u) => Some(f(t, u))
      }

    override def collect[U](f: PartialFunction[T, U]): ReadChannel[U] =
      ch.collect {
        case Some(s) if f.isDefinedAt(s) => f(s)
      }
  }

  trait BooleanOps[Out, ZipOut] extends Base[Boolean, Out, Boolean, ZipOut] {
    def &&(other: ReadChannel[Boolean]): ReadChannel[ZipOut] =
      zipWith(other)(_ && _)
    def &&(argument: => Boolean): ReadChannel[Out] = map(_ && argument)

    def ||(other: ReadChannel[Boolean]): ReadChannel[ZipOut] =
      zipWith(other)(_ || _)
    def ||(argument: => Boolean): ReadChannel[Out] = map(_ || argument)

    def unary_! = map(!_)
  }

  trait NumericOps[T, Out, ZipOut] extends Base[T, Out, T, ZipOut] {
    val num: Numeric[T]

    import num._

    def +(other: ReadChannel[T]): ReadChannel[ZipOut] = zipWith(other)(plus)
    def +(argument: => T): ReadChannel[Out] = map(_ + argument)
    def -(other: ReadChannel[T]): ReadChannel[ZipOut] = zipWith(other)(minus)
    def -(argument: => T): ReadChannel[Out] = map(_ - argument)
    def *(other: ReadChannel[T]): ReadChannel[ZipOut] = zipWith(other)(times)
    def *(argument: => T): ReadChannel[Out] = map(_ * argument)

    def unary_-(other: ReadChannel[T]): ReadChannel[Out] = map(-_)
  }

  trait IntegralOps[T, Out, ZipOut] extends Base[T, Out, T, ZipOut] {
    val integral: Integral[T]

    import integral._

    def /(other: ReadChannel[T]): ReadChannel[ZipOut] = zipWith(other)(quot)
    def /(arg: => T): ReadChannel[Out] = map(_ / arg)
    def %(other: ReadChannel[T]): ReadChannel[ZipOut] = zipWith(other)(rem)
  }

  trait FractionalOps[T, Out, ZipOut] extends Base[T, Out, T, ZipOut] {
    val fractional: Fractional[T]

    import fractional._

    def /(other: ReadChannel[T]): ReadChannel[ZipOut] = zipWith(other)(div)
    def /(arg: => T): ReadChannel[Out] = map(_ / arg)
  }

  trait OrderingOps[T, Out, ZipOut] extends Base[T, Out, Boolean, ZipOut] {
    val ordering: Ordering[T]

    import ordering._

    def <(other: ReadChannel[T]): ReadChannel[ZipOut] = zipWith(other)(_ < _)
    def <=(other: ReadChannel[T]): ReadChannel[ZipOut] = zipWith(other)(_ <= _)
    def <(arg: => T): ReadChannel[Out] = map(_ < arg)
    def <=(arg: => T): ReadChannel[Out] = map(_ <= arg)

    def >(other: ReadChannel[T]): ReadChannel[ZipOut] = zipWith(other)(_ > _)
    def >=(other: ReadChannel[T]): ReadChannel[ZipOut] = zipWith(other)(_ >= _)
    def >(arg: => T): ReadChannel[Out] = map(_ > arg)
    def >=(arg: => T): ReadChannel[Out] = map(_ >= arg)
  }

  trait StringOps[Out, ZipOut] extends Base[String, Out, String, ZipOut] {
    def +(other: ReadChannel[String]): ReadChannel[ZipOut] =
      zipWith(other)(_ + _)
    def +(value: => String): ReadChannel[Out] = map(_ + value)
  }
}

trait Operators {
  implicit class StringChannel(val ch: ReadChannel[String])
    extends Operators.StringOps[String, String]
    with Operators.DefaultBase[String, String]

  implicit class OptionalStringChannel(val ch: ReadChannel[Option[String]])
    extends Operators.StringOps[Option[String], Option[String]]
    with Operators.OptionBase[String, String]

  implicit class BooleanChannel(val ch: ReadChannel[Boolean])
    extends Operators.BooleanOps[Boolean, Boolean]
    with Operators.DefaultBase[Boolean, Boolean]

  implicit class OptionalBooleanChannel(val ch: ReadChannel[Option[Boolean]])
    extends Operators.BooleanOps[Option[Boolean], Option[Boolean]]
    with Operators.OptionBase[Boolean, Boolean]

  implicit class BooleanValue(value: Boolean) {
    def &&(ch: ReadChannel[Boolean]): ReadChannel[Boolean] = ch && value
    def ||(ch: ReadChannel[Boolean]): ReadChannel[Boolean] = ch || value
  }

  implicit class BooleanValue2(value: Boolean) {
    def &&(ch: ReadChannel[Option[Boolean]]): ReadChannel[Option[Boolean]] =
      ch && value
    def ||(ch: ReadChannel[Option[Boolean]]): ReadChannel[Option[Boolean]] =
      ch || value
  }

  implicit class NumericChannel[T: Numeric](val ch: ReadChannel[T])
                                           (implicit val num: Numeric[T])
    extends Operators.NumericOps[T, T, T]
    with Operators.DefaultBase[T, T] {

    import num._

    @deprecated("Use map(_.toInt) instead", "v0.1.5")
    def toInt: ReadChannel[Int] = ch.map(_.toInt)

    @deprecated("Use map(_.toLong) instead", "v0.1.5")
    def toLong: ReadChannel[Long] = ch.map(_.toLong)

    @deprecated("Use map(_.toFloat) instead", "v0.1.5")
    def toFloat: ReadChannel[Float] = ch.map(_.toFloat)

    @deprecated("Use map(_.toDouble) instead", "v0.1.5")
    def toDouble: ReadChannel[Double] = ch.map(_.toDouble)
  }

  implicit class OptionalNumericChannel[T: Numeric](val ch: ReadChannel[Option[T]])
                                                   (implicit val num: Numeric[T])
    extends Operators.NumericOps[T, Option[T], Option[T]]
    with Operators.OptionBase[T, T]

  implicit class NumericValue[T: Numeric](value: T)(implicit num: Numeric[T]) {
    import num._

    def +(other: ReadChannel[T]): ReadChannel[T] = other.map(plus(value, _))
    def -(other: ReadChannel[T]): ReadChannel[T] = other.map(minus(value, _))
    def *(other: ReadChannel[T]): ReadChannel[T] = other.map(times(value, _))
  }

  implicit class NumericValue2[T: Numeric](value: T)(implicit num: Numeric[T]) {
    import num._

    def +(other: ReadChannel[Option[T]]): ReadChannel[Option[T]] =
      other.mapValues((x: T) => plus(value, x))
    def -(other: ReadChannel[Option[T]]): ReadChannel[Option[T]] =
      other.mapValues((x: T) => minus(value, x))
    def *(other: ReadChannel[Option[T]]): ReadChannel[Option[T]] =
      other.mapValues((x: T) => times(value, x))
  }

  implicit class IntegralChannel[T: Integral](val ch: ReadChannel[T])
                                             (implicit val integral: Integral[T])
    extends Operators.IntegralOps[T, T, T]
    with Operators.DefaultBase[T, T]

  implicit class OptionalIntegralChannel[T: Integral](val ch: ReadChannel[Option[T]])
                                                     (implicit val integral: Integral[T])
    extends Operators.IntegralOps[T, Option[T], Option[T]]
    with Operators.OptionBase[T, T]

  implicit class IntegralValue[T: Integral](value: T)(implicit num: Integral[T]) {
    import num._

    def /(other: ReadChannel[T]): ReadChannel[T] = other.map(quot(value, _))
    def %(other: ReadChannel[T]): ReadChannel[T] = other.map(rem(value, _))
  }

  implicit class IntegralValue2[T: Integral](value: T)
                                            (implicit num: Integral[T]) {
    import num._

    def /(other: ReadChannel[Option[T]]): ReadChannel[Option[T]] =
      other.mapValues((t: T) => quot(value, t))
    def %(other: ReadChannel[Option[T]]): ReadChannel[Option[T]] =
      other.mapValues((t: T) => rem(value, t))
  }

  implicit class FractionalChannel[T: Fractional](val ch: ReadChannel[T])
                                                 (implicit val fractional: Fractional[T])
    extends Operators.FractionalOps[T, T, T]
    with Operators.DefaultBase[T, T]

  implicit class OptionalFractionalChannel[T: Fractional](val ch: ReadChannel[Option[T]])
                                                         (implicit val fractional: Fractional[T])
    extends Operators.FractionalOps[T, Option[T], Option[T]]
    with Operators.OptionBase[T, T]

  implicit class FractionalValue[T: Fractional](value: T)
                                               (implicit num: Fractional[T]) {
    import num._
    def /(other: ReadChannel[T]): ReadChannel[T] = other.map(div(value, _))
  }

  implicit class FractionalValue2[T: Fractional](value: T)
                                               (implicit num: Fractional[T]) {
    import num._
    def /(other: ReadChannel[Option[T]]): ReadChannel[Option[T]] =
      other.mapValues((x: T) => div(value, x))
  }

  implicit class OrderingChannel[T: Ordering](val ch: ReadChannel[T])
                                             (implicit val ordering: Ordering[T])
    extends Operators.OrderingOps[T, Boolean, Boolean]
    with Operators.DefaultBase[T, Boolean]

  implicit class OptionalOrderingChannel[T: Fractional](val ch: ReadChannel[Option[T]])
                                                       (implicit val ordering: Ordering[T])
    extends Operators.OrderingOps[T, Option[Boolean], Option[Boolean]]
    with Operators.OptionBase[T, Boolean]

  implicit class OrderingValue[T: Ordering](value: T)
                                           (implicit ordering: Ordering[T]) {
    import ordering._

    def >(other: ReadChannel[T]): ReadChannel[Boolean] = other.map(value > _)
    def >=(other: ReadChannel[T]): ReadChannel[Boolean] = other.map(value >= _)
    def <(other: ReadChannel[T]): ReadChannel[Boolean] = other.map(value < _)
    def <=(other: ReadChannel[T]): ReadChannel[Boolean] = other.map(value <= _)
  }

  implicit class OrderingValue2[T: Ordering](value: T)
                                            (implicit ordering: Ordering[T]) {
    import ordering._

    def >(other: ReadChannel[Option[T]]): ReadChannel[Option[Boolean]] =
      other.mapValues((t: T) => value > t)
    def >=(other: ReadChannel[Option[T]]): ReadChannel[Option[Boolean]] =
      other.mapValues((t: T) => value >= t)
    def <(other: ReadChannel[Option[T]]): ReadChannel[Option[Boolean]] =
      other.mapValues((t: T) => value < t)
    def <=(other: ReadChannel[Option[T]]): ReadChannel[Option[Boolean]] =
      other.mapValues((t: T) => value <= t)
  }
}
