package pl.metastack.metarx.reactive.stream

trait Tail[Container[_], T] {
  /** Skips first element */
  def tail: Container[T]
}
