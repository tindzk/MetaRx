package pl.metastack.metarx

class Dep[T](sub: Sub[T],
             fwd: T => T,
             bwd: T => ReadChannel[T]
            ) extends StateChannel[T] with ChannelDefaultSize[T] {
  silentAttach(value => sub := bwd(value))

  def produce(subscriber: ReadChannel[T]): Unit =
    sub := subscriber.flatMap(bwd)

  def :=(subscriber: ReadChannel[T]): Unit = produce(subscriber)

  def get: T = fwd(sub.get)

  def flush(f: T => Unit): Unit = f(get)
}
