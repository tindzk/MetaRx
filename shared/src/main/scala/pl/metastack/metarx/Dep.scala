package pl.metastack.metarx

class Dep[T](sub: Sub[T],
             fwd: ReadChannel[T] => ReadChannel[T],
             bwd: T => T
            ) extends StateChannel[T] with ChannelDefaultSize[T] {
  silentAttach(value => sub := fwd(Var(value)))

  def produce(subscriber: ReadChannel[T]): Unit =
    sub := fwd(subscriber)

  def :=(subscriber: ReadChannel[T]): Unit = produce(subscriber)

  def get: T = bwd(sub.get)

  def flush(f: T => Unit): Unit = f(get)
}
