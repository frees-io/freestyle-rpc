/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc.http

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.syntax._
import higherkindness.mu.http.implicits._
import fs2.interop.reactivestreams._
import monix.reactive.Observable
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class UnaryGreeterRestService[F[_]: Sync](
    implicit handler: UnaryGreeter[F],
    decoderHelloRequest: io.circe.Decoder[HelloRequest],
    encoderHelloResponse: io.circe.Encoder[HelloResponse])
    extends Http4sDsl[F] {

  import higherkindness.mu.rpc.protocol.Empty

  private implicit val requestDecoder: EntityDecoder[F, HelloRequest] = jsonOf[F, HelloRequest]

  def service: HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root / "getHello" => Ok(handler.getHello(Empty).map(_.asJson))

    case msg @ POST -> Root / "sayHello" =>
      for {
        request  <- msg.as[HelloRequest]
        response <- Ok(handler.sayHello(request).map(_.asJson)).adaptErrors
      } yield response
  }
}

class Fs2GreeterRestService[F[_]: Sync](
    implicit handler: Fs2Greeter[F],
    decoderHelloRequest: io.circe.Decoder[HelloRequest],
    encoderHelloResponse: io.circe.Encoder[HelloResponse])
    extends Http4sDsl[F] {

  private implicit val requestDecoder: EntityDecoder[F, HelloRequest] = jsonOf[F, HelloRequest]

  def service: HttpRoutes[F] = HttpRoutes.of[F] {

    case msg @ POST -> Root / "sayHellos" =>
      val requests = msg.asStream[HelloRequest]
      Ok(handler.sayHellos(requests).map(_.asJson))

    case msg @ POST -> Root / "sayHelloAll" =>
      for {
        request   <- msg.as[HelloRequest]
        responses <- Ok(handler.sayHelloAll(request).asJsonEither)
      } yield responses

    case msg @ POST -> Root / "sayHellosAll" =>
      val requests = msg.asStream[HelloRequest]
      Ok(handler.sayHellosAll(requests).asJsonEither)
  }
}

class MonixGreeterRestService[F[_]: ConcurrentEffect](
    implicit handler: MonixGreeter[F],
    ec: scala.concurrent.ExecutionContext,
    decoderHelloRequest: io.circe.Decoder[HelloRequest],
    encoderHelloResponse: io.circe.Encoder[HelloResponse])
    extends Http4sDsl[F] {

  private implicit val requestDecoder: EntityDecoder[F, HelloRequest] = jsonOf[F, HelloRequest]
  implicit val scheduler: monix.execution.Scheduler                   = monix.execution.Scheduler(ec)

  def service: HttpRoutes[F] = HttpRoutes.of[F] {

    case msg @ POST -> Root / "sayHellos" =>
      val requests = msg.asStream[HelloRequest]
      Ok(
        handler
          .sayHellos(Observable.fromReactivePublisher(requests.toUnicastPublisher))
          .map(_.asJson))

    case msg @ POST -> Root / "sayHelloAll" =>
      for {
        request   <- msg.as[HelloRequest]
        responses <- Ok(handler.sayHelloAll(request).toReactivePublisher.toStream.asJsonEither)
      } yield responses

    case msg @ POST -> Root / "sayHellosAll" =>
      val requests = msg.asStream[HelloRequest]
      Ok(
        handler
          .sayHellosAll(Observable.fromReactivePublisher(requests.toUnicastPublisher))
          .toReactivePublisher
          .toStream
          .asJsonEither)
  }
}
