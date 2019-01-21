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

package higherkindness.mu.rpc
package channel

import java.net.URI
import java.util.concurrent.{Callable, Executor, Executors, TimeUnit}

import com.google.common.util.concurrent.{ListenableFuture, ListeningExecutorService, MoreExecutors}
import higherkindness.mu.rpc.channel.utils.StringMarshaller
import higherkindness.mu.rpc.common.{RpcBaseTestSuite, SC}
import higherkindness.mu.rpc.testing.client.FakeNameResolverFactory
import higherkindness.mu.rpc.testing.interceptors.NoopInterceptor
import io.grpc.internal.testing.TestUtils
import io.grpc.util.RoundRobinLoadBalancerFactory
import io.grpc._

trait RpcClientTestSuite extends RpcBaseTestSuite {

  trait DummyData {

    type M = MethodDescriptor[String, String]
    type C = ClientCall[String, String]

    val managedChannelMock: ManagedChannel = mock[ManagedChannel]
    val clientCallMock: C                  = stub[C]

    val methodDescriptor: M = MethodDescriptor
      .newBuilder(new StringMarshaller(), new StringMarshaller())
      .setType(MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(MethodDescriptor.generateFullMethodName("foo.Bar", "Bar"))
      .build()

    val authority: String      = s"${SC.host}:${SC.port}"
    val foo                    = "Bar"
    val failureMessage: String = "❗"

    val managedChannelConfigAllList: List[ManagedChannelConfig] = List(
      DirectExecutor,
      SetExecutor(new Executor() {
        override def execute(r: Runnable): Unit =
          throw new RuntimeException("Test executor")
      }),
      AddInterceptorList(List(new NoopInterceptor())),
      AddInterceptor(new NoopInterceptor()),
      UserAgent("User-Agent"),
      OverrideAuthority(TestUtils.TEST_SERVER_HOST),
      UsePlaintext(),
      NameResolverFactory(
        FakeNameResolverFactory(new URI("defaultscheme", "", "/[valid]", null).getScheme)),
      LoadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance()),
      DefaultLoadBalancingPolicy("round_robin"),
      SetDecompressorRegistry(DecompressorRegistry.getDefaultInstance),
      SetCompressorRegistry(CompressorRegistry.getDefaultInstance),
      SetIdleTimeout(1, TimeUnit.MINUTES),
      SetMaxInboundMessageSize(4096000)
    )
  }

  object implicits extends Helpers with DummyData {

    val service: ListeningExecutorService =
      MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10))

    val exception: Throwable = new RuntimeException("Test exception")

    def failedFuture[T]: ListenableFuture[T] =
      service.submit(new Callable[T] {
        override def call(): T = throw exception
      })

    def successfulFuture[T](value: T): ListenableFuture[T] =
      service.submit(new Callable[T] {
        override def call(): T = value
      })
  }
}
