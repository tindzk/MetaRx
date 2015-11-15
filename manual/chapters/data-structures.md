[package value="pl.metastack.metarx"]
# Reactive data structures
MetaRx currently implements four reactive data structures:

- **Channels:** single values like ``T``
- **Buffers:** lists like ``Seq[T]``
- **Dictionaries:**  maps like ``Map[A, B]``
- **Sets:**  reactive ``Set[T]``

## Channels
A channel models continuous values as a stream. It serves as a multiplexer for typed messages that consist of immutable values. Messages sent to the channel get propagated to the observers that have been attached to the channel — in the same order as they were added. It is possible to operate on channels with higher-order functions such as ``map()``, ``filter()`` or ``take()``. These methods may be chained, such that every produced values is propagated down the observer chain.

MetaRx differentiates between two top-level channel types:

- **Channel:** corresponds to a reactive ``T``
- **Partial channel:** corresponds to a reactive ``Option[T]``

There are four channel implementations:

- ``Channel``: stream that does not persist its values
- ``Var``: variable stream; its value is always defined and has an initial value[footnote]{In Rx terms, ``Var`` would correspond to a *cold observer* as attaching to it will flush its current value. This is different from ``Channel`` which loses its messages when there are no subscribers.}
- ``LazyVar``: stream for lazily evaluated variables
- ``PtrVar``: stream for generic events[footnote]{It can be used to create delta channels from DOM variables by binding to the corresponding events that triggered by the value changes. For an example see ``Node.click``.}

Partial channels model optional values:

- ``PartialChannel``: base type
- ``Opt``: stream that has two states, either *defined with a value* or *undefined*

> **Note:** ``Opt[T]`` is merely a convenience type and ``Var[Option[T]]`` could be used, too.

### Operations
Here is a simple example for a channel that receives integers. We register an observer which prints all values on the console:

[scala type="section" value="produce" file="Examples"]

> **Note:** The ``:=`` operator is a shortcut for the method ``produce``.

The return values of operations are channels, therefore chaining is possible. Channels can be used to express data dependencies:

```scala
val ch = Channel[Int]()
ch.filter(_ > 3)
  .map(_ + 1)
  .attach(println)
ch := 42  // 43 printed
ch := 1   // nothing printed
```

Use the method ``distinct`` to produce a value if it is the first or different from the previous one. A use case is to perform time-consuming operations such as performing HTTP requests only once for the same user input:

```scala
ch.distinct.attach { query =>
  // perform HTTP request
}
```

Considering that you want to observe multiple channels of the same type and merge the produced values, you can use the ``|`` operator[footnote]{It is an alias for the method ``merge()``}:

```scala
val a = Channel[String]()
val b = Channel[String]()
val c = Channel[String]()

val merged: ReadChannel[String] = a | b | c
```

It must be noted that streaming operations have different semantics than their non-reactive counterparts. For brevity, only certain combinators are covered by the manual. For the rest, please refer to the ScalaDoc documentation.

For Boolean channels, the logical operators are defined, and give a new channel with the result:

```scala
val a = Channel[Boolean]()
val b = Channel[Boolean]()

val aAndB: ReadChannel[Boolean] = a && b // same as a.zip(b).map { case (aVal, bVal) => aVal && bVal }
val aOrB: ReadChannel[Boolean] = a || b
val notA = !a // or a.isFalse()
```

Furthermore `onTrue()` and `onFalse()` are defined and will give a `ReadChannel[Unit]` that trigger when either true or false value is received.

Aritmetic operators like `+, -, *, /, %, <, <=, >, >=, ===, !==` are also supported for types that define them (supported by Numeric/Ordering traits):

```scala
val a = Channel[Int]()
val b = Channel[Int]()

val c: ReadChannel[Int] = 5 - 2 * a + 3 / b
val d: ReadChannel[Boolean] = c >= 42
```

### State channels
For better performance, ``Channel`` does not cache the produced values. Some operations cannot be implemented without access to the current value, though. And often it is necessary to poll the current value. For these reasons *state channels* such as ``Var`` or ``Opt`` were introduced. The following example visualises the different behaviours:

```scala
val ch = Var(42)
ch.attach(println)  // prints 42

val ch2 = Channel[Int]()
ch2 := 42  // Value is lost as ch2 does not have any observers
ch2.attach(println)
```

``update()`` is an operation that requires that the produced values are persisted. ``update()`` takes a function which modifies the current value:

```scala
val ch = Var(2)
ch.attach(println)
ch.update(_ + 1)  // produces 3
```

A partially-defined channel (``Opt``) is constructed as follows:

```scala
val x = Opt[Int]()
x := 42
```

Alternatively, a default value may be passed:

```scala
val x = Opt(42)
```

A state channel provides all the methods a channel does. ``Var[T]`` and ``Opt[T]`` can be obtained from any existing ``ReadChannel[T]`` using the method ``cache``:

```scala
val chOpt = ch.cache      // Opt[Int]
val chVar = ch.cache(42)  // Var[Int]
```

``chOpt`` is undefined as long as no value was produced on ``ch``. ``chVar`` will be initialised with 42 and the value is overridden with the first produced value on ``ch``.

``biMap()`` allows to implement a bi-directional map, i.e. a stream with back-propagation:

```scala
val map   = Map(1 -> "one", 2 -> "two", 3 -> "three")
val id    = Var(2)
val idMap = id.biMap(
  (id: Int)     => map(id)
, (str: String) => map.find(_._2 == str).get._1)
id   .attach(x => println("id   : " + x))
idMap.attach(x => println("idMap: " + x))
idMap := "three"
```

The output is:

```
id   : 2
idMap: two
id   : 3
idMap: three
```

``biMap()`` can be used to implement a lens as a channel. The following example defines a lens for the field ``b``. It has a back channel that composes a new object with the changed field value.

```scala
case class Test(a: Int, b: Int)
val test = Var(Test(1, 2))
val lens = test.biMap(_.b, (x: Int) => test.get.copy(b = x))
test.attach(println)
lens := 42  // produces Test(1, 42)
```

A ``LazyVar`` evaluates its argument lazily. In the following example, it points to a mutable variable:

```scala
var counter = 0
val ch = LazyVar(counter)
ch.attach(value => { counter += 1; println(value) })  // prints 0
ch.attach(value => { counter += 1; println(value) })  // prints 1
```

### Call semantics
Functions passed to higher-order operations are evaluated on-demand:

```scala
val ch = Var(42).map(i => { println(i); i + 1 })
ch.attach(_ => ())  // prints 42
ch.attach(_ => ())  // prints 42
```

The value of a state channel gets propagated to a child when it requests the value (``flush()``). In the example, ``Var`` delays the propagation of the initial value 42 until the first ``attach()`` call. ``attach()`` goes up the channel chain and triggers the flush on each channel. In other words, ``map(f)`` merely registers an observer, but doesn't call ``f`` right away. ``f`` is called each time when any of its direct or indirect children uses ``attach()``.

This reduces the memory usage and complexity of the channel implementation as no caching needs to be performed. On the other hand, you may want to perform on-site caching of the results of ``f``, especially if the function is side-effecting.

The current value of a state channel may be read at any time using ``.get`` (if available) or ``flush()``.

There are operations that maintain state for all observers. For example, ``skip(n)`` counts the number of produced values[footnote]{``n`` must be greater than 0.}. As soon as ``n`` is exceeded, all subsequent values are passed on. The initial ``attach()`` calls ignore the first value (42), but deal with all values after that:

```scala
val ch = Var(42)
val dch = ch.drop(1)
dch.attach(println)
dch.attach(println)
ch := 23  // produces 23 twice
```

### Cycles
Certain propagation flows may lead to cycles:

```scala
val todo = Channel[String]()
todo.attach { t =>
    println(t)
    todo := ""
}
todo := "42"
```

Setting ``todo`` will result in an infinite loop. Such flows are detected and will lead to a run-time exception. Otherwise, the application would block indefinitely which makes debugging more difficult.

If a cycle as in the above example is expected, use the combinator ``filterCycles`` to make it explicit. This will ignore value propagations caused by a cycle.

## Buffers
Buffers are reactive lists. State changes such as row additions, updates or removals are encoded as delta objects. This allows to reflect these changes directly in the DOM, without having to re-render the entire list. ``Buffer[T]`` is therefore more efficient than ``Channel[Seq[T]]`` when dealing with list changes.

The following example creates a buffer with three initial rows, observes the size[footnote]{``size`` returns a ``ReadChannel[Int]``.} and then adds another row:

```scala
val buf = Buffer(1, 2, 3)
buf.size.attach(println) // Prints 3
buf += 4  // Inserts row 4, prints 4
```

All polling methods have a dollar sign as suffix ``$``:

```scala
val buf = Buffer(1, 2, 3)
println(buf.size$) // Prints 3
```

An example of using ``removeAll()``:

```scala
val buf  = Buffer(3, 4, 5)
val mod2 = buf.filter$(_ % 2 == 0)

buf.removeAll(mod2.get)
```

> **Note:** ``Buffer`` will identify rows by their value if the row type is a ``case class``. In this case, operations like ``insertAfter()`` or ``remove()`` will always refer to the first occurrence. This is often not desired. An alternative would be to define a ``class`` instead or to wrap the values in a ``Ref[_]`` object.

```scala
val todos = Buffer[Ref[Todo]]()
todos.map { case tr @ Ref(t) =>
  ...
}
```

> The value of a ``Ref[_]`` can be obtained by calling ``get``. However, it is more convenient to do pattern matching as in the example.

You can observe the delta objects produced by a buffer:

```scala
val buf = Buffer(1, 2, 3)
buf.changes.attach(println)
buf += 4
buf.clear()
```

This prints:

```
Insert(Last(),1)
Insert(Last(),2)
Insert(Last(),3)
Insert(Last(),4)
Clear()
```

All streaming operations that a buffer provides are implemented in terms of the ``changes`` channel.

## Dictionaries
Dictionaries are unordered maps from ``A`` to ``B``. MetaRx abbreviates the type as ``Dict``.

## Sets
Reactive sets are implemented as ``BufSet``[footnote]{This name was chosen as ``Set`` would have collided with Scala's implementation.}.
