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

import cats.Id
import cats.data.Kleisli
import cats.implicits._
import freestyle.free._
import freestyle.free.implicits._
import freestyle.free.config.implicits._
import io.grpc.Server

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class ServerConfigTests extends RpcServerTestSuite {

  import implicits._

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  "ServerConfig.Op" should {

    "load the port specified in the config file" in {

      val serverConf: ServerW =
        BuildServerFromConfig[ServerConfig.Op]("rpc.server.port")
          .interpret[Future]
          .await

      serverConf.port shouldBe port
    }

    "load the default port when the config port path is not found" in {

      val serverConf: ServerW =
        BuildServerFromConfig[ServerConfig.Op]("rpc.wrong.path")
          .interpret[Future]
          .await

      serverConf.port shouldBe defaultPort
    }

  }

  "GrpcConfigInterpreter" should {

    "work as expected" in {

      val kInterpreter =
        new GrpcKInterpreter[Id](serverMock).apply(Kleisli[Id, Server, Int](s => s.getPort))

      kInterpreter shouldBe port
    }

  }

  "SServerBuilder" should {

    "work as expected" in {

      val configList: List[GrpcConfig] = List(AddService(sd1))
      val server: Server               = SServerBuilder(port, configList).build

      server.getServices.asScala.toList shouldBe List(sd1)
    }
  }
}
