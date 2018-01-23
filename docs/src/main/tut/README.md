---
layout: docs
title: RPC
permalink: /docs/rpc
---

# Freestyle-RPC

[RPC] atop **Freestyle** is **`frees-rpc`**.

Freestyle RPC is a purely functional library for building RPC endpoint-based services with support for [RPC] and [HTTP/2].

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

  - [What’s frees-rpc](#whats-frees-rpc)
  - [Installation](#installation)
  - [About gRPC](#about-grpc)
  - [Messages and Services](#messages-and-services)
    - [gRPC](#grpc)
    - [frees-rpc](#frees-rpc)
  - [Service Methods](#service-methods)
  - [Generating a .proto file](#generating-a-proto-file)
    - [Plugin Installation](#plugin-installation)
    - [Plugin Settings](#plugin-settings)
    - [Generation with protoGen](#generation-with-protogen)
  - [RPC Service Implementations](#rpc-service-implementations)
    - [Server](#server)
    - [Server Runtime](#server-runtime)
      - [Execution Context](#execution-context)
      - [Runtime Implicits](#runtime-implicits)
    - [Server Bootstrap](#server-bootstrap)
    - [Client](#client)
    - [Client Runtime](#client-runtime)
      - [Execution Context](#execution-context-1)
      - [Runtime Implicits](#runtime-implicits-1)
    - [Client Program](#client-program)
  - [frees-rpc annotations](#frees-rpc-annotations)
  - [Metrics Reporting](#metrics-reporting)
    - [Monitor Server Calls](#monitor-server-calls)
    - [Monitor Client Calls](#monitor-client-calls)
    - [Dropwizard Metrics](#dropwizard-metrics)
  - [Next Steps](#next-steps)
  - [Comparing HTTP and RPC](#comparing-http-and-rpc)
- [References](#references)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## What’s frees-rpc

[frees-rpc] provides the ability to combine [RPC] protocols, services, and clients in your `Freestyle` program, thanks to [gRPC]. Although it's fully integrated with [gRPC], there are some important differences when defining the protocols, as we’ll see later on, since [frees-rpc] follows the same philosophy as `Freestyle` core, being macro-powered.

## Installation

`frees-rpc` is cross-built for Scala `2.11.x` and `2.12.x`.

It's divided into multiple and different artifacts, grouped by scope:

* `Server`: specifically for the RPC server.
* `Client`: focused on the RPC auto-derived clients by `frees-rpc`.
* `Server/Client`: used from other artifacts for both Server and Client.
* `Test`: useful to test `frees-rpc` applications.

*Artifact Name* | *Scope* | *Mandatory* | *Description*
--- | --- | --- | ---
`frees-rpc-server` | Server | Yes | Needed to attach RPC Services and spin-up an RPC Server.
`frees-rpc-client-core` | Client | Yes | Mandatory to define protocols and auto-derived clients.
`frees-rpc-client-netty` | Client | Yes* | Optional if you use `OkHttp`, required from the client perspective.
`frees-rpc-client-okhttp` | Client | Yes* | Optional if you use `Netty`, required from the client perspective.
`frees-rpc-config` | Server/Client | No | It provides configuration helpers using [frees-config] to load the application configuration values.
`frees-rpc-prometheus-server` | Server | No | Scala interceptors which can be used to monitor gRPC services using Prometheus, on the _Server_ side.
`frees-rpc-prometheus-client` | Client | No | Scala interceptors which can be used to monitor gRPC services using Prometheus, on the _Client_ side.
`frees-rpc-prometheus-shared` | Server/Client | No | Common code for both the client and the server in the prometheus scope.
`frees-rpc-dropwizard-server` | Server | No | Scala interceptors which can be used to monitor gRPC services using Dropwizard metrics, on the _Server_ side.
`frees-rpc-dropwizard-client` | Client | No | Scala interceptors which can be used to monitor gRPC services using Dropwizard metrics, on the _Client_ side.
`frees-rpc-interceptors` | Server/Client | No | Commons related to gRPC interceptors.
`frees-rpc-testing` | Test | No | Utilities to test out `frees-rpc` applications. It provides the `grpc-testing` library as the transitive dependency.
`frees-rpc-common` | Server/Client | Provided* | Common things that are used throughout the project.
`frees-rpc-internal` | Server/Client | Provided* | Macros.
`frees-rpc-async` | Server/Client | Provided* | Async instances useful for interacting with the RPC services on both sides, server and the client.

* `Yes*`: on the client-side, you must choose either `Netty` or `OkHttp` as the transport layer.
* `Provided*`: you don't need to add it to your build, it'll be transitively provided when using other dependencies.

You can install any of these dependencies in your build as follows:

[comment]: # (Start Replace)

```scala
// required for the RPC Server:
libraryDependencies += "io.frees" %% "frees-rpc-server"            % "0.10.0"

// required for a protocol definition:
libraryDependencies += "io.frees" %% "frees-rpc-client-core"       % "0.10.0"

// required for the use of the derived RPC Client/s, using either Netty or OkHttp as transport layer:
libraryDependencies += "io.frees" %% "frees-rpc-client-netty"      % "0.10.0"
// or:
libraryDependencies += "io.frees" %% "frees-rpc-client-okhttp"     % "0.10.0"

// optional - for both server and client configuration.
libraryDependencies += "io.frees" %% "frees-rpc-config"            % "0.10.0"

// optional - for both server and client metrics reporting, using Prometheus.
libraryDependencies += "io.frees" %% "frees-rpc-prometheus-server" % "0.10.0"
libraryDependencies += "io.frees" %% "frees-rpc-prometheus-client" % "0.10.0"

// optional - for both server and client metrics reporting, using Dropwizard.
libraryDependencies += "io.frees" %% "frees-rpc-dropwizard-server" % "0.10.0"
libraryDependencies += "io.frees" %% "frees-rpc-dropwizard-client" % "0.10.0"
```

[comment]: # (End Replace)

## About gRPC

> [gRPC](https://grpc.io/about/) is a modern, open source, and high-performance RPC framework that can run in any environment. It can efficiently connect services in and across data centers with pluggable support for load balancing, tracing, health checking, and authentication. It's also applicable in the last mile of distributed computing to connect devices, mobile applications, and browsers to backend services.

In this project, we are focusing on the [Java gRPC] implementation.

In the upcoming sections, we'll take a look at how we can implement RPC protocols (Messages and Services) in both *gRPC* and *frees-rpc*.

## Messages and Services

### gRPC

As you might know, [gRPC] uses protocol buffers (a.k.a. *protobuf*) by default:

* As the Interface Definition Language (IDL) - for describing both the service interface and the structure of the payload messages. It is possible to use other alternatives if desired.
* For serializing/deserializing structure data - similarly, as you do with [JSON] data, defining files `.json` extension, with protobuf you have to define proto files with `.proto` as an extension.

In the example given in the [gRPC guide], you might have a proto file like this:

```
message Person {
  string name = 1;
  int32 id = 2;
  bool has_ponycopter = 3;
}
```

Then, once you’ve specified your data structures, you can use the protobuf compiler `protoc` to generate data access classes in your preferred language(s) from your proto definition.

Likewise, you can define [gRPC] services in your proto files, with RPC method parameters and return types specified as protocol buffer messages:

```
// The greeter service definition.
service Greeter {
  // Sends a greeting
  rpc SayHello (HelloRequest) returns (HelloReply) {}
}

// The request message containing the user's name.
message HelloRequest {
  string name = 1;
}

// The response message containing the greetings
message HelloReply {
  string message = 1;
}
```

Correspondingly, [gRPC] also uses protoc with a special [gRPC] plugin to generate code from your proto file for this `Greeter` RPC service.

You can find more information about Protocol Buffers in the [Protocol Buffers' documentation](https://developers.google.com/protocol-buffers/docs/overview).

### frees-rpc

In the previous section, we’ve seen an overview of what [gRPC] offers for defining protocols and generating code (compiling protocol buffers). Now, we'll show how [frees-rpc] offers the same, but in the **Freestyle** fashion, following the FP principles.

First things first, the main difference in respect to [gRPC] is that [frees-rpc] doesn’t need `.proto` files, but it still uses protobuf, thanks to the [PBDirect] library, which allows to read and write Scala objects directly to protobuf with no `.proto` file definitions. Therefore, in summary, we have:

* Your protocols, both messages, and services, will reside with your business-logic in your Scala files using [scalameta] annotations to set them up. We’ll see more details on this shortly.
* Instead of reading `.proto` files to set up the [RPC] messages and services, [frees-rpc] offers (as an optional feature) to generate them, based on your protocols defined in your Scala code. This feature is offered to maintain compatibility with other languages and systems outside of Scala. We'll check out this feature further in [this section](#generating-a-proto-file).

Let’s start looking at how to define the `Person` message that we saw previously.
Before starting, these are the Scala imports we need:

```tut:silent
import freestyle.free._
import freestyle.rpc.protocol._
```

`Person` definition would be defined as follows:

```tut:silent
/**
  * Message Example.
  *
  * @param name Person name.
  * @param id Person Id.
  * @param has_ponycopter Has Ponycopter check.
  */
@message
case class Person(name: String, id: Int, has_ponycopter: Boolean)
```

As we can see, it’s quite simple since it’s just a Scala case class preceded by the `@message` annotation (`@message` is optional though and used exclusively by [sbt-freestyle-protogen](https://github.com/frees-io/sbt-freestyle-protogen)):

By the same token, let’s see now how the `Greeter` service would be translated to the [frees-rpc] style (in your `.scala` file):

```tut:silent
@option(name = "java_package", value = "quickstart", quote = true)
@option(name = "java_multiple_files", value = "true", quote = false)
@option(name = "java_outer_classname", value = "Quickstart", quote = true)
object protocols {

  /**
   * The request message containing the user's name.
   * @param name User's name.
   */
  @message
  case class HelloRequest(name: String)

  /**
   * The response message,
   * @param message Message containing the greetings.
   */
  @message
  case class HelloReply(message: String)

  @service
  trait Greeter[F[_]] {

    /**
     * The greeter service definition.
     *
     * @param request Say Hello Request.
     * @return HelloReply.
     */
    @rpc(Protobuf) def sayHello(request: HelloRequest): F[HelloReply]

  }
}
```

Naturally, the [RPC] services are grouped in a [@tagless algebra]. Therefore, we are following one of the primary principles of Freestyle; you only need to concentrate on the API that you want to expose as abstract smart constructors, without worrying how they will be implemented.

In the above example, we can see that `sayHello` returns a `FS[HelloReply]`. However, very often the services might:

* Return an empty response.
* Receive an empty request.
* A combination of both.

`frees-rpc` provides an `Empty` object, defined at `freestyle.rpc.protocol`, that you might want to use for these purposes.

For instance:

```tut:silent
@option(name = "java_package", value = "quickstart", quote = true)
@option(name = "java_multiple_files", value = "true", quote = false)
@option(name = "java_outer_classname", value = "Quickstart", quote = true)
object protocol {

  /**
   * The request message containing the user's name.
   * @param name User's name.
   */
  @message
  case class HelloRequest(name: String)

  /**
   * The response message,
   * @param message Message containing the greetings.
   */
  @message
  case class HelloReply(message: String)

  @service
  trait Greeter[F[_]] {

    /**
     * The greeter service definition.
     *
     * @param request Say Hello Request.
     * @return HelloReply.
     */
    @rpc(Protobuf) def sayHello(request: HelloRequest): F[HelloReply]

    @rpc(Protobuf) def emptyResponse(request: HelloRequest): F[Empty.type]

    @rpc(Protobuf) def emptyRequest(request: Empty.type): F[HelloReply]

    @rpc(Protobuf) def emptyRequestRespose(request: Empty.type): F[Empty.type]
  }
}
```

We are also using some additional annotations:

* `@option`: used to define the equivalent headers in `.proto` files.
* `@service`: it tags the `@free` algebra as an [RPC] service, in order to derive server and client code (macro expansion). **Important**: `@free` annotation should go first, followed by `@service` annotation, and not inversely.
* `@rpc(Protobuf)`: this annotation indicates that the method is an RPC service. It receives as argument the type of serialization that will be used to encode/decode data, `Protocol Buffers` in the example. `Avro` is also supported as the another type of serialization.

We'll see more details about these and other annotations in the following sections.

## Service Methods

As [gRPC], [frees-rpc] allows you to define four kinds of service methods:

* **Unary RPC**: the simplest way of communication, one client request, and one server response.
* **Server streaming RPC**: similar to the unary, but in this case, the server will send back a stream of responses for a client request.
* **Client streaming RPC**: in this case is the client who sends a stream of requests. The server will respond with a single response.
* **Bidirectional streaming RPC**: it would be a mix of server and client streaming since both sides will be sending a stream of data.

Let's complete our protocol's example with these four kinds of service methods:

```tut:silent
@option(name = "java_package", value = "quickstart", quote = true)
@option(name = "java_multiple_files", value = "true", quote = false)
@option(name = "java_outer_classname", value = "Quickstart", quote = true)
object service {

  import monix.reactive.Observable

  @message
  case class HelloRequest(greeting: String)

  @message
  case class HelloResponse(reply: String)

  @service
  trait Greeter[F[_]] {

    /**
     * Unary RPCs where the client sends a single request to the server and gets a single response back,
     * just like a normal function call.
     *
     * https://grpc.io/docs/guides/concepts.html
     *
     * @param request Client Request.
     * @return Server Response.
     */
    @rpc(Protobuf)
    def sayHello(request: HelloRequest): F[HelloResponse]

    /**
     * Server streaming RPCs where the client sends a request to the server and gets a stream to read a
     * sequence of messages back. The client reads from the returned stream until there are no more messages.
     *
     * https://grpc.io/docs/guides/concepts.html
     *
     * @param request Client Request.
     * @return Stream of server responses.
     */
    @rpc(Protobuf)
    @stream[ResponseStreaming.type]
    def lotsOfReplies(request: HelloRequest): F[Observable[HelloResponse]]

    /**
     * Client streaming RPCs where the client writes a sequence of messages and sends them to the server,
     * again using a provided stream. Once the client has finished writing the messages, it waits for
     * the server to read them and return its response.
     *
     * https://grpc.io/docs/guides/concepts.html
     *
     * @param request Stream of requests.
     * @return Single Server Response.
     */
    @rpc(Protobuf)
    @stream[RequestStreaming.type]
    def lotsOfGreetings(request: Observable[HelloRequest]): F[HelloResponse]

    /**
     * Bidirectional streaming RPCs where both sides send a sequence of messages using a read-write stream.
     * The two streams operate independently, so clients and servers can read and write in whatever order
     * they like: for example, the server could wait to receive all the client messages before writing its
     * responses, or it could alternately read a message then write a message, or some other combination of
     * reads and writes. The order of messages in each stream is preserved.
     *
     * https://grpc.io/docs/guides/concepts.html
     *
     * @param request Stream of requests.
     * @return Stream of server responses.
     */
    @rpc(Protobuf)
    @stream[BidirectionalStreaming.type]
    def bidiHello(request: Observable[HelloRequest]): F[Observable[HelloResponse]]

  }

}
```

The code might be explanatory by itself but let's review the different services one by one:

* `sayHello`: unary RPC, only the `@rpc` annotation would be needed in this case.
* `lotsOfReplies `: Server streaming RPC, where `@rpc` and `@stream` annotations are needed here. However, there are three different types of streaming (server, client and bidirectional), that are specified by the type parameter required in the `@stream` annotation, `@stream[ResponseStreaming.type]` in this particular definition.
* `lotsOfGreetings `: Client streaming RPC, `@rpc` should be sorted by the `@stream[RequestStreaming.type]` annotation.
* `bidiHello `: Bidirectional streaming RPC, where `@rpc` is accompanied by the `@stream[BidirectionalStreaming.type]` annotation.

**Note**: in [frees-rpc], the streaming features have been implemented with `monix.reactive.Observable`, see the [Monix Docs](https://monix.io/docs/2x/reactive/observable.html) for a wider explanation. These monix extensions have been implemented on top of the [gRPC Java API](https://grpc.io/grpc-java/javadoc/) and the `StreamObserver` interface.

## Generating a .proto file

Before entering implementation details, we mentioned that the [frees-rpc] ecosystem brings the ability to generate `.proto` files from the Scala definition, in order to maintain compatibility with other languages and systems outside of Scala.

This responsibility relies on [sbt-freestyle-protogen](https://github.com/frees-io/sbt-freestyle-protogen), an Sbt plugin to generate `.proto` files from the [frees-rpc] service definitions.

### Plugin Installation

Add the following line to _project/plugins.sbt_:

```scala
addSbtPlugin("io.frees" % "sbt-frees-protogen" % "0.0.14")
```

### Plugin Settings

There are a couple key settings that can be configured according to various needs:

* **`protoGenSourceDir`**: the Scala source directory, where your [frees-rpc] definitions are placed. By default: `baseDirectory.value / "src" / "main" / "scala"`.
* **`protoGenTargetDir`**: The protobuf target directory, where the `protoGen` task will write the `.proto` files, based on [frees-rpc] service definitions. By default: `baseDirectory.value / "src" / "main" / "proto"`.

Directories must exist; otherwise, the `protoGen` task will fail.

### Generation with protoGen

At this point, each time you want to update your `.proto` files from the scala definition, you have to run the following sbt task:

```bash
sbt protoGen
```

Using the example above, the result would be placed at `/src/main/proto/service.proto`, in the case that the scala file is named as `service.scala`. The content should be similar to:

```
// This file has been automatically generated for use by
// sbt-frees-protogen plugin, from freestyle-rpc service definitions

syntax = "proto3";

option java_package = "quickstart";
option java_multiple_files = true;
option java_outer_classname = "Quickstart";

message HelloRequest {
   string greeting = 1;
}

message HelloResponse {
   string reply = 1;
}

service Greeter {
   rpc sayHello (HelloRequest) returns (HelloResponse) {}
   rpc lotsOfReplies (HelloRequest) returns (stream HelloResponse) {}
   rpc lotsOfGreetings (stream HelloRequest) returns (HelloResponse) {}
   rpc bidiHello (stream HelloRequest) returns (stream HelloResponse) {}
}
```

## RPC Service Implementations

So far so good, not too much code, no business logic, just a protocol definition with Scala annotations. Conversely, in this section, we are going to see how to complete our quickstart example. We'll take a look at both sides, the server and the client.

### Server

Predictably, generating the server code is just implementing a service [Handler](http://frees.io/docs/core/interpreters/).

Next, our dummy `Greeter` server implementation:

```tut:silent
import cats.effect.Async
import cats.syntax.applicative._
import freestyle.free._
import freestyle.rpc.server.implicits._
import monix.execution.Scheduler
import monix.eval.Task
import monix.reactive.Observable
import service._

class ServiceHandler[F[_]: Async](implicit S: Scheduler) extends Greeter[F] {

  private[this] val dummyObservableResponse: Observable[HelloResponse] =
    Observable.fromIterable(1 to 5 map (i => HelloResponse(s"Reply $i")))

  override def sayHello(request: HelloRequest): F[HelloResponse] =
    HelloResponse(reply = "Good bye!").pure

  override def lotsOfReplies(request: HelloRequest): F[Observable[HelloResponse]] =
    dummyObservableResponse.pure

  override def lotsOfGreetings(request: Observable[HelloRequest]): F[HelloResponse] =
    request
      .foldLeftL((0, HelloResponse(""))) {
        case ((i, response), currentRequest) =>
          val currentReply: String = response.reply
          (
            i + 1,
            response.copy(
              reply = s"$currentReply\nRequest ${currentRequest.greeting} -> Response: Reply $i"))
      }
      .map(_._2)
      .to[F]

  override def bidiHello(request: Observable[HelloRequest]): F[Observable[HelloResponse]] =
    request
      .flatMap { request: HelloRequest =>
        println(s"Saving $request...")
        dummyObservableResponse
      }
      .onErrorHandle { e =>
        throw e
      }
      .pure
}
```

That's all. We have exposed a potential implementation on the server side.

### Server Runtime

As you can see, the generic handler above requires `F` as the type parameter, which corresponds with our target `Monad` when interpreting our program. In this section, we will satisfy all the runtime requirements, in order to make our server runnable.

#### Execution Context

In [frees-rpc] programs, we'll at least need an implicit evidence related to the [Monix] Execution Context: `monix.execution.Scheduler`.

> The `monix.execution.Scheduler` is inspired by `ReactiveX`, being an enhanced Scala `ExecutionContext` and also a replacement for Java’s `ScheduledExecutorService`, but also for Javascript’s `setTimeout`.

```tut:silent
import monix.execution.Scheduler

trait CommonRuntime {

  implicit val S: Scheduler = monix.execution.Scheduler.Implicits.global

}
```

As a side note, `CommonRuntime` will also be used later on for the client program.

#### Runtime Implicits

For the server bootstrapping, remember adding `frees-rpc-server` dependency to your build.

Now, we need to implicitly provide two things:

* A runtime interpreter of our `ServiceHandler` tied to a specific type. In our case, we'll use `cats.effects.IO`.
* A `ServerW` implicit evidence, compounded by:
	* RPC port where the server will bootstrap.
	* The set of configurations we want to add to our [gRPC] server, like our `Greeter` service definition. All these configurations are aggregated in a `List[GrpcConfig]`. Later on, an internal builder will build the final server based on this list. The full available list of settings are exposed in [this file](https://github.com/frees-io/freestyle-rpc/blob/master/rpc/src/main/scala/server/GrpcConfig.scala).

In summary, the result would be as follows:

```tut:silent
import cats.~>
import cats.effect.IO
import freestyle.rpc.server._
import freestyle.rpc.server.handlers._
import freestyle.rpc.server.implicits._
import freestyle.async.catsEffect.implicits._
import service._

object gserver {

  trait Implicits extends CommonRuntime {

    implicit val greeterServiceHandler: ServiceHandler[IO] = new ServiceHandler[IO]

    val grpcConfigs: List[GrpcConfig] = List(
      AddService(Greeter.bindService[IO])
    )

    implicit val serverW: ServerW = ServerW(8080, grpcConfigs)
  }

  object implicits extends Implicits

}
```

Here are a few additional notes related to the previous snippet of code:

* The Server will bootstrap on port `8080`.
* `Greeter.bindService` is an auto-derived method which creates, behind the scenes, the binding service for [gRPC]. It requires two type parameters, `F[_]` and `M[_]`.
	* `F[_]` would be our algebra, which matches with our `Greeter` service definition.
	* `M[_]`, the target monad, in our example: `cats.effects.IO`.

### Server Bootstrap

What else is needed? We just need to define a `main` method:

```tut:silent
import cats.effect.IO
import cats.effect.IO._
import freestyle.rpc.server.GrpcServer
import freestyle.rpc.server.implicits._

object RPCServer {

  import gserver.implicits._

  def main(args: Array[String]): Unit =
    server[IO].unsafeRunSync()

}
```

Fortunately, once all the runtime requirements are in place (**`import gserver.implicits._`**), we only have to write the previous piece of code, which primarily, should be the same in all cases (except if your target Monad is different from `cats.effects.IO`).

### Client

[frees-rpc] derives a client automatically based on the protocol. This is especially useful because you can distribute it depending on the protocol/service definitions. If you change something in your protocol definition, you will get a new client for free without having to write anything.

You will need to add either `frees-rpc-client-netty` or `frees-rpc-client-okhttp` to your build.

### Client Runtime

Similarly in this section, as we saw for the server case, we are defining all the client runtime configurations needed for communication with the server.

#### Execution Context

In our example, we are going to use the same Execution Context described for the Server. However, for the sake of observing a slightly different runtime configuration, our client will be interpreting to `monix.eval.Task`. Hence, in this case, we would only need the `monix.execution.Scheduler` implicit evidence.

We are going to interpret to `monix.eval.Task`, however, behind the scenes, we will use the [cats-effect] `IO` monad as an abstraction. Concretely, Freestyle has an integration with `cats-effect` that is included transitively in the classpath through `frees-async-cats-effect` dependency.

#### Runtime Implicits

First of all, we need to configure how the client will reach the server in terms of the transport layer. There are two supported methods:

* By Address (host/port): brings the ability to create a channel with the target's address and port number.
* By Target: it can create a channel with a target string, which can be either a valid [NameResolver](https://grpc.io/grpc-java/javadoc/io/grpc/NameResolver.html)-compliant URI or an authority string.

Additionally, we can add more optional configurations that can be used when the connection is occurring. All the options are available [here](https://github.com/frees-io/freestyle-rpc/blob/6b0e926a5a14fbe3d9282e8c78340f2d9a0421f3/rpc/src/main/scala/client/ChannelConfig.scala#L33-L46). As we will see shortly in our example, we are going to skip the negotiation (`UsePlaintext(true)`).

Given the transport settings and a list of optional configurations, we can create the [ManagedChannel.html](https://grpc.io/grpc-java/javadoc/io/grpc/ManagedChannel.html) object, using the `ManagedChannelInterpreter` builder.

So, taking into account all we have just said, how would our code look?

```tut:silent
import cats.implicits._
import cats.effect.IO
import freestyle.free.config.implicits._
import freestyle.async.catsEffect.implicits._
import freestyle.rpc.client._
import freestyle.rpc.client.config._
import freestyle.rpc.client.implicits._
import monix.eval.Task
import io.grpc.ManagedChannel
import service._

import scala.util.{Failure, Success, Try}

object gclient {

  trait Implicits extends CommonRuntime {

    val channelFor: ManagedChannelFor =
      ConfigForAddress[Try]("rpc.host", "rpc.port") match {
        case Success(c) => c
        case Failure(e) =>
          e.printStackTrace()
          throw new RuntimeException("Unable to load the client configuration", e)
    }

    implicit val serviceClient: Greeter.Client[Task] =
      Greeter.client[Task](channelFor)
  }

  object implicits extends Implicits
}
```

**Notes**:

* `host` and `port` would be read from the application configuration file.
* To be able to use the `ConfigForAddress` helper, you need to add the `frees-rpc-config` dependency to your build.

### Client Program

Once we have our runtime configuration defined as above, everything gets easier. This is an example of a client application, following our dummy quickstart:

```tut:silent
import service._
import gclient.implicits._
import monix.eval.Task

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object RPCDemoApp {

  def main(args: Array[String]): Unit = {

    val result = Await.result(serviceClient.sayHello(HelloRequest("foo")).runAsync, Duration.Inf)

    println(s"Result = $result")

  }

}
```

## frees-rpc annotations

Provided below is a summary of all the current annotations that [frees-rpc] provides:

Annotation | Scope | Arguments | Description
--- | --- | --- | ---
@service | [@tagless algebra] | - | Tags the `@free` algebra as [RPC] service, in order to derive server and client code (macro expansion). **Important**: `@free` annotation should go first, followed by the `@service` annotation, and not inversely.
@rpc | `Method` | (`SerializationType`) | Indicates the method is an RPC service. As `SerializationType` parameter value, `Protobuf` and `Avro` are the current supported serialization methods.
@stream | `Method` | [`S <: StreamingType`] | There are three different types of streaming: server, client, and bidirectional. Hence, the `S` type parameter can be `ResponseStreaming`, `RequestStreaming`, or `BidirectionalStreaming`, respectively.
@message | `Case Class` | - | Tags a case class a protobuf message.
@option | `Object` | [name: String, value: String, quote: Boolean] | used to define the equivalent headers in `.proto` files

## Metrics Reporting

[frees-rpc] currently provides two different ways to monitor [gRPC] services: `Prometheus` and `Dropwizard` (using the `Prometheus` extension). The usage is quite similar for both.

### Monitor Server Calls

In order to monitor the RPC calls on the server side, it's necessary to intercept them. We'll see how to do this in the next code fragment:

```tut:silent
import cats.~>
import cats.effect.IO
import freestyle.rpc.server._
import freestyle.rpc.server.handlers._
import freestyle.rpc.server.implicits._
import freestyle.async.catsEffect.implicits._
import service._

import io.prometheus.client.CollectorRegistry
import freestyle.rpc.prometheus.shared.Configuration
import freestyle.rpc.prometheus.server.MonitoringServerInterceptor

object InterceptingServerCalls extends CommonRuntime {

  import freestyle.rpc.interceptors.implicits._

  lazy val cr: CollectorRegistry = new CollectorRegistry()
  lazy val monitorInterceptor = MonitoringServerInterceptor(
    Configuration.defaultBasicMetrics.withCollectorRegistry(cr)
  )

  implicit val greeterServiceHandler: ServiceHandler[IO] = new ServiceHandler[IO]

  val grpcConfigs: List[GrpcConfig] = List(
    AddService(Greeter.bindService[IO].interceptWith(monitorInterceptor))
  )

  implicit val serverW: ServerW = ServerW(8080, grpcConfigs)

}
```

### Monitor Client Calls

In this case, in order to intercept the client calls we need additional configuration settings (by using `AddInterceptor`):

```tut:silent
import cats.implicits._
import cats.effect.IO
import freestyle.free.config.implicits._
import freestyle.async.catsEffect.implicits._
import freestyle.rpc.client._
import freestyle.rpc.client.config._
import freestyle.rpc.client.implicits._
import monix.eval.Task
import io.grpc.ManagedChannel
import service._

import scala.util.{Failure, Success, Try}

import freestyle.rpc.prometheus.shared.Configuration
import freestyle.rpc.prometheus.client.MonitoringClientInterceptor

object InterceptingClientCalls extends CommonRuntime {

  val channelFor: ManagedChannelFor =
    ConfigForAddress[Try]("rpc.host", "rpc.port") match {
      case Success(c) => c
      case Failure(e) =>
        e.printStackTrace()
        throw new RuntimeException("Unable to load the client configuration", e)
      }

  implicit val serviceClient: Greeter.Client[Task] =
    Greeter.client[Task](
      channelFor = channelFor,
      channelConfigList = List(
        UsePlaintext(true),
        AddInterceptor(
          MonitoringClientInterceptor(
            Configuration.defaultBasicMetrics
          )
        )
      )
    )
}
```

That's using `Prometheus` to monitor both [gRPC] ends.

### Dropwizard Metrics

The usage is equivalent, however, in this case, we need to put an instance of `com.codahale.metrics.MetricRegistry` on the scene, then, using the _Dropwizard_ integration that _Prometheus_ already provides (`DropwizardExports`) you can associate it with the collector registry:

```tut:silent
import com.codahale.metrics.MetricRegistry
import io.prometheus.client.dropwizard.DropwizardExports

val metrics: MetricRegistry      = new MetricRegistry
val configuration: Configuration = Configuration.defaultBasicMetrics
configuration.collectorRegistry.register(new DropwizardExports(metrics))
```

## Next Steps

If you want to delve deeper into [frees-rpc], we have a complete example at the [freestyle-rpc-examples] repository, which is based on the [Route Guide Demo](https://grpc.io/docs/tutorials/basic/java.html#generating-client-and-server-code) originally shared by the [gRPC Java Project](https://github.com/grpc/grpc-java/tree/6ea2b8aacb0a193ac727e061bc228b40121460e3/examples/src/main/java/io/grpc/examples/routeguide).

## Comparing HTTP and RPC

This extra section is not specifically about [frees-rpc]. Very often our microservices architectures are based on `HTTP` where perhaps, it is not the best glue to connect them, and [RPC] services might fit better.

[Metrifier] is a project where we compare, in different bounded ecosystems, `HTTP` and` RPC`. And it turns out RPC is usually faster than HTTP. If you're interested in learning more, we encourage to take a look at the documentation.

# References

* [Freestyle](http://frees.io/)
* [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call)
* [gRPC](https://grpc.io/)
* [Protocol Buffers Docs](https://developers.google.com/protocol-buffers/docs/overview)
* [scalameta](https://github.com/scalameta/scalameta)
* [PBDirect](https://github.com/btlines/pbdirect)
* [ScalaPB](https://scalapb.github.io/)
* [Monix](https://monix.io)
* [gRPC Java API](https://grpc.io/grpc-java/javadoc/)
* [Metrifier](https://github.com/47deg/metrifier)


[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
[frees-rpc]: https://github.com/frees-io/freestyle-rpc
[Java gRPC]: https://github.com/grpc/grpc-java
[JSON]: https://en.wikipedia.org/wiki/JSON
[gRPC guide]: https://grpc.io/docs/guides/
[@tagless algebra]: http://frees.io/docs/core/algebras/
[PBDirect]: https://github.com/btlines/pbdirect
[scalameta]: https://github.com/scalameta/scalameta
[Monix]: https://monix.io/
[cats-effect]: https://github.com/typelevel/cats-effect
[freestyle-rpc-examples]: https://github.com/frees-io/freestyle-rpc-examples
[Metrifier]: https://github.com/47deg/metrifier
[frees-config]: http://frees.io/docs/patterns/config/