package pl.metastack.metarx.reactive.stream

trait Aggregate[Container[_], T] {
  /** Filters out (merges) duplicates */
  def distinct: Container[T]
}
