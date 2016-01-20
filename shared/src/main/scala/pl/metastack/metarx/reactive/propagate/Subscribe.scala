package pl.metastack.metarx.reactive.propagate

import pl.metastack.metarx.ReadChannel

trait Subscribe[T] {
  def subscribe(ch: ReadChannel[T]): ReadChannel[Unit]
  def <<(ch: ReadChannel[T]): ReadChannel[Unit] = subscribe(ch)
}
