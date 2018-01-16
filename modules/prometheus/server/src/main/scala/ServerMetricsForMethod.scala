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
package server

import freestyle.rpc.interceptors.GrpcMethodInfo
import io.grpc.Status.Code
import io.prometheus.client.SimpleCollector

case class ServerMetricsForMethod(method: GrpcMethodInfo, serverMetrics: ServerMetrics) {

  import serverMetrics._

  def recordCallStarted(): Unit =
    addLabels(serverStarted).inc()

  def recordServerHandled(code: Code): Unit =
    addLabels(serverHandled, code.toString).inc()

  def recordStreamMessageReceived(): Unit =
    addLabels(serverStreamMessagesReceived).inc()

  def recordStreamMessageSent(): Unit =
    addLabels(serverStreamMessagesSent).inc()

  def recordLatency(latencySec: Double): Unit =
    serverHandledLatencySeconds foreach (_ =>
      addLabels(serverHandledLatencySeconds.get).observe(latencySec))

  private def addLabels[T](collector: SimpleCollector[T], labels: String*): T =
    collector.labels(
      List(method.`type`.toString, method.serviceName, method.methodName) ++ labels.toList: _*)

}
