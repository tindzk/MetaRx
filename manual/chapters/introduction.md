# Introduction
## Installation
Add the following dependencies to your build configuration:

```scala
libraryDependencies += "pl.metastack" %%  "metarx" % "%version%"  // Scala
libraryDependencies += "pl.metastack" %%% "metarx" % "%version%"  // Scala.js
```

## Concepts
Reactive programming is a paradigm that focuses on:

* propagation of data, specifically changes, and
* data flow.

Concretely, a data structure is said to be *reactive* (or *streaming*) if it models its state as streams. It does this by defining change objects (*deltas*) and mapping its operations onto these. The published stream is read-only and can be subscribed. If the stream does not have any subscribers, the state would not get persisted and is lost.

> **Example:** A reactive list implementation could map all its operations like ``clear()`` or ``insertAfter()`` on the two delta types ``Insert`` and ``Delete``. A subscriber can interpret the deltas and persist the computed list in an in-memory buffer.

Another property of a reactive data structure is that it does not only stream deltas, but also state observations. Sticking to the reactive list example, the deltas could allow streaming observations on the list's inherent properties — one being the length, another the existence of a certain element, i.e. ``contains(value)``.

Finally, a *mutable* reactive data structure is an extension with the sole difference that it maintains an internal state which always represents the computed result after a delta was received. This is a hybrid solution bridging mutable object-oriented objects with reactive data structures. The mutable variant of our reactive list could send its current state when a subscriber is registering. This ultimately leads to better legibility of code as subscribers can register at any point without caring whether the expected data has been propagated already. The second reason is that otherwise we would need multiple instances of mutual objects that interpret the deltas. This is often undesired as having multiple such instances incurs a memory overhead.

To recap, a reactive data structure has four layers:

- **State:** interpretation of the delta stream and "converting" it into a mutable object
- **Mutation operations:** functions to produce deltas on the stream[footnote]{These functions do not access the state in any way.}
- **Polling operations:** blocking functions to query the state
- **Streaming operations:** publish the state changes as a stream

Obviously, the first three layers are the very foundation of object-orientation. It is different in that a) modifications are encoded as deltas and b) there are streaming operations.

For now we just covered the first component of reactive programming: data propagation. The second cornerstone, data flow, is equally important, though. Streams describe data flow in terms of dependencies. Considering you want to plot a line as a graph using the formula $y = mx+b$ and the user provides the values for $m$ and $b$, then you would wrap these inputs in channels and express the dependencies using combinators[footnote]{The types in the code only serve illustration purposes}:

```scala
val m = Opt[Int]()
val b = Opt[Int]()

// Produces when user provided `m` and `b`
val mAndB: ReadChannel[(Int, Int)] = m.zip(b)

// Function channel to calculate `y` for current input
val y: ReadChannel[Int => Int] =
  mAndB.map { case (m, b) =>
    (x: Int) => m * x + b
  }
```

The user could listen to ``y`` and whenever it receives a new function, it can just call it for all the ``x`` in the interval of the shown graph. The example shows that messages in streams are not bound to data objects and even immutable functions could be passed around.

The data propagation is illustrated by the following diagram:

![Change propagation for $y=mx+b$](images/data-flow.png)

As soon as the user inserts a value for ``m`` as well as ``b``, ``mAndB`` will produce a tuple. Then, ``y`` computes the final function.

How channels work in detail is explained in the following sections. This example should only give an intuition of the fundamental concepts and how data dependencies are expressed.

## Requirements
The term "stream" was used several times. This term is polysemous and requires further explanation. In reactive programming there are different types of streams with severe semantic differences.

> [Rx](https://rx.codeplex.com/) (Reactive Extensions) is a contract designed by Microsoft which calls these streams *observables* and defines rules how to properly interact with these. An observable can be subscribed to with an *observer* which has one function for the next element and two auxiliary ones for handling errors and the completion of the stream.
> Furthermore, observables are subdivided into *cold* and *hot* observables[footnote]{Source: [leecampbell.blogspot.de](http://leecampbell.blogspot.de/2010/08/rx-part-7-hot-and-cold-observables.html) (4th February 2015)}:
>
> - **Cold observable:** Streams that are passive and start publishing on request
> - **Hot observable:** Streams that are active and publish regardless of subscriptions
>
> There are extensions to Rx which introduce back-pressure[footnote]{For instance, [Monifu](https://github.com/monifu/monifu) implements this feature.} to deal with streams that are producing values too fast. This may not be confused with back-propagation which describes those streams where the subscribers could propagate values back to the producer.

This illustrates the diversity of streams. Due to the nature of MetaRx, streams had to be implemented differently from the outlined ones. Some of the requirements were:

- lightweight design
- support for n-way binding
- usable as the basis for reactive data structures
- provide functionality for resource management
- require little boilerplate to define new operations

To better differentiate from the established reactive frameworks, a less biased term than *observable* was called for and the reactive streams are therefore called *channels* in MetaRx. The requirements have been implemented as follows: A subscriber is just a function pointer (wrapped around a small object). A channel can have an unlimited number of children whereas each of the child channels knows their parent. A function for flushing the content of a channel upon subscription can be freely defined during instantiation[footnote]{This function is called by ``attach()`` and produces multiple values which is necessary for some reactive data structures like lists.}. When a channel is destroyed, so are its children. Error handling is not part of the implementation. Similarly, no back-pressure is performed, but back-propagation is implemented for some selected operations like ``biMap()``.

For client-side web development only a small percentage of the interaction with streams require the features observables provide and this does not justify a more complex overall design. It is possible to use a full-fledged model like Rx or Monifu for just those areas of the application where necessary by redirecting (piping) the channel output.
