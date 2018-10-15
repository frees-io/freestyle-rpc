---
layout: docs
title: Annotations
permalink: /docs/rpc/annotations
---

# Annotations

Provided below is a summary of all the current annotations that [frees-rpc] provides:

Annotation | Scope | Arguments | Description
--- | --- | --- | ---
@service | `Trait` | (`SerializationType`, `Compression`) | Tags the trait as an [RPC] service, in order to derive server and client code (macro expansion). As `SerializationType` parameter value, `Protobuf` and `Avro` are the current supported serialization methods. As `Compression` parameter value, only `Gzip` is supported.
@message | `Case Class` | - | Tags the case class as an RPC message.
@option | `Object` | `(name: String, value: Any)` | Defines the equivalent headers in `.proto` files.
@outputPackage | `Object` | `(value: String)` | Defines the `package` declaration in `.proto` files, and the `namespace` tag in `.avpr` files.
@outputName  | `Object` | `(value: String)` | Defines the `protocol` tag in `.avpr` files; if missing the `idlGen` tool will use the source file name (without `.scala` extension) instead.

[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
[frees-rpc]: https://github.com/higherkindness/mu-rpc
[Java gRPC]: https://github.com/grpc/grpc-java
[JSON]: https://en.wikipedia.org/wiki/JSON
[gRPC guide]: https://grpc.io/docs/guides/
[@tagless algebra]: http://frees.io/docs/core/algebras/
[PBDirect]: https://github.com/btlines/pbdirect
[scalamacros]: https://github.com/scalamacros/paradise
[Monix]: https://monix.io/
[cats-effect]: https://github.com/typelevel/cats-effect
[Metrifier]: https://github.com/47deg/metrifier
