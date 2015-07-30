package pl.metastack.metarx.reactive.poll

trait Count[T] {
  def contains$(value: T): Boolean
}
