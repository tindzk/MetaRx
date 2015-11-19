[package value="pl.metastack.metarx.manual"]
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
- ``Var``: variable stream; its value is always defined and has an initial value[footnote]{In Rx terms, code{Var} would correspond to a i{cold observer} as attaching to it will flush its current value. This is different from code{Channel} which loses its messages when there are no subscribers.}
- ``LazyVar``: stream for lazily evaluated variables
- ``PtrVar``: stream for generic events[footnote]{It can be used to create delta channels from DOM variables by binding to the corresponding events that triggered by the value changes. For an example see code{Node.click}.}

Partial channels model optional values:

- ``PartialChannel``: base type
- ``Opt``: stream that has two states, either *defined with a value* or *undefined*

> **Note:** ``Opt[T]`` is merely a convenience type and ``Var[Option[T]]`` could be used, too.

### Operations
Here is a simple example for a channel that receives integers. We register an observer which prints all values on the console:

[scala type="section" value="produce" file="Examples"]

> **Note:** The ``:=`` operator is a shortcut for the method ``produce``.

The return values of operations are channels, therefore chaining is possible. Channels can be used to express data dependencies:

[scala type="section" value="chaining" file="Examples"]

Use the method `distinct` to produce a value if it is the first or different from the previous one. A use case is to perform time-consuming operations such as performing HTTP requests only once for the same user input:

```scala
ch.distinct.attach { query =>
  // perform HTTP request
}
```

Considering that you want to observe multiple channels of the same type and merge the produced values, you can use `merge()`:

[scala type="section" value="merging" file="Examples"]

A related function is `or()`, which detects changes in any of the passed channels. The operator `|` was defined for it:

[scala type="section" value="or" file="Examples"]

For Boolean channels, the logical operators are defined, and yield a new channel with the result:

[scala type="section" value="logical-operators" file="Examples"]

Furthermore, `onTrue()` and `onFalse()` are defined and will give a `ReadChannel[Unit]` that triggers when either `true` or `false` was produced.

Aritmetic operators like `+`, `-`, `*`, `/`, `%`, `<`, `<=`, `>`, `>=`, `===` and `!==` are also supported by those channels whose underlying types implement the Scala's `Numeric` or `Ordering` trait:

[scala type="section" value="arithmetic-operators" file="Examples"]

It must be noted that streaming operations have different semantics than their non-reactive counterparts. For brevity, only certain combinators are covered by the manual. For the rest, please refer to the ScalaDoc documentation.

### State channels
For better performance, `Channel` does not cache the produced values. Some operations cannot be implemented without access to the current value though. Also, often it is necessary to poll the current value. For these reasons *state channels* such as `Var` or `Opt` were introduced. The following example visualises the different behaviours:

[scala type="section" value="state-channel" file="Examples"]

`update()` is an operation that requires that the produced values are persisted. `update()` takes a function which modifies the current value:

[scala type="section" value="state-channel-update" file="Examples"]

A partially-defined channel (`Opt`) is constructed as follows:

[scala type="section" value="opt" file="Examples"]

Alternatively, a default value may be passed:

[scala type="section" value="opt-default" file="Examples"]

A state channel provides all the methods a channel does. `Var[T]` and `Opt[T]` can be obtained from any existing `ReadChannel[T]` using the method `state`:

[scala type="section" value="state" file="Examples"]

``chOpt`` is undefined as long as no value was produced on ``ch``. ``chVar`` will be initialised with 42 and the value is overridden with the first produced value on ``ch``.

If writing capabilities are not required, `cache` is to be preferred:

[scala type="section" value="cache" file="Examples"]

`biMap()` allows to implement a bi-directional map, i.e. a stream with back-propagation:

[scala type="section" value="bimap" file="Examples"]

``biMap()`` can be used to implement a lens as a channel. The following example defines a lens for the field ``b``. It has a back channel that composes a new object with the changed field value.

[scala type="section" value="bimap-lens" file="Examples"]

A `LazyVar` evaluates its argument lazily. In the following example, it points to a mutable variable:

[scala type="section" value="lazyvar" file="Examples"]

### Call semantics
Functions passed to higher-order operations are evaluated on-demand:

[scala type="section" value="call-semantics" file="Examples"]

The value of a state channel gets propagated to a child when it requests the value (``flush()``). In the example, ``Var`` delays the propagation of the initial value 42 until the first ``attach()`` call. ``attach()`` goes up the channel chain and triggers the flush on each channel. In other words, ``map(f)`` merely registers an observer, but doesn't call ``f`` right away. ``f`` is called each time when any of its direct or indirect children uses ``attach()``.

This reduces the memory usage and complexity of the channel implementation as no caching needs to be performed. On the other hand, you may want to perform on-site caching of the results of ``f``, especially if the function is side-effecting.

The current value of a state channel may be read at any time using ``.get`` (if available) or ``flush()``.

There are operations that maintain state for all observers. For example, ``skip(n)`` counts the number of produced values[footnote]{code{n} must be greater than 0.}. As soon as ``n`` is exceeded, all subsequent values are passed on. The initial ``attach()`` calls ignore the first value (42), but deal with all values after that:

[scala type="section" value="call-semantics-drop" file="Examples"]

### Cycles
Certain propagation flows may lead to cycles:

[scala type="section" value="cycle" file="Examples"]

Setting ``todo`` will result in an infinite loop. Such flows are detected and will lead to a run-time exception. Otherwise, the application would block indefinitely which makes debugging more difficult.

If a cycle as in the above example is expected, use the combinator ``filterCycles`` to make it explicit. This will ignore value propagations caused by a cycle.

## Buffers
Buffers are reactive lists. State changes such as row additions, updates or removals are encoded as delta objects. This allows to reflect these changes directly in the DOM, without having to re-render the entire list. ``Buffer[T]`` is therefore more efficient than ``Channel[Seq[T]]`` when dealing with list changes.

The following example creates a buffer with three initial rows, observes the size[footnote]{code{size} returns a code{ReadChannel[Int]}.} and then adds another row:

[scala type="section" value="buffer" file="Examples"]

All polling methods have a dollar sign as suffix ``$``:

[scala type="section" value="buffer-distinct" file="Examples"]

An example of using ``removeAll()``:

[scala type="section" value="buffer-remove-all" file="Examples"]

> **Note:** ``Buffer`` will identify rows by their value if the row type is a ``case class``. In this case, operations like ``insertAfter()`` or ``remove()`` will always refer to the first occurrence. This is often not desired. An alternative would be to define a ``class`` instead or to wrap the values in a ``Ref[_]`` object.

[scala type="section" value="buffer-ref" file="Examples"]

> The value of a ``Ref[_]`` can be obtained by calling ``get``. However, it is more convenient to do pattern matching as in the example.

You can observe the delta objects produced by a buffer:

[scala type="section" value="buffer-changes" file="Examples"]

All streaming operations that a buffer provides are implemented in terms of the ``changes`` channel.

## Dictionaries
Dictionaries are unordered maps from ``A`` to ``B``. MetaRx abbreviates the type as ``Dict``.

## Sets
Reactive sets are implemented as ``BufSet``[footnote]{This name was chosen as code{Set} would have collided with Scala's implementation.}.
