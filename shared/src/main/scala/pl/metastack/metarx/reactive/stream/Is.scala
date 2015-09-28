package pl.metastack.metarx.reactive.stream

import pl.metastack.metarx.ReadChannel

/**
 * Operations for streams that can only have one value at the same time, like
 * channels
 */
trait Is[T] {
  /** Current value is equal to `value` */
  def is(value: T): ReadChannel[Boolean]
  def is(value: ReadChannel[T]): ReadChannel[Boolean]
  def ===(value: T) = is(value)
  def ===(value: ReadChannel[T]) = is(value)

  /** Current value is not equal to `value` */
  def isNot(value: T): ReadChannel[Boolean]
  def isNot(value: ReadChannel[T]): ReadChannel[Boolean]
  def !==(value: T) = isNot(value)
  def !==(value: ReadChannel[T]) = isNot(value)
}
