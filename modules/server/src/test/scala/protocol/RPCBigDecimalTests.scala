/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freestyle.rpc
package protocol

import cats.Applicative
import cats.syntax.applicative._
import org.scalatest._
import freestyle.rpc.common._
import freestyle.rpc.internal.encoders.avro.bigdecimal._
import freestyle.rpc.internal.encoders.avro.bigdecimal.marshallers._
import freestyle.rpc.internal.encoders.pbd.bigDecimal._
import freestyle.rpc.testing.servers.withServerChannel
import org.scalacheck.Prop._
import org.scalatest.prop.Checkers

class RPCBigDecimalTests extends RpcBaseTestSuite with BeforeAndAfterAll with Checkers {

  object RPCService {

    case class Request(bigDecimal: BigDecimal, label: String)

    case class Response(bigDecimal: BigDecimal, result: String, check: Boolean)

    @service(Protobuf) trait ProtoRPCServiceDef[F[_]] {
      def bigDecimalProto(bd: BigDecimal): F[BigDecimal]
      def bigDecimalProtoWrapper(req: Request): F[Response]
    }
    @service(Avro) trait AvroRPCServiceDef[F[_]] {
      def bigDecimalAvro(bd: BigDecimal): F[BigDecimal]
      def bigDecimalAvroWrapper(req: Request): F[Response]
    }
    @service(AvroWithSchema) trait AvroWithSchemaRPCServiceDef[F[_]] {
      def bigDecimalAvroWithSchema(bd: BigDecimal): F[BigDecimal]
      def bigDecimalAvroWithSchemaWrapper(req: Request): F[Response]
    }

    class RPCServiceDefImpl[F[_]: Applicative]
        extends ProtoRPCServiceDef[F]
        with AvroRPCServiceDef[F]
        with AvroWithSchemaRPCServiceDef[F] {

      def bigDecimalProto(bd: BigDecimal): F[BigDecimal] = bd.pure

      def bigDecimalProtoWrapper(req: Request): F[Response] =
        Response(req.bigDecimal, req.label, check = true).pure

      def bigDecimalAvro(bd: BigDecimal): F[BigDecimal] = bd.pure

      def bigDecimalAvroWrapper(req: Request): F[Response] =
        Response(req.bigDecimal, req.label, check = true).pure

      def bigDecimalAvroWithSchema(bd: BigDecimal): F[BigDecimal] = bd.pure

      def bigDecimalAvroWithSchemaWrapper(req: Request): F[Response] =
        Response(req.bigDecimal, req.label, check = true).pure
    }

  }

  "A RPC server" should {

    import RPCService._
    import monix.execution.Scheduler.Implicits.global

    implicit val H: RPCServiceDefImpl[ConcurrentMonad] = new RPCServiceDefImpl[ConcurrentMonad]

    "be able to serialize and deserialize BigDecimal using proto format" in {

      withServerChannel(ProtoRPCServiceDef.bindService[ConcurrentMonad]) { sc =>
        val client: ProtoRPCServiceDef.Client[ConcurrentMonad] =
          ProtoRPCServiceDef.clientFromChannel[ConcurrentMonad](sc.channel)

        check {
          forAll { bd: BigDecimal =>
            client.bigDecimalProto(bd).unsafeRunSync() == bd
          }
        }

      }

    }

    "be able to serialize and deserialize BigDecimal in a Request using proto format" in {

      withServerChannel(ProtoRPCServiceDef.bindService[ConcurrentMonad]) { sc =>
        val client: ProtoRPCServiceDef.Client[ConcurrentMonad] =
          ProtoRPCServiceDef.clientFromChannel[ConcurrentMonad](sc.channel)

        check {
          forAll { (bd: BigDecimal, s: String) =>
            client.bigDecimalProtoWrapper(Request(bd, s)).unsafeRunSync() == Response(
              bd,
              s,
              check = true)
          }
        }

      }

    }

    "be able to serialize and deserialize BigDecimal using avro format" in {

      withServerChannel(AvroRPCServiceDef.bindService[ConcurrentMonad]) { sc =>
        val client: AvroRPCServiceDef.Client[ConcurrentMonad] =
          AvroRPCServiceDef.clientFromChannel[ConcurrentMonad](sc.channel)

        check {
          forAll { bd: BigDecimal =>
            client.bigDecimalAvro(bd).unsafeRunSync() == bd
          }
        }

      }

    }

    "be able to serialize and deserialize BigDecimal in a Request using avro format" in {

      withServerChannel(AvroRPCServiceDef.bindService[ConcurrentMonad]) { sc =>
        val client: AvroRPCServiceDef.Client[ConcurrentMonad] =
          AvroRPCServiceDef.clientFromChannel[ConcurrentMonad](sc.channel)

        check {
          forAll { (bd: BigDecimal, s: String) =>
            client.bigDecimalAvroWrapper(Request(bd, s)).unsafeRunSync() == Response(
              bd,
              s,
              check = true)
          }
        }

      }

    }

    "be able to serialize and deserialize BigDecimal using avro with schema format" in {

      withServerChannel(AvroWithSchemaRPCServiceDef.bindService[ConcurrentMonad]) { sc =>
        val client: AvroWithSchemaRPCServiceDef.Client[ConcurrentMonad] =
          AvroWithSchemaRPCServiceDef.clientFromChannel[ConcurrentMonad](sc.channel)

        check {
          forAll { bd: BigDecimal =>
            client.bigDecimalAvroWithSchema(bd).unsafeRunSync() == bd
          }
        }

      }

    }

    "be able to serialize and deserialize BigDecimal in a Request using avro with schema format" in {

      withServerChannel(AvroWithSchemaRPCServiceDef.bindService[ConcurrentMonad]) { sc =>
        val client: AvroWithSchemaRPCServiceDef.Client[ConcurrentMonad] =
          AvroWithSchemaRPCServiceDef.clientFromChannel[ConcurrentMonad](sc.channel)

        check {
          forAll { (bd: BigDecimal, s: String) =>
            client.bigDecimalAvroWithSchemaWrapper(Request(bd, s)).unsafeRunSync() == Response(
              bd,
              s,
              check = true)
          }
        }
      }
    }
  }
}
