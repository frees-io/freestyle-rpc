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
package marshallers

import java.io.{ByteArrayInputStream, InputStream}

import com.google.common.io.ByteStreams
import com.google.protobuf.{CodedInputStream, CodedOutputStream}
import freestyle.rpc.internal.util.EncoderUtil
import freestyle.rpc.jodatime.util.JodaTimeUtil
import io.grpc.MethodDescriptor.Marshaller
import org.apache.avro.Schema
import org.apache.avro.Schema.Field
import org.joda.time.{LocalDate, LocalDateTime}

object jodaTimeEncoders {

  object pbd {

    import pbdirect._

    implicit object JodaLocalDateWriter extends PBWriter[LocalDate] {
      override def writeTo(index: Int, value: LocalDate, out: CodedOutputStream): Unit =
        out.writeByteArray(
          index,
          EncoderUtil.intToByteArray(JodaTimeUtil.jodaLocalDateToInt(value)))
    }

    implicit object JodaLocalDateReader extends PBReader[LocalDate] {
      override def read(input: CodedInputStream): LocalDate =
        JodaTimeUtil.intToJodaLocalDate(EncoderUtil.byteArrayToInt(input.readByteArray()))
    }

    implicit object JodaLocalDateTimeWriter extends PBWriter[LocalDateTime] {
      override def writeTo(index: Int, value: LocalDateTime, out: CodedOutputStream): Unit =
        out.writeByteArray(
          index,
          EncoderUtil.longToByteArray(JodaTimeUtil.jodaLocalDatetimeToLong(value)))
    }

    implicit object JodaLocalDateTimeReader extends PBReader[LocalDateTime] {
      override def read(input: CodedInputStream): LocalDateTime =
        JodaTimeUtil.longToJodaLocalDateTime(EncoderUtil.byteArrayToLong(input.readByteArray()))
    }

    implicit object JodaLocalDateMarshaller extends Marshaller[LocalDate] {

      override def parse(stream: InputStream): LocalDate =
        Iterator.continually(stream.read).takeWhile(_ != -1).map(_.toByte).toArray.pbTo[LocalDate]

      override def stream(value: LocalDate): InputStream = new ByteArrayInputStream(value.toPB)

    }

    implicit object JodaLocalDateTimeMarshaller extends Marshaller[LocalDateTime] {

      override def parse(stream: InputStream): LocalDateTime =
        Iterator
          .continually(stream.read)
          .takeWhile(_ != -1)
          .map(_.toByte)
          .toArray
          .pbTo[LocalDateTime]

      override def stream(value: LocalDateTime): InputStream =
        new ByteArrayInputStream(value.toPB)

    }

  }

  object avro {

    import com.sksamuel.avro4s._

    implicit object JodaLocalDateToSchema extends ToSchema[LocalDate] {
      override val schema: Schema = Schema.create(Schema.Type.INT)
    }

    implicit object JodaLocalDateFromValue extends FromValue[LocalDate] {
      override def apply(value: Any, field: Field): LocalDate =
        JodaTimeUtil.intToJodaLocalDate(value.asInstanceOf[Int])
    }

    implicit object JodalocalDateToValue extends ToValue[LocalDate] {
      override def apply(value: LocalDate): Int =
        JodaTimeUtil.jodaLocalDateToInt(value)
    }

    implicit object JodaLocalDateTimeToSchema extends ToSchema[LocalDateTime] {
      override val schema: Schema = Schema.create(Schema.Type.LONG)
    }

    implicit object JodaLocalDateTimeFromValue extends FromValue[LocalDateTime] {
      def apply(value: Any, field: Field): LocalDateTime =
        JodaTimeUtil.longToJodaLocalDateTime(value.asInstanceOf[Long])
    }

    implicit object JodaLocalDateTimeToValue extends ToValue[LocalDateTime] {
      override def apply(value: LocalDateTime): Long =
        JodaTimeUtil.jodaLocalDatetimeToLong(value)
    }

    implicit object JodaLocalDateMarshaller extends Marshaller[LocalDate] {
      override def stream(value: LocalDate): InputStream =
        new ByteArrayInputStream(EncoderUtil.intToByteArray(JodaTimeUtil.jodaLocalDateToInt(value)))

      override def parse(stream: InputStream): LocalDate =
        JodaTimeUtil.intToJodaLocalDate(EncoderUtil.byteArrayToInt(ByteStreams.toByteArray(stream)))
    }

    implicit object JodaLocalDateTimeMarshaller extends Marshaller[LocalDateTime] {
      override def stream(value: LocalDateTime): InputStream =
        new ByteArrayInputStream(
          EncoderUtil.longToByteArray(JodaTimeUtil.jodaLocalDatetimeToLong(value)))

      override def parse(stream: InputStream): LocalDateTime =
        JodaTimeUtil.longToJodaLocalDateTime(
          EncoderUtil.byteArrayToLong(ByteStreams.toByteArray(stream)))
    }
  }

}
