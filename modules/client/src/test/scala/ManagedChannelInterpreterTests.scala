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
package client

import cats.data.Kleisli
import freestyle.rpc.common.SC
import io.grpc.ManagedChannel

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

abstract class ManagedChannelInterpreterTests extends RpcClientTestSuite {

  import implicits._

  "ManagedChannelInterpreter" should {

    "build a io.grpc.ManagedChannel based on the specified configuration, for an address" in {

      val channelFor: ChannelFor = ChannelForAddress(SC.host, SC.port)

      val channelConfigList: List[ManagedChannelConfig] = List(UsePlaintext(true))

      val managedChannelInterpreter =
        new ManagedChannelInterpreter[Future](channelFor, channelConfigList)

      val mc: ManagedChannel = managedChannelInterpreter.build(channelFor, channelConfigList)

      mc shouldBe an[ManagedChannel]
    }

    "build a io.grpc.ManagedChannel based on the specified configuration, for an target" in {

      val channelFor: ChannelFor = ChannelForTarget(SC.host)

      val channelConfigList: List[ManagedChannelConfig] = List(UsePlaintext(true))

      val managedChannelInterpreter =
        new ManagedChannelInterpreter[Future](channelFor, channelConfigList)

      val mc: ManagedChannel = managedChannelInterpreter.build(channelFor, channelConfigList)

      mc shouldBe an[ManagedChannel]
    }

    "apply should work as expected" in {

      val channelFor: ChannelFor = ChannelForTarget(SC.host)

      val channelConfigList: List[ManagedChannelConfig] = List(UsePlaintext(true))

      val managedChannelInterpreter =
        new ManagedChannelInterpreter[Future](channelFor, channelConfigList)

      val kleisli: ManagedChannelOps[Future, String] =
        Kleisli[Future, ManagedChannel, String]((_: ManagedChannel) => Future.successful(foo))

      Await.result(managedChannelInterpreter[String](kleisli), Duration.Inf) shouldBe foo
    }

    "build a io.grpc.ManagedChannel based on any configuration combination" in {

      val channelFor: ChannelFor = ChannelForAddress(SC.host, SC.port)

      val channelConfigList: List[ManagedChannelConfig] = managedChannelConfigAllList

      val managedChannelInterpreter =
        new ManagedChannelInterpreter[Future](channelFor, channelConfigList)

      val mc: ManagedChannel = managedChannelInterpreter.build(channelFor, channelConfigList)

      mc shouldBe an[ManagedChannel]
    }

    "throw an exception when configuration is not recognized" in {

      val channelFor: ChannelFor = ChannelForAddress(SC.host, SC.port)

      case object Unexpected extends ManagedChannelConfig
      val channelConfigList: List[ManagedChannelConfig] = List(Unexpected)

      val managedChannelInterpreter =
        new ManagedChannelInterpreter[Future](channelFor, channelConfigList)

      an[MatchError] shouldBe thrownBy(
        managedChannelInterpreter.build(channelFor, channelConfigList))
    }

    "throw an exception when ChannelFor is not recognized" in {

      val channelFor: ChannelFor = ChannelForPort(SC.port)

      val managedChannelInterpreter =
        new ManagedChannelInterpreter[Future](channelFor, Nil)

      an[IllegalArgumentException] shouldBe thrownBy(
        managedChannelInterpreter.build(channelFor, Nil))
    }
  }
}
