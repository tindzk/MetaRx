package pl.metastack.metarx.reactive.propagate

import pl.metastack.metarx.Channel

trait Bind[T] {
  def bind(other: Channel[T])
  def <<>>(other: Channel[T]): Unit = bind(other)
}
