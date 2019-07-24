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

package higherkindness.mu.rpc.healthcheck

import higherkindness.mu.rpc.protocol.{service, Empty, Protobuf}
import monix.reactive.Observable

object service {

  case class HealthCheck(nameService: String)
  case class HealthStatus(hc: HealthCheck, status: ServerStatus)
  case class WentNice(ok: Boolean)

  case class AllStatus(all: List[(HealthCheck, ServerStatus)])
  @service(Protobuf)
  trait HealthCheckService[F[_]] {

    def check(service: HealthCheck): F[ServerStatus]
    def setStatus(newStatus: HealthStatus): F[WentNice]
    def clearStatus(service: HealthCheck): F[WentNice]

    def checkAll(empty: Empty.type): F[AllStatus]
    def cleanAll(empty: Empty.type): F[WentNice]

    def watch(service: HealthCheck): Observable[HealthStatus]
  }
}
