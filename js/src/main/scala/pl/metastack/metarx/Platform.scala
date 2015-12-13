package pl.metastack.metarx

object Platform extends DefaultScheduler {
  implicit lazy val DefaultScheduler: Scheduler = new AsyncScheduler
}
