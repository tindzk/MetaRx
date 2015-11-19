[package value="pl.metastack.metarx.manual"]
# Introduction
MetaRx provides reactive data structures for Scala and Scala.js. It is particularly useful in conjunction with user interfaces.

## Installation
Add the following dependencies to your build configuration:

```scala
libraryDependencies += "pl.metastack" %%  "metarx" % "%version%"  // Scala
libraryDependencies += "pl.metastack" %%% "metarx" % "%version%"  // Scala.js
```

## Example
The following example illustrates how you can model data flows in MetaRx.

In the first line, we define a *channel* that takes integer values. We derive from it another channel that increments every value by 1. The `===` operator in line 3 yields a channel that produces `true` if the current value is 1, otherwise `false`.

`attach()` attaches a callback that is executed for every produced value.

[scala type="section" value="introduction" file="Examples"]

## Features
* Reactive data structures:
    * Channels
    * Buffers
    * Dictionaries
    * Sets
* Scala.js support
* Thread-safe

## Comparison
MetaRx was developed with simplicity in mind. By design, it doesn't support back-pressure. It can be used in conjunction with other frameworks such as [Akka Streams](http://doc.akka.io/docs/akka-stream-and-http-experimental/current/scala.html).

