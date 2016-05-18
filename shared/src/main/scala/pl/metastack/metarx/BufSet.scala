package pl.metastack.metarx

import scala.collection.mutable

/**
 * Reactive set
 */
object BufSet {
  sealed trait Delta[T]
  object Delta {
    case class Insert[T](value: T) extends Delta[T]
    case class Remove[T](value: T) extends Delta[T]
    case class Clear[T]() extends Delta[T]
  }

  def apply[T](values: T*): BufSet[T] = {
    val result = new BufSet[T]
    values.foreach(result.insertIfNotExists)
    result
  }

  def apply[T](set: Set[T]): BufSet[T] = {
    val result = new BufSet[T]
    result ++= set
    result
  }

  implicit def BufSetToSeq[T](buf: BufSet[T]): Seq[T] = buf.elements.toSeq
}

object DeltaBufSet {
  import BufSet.Delta

  def apply[T](delta: ReadChannel[Delta[T]]): DeltaBufSet[T] =
    new DeltaBufSet[T] {
      override val changes = delta
    }
}

trait DeltaBufSet[T]
  extends reactive.stream.Size
  with reactive.stream.Map[DeltaBufSet, T]
  with reactive.stream.Filter[DeltaBufSet, T, T]
{
  import BufSet.Delta
  val changes: ReadChannel[Delta[T]]

  def size: ReadChannel[Int] = {
    val count = Var(0)

    changes.attach {
      case Delta.Insert(_) => count.update(_ + 1)
      case Delta.Remove(_) => count.update(_ - 1)
      case Delta.Clear() if count.get != 0 => count := 0
    }

    count
  }

  def map[C](f: T => C): DeltaBufSet[C] =
    DeltaBufSet[C](changes.map {
      case Delta.Insert(v) => Delta.Insert(f(v))
      case Delta.Remove(v) => Delta.Remove(f(v))
      case Delta.Clear() => Delta.Clear()
    })

  def filter(f: T => Boolean): DeltaBufSet[T] =
    DeltaBufSet[T](changes.collect {
      case d @ Delta.Insert(value) if f(value) => d
      case d @ Delta.Remove(value) if f(value) => d
      case d @ Delta.Clear() => d
    })
}

trait StateBufSet[T] extends Disposable {
  import BufSet.Delta

  private[metarx] val elements = mutable.HashSet.empty[T]

  val changes = new RootChannel[Delta[T]] {
    def flush(f: Delta[T] => Unit) {
      elements.foreach(value => f(Delta.Insert(value)))
    }
  }

  private[metarx] val subscription = changes.attach {
    case Delta.Insert(value) =>
      assert(!elements.contains(value), "Value already exists")
      elements += value
    case Delta.Remove(value) =>
      assert(elements.contains(value), "Value does not exist")
      elements -= value
    case Delta.Clear() => elements.clear()
  }

  def dispose() {
    subscription.dispose()
  }
}

trait WriteBufSet[T]
  extends reactive.mutate.BufSet[T]
{
  import BufSet.Delta

  val changes: Channel[Delta[T]]

  def insert(value: T) {
    changes ! Delta.Insert(value)
  }

  def insertAll(values: Set[T]) {
    values.foreach(insert)
  }

  def remove(value: T) {
    changes ! Delta.Remove(value)
  }

  def removeAll(values: Set[T]) {
    values.foreach(remove)
  }

  def replace(reference: T, element: T) {
    remove(reference)
    insert(element)
  }

  def set(values: Set[T]) {
    clear()
    insertAll(values)
  }

  def toggle(state: Boolean, values: T*) {
    if (state) insertAll(values.toSet)
    else removeAll(values.toSet)
  }

  def clear() {
    changes ! Delta.Clear()
  }
}

trait PollBufSet[T]
  extends reactive.poll.Count[T]
  with reactive.poll.Empty
  with reactive.poll.BufSet[T]
{
  import BufSet.Delta

  private[metarx] val elements: mutable.HashSet[T]

  val changes: ReadChannel[Delta[T]]

  def get: Set[T] = elements.toSet

  def foreach(f: T => Unit) {
    elements.foreach(f)
  }

  def isEmpty$: Boolean = elements.isEmpty
  def nonEmpty$: Boolean = elements.nonEmpty
  def contains$(value: T): Boolean = elements.contains(value)

  @deprecated("Use `get`", "v0.1.5")
  def toSet$: Set[T] = elements.toSet

  def toSeq: ReadChannel[Seq[T]] = changes.map(_ => elements.toSeq)
}

trait ReadBufSet[T]
  extends PollBufSet[T]
  with DeltaBufSet[T]

class BufSet[T]
  extends ReadBufSet[T]
  with WriteBufSet[T]
  with StateBufSet[T]
{
  def insertIfNotExists(value: T) {
    if (!contains$(value)) insert(value)
  }

  def removeIfExists(value: T) {
    if (contains$(value)) remove(value)
  }

  def update(f: T => T) {
    set(elements.map(f).toSet)
  }
}
