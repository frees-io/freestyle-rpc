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
package protocol

import scala.annotation.StaticAnnotation

sealed trait StreamingType         extends Product with Serializable
case object RequestStreaming       extends StreamingType
case object ResponseStreaming      extends StreamingType
case object BidirectionalStreaming extends StreamingType

sealed trait SerializationType extends Product with Serializable
case object Protobuf           extends SerializationType
case object Avro               extends SerializationType
case object AvroWithSchema     extends SerializationType

sealed abstract class CompressionType extends Product with Serializable
case object Identity                  extends CompressionType
case object Gzip                      extends CompressionType

@deprecated("Streaming type is now determined from request and response types", "0.14.0")
class stream[S <: StreamingType]       extends StaticAnnotation
class message                          extends StaticAnnotation
class option(name: String, value: Any) extends StaticAnnotation
class outputPackage(value: String)     extends StaticAnnotation
class outputName(value: String)        extends StaticAnnotation

@message
object Empty
