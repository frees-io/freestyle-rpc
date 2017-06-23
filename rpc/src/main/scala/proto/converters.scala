/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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
package protocol

import freestyle.rpc.protocol.model._

import scala.meta.Defn.{Class, Trait}
import scala.meta._
import scala.meta.contrib._

object converters {

  trait ScalaMetaSource2ProtoDefinitions {
    def convert(s: Source): ProtoDefinitions
  }

  class DefaultScalaMetaSource2ProtoDefinitions(
      implicit MC: ScalaMetaClass2ProtoMessage,
      SC: ScalaMetaTrait2ProtoService)
      extends ScalaMetaSource2ProtoDefinitions {

    override def convert(s: Source): ProtoDefinitions = ProtoDefinitions(
      messages = messageClasses(s).map(MC.convert).toList,
      services = serviceClasses(s).map(SC.convert).toList
    )

    private[this] def messageClasses(source: Source): Seq[Class] = source.collect {
      case c: Class if c.hasMod(mod"@message") => c
    }

    private[this] def serviceClasses(source: Source): Seq[Trait] = source.collect {
      case t: Trait if t.hasMod(mod"@service") => t
    }
  }

  object ScalaMetaSource2ProtoDefinitions {
    implicit def defaultSourceToProtoDefinitions(
        implicit MC: ScalaMetaClass2ProtoMessage,
        SC: ScalaMetaTrait2ProtoService): ScalaMetaSource2ProtoDefinitions =
      new DefaultScalaMetaSource2ProtoDefinitions
  }

  trait ScalaMetaClass2ProtoMessage {
    def convert(c: Class): ProtoMessage
  }

  object ScalaMetaClass2ProtoMessage {
    implicit def defaultClass2MessageConverter(implicit PC: ScalaMetaParam2ProtoMessageField) =
      new ScalaMetaClass2ProtoMessage {
        override def convert(c: Class): ProtoMessage = ProtoMessage(
          name = c.name.value,
          fields =
            c.ctor.paramss.flatten.zipWithIndex.map { case (p, t) => PC.convert(p, t + 1) }.toList
        )
      }
  }

  trait ScalaMetaParam2ProtoMessageField {
    def convert(p: Term.Param, tag: Int): ProtoMessageField
  }

  object ScalaMetaParam2ProtoMessageField {
    implicit def defaultParam2ProtoMessageField: ScalaMetaParam2ProtoMessageField =
      new ScalaMetaParam2ProtoMessageField {
        override def convert(p: Term.Param, tag: Int): ProtoMessageField = p match {
          case param"..$mods $paramname: Double = $expropt" =>
            ProtoDouble(name = paramname.value, tag = tag)
          case param"..$mods $paramname: Float = $expropt" =>
            ProtoFloat(name = paramname.value, tag = tag)
          case param"..$mods $paramname: Long = $expropt" =>
            ProtoInt64(name = paramname.value, tag = tag)
          case param"..$mods $paramname: Boolean = $expropt" =>
            ProtoBool(name = paramname.value, tag = tag)
          case param"..$mods $paramname: Int = $expropt" =>
            ProtoInt32(name = paramname.value, tag = tag)
          case param"..$mods $paramname: String = $expropt" =>
            ProtoString(name = paramname.value, tag = tag)
          case param"..$mods $paramname: Array[Byte] = $expropt" =>
            ProtoBytes(name = paramname.value, tag = tag)
          case param"..$mods $paramname: $tpe = $expropt" =>
            ProtoCustomType(name = paramname.value, tag = tag, id = tpe.toString())
        }
      }
  }

  trait ScalaMetaTrait2ProtoService {
    def convert(t: Trait): ProtoService
  }

  object ScalaMetaTrait2ProtoService {
    implicit def defaultTrait2ServiceConverter =
      new ScalaMetaTrait2ProtoService {
        override def convert(t: Trait): ProtoService = {
          println(t.structure)
          ProtoService(
            name = t.name.value,
            rpcs = t.collect {
              case q"@rpc @stream[ResponseStreaming] def $name[..$tparams]($request, observer: StreamObserver[$response]): FS[Unit]" =>
                ProtoServiceField(
                  name = name.value,
                  request = extractParamType(request),
                  response = response.toString(),
                  streamingType = Some(ResponseStreaming))
              case q"@rpc @stream[RequestStreaming] def $name[..$tparams]($param): FS[StreamObserver[$response]]" =>
                ProtoServiceField(
                  name = name.value,
                  request = extractParamStreamingType(param),
                  response = response.toString(),
                  streamingType = Some(RequestStreaming)
                )
              case q"@rpc @stream[BidirectionalStreaming] def $name[..$tparams]($param): FS[StreamObserver[$response]]" =>
                ProtoServiceField(
                  name = name.value,
                  request = extractParamStreamingType(param),
                  response = response.toString(),
                  streamingType = Some(BidirectionalStreaming)
                )
              case q"@rpc def $name[..$tparams]($request): FS[$response]" =>
                ProtoServiceField(
                  name = name.value,
                  request = extractParamType(request),
                  response = response.toString(),
                  streamingType = None)
            }
          )
        }

        private[this] def extractParamType(param: Term.Param): String =
          param.decltpe match {
            case Some(retType) => retType.toString()
            case None =>
              throw new IllegalArgumentException(s"unexpected $param without return type")
          }

        private[this] def extractParamStreamingType(param: Term.Param): String =
          param.decltpe match {
            case Some(retType) =>
              retType match {
                case t"StreamObserver[$request]" => request.toString()
                case _ =>
                  throw new IllegalArgumentException(s"$param not enclosed in StreamObserver[_]")
              }
            case None =>
              throw new IllegalArgumentException(s"unexpected $param without return type")
          }

      }
  }

}
