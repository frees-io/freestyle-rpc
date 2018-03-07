---
layout: docs
title: Quickstart
permalink: /docs/rpc/quickstart
---

# Quickstart

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
`frees-rpc-netty-ssl` | Server/Client | No | It adds the `io.netty:netty-tcnative-boringssl-static:jar` dependency, aligned with the Netty version (if that's the case) used in the `frees-rpc` build. See [this section](https://github.com/grpc/grpc-java/blob/master/SECURITY.md#netty) for more information. Adding this you wouldn't need to figure out which would be the right version, `frees-rpc` gives you the right one.

* `Yes*`: on the client-side, you must choose either `Netty` or `OkHttp` as the transport layer.
* `Provided*`: you don't need to add it to your build, it'll be transitively provided when using other dependencies.

You can install any of these dependencies in your build as follows:

[comment]: # (Start Replace)

```scala
// required for the RPC Server:
libraryDependencies += "io.frees" %% "frees-rpc-server"            % "0.11.1"

// required for a protocol definition:
libraryDependencies += "io.frees" %% "frees-rpc-client-core"       % "0.11.1"

// required for the use of the derived RPC Client/s, using either Netty or OkHttp as transport layer:
libraryDependencies += "io.frees" %% "frees-rpc-client-netty"      % "0.11.1"
// or:
libraryDependencies += "io.frees" %% "frees-rpc-client-okhttp"     % "0.11.1"

// optional - for both server and client configuration.
libraryDependencies += "io.frees" %% "frees-rpc-config"            % "0.11.1"

// optional - for both server and client metrics reporting, using Prometheus.
libraryDependencies += "io.frees" %% "frees-rpc-prometheus-server" % "0.11.1"
libraryDependencies += "io.frees" %% "frees-rpc-prometheus-client" % "0.11.1"

// optional - for both server and client metrics reporting, using Dropwizard.
libraryDependencies += "io.frees" %% "frees-rpc-dropwizard-server" % "0.11.1"
libraryDependencies += "io.frees" %% "frees-rpc-dropwizard-client" % "0.11.1"

// optional - for the communication between server and client by using SSL/TLS.
libraryDependencies += "io.frees" %% "frees-rpc-netty-ssl" % "0.11.1"
```

[comment]: # (End Replace)