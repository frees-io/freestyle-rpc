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
package prometheus
package client

import freestyle.rpc.common._
import freestyle.rpc.withouttagless.Utils.client.MyRPCClient
import io.prometheus.client.Collector
import org.scalatest.BeforeAndAfterAll
import freestyle.rpc.interceptors.metrics._

import scala.collection.JavaConverters._

abstract class BaseMonitorClientInterceptorTests extends RpcBaseTestSuite with BeforeAndAfterAll {

  import freestyle.rpc.withouttagless.Utils.database._
  import freestyle.rpc.prometheus.shared.RegistryHelper._
  import freestyle.rpc.prometheus.client.implicits._

  def name: String
  def defaultClientRuntime: ClientRuntime
  def allMetricsClientRuntime: ClientRuntime
  def clientRuntimeWithNonDefaultBuckets(buckets: Vector[Double]): ClientRuntime

  override protected def beforeAll(): Unit = {
    import freestyle.rpc.server.implicits._
    serverStart[ConcurrentMonad].unsafeRunSync()
  }

  override protected def afterAll(): Unit = {
    import freestyle.rpc.server.implicits._
    serverStop[ConcurrentMonad].unsafeRunSync()
  }

  s"MonitorClientInterceptor for $name" should {

    "work for unary RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      val clientRuntime: ClientRuntime = defaultClientRuntime
      import clientRuntime._

      clientProgram[ConcurrentMonad].unsafeRunSync()

      val startedTotal: Double = extractMetricValue(clientMetricRpcStarted)
      startedTotal should be >= 0d
      startedTotal should be <= 1d
      findRecordedMetricOrThrow(clientMetricStreamMessagesReceived).samples shouldBe empty
      findRecordedMetricOrThrow(clientMetricStreamMessagesSent).samples shouldBe empty

      val handledSamples =
        findRecordedMetricOrThrow(clientMetricRpcCompleted).samples.asScala.toList
      handledSamples.size shouldBe 1
      handledSamples.headOption.foreach { s =>
        s.value should be >= 0d
        s.value should be <= 1d

        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "UNARY",
          "RPCService",
          "unary",
          "OK")
      }

    }

    "work for client streaming RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[D] =
        APP.cs(cList, i)

      def clientProgram2[F[_]](implicit APP: MyRPCClient[F]): F[D] =
        APP.cs(List(c1), i)

      val clientRuntime: ClientRuntime = defaultClientRuntime
      import clientRuntime._

      clientProgram[ConcurrentMonad].unsafeRunSync()

      val startedTotal: Double = extractMetricValue(clientMetricRpcStarted)

      startedTotal should be >= 0d
      startedTotal should be <= 1d

      val msgSentTotal: Double = extractMetricValue(clientMetricStreamMessagesSent)
      msgSentTotal should be >= 0d
      msgSentTotal should be <= 2d

      findRecordedMetricOrThrow(clientMetricStreamMessagesReceived).samples shouldBe empty

      clientProgram2[ConcurrentMonad].unsafeRunSync()

      val msgSentTotal2: Double = extractMetricValue(clientMetricStreamMessagesSent)
      msgSentTotal2 should be >= 0d
      msgSentTotal2 should be <= 3d

      val handledSamples =
        findRecordedMetricOrThrow(clientMetricRpcCompleted).samples.asScala.toList
      handledSamples.size shouldBe 1
      handledSamples.headOption.foreach { s =>
        s.value should be >= 0d
        s.value should be <= 2d

        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "CLIENT_STREAMING",
          "RPCService",
          "clientStreaming",
          "OK")
      }

    }

    "work for server streaming RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[List[C]] =
        APP.ss(a2.x, a2.y)

      val clientRuntime: ClientRuntime = defaultClientRuntime
      import clientRuntime._

      clientProgram[ConcurrentMonad].unsafeRunSync()

      val startedTotal: Double     = extractMetricValue(clientMetricRpcStarted)
      val msgReceivedTotal: Double = extractMetricValue(clientMetricStreamMessagesReceived)
      findRecordedMetricOrThrow(clientMetricStreamMessagesSent).samples shouldBe empty

      startedTotal should be >= 0d
      startedTotal should be <= 1d
      msgReceivedTotal should be >= 0d
      msgReceivedTotal should be <= 2d

      val handledSamples =
        findRecordedMetricOrThrow(clientMetricRpcCompleted).samples.asScala.toList
      handledSamples.size shouldBe 1
      handledSamples.headOption.foreach { s =>
        s.value should be >= 0d
        s.value should be <= 1d
        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "SERVER_STREAMING",
          "RPCService",
          "serverStreaming",
          "OK"
        )
      }

    }

    "work for bidirectional streaming RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[E] =
        APP.bs(eList)

      val clientRuntime: ClientRuntime = defaultClientRuntime
      import clientRuntime._

      clientProgram[ConcurrentMonad].unsafeRunSync()

      val startedTotal: Double     = extractMetricValue(clientMetricRpcStarted)
      val msgReceivedTotal: Double = extractMetricValue(clientMetricStreamMessagesReceived)
      val msgSentTotal: Double     = extractMetricValue(clientMetricStreamMessagesSent)

      startedTotal should be >= 0d
      startedTotal should be <= 1d
      msgReceivedTotal should be >= 0d
      msgReceivedTotal should be <= 4d
      msgSentTotal should be >= 0d
      msgSentTotal should be <= 2d

      val handledSamples =
        findRecordedMetricOrThrow(clientMetricRpcCompleted).samples.asScala.toList
      handledSamples.size shouldBe 1
      handledSamples.headOption.foreach { s =>
        s.value should be >= 0d
        s.value should be <= 1d

        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "BIDI_STREAMING",
          "RPCService",
          "biStreaming",
          "OK")
      }

    }

    "work when no histogram is enabled" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      val clientRuntime: ClientRuntime = defaultClientRuntime
      import clientRuntime._

      clientProgram[ConcurrentMonad].unsafeRunSync()

      findRecordedMetric(clientMetricCompletedLatencySeconds) shouldBe None

    }

    "work when histogram is enabled" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      val clientRuntime: ClientRuntime = allMetricsClientRuntime
      import clientRuntime._

      clientProgram[ConcurrentMonad].unsafeRunSync()

      val metric: Option[Collector.MetricFamilySamples] =
        findRecordedMetric(clientMetricCompletedLatencySeconds)

      metric shouldBe defined
      metric.map { m =>
        m.samples.size should be > 0
      }

    }

    "work for different buckets" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      val buckets: Vector[Double]      = Vector[Double](0.1, 0.2)
      val clientRuntime: ClientRuntime = clientRuntimeWithNonDefaultBuckets(buckets)
      import clientRuntime._

      clientProgram[ConcurrentMonad].unsafeRunSync()

      countSamples(
        clientMetricCompletedLatencySeconds,
        "grpc_client_completed_latency_seconds_bucket") shouldBe (buckets.size + 1)

    }

    "work when combining multiple calls" in {

      def unary[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      def clientStreaming[F[_]](implicit APP: MyRPCClient[F]): F[D] =
        APP.cs(cList, i)

      val clientRuntime: ClientRuntime = defaultClientRuntime
      import clientRuntime._

      (for {
        a <- unary[ConcurrentMonad]
        b <- clientStreaming[ConcurrentMonad]
      } yield (a, b)).unsafeRunSync()

      findRecordedMetricOrThrow(clientMetricRpcStarted).samples.size() shouldBe 2
      findRecordedMetricOrThrow(clientMetricRpcCompleted).samples.size() shouldBe 2

    }

  }
}
