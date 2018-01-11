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

import cats.{Applicative, Apply}
import cats.syntax.apply._
import cats.syntax.functor._
import freestyle.rpc.async.RPCAsyncImplicits
import freestyle.rpc.server.handlers.GrpcServerHandler

trait ServerImplicits {

  implicit def grpcServerHandler[F[_]: Applicative](implicit SW: ServerW): GrpcServer[F] =
    GrpcServerHandler[F].mapK[F](new GrpcKInterpreter[F](SW.server))

}

trait Helpers {

  def server[F[_]: Apply](implicit S: GrpcServer[F]): F[Unit] =
    S.start() *> S.awaitTermination().void

}

object implicits extends RPCAsyncImplicits with Helpers with ServerImplicits
