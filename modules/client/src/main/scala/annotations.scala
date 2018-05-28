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

package freestyle
package rpc

import freestyle.rpc.internal.serviceImpl

import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.meta._

// $COVERAGE-OFF$
package object protocol {

  @compileTimeOnly("enable macro paradise to expand @service macro annotations")
  class service(serializationType: SerializationType) extends StaticAnnotation {
    import scala.meta._

    inline def apply(defn: Any): Any = meta {

      val serType: SerializationType = this match {
        case q"new $_(Avro)"           => Avro
        case q"new $_(AvroWithSchema)" => AvroWithSchema
        case q"new $_(Protobuf)"       => Protobuf
        case _                         => abort("Invalid serialization type")
      }

      serviceImpl.service(defn, serType)
    }
  }
}

// $COVERAGE-ON$
