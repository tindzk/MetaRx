package pl.metastack.metarx.reactive.stream

import pl.metastack.metarx.ReadBufSet

trait FilterOrdered[Container[_], T] {
  /**
   * Splits stream into two sub-streams
   *
   * The left stream contains all elements as long as `f` is true, all
   * subsequent elements go to the right stream.
   */
  def span(f: T => Boolean): (Container[T], Container[T])

  /** Remove all elements from `other` */
  def diff(other: ReadBufSet[T]): Container[T]

  def -(other: ReadBufSet[T]) = diff(other)
}
