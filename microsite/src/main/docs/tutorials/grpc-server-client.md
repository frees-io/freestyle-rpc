---
layout: docs
title: gRPC server and client
section: tutorials
permalink: tutorials/grpc-server-client
---

# Tutorial: gRPC server and client

This tutorial will show you how to implement a working gRPC server and client
based on a Mu service defintion.

This tutorial is aimed at developers who:

* are new to Mu-Scala
* have some understanding of [cats-effect]
* have read the [Getting Started guide](../getting-started)
* have followed either the [RPC service definition with Protobuf](service-definition/protobuf) or 
  [RPC service definition with Avro](service-definition/avro) tutorial

Mu supports both Protobuf and Avro. For the purposes of this tutorial we will
assume you are using Protobuf, but it's possible to follow the tutorial even if
you are using Avro.

## Service definition

If you have followed one of the previous tutorials, you should already have a
service definition that looks like this:

```scala mdoc:silent
import higherkindness.mu.rpc.protocol._

object hello {
  case class HelloRequest(@pbdirect.pbIndex(1) name: String)
  case class HelloResponse(@pbdirect.pbIndex(1) greeting: String, @pbdirect.pbIndex(2) happy: Boolean)

  // Note: the @service annotation in your code might reference Avro instead of Protobuf
  @service(Protobuf, compressionType = Identity, namespace = Some("com.example"))
  trait Greeter[F[_]] {
    def SayHello(req: HelloRequest): F[HelloResponse]
  }

}
```

## Implement the server

This is the interesting part: writing the business logic for your service.

We do this by implementing the `Greeter` trait. Let's make a `Greeter` that says
"hello" in a happy voice:

```scala mdoc:silent
import cats.Applicative
import cats.syntax.applicative._
import hello._

class HappyGreeter[F[_]: Applicative] extends Greeter[F] {

  def SayHello(req: HelloRequest): F[HelloResponse] =
    HelloResponse(s"Hello, ${req.name}!", happy = true).pure[F]

}
```

Note that in this implementation we aren't performing any effects, so we don't
care what `F[_]` is as long as we can lift a pure value into it.

## Server entrypoint

Now we have a `Greeter` implementation, let's expose it as a gRPC server.

We're going to use cats-effect `IO` as our concrete IO monad, and we'll make use
`IOApp` from cats-effect.

```scala mdoc:silent
import cats.effect.{IO, IOApp, ExitCode}
import hello.Greeter
import higherkindness.mu.rpc.server.{GrpcServer, AddService}

object Server extends IOApp {

  implicit val greeter: Greeter[IO] = new HappyGreeter[IO]  // 1

  def run(args: List[String]): IO[ExitCode] = for {
    serviceDef <- Greeter.bindService[IO]                                      // 2
    server     <- GrpcServer.default[IO](12345, List(AddService(serviceDef)))  // 3
    _          <- GrpcServer.server[IO](server)                                // 4
  } yield ExitCode.Success

}
```

Let's go through this line by line.

1. First we instantiate our `HappyGreeter`, concretized to `IO`, and make it
   available implicitly for use by `Greeter.bindService`.

2. Next we call `Greeter.bindService`. This is a helper method generated by the
   `@service` macro annotation on the `Greeter` trait. It converts our `Greeter`
   service into a gRPC "service definition", returning
   `IO[io.grpc.ServerServiceDefinition]`. 
   Each Scala method in the service will become a gRPC method of the same name,
   with the following adjustments:
  - If the `@service` annotation has a `methodNameStyle = Capitalize` argument,
    the first letter of the method name will be capitalized;
  - If the `@service` annotation has a `namespace = Some(ns)` argument, 
    the method name will be prefixed by the value of `ns`, followed by a dot.

3. We build a description of the whole gRPC server by calling
   `GrpcServer.default`. We tell it the port we want to run on (12345), and the
   list of services we want it to expose. The method is called `default` because
   we want to use gRPC's default HTTP transport layer.

4. Finally we can call `GrpcServer.server`, passing it our server description.
   This actually starts the server.

If you copy the above code into a `.scala` file in the `server` module of your
project, you should be able to start a server using `sbt server/run`.

## Client

Let's see how to make a client to communicate with the server.

Here is a tiny demo that makes a request to the `SayHello` endpoint and
prints out the reply to the console.

```scala mdoc:silent
import cats.effect.{IO, IOApp, Resource, ExitCode}
import hello.{Greeter, HelloRequest}
import higherkindness.mu.rpc._

object ClientDemo extends IOApp {

  val channelFor: ChannelFor = ChannelForAddress("localhost", 12345)  // 1

  val clientResource: Resource[IO, Greeter[IO]] = Greeter.client[IO](channelFor)  // 2

  def run(args: List[String]): IO[ExitCode] =
    for {
      response   <- clientResource.use(c => c.SayHello(HelloRequest(name = "Chris")))  // 3
      serverMood = if (response.happy) "happy" else "unhappy"
      _          <- IO(println(s"The $serverMood server says '${response.greeting}'"))
    } yield ExitCode.Success

}
```

Again we'll go through this line by line.

1. We create a channel, which tells the client how to connect to the server.

2. We call `Greeter.client`, another helper method generated by the `@service`
   macro. This returns a cats-effect
   [Resource](https://typelevel.org/cats-effect/datatypes/resource.html), which
   will take care of safely allocated and cleaning up resources every time we
   want to use a client.

3. We `use` the resource, using the resulting client to send a request to the
   `SayHello` endpoint and get back a response.

If you copy the above code into a `.scala` file in the `client` module of your
project, and your server is still running, you should be able to see the client
in action using `sbt client/run`.

```
[info] running com.example.ClientDemo
The happy server says 'Hello, Chris!'
[success] Total time: 1 s, completed 5 Mar 2020, 15:49:03
```

## Next steps

If you want to write tests for your RPC service, take a look at the [Testing an RPC service tutorial](testing-rpc-service).

[cats-effect]: https://typelevel.org/cats-effect/
[gRPC]: https://grpc.io/
[Protocol Buffers]: https://developers.google.com/protocol-buffers