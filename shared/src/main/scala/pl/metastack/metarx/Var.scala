package pl.metastack.metarx

import java.util.concurrent.atomic.AtomicReference

class Var[T](value: T)
  extends StateChannel[T]
  with ChannelDefaultSize[T]
  with reactive.mutate.PartialChannel[T]
{
  private val v = new AtomicReference(value)

  override def get: T = v.get

  override def produce(value: T): Unit = {
    v.set(value)
    super.produce(value)
  }

  override def flush(f: T => Unit): Unit = f(v.get)

  override def clear()(implicit ev: T <:< Option[_]): Unit =
    asInstanceOf[StateChannel[Option[_]]].produce(None)

  override def partialUpdate[U](f: PartialFunction[U, U])
                               (implicit ev: T <:< Option[U]): Unit =
    v.asInstanceOf[AtomicReference[Option[U]]]
     .get
     .foreach(value =>
       asInstanceOf[StateChannel[Option[U]]]
         .produce(f.lift(value)))

  override def toString = s"Var($get)"
}

object Var {
  def apply[T](value: T) = new Var(value)
}

/** Upon each subscription, emits `value`, which is evaluated lazily. */
class LazyVar[T](value: => T)
  extends Channel[T]
  with ReadStateChannel[T]
  with ChannelDefaultSize[T]
  with ChannelDefaultDispose[T] {
  override def get: T = value
  override def flush(f: T => Unit): Unit = f(value)

  def produce(): Unit = produce(value)

  override def toString = s"LazyVar($get)"
}

object LazyVar {
  def apply[T](value: => T): LazyVar[T] = new LazyVar(value)
}

/**
  * Every produced value on the channel `change` indicates that the underlying
  * variable was modified and the current value can be retrieved via `get`.
  * If a value v is produced on the resulting channel instead, then set(v) is
  * called.
  */
class PtrVar[T](change: ReadChannel[_], _get: => T, _set: T => Unit)
  extends StateChannel[T] with ChannelDefaultSize[T]
{
  private val attached = change.attach(_ => produce())

  override def get: T = _get
  override def set(value: T): Unit = _set(value)

  override def produce(value: T): Unit = {
    set(value)
    super.produce(value)
  }

  def produce(): Unit = super.produce(get)

  override def flush(f: T => Unit): Unit = f(get)

  override def dispose(): Unit = {
    attached.dispose()
    super.dispose()
  }

  override def toString = s"PtrVar($get)"
}

object PtrVar {
  def apply[T](change: ReadChannel[_], get: => T, set: T => Unit): PtrVar[T] =
    new PtrVar[T](change, get, set)
}
