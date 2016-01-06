package pl.metastack.metarx

/** @see [[Sub.dep()]] */
class Dep[T, U] private[metarx](sub: Sub[T],
                                fwd: ReadChannel[T] => ReadChannel[U],
                                bwd: ReadChannel[U] => ReadChannel[T])
  extends Sub[U](null.asInstanceOf[U]) {
  private var ignore = true
  this := fwd(sub)
  ignore = false

  override def produce(value: ReadChannel[U]): Unit = {
    if (!ignore) sub := bwd(value)
    super.produce(value)
  }

  override def produce(value: U): Unit = {
    if (!ignore) sub := bwd(Var(value))
    super.produce(value)
  }
}