---
layout: docs
title: gRPC Streaming
section: guides
permalink: /guides/grpc-streaming
---

# gRPC Streaming

In the [tutorials](../tutorials), we only defined gRPC services with so-called
"unary" endpoints. This is an endpoint that does not involve streaming. The
client sends a single request and receives a single response.

[gRPC] also defines the following kinds of streaming, all of which are supported
by Mu.

* **Server streaming RPC**: similar to the unary service, but in this case the
  server will send back a stream of responses for a client request.
* **Client streaming RPC**: in this case is the client which sends a stream of
  requests. The server will respond with a single response.
* **Bidirectional streaming RPC**: a mix of server and client streaming as both
  sides will be sending a stream of data.

## Protobuf

Mu only officially supports streaming for Protobuf, not Avro. This is because
Avro does not (yet) have support for streaming RPC endpoints in its protocol
specification.

The relevant Avro issue is
[AVRO-406](https://issues.apache.org/jira/browse/AVRO-406).

## Stream implementation

Mu supports both [Monix
Observable](https://monix.io/docs/2x/reactive/observable.html) and [FS2
Stream](https://github.com/functional-streams-for-scala/fs2) for streaming of
RPC requests and responses. You can choose whichever data type fits your
application's needs best.

## Service definition with streaming endpoints

Let's see what a Mu RPC service definition looks like when we introduce
streaming endpoints.

Before starting, here is one import we'll need:

```scala mdoc:silent
import higherkindness.mu.rpc.protocol._
```

And here are the equest/response models for our service:

```scala mdoc:silent
case class HelloRequest(greeting: String)

case class HelloResponse(reply: String)
```

We'll write the service definition using both Monix and FS2 so we can compare.

### Using Monix Observable

```scala mdoc:silent
object MonixService {

  import monix.reactive.Observable

  @service(Protobuf)
  trait Greeter[F[_]] {

    /**
     * Server streaming RPC where the client sends a request to the server and gets a stream to read a
     * sequence of messages back. The client reads from the returned stream until there are no more messages.
     *
     * https://grpc.io/docs/guides/concepts.html
     *
     * @param request Single client request.
     * @return Stream of server responses.
     */
    def lotsOfReplies(request: HelloRequest): Observable[HelloResponse]

    /**
     * Client streaming RPC where the client writes a sequence of messages and sends them to the server,
     * again using a provided stream. Once the client has finished writing the messages, it waits for
     * the server to read them and return its response.
     *
     * https://grpc.io/docs/guides/concepts.html
     *
     * @param request Stream of client requests.
     * @return Single server response.
     */
    def lotsOfGreetings(request: Observable[HelloRequest]): F[HelloResponse]

    /**
     * Bidirectional streaming RPC where both sides send a sequence of messages using a read-write stream.
     * The two streams operate independently, so clients and servers can read and write in whatever order
     * they like: for example, the server could wait to receive all the client messages before writing its
     * responses, or it could alternately read a message then write a message, or some other combination of
     * reads and writes. The order of messages in each stream is preserved.
     *
     * https://grpc.io/docs/guides/concepts.html
     *
     * @param request Stream of client requests.
     * @return Stream of server responses.
     */
    def bidiHello(request: Observable[HelloRequest]): Observable[HelloResponse]

  }

}
```

Let's review the different endpoints one by one:

* `lotsOfReplies `: Server streaming RPC. The server receives a single `HelloRequest` from the client, and responds with an `Observable[HelloResponse]`, i.e. a stream.
* `lotsOfGreetings `: Client streaming RPC. The client sends an
  `Observable[HelloRequest]`, i.e. a stream of requests, and receives a single
  `HelloResponse`.
* `bidiHello `: Bidirectional streaming RPC. The client sends a stream of
  requests and the server sends a stream of responses.

### Using FS2

Let's write the same service definition using `fs2.Stream` instead of `Observable`.

```scala mdoc:silent
object servicefs2 {

  import fs2.Stream

  @service(Protobuf)
  trait Greeter[F[_]] {

    /**
     * Server streaming RPC
     *
     * @param request Single client request.
     * @return Stream of server responses.
     */
    def lotsOfReplies(request: HelloRequest): Stream[F, HelloResponse]

    /**
     * Client streaming RPC
     *
     * @param request Stream of client requests.
     * @return Single server response.
     */
    def lotsOfGreetings(request: Stream[F, HelloRequest]): F[HelloResponse]

    /**
     * Bidirectional streaming RPC
     *
     * @param request Stream of client requests.
     * @return Stream of server responses.
     */
    def bidiHello(request: Stream[F, HelloRequest]): Stream[F, HelloResponse]

  }

}
```

[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
