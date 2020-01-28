---
layout: docs
title: Mu Home
permalink: /
---

# Quickstart

## What is Mu?

[Mu] is a toolkit designed to make it easier to build and maintain
microservices in a functional style.

While you focus on implementing the business logic for your service, let Mu take
care of the boilerplate, including:

* generation of model classes, service interfaces and clients from [Avro],
  [Protobuf] or [OpenAPI] <abbr title="Interface definition language">IDL</abbr> files
* serialization of requests and responses into Avro/Protobuf/JSON
* handling of streaming requests and responses using either [FS2] or [Monix]
  Observables.
* schema management and schema evolution
* metrics reporting
* ... and plenty more features on the way!

Specifically, Mu helps you to build:

* [gRPC] servers and clients based on either [Avro] or [Protobuf] protocol definitions
* REST servers and clients based on [OpenAPI] definitions

## Installation

[comment]: # (Start Replace)

The current version of [Mu] is "0.20.1".

[comment]: # (End Replace)

Mu is published for Scala `2.12.x`.

Because Mu makes use of macro annotations, you need to add the following to your
build.sbt:

```
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch)
```

We've found that the compiler plugin needs to be added to your build.sbt file
*after* your library dependencies due to the manner in which SBT evaluates the
build file.

### Artifacts

[Mu] is divided into multiple artifacts, grouped by scope:

* `Server`: specifically for the RPC server.
* `Client`: focused on the RPC auto-derived clients by `Mu`.
* `Server/Client`: used from other artifacts for both Server and Client.
* `Test`: useful to test `Mu` applications.

#### Common

| *Artifact Name*  | *Scope*  | *Mandatory*  | *Description*  |
|---|---|---|---|
| `mu-common`  | Server/Client  | Provided*  | Common things that are used throughout the project.  |
| `mu-rpc-internal-core`  | Server/Client  | Provided*  | Macros.  |
| `mu-rpc-internal-monix`  | Server/Client  | Provided*  | Macros.  |
| `mu-rpc-internal-fs2`  | Server/Client  | Provided*  | Macros.  |

#### Client/Server

| *Artifact Name*  | *Scope*  | *Mandatory*  | *Description*  |
|---|---|---|---|
| `mu-rpc-server`  | Server  | Yes  | Needed to attach RPC Services and spin-up an RPC Server.  |
| `mu-rpc-channel`  | Client  | Yes  | Mandatory to define protocols and auto-derived clients.  |
| `mu-rpc-monix`  | Client  | Yes  | Mandatory to define streaming operations with Monix Observables.  |
| `mu-rpc-fs2`  | Client  | Yes  | Mandatory to define streaming operations with fs2 Streams.  |
| `mu-rpc-netty`  | Client  | Yes*  | Mandatory on the client side if we are using `Netty` on the server side.  |
| `mu-rpc-netty-ssl`  | Server/Client  | No  | Adds the `io.netty:netty-tcnative-boringssl-static:jar` dependency, aligned with the Netty version (if that's the case) used in the `mu-rpc` build. See [this section](https://github.com/grpc/grpc-java/blob/master/SECURITY.md#netty) for more information. By adding this you wouldn't need to figure the right version, `mu-rpc` gives you the right one.  |
| `mu-rpc-okhttp`  | Client  | Yes*  | Mandatory on the client side if we are using `OkHttp` on the server side.  |

* `Yes*`: on the client-side, you must choose either `Netty` or `OkHttp` as the transport layer.
* `Provided*`: you don't need to add it to your build, it'll be transitively provided when using other dependencies.

#### Metrics

| *Artifact Name*   | *Scope*  | *Mandatory*  | *Description*  |
|---|---|---|---|
| `mu-rpc-prometheus`  | Server/Client  | No  | Scala interceptors which can be used to monitor gRPC services using Prometheus.  |
| `mu-rpc-dropwizard`  | Server/Client  | No  | Scala interceptors which can be used to monitor gRPC services using Dropwizard metrics.  |

#### Other

| *Artifact Name*  | *Scope*  | *Mandatory*  | *Description*  |
|---|---|---|---|
| `mu-config`  | Server/Client  | No  | Provides configuration helpers using [pureconfig] to load the application configuration values.  |
| `mu-rpc-testing`  | Test  | No  | Utilities to test out `Mu` applications. It provides the `grpc-testing` library as the transitive dependency.  |
| `mu-rpc-client-cache`  | Client  | No  | Provides an algebra for caching RPC clients.  |
| `mu-rpc-marshallers-jodatime`  | Server/Client  | No  | Provides marshallers for serializing and deserializing the `LocalDate` and `LocalDateTime` joda instances.  |

### Build
You can install any of these dependencies in your build as follows:

[comment]: # (Start Replace)

```scala
// required for the RPC server:
libraryDependencies += "io.higherkindness" %% "mu-rpc-server" % "0.20.1"

// required for a protocol definition:
libraryDependencies += "io.higherkindness" %% "mu-rpc-channel" % "0.20.1"

// required for a protocol definition with streaming operations:
libraryDependencies += "io.higherkindness" %% "mu-rpc-monix" % "0.20.1"
// or:
libraryDependencies += "io.higherkindness" %% "mu-rpc-fs2" % "0.20.1"

// required for the use of the derived RPC client/s, using either Netty or OkHttp as transport layer:
libraryDependencies += "io.higherkindness" %% "mu-rpc-netty" % "0.20.1"
// or:
libraryDependencies += "io.higherkindness" %% "mu-rpc-okhttp" % "0.20.1"

// optional - for both server and client configuration.
libraryDependencies += "io.higherkindness" %% "mu-config" % "0.20.1"

// optional - for both server and client metrics reporting, using Prometheus.
libraryDependencies += "io.higherkindness" %% "mu-rpc-prometheus" % "0.20.1"

// optional - for both server and client metrics reporting, using Dropwizard.
libraryDependencies += "io.higherkindness" %% "mu-rpc-dropwizard" % "0.20.1"

// optional - for the communication between server and client by using SSL/TLS.
libraryDependencies += "io.higherkindness" %% "mu-rpc-netty-ssl" % "0.20.1"

// optional - for using the jodatime marshallers.
libraryDependencies += "io.higherkindness" %% "mu-rpc-marshallers-jodatime" % "0.20.1"

// optional - for using the client cache.
libraryDependencies += "io.higherkindness" %% "mu-rpc-client-cache" % "0.20.1"
```

[comment]: # (End Replace)

[Avro]: https://avro.apache.org/
[FS2]: https://github.com/functional-streams-for-scala/fs2
[gRPC]: https://grpc.io/
[Monix]: https://monix.io/
[Mu]: https://github.com/higherkindness/mu-scala
[OpenAPI]: https://swagger.io/docs/specification/about/
[Protobuf]: https://developers.google.com/protocol-buffers
[pureconfig]: https://github.com/pureconfig/pureconfig
