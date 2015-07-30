package pl.metastack.metarx.reactive.stream

import pl.metastack.metarx.{ReadPartialChannel, ReadStateChannel}

trait Cache[T] {
  def cache: ReadPartialChannel[T]
  def cache(default: T): ReadStateChannel[T]
}
