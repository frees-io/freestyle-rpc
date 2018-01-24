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
package withtagless

import cats.effect.Async
import cats.syntax.applicative._
import freestyle.tagless.tagless
import freestyle.rpc.common._
import freestyle.rpc.protocol._
import freestyle.rpc.server.implicits._
import monix.reactive.Observable

object TaglessUtils extends CommonUtils {

  object service {

    @tagless
    @service
    trait TaglessRPCService {

      @rpc(Protobuf) def notAllowed(b: Boolean): FS[C]

      @rpc(Avro) def unary(a: A): FS[C]

      @rpc(Protobuf) def empty(empty: Empty.type): FS[Empty.type]

      @rpc(Protobuf) def emptyParam(a: A): FS[Empty.type]

      @rpc(Protobuf) def emptyParamResponse(empty: Empty.type): FS[A]

      @rpc(Avro) def emptyAvro(empty: Empty.type): FS[Empty.type]

      @rpc(Avro) def emptyAvroParam(a: A): FS[Empty.type]

      @rpc(Avro) def emptyAvroParamResponse(empty: Empty.type): FS[A]

      @rpc(Protobuf)
      @stream[ResponseStreaming.type]
      def serverStreaming(b: B): FS[Observable[C]]

      @rpc(Protobuf)
      @stream[RequestStreaming.type]
      def clientStreaming(oa: Observable[A]): FS[D]

      @rpc(Avro)
      @stream[BidirectionalStreaming.type]
      def biStreaming(oe: Observable[E]): FS[Observable[E]]
    }

  }

  object client {

    @tagless
    trait MyRPCClient {
      def notAllowed(b: Boolean): FS[C]
      def empty: FS[Empty.type]
      def emptyParam(a: A): FS[Empty.type]
      def emptyParamResponse: FS[A]
      def emptyAvro: FS[Empty.type]
      def emptyAvroParam(a: A): FS[Empty.type]
      def emptyAvroParamResponse: FS[A]
      def u(x: Int, y: Int): FS[C]
      def ss(a: Int, b: Int): FS[List[C]]
      def cs(cList: List[C], bar: Int): FS[D]
      def bs(eList: List[E]): FS[E]
    }

  }

  object handlers {

    object server {

      import database._
      import service._
      import freestyle.rpc.protocol._

      class TaglessRPCServiceServerHandler[F[_]: Async] extends TaglessRPCService.Handler[F] {

        def notAllowed(b: Boolean): F[C] = c1.pure

        def empty(empty: Empty.type): F[Empty.type] = Empty.pure

        def emptyParam(a: A): F[Empty.type] = Empty.pure

        def emptyParamResponse(empty: Empty.type): F[A] = a4.pure

        def emptyAvro(empty: Empty.type): F[Empty.type] = Empty.pure

        def emptyAvroParam(a: A): F[Empty.type] = Empty.pure

        def emptyAvroParamResponse(empty: Empty.type): F[A] = a4.pure

        def unary(a: A): F[C] = c1.pure

        def serverStreaming(b: B): F[Observable[C]] = {
          debug(s"[SERVER] b -> $b")
          val obs = Observable.fromIterable(cList)
          obs.pure
        }

        def clientStreaming(oa: Observable[A]): F[D] =
          oa.foldLeftL(D(0)) {
              case (current, a) =>
                debug(s"[SERVER] Current -> $current / a -> $a")
                D(current.bar + a.x + a.y)
            }
            .to[F]

        def biStreaming(oe: Observable[E]): F[Observable[E]] =
          oe.flatMap { e: E =>
            save(e)

            Observable.fromIterable(eList)
          }.pure

        def save(e: E) = e // do something else with e?
      }

    }

    object client {

      import service._
      import freestyle.rpc.withtagless.TaglessUtils.client.MyRPCClient
      import freestyle.rpc.protocol._

      class TaglessRPCServiceClientHandler[F[_]: Async](
          implicit client: TaglessRPCService.Client[F])
          extends MyRPCClient.Handler[F] {

        override def notAllowed(b: Boolean): F[C] =
          client.notAllowed(b)

        override def empty: F[Empty.type] =
          client.empty(protocol.Empty)

        override def emptyParam(a: A): F[Empty.type] =
          client.emptyParam(a)

        override def emptyParamResponse: F[A] =
          client.emptyParamResponse(protocol.Empty)

        override def emptyAvro: F[Empty.type] =
          client.emptyAvro(protocol.Empty)

        override def emptyAvroParam(a: A): F[Empty.type] =
          client.emptyAvroParam(a)

        override def emptyAvroParamResponse: F[A] =
          client.emptyAvroParamResponse(protocol.Empty)

        override def u(x: Int, y: Int): F[C] =
          client.unary(A(x, y))

        override def ss(a: Int, b: Int): F[List[C]] =
          client
            .serverStreaming(B(A(a, a), A(b, b)))
            .zipWithIndex
            .map {
              case (c, i) =>
                debug(s"[CLIENT] Result #$i: $c")
                c
            }
            .toListL
            .to[F]

        override def cs(cList: List[C], bar: Int): F[D] =
          client.clientStreaming(Observable.fromIterable(cList.map(c => c.a)))

        override def bs(eList: List[E]): F[E] =
          client
            .biStreaming(Observable.fromIterable(eList))
            .firstL
            .to[F]

      }

    }

  }

  trait TaglessRuntime {

    import service._
    import handlers.server._
    import handlers.client._
    import freestyle.rpc.server._
    import freestyle.rpc.server.implicits._

    //////////////////////////////////
    // Server Runtime Configuration //
    //////////////////////////////////

    implicit def taglessRPCHandler[F[_]: Async]: TaglessRPCService.Handler[F] =
      new TaglessRPCServiceServerHandler[F]

    val grpcConfigs: List[GrpcConfig] = List(
      AddService(TaglessRPCService.bindService[ConcurrentMonad])
    )

    implicit val serverW: ServerW = createServerConf(grpcConfigs)

    //////////////////////////////////
    // Client Runtime Configuration //
    //////////////////////////////////

    implicit val taglessRPCServiceClient: TaglessRPCService.Client[ConcurrentMonad] =
      TaglessRPCService.client[ConcurrentMonad](createManagedChannelFor)

    implicit val taglessRPCServiceClientHandler: TaglessRPCServiceClientHandler[ConcurrentMonad] =
      new TaglessRPCServiceClientHandler[ConcurrentMonad]

  }

  object implicits extends TaglessRuntime

}
