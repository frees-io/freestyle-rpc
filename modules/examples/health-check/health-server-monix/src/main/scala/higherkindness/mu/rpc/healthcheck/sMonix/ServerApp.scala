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

package higherkindness.mu.rpc.healthcheck.sMonix

import cats.effect.IO
import higherkindness.mu.rpc.server.{AddService, GrpcConfig, GrpcServer}
import gserver.implicits._
import cats.instances.list._
import cats.syntax.traverse._
import higherkindness.mu.rpc.healthcheck.monix.handler.HealthServiceMonix
import higherkindness.mu.rpc.healthcheck.monix.serviceMonix.HealthCheckServiceMonix

object ServerApp {

  def main(args: Array[String]): Unit = {
    val healthCheck: IO[HealthCheckServiceMonix[IO]] = HealthServiceMonix.buildInstance[IO]

    def grpcConfigs(implicit HC: HealthCheckServiceMonix[IO]): IO[List[GrpcConfig]] =
      List(
        HealthCheckServiceMonix.bindService[IO]
      ).sequence.map(_.map(AddService))

    val runServer = for {
      health <- healthCheck
      config <- grpcConfigs(health)
      server <- GrpcServer.default[IO](50051, config)
      _      <- GrpcServer.server[IO](server)
    } yield ()

    runServer.unsafeRunSync
  }
}
