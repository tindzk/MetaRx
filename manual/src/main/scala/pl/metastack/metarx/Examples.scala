package pl.metastack.metarx

import pl.metastack.metadocs.SectionSupport

import scala.collection.mutable.ArrayBuffer

object Examples extends SectionSupport {
  section("produce") {
    val result = ArrayBuffer.empty[Int]

    val ch = Channel[Int]() // initialise
    ch.attach(result += _)  // attach observer
    ch := 42                // produce value

    result
  }
}
