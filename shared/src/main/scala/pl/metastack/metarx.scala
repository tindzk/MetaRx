package pl.metastack

package object metarx
  extends BufferImplicits
  with ChannelImplicits
  with Operators {

  type Opt[T] = Var[Option[T]]
  type ReadPartialChannel[T] = ReadStateChannel[Option[T]]

  implicit def FunctionToWriteChannel[T](f: T => Unit): WriteChannel[T] = {
    val ch = Channel[T]()
    ch.attach(f)
    ch
  }

  implicit class OptExtensions[T](opt: Opt[T]) {
    def :=(t: T): Unit =
      opt := Some(t)
  }
}
