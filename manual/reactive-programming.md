[package value="pl.metastack.metarx.manual"]
# Reactive programming
Reactive programming is a paradigm that is concerned with:

* propagating data, specifically changes, and
* specifying data flows.

## Data propagation
Concretely, a data structure is said to be *reactive* (or *streaming*) if it models its state as a stream. It does this by defining change objects (also called *deltas*) and mapping its operations onto these. The published stream is read-only and can be subscribed. If the stream does not have any subscribers, the state does not get persisted and is lost.

**Example:** A reactive list implementation could map all of its operations like `append()`, `insertAfter()`, `delete()` and `clear()` on only two delta types, namely `Insert` and `Delete`. A subscriber can interpret the delta objects and persist the computed list in an in-memory buffer.

A reactive data structure can also stream state observations. Sticking to the reactive list example, we could stream observations based on the list's inherent properties — one being its current length, another the existence of a certain element, i.e. ``contains(value)``.

Finally, a *mutable* reactive data structure is an extension with the sole difference that it maintains an internal state which always represents the computed result after a delta was received. This is a hybrid solution bridging mutable object-oriented objects with reactive data structures.

The mutable variant of our reactive list could send its current state when a subscriber is registering. Subscribers can register at any point without caring whether the expected data has been propagated yet already. The second motivation for a mutable reactive data structure is that they reduce the overall memory usage; otherwise we would need multiple instances of mutable objects that interpret the deltas.

## Data flows
For now we have just covered the first component of reactive programming: data propagation. The second cornerstone, data flow, is equally important though. Streams describe data flow in terms of dependencies. Considering you want to plot a line as a graph using the formula `y = mx + b` while the user provides the values for `m` and `b`, then you would wrap these inputs in channels and express the dependencies using combinators[footnote]{The types in the code are not needed for type inference and only serve illustration purposes.}:

[scala type="section" value="reactive-programming" file="Examples"]

In `values` we listen to `y` and whenever it receives a new function, it calls it for all the `x` in the interval of the shown graph. The example shows that messages in streams are not bound to data objects and even immutable functions could be passed around.

The data propagation from the example is illustrated by the following diagram:

![Change propagation for $y=mx+b$](images/data-flow.png)

As soon as the user inserts a new value for `m` or `b`, ``mAndB`` will produce a tuple. Then, `y` computes the final function.

How channels work in detail is explained in the following sections. This example should only give an intuition of the fundamental concept as well as how data dependencies are expressed.

## Streams
The term "stream" was used several times in this chapter. This term is polysemous and requires further explanation. In reactive programming there are different types of streams with significant semantic differences.

### Observables
[Rx](https://rx.codeplex.com/) (Reactive Extensions) is a contract designed by Microsoft which calls streams *observables* and defines rules how to properly interact with them. An observable can be subscribed to with an *observer* which has one function for the next element and two auxiliary ones for handling errors and the completion of the stream.

Furthermore, observables are subdivided into *cold* and *hot* observables[footnote]{Source: url[http://leecampbell.blogspot.de/2010/08/rx-part-7-hot-and-cold-observables.html]{leecampbell.blogspot.de} (4th February 2015)}:

- **Cold observable:** Streams that are passive and start publishing on request
- **Hot observable:** Streams that are active and publish regardless of subscriptions

### Back-pressure
There are extensions to Rx which introduce back-pressure[footnote]{For instance, url[https://github.com/monifu/monifu]{Monifu} implements this feature.} to deal with streams that are producing values too fast. This may not be confused with back-propagation which describes those streams where the subscribers could propagate values back to the producer.

### Implementation
This illustrates the diversity of streams. Due to the nature of MetaRx, streams had to be implemented differently from the outlined ones. Some of the requirements were:

- lightweight design
- support for n-way binding
- usable as the basis for reactive data structures
- provide functionality for resource management
- require little boilerplate to define new operations

To better differentiate from the established reactive frameworks, a less biased term than *observable* was called for and the reactive streams are therefore called *channels* in MetaRx. The requirements have been implemented as follows: A subscriber is just a function pointer (wrapped around a small object). A channel can have an unlimited number of children whereas each of the child channels knows their parent. A function for flushing the content of a channel upon subscription can be freely defined during instantiation[footnote]{This function is called by code{attach()} and produces multiple values which is necessary for some reactive data structures like lists.}. When a channel is destroyed, so are its children. Error handling is not part of the implementation. Similarly, no back-pressure is performed, but back-propagation is implemented for some selected operations like ``biMap()``.

For client-side web development only a small percentage of the interaction with streams require the features observables provide and this does not justify a more complex overall design. It is possible to use a full-fledged model like Rx, Monifu or Akka Streams for just those areas of the application where necessary by redirecting (piping) the channel output.

## Summary
To recap, a reactive data structure has four layers:

- **State:** interpretation of the delta stream and "converting" it into a mutable object
- **Mutation operations:** functions to produce deltas on the stream[footnote]{These functions do not access the state in any way.}
- **Polling operations:** blocking functions to query the state
- **Streaming operations:** publish the state changes as a stream

Obviously, the first three layers are the very foundation of object-orientation. It is different in that a) modifications are encoded as deltas and b) there are streaming operations.
