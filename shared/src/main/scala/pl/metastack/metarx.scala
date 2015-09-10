package pl.metastack

package object metarx extends OptImplicits {
  implicit def FunctionToWriteChannel[T](f: T => Unit): WriteChannel[T] = {
    val ch = Channel[T]()
    ch.attach(f)
    ch
  }

  implicit class OptExtensions[T](opt: Opt[T]) {
    def :=(t: T) {
      opt := Some(t)
    }
  }

  implicit class ReadChannelBooleanExtensions(rch: ReadChannel[Boolean]) {
    def &&(other: ReadChannel[Boolean]): ReadChannel[Boolean] =  {
      rch.flatMap (thisVal => other.map(otherVal => thisVal && otherVal) )
    }
    def &&(argument: Boolean): ReadChannel[Boolean] = {
      rch.map(value => value && argument)
    }

    def ||(other: ReadChannel[Boolean]): ReadChannel[Boolean] =  {
      rch.flatMap (thisVal => other.map(otherVal => thisVal || otherVal) )
    }
    def ||(argument: Boolean): ReadChannel[Boolean] = {
      rch.map(value => value || argument)
    }

    def isFalse: ReadChannel[Boolean] = rch.map(!_)
    def unary_! = isFalse

    def onTrue: ReadChannel[Unit] = rch.collect { case true => Unit }
    def onFalse: ReadChannel[Unit] = rch.collect { case false => Unit }
  }

  implicit class BooleanExtensions(boolVal: Boolean) {
    def &&(rch: ReadChannel[Boolean]): ReadChannel[Boolean] = rch && boolVal
    def ||(rch: ReadChannel[Boolean]): ReadChannel[Boolean] = rch || boolVal
  }
}