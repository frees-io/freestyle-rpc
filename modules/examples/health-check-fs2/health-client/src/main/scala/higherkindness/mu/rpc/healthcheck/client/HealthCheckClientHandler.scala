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

package higherkindness.mu.rpc.healthcheck.client

import cats.effect.{Async, Resource, Sync}
import higherkindness.mu.rpc.healthcheck.service.HealthCheckServiceFS2
import cats.implicits._
import cats.syntax._
import higherkindness.mu.rpc.healthcheck._
import higherkindness.mu.rpc.protocol.Empty
import org.log4s.Logger

class HealthCheckClientHandler[F[_]: Async](client: Resource[F, HealthCheckServiceFS2[F]])(
    implicit logger: Logger) {

  def updatingWatching(name: String) =
    for {
      _      <- client.use(_.setStatus(HealthStatus(HealthCheck(name), ServerStatus("SERVING"))))
      status <- client.use(_.check(HealthCheck(name)))
      _      <- Async[F].delay(logger.info("Status of " + name + " service update to" + status))
    } yield ()

  def watching(name: String) = client.use(
    _.watch(HealthCheck(name))
      .evalMap(hs =>
        Async[F].delay(logger.info(
          "Service " + hs.hc.nameService + " updated to status " + hs.status.status + ". ")))
      .compile
      .drain
  )

  def settingAndCheck(name: String, status: ServerStatus) =
    for {
      _     <- Sync[F].delay(logger.info("/////////////////////////////////UNARY"))
      _     <- Sync[F].delay(logger.info("UNARY: Is there some server named " + name.toUpperCase + "?"))
      known <- client.use(_.check(HealthCheck(name)))
      _     <- Sync[F].delay(logger.info("UNARY: Actually the status is " + known.status))
      _ <- Sync[F].delay(
        logger.info(
          "UNARY: Setting " + name.toUpperCase + " service with " + status.status + " status"))
      wentNiceSet <- client.use(_.setStatus(HealthStatus(HealthCheck(name), status)))
      _ <- Sync[F].delay(
        logger.info("UNARY: Added status: " + status.status + " to service: " + name.toUpperCase))
      status <- client.use(_.check(HealthCheck(name)))
      _ <- Sync[F].delay(
        logger.info(
          "UNARY: Checked the status of " + name.toUpperCase + ". Obtained: " + status.status))
      wentNiceClean <- client.use(_.clearStatus(HealthCheck(name)))
      _ <- Sync[F].delay(
        logger.info(
          "UNARY: Cleaned " + name.toUpperCase + " status. Went ok?: " + wentNiceClean.ok))
      unknown <- client.use(_.check(HealthCheck(name)))
      _ <- Sync[F].delay(
        logger.info("UNARY: Current status of " + name.toUpperCase + ": " + unknown.status))
    } yield ()

  def settingAndFullClean(namesAndStatuses: List[(String, ServerStatus)]) =
    for {
      _ <- Sync[F].delay(logger.info("/////////////////////////////////UNARY ALL"))
      _ <- Sync[F].delay(logger.info("UNARY ALL: Setting services: " + namesAndStatuses))
      wentNiceSet <- namesAndStatuses.traverse(l =>
        client.use(_.setStatus(HealthStatus(HealthCheck(l._1), l._2))))
      allStatuses1 <- client.use(_.checkAll(Empty))
      _ <- Sync[F].delay(
        logger.info("UNARY ALL: All statuses are: " + allStatuses1.all.mkString("\n")))
      _ <- Sync[F].delay(
        logger.info(
          "UNARY ALL: Went it ok in all cases? " + !wentNiceSet.contains(WentNice(false))))
      wentNiceClear <- client.use(_.cleanAll(Empty))
      _             <- Sync[F].delay(logger.info("UNARY ALL: Went the cleaning part nice? " + wentNiceClear.ok))
      allStatuses2  <- client.use(_.checkAll(Empty))
      _ <- Sync[F].delay(
        logger.info("UNARY ALL: All statuses are: " + allStatuses2.all.mkString("\n")))

    } yield ()

}
