package pl.metastack.metarx

/** @see [[Sub.dep()]] */
class Dep[T, U] private[metarx](sub: Sub[T],
                                fwd: ReadChannel[T] => ReadChannel[U],
                                bwd: ReadChannel[U] => ReadChannel[T])
  extends Sub[U](null.asInstanceOf[U]) {

  private val attached = sub.attach { s =>
    super.produce(fwd(Var(s)))
  }

  override def produce(value: ReadChannel[U]): Unit = {
    sub := bwd(value)
    super.produce(value)
  }

  override def produce(value: U): Unit =
    sub := bwd(Var(value))

  override def dispose(): Unit = {
    attached.dispose()
    super.dispose()
  }
}