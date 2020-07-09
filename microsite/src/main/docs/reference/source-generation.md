---
layout: docs
title: Source generation
section: reference
permalink: /reference/source-generation
---

# Source generation reference

This is a reference page for the `sbt-mu-srcgen` sbt plugin, which generates
Scala source code from Avro/Protobuf/OpenAPI IDL files.

## Settings

This section explains each of the sbt plugin's settings.  As a reminder, this plugin needs to be manually enabled for any module for which you want to generate code; you can do that by adding the following to your `build.sbt`:

```scala
enablePlugins(SrcGenPlugin)
```

### muSrcGenIdlType

The most important sbt setting is `muSrcGenIdlType`, which tells the plugin what kind of
IDL files (Avro/Protobuf/OpenAPI) to look for.

```scala
muSrcGenIdlType := IdlType.Proto // or IdlType.Avro or IdlType.OpenAPI
```

### muSrcGenSerializationType

Another important setting is `muSrcGenSerializationType`, which specifies how
messages should be encoded on the wire. This should match the format you chose
for the `muSrcGenIdlType` setting:

* For Protobuf, choose `SerializationType.Protobuf`
* For Avro, choose either `SerializationType.Avro` or `SerializationType.AvroWithSchema`.
    * If you choose `Avro`, it means your client and server must always use
      exactly the same version of the schema.
    * If you choose `AvroWithSchema`, the writer schema will be included in
      every message sent, which introduces a bandwidth overhead but allows
      schema evolution. In other words, the server and client can use different
      versions of a schema, as long as they are compatible with each other.  See
      the [schema evolution](schema-evolution/avro) section for more details on
      schema evolution.
* For OpenAPI, this setting is ignored, so you don't need to set it.

```scala
muSrcGenSerializationType := SerializationType.Protobuf // or SerializationType.Avro or SerializationType.AvroWithSchema
```

### Other basic settings

| Setting | Description | Default value |
| --- | --- | --- |
| `muSrcGenSourceDirs` | The list of directories where your IDL files can be found.<br/><br/>Note: all the directories configured as sources will be distributed in the resulting jar artifact preserving the same folder structure as in the source. | `Compile / resourceDirectory`, typically `src/main/resources/` |
| `muSrcGenIdlTargetDir` | The directory where all discovered IDL files will be copied in preparation for Scala code generation. The plugin will automatically copy the following to the target directory:<br/> * All the IDL files and directories in the directory specified by `muSrcGenSourceDirs`<br/> * All the IDL files extracted from the JAR files or sbt modules specified by `muSrcGenJarNames` (see the "Advanced settings" section below) | `Compile / resourceManaged`, typically `target/scala-2.13/resource_managed/main` |
| `muSrcGenTargetDir` | The directory where the `muSrcGen` task will write the generated files. The files will be placed in subdirectories based on the namespaces declared in the IDL files. | `Compile / sourceManaged`, typically `target/scala-2.13/src_managed/main/` |

**Note**: The directories referenced in `muSrcGenSourceDirs` must exist. Target directories will be created upon generation.

### Advanced settings

| Setting | Description | Default value |
| --- | --- | --- |
| `muSrcGenJarNames` | A list of JAR file or sbt module names where extra IDL files can be found. See the [srcGenJarNames section](#musrcgenjarnames) section below for more details. | `Nil` |
| `muSrcGenIdlExtension` | The extension of IDL files to extract from JAR files or sbt modules. | * `avdl` if `muSrcGenIdlType` is `avro`<br/> * `proto` if `muSrcGenIdlType` is `Proto` |
| `muSrcGenBigDecimal` | Specifies how Avro `decimal` types will be represented in the generated Scala. `ScalaBigDecimalGen` produces `scala.math.BigDecimal`. `ScalaBigDecimalTaggedGen` produces `scala.math.BigDecimal` tagged with the 'precision' and 'scale' using a Shapeless tag, e.g. `scala.math.BigDecimal @@ (Nat._8, Nat._2)`. | `ScalaBigDecimalTaggedGen`
| `muSrcGenCompressionType` | The compression type that will be used by generated RPC services. Set to `higherkindness.mu.rpc.srcgen.Model.GzipGen` for Gzip compression. | `higherkindness.mu.rpc.srcgen.Model.NoCompressionGen` |
| `muSrcGenIdiomaticEndpoints` | Flag indicating if idiomatic gRPC endpoints should be used. If `true`, the service operations will be prefixed by the namespace and the methods will be capitalized. | `false` |
| `muSrcGenStreamingImplementation` | Specifies whether generated Scala code will use FS2 `Stream[F, A]` or Monix `Observable[A]` as its streaming implementation. FS2 is the default. This setting is only relevant if you have any RPC endpoint definitions that involve streaming. | `higherkindness.mu.rpc.srcgen.Model.Fs2Stream` |
| `muSrcGenMarshallerImports` | see explanation below | see explanation below |

### muSrcGenMarshallerImports

This setting specifies additional imports to add on top to the generated service files. This property can be used for importing extra codecs for your services.

By default:
  * `List(BigDecimalAvroMarshallers, JavaTimeDateAvroMarshallers)` if `muSrcGenSerializationType` is `Avro` or `AvroWithSchema` and `muSrcGenBigDecimal` is `ScalaBigDecimalGen`
  * `List(BigDecimalTaggedAvroMarshallers, JavaTimeDateAvroMarshallers)` if `muSrcGenSerializationType` is `Avro` or `AvroWithSchema` and `muSrcGenBigDecimal` is `ScalaBigDecimalTaggedGen`
  * `List(BigDecimalProtobufMarshallers, JavaTimeDateProtobufMarshallers)` if `muSrcGenSerializationType` is `Protobuf`.

The `JodaDateTimeAvroMarshallers` and `JodaDateTimeProtobufMarshallers` are also available, but they need the dependency `mu-rpc-marshallers-jodatime`.

You can also specify custom imports with the following:

```scala
muSrcGenMarshallerImports := List(higherkindness.mu.rpc.srcgen.Model.CustomMarshallersImport("com.sample.marshallers._"))
```

See the [custom gRPC serialization guide](../guides/custom-grpc-serialization) for more information.

### muSrcGenJarNames

You can use IDL files packaged into artifacts within your classpath, e.g. JAR
files added to the classpath via `libraryDependencies`, or other sbt modules.

`muSrcGenJarNames` can be very useful when you want to distribute your `IDL` files
without binary code (to prevent binary conflicts in clients).

The following example shows how to set up a dependency with another artifact or
sbt module containing the IDL definitions (`foo-domain`):

```scala
//...
.settings(
  Seq(
      muSrcGenIdlType := IdlType.Avro,
      muSrcGenSerializationType := SerializationType.AvroWithSchema,
      muSrcGenJarNames := Seq("foo-domain"),
      muSrcGenTargetDir := (Compile / sourceManaged).value / "compiled_avro",
      libraryDependencies ++= Seq(
        "io.higherkindness" %% "mu-rpc-service" % V.muRPC
      )
  )
)
//...
```

## Implementation note: two-stage code generation

For gRPC services generated from an Avro or Protobuf definition, there are
actually two stages of code generation at work.

1. The `sbt-mu-srcgen` plugin will parse your IDL files and transform them into
   Scala code. It writes this code to `.scala` files under
   `target/scala-2.13/src_managed`.

2. The generated Scala code contains `@service` macro annotations. When these
   files are compiled, the compiler will expand these annotations by executing a
   macro, which generates a load of boilerplate code to help with building a
   gRPC server or client.

For example, the following `.proto` file:

```proto
syntax = "proto3";

package foo.bar;

message MyRequest {
  string a = 1;
}

message MyResponse {
  string a= 1;
}

service MyService {
  rpc MyEndpoint (MyRequest) returns (MyResponse);
}
```

would result in a `.scala` file that looks like (slightly simplified):

```scala
package foo.bar

object myproto {
  final case class MyRequest(a: String)
  final case class MyResponse(a: String)

  @service(Protobuf) trait MyService[F[_]] {
    def MyEndpoint(req: MyReqeust): F[MyResponse]
  }
}
```

After the `@service` annotation is expanded at compile time, the entire
generated code would look something like:

```scala
package foo.bar

object myproto {
  final case class MyRequest(a: String)
  final case class MyResponse(a: String)

  @service(Protobuf) trait MyService[F[_]] {
    def MyEndpoint(req: MyReqeust): F[MyResponse]
  }

  object MyService {

    def bindService[F[_]: ConcurrentEffect](
      implicit algebra: MyService[F]
    ): F[io.grpc.ServerServiceDefinition] = ...

    def client[F[_]: ConcurrentEffect: ContextShift](
      channelFor: higherkindness.mu.rpc.ChannelFor,
      channelConfigList: List[higherkindness.mu.rpc.channel.ManagedChannelConfig] = List(UsePlaintext),
      options: io.grpc.CallOptions = io.grpc.CallOptions.DEFAULT
    ): Resource[F, MyService[F]] = ...

    def clientFromChannel[F[_]: ConcurrentEffect: ContextShift](
      channel: F[io.grpc.ManagedChannel],
      options: io.grpc.CallOptions = io.grpc.CallOptions.DEFAULT
    ): Resource[F, MyService[F]]

    def unsafeClient[F[_]: ConcurrentEffect: ContextShift](
      channelFor: higherkindness.mu.rpc.ChannelFor,
      channelConfigList: List[higherkindness.mu.rpc.channel.ManagedChannelConfig] = List(UsePlaintext),
      options: io.grpc.CallOptions = io.grpc.CallOptions.DEFAULT
    ): MyService[F] = ...

    def unsafeClientFromChannel[F[_]: ConcurrentEffect: ContextShift](
      channel: io.grpc.ManagedChannel,
      options: io.grpc.CallOptions = io.grpc.CallOptions.DEFAULT
    ): MyService[F]

  }

}
```

You can see that the macro has generated a `MyService` companion object
containing a number of helper methods for building gRPC servers and clients.

[Mu]: https://github.com/higherkindness/mu-scala
