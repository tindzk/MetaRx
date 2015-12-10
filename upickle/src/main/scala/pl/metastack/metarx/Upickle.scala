package pl.metastack.metarx

object Upickle {
  import upickle.default._

  implicit def VarW[T: Writer] = Writer[Var[T]] { case x =>
    writeJs(x.get)
  }

  implicit def VarR[T: Reader]: Reader[Var[T]] = Reader[Var[T]] { case x =>
    Var(implicitly[Reader[T]].read(x))
  }

  implicit def OptW[T: Writer] = Writer[Opt[T]] {
    case x => writeJs(x.get)
  }

  implicit def OptR[T: Reader]: Reader[Opt[T]] = Reader[Opt[T]] { case x =>
    new Opt(implicitly[Reader[Option[T]]].read(x))
  }

  implicit def BufferW[T: Writer] = Writer[Buffer[T]] { case x =>
    writeJs(x.get)
  }

  implicit def BufferR[T: Reader]: Reader[Buffer[T]] = Reader[Buffer[T]] { case x =>
    Buffer.from(implicitly[Reader[Seq[T]]].read(x))
  }
}