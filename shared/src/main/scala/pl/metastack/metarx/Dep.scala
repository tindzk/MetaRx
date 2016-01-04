package pl.metastack.metarx

class Dep[T](sub: Sub[T],
             f: ReadChannel[T] => ReadChannel[T],
             f2: => T
            ) extends StateChannel[T] with ChannelDefaultSize[T] {
  attach(value => sub := f(Var(value)))

  def produce(subscriber: ReadChannel[T]): Unit =
    sub := f(subscriber)

  def :=(subscriber: ReadChannel[T]): Unit = produce(subscriber)

  def get: T = f2

  def flush(f: T => Unit): Unit = f(get)
}
