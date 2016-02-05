package pl.metastack.metarx.reactive.stream

import pl.metastack.metarx.ReadBuffer

trait Concatenate[Container[_], T] {
  def concat(buffer: ReadBuffer[T]): ReadBuffer[T]

  def ++(buffer: ReadBuffer[T]): ReadBuffer[T] = concat(buffer)
}
