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

package mu.rpc
package client

import cats.effect.{Effect, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._

import io.grpc._

class ManagedChannelInterpreter[F[_]](
    initConfig: ChannelFor,
    configList: List[ManagedChannelConfig])(implicit F: Sync[F]) {

  def apply[A](fa: ManagedChannelOps[F, A]): F[A] =
    fa(new ManagedChannelInterpreter(initConfig, configList).build)

  def build[T <: ManagedChannelBuilder[T]]: F[ManagedChannel] = {

    val builder: F[T] = initConfig match {
      case ChannelForAddress(name, port) =>
        F.delay(ManagedChannelBuilder.forAddress(name, port).asInstanceOf[T])
      case ChannelForTarget(target) =>
        F.delay(ManagedChannelBuilder.forTarget(target).asInstanceOf[T])
      case e =>
        F.raiseError(new IllegalArgumentException(s"ManagedChannel not supported for $e"))
    }

    for {
      b          <- builder
      configured <- F.delay(configList.foldLeft(b)(configureChannel))
      built      <- F.delay(configured.build())
    } yield built
  }

  def unsafeBuild[T <: ManagedChannelBuilder[T]](implicit E: Effect[F]): ManagedChannel =
    E.toIO(new ManagedChannelInterpreter(initConfig, configList).build).unsafeRunSync()

}
