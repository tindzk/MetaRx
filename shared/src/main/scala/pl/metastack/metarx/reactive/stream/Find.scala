package pl.metastack.metarx.reactive.stream

import pl.metastack.metarx.ReadPartialChannel

trait Find[T] {
  /** Finds first value for which `f` is true */
  def find(f: T => Boolean): ReadPartialChannel[T]
}
