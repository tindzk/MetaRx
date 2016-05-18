package pl.metastack.metarx

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global

case class Test(a: Int, b: Boolean)

class ChannelTest extends CompatTest {
  test("should never be equal to some other Channel") {
    val a = Channel[Int]()
    val b = Channel[Int]()

    assertEquals(a == b, false)
    assertEquals(a == a, true)
  }

  test("should be usable as key in HashMap") {
    val map = new mutable.HashMap[Channel[Int], Int]()

    val a = Channel[Int]()
    val b = Channel[Int]()

    map += (a -> 1)
    map += (b -> 2)

    assertEquals(map(a), 1)
    assertEquals(map(b), 2)

    a ! 1

    assertEquals(map(a), 1)
    assertEquals(map(b), 2)
  }

  test("head()") {
    var value = 0
    val ch = Var(1)
    val hd = ch.head
    hd.attach(value = _)
    assertEquals(value, 1)
    value = -1
    hd.attach(value = _)
    assertEquals(value, 1)
  }

  test("distinct()") {
    val ch = Var(1)
    val dis = ch.distinct
    ch ! 1
    var sum = 0
    dis.attach(sum += _)
    assertEquals(sum, 1)
  }

  test("distinct() (2)") {
    val ch = Opt[Int](0)
    val ch2 = ch.values.distinct

    var sum = 0
    ch2.attach(sum += _)

    ch := 1
    ch := 1
    ch := 2

    assertEquals(sum, 3)
  }

  test("distinct() (3)") {
    val ch = Opt[Int](0)
    val ch2 = ch.values.distinct

    var sum = 0

    ch := 1
    ch := 1
    ch := 2

    ch2.attach(sum += _)
    assertEquals(sum, 2)
  }

  test("distinct() (4)") {
    val ch = LazyVar[Int](42).distinct

    var sum = 0
    ch.attach(sum += _)

    assertEquals(sum, 42)
  }

  test("attach()") {
    val ch = LazyVar[Int](42)
    var sum = 0
    ch.attach(sum += _)
    assertEquals(sum, 42)
  }

  test("take()") {
    val ch = Channel[Int]()

    var items = mutable.ArrayBuffer.empty[Int]
    ch.take(2).attach(items += _)
    assertEquals(ch.children.size, 1)

    ch ! 1
    ch ! 2

    assertEquals(items, Seq(1, 2))
    assertEquals(ch.children.size, 0)

    ch ! 3
    ch ! 4

    assertEquals(items, Seq(1, 2))
  }

  test("take() (2)") {
    val ch = Var[Int](0)

    var items = mutable.ArrayBuffer.empty[Int]
    ch.take(2).attach(items += _)

    ch ! 1
    ch ! 2
    ch ! 3

    assertEquals(items, Seq(0, 1))
  }

  test("take() (3)") {
    /* Multiple subscribers */
    val ch = Var[Int](0)

    var items = mutable.ArrayBuffer.empty[Int]
    var items2 = mutable.ArrayBuffer.empty[Int]

    val tk = ch.take(2)
    tk.attach(items += _)

    ch := 1

    tk.attach(items2 += _)

    ch := 2
    ch := 3

    assertEquals(items, Seq(0, 1))
    assertEquals(items2, Seq(1))
  }

  test("take() (4)") {
    val ch  = Var(42)
    val dch = ch.take(2)

    val arr  = mutable.ArrayBuffer.empty[Int]
    val arr2 = mutable.ArrayBuffer.empty[Int]

    dch.attach(arr  += _)
    dch.attach(arr2 += _)

    ch ! 23
    ch ! 3

    assertEquals(arr,  mutable.ArrayBuffer(42, 23))
    assertEquals(arr2, mutable.ArrayBuffer(42, 23))
  }

  test("takeWhile()") {
    val ch  = Var(42)
    val dch = ch.takeWhile(_ > 10)

    val arr = mutable.ArrayBuffer.empty[Int]
    dch.attach(arr += _)

    ch ! 50
    ch ! 10
    ch ! 60

    assertEquals(arr, mutable.ArrayBuffer(42, 50))
  }

  test("drop()") {
    val ch = Channel[Int]()

    var sum = 0
    ch.drop(2).attach(sum += _)

    ch ! 1
    ch ! 2
    ch ! 3
    ch ! 4

    assertEquals(sum, 3 + 4)
  }

  test("drop() (2)") {
    val ch = Var(42)
    val dch = ch.drop(1)

    val arr = mutable.ArrayBuffer.empty[Int]
    dch.attach(arr += _)
    dch.attach(arr += _)

    ch ! 5

    assertEquals(arr, mutable.ArrayBuffer(5, 5))
  }

  /*test("toOpt()") {
    val ch = Channel[Test]()
    val cache = ch.toOpt

    val a = cache.value[Int](_ >> 'a)

    var sum = 0
    a.attach(sum += _)

    ch := Test(1, true)
    assertEquals(sum, 1)

    ch := Test(2, false)
    assertEquals(sum, 1 + 2)

    a := 3
    assertEquals(sum, 1 + 2 + 3)
  }

  test("value()") {
    val ch = Var(Test(1, true))

    val a = ch.value[Int](_ >> 'a)

    var sum = 0
    a.attach(sum += _)
    assertEquals(sum, 1)

    var sum2 = 0
    ch.attach(cur => sum2 += cur.a)
    assertEquals(sum2, 1)

    ch := Test(2, false)
    ch := Test(3, true)

    assertEquals(sum, 1 + 2 + 3)
    assertEquals(sum2, 1 + 2 + 3)

    a := 4

    assertEquals(sum, 1 + 2 + 3 + 4)
    assertEquals(sum2, 1 + 2 + 3 + 4)
  }*/

  test("Var()") {
    val ch = Var(42)
    var sum = 0

    ch.attach(value => sum += value)
    assertEquals(sum, 42)

    ch.attach(value => sum += value + 1)
    assertEquals(sum, 42 + 43)
  }

  /*
  test("+()") {
    val ch = Channel[Int]()

    var sum = 0
    val childCh = ch + (sum += (_: Int))

    ch := 42
    assertEquals(sum, 0)

    childCh := 23
    assertEquals(sum, 23)
  }

  test("+() with attach()") {
    val ch = Channel[Int]()
    val childCh = ch + ((_: Int) => ())

    var sum = 0
    childCh.attach(sum += _)

    ch := 42
    assertEquals(sum, 42)
  }

  test("+() with chaining") {
    val ch = Channel[Int]()
    var chSum = 0
    ch.attach(chSum += _)

    val ch2 = Channel[Int]()
    var chSum2 = 0
    ch2.attach(chSum2 += _)

    val ch3 = Channel[Int]()
    var chSum3 = 0
    ch3.attach(chSum3 += _)

    val childCh = ch + ch2 + ch3

    ch := 42
    assertEquals(chSum, 42)
    assertEquals(chSum2, 0)
    assertEquals(chSum3, 0)

    ch2 := 43
    assertEquals(chSum, 42)
    assertEquals(chSum2, 43)
    assertEquals(chSum3, 0)

    ch3 := 44
    assertEquals(chSum, 42)
    assertEquals(chSum2, 43)
    assertEquals(chSum3, 44)

    childCh := 23
    assertEquals(chSum, 42 + 23)
    assertEquals(chSum2, 43 + 23)
    assertEquals(chSum3, 44 + 23)
  }
  */

  test("map()") {
    val ch = Channel[Int]()
    val map = ch.map(_ + 1)

    var sum = 0
    map.attach(value => sum += value)
    ch ! 42
    assertEquals(sum, 43)

    map.attach(value => sum += value + 1)
    ch ! 43
    assertEquals(sum, 43 + 44 + 45)
  }

  test("zip()") {
    val ch = Var(0)
    val ch2 = Var(1)

    val zip = ch.zip(ch2)

    var values = mutable.ArrayBuffer.empty[(Int, Int)]
    zip.attach(values += _)

    assertEquals(values, Seq((0, 1)))

    ch ! 2
    assertEquals(values, Seq((0, 1), (2, 1)))
  }

  test("zip() (2)") {
    val ch = Var(0)
    val ch2 = Channel[Int]()

    val zip = ch.zip(ch2)

    var value = (-1, -1)
    zip.attach(value = _)

    ch2 ! 42
    assertEquals(value, (0, 42))

    ch := 23
    ch2 ! 42
    assertEquals(value, (23, 42))

    ch := 24
    assertEquals(value, (23, 42))

    ch2 ! 43
    assertEquals(value, (24, 43))
  }

  test("zip() (3)") {
    val var1 = Var(1)
    val var2 = Var(2)

    var values = mutable.ArrayBuffer.empty[(Int, Int)]
    var1.zip(var2).attach(values += _)

    assertEquals(values, Seq((1, 2)))

    var1 := 3
    assertEquals(values, Seq((1, 2), (3, 2)))

    var2 := 4
    assertEquals(values, Seq((1, 2), (3, 2), (3, 4)))
  }

  test("zip() with multiple inputs") {
    val ch = Var(0)
    val ch2 = Var(1)
    val ch3 = Var("abc")
    val ch4 = Var(3)

    val zip = ch.zip(ch2, ch3, ch4)

    var values = mutable.ArrayBuffer.empty[(Int, Int, String, Int)]
    zip.attach(values += _)

    assertEquals(values, Seq((0, 1, "abc", 3)))

    ch4 := 10
    ch3 := "def"
    assertEquals(values, Seq((0, 1, "abc", 3), (0, 1, "abc", 10), (0, 1, "def", 10)))
  }

  test("zipWith()") {
    val ch = Var(0)
    val ch2 = Var(1)

    val zip = ch.zipWith(ch2)(_ + _)

    var values = mutable.ArrayBuffer.empty[Int]
    zip.attach(values += _)

    assertEquals(values, Seq(1))

    ch ! 2
    assertEquals(values, Seq(1, 3))
  }

  test("flatMap()") {
    val ch = Channel[Int]()

    val ch2 = Var(0)
    var values = mutable.ArrayBuffer.empty[Int]
    ch2.attach(values += _)

    val fmap = ch.flatMap(_ => ch2)

    var cur = -1
    fmap.attach(cur = _)

    ch ! 1
    assertEquals(cur, 0)
    assertEquals(values, mutable.ArrayBuffer(0))

    ch ! 2
    ch2 ! 2 /* Previous handlers attached to ch2 must be still valid. */
    assertEquals(cur, 2)
    assertEquals(values, mutable.ArrayBuffer(0, 2))
  }

  test("flatMap() (2)") {
    val ch = Channel[Int]()

    val ch2 = Var(2)
    val ch3 = Var(3)

    val fmap = ch.flatMap {
      case 0 => ch2
      case 1 => ch3
    }

    var cur = -1
    fmap.attach(cur = _)

    ch ! 0
    assertEquals(cur, 2)
    ch2 ! 42
    assertEquals(cur, 42)

    ch ! 1
    assertEquals(cur, 3)
    ch3 ! 42
    ch2 ! 50 /* ch2 should not be attached anymore to flatMap(). */
    assertEquals(cur, 42)
  }

  test("flatMap() (3)") {
    var value = ""
    Var(42)
      .flatMap(x => Var(x.toString))
      .filter(_ => true)
      .attach(value = _)
    assertEquals(value, "42")
  }

  test("flatMapCh()") {
    val ch = Channel[Var[Int]]()
    val a = ch.flatMapCh(cur => cur)
    val b = ch.flatMapCh(cur => cur)

    ch ! Var(42)
  }

  /* test("flatMapCh()") {
    val ch = Channel[Var[Test]]()
    val a = ch.flatMapCh(_.value[Int](_ >> 'a))
    val b = ch.flatMapCh(_.value[Boolean](_ >> 'b))

    var sum = 0
    a.attach(sum += _)
    b.attach(cur => sum += (if (cur) 1 else 0))

    assertEquals(sum, 0)

    val v = Var(Test(2, false))
    ch := v
    assertEquals(sum, 2)

    sum = 0
    v := Test(3, false)
    assertEquals(sum, 3)
  } */

  test("flatMapCh() (2)") {
    val ch = Opt[Int]()
    val ch2 = Channel[Int]()

    val map = ch.flatMapCh(_ => ch2)

    var sum = 0
    ch2.attach(sum += _)

    map ! 1
    ch := 0

    map ! 5
    assertEquals(sum, 5)
  }

  test("flatMapCh() (3)") {
    val ch = Opt[Int]()
    val map = ch.flatMapCh(_ => Var(42))

    var sum = 0

    ch.values.attach(sum += _)
    map.attach(sum += _)

    ch := 0
    ch := 0

    assertEquals(sum, 84)
  }

  test("writeTo()") {
    val chIn = Channel[Int]()
    val chOut = Channel[Int]()

    var out = -1
    chOut.attach(out = _)

    val ch = chIn.writeTo(chOut)

    chIn ! 1
    assertEquals(out, -1)

    ch ! 1
    assertEquals(out, 1)
  }

  test("merge()") {
    val ch = Var[Int](5)
    val ch2 = Channel[Int]()

    var out = -1
    ch.merge(ch2).attach(out = _)

    assertEquals(out, 5)

    ch ! 1
    assertEquals(out, 1)

    ch2 ! 2
    assertEquals(out, 2)
  }

  test("tail()") {
    val ch = Var[Int](42)

    var out = -1
    ch.tail.attach(out = _)

    assertEquals(out, -1)

    ch ! 43
    assertEquals(out, 43)
  }

  test("isEmpty()") {
    val ch = Channel[Int]()

    var states = mutable.ArrayBuffer.empty[Boolean]
    ch.isEmpty.attach(states += _)

    ch ! 1
    ch ! 2

    assertEquals(states, mutable.ArrayBuffer(true, false))
  }

  test("isEmpty() (2)") {
    val ch = Var[Int](2)

    var states = mutable.ArrayBuffer.empty[Boolean]
    ch.isEmpty.attach(states += _)

    ch ! 1

    assertEquals(states, mutable.ArrayBuffer(false))
  }

  test("nonEmpty()") {
    val ch = Channel[Int]()

    var states = mutable.ArrayBuffer.empty[Boolean]
    ch.nonEmpty.attach(states += _)

    ch ! 1
    ch ! 2

    assertEquals(states, mutable.ArrayBuffer(false, true))
  }

  test("nonEmpty() (2)") {
    val ch = Var[Int](2)

    var states = mutable.ArrayBuffer.empty[Boolean]
    ch.nonEmpty.attach(states += _)

    ch ! 1

    assertEquals(states, mutable.ArrayBuffer(true))
  }

  test("collect()") {
    val ch = Channel[Int]()
    val map = ch.collect { case 1 => 42 }

    var states = mutable.ArrayBuffer.empty[Int]
    map.attach(states += _)

    ch ! 2
    ch ! 3
    ch ! 1
    ch ! 4

    assertEquals(states, mutable.ArrayBuffer(42))
  }

  test("collect() (2)") {
    val ch = Buffer[Int](1, 2, 3).buffer
    val map = ch.changes.collect { case Buffer.Delta.Insert(_, 2) => 42 }

    var states = mutable.ArrayBuffer.empty[Int]
    map.attach(states += _)

    assertEquals(states, mutable.ArrayBuffer(42))
  }

  test("Channel()") {
    val rd = Channel[Int]()
    val wr = Channel[Int]()

    val ch = Channel(rd, wr)

    var states = mutable.ArrayBuffer.empty[Int]
    ch.attach(states += _)

    var wrStates = mutable.ArrayBuffer.empty[Int]
    wr.attach(wrStates += _)

    rd ! 1
    assertEquals(states, mutable.ArrayBuffer(1))
    assertEquals(wrStates, mutable.ArrayBuffer())

    ch ! 2
    assertEquals(states, mutable.ArrayBuffer(1, 2))
    assertEquals(wrStates, mutable.ArrayBuffer(2))
  }

  test("state()") {
    val chStr = Channel[String]()

    val strValues = mutable.ArrayBuffer.empty[String]
    chStr.attach(strValues += _)

    val varInt = chStr.biMap[Int](_.toInt, _.toString).state
    assertEquals(strValues, Seq())

    assertEquals(varInt.get, None)

    varInt := 1
    assertEquals(strValues, Seq("1"))
  }

  test("state(value)") {
    val chStr = Channel[String]()

    val strValues = mutable.ArrayBuffer.empty[String]
    chStr.attach(strValues += _)

    val varInt = chStr.biMap[Int](_.toInt, _.toString).state(1)
    assertEquals(strValues, Seq("1"))

    assertEquals(varInt.get, 1)

    varInt ! 2
    assertEquals(strValues, Seq("1", "2"))
  }

  test("values()") {
    val intValues = mutable.ArrayBuffer.empty[Int]

    val ch = Var[Option[Int]](None)
    ch.values.attach(intValues += _)
    assertEquals(intValues, Seq.empty)

    ch ! Some(42)
    assertEquals(intValues, Seq(42))
  }

  test("Conversion from Future[_]") {
    val intValues = mutable.ArrayBuffer.empty[Int]

    val p = Promise[Int]()
    val f = p.future

    val ch = Channel.from(f)
    ch.attach(intValues += _)

    p.success(42)
    f.onComplete { v =>
      assertEquals(intValues, Seq(42))
    }
  }

  test("Logical tests for ReadChannel[Boolean]") {
    import pl.metastack.metarx._

    val ch1 = Var[Boolean](true)
    val chRes = !ch1

    val states = mutable.ArrayBuffer.empty[Boolean]
    chRes.attach(states += _)

    ch1 := false
    assertEquals(states, mutable.ArrayBuffer(false, true))

    val trueRes = ch1.onTrue
    val falseRes = ch1.onFalse

    var numTrues = 0
    trueRes.attach(_ => numTrues += 1)
    var numFalses = 0
    falseRes.attach(_ => numFalses += 1)
    assertEquals(numTrues, 0)
    assertEquals(numFalses, 1)

    ch1 ! true
    assertEquals(numTrues, 1)
    assertEquals(numFalses, 1)

    ch1 ! false
    assertEquals(numTrues, 1)
    assertEquals(numFalses, 2)
  }

  test("Logical tests for Opt[Boolean]") {
    import pl.metastack.metarx._

    val opt = Opt[Boolean]()
    var count = 0

    val trueCh = opt.onTrue
    trueCh.attach(_ => count += 1)

    opt := false
    assert(count == 0)

    opt := true
    assert(count == 1)
  }

  test("Logical operators for ReadChannel[Boolean]") {
    import pl.metastack.metarx._

    val ch1 = Var(false)
    val ch2 = Var(false)

    val andRes = Channel[Boolean]()
    var andStates = mutable.ArrayBuffer.empty[Boolean]
    andRes.attach(andStates += _)

    val orRes = Channel[Boolean]()
    var orStates = mutable.ArrayBuffer.empty[Boolean]
    orRes.attach(orStates += _)

    andRes << (ch1 && ch2)
    orRes << (ch1 || ch2)

    ch1 := true

    ch1 := false
    ch2 := true

    ch1 := true

    assertEquals(andStates, mutable.ArrayBuffer(false, false, false, false, true))
    assertEquals(orStates, mutable.ArrayBuffer(false, true, false, true, true))
  }

  test("Logical operators for Var[Boolean]") {
    import pl.metastack.metarx._

    val ch1 = Var[Boolean](false)
    val ch2 = Var[Boolean](false)

    val andRes = Channel[Boolean]()
    var andStates = mutable.ArrayBuffer.empty[Boolean]
    andRes.attach(andStates += _)

    val orRes = Channel[Boolean]()
    var orStates = mutable.ArrayBuffer.empty[Boolean]
    orRes.attach(orStates += _)

    andRes << (ch1 && ch2)
    orRes << (ch1 || ch2)

    ch1 := true
    ch2 := true

    ch2 := false

    assertEquals(andStates, mutable.ArrayBuffer(false, false, true, false))
    assertEquals(orStates, mutable.ArrayBuffer(false, true, true, true))
  }

  test("Logical && operator between Channel[Boolean] and Boolean") {
    import pl.metastack.metarx._

    val ch = Var(false)
    var inputBoolean = false

    val andRes = ch && inputBoolean
    var andStates = mutable.ArrayBuffer.empty[Boolean]
    andRes.attach(andStates += _)

    ch := true

    inputBoolean = true

    andRes.dispose()
    val andRes2 = ch && inputBoolean
    var andStates2 = mutable.ArrayBuffer.empty[Boolean]
    andRes2.attach(andStates2 += _)
    ch := false

    assertEquals(andStates, mutable.ArrayBuffer(false, false))
    assertEquals(andStates2, mutable.ArrayBuffer(true, false))
  }

  test("Logical && operator between Opt[Boolean] and Boolean") {
    import pl.metastack.metarx._

    val ch = Opt[Boolean]()

    val and = ch && false
    var states = mutable.ArrayBuffer.empty[Option[Boolean]]
    and.attach(states += _)

    ch := true

    assertEquals(states, mutable.ArrayBuffer(None, Some(false)))
  }

  test("Logical || operator between Channel[Boolean] and Boolean") {
    import pl.metastack.metarx._

    val ch = Var(false)
    var inputBoolean = false

    val orRes = ch || inputBoolean
    var orStates = mutable.ArrayBuffer.empty[Boolean]
    orRes.attach(orStates += _)

    ch ! true

    orRes.dispose()
    inputBoolean = true
    val orRes2 = ch || inputBoolean
    var orStates2 = mutable.ArrayBuffer.empty[Boolean]
    orRes2.attach(orStates2 += _)

    ch ! false

    assertEquals(orStates, mutable.ArrayBuffer(false, true))
    assertEquals(orStates2, mutable.ArrayBuffer(true, true))
  }

  test("Logical operators between Boolean and Channel[Boolean]") {
    import pl.metastack.metarx._

    val ch = Channel[Boolean]()
    var inputBoolean = false

    val andRes = Channel[Boolean]()
    var andStates = mutable.ArrayBuffer.empty[Boolean]
    andRes.attach(andStates += _)

    val orRes = Channel[Boolean]()
    var orStates = mutable.ArrayBuffer.empty[Boolean]
    orRes.attach(orStates += _)

    andRes << (inputBoolean && ch)
    orRes << (inputBoolean || ch)

    ch ! false
    ch ! true

    inputBoolean = true
    andRes << (ch && inputBoolean)
    orRes << (ch || inputBoolean)
    ch ! false
    ch ! true

    assertEquals(andStates, mutable.ArrayBuffer(false, false, false, false, false, true))
    assertEquals(orStates, mutable.ArrayBuffer(false, true, false, true, true, true))
  }

  test("Logical operators between Var[Numeric]") {
    val ch1 = Var[Int](5)
    val ch2 = Var[Int](4)

    val plusRes = ch1 + ch2
    var plusStates = mutable.ArrayBuffer.empty[Int]
    plusRes.attach(plusStates += _)

    val negRes = ch1 - ch2
    var negStates = mutable.ArrayBuffer.empty[Int]
    negRes.attach(negStates += _)

    assertEquals(plusStates, mutable.ArrayBuffer(9))
    assertEquals(negStates, mutable.ArrayBuffer(1))
  }

  test("Logical division between Var[Integral]") {
    val ch1 = Var(5)
    val ch2 = Var(4)

    val divRes = ch1 / ch2
    var divStates = mutable.ArrayBuffer.empty[Int]
    divRes.attach(divStates += _)

    val remRes = ch1 % ch2
    var remStates = mutable.ArrayBuffer.empty[Double]
    remRes.attach(remStates += _)

    ch1 := 12
    ch2 := 5

    assertEquals(divStates, mutable.ArrayBuffer(1, 3, 2))
    assertEquals(remStates, mutable.ArrayBuffer(1, 0, 2))
  }

  test("Logical division between Integral and Channel[Integral]") {
    val var1 = Var[Int](5)
    val res = 10 / var1

    assertEquals(res.cache.get, Some(2))
  }

  test("Logical division between Channel[Fractional]") {
    val ch1 = Var[Double](9)
    val ch2 = Var[Double](2)

    val divRes = ch1 / ch2
    var divStates = mutable.ArrayBuffer.empty[Double]
    divRes.attach(divStates += _)

    ch1 := 5
    ch2 := 4

    assertEquals(divStates, mutable.ArrayBuffer(4.5, 2.5, 1.25))
  }

  test("Logical division between Fractional and Channel[Fractional]") {
    val var1 = Var(5.0)
    val res = 10.0 / var1

    assertEquals(res.cache.get, Some(2.0))
  }

  test("Logical operators for Var[Ordering]") {
    import pl.metastack.metarx._

    val ch1 = Var[Int](6)
    val ch2 = Var[Int](5)

    val ch1Bigger = ch1 > ch2
    var ch1BiggerStates = mutable.ArrayBuffer.empty[Boolean]
    ch1Bigger.attach(ch1BiggerStates += _)

    val ch1LessOrEq = ch1 <= ch2
    var ch1LessOrEqStates = mutable.ArrayBuffer.empty[Boolean]
    ch1LessOrEq.attach(ch1LessOrEqStates += _)

    ch2 ! 6
    ch2 ! 5

    assertEquals(ch1BiggerStates, mutable.ArrayBuffer(true, false, true))
    assertEquals(ch1LessOrEqStates, mutable.ArrayBuffer(false, true, false))
  }

  test("Logical operators between Ordering and Channel[Ordering]") {
    val ch1 = Channel[Int]()

    val ch1Bigger = 5 < ch1
    var ch1BiggerStates = mutable.ArrayBuffer.empty[Boolean]
    ch1Bigger.attach(ch1BiggerStates += _)

    ch1 ! 5
    ch1 ! 4
    ch1 ! 6

    assertEquals(ch1BiggerStates, mutable.Buffer(false, false, true))
  }

  test("Extra operations for Channel[String]") {
    val test1 = "Test1"
    val test2 = "Test2"
    val concated = test1 + test2

    val ch1 = Var(test1)
    val ch2 = Var(test2)
    val ch3 = ch1 + ch2

    assertEquals(ch3.cache.get, Some(concated))

    val ch4 = ch1 + test2
    assertEquals(ch4.cache.get, Some(concated))
  }

  test("throttle()") {
    import scala.concurrent.duration._
    import Platform.DefaultScheduler
    val scheduler = implicitly[Scheduler]

    val ch = Channel[Int]()
    val throttled = ch.throttle(600.millis)

    var i = 0
    val task = scheduler.schedule(500.millis) {
      ch ! i
      i += 1
    }

    val collected = ArrayBuffer.empty[Int]
    throttled.attach(collected += _)

    scheduler.scheduleOnce(2000.millis) {
      assertEquals(collected, Seq(0, 2))
      task.cancel()
    }
  }

  test("for ... yield") {
    val var1 = Var(1)
    val var2 = Var(1)

    val values = for {
      x <- var1 if x > 1
      y <- var2 if y > 1
    } yield (x, y)

    val collected = mutable.ArrayBuffer.empty[(Int, Int)]
    values.attach(collected += _)

    var1 := 2
    var2 := 5
    var1 := 3

    assertEquals(collected, Seq((2, 5), (3, 5)))
  }
}
