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
package internal

import freestyle.rpc.internal.service.GRPCServiceDefBuilder
import io.grpc._
import io.grpc.testing.TestMethodDescriptors

class GRPCServiceDefBuilderTests extends RpcBaseTestSuite {

  val serviceName        = "service_foo"
  val invalidServiceName = "invalid_service_name"
  val flowMethod: MethodDescriptor[String, Integer] =
    TestMethodDescriptors.noopMethod[String, Integer]()
  val headers: Metadata                     = new Metadata()
  val listener: ServerCall.Listener[String] = new ServerCall.Listener[String]() {}

  val handler: ServerCallHandler[String, Integer] = new ServerCallHandler[String, Integer]() {
    override def startCall(
        call: ServerCall[String, Integer],
        headers: Metadata): ServerCall.Listener[String] = listener
  }

  "GRPCServiceDefBuilder.apply" should {

    "build a ServerServiceDefinition based on the provided " +
      "MethodDescriptor's and ServerCallHandler's" in {

      val gRPCServiceDefBuilder =
        new GRPCServiceDefBuilder(serviceName, (flowMethod, handler))

      gRPCServiceDefBuilder.apply.getServiceDescriptor.getName shouldBe serviceName
    }

    "throw an java.lang.IllegalArgumentException when the serviceName is not valid" in {

      val gRPCServiceDefBuilder =
        new GRPCServiceDefBuilder(invalidServiceName, (flowMethod, handler))

      an[IllegalArgumentException] should be thrownBy gRPCServiceDefBuilder.apply
    }

  }

}
