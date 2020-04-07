/*
 * Copyright 2017-2020 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc.internal.server

import fs2.Stream
import cats.data.Kleisli
import cats.effect.ConcurrentEffect
import io.grpc.{Metadata, MethodDescriptor, ServerCallHandler}
import org.lyranthe.fs2_grpc.java_runtime.server.{
  Fs2ServerCallHandler,
  GzipCompressor,
  ServerCallOptions
}
import higherkindness.mu.rpc.protocol.{CompressionType, Gzip}
import natchez.{EntryPoint, Span}

object fs2Calls {

  private def serverCallOptions(compressionType: CompressionType): ServerCallOptions =
    compressionType match {
      case Gzip => ServerCallOptions.default.withServerCompressor(Some(GzipCompressor))
      case _    => ServerCallOptions.default
    }

  def unaryMethod[F[_]: ConcurrentEffect, Req, Res](
      f: (Req, Metadata) => F[Res],
      compressionType: CompressionType
  ): ServerCallHandler[Req, Res] =
    Fs2ServerCallHandler[F].unaryToUnaryCall[Req, Res](
      f,
      serverCallOptions(compressionType)
    )

  def clientStreamingMethod[F[_]: ConcurrentEffect, Req, Res](
      f: (Stream[F, Req], Metadata) => F[Res],
      compressionType: CompressionType
  ): ServerCallHandler[Req, Res] =
    Fs2ServerCallHandler[F].streamingToUnaryCall[Req, Res](
      f,
      serverCallOptions(compressionType)
    )

  def serverStreamingMethod[F[_]: ConcurrentEffect, Req, Res](
      f: (Req, Metadata) => Stream[F, Res],
      compressionType: CompressionType
  ): ServerCallHandler[Req, Res] =
    Fs2ServerCallHandler[F].unaryToStreamingCall[Req, Res](
      f,
      serverCallOptions(compressionType)
    )

  def bidiStreamingMethod[F[_]: ConcurrentEffect, Req, Res](
      f: (Stream[F, Req], Metadata) => Stream[F, Res],
      compressionType: CompressionType
  ): ServerCallHandler[Req, Res] =
    Fs2ServerCallHandler[F].streamingToStreamingCall[Req, Res](
      f,
      serverCallOptions(compressionType)
    )

  def tracingClientStreamingMethod[F[_]: ConcurrentEffect, Req, Res](
      f: Stream[Kleisli[F, Span[F], *], Req] => Kleisli[F, Span[F], Res],
      entrypoint: EntryPoint[F],
      descriptor: MethodDescriptor[Req, Res],
      compressionType: CompressionType
  ): ServerCallHandler[Req, Res] =
    clientStreamingMethod[F, Req, Res](
      { (req: Stream[F, Req], metadata: Metadata) =>
        val kernel  = extractTracingKernel(metadata)
        val streamK = req.translateInterruptible(Kleisli.liftK[F, Span[F]])
        entrypoint.continueOrElseRoot(descriptor.getFullMethodName(), kernel).use[F, Res] { span =>
          f(streamK).run(span)
        }
      },
      compressionType
    )

}
