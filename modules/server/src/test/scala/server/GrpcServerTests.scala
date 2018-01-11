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
package server

import java.util.concurrent.TimeUnit

import cats.Monad
import cats.syntax.apply._
import freestyle.rpc.common.{ConcurrentMonad, SC}
import io.grpc.{Server, ServerServiceDefinition}

class GrpcServerTests extends RpcServerTestSuite {

  import implicits._

  "GrpcServer" should {

    type Result = (
        Boolean,
        Server,
        Int,
        List[ServerServiceDefinition],
        List[ServerServiceDefinition],
        List[ServerServiceDefinition],
        Server,
        Server,
        Boolean,
        Boolean,
        Boolean,
        Unit)

    "behaves as expected" in {

      def program[F[_]: Monad](APP: GrpcServer[F]): F[Result] = {

        import APP._

        val a = isShutdown
        val b = start()
        val c = getPort
        val d = getServices
        val e = getImmutableServices
        val f = getMutableServices
        val g = shutdown()
        val h = shutdownNow()
        val i = isShutdown
        val j = isTerminated
        val k = awaitTerminationTimeout(timeout, timeoutUnit)
        val l = awaitTermination()

        (a, b, c, d, e, f, g, h, i, j, k, l).tupled
      }

      program[ConcurrentMonad](grpcServerHandlerTests).unsafeRunSync() shouldBe ((
        b,
        serverCopyMock,
        SC.port,
        serviceList,
        immutableServiceList,
        mutableServiceList,
        serverCopyMock,
        serverCopyMock,
        b,
        b,
        b,
        unit): Result)

      (serverMock.start _: () => Server).verify().once()
      (serverMock.getPort _: () => Int).verify().once()
      (serverMock.getServices _: () => java.util.List[ServerServiceDefinition]).verify().once()
      (serverMock.getImmutableServices _: () => java.util.List[ServerServiceDefinition])
        .verify()
        .once()
      (serverMock.getMutableServices _: () => java.util.List[ServerServiceDefinition])
        .verify()
        .once()
      (serverMock.shutdown _: () => Server).verify().once()
      (serverMock.shutdownNow _: () => Server).verify().once()
      (serverMock.isShutdown _: () => Boolean).verify().twice()
      (serverMock.isTerminated _: () => Boolean).verify().once()
      (serverMock.awaitTermination(_: Long, _: TimeUnit)).verify(timeout, timeoutUnit).once()
      (serverMock.awaitTermination _: () => Unit).verify().once()
    }

  }

}
