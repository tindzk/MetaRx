package pl.metastack.metarx

import java.util.concurrent.atomic.AtomicReference

sealed class Sub[T](init: T) extends Var[T](init) {
  private val subscription =
    new AtomicReference(Option.empty[ReadChannel[Unit]])

  def produce(subscriber: ReadChannel[T]): Unit = {
    val old = subscription.getAndSet(Some(subscriber.attach(super.produce)))
    old.foreach(_.dispose())
  }

  def :=(subscriber: ReadChannel[T]): Unit = produce(subscriber)

  override def produce(value: T): Unit = {
    val old = subscription.getAndSet(None)
    old.foreach(_.dispose())
    super.produce(value)
  }

  def detach(): Unit = {
    val old = subscription.getAndSet(None)
    old.foreach(_.dispose())
  }

  override def toString = s"Sub()"
  override def dispose(): Unit = detach()
}

object Sub {
  def apply[T](init: T): Sub[T] = new Sub[T](init)

  def apply[T](ch: ReadChannel[T]): Sub[T] = {
    val subscriber = new Sub[T](null.asInstanceOf[T])
    subscriber := ch
    subscriber
  }
}
