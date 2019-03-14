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

package higherkindness.mu.rpc.idlgen.proto

import java.io.File

import cats.effect.IO
import higherkindness.mu.rpc.idlgen._
import higherkindness.skeuomorph.mu.MuF
import higherkindness.skeuomorph.protobuf.ParseProto.{parseProto, ProtoSource}
import higherkindness.skeuomorph.protobuf.{ProtobufF, Protocol}
import org.log4s._
import qq.droste.data.Mu
import qq.droste.data.Mu._

object ProtoSrcGenerator extends SrcGenerator {

  private[this] val logger = getLogger

  val idlType: String = proto.IdlType

  def inputFiles(files: Set[File]): Seq[File] = {
    val protoFiles = files.filter(_.getName.endsWith(ProtoExtension))
    protoFiles.toSeq
  }

  def generateFrom(
      inputFile: File,
      serializationType: String,
      options: String*): Option[(String, Seq[String])] = Option(getCode(inputFile))

  private def getCode(file: File): (String, Seq[String]) = {

    val source = ProtoSource(file.getName, file.getParent)

    val protobufProtocol: Protocol[Mu[ProtobufF]] =
      parseProto[IO, Mu[ProtobufF]].parse(source).unsafeRunSync()

    val parseProtocol: Protocol[Mu[ProtobufF]] => higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] = {
      p: Protocol[Mu[ProtobufF]] =>
        higherkindness.skeuomorph.mu.Protocol.fromProtobufProto(p)
    }

    val printProtocol: higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] => String = {
      p: higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] =>
        higherkindness.skeuomorph.mu.print.proto.print(p)
    }

    val result: String = (parseProtocol andThen printProtocol)(protobufProtocol)

    println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
    println(result.withImports.withCoproducts)

    getPath(protobufProtocol) -> Seq(result.withImports.withCoproducts)
  }

  private def getPath(p: Protocol[Mu[ProtobufF]]): String =
    s"${p.pkg.toPath}/${p.name}$ScalaFileExtension"

  implicit class StringOps(self: String) {

    def withImports: String =
      (self.split("\n", 2).toList match {
        case h :: t =>
          List(
            h,
            "\n",
            "import higherkindness.mu.rpc.protocol._",
            "import fs2.Stream",
            "import shapeless.{:+:, CNil}") ++ t
        case a => a
      }).mkString("\n")

    def withCoproducts: String =
      """((Cop\[)(((\w+)(\s)?(\:\:)(\s)?)+)(TNil)(\]))""".r.replaceAllIn(self, _.matched.cleanCop)

    def cleanCop: String =
      self.replace("Cop[", "").replace("::", ":+:").replace("TNil]", "CNil")

    def toPath: String = self.replace('.', '/')

  }

}
