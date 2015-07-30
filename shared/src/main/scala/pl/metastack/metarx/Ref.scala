package pl.metastack.metarx

/**
 * Ref makes references to values explicit. In Scala, objects may have different
 * equality semantics. For example, case classes always implement structural
 * equality, but ordinary classes not necessarily. To use different instances of
 * the same value in a hash table, all objects must be wrapped. Ref is a simple
 * solution for this and ensures that physical equality is always performed as
 * hashCode cannot be overridden.
 */
sealed class Ref[T](val get: T) {
  override def toString = get.toString
}

object Ref {
  def apply[T](get: T) = new Ref[T](get)
  def unapply[T](ref: Ref[T]): Option[T] = Some(ref.get)
}
