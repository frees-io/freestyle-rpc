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
package internal
package client

import cats.effect.{Async, LiftIO}
import freestyle.async.guava.implicits._
import freestyle.async.catsEffect.implicits._
import io.grpc.{CallOptions, Channel, MethodDescriptor}
import io.grpc.stub.{ClientCalls, StreamObserver}
import monix.execution.Scheduler
import monix.reactive.Observable

object monixCalls {

  import freestyle.rpc.internal.converters._

  def unary[F[_]: Async, Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions)(implicit S: Scheduler): F[Res] =
    listenableFuture2Async(
      ClientCalls
        .futureUnaryCall(channel.newCall(descriptor, options), request))

  def serverStreaming[Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions): Observable[Res] =
    Observable
      .fromReactivePublisher(createPublisher(request, descriptor, channel, options))

  def clientStreaming[F[_]: LiftIO, Req, Res](
      input: Observable[Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions)(implicit S: Scheduler, L: LiftTask[F]): F[Res] =
    L.liftTask {
      input
        .liftByOperator(
          StreamObserver2MonixOperator(
            (outputObserver: StreamObserver[Res]) =>
              ClientCalls.asyncClientStreamingCall(
                channel.newCall(descriptor, options),
                outputObserver
            )
          )
        )
        .firstL
    }

  def bidiStreaming[Req, Res](
      input: Observable[Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions): Observable[Res] =
    input.liftByOperator(
      StreamObserver2MonixOperator(
        (outputObserver: StreamObserver[Res]) =>
          ClientCalls.asyncBidiStreamingCall(
            channel.newCall(descriptor, options),
            outputObserver
        ))
    )
}
