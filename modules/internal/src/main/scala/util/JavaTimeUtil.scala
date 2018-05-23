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
package util

import java.time._

object JavaTimeUtil {

  def localDateToInt(value: LocalDate): Int = value.toEpochDay.toInt

  def intToLocalDate(value: Int): LocalDate = LocalDate.ofEpochDay(value.toLong)

  def localDateTimeToLong(value: LocalDateTime): Long =
    value.toInstant(ZoneOffset.UTC).toEpochMilli

  def longToLocalDateTime(value: Long): LocalDateTime =
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC).toLocalDateTime
}
