package pl.metastack.metarx

object Platform {
  implicit lazy val DefaultScheduler: Scheduler = new AsyncScheduler
}
