---
layout: docs
title: Generating sources from IDL
permalink: /generate-sources-from-idl
---

# Generate sources from IDL

Since `Protobuf` and `Avro` are both a language-neutral, platform-neutral way of serializing structured data, our aim with [mu] is to create Scala definitions from these IDL files. 
Before going into implementation details let's see how to create the Scala files with our definitions.

Currently, `Avro` is supported in both `.avpr` (JSON) and `.avdl` (Avro IDL) formats, along with `Protobuf`.
The plugin's implementation basically wraps the [avrohugger] library for `Avro` 
and uses [skeuomorph] for `Protobuf` adding some mu-specific extensions.
## Plugin Installation

Add the following line to _project/plugins.sbt_:

[comment]: # (Start Replace)

```scala
addSbtPlugin("io.higherkindness" % "sbt-mu-idlgen" % "0.18.3")
```

[comment]: # (End Replace)

Note that the plugin is only available for Scala 2.12.


### Plugin Settings

For generating Scala definitions we use `srcGen`. The easiest way to use the plugin is integrating the source generation in your compile process by adding this import to your `build.sbt` file:

```scala
import higherkindness.mu.rpc.idlgen.IdlGenPlugin.autoImport._
``` 
and the setting,   
                                                                                
```scala  
idlType := proto  
sourceGenerators in Compile += (srcGen in Compile).taskValue
```

Otherwise, you can run the following sbt task (with the settings above in your `build.sbt`):
```sbtshell
sbt
project foo
srcGen   
```
Note that ``project foo`` it's only for multi-module projects.


There are a couple key settings that can be configured according to various needs. In that case you must add it as the following example:

```scala
idlType := "proto",
srcGenTargetDir := (Compile / sourceManaged).value / "mu_proto",
srcGenSerializationType := "Protobuf",
sourceGenerators in Compile += (srcGen in Compile).taskValue
```
In the example above we are using `proto` files with `Protobuf` serialization. Also, we are putting them in ``target/scala-2.12/src_managed/main/mu_proto``.

Some setting options:

* **`idlType`**: the type of IDL to be generated, either `proto` or `avro`.
* **`srcGenSerializationType`**: the serialization type when generating Scala sources from the IDL definitions. `Protobuf`, `Avro` or `AvroWithSchema`(see [Schema evolution/Avro](schema-evolution/avro)) are the current supported serialization types. By default, the serialization type is 'Avro'.
* **`srcGenSourceDirs`**: the list of directories where your IDL files are placed. By default: `Compile / resourceDirectory`, typically `src/main/resources/`.
* **`srcGenIDLTargetDir`**: the target directory where all the IDL files specified in `srcGenSourceDirs` will be copied. Given this configuration, the plugin will automatically copy the following to this target directory:
  * All the definitions extracted from the different `jar` or `sbt` modules, and also,
  * All the source folders specified in the `srcGenSourceDirs` setting.
* **`srcGenTargetDir`**: the Scala target directory, where the `srcGen` task will write the generated files in subpackages based on the namespaces declared in the IDL files. By default, `Compile / sourceManaged`, tipically  `target/scala-2.12/src_managed/main/`.

The source directory must exist. Target directories will be created upon generation.

There are more options:

* **`genOptions`**: additional options to add to the generated `@service` annotations, after the IDL type. Currently only supports `"Gzip"`.
* **`srcGenJarNames`**: the list of jar names or sbt modules containing the IDL definitions that will be used at compilation time by `srcGen` to generate the Scala sources. By default, this sequence is empty.
* **`idlGenBigDecimal`**: specifies how the `decimal` types will be generated. `ScalaBigDecimalGen` produces `scala.math.BigDecimal` and `ScalaBigDecimalTaggedGen` produces `scala.math.BigDecimal` but tagged with the 'precision' and 'scale'. i.e. `scala.math.BigDecimal @@ (Nat._8, Nat._2)`. By default `ScalaBigDecimalTaggedGen`.
* **`idlGenMarshallerImports`**: additional imports to add on top to the generated service files. This property can be used for importing extra codecs for your services. By default:
  * `List(BigDecimalAvroMarshallers, JavaTimeDateAvroMarshallers)` if `srcGenSerializationType` is `Avro` or `AvroWithSchema` and `idlGenBigDecimal` is `ScalaBigDecimalGen`
  * `List(BigDecimalTaggedAvroMarshallers, JavaTimeDateAvroMarshallers)` if `srcGenSerializationType` is `Avro` or `AvroWithSchema` and `idlGenBigDecimal` is `ScalaBigDecimalTaggedGen`
  * `List(BigDecimalProtobufMarshallers, JavaTimeDateProtobufMarshallers)` if `srcGenSerializationType` is `Protobuf`.

The `JodaDateTimeAvroMarshallers` and `JodaDateTimeProtobufMarshallers` are also available, but they need the dependency `mu-rpc-marshallers-jodatime`. You can also specify custom imports with the following:
  * `idlGenMarshallerImports := List(higherkindness.mu.rpc.idlgen.Model.CustomMarshallersImport("com.sample.marshallers._"))`
  * See the [Custom codecs section in core concepts](core-concepts#custom-codecs) for more information.


You can even use `IDL` definitions packaged into artifacts within your classpath. 
In that particular situation, you need to setup `srcGenJarNames`, 
specifying the artifact names (or sbt module names) that will be unzipped/used to extract the `IDL` files.

`srcGenJarNames ` can be very useful when you want to distribute your `IDL` files 
without binary code (to prevent binary conflicts in clients).


*Note*: regarding `srcGenSourceDirs`, all the directories configured as the source 
will be distributed in the resulting jar artifact preserving the same folder structure as in the source.

The following example shows how to set up a dependency with another artifact or sbt module containing the IDL definitions (`foo-domain`):

```
//...
.settings(
  Seq(
      idlType := "avro",
      srcGenSerializationType := "AvroWithSchema",
      srcGenJarNames := Seq("foo-domain"),
      srcGenTargetDir := (Compile / sourceManaged).value / "compiled_avro",
      sourceGenerators in Compile += (Compile / srcGen).taskValue,
      libraryDependencies ++= Seq(
        "io.higherkindness" %% "mu-rpc-channel" % V.muRPC
      )
  )
)
//...
```

##Generating sources

Let's supose a `proto` file like

  ```proto
    syntax = "proto3";
    
    package foo.bar;
    
    message HelloRequest {
      string arg1 = 1;
      string arg2 = 2;
      repeated string arg3 = 3;
    }
    
    message HelloResponse {
      string arg1 = 1;
      string arg2 = 2;
      repeated string arg3 = 3;
    }
    
    service ProtoGreeter {
      rpc SayHelloProto (HelloRequest) returns (HelloResponse);
      rpc LotsOfRepliesProto (HelloRequest) returns (stream HelloResponse);
      rpc LotsOfGreetingsProto (stream HelloRequest) returns (HelloResponse);
      rpc BidiHelloProto (stream HelloRequest) returns (stream HelloResponse);
      rpc BidiHelloFs2Proto (stream HelloRequest) returns (stream HelloResponse);
    }
  ```
 As you can see, we are defining two case classes, ``HelloRequest`` and `HelloResponse`, and a service, `ProtoGreeter`. 
 Therefore, the generated scala definition is
 
 ```scala
    package foo.bar
    import higherkindness.mu.rpc.protocol._
    import fs2.Stream
    import shapeless.{:+:, CNil}
    
    
    
    object GreeterService { 
    
    @message final case class HelloRequest(arg1: String, arg2: String, arg3: List[String])
    @message final case class HelloResponse(arg1: String, arg2: String, arg3: List[String])
    @service(Protobuf) trait ProtoGreeter[F[_]] {
      def SayHelloProto(req: HelloRequest): F[HelloResponse]
      def LotsOfRepliesProto(req: HelloRequest): Stream[F, HelloResponse]
      def LotsOfGreetingsProto(req: Stream[F, HelloRequest]): F[HelloResponse]
      def BidiHelloProto(req: Stream[F, HelloRequest]): Stream[F, HelloResponse]
      def BidiHelloFs2Proto(req: Stream[F, HelloRequest]): Stream[F, HelloResponse]
    }
    
    }
 ```
Note that Scala case class is preceded by the `@message` annotation. It's generated by `srcGen` but not necessary for [mu].

Also, we can use `avro`. From an `.avpr`: 

```avroidl

{
  "namespace" : "foo.bar",
  "protocol" : "MyGreeterService",
  "types" : [
    {
      "name" : "HelloRequest",
      "type" : "record",
      "fields" : [
        {
          "name" : "arg1",
          "type" : "string"
        },
        {
          "name" : "arg2",
          "type" : [
            "null",
            "string"
          ]
        },
        {
          "name" : "arg3",
          "type" : {
            "type" : "array",
            "items" : "string"
          }
        }
      ]
    },
    {
      "name" : "HelloResponse",
      "type" : "record",
      "fields" : [
        {
          "name" : "arg1",
          "type" : "string"
        },
        {
          "name" : "arg2",
          "type" : [
            "null",
            "string"
          ]
        },
        {
          "name" : "arg3",
          "type" : {
            "type" : "array",
            "items" : "string"
          }
        }
      ]
    }
  ],
  "messages" : {
    "sayHelloAvro" : {
      "request" : [
        {
          "name" : "arg",
          "type" : "HelloRequest"
        }
      ],
      "response" : "HelloResponse"
    },
    "sayNothingAvro" : {
      "request" : [
      ],
      "response" : "null"
    }
  }
}

```
or ``.avdl``:

```avroidl
@namespace("foo.bar")
protocol MyGreeterService{

    record HelloRequest {
        string arg1;
        union { null, string } arg2;
        array<string> arg3;
    }

    record HelloResponse {
        string arg1;
        union { null, string } arg2;
        array<string>  arg3;
    }

    foo.bar.HelloResponse sayHelloAvro(foo.bar.HelloRequest arg);

    void sayNothingAvro();
}
```


So we get the same service:

```scala
package foo.bar

import higherkindness.mu.rpc.internal.encoders.avro.bigDecimalTagged._
import higherkindness.mu.rpc.internal.encoders.avro.javatime._
import higherkindness.mu.rpc.protocol._

@message case class HelloRequest(arg1: String, arg2: Option[String], arg3: Seq[String])

@message case class HelloResponse(arg1: String, arg2: Option[String], arg3: Seq[String])

@service(Avro) trait MyGreeterService[F[_]] {

  def sayHelloAvro(arg: foo.bar.HelloRequest): F[foo.bar.HelloResponse]

  def sayNothingAvro(arg: Empty.type): F[Empty.type]

}
```
 
[mu]: https://github.com/higherkindness/mu
[avrohugger]: https://github.com/julianpeeters/avrohugger
[skeuomorph]: https://github.com/higherkindness/skeuomorph