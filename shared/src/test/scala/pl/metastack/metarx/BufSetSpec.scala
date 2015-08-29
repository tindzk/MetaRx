package pl.metastack.metarx

import minitest._

object BufSetSpec extends SimpleTestSuite {
  test("Creation") {
    val set = BufSet(1, 1, 1)
    assertEquals(set.toSet$, Set(1))
  }

  test("Inserting") {
    val set = BufSet(42)
    set.insertIfNotExists(23)
    assertEquals(set.toSet$, Set(42, 23))
    set.insertIfNotExists(23)
    intercept[AssertionError] {
      set.insert(23)
    }
  }

  test("Removing") {
    val set = BufSet(42)
    set.removeIfExists(42)
    assertEquals(set.toSet$, Set.empty)
    set.removeIfExists(42)
    intercept[AssertionError] {
      set.remove(42)
    }
  }
}
