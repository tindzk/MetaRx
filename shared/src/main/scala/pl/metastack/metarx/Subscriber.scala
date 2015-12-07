package pl.metastack.metarx

sealed class Subscriber[T]()
  extends Channel[T]
  with ChannelDefaultSize[T]
{
  private var subscription = Option.empty[ReadChannel[Unit]]

  def produce(subscriber: ReadChannel[T]): Unit = {
    subscription.foreach(_.dispose())
    subscription = Some(this << subscriber)
  }

  def :=(subscriber: ReadChannel[T]): Unit = produce(subscriber)

  def detach(): Unit = {
    subscription.foreach(_.dispose())
    subscription = None
  }

  override def toString = s"Subscriber()"

  override def flush(f: T => Unit): Unit = {}
  override def dispose(): Unit = detach()
}

object Subscriber {
  def apply[T](): Subscriber[T] = new Subscriber[T]()

  def apply[T](ch: ReadChannel[T]): Subscriber[T] = {
    val subscriber = new Subscriber[T]()
    subscriber := ch
    subscriber
  }
}
