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

package higherkindness.mu.http

import cats.ApplicativeError
import cats.effect._
import cats.implicits._
import fs2.{RaiseThrowable, Stream}
import io.grpc.Status.Code._
import org.typelevel.jawn.ParseException
import io.circe._
import io.circe.jawn.CirceSupportParser.facade
import io.circe.syntax._
import io.grpc.{Status => _, _}
import jawnfs2._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.Status.Ok
import scala.util.control.NoStackTrace

object implicits {

  implicit class MessageOps[F[_]](private val message: Message[F]) extends AnyVal {

    def jsonBodyAsStream[A](
        implicit decoder: Decoder[A],
        F: ApplicativeError[F, Throwable]): Stream[F, A] =
      message.body.chunks.parseJsonStream.map(_.as[A]).rethrow
  }

  implicit class RequestOps[F[_]](private val request: Request[F]) {

    def asStream[A](implicit decoder: Decoder[A], F: ApplicativeError[F, Throwable]): Stream[F, A] =
      request
        .jsonBodyAsStream[A]
        .adaptError { // mimic behavior of MessageOps.as[T] in handling of parsing errors
          case ex: ParseException =>
            MalformedMessageBodyFailure(ex.getMessage, Some(ex)) // will return 400 instead of 500
        }
  }

  implicit class ResponseOps[F[_]](private val response: Response[F]) {

    implicit def EitherDecoder[A, B](
        implicit a: Decoder[A],
        b: Decoder[B]): Decoder[Either[A, B]] = {
      val l: Decoder[Either[A, B]] = a.map(Left.apply)
      val r: Decoder[Either[A, B]] = b.map(Right.apply)
      l or r
    }

    implicit private val throwableDecoder: Decoder[Throwable] =
      Decoder.decodeTuple2[String, String].map {
        case (cls, msg) =>
          Class
            .forName(cls)
            .getConstructor(classOf[String])
            .newInstance(msg)
            .asInstanceOf[Throwable]
      }

    def asStream[A](
        implicit decoder: Decoder[A],
        F: ApplicativeError[F, Throwable],
        R: RaiseThrowable[F]): Stream[F, A] =
      if (response.status.code != Ok.code) Stream.raiseError(ResponseError(response.status))
      else response.jsonBodyAsStream[Either[Throwable, A]].rethrow
  }

  implicit class Fs2StreamOps[F[_], A](private val stream: Stream[F, A]) {

    implicit def EitherEncoder[A, B](implicit ea: Encoder[A], eb: Encoder[B]): Encoder[Either[A, B]] =
      new Encoder[Either[A, B]] {
        final def apply(a: Either[A, B]): Json = a match {
          case Left(a)  => a.asJson
          case Right(b) => b.asJson
        }
      }

    implicit val throwableEncoder: Encoder[Throwable] = new Encoder[Throwable] {
      def apply(ex: Throwable): Json = (ex.getClass.getName, ex.getMessage).asJson
    }

    def asJsonEither(implicit encoder: Encoder[A]): Stream[F, Json] = stream.attempt.map(_.asJson)
  }

  implicit class FResponseOps[F[_]: Sync](private val response: F[Response[F]])
      extends Http4sDsl[F] {

    def adaptErrors: F[Response[F]] = response.handleErrorWith {
      case se: StatusException         => errorFromStatus(se.getStatus, se.getMessage)
      case sre: StatusRuntimeException => errorFromStatus(sre.getStatus, sre.getMessage)
      case other: Throwable            => InternalServerError(other.getMessage)
    }

    private def errorFromStatus(status: io.grpc.Status, message: String): F[Response[F]] =
      status.getCode match {
        case INVALID_ARGUMENT  => BadRequest(message)
        case UNAUTHENTICATED   => Forbidden(message)
        case PERMISSION_DENIED => Forbidden(message)
        case NOT_FOUND         => NotFound(message)
        case UNAVAILABLE       => ServiceUnavailable(message)
        case _                 => InternalServerError(message)
      }
  }

  def handleResponseError[F[_]: Sync](errorResponse: Response[F]): F[Throwable] =
    errorResponse.bodyAsText.compile.foldMonoid.map(body =>
      ResponseError(errorResponse.status, Some(body).filter(_.nonEmpty)))
}

final case class ResponseError(status: Status, msg: Option[String] = None)
    extends RuntimeException(status + msg.fold("")(": " + _))
    with NoStackTrace
