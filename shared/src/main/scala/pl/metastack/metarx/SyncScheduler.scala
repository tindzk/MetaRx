package pl.metastack.metarx

import scala.concurrent.duration._

class SyncScheduler extends Scheduler {
  override def schedule(interval: FiniteDuration, r: Runnable): Cancelable = ???

  override def scheduleOnce(r: Runnable): Cancelable = {
    r.run()
    Cancelable()
  }

  override def scheduleOnce(initialDelay: FiniteDuration,
                            r: Runnable
                           ): Cancelable = ???
}
