package pl.metastack.metarx.reactive.propagate

import pl.metastack.metarx.{ReadChannel, WriteChannel}

trait Publish[T] {
  def publish(ch: WriteChannel[T]): ReadChannel[Unit]
  def >>(ch: WriteChannel[T]) = publish(ch)
}
