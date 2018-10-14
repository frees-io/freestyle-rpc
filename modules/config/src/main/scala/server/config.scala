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
package server

import cats.Functor

package object config {

  def BuildServerFromConfig[F[_]: Functor](portPath: String, configList: List[GrpcConfig] = Nil)(
      implicit SC: ServerConfig[F]): F[GrpcServer[F]] =
    SC.buildServer(portPath, configList)

  def BuildNettyServerFromConfig[F[_]: Functor](
      portPath: String,
      configList: List[GrpcConfig] = Nil)(implicit SC: ServerConfig[F]): F[GrpcServer[F]] =
    SC.buildNettyServer(portPath, configList)

}
