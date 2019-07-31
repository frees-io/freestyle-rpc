/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc.idlgen.openapi

import java.io.File
import java.nio.file.{Path, Paths}
import higherkindness.mu.rpc.idlgen._
import higherkindness.skeuomorph.openapi._
import schema.OpenApi
import ParseOpenApi._
import print._
import client.print._
import client.http4s.circe._
import client.http4s.print._
import client.http4s.print.v20._

import cats.data.Nested
import cats.implicits._

import higherkindness.skeuomorph.Parser
import cats.effect._
import higherkindness.skeuomorph.openapi.JsonSchemaF

object OpenApiSrcGenerator {

  def apply(): SrcGenerator = new SrcGenerator {
    def idlType: String = IdlType
    protected def inputFiles(files: Set[File]): Seq[File] =
      files.filter(handleFile(_)(_ => true, _ => true, false)).toSeq

    protected def generateFrom(
        inputFile: File,
        serializationType: String,
        options: String*): Option[(String, Seq[String])] =
      getCode[IO](inputFile).value.unsafeRunSync()

    private def getCode[F[_]: Sync](file: File): Nested[F, Option, (String, Seq[String])] =
      parseFile[F]
        .apply(file)
        .map(OpenApi.extractNestedTypes[JsonSchemaF.Fixed])
        .map { openApi =>
          pathFrom(Paths.get("."), "foo") ->
            Seq(
              model[JsonSchemaF.Fixed].print(openApi),
              interfaceDefinition.print(openApi),
              impl.print(PackageName("foo") -> openApi)
            )
        }

    private def pathFrom(path: Path, name: String): String =
      s"${path}/$name$ScalaFileExtension"

    private def parseFile[F[_]: Sync]: File => Nested[F, Option, OpenApi[JsonSchemaF.Fixed]] =
      x =>
        Nested(
          handleFile(x)(
            Parser[F, JsonSource, OpenApi[JsonSchemaF.Fixed]].parse(_).map(_.some),
            Parser[F, YamlSource, OpenApi[JsonSchemaF.Fixed]].parse(_).map(_.some),
            Sync[F].delay(none)
          ))

    private def handleFile[T](
        file: File)(json: JsonSource => T, yaml: YamlSource => T, none: T): T = file match {
      case x if (x.getName().endsWith(JsonExtension)) => json(JsonSource(file))
      case x if (x.getName().endsWith(YamlExtension)) => yaml(YamlSource(file))
      case _                                          => none
    }
  }
}
