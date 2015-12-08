package pl.metastack.metarx

import java.util.concurrent.atomic.AtomicReference

sealed class Var[T](init: T)
  extends StateChannel[T]
  with ChannelDefaultSize[T]
{
  private val v = new AtomicReference(init)
  attach(v.set)

  def flush(f: T => Unit): Unit = f(v.get)
  def get: T = v.get

  override def toString = s"Var(${v.toString})"
}

object Var {
  def apply[T](v: T) = new Var(v)
}

/* Upon each subscription, emit `v`. `v` is evaluated lazily. */
object LazyVar {
  def apply[T](v: => T) = new StateChannel[T]
    with ChannelDefaultSize[T]
  {
    def get: T = v
    def flush(f: T => Unit) { f(v) }
    // TODO Should not provide produce(T), only produce()
    def produce() { this := v }

    override def toString = s"LazyVar(${v.toString})"
  }
}

/** Every produced value on the channel `change` indicates that the underlying
 * variable was modified and the current value can be retrieved via `get`.
 * If a value v is produced on the resulting channel instead, then set(v) is
 * called.
 */
object PtrVar {
  def apply[T](change: ReadChannel[_], _get: => T, set: T => Unit) = new StateChannel[T]
    with ChannelDefaultSize[T]
  {
    val sub = attach(set)
    change.attach(_ => produce())

    def get: T = _get
    def flush(f: T => Unit): Unit = f(get)
    def produce(): Unit = produce(get, sub)

    override def toString = s"PtrVar(${get.toString})"
  }
}
