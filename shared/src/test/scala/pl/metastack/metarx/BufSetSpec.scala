package pl.metastack.metarx

class BufSetSpec extends CompatTest {
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

  test("Replace") {
    val set = BufSet(1, 2, 3)
    set.replace(2, 4)
    assertEquals(set.toSet$, Set(1, 3, 4))
  }

  test("Update") {
    val set = BufSet(1, 2, 3)
    set.update(_ + 1)
    assertEquals(set.toSet$, Set(2, 3, 4))
  }
}
