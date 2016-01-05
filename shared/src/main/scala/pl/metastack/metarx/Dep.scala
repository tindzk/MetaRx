package pl.metastack.metarx

class Dep[T, U](sub: Sub[T],
                fwd: T => U,
                bwd: U => ReadChannel[T]
               ) extends StateChannel[U] with ChannelDefaultSize[U] {
  silentAttach(value => sub := bwd(value))

  def produce(subscriber: ReadChannel[U]): Unit =
    sub := subscriber.flatMap(bwd)

  def :=(subscriber: ReadChannel[U]): Unit = produce(subscriber)

  def get: U = fwd(sub.get)

  def flush(f: U => Unit): Unit = f(get)
}
