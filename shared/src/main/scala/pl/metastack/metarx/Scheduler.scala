package pl.metastack.metarx

import scala.concurrent.duration._

/**
 * Inspired from Monifu's scheduling code.
 */
trait Scheduler {
  def schedule(interval: FiniteDuration, r: Runnable): Cancelable
  def scheduleOnce(action: Runnable): Cancelable
  def scheduleOnce(initialDelay: FiniteDuration, action: Runnable): Cancelable

  def schedule(interval: FiniteDuration)(action: => Unit): Cancelable =
    schedule(interval, new Runnable {
      def run(): Unit = action
    })

  def scheduleOnce(action: => Unit): Cancelable =
    scheduleOnce(new Runnable {
      def run(): Unit = action
    })

  def scheduleOnce(initialDelay: FiniteDuration)(action: => Unit): Cancelable =
    scheduleOnce(initialDelay, new Runnable {
      def run(): Unit = action
    })

  def currentTimeMillis(): Long = System.currentTimeMillis()
}

trait Cancelable {
  def cancel(): Boolean
}

object Cancelable {
  def apply(): Cancelable = apply({})

  def apply(callback: => Unit): Cancelable =
    new Cancelable {
      var isCanceled = false

      def cancel(): Boolean =
        if (isCanceled) false
        else {
          isCanceled = true
          callback
          true
        }
    }
}

trait DefaultScheduler {
  implicit val scheduler = new SyncScheduler
}
