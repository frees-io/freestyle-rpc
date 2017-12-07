/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle.tagless.rpc

import freestyle.free._
import org.scalatest._
import freestyle.free.rpc.server.GrpcServerApp
import freestyle.tagless.rpc.TaglessUtils.clientProgram.MyRPCClient
import freestyle.free.rpc.protocol.Empty
import freestyle.free.rpc.RpcBaseTestSuite

class FreesRPCTests extends RpcBaseTestSuite with BeforeAndAfterAll {

  import cats.implicits._
  import freestyle.tagless.rpc.TaglessUtils.service._
  import freestyle.tagless.rpc.TaglessUtils.database._
  import freestyle.tagless.rpc.TaglessUtils.helpers._
  import freestyle.tagless.rpc.TaglessUtils.implicits._

  override protected def beforeAll(): Unit = {
    import freestyle.free.rpc.server.implicits._
    serverStart[GrpcServerApp.Op].runF
  }

  override protected def afterAll(): Unit = {
    import freestyle.free.rpc.server.implicits._
    serverStop[GrpcServerApp.Op].runF
  }

  "frees-rpc server" should {

    import freestyle.free.rpc.server.implicits._

    "allow to startup a server and check if it's alive" in {

      def check[M[_]](implicit APP: GrpcServerApp[M]): FreeS[M, Boolean] =
        APP.server.isShutdown

      check[GrpcServerApp.Op].runF shouldBe false

    }

    "allow to get the port where it's running" in {

      def check[M[_]](implicit APP: GrpcServerApp[M]): FreeS[M, Int] =
        APP.server.getPort

      check[GrpcServerApp.Op].runF shouldBe port

    }

  }

  "frees-rpc client" should {

    import freestyle.free.rpc.client.implicits._

    "be able to run unary services" in {

      def clientProgram[M[_]](implicit APP: MyRPCClient[M]): FreeS[M, C] =
        APP.u(a1.x, a1.y)

      clientProgram[MyRPCClient.Op].runF shouldBe c1

    }

    "be able to run server streaming services" in {

      def clientProgram[M[_]](implicit APP: MyRPCClient[M]): FreeS[M, List[C]] =
        APP.ss(a2.x, a2.y)

      clientProgram[MyRPCClient.Op].runF shouldBe cList

    }

    "be able to run client streaming services" in {

      def clientProgram[M[_]](implicit APP: MyRPCClient[M]): FreeS[M, D] =
        APP.cs(cList, i)

      clientProgram[MyRPCClient.Op].runF shouldBe dResult
    }

    "be able to run client bidirectional streaming services" in {

      def clientProgram[M[_]](implicit APP: MyRPCClient[M]): FreeS[M, E] =
        APP.bs(eList)

      clientProgram[MyRPCClient.Op].runF shouldBe e1

    }

    "be able to run rpc services monadically" in {

      def clientProgram[M[_]](implicit APP: MyRPCClient[M]): FreeS[M, (C, List[C], D, E)] = {
        for {
          w <- APP.u(a1.x, a1.y)
          x <- APP.ss(a2.x, a2.y)
          y <- APP.cs(cList, i)
          z <- APP.bs(eList)
        } yield (w, x, y, z)
      }

      clientProgram[MyRPCClient.Op].runF shouldBe ((c1, cList, dResult, e1))

    }

    "#67 issue - booleans as request are not allowed" ignore {

      def clientProgram[M[_]](implicit APP: MyRPCClient[M]): FreeS[M, C] =
        APP.notAllowed(true)

      clientProgram[MyRPCClient.Op].runF shouldBe c1

    }

    "be able to invoke services with empty requests" in {

      def clientProgram[M[_]](implicit APP: MyRPCClient[M]): FreeS[M, Empty.type] =
        APP.empty

      clientProgram[MyRPCClient.Op].runF shouldBe Empty

    }

    "#71 issue - empty for avro" in {

      def clientProgram[M[_]](implicit APP: MyRPCClient[M]): FreeS[M, Empty.type] =
        APP.emptyAvro

      clientProgram[MyRPCClient.Op].runF shouldBe Empty
    }

    "#71 issue - empty response with one param for avro" in {

      def clientProgram[M[_]](implicit APP: MyRPCClient[M]): FreeS[M, Empty.type] =
        APP.emptyAvroParam(a4)

      clientProgram[MyRPCClient.Op].runF shouldBe Empty
    }

    "#71 issue - response with empty params for avro" in {

      def clientProgram[M[_]](implicit APP: MyRPCClient[M]): FreeS[M, A] =
        APP.emptyAvroParamResponse

      clientProgram[MyRPCClient.Op].runF shouldBe a4

    }

    "#71 issue - empty response with one param for proto" in {

      def clientProgram[M[_]](implicit APP: MyRPCClient[M]): FreeS[M, Empty.type] =
        APP.emptyParam(a4)

      clientProgram[MyRPCClient.Op].runF shouldBe Empty
    }

    "#71 issue - response with empty params for proto" in {

      def clientProgram[M[_]](implicit APP: MyRPCClient[M]): FreeS[M, A] =
        APP.emptyParamResponse

      clientProgram[MyRPCClient.Op].runF shouldBe a4

    }

  }

}