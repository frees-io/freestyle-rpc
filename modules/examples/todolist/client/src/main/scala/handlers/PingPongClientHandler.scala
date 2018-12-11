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

package examples.todolist.client
package handlers

import cats.Monad.ops._
import cats.effect.{Resource, Sync}
import examples.todolist.client.clients.PingPongClient
import examples.todolist.protocol.Protocols.PingPongService
import mu.rpc.protocol.Empty
import org.log4s._

class PingPongClientHandler[F[_]: Sync](implicit client: Resource[F, PingPongService.Client[F]])
    extends PingPongClient[F] {

  val logger: Logger = getLogger

  override def ping(): F[Unit] =
    client
      .use(_.ping(Empty))
      .map(p => logger.info(s"Pong received with timestamp: ${p.time}"))

}
