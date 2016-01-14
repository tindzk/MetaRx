package pl.metastack.metarx

import java.util.concurrent.atomic.AtomicReference

/** Binary channel; use to communicate between two components. Values produced
  * on `Bin` will be propagated to `left` and `right`, but not between those two.
  * It can be used for two-way binding.
  */
class Bin[T](init: T)
  extends StateChannel[T]
  with ChannelDefaultSize[T]
{
  private val v = new AtomicReference(init)

  val left  = StateChannel[T](v.get)
  val right = StateChannel[T](v.get)

  left.attach(v.set)
  right.attach(v.set)

  attach { value =>
    left := value
    right := value
  }

  def flush(f: T => Unit): Unit = f(v.get)

  def get: T = v.get

  override def toString = s"Bin(${v.toString})"
}

object Bin {
  def apply[T](v: T) = new Bin(v)
}
