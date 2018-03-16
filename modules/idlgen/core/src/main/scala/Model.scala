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

package freestyle.rpc.idlgen

import freestyle.rpc.protocol._
import freestyle.rpc.internal.util.StringUtil._
import scala.meta._

case class RpcDefinitions(
    outputName: String,
    outputPackage: Option[String],
    options: Seq[RpcOption],
    messages: Seq[RpcMessage],
    services: Seq[RpcService])

case class RpcOption(name: String, value: String)

case class RpcMessage(name: String, params: Seq[Term.Param]) {
  // Workaround for `Term.Param` using referential equality; needed mostly for unit testing
  override def equals(other: Any): Boolean = other match {
    case that: RpcMessage =>
      this.name == that.name && this.params.map(_.toString.trimAll) == that.params.map(
        _.toString.trimAll)
    case _ => false
  }
}

case class RpcService(name: String, requests: Seq[RpcRequest])

case class RpcRequest(
    serializationType: SerializationType,
    name: String,
    requestType: Type,
    responseType: Type,
    streamingType: Option[StreamingType] = None) {
  // Workaround for `Type` using referential equality; needed mostly for unit testing
  override def equals(other: Any): Boolean = other match {
    case that: RpcRequest =>
      this.serializationType == that.serializationType &&
        this.name == that.name &&
        this.requestType.toString.trimAll == that.requestType.toString.trimAll &&
        this.responseType.toString.trimAll == that.responseType.toString.trimAll &&
        this.streamingType == that.streamingType
    case _ => false
  }
}
