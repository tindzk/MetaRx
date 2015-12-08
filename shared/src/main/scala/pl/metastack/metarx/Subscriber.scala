package pl.metastack.metarx

import java.util.concurrent.atomic.AtomicReference

sealed class Subscriber[T]()
  extends Channel[T]
  with ChannelDefaultSize[T]
{
  private val subscription =
    new AtomicReference(Option.empty[ReadChannel[Unit]])

  def produce(subscriber: ReadChannel[T]): Unit = {
    val old = subscription.getAndSet(Some(this << subscriber))
    old.foreach(_.dispose())
  }

  def :=(subscriber: ReadChannel[T]): Unit = produce(subscriber)

  def detach(): Unit = {
    val old = subscription.getAndSet(None)
    old.foreach(_.dispose())
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
