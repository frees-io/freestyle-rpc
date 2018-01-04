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

import cats.{~>, Id}
import io.grpc.{Server, ServerServiceDefinition}

import scala.collection.JavaConverters._
import scala.concurrent.duration.TimeUnit
import scala.concurrent.Future

trait RpcServerTestSuite extends RpcBaseTestSuite {

  trait DummyData {

    val serverMock: Server                                  = stub[Server]
    val serverCopyMock: Server                              = stub[Server]
    val timeout: Long                                       = 1l
    val timeoutUnit: TimeUnit                               = TimeUnit.MINUTES
    val b: Boolean                                          = true
    val unit: Unit                                          = ()
    val sd1: ServerServiceDefinition                        = ServerServiceDefinition.builder("s1").build()
    val sd2: ServerServiceDefinition                        = ServerServiceDefinition.builder("s2").build()
    val serviceList: List[ServerServiceDefinition]          = List(sd1, sd2)
    val immutableServiceList: List[ServerServiceDefinition] = List(sd1)
    val mutableServiceList: List[ServerServiceDefinition]   = List(sd2)

    (serverMock.start _: () => Server).when().returns(serverCopyMock)
    (serverMock.getPort _: () => Int).when().returns(port)
    (serverMock.getServices _: () => java.util.List[ServerServiceDefinition])
      .when()
      .returns(serviceList.asJava)
    (serverMock.getImmutableServices _: () => java.util.List[ServerServiceDefinition])
      .when()
      .returns(immutableServiceList.asJava)
    (serverMock.getMutableServices _: () => java.util.List[ServerServiceDefinition])
      .when()
      .returns(mutableServiceList.asJava)
    (serverMock.shutdown _: () => Server).when().returns(serverCopyMock)
    (serverMock.shutdownNow _: () => Server).when().returns(serverCopyMock)
    (serverMock.isShutdown _: () => Boolean).when().returns(b)
    (serverMock.isTerminated _: () => Boolean).when().returns(b)
    (serverMock.awaitTermination(_: Long, _: TimeUnit)).when(timeout, timeoutUnit).returns(b)
    (serverMock.awaitTermination _: () => Unit).when().returns(unit)
  }

  object implicits extends Helpers with DummyData {

    def idApply[A](fa: GrpcServer.Op[A]): Id[A] = {
      import GrpcServer._
      fa match {
        case StartOp()                       => serverMock.start()
        case GetPortOp()                     => serverMock.getPort
        case GetServicesOp()                 => serverMock.getServices.asScala.toList
        case GetImmutableServicesOp()        => serverMock.getImmutableServices.asScala.toList
        case GetMutableServicesOp()          => serverMock.getMutableServices.asScala.toList
        case ShutdownOp()                    => serverMock.shutdown()
        case ShutdownNowOp()                 => serverMock.shutdownNow()
        case IsShutdownOp()                  => serverMock.isShutdown
        case IsTerminatedOp()                => serverMock.isTerminated
        case AwaitTerminationTimeoutOp(t, u) => serverMock.awaitTermination(t, u)
        case AwaitTerminationOp()            => serverMock.awaitTermination()
      }
    }

    implicit val grpcServerHandlerId: GrpcServer.Op ~> Id =
      new (GrpcServer.Op ~> Id) {
        override def apply[A](fa: GrpcServer.Op[A]): Id[A] = idApply(fa)
      }

    implicit val grpcServerHandlerFuture: GrpcServer.Op ~> Future =
      new (GrpcServer.Op ~> Future) {
        override def apply[A](fa: GrpcServer.Op[A]): Future[A] = Future.successful(idApply(fa))
      }
  }
}
