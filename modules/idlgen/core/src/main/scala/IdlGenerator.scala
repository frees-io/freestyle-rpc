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

import freestyle.rpc.protocol.SerializationType
import java.io.File
import scala.meta._

trait IdlGenerator extends Generator {

  def serializationType: SerializationType
  def outputSubdir: String
  def fileExtension: String

  def inputFiles(files: Set[File]): Seq[File] =
    files.filter(_.getName.endsWith(ScalaFileExtension)).toSeq

  def generateFrom(inputFile: File, options: String*): Option[(String, Seq[String])] = {
    val inputName   = inputFile.getName.replaceAll(ScalaFileExtension, "")
    val definitions = ScalaParser.parse(inputFile.parse[Source].get, inputName)
    generateFrom(definitions).map(output => s"$outputSubdir/$inputName$fileExtension" -> output)
  }

  def generateFrom(rpc: RpcDefinitions): Option[Seq[String]] = {
    val messages = rpc.messages
    val services = filterServices(rpc.services)
    if (messages.nonEmpty || services.nonEmpty)
      Some(generateFrom(rpc.outputName, rpc.outputPackage, rpc.options, messages, services))
    else None
  }

  protected def generateFrom(
      outputName: String,
      outputPackage: Option[String],
      options: Seq[RpcOption],
      messages: Seq[RpcMessage],
      services: Seq[RpcService]): Seq[String]

  private def filterServices(services: Seq[RpcService]): Seq[RpcService] =
    services
      .map(service =>
        service.copy(requests = service.requests.filter(_.serializationType == serializationType)))
      .filter(_.requests.nonEmpty)
}
