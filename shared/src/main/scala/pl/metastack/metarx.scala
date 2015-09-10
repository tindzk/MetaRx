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
    def otherWithOperation(other: ReadChannel[Boolean], operator: (Boolean, Boolean) => Boolean) = {
      rch.flatMap(thisVal => other.map(otherVal => operator(thisVal, otherVal)))
    }
    def booleanWithOperation(argument: Boolean, operator: (Boolean, Boolean) => Boolean): ReadChannel[Boolean] = {
      rch.map(value => operator(value, argument))
    }

    def &&(other: ReadChannel[Boolean]): ReadChannel[Boolean] =  {
      otherWithOperation(other, _ && _)
    }
    def &&(argument: Boolean): ReadChannel[Boolean] = {
      booleanWithOperation(argument, _ && _)
    }

    def ||(other: ReadChannel[Boolean]): ReadChannel[Boolean] =  {
      otherWithOperation(other, _ || _)
    }
    def ||(argument: Boolean): ReadChannel[Boolean] = {
      booleanWithOperation(argument, _ || _)
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
