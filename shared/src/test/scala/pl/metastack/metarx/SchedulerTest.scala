package pl.metastack.metarx

import minitest.SimpleTestSuite

import scala.collection.mutable.ArrayBuffer

object SchedulerTest extends SimpleTestSuite {

  test("schedule()") {
    import scala.concurrent.duration._
    val ch = Channel[Int]()

    val scheduler: Scheduler = Platform.DefaultScheduler
    var i = 0
    val task = scheduler.schedule(500.millis) {
      ch := i
      i += 1
    }

    val collected = ArrayBuffer.empty[Int]
    ch.attach(collected += _)

    scheduler.scheduleOnce(2100.millis) {
      assertEquals(collected, Seq(0, 1, 2, 3))
      task.cancel()
    }
  }

}
