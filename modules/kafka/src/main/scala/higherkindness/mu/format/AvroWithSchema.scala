/*
 * Copyright 2017-2020 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.format

import java.io.ByteArrayOutputStream
import com.sksamuel.avro4s._

object AvroWithSchema {
  implicit def serialiser[T: Encoder: SchemaFor]: Serialiser[T] = new Serialiser[T] {
    override def serialise(t: T): Array[Byte] = {
      val bOut = new ByteArrayOutputStream()
      val out  = AvroOutputStream.data[T].to(bOut).build(AvroSchema[T])
      out.write(t)
      out.close()
      bOut.toByteArray
    }
  }

  implicit def deserialiser[T: Decoder]: Deserialiser[T] = new Deserialiser[T] {
    override def deserialise(bytes: Array[Byte]): T = {
      val in = AvroInputStream.data[T].from(bytes).build
      in.close()
      in.iterator.toSet.head
    }
  }
}
