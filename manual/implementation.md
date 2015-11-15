# Implementation
This section explains how reactive data structures are implemented in MetaRx. The design decisions will be beneficial for you to better understand the API and to design your own reactive data structures.

To leverage the capabilities of Scala's type system, we decided to separate the logic into distinct traits. Each data structure defines six traits which, when combined using the Cake pattern, yield a mutable reactive object without any additional code needed:

![Traits](images/traits.png)

For a hypothetical reactive data structure ``X`` you would define:

```scala
object X {
  /* Define delta change type */
}

/* Read/write access to state */
trait StateX[T] extends Disposable {
  /* This could be any kind of mutable storage */
  val state: Storage[T] = ...
  /* Channel needed by the other traits */
  val changes: Channel[X.Delta[T]] = ...
  /* Listen to `changes` and persist these in `state` */
  changes.attach { ... }
  /* Free resources */
  def dispose() { changes.dispose() }
}

/* The name may suggest otherwise, but it does not have any access
 * to the state; it only produces delta objects
 */
trait WriteX[T] {
  val changes: WriteChannel[X.Delta[T]]
  /* Also define operations to generate delta change objects */
}

trait DeltaX[T] {
  val changes: ReadChannel[X.Delta[T]]
  /* Also define streaming operations that listen to changes
   * and process these
   */
}

trait PollX[T] {
  val changes: ReadChannel[X.Delta[T]]
  /* Only read-only access is permitted here */
  val state: Storage[T]
  /* Also define streaming operations that need the state */
}

trait ReadX[T] extends DeltaX[T] with PollX[T]

case class X[T]()
  extends ReadX[T]
  with WriteX[T]
  with StateX[T]
```

A call to ``X()`` now yields a mutable reactive instance of our newly defined data structure.

It would have been possible to implement ``X`` as a single class, but the chosen approach offers more flexibility. Each of the traits are exchangeable. There are more possibilities for object instantiations. For example, often a change stream is already available. In this case, ``DeltaX[T]`` could be instantiated with a custom value for ``changes``. The caller can decide whether it needs any of the operations that ``PollX`` defines. Depending on this decision it will either buffer the data or not. This ultimately leads to a more memory-efficient design as the responsibility of memory allocation is often shifted to the caller. It is in some way similar to what Python allows with its ``yield`` expression.

The delta trait has a read-only version of the change stream. It may define operations that apply transformations directly on the stream without building any complex intermediate results. A prominent example would be the higher-order function ``map()``. As ``map()`` works on a per-element basis and does not need any access to the state, it can be implemented efficiently. As a consequence, this allows for chaining: ``list.map(f).map(g).buffer`` would compute the final list at the very end with the ``buffer`` call[footnote]{This is largely inspired by Scala's [``SeqView``](http://www.scala-lang.org/api/current/index.html#scala.collection.SeqView).}.

Another motivating reason for this design is precisely the immutability of delta objects. The stream could be forwarded directly to the client which may render the elements in the browser on-the-fly. A similar use case would be persistence, for example in an asynchronous database.

Scala's type refinements for traits come in useful. ``X`` takes ``changes``
from ``StateX``. It points to the same memory address in ``WriteX`` and ``DeltaX`` even though they are declared with different types. This is because ``Channel`` inherits both from ``WriteChannel`` and ``ReadChannel``.

The type-safety has an enormous benefit: A function can use a mutable stream internally, but returning the stream with writing capabilities would lead to unpredictable results. If the caller accidentally writes to this stream, this operation will succeed and in the worst case other subscribers receive the messages as well. As ``X`` inherits from ``ReadX``, the function can be more explicit and revoke some of its capabilities simply by returning ``ReadX[T]``. Similarly, if the caller *should* get writing capabilities and no read capabilities, this can be made explicit as well. This will make it trivial to find bugs related to reading and writing capabilities of streams directly during compile-time. And it makes interfaces more intelligible as a more specific type reduces the semantic space of a function.

The third advantage is correctness: With the functionality separated into different traits, the proper behaviour can be ensured using property-based testing. Rules for the generation of delta objects could be defined[footnote]{For example, a ``Delta.Clear`` may only be generated after ``Delta.Insert``.}. This stream is then used in ``StateX`` and all other traits can be tested whether they behave as expected. Presently, a very basic approach for property-based testing is implemented, but future versions will explore better ways to achieve a higher coverage.

A variety of generally applicable reactive operations were specified as traits in ``pl.metastack.metarx.reactive``. They can be seen as a contract and a reactive data structure should strive to implement as many as possible of these. Depending on conceptual differences, not every operation can be defined on a data structure, though. As the signatures are given, this ensures that all data structures use the operations consistently. Each of the traits group functions that are similar in their behaviour. Furthermore, the traits are combined into sub-packages which follow the properties mentioned at the beginning of the chapter, namely ``pl.metastack.metarx.reactive.{mutate, poll, stream}``.

To summarise, for a reactive data structure it is necessary to declare several traits with the following capabilities:

|           | **State** | **Mutation** | **Polling** | **Streaming** |
|-----------|-----------|--------------|-------------|---------------|
| ``Delta`` | no        | no           | no          | yes           |
| ``Poll``  | no        | no           | yes         | yes[footnote]{This is a practical decision. The ``Poll`` trait has direct access to the state. Thus, certain streaming operations can be implemented more efficiently. This should be avoided though as a delta stream would need to be persisted first in order for the ``Poll`` trait to be applicable.}  |
| ``Read``  | no        | no           | yes         | yes           |
| ``Write`` | no        | yes          | no          | no            |
| ``State`` | yes       | no           | no          | no            |

: Traits and layers of a reactive data structure
