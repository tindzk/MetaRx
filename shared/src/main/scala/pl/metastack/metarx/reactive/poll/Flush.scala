package pl.metastack.metarx.reactive.poll

trait Flush[T] {
  /** Call `f` for each element */
  def flush(f: T => Unit)
}
