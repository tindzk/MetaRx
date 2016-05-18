package pl.metastack.metarx

/** @see [[Sub.dep()]] */
class Dep[T, U] private[metarx](sub: Sub[T],
                                fwd: ReadChannel[T] => ReadChannel[U],
                                bwd: ReadChannel[U] => ReadChannel[T])
  extends Sub[U](null.asInstanceOf[U]) {
  sub.attach { s =>
    super.set(fwd(Var(s)))
  }

  override def set(value: ReadChannel[U]): Unit = {
    sub := bwd(value)
    super.set(value)
  }

  override def set(value: U): Unit =
    sub := bwd(Var(value))
}