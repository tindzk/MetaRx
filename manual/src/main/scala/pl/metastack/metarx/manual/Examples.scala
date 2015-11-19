package pl.metastack.metarx.manual

import pl.metastack.metadocs.SectionSupport

import scala.collection.mutable.ArrayBuffer

object Examples extends SectionSupport {
  section("introduction") {
    import pl.metastack.metarx._

    val ch      = Channel[Int]()
    val plusOne = ch.map(_ + 1)
    val isOne   = plusOne === 1

    isOne.attach(println)

    ch := 1
  }

  import pl.metastack.metarx._

  section("reactive-programming") {
    val m: Var[Int] = Var(5)
    val b: Var[Int] = Var(10)

    // Produces when user provided `m` and `b`
    val mAndB: ReadChannel[(Int, Int)] = m.zip(b)

    // Function channel to calculate `y` for current input
    val y: ReadChannel[Int => Int] =
      mAndB.map { case (m, b) =>
        (x: Int) => m * x + b
      }

    // Render `y` for inputs [-5, 5]
    val values: ReadChannel[Seq[Int]] = y.map((-5 to 5).map(_))
    values.attach(println)

    // Simulate change of `m` in UI
    m := 10
  }

  section("produce") {
    val ch = Channel[Int]() // initialise
    ch.attach(println)      // attach observer
    ch := 42                // produce value
  }

  section("chaining") {
    val ch = Channel[Int]()
    ch.filter(_ > 3)
      .map(_ + 1)
      .attach(println)
    ch := 42
    ch := 1
  }

  section("merging") {
    val a = Channel[String]()
    val b = Channel[String]()
    val c = Channel[String]()

    val merged: ReadChannel[String] = a.merge(b).merge(c)
    merged.attach(println)

    c := "test"
  }

  section("or") {
    val a = Channel[String]()
    val b = Channel[String]()
    val c = Channel[String]()

    val or: ReadChannel[Unit] = a | b | c
    or.attach(println)

    b := "test"
  }

  section("logical-operators") {
    val a = Channel[Boolean]()
    val b = Channel[Boolean]()

    // a.zip(b).map { case (aVal, bVal) => aVal && bVal }
    val aAndB: ReadChannel[Boolean] = a && b

    // a.zip(b).map { case (aVal, bVal) => aVal || bVal }
    val aOrB: ReadChannel[Boolean] = a || b

    // a.isFalse()
    val notA = !a
  }

  section("arithmetic-operators") {
    val a = Channel[Int]()
    val b = Channel[Int]()

    val c: ReadChannel[Int] = 5 - 2 * a + 3 / b
    val d: ReadChannel[Boolean] = c >= 42
  }

  section("state-channel") {
    val ch = Var(42)
    ch.attach(println)

    val ch2 = Channel[Int]()
    ch2 := 42  // Value is lost as ch2 does not have any observers
    ch2.attach(println)
  }

  section("state-channel-update") {
    val ch = Var(2)
    ch.attach(println)
    ch.update(_ + 1)
  }

  section("opt") {
    val x = Opt[Int]()
    x := 42
  }

  section("opt-default") {
    val x = Opt(42)
  }

  section("cache") {
    val ch = Channel[Int]()
    val chPart:  ReadPartialChannel[Int] = ch.cache
    val chState: ReadStateChannel[Int]   = ch.cache(42)
  }

  section("state") {
    val ch = Channel[Int]()
    val chOpt: Opt[Int] = ch.state
    val chVar: Var[Int] = ch.state(42)
  }

  section("bimap") {
    val map   = Map(1 -> "one", 2 -> "two", 3 -> "three")
    val id    = Var(2)
    val idMap = id.biMap(
      (id: Int)     => map(id),
      (str: String) => map.find(_._2 == str).get._1)
    id   .attach(x => println("id   : " + x))
    idMap.attach(x => println("idMap: " + x))
    idMap := "three"
  }

  section("bimap-lens") {
    case class Test(a: Int, b: Int)
    val test = Var(Test(1, 2))
    val lens = test.biMap(_.b, (x: Int) => test.get.copy(b = x))
    test.attach(println)
    lens := 42
  }

  section("lazyvar") {
    var counter = 0
    val ch = LazyVar(counter)
    ch.attach(value => { counter += 1; println(value) })
    ch.attach(value => { counter += 1; println(value) })
  }

  section("call-semantics") {
    val ch = Var(42).map(i => { println(i); i + 1 })
    ch.attach(_ => ())
    ch.attach(_ => ())
  }

  section("call-semantics-drop") {
    val ch = Var(42)
    val dch = ch.drop(1)
    dch.attach(println)
    dch.attach(println)
    ch := 23
  }

  sectionNoExec("cycle") {
    val todo = Channel[String]()
    todo.attach { t =>
      println(t)
      todo := ""
    }
    todo := "42"
  }

  section("buffer") {
    val buf = Buffer(1, 2, 3)
    buf.size.attach(println)
    buf += 4
  }

  section("buffer-distinct") {
    val buf = Buffer(1, 2, 2, 3)
    println(buf.distinct$)
  }

  section("buffer-remove-all") {
    val buf  = Buffer(3, 4, 5)
    val mod2 = buf.filter$(_ % 2 == 0)

    buf.removeAll(mod2.get)
  }

  sectionNoExec("buffer-ref") {
    case class Todo(value: String)
    val todos = Buffer[Ref[Todo]]()
    todos.map { case tr @ Ref(t) =>
      // ...
    }
  }

  section("buffer-changes") {
    val buf = Buffer(1, 2, 3)
    buf.changes.attach(println)
    buf += 4
    buf.clear()
  }
}
