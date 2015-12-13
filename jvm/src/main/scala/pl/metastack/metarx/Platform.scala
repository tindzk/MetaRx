package pl.metastack.metarx

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

object Platform extends DefaultScheduler {
  implicit lazy val DefaultScheduler: Scheduler = new AsyncScheduler(
    Executors.newSingleThreadScheduledExecutor(),
    ExecutionContext.Implicits.global)
}
