package pl.metastack.metarx

import java.util.concurrent.atomic.AtomicReference

class Sub[T](init: T) extends Var[T](init) {
  private val subscription =
    new AtomicReference(Option.empty[ReadChannel[Unit]])

  def produce(subscriber: ReadChannel[T]): Unit = {
    val old = subscription.getAndSet(Some(subscriber.attach(super.produce)))
    old.foreach(_.dispose())
  }

  def !(subscriber: ReadChannel[T]): Unit = produce(subscriber)

  def set(subscriber: ReadChannel[T]): Unit = produce(subscriber)
  def :=(subscriber: ReadChannel[T]): Unit = set(subscriber)

  override def produce(value: T): Unit = {
    val old = subscription.getAndSet(None)
    old.foreach(_.dispose())
    super.produce(value)
  }

  def detach(): Unit = {
    val old = subscription.getAndSet(None)
    old.foreach(_.dispose())
  }

  def dep[U](fwd: ReadChannel[T] => ReadChannel[U],
             bwd: ReadChannel[U] => ReadChannel[T]): Dep[T, U] =
    new Dep(this, fwd, bwd)

  override def toString = s"Sub()"

  override def dispose(): Unit = {
    detach()
    super.dispose()
  }
}

object Sub {
  def apply[T](init: T): Sub[T] = new Sub[T](init)

  def apply[T](ch: ReadChannel[T]): Sub[T] = {
    val subscriber = new Sub[T](null.asInstanceOf[T])
    subscriber := ch
    subscriber
  }
}
