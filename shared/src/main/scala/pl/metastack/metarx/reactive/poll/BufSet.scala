package pl.metastack.metarx.reactive.poll

trait BufSet[T] {
  def toSet$: Set[T]
}
