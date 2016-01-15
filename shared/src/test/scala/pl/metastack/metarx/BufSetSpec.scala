package pl.metastack.metarx

import org.scalatest.FunSuite

class BufSetSpec extends FunSuite {
  test("Creation") {
    val set = BufSet(1, 1, 1)
    assert(set.get == Set(1))
  }

  test("Inserting") {
    val set = BufSet(42)
    set.insertIfNotExists(23)
    assert(set.get == Set(42, 23))
    set.insertIfNotExists(23)
    intercept[AssertionError] {
      set.insert(23)
    }
  }

  test("Removing") {
    val set = BufSet(42)
    set.removeIfExists(42)
    assert(set.get == Set.empty)
    set.removeIfExists(42)
    intercept[AssertionError] {
      set.remove(42)
    }
  }

  test("Replace") {
    val set = BufSet(1, 2, 3)
    set.replace(2, 4)
    assert(set.get == Set(1, 3, 4))
  }

  test("Update") {
    val set = BufSet(1, 2, 3)
    set.update(_ + 1)
    assert(set.get == Set(2, 3, 4))
  }
}
