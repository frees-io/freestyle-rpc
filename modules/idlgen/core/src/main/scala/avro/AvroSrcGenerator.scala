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

package freestyle.rpc.idlgen.avro

import avrohugger.Generator
import avrohugger.filesorter.AvdlFileSorter
import avrohugger.format.Standard
import avrohugger.types._
import freestyle.rpc.idlgen._
import freestyle.rpc.internal.util.FileUtil._
import java.io.File
import org.apache.avro._
import org.log4s._
import scala.collection.JavaConverters._
import scala.util.Right

object AvroSrcGenerator extends SrcGenerator {

  private[this] val logger = getLogger

  private val mainGenerator = Generator(Standard)
  private val adtGenerator = mainGenerator.copy(avroScalaCustomTypes =
    Some(AvroScalaTypes.defaults.copy(protocol = ScalaADT))) // ScalaADT: sealed trait hierarchies

  val idlType: String = avro.IdlType

  def inputFiles(inputPath: File): Seq[File] = {
    val avprFiles = inputPath.allMatching(_.getName.endsWith(AvprExtension))
    val avdlFiles = inputPath.allMatching(_.getName.endsWith(AvdlExtension))
    // Watch out: FileSorter requires canonical files and goes into an infinite loop on unresolved imports (!)
    avprFiles ++ AvdlFileSorter.sortSchemaFiles(avdlFiles.map(_.getCanonicalFile))
  }

  def generateFrom(inputFile: File, options: String*): Option[(String, Seq[String])] =
    generateFrom(
      mainGenerator.fileParser
        .getSchemaOrProtocols(inputFile, mainGenerator.format, mainGenerator.classStore),
      options)

  def generateFrom(input: String, options: String*): Option[(String, Seq[String])] =
    generateFrom(
      mainGenerator.stringParser
        .getSchemaOrProtocols(input, mainGenerator.schemaStore),
      options)

  private def generateFrom(
      schemasOrProtocols: List[Either[Schema, Protocol]],
      options: Seq[String]): Option[(String, Seq[String])] =
    Some(schemasOrProtocols)
      .filter(_.nonEmpty)
      .flatMap(_.last match {
        case Right(p) => Some(p)
        case _        => None
      })
      .map(generateFrom(_, options))

  def generateFrom(protocol: Protocol, options: Seq[String]): (String, Seq[String]) = {

    val outputPath =
      s"${protocol.getNamespace.replace('.', '/')}/${protocol.getName}$ScalaFileExtension"

    val schemaGenerator = if (protocol.getMessages.isEmpty) adtGenerator else mainGenerator
    val schemaLines = schemaGenerator
      .protocolToStrings(protocol)
      .mkString
      .split('\n')
      .toSeq
      .tail // remove top comment and get package declaration on first line
      .filterNot(_ == "()") // https://github.com/julianpeeters/sbt-avrohugger/issues/33

    val packageLines = Seq(schemaLines.head, "")

    val importLines = Seq("import freestyle.rpc.protocol._")

    val messageLines = schemaLines.tail.map(line =>
      if (line.contains("case class")) s"@message $line" else line) :+ "" // note: can be "final case class"

    val rpcAnnotation = s"  @rpc(${("Avro" +: options).mkString(", ")})"
    val requestLines = protocol.getMessages.asScala.toSeq.flatMap {
      case (name, message) =>
        val comment = Seq(Option(message.getDoc).map(doc => s"  /** $doc */")).flatten
        try comment ++ Seq(
          rpcAnnotation,
          parseMessage(name, message.getRequest, message.getResponse),
          "")
        catch {
          case ParseException(msg) =>
            logger.warn(s"$msg, cannot be converted to freestyle-rpc: $message")
            Seq.empty
        }
    }

    val serviceLines =
      if (requestLines.isEmpty) Seq.empty
      else Seq(s"@service trait ${protocol.getName}[F[_]] {", "") ++ requestLines :+ "}"

    outputPath -> (packageLines ++ importLines ++ messageLines ++ serviceLines)
  }

  private def parseMessage(name: String, request: Schema, response: Schema): String = {
    val args = request.getFields.asScala
    if (args.size > 1)
      throw ParseException("RPC method has more than 1 request parameter")
    val requestParam = {
      if (args.isEmpty)
        s"$DefaultRequestParamName: $EmptyType"
      else {
        val arg = args.head
        if (arg.schema.getType != Schema.Type.RECORD)
          throw ParseException("RPC method request parameter is not a record type")
        s"${arg.name}: ${arg.schema.getFullName}"
      }
    }
    val responseParam = {
      if (response.getType == Schema.Type.NULL) EmptyType
      else s"${response.getNamespace}.${response.getName}"
    }
    s"  def $name($requestParam): F[$responseParam]"
  }

  private case class ParseException(msg: String) extends Exception

}
