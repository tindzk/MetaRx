package pl.metastack.metarx.reactive.mutate

trait PartialChannel[T] {
  def partialUpdate[U](f: PartialFunction[U, U])
                      (implicit ev: T <:< Option[U]): Unit

  /** Clear current value (if exists) */
  def clear()(implicit ev: T <:< Option[_]): Unit
}
