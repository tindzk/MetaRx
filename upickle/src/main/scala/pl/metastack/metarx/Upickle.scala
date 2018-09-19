package pl.metastack.metarx

object Upickle {
  import upickle.default._

  implicit def varReadWrite[T](implicit readWriter: ReadWriter[T]) : ReadWriter[Var[T]] =
    readwriter[T].bimap[Var[T]](_.get, Var(_))

  implicit def optReadWrite[T](implicit readWriter: ReadWriter[Option[T]]): ReadWriter[Opt[T]] =
    readwriter[Option[T]].bimap[Opt[T]](_.get, Var(_))

  implicit def bufferReadWrite[T](implicit readWriter: ReadWriter[Seq[T]]): ReadWriter[Buffer[T]] =
    readwriter[Seq[T]].bimap[Buffer[T]](_.get, Buffer.from(_))

}