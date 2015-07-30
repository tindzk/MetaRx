package pl.metastack.metarx.reactive.poll

trait PartialChannel {
  /** Value is defined */
  def isDefined$: Boolean

  /** Value is undefined */
  def undefined$: Boolean
}
