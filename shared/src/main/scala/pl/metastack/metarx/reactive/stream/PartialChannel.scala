package pl.metastack.metarx.reactive.stream

import pl.metastack.metarx.ReadChannel

trait PartialChannel[T] {
  /** true if partial channel has a value, false otherwise */
  def isDefined: ReadChannel[Boolean]

  /** Filters out all defined values */
  def values: ReadChannel[T]
}
