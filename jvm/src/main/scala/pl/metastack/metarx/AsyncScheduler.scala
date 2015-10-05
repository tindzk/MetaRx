package pl.metastack.metarx

import java.util.concurrent.{ScheduledExecutorService, TimeUnit}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class AsyncScheduler(s: ScheduledExecutorService,
                     ec: ExecutionContext) extends Scheduler {
  def schedule(interval: FiniteDuration, r: Runnable): Cancelable =
    schedule(interval.length, interval.unit, r)

  def schedule(interval: Long, unit: TimeUnit, r: Runnable): Cancelable = {
    require(interval > 0)
    val initialDelay = interval
    val task = s.scheduleAtFixedRate(r, initialDelay, interval, unit)
    Cancelable(task.cancel(true))
  }

  def scheduleOnce(initialDelay: FiniteDuration, r: Runnable): Cancelable =
    scheduleOnce(initialDelay.length, initialDelay.unit, r)

  def scheduleOnce(initialDelay: Long, unit: TimeUnit, r: Runnable): Cancelable = {
    if (initialDelay <= 0) {
      ec.execute(r)
      Cancelable()
    } else {
      val task = s.schedule(r, initialDelay, unit)
      Cancelable(task.cancel(true))
    }
  }
}
