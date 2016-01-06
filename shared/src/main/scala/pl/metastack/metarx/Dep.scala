package pl.metastack.metarx

/** @see [[Sub.dep()]] */
private[metarx] class Dep[T, U](sub: Sub[T],
                                fwd: ReadChannel[T] => ReadChannel[U],
                                bwd: ReadChannel[U] => ReadChannel[T]) {
  private var value: U = _

  private val internal = Sub[U](fwd(sub))
  internal.attach(u => value = u)

  sub := sub.get

  def :=(channel: ReadChannel[U]): Unit = sub := bwd(channel)
  def :=(value: U): Unit = sub := bwd(Var(value))
  def get: U = value

  def toReadChannel: ReadChannel[U] = fwd(sub)
}