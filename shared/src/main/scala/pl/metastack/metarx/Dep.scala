package pl.metastack.metarx

/** @see [[Sub.dep()]] */
class Dep[T, U] private[metarx](sub: Sub[T],
                                fwd: ReadChannel[T] => ReadChannel[U],
                                bwd: ReadChannel[U] => ReadChannel[T]) {
  private val channel = fwd(sub)
  private val internal = Sub[U](channel)

  def :=(channel: ReadChannel[U]): Unit = sub := bwd(channel)
  def :=(value: U): Unit = sub := bwd(Var(value))
  def get: U = internal.get

  def toReadChannel: ReadChannel[U] = channel
}