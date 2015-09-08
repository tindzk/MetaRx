package pl.metastack.metarx

import scala.concurrent.{ExecutionContext, Future}

trait OptImplicits {
  implicit class PartialChannelExtensions[T](ch: ReadChannel[Option[T]]) {
    def values: ReadChannel[T] =
      ch.forkUni {
        case None        => Result.Next()
        case Some(value) => Result.Next(value)
      }

    def mapOrElse[U](f: T => U, default: => U): ReadChannel[U] = {
      lazy val d = default
      ch.forkUni {
        case None        => Result.Next(d)
        case Some(value) => Result.Next(f(value))
      }
    }

    def size: ReadChannel[Int] =
      ch.foldLeft(0) {
        case (acc, Some(_)) => acc + 1
        case (acc, None)    => 0
      }

    def orElse(default: => ReadChannel[T]): ReadChannel[T] =
      ch.flatMap {
        case None        => default
        case Some(value) => Var(value)
      }

    def contains(value: T): ReadChannel[Boolean] =
      ch.map {
        case Some(`value`) => true
        case _             => false
      }
  }
}

object OptImplicits extends OptImplicits

trait ReadPartialChannel[T]
  extends ReadStateChannel[Option[T]]
  with reactive.poll.Empty
  with reactive.poll.Count[T]
  with reactive.stream.PartialChannel[T]
{
  @inline def values: ReadChannel[T] =
    OptImplicits.PartialChannelExtensions(this).values

  @inline def mapOrElse[U](f: T => U, default: => U): ReadChannel[U] =
    OptImplicits.PartialChannelExtensions(this).mapOrElse(f, default)

  @inline def size: ReadChannel[Int] =
    OptImplicits.PartialChannelExtensions(this).size

  @inline def orElse(default: => ReadChannel[T]): ReadChannel[T] =
    OptImplicits.PartialChannelExtensions(this).orElse(default)

  @inline def contains(value: T): ReadChannel[Boolean] =
    OptImplicits.PartialChannelExtensions(this).contains(value)
}

trait PartialChannel[T]
  extends StateChannel[Option[T]]
  with ReadPartialChannel[T]

/**
 * Publishes a stream of defined values. Use isEmpty() to detect when the
 * current value is cleared.
 */
sealed class Opt[T](private var v: Option[T] = None)
  extends PartialChannel[T]
  with reactive.poll.PartialChannel
  with reactive.mutate.PartialChannel[T]
{
  attach(v = _)

  def isEmpty$: Boolean = v.isEmpty
  def nonEmpty$: Boolean = v.nonEmpty

  def isDefined$: Boolean = v.isDefined
  def undefined$: Boolean = v.isEmpty

  def contains$(value: T): Boolean = v.contains(value)

  def isDefined: ReadChannel[Boolean] = isNot(None)
  def undefined: ReadChannel[Boolean] = is(None)

  def flush(f: Option[T] => Unit) { f(v) }

  def clear() { produce(None) }

  def partialUpdate(f: PartialFunction[T, T]) {
    v.foreach(value => produce(f.lift(value)))
  }

  def get: Option[T] = v

  private def str = get.map(_.toString).getOrElse("<undefined>")
  override def toString = s"Opt($str)"
}

object Opt {
  def apply[T](): Opt[T] = new Opt[T]()
  def apply[T](value: T): Opt[T] = new Opt(Some(value))

  def from[T](future: Future[T])(implicit exec: ExecutionContext): Opt[T] = {
    val opt = new Opt[T]()
    future.foreach(v => opt.produce(Some(v)))
    opt
  }
}
