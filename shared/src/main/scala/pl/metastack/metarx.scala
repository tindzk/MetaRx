package pl.metastack

package object metarx {
  implicit def FunctionToWriteChannel[T](f: T => Unit): WriteChannel[T] = {
    val ch = Channel[T]()
    ch.attach(f)
    ch
  }

  implicit class PimpedOpt[T](opt: Opt[T]) {
    def :=(t: T) {
      opt := Some(t)
    }
  }
}