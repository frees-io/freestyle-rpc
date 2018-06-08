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
package netty

import cats.syntax.either._

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import freestyle.rpc.common.SC
import io.grpc.ManagedChannel
import io.grpc.internal.GrpcUtil
import io.grpc.netty.{GrpcSslContexts, NegotiationType, NettyChannelBuilder}
import io.netty.channel.ChannelOption
import io.netty.channel.local.LocalChannel
import io.netty.channel.nio.NioEventLoopGroup

class ManagedChannelInterpreterNettyTests extends ManagedChannelInterpreterTests {

  import implicits._

  "NettyChannelInterpreter" should {

    "build an io.grpc.ManagedChannel based on the specified configuration, for a Socket Address" in {

      val channelFor: ChannelFor = ChannelForSocketAddress(new InetSocketAddress(SC.host, 45455))

      val channelConfigList =
        List(UsePlaintext().asLeft[NettyChannelConfig])

      val interpreter = new NettyChannelInterpreter(channelFor, channelConfigList)

      val mc: ManagedChannel = interpreter.build

      mc shouldBe a[ManagedChannel]

      mc.shutdownNow()
    }

    "build an io.grpc.ManagedChannel based on the specified configuration, for a Target" in {

      val channelFor: ChannelFor = ChannelForTarget(SC.host)

      val interpreter = new NettyChannelInterpreter(channelFor, Nil)

      val mc: ManagedChannel = interpreter.build

      mc shouldBe a[ManagedChannel]

      mc.shutdownNow()
    }

    "build an io.grpc.ManagedChannel based on any configuration combination" in {

      val channelFor: ChannelFor = ChannelForAddress(SC.host, SC.port)

      val channelConfigList: List[Either[ManagedChannelConfig, NettyChannelConfig]] = managedChannelConfigAllList
        .map(_.asLeft[NettyChannelConfig]) ++ List(
        NettyChannelType((new LocalChannel).getClass).asRight[ManagedChannelConfig],
        NettyWithOption[Boolean](ChannelOption.valueOf("ALLOCATOR"), true)
          .asRight[ManagedChannelConfig],
        NettyNegotiationType(NegotiationType.PLAINTEXT).asRight[ManagedChannelConfig],
        NettyEventLoopGroup(new NioEventLoopGroup(0)).asRight[ManagedChannelConfig],
        NettySslContext(GrpcSslContexts.forClient.build).asRight[ManagedChannelConfig],
        NettyFlowControlWindow(NettyChannelBuilder.DEFAULT_FLOW_CONTROL_WINDOW)
          .asRight[ManagedChannelConfig],
        NettyMaxHeaderListSize(GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE).asRight[ManagedChannelConfig],
        NettyUsePlaintext().asRight[ManagedChannelConfig],
        NettyUseTransportSecurity.asRight[ManagedChannelConfig],
        NettyKeepAliveTime(1, TimeUnit.MINUTES).asRight[ManagedChannelConfig],
        NettyKeepAliveTimeout(1, TimeUnit.MINUTES).asRight[ManagedChannelConfig],
        NettyKeepAliveWithoutCalls(false).asRight[ManagedChannelConfig],
      )

      val interpreter = new NettyChannelInterpreter(channelFor, channelConfigList)

      val mc: ManagedChannel = interpreter.build

      mc shouldBe a[ManagedChannel]

      mc.shutdownNow()
    }

    "throw an exception when ChannelFor is not recognized" in {

      val channelFor: ChannelFor = ChannelForPort(SC.port)

      val interpreter = new NettyChannelInterpreter(channelFor, Nil)

      an[IllegalArgumentException] shouldBe thrownBy(interpreter.build)
    }
  }

}
