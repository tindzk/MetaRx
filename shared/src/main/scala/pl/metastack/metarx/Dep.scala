package pl.metastack.metarx

class Dep[T, U](sub: Sub[T],
                fwd: T => U,
                bwd: U => ReadChannel[T]
               ) extends StateChannel[U] with ChannelDefaultSize[U] {
  var skip = false

  val ignore = silentAttach { value =>
    skip = true
    sub := bwd(value)
    skip = false
  }

  sub.attach(value => if (!skip) produce(fwd(value), ignore))

  def produce(subscriber: ReadChannel[U]): Unit =
    sub := subscriber.flatMap(bwd)

  def :=(subscriber: ReadChannel[U]): Unit = produce(subscriber)

  def get: U = fwd(sub.get)

  def flush(f: U => Unit): Unit = f(get)
}
