package pl.metastack.metarx

/** @see [[Sub.dep()]] */
private[metarx] class Dep[T, U](sub: Sub[T],
                                fwd: ReadChannel[T] => ReadChannel[U],
                                bwd: ReadChannel[U] => ReadChannel[T]) {
  private val internal = Sub[U](fwd(sub))

  def :=(channel: ReadChannel[U]): Unit = sub := bwd(channel)
  def :=(value: U): Unit = sub := bwd(Var(value))
  def get: U = internal.get

  def toReadChannel: ReadChannel[U] = fwd(sub)
}