package pl.metastack.metarx

import java.util.concurrent.atomic.AtomicReference

/**
  * Binary channel
  *
  * Use to communicate between two components. Values produced on `Bin` will be
  * propagated to `left` and `right`, but not between those two. It can be used
  * for two-way binding.
  */
class Bin[T](value: T)
  extends StateChannel[T]
  with ChannelDefaultSize[T]
{
  private val v = new AtomicReference(value)

  val left  = LazyVar[T](v.get)
  val right = LazyVar[T](v.get)

  left.attach(v.set)
  right.attach(v.set)

  attach { value =>
    left := value
    right := value
  }

  def flush(f: T => Unit): Unit = f(v.get)

  def get: T = v.get

  override def toString = s"Bin($get)"
}

object Bin {
  def apply[T](value: T) = new Bin(value)
}
