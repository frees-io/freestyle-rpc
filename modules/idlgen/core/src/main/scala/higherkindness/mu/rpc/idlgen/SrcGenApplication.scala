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

package higherkindness.mu.rpc.idlgen

import java.io.File

import higherkindness.mu.rpc.idlgen.Model.{
  BigDecimalTypeGen,
  CompressionTypeGen,
  MarshallersImport,
  UseIdiomaticEndpoints
}
import higherkindness.mu.rpc.idlgen.avro.AvroSrcGenerator
import higherkindness.mu.rpc.idlgen.proto.ProtoSrcGenerator
import higherkindness.mu.rpc.idlgen.openapi.OpenApiSrcGenerator
import higherkindness.mu.rpc.idlgen.openapi.OpenApiSrcGenerator.HttpImpl
import java.nio.file.Path

object SrcGenApplication {
  def apply(
      marshallersImports: List[MarshallersImport],
      bigDecimalTypeGen: BigDecimalTypeGen,
      compressionType: CompressionTypeGen,
      useIdiomaticEndpoints: UseIdiomaticEndpoints,
      idlTargetDir: File,
      resourcesBasePath: Path
  ): GeneratorApplication[SrcGenerator] =
    new GeneratorApplication(
      ProtoSrcGenerator.build(compressionType, useIdiomaticEndpoints, idlTargetDir),
      AvroSrcGenerator(
        marshallersImports,
        bigDecimalTypeGen,
        compressionType,
        useIdiomaticEndpoints),
      OpenApiSrcGenerator(HttpImpl.Http4sV20, resourcesBasePath)
    ) {
      def main(args: Array[String]): Unit = {
        generateFrom(args)
        (): Unit
      }
    }
}
