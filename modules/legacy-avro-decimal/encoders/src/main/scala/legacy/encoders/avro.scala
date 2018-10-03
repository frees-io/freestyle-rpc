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

package freestyle.rpc.protocol.legacy

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.ByteBuffer

import com.google.common.io.ByteStreams
import com.sksamuel.avro4s.{FromValue, ToSchema, ToValue}
import freestyle.rpc.internal.util.BigDecimalUtil
import io.grpc.MethodDescriptor.Marshaller
import org.apache.avro.Schema
import org.apache.avro.Schema.Field

object avro {

  import AvroDecimalCompatUtils._

  implicit object bigDecimalToSchema extends ToSchema[AvroDecimalCompat] {
    override val schema: Schema = Schema.create(Schema.Type.BYTES)
  }

  implicit object bigDecimalFromValue extends FromValue[AvroDecimalCompat] {
    def apply(value: Any, field: Field): AvroDecimalCompat =
      AvroDecimalCompat(BigDecimalUtil.byteToBigDecimal(value.asInstanceOf[ByteBuffer].array()))
  }

  implicit object bigDecimalToValue extends ToValue[AvroDecimalCompat] {
    override def apply(value: AvroDecimalCompat): ByteBuffer =
      ByteBuffer.wrap(BigDecimalUtil.bigDecimalToByte(value.toBigDecimal))
  }

  implicit val bigDecimalMarshaller: Marshaller[AvroDecimalCompat] =
    new Marshaller[AvroDecimalCompat] {
      override def stream(value: AvroDecimalCompat): InputStream =
        new ByteArrayInputStream(BigDecimalUtil.bigDecimalToByte(value.toBigDecimal))

      override def parse(stream: InputStream): AvroDecimalCompat =
        AvroDecimalCompat(BigDecimalUtil.byteToBigDecimal(ByteStreams.toByteArray(stream)))
    }

}
