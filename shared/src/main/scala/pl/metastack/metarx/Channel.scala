package pl.metastack.metarx

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

object Channel {
  type Observer[T, U] = T => Result[U]

  def apply[T](): Channel[T] =
    new RootChannel[T] {
      def flush(f: T => Unit) { }
    }

  def from[T](future: Future[T])
             (implicit exec: ExecutionContext): ReadChannel[T] = {
    val ch = Channel[T]()
    future.foreach(ch.produce)
    ch
  }

  /** Combine a read with a write channel. */
  def apply[T](read: ReadChannel[T], write: WriteChannel[T]): Channel[T] = {
    val res = new RootChannel[T] {
      def flush(f: T => Unit) { read.flush(f) }
    }
    val pub = res.publish(write)
    res.subscribe(read, pub)
    res
  }
}

trait ChannelImplicits {
  implicit def FutureToReadChannel[T](future: Future[T])
                                     (implicit exec: ExecutionContext): ReadChannel[T] = Channel.from(future)
}

object ChannelImplicits extends ChannelImplicits

trait Result[T] {
  val values: Seq[T]
}

object Result {
  case class Next[T](values: T*) extends Result[T]
  case class Done[T](values: T*) extends Result[T]
}

trait ReadChannel[T]
  extends reactive.stream.Head[T]
  with reactive.stream.Tail[ReadChannel, T]
  with reactive.stream.Take[ReadChannel, T]
  with reactive.stream.Fold[T]
  with reactive.stream.Is[T]
  with reactive.stream.Aggregate[ReadChannel, T]
  with reactive.stream.Filter[ReadChannel, T, T]
  with reactive.stream.Map[ReadChannel, T]
  with reactive.stream.MapExtended[ReadChannel, T]
  with reactive.stream.Cache[T]
  with reactive.stream.Size
  with reactive.stream.PartialChannel[T]
  with reactive.poll.Flush[T]
  with reactive.propagate.Publish[T]
  with Disposable
{ self =>
  import Channel.Observer

  private[metarx] val children = Array[ChildChannel[T, _]]()

  def cache: ReadPartialChannel[T] = {
    val res = Opt[T]()
    res << map(Some(_))
    res
  }

  def cache(default: T): ReadStateChannel[T] = {
    val res = Var[T](default)
    res << this
    res
  }

  def flush(f: T => Unit)

  def publish(ch: WriteChannel[T]): ReadChannel[Unit] = ch.subscribe(this)
  def publish[U](ch: WriteChannel[T], ignore: ReadChannel[U]): ReadChannel[Unit] = ch.subscribe(this, ignore)

  def or(ch: ReadChannel[_]): ReadChannel[Unit] = {
    val that = this

    val res = new RootChannel[Unit] {
      def flush(f: Unit => Unit) {
        that.flush(_ => f(()))
        ch.flush(_ => f(()))
      }
    }

    attach(_ => res := (()))
    ch.attach(_ => res := (()))

    res
  }

  def |(ch: ReadChannel[_]) = or(ch)

  def merge(ch: ReadChannel[T]): ReadChannel[T] = {
    val that = this

    val res = new RootChannel[T] {
      def flush(f: T => Unit) {
        that.flush(f)
        ch.flush(f)
      }
    }

    res << this
    res << ch

    res
  }

  def zip[U](other: ReadChannel[U]): ReadChannel[(T, U)] = {
    val that = this

    val res = new RootChannel[(T, U)] {
      def flush(f: ((T, U)) => Unit) {
        that.flush(t => other.flush(u => f((t, u))))
      }
    }

    attach(t => other.flush(u => res.produce((t, u))))
    other.attach(u => flush(t => res.produce((t, u))))

    res
  }

  /** Helper function to zip channel with the two given channels. Can be used
    * to avoid nested tuples.
    */
  def zip[U, V](other1: ReadChannel[U],
                other2: ReadChannel[V]): ReadChannel[(T, U, V)] =
    zip(other1)
      .zip(other2)
      .map(z => (z._1._1, z._1._2, z._2))

  def zip[U, V, W](other1: ReadChannel[U],
                   other2: ReadChannel[V],
                   other3: ReadChannel[W]): ReadChannel[(T, U, V, W)] =
    zip(other1, other2)
      .zip(other3)
      .map(z => (z._1._1, z._1._2, z._1._3, z._2))

  def zip[U, V, W, X](other1: ReadChannel[U],
                      other2: ReadChannel[V],
                      other3: ReadChannel[W],
                      other4: ReadChannel[X]): ReadChannel[(T, U, V, W, X)] =
    zip(other1, other2, other3)
      .zip(other4)
      .map(z => (z._1._1, z._1._2, z._1._3, z._1._4, z._2))

  def zipWith[U, V](other: ReadChannel[U])(f: (T, U) => V): ReadChannel[V] =
    zip(other).map(f.tupled)

  def child(): ReadChannel[T] =
    forkUni(t => Result.Next(t))

  def silentAttach(f: T => Unit): ReadChannel[Unit] =
    forkUni { value =>
      f(value)
      Result.Next()
    }

  def attach(f: T => Unit): ReadChannel[Unit] = {
    val ch = silentAttach(f).asInstanceOf[UniChildChannel[T, Unit]]
    flush(ch.process)
    ch
  }

  def detach(ch: ChildChannel[T, _]): Unit =
    children -= ch

  /** Buffers all produced elements */
  def buffer: Buffer[T] = {
    val buf = Buffer[T]()
    attach(value => buf.append(value))
    buf
  }

  /** Buffers only current element */
  def toBuffer: Buffer[T] = {
    val buf = Buffer[T]()
    attach(value => buf.set(Seq(value)))
    buf
  }

  /** Uni-directional fork for values */
  def forkUni[U](observer: Observer[T, U], filterCycles: Boolean = false): ReadChannel[U] = {
    val ch = UniChildChannel[T, U](this, observer, None, filterCycles)
    children += ch
    ch
  }

  def forkUniState[U](observer: Observer[T, U], onFlush: => Option[U]): ReadChannel[U] = {
    val ch = UniChildChannel[T, U](this, observer, Some(() => onFlush))
    children += ch
    flush(ch.process) /* Otherwise onFlush will be None for the initial value */
    ch
  }

  /** Uni-directional fork for channels */
  def forkUniFlat[U](observer: Observer[T, ReadChannel[U]]): ReadChannel[U] = {
    val ch = FlatChildChannel[T, U](this, observer)
    children += ch
    flush(ch.process)
    ch
  }

  /** Bi-directional fork for channels */
  def forkBiFlat[U](obs: Observer[T, Channel[U]]): Channel[U] = {
    val ch = BiFlatChildChannel[T, U](this, obs)
    children += ch
    ch
  }

  def filter(f: T => Boolean): ReadChannel[T] =
    forkUni { value =>
      if (f(value)) Result.Next(value)
      else Result.Next()
    }

  def withFilter(f: T => Boolean): WithFilter = new WithFilter(f)

  class WithFilter(p: T => Boolean) {
    def map[U](f: T => U): ReadChannel[U] = self.filter(p).map(f)
    def flatMap[U](f: T => ReadChannel[U]): ReadChannel[U] = self.filter(p).flatMap(f)
    def foreach[U](f: T => U): Unit = self.filter(p).foreach(f)
    def withFilter[U](q: T => Boolean): WithFilter = new WithFilter(x => p(x) && q(x))
  }

  def foreach[U](f: T => U): Unit = attach { x => f(x); () }

  def filterCycles: ReadChannel[T] =
    forkUni(value => Result.Next(value), filterCycles = true)

  def take(count: Int): ReadChannel[T] = {
    assert(count > 0)
    var cnt = count
    forkUni { value =>
      if (cnt > 1) { cnt -= 1; Result.Next(value) }
      else Result.Done(value)
    }
  }

  def drop(count: Int): ReadChannel[T] = {
    assert(count > 0)
    var cnt = count
    forkUniState(value =>
      if (cnt > 0) { cnt -= 1; Result.Next() }
      else Result.Next(value)
    , None
    )
  }

  def head: ReadChannel[T] = forkUni(value => Result.Done(value))
  def tail: ReadChannel[T] = drop(1)

  def isHead(value: T): ReadChannel[Boolean] =
    take(1).map(_ == value)

  def map[U](f: T => U): ReadChannel[U] =
    forkUni { value =>
      Result.Next(f(value))
    }

  def mapTo[U](f: T => U): DeltaDict[T, U] = {
    val delta: ReadChannel[Dict.Delta[T, U]] = map[Dict.Delta[T, U]] { value =>
      Dict.Delta.Insert(value, f(value))
    }

    DeltaDict(delta)
  }

  def is(value: T): ReadChannel[Boolean] =
    forkUni { t =>
      Result.Next(t == value)
    }.distinct

  def is(other: ReadChannel[T]): ReadChannel[Boolean] = {
    zip(other).map { case (a, b) => a == b }
  }

  def isNot(value: T): ReadChannel[Boolean] =
    forkUni { t =>
      Result.Next(t != value)
    }.distinct

  def isNot(other: ReadChannel[T]): ReadChannel[Boolean] = !is(other)

  def flatMap[U](f: T => ReadChannel[U]): ReadChannel[U] =
    forkUniFlat(value => Result.Next(f(value)))

  def flatMapSeq[U](f: T => Seq[U]): ReadChannel[U] =
    forkUni(value => Result.Next(f(value): _*))

  /** flatMap with back-propagation. */
  def flatMapCh[U](f: T => Channel[U]): Channel[U] =
    forkBiFlat(value => Result.Next(f(value)))

  def flatMapBuf[U](f: T => ReadBuffer[U]): ReadBuffer[U] = {
    val buf = Buffer[U]()
    var child: ReadChannel[Unit] = null
    attach { value =>
      buf.clear()
      if (child != null) child.dispose()
      child = buf.changes.subscribe(f(value).changes)
    }
    buf
  }

  def collect[U](f: PartialFunction[T, U]): ReadChannel[U] =
    forkUni { value =>
      Result.Next(f.lift(value).toSeq: _*)
    }

  /** @note Caches the accumulator value. */
  def foldLeft[U](acc: U)(f: (U, T) => U): ReadChannel[U] = {
    var accum = acc
    forkUniState(value => {
      accum = f(accum, value)
      Result.Next(accum)
    }, Some(accum))
  }

  def takeUntil(ch: ReadChannel[_]): ReadChannel[T] = {
    val res = forkUni { value =>
      Result.Next(value)
    }

    ch.forkUni[Any] { _ =>
      res.dispose()
      Result.Done()
    }

    res
  }

  def takeWhile(f: T => Boolean): ReadChannel[T] =
    forkUni { value =>
      if (f(value)) Result.Next(value)
      else Result.Done()
    }

  def writeTo(write: WriteChannel[T]): Channel[T] = {
    val res = Channel[T]()
    val ignore = write.subscribe(res)
    res.subscribe(this, ignore)
    res
  }

  def distinct: ReadChannel[T] = {
    var cur = Option.empty[T]
    forkUniState(value => {
      if (cur.contains(value)) Result.Next()
      else {
        cur = Some(value)
        Result.Next(value)
      }
    }, cur)
  }

  def throttle(interval: FiniteDuration)
              (implicit scheduler: Scheduler): ReadChannel[T] = {
    val intervalMs = interval.toMillis
    var next = 0L
    forkUni { t =>
      val time = scheduler.currentTimeMillis()
      if (next > time) Result.Next()
      else {
        next = time + intervalMs
        Result.Next(t)
      }
    }
  }

  override def values[U](implicit ev: T <:< Option[U]): ReadChannel[U] =
    forkUni { v =>
      ev(v) match {
        case None        => Result.Next()
        case Some(value) => Result.Next(value)
      }
    }

  override def isDefined(implicit ev: T <:< Option[_]): ReadChannel[Boolean] =
    map(ev).isNot(None)

  override def undefined(implicit ev: T <:< Option[_]): ReadChannel[Boolean] =
    map(ev).is(None)

  override def mapValues[U, V](f: U => V)(implicit ev: T <:< Option[U]): ReadChannel[Option[V]] =
    forkUni { v =>
      ev(v) match {
        case None        => Result.Next(None)
        case Some(value) => Result.Next(Some(f(value)))
      }
    }

  override def mapOrElse[U, V](f: U => V, default: => V)(implicit ev: T <:< Option[U]): ReadChannel[V] = {
    lazy val d = default
    forkUni { v =>
      ev(v) match {
        case None        => Result.Next(d)
        case Some(value) => Result.Next(f(value))
      }
    }
  }

  override def count(implicit ev: T <:< Option[_]): ReadChannel[Int] =
    foldLeft(0) { case (acc, v) =>
      ev(v) match {
        case Some(_) => acc + 1
        case None    => 0
      }
    }

  override def orElse[U](default: => ReadChannel[U])(implicit ev: T <:< Option[U]): ReadChannel[U] =
    flatMap { v =>
      ev(v) match {
        case None        => default
        case Some(value) => Var(value)
      }
    }

  override def contains[U](value: U)(implicit ev: T <:< Option[U]): ReadChannel[Boolean] =
    map { v =>
      ev(v) match {
        case Some(`value`) => true
        case _             => false
      }
    }
}

trait WriteChannel[T]
  extends reactive.propagate.Produce[T]
  with reactive.propagate.Subscribe[T]
{
  import Channel.Observer

  private[metarx] val children: Array[ChildChannel[T, _]]

  def detach(ch: ChildChannel[T, _])

  def flush(f: T => Unit)

  def produce(value: T): Unit =
    children.foreach(_.process(value))

  def produce[U](value: T, ignore: ReadChannel[U]*) {
    assume(ignore.forall(cur => children.contains(cur.asInstanceOf[ChildChannel[T, _]])))
    children.foreach { child =>
      if (!ignore.contains(child)) child.process(value)
    }
  }

  def flatProduce(value: Option[T]) {
    value.foreach(produce)
  }

  def flatProduce[U](value: Option[T], ignore: ReadChannel[U]*) {
    value.foreach(v => produce(v, ignore: _*))
  }

  /** Bi-directional fork for values */
  def forkBi[U](fwd: Observer[T, U], bwd: Observer[U, T], silent: Boolean = false): Channel[U] = {
    val ch = BiChildChannel[T, U](this, fwd, bwd)
    children += ch
    if (!silent) flush(ch.process)
    ch
  }

  /** Redirect stream from `other` to `this`. */
  def subscribe(ch: ReadChannel[T]): ReadChannel[Unit] = ch.attach(produce)

  def subscribe[U](ch: ReadChannel[T], ignore: ReadChannel[U]): ReadChannel[Unit] =
    ch.attach(produce(_, ignore))
}

trait Channel[T]
  extends ReadChannel[T]
  with WriteChannel[T]
  with reactive.propagate.Bind[T]
{
  def dispose(): Unit

  def toOpt: Opt[T] = {
    val res = Opt[T]()
    attach(res := Some(_))
    res
  }

  def state: Opt[T] = {
    val res = Opt[T]()
    res <<>> partialBiMap[Option[T]](x => Some(Some(x)), identity[Option[T]])
    res
  }

  def state(default: T): Var[T] = {
    val res = Var[T](default)
    res <<>> this
    res
  }

  def biMap[U](f: T => U, g: U => T): Channel[U] =
    forkBi(
      fwdValue => Result.Next(f(fwdValue)),
      bwdValue => Result.Next(g(bwdValue)))

  def partialBiMap[U](f: T => Option[U], g: U => Option[T]): Channel[U] =
    forkBi(
      fwdValue => Result.Next(f(fwdValue).toSeq: _*),
      bwdValue => Result.Next(g(bwdValue).toSeq: _*))

  /** Two-way binding; synchronises `this` and `other`. */
  def bind(other: Channel[T]) {
    var obsOther: ReadChannel[Unit] = null
    val obsThis = silentAttach(other.produce(_, obsOther))
    obsOther = other.attach(produce(_, obsThis))
    flush(obsThis.asInstanceOf[UniChildChannel[Any, Any]].process)
  }

  def bind(other: Channel[T], ignoreOther: ReadChannel[Unit]) {
    var obsOther: ReadChannel[Unit] = null
    val obsThis = silentAttach(other.produce(_, obsOther, ignoreOther))
    obsOther = other.attach(produce(_, obsThis))
    flush(obsThis.asInstanceOf[UniChildChannel[Any, Any]].process)
  }

  /*def +(write: WriteChannel[T]): Channel[T] = {
    val res = new RootChannel[T] {
      def flush(f: T => Unit) { Channel.this.flush(f) }
    }
    val ignore = write << res
    this <<>> (res, ignore)
    res
  }*/

  override def toString = "Channel()"
}

trait ChildChannel[T, U]
  extends Channel[U]
  with ChannelDefaultSize[U]
{
  /** Return true if the stream is completed. */
  def process(value: T)
}

case class FlatChildChannel[T, U](parent: ReadChannel[T],
                                  observer: Channel.Observer[T, ReadChannel[U]])
  extends ChildChannel[T, U]
{
  private var bound: ReadChannel[U] = null
  private var subscr: ReadChannel[Unit] = null

  def onChannel(ch: ReadChannel[U]) {
    if (subscr != null) subscr.dispose()
    bound = ch
    subscr = bound.attach(this := _)
  }

  def process(value: T) {
    observer(value) match {
      case Result.Next(values @ _*) =>
        values.foreach(onChannel)

      case Result.Done(values @ _*) =>
        values.foreach(onChannel)
        dispose()
    }
  }

  def flush(f: U => Unit) {
    parent.flush { value =>
      observer(value) match {
        case Result.Next(values @ _*) =>
          values.foreach(_.flush(f))
        case Result.Done(values @ _*) =>
          values.foreach(_.flush(f))
          dispose()
      }
    }
  }

  def dispose() {
    parent.detach(this)

    if (subscr != null) subscr.dispose()

    children.foreach(_.dispose())
    children.clear()
  }

  override def toString = "FlatChildChannel()"
}

/** Uni-directional child */
case class UniChildChannel[T, U](parent: ReadChannel[T],
                                 observer: Channel.Observer[T, U],
                                 onFlush: Option[() => Option[U]],
                                 doFilterCycles: Boolean = false)
  extends ChildChannel[T, U]
{
  private var inProcess = false

  def process(value: T) {
    synchronized {
      if (doFilterCycles) {
        if (inProcess) return
      } else assert(!inProcess, "Cycle found")

      inProcess = true

      try {
        observer(value) match {
          case Result.Next(values@_*) =>
            values.foreach(produce)
          case Result.Done(values@_*) =>
            values.foreach(produce)
            dispose()
        }
      } finally {
        inProcess = false
      }
    }
  }

  def flush(f: U => Unit) {
    synchronized {
      inProcess = true
      if (onFlush.isDefined) onFlush.get().foreach(f)
      else parent.flush(observer(_).values.foreach(f))
      inProcess = false
    }
  }

  def dispose() {
    parent.detach(this)

    children.foreach(_.dispose())
    children.clear()
  }

  override def toString = "UniChildChannel()"
}

/** Bi-directional child */
case class BiChildChannel[T, U](parent: WriteChannel[T],
                                fwd: Channel.Observer[T, U],
                                bwd: Channel.Observer[U, T])
  extends ChildChannel[T, U]
{
  val back = silentAttach { value =>
    bwd(value) match {
      case Result.Next(values @ _*) =>
        values.foreach(r => parent.produce(r, this))
        Result.Next()
      case Result.Done(values @ _*) =>
        values.foreach(r => parent.produce(r, this))
        Result.Done()
    }
  }

  def process(value: T) {
    fwd(value) match {
      case Result.Next(values @ _*) =>
        values.foreach(r => produce(r, back))
      case Result.Done(values @ _*) =>
        values.foreach(r => produce(r, back))
        dispose()
    }
  }

  def flush(f: U => Unit) {
    parent.flush(fwd(_).values.foreach(f))
  }

  def dispose() {
    parent.detach(this)

    back.dispose()

    children.foreach(_.dispose())
    children.clear()
  }

  override def toString = "BiChildChannel()"
}

case class BiFlatChildChannel[T, U](parent: ReadChannel[T],
                                    observer: Channel.Observer[T, Channel[U]])
  extends ChildChannel[T, U]
{
  private var bound: Channel[U] = null
  private var subscr: ReadChannel[Unit] = null

  val back = silentAttach { value =>
    if (bound != null && subscr != null) bound.produce(value, subscr)
  }

  def onChannel(ch: Channel[U]) {
    if (subscr != null) subscr.dispose()
    bound = ch
    subscr = bound.silentAttach(produce(_, back))
    bound.flush(produce(_, back))
  }

  def process(value: T) {
    observer(value) match {
      case Result.Next(values @ _*) =>
        values.foreach(onChannel)

      case Result.Done(values @ _*) =>
        values.foreach(onChannel)
        dispose()
    }
  }

  def flush(f: U => Unit) {
    if (bound != null) bound.flush(f)
  }

  def dispose() {
    parent.detach(this)

    if (subscr != null) subscr.dispose()

    back.dispose()

    children.foreach(_.dispose())
    children.clear()
  }

  override def toString = "BiFlatChildChannel()"
}

trait ChannelDefaultSize[T] {
  this: ReadChannel[T] =>

  def size: ReadChannel[Int] =
    foldLeft(0) { case (acc, cur) => acc + 1 }
}

trait RootChannel[T]
  extends Channel[T]
  with ChannelDefaultSize[T]
{
  def dispose() {
    children.foreach(_.dispose())
    children.clear()
  }
}

trait ReadStateChannel[T] extends ReadChannel[T] {
  def get: T
}

/** In Rx terms, a [[StateChannel]] can be considered a cold observable. */
trait StateChannel[T] extends Channel[T] with ReadStateChannel[T] {
  def update(f: T => T) {
    flush(t => produce(f(t)))
  }

  // Shapeless has not been built yet for Scala.js 0.6.0
  /*def value[U](f: shapeless.Lens[T, T] => shapeless.Lens[T, U]) =
    lens(f(shapeless.lens[T]))

  /** Two-way lens that propagates back changes to all observers. */
  def lens[U](l: shapeless.Lens[T, U]): Channel[U] = {
    var cur: Option[T] = None
    forkBi(
      fwdValue => { cur = Some(fwdValue); Result.Next(Some(l.get(fwdValue))) },
      bwdValue => Result.Next(Some(l.set(cur.get)(bwdValue))))
  }*/

  def dispose() {
    children.foreach(_.dispose())
    children.clear()
  }
}
