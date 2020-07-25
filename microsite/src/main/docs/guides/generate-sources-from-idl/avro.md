---
layout: docs
title: Generating sources from Avro
section: guides
permalink: /guides/generate-sources-from-avro
---

# Generating sources from Avro

## Getting started

First add the sbt plugin in `project/plugins.sbt`:

```scala
addSbtPlugin("io.higherkindness" % "sbt-mu-srcgen" % "@VERSION@")
```

**NOTE**

For users of the `sbt-mu-srcgen` plugin `v0.22.x` and below, the plugin is enabled automatically as soon as it's added to the `project/plugins.sbt`.  However, for users of the `sbt-mu-srcgen` plugin `v0.23.x` and beyond, the plugin needs to be manually enabled for any module for which you want to generate code.  To enable the module, add the following line to your `build.sbt`
```scala
enablePlugins(SrcGenPlugin)
```

Once the plugin is enabled, you can configure it by adding a few lines to `build.sbt`:

```scala
import higherkindness.mu.rpc.srcgen.Model._

// Look for Avro IDL files
muSrcGenIdlType := IdlType.Avro

// Make it easy for 3rd-party clients to communicate with our gRPC server
muSrcGenIdiomaticEndpoints := true
```

Finally, make sure you have Scala macro annotations enabled, to ensure the
generated code compiles. How you do this depends on which Scala version you are
using.

For Scala 2.12, add this to `build.sbt`:

```scala
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch)
```

For Scala 2.13, add this:

```scala
scalacOptions += "-Ymacro-annotations"
```

Suppose you want to generate Scala code for a gRPC service based on the
following Avro IDL file, `src/main/resources/hello.avdl`:

```plaintext
@namespace("foo")
protocol AvroGreeter {

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

    foo.HelloResponse sayHelloAvro(foo.HelloRequest arg);

}
```

**NOTE:** please be aware that `mu-scala` prohibits the use of primitive types in request arguments; for more context, see the [source generation reference](/reference/source-generation).

You can run the source generator directly:

```sh
$ sbt muSrcGen
```

or as part of compilation:

```sh
$ sbt compile
```

Once the source generator has run, there should be a generated Scala file at
`target/scala-2.13/src_managed/main/foo/AvroGreeter.scala`.

It will look like this (tidied up and simplified for readability):

```scala
package foo

import higherkindness.mu.rpc.internal.encoders.avro.bigDecimalTagged._
import higherkindness.mu.rpc.internal.encoders.avro.javatime._
import higherkindness.mu.rpc.protocol._

final case class HelloRequest(
  arg1: String,
  arg2: Option[String],
  arg3: Seq[String]
)

final case class HelloResponse(
  arg1: String,
  arg2: Option[String],
  arg3: Seq[String]
)

@service(Avro,compressionType = Identity,namespace = Some("foo"),methodNameStyle = Capitalize) trait AvroGreeter[F[_]] {
  def sayHelloAvro(arg: HelloRequest): F[HelloResponse]
}
```

It's also possible to generate Scala code from `.avpr` (JSON) files.

Suppose you delete `src/main/resources/hello.avdl` and replace it with `src/main/resources/hello.avpr`:

```plaintext
{
  "namespace" : "foo",
  "protocol" : "AvroGreeter",
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
    }
  }
}
```

If you run `sbt clean muSrcGen`, you should end up with exactly the same generated
Scala file as before.

## Avro code generation details

This section explains the different Scala structures that are generated from Avro IDL.

To achieve this generation Mu's source generator uses [avrohugger](https://github.com/julianpeeters/avrohugger) behind the scenes.

### Avro Protocols

Let's start from the beginning, everything on Avro should be declared inside a `protocol`.

The name of that protocol will be the name of our Scala file.

```plaintext
protocol People {
 ...
}
```

***muSrcGen =>***

`People.scala`

Furthermore, the `protocol` can have a `namespace` which will be our Scala package:

```plaintext
@namespace("example.protocol")
protocol People {
 ...
}
```

***muSrcGen =>***

`example.protocol.People.scala`

### Messages

On Avro, the messages are declared with the keyword `record` and contains different fields inside.
The `record` will be translated to a `case class` with the same fields on it:

```plaintext
record Person {
  string name;
  int age;
  boolean crossfitter;
}
```

***muSrcGen =>***

```scala mdoc:silent
case class Person(name: String, age: Int, crossfitter: Boolean)
```

### Enums

Avro supports `enum`s too and they are translated to a Scala `Enumeration`:

```plaintext
enum Errors {
  NotFound, Duplicated, None
}
```

***muSrcGen =>***

```scala mdoc:silent
object Errors extends Enumeration {
  type Errors = Value
  val NotFound, Duplicated, None = Value
}
```

### Unions

`Unions` are a complex Avro type for fields inside `record`s.
As its name suggest, it represents a type composed by another types.

Depending on the types composing the `union`, `Mu` will interpret it on different ways:

### Optional fields

When we add a **`null`** to a `union` expression, we'll get a Scala `Option` of the other types declared along the `null`:

```plaintext
record PeopleRequest {
  union {null, string} name;
}
```

***muSrcGen =>***

```scala mdoc:silent
case class PeopleRequest(name: Option[String])
```

### Eithers

When we join **`two non-null types`** on a `union` we'll get an Scala `Either` with the same types order:

```plaintext
record PeopleResponse {
  union { Errors, Person } result;
}
```

***muSrcGen =>***

```scala mdoc:silent
case class PeopleResponse(result: Either[Errors.Value, Person])
```

### Coproducts

And finally, when we have **`three or more non-null types`** on a single `union`,
we'll have a [shapeless](https://github.com/milessabin/shapeless/wiki/Feature-overview:-shapeless-2.0.0#coproducts-and-discriminated-unions)' `Coproduct` on the same order as well:

```plaintext
record PeopleResponse {
  union{ string, int, Errors } result;
}
```

***muSrcGen =>***

```scala mdoc:silent:nest
import shapeless.{:+:, CNil}

case class PeopleResponse(result: String :+: Int :+: Errors.Value :+: CNil)
```

### Services

When we declare a method or `endpoint` inside a `protocol` this will be converted to a `trait` and intended as a **`Mu service`**.

As we would want to have our models separated from our services. Avro make us able to import other Avro files to use their `record`s:

```plaintext
protocol PeopleService {
  import idl "People.avdl"; //Under the same folder

  example.protocol.PeopleResponse getPerson(example.protocol.PeopleRequest request);

}
```

***muSrcGen =>***

```scala
@service(Avro) trait PeopleService[F[_]] {

  def getPerson(request: example.protocol.PeopleRequest): F[example.protocol.PeopleResponse]

}
```

Also, an endpoint can be declared without params or non returning anything and `Mu` will use its `Empty` type to cover these cases:

```plaintext
protocol PeopleService {

  void insertPerson();

}
```

***muSrcGen =>***

```scala
@service(Avro) trait PeopleService[F[_]] {

  def insertPerson(arg: Empty.type): F[Empty.type]

}
```

For a full understanding of the Avro syntax we recommend you to take a look to
the [Avro Official site](http://avro.apache.org/docs/current/idl.html) where you
can find all the Avro supported types and some interesting resources.