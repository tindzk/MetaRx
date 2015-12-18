package pl.metastack.metarx.reactive.stream

import pl.metastack.metarx.ReadChannel

trait PartialChannel[T] {
  /** true if partial channel has a value, false otherwise */
  def isDefined(implicit ev: T <:< Option[_]): ReadChannel[Boolean]

  def undefined(implicit ev: T <:< Option[_]): ReadChannel[Boolean]

  /** Filters out all defined values */
  def values[U](implicit ev: T <:< Option[U]): ReadChannel[U]

  def mapValues[U, V](f: U => V)(implicit ev: T <:< Option[U]): ReadChannel[Option[V]]

  def mapOrElse[U, V](f: U => V, default: => V)(implicit ev: T <:< Option[U]): ReadChannel[V]

  def count(implicit ev: T <:< Option[_]): ReadChannel[Int]

  def orElse[U](default: => ReadChannel[U])(implicit ev: T <:< Option[U]): ReadChannel[U]

  def contains[U](value: U)(implicit ev: T <:< Option[U]): ReadChannel[Boolean]
}
