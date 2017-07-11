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

package freestyle.rpc.internal.service

import freestyle.internal.ScalametaUtil

import scala.collection.immutable.Seq
import scala.meta.Defn.{Class, Object, Trait}
import scala.meta._

// $COVERAGE-OFF$ScalaJS + coverage = fails with NoClassDef exceptions
object serviceImpl {

  import errors._

  def service(defn: Any): Stat = defn match {

    case Term.Block(Seq(cls: Trait, companion: Object)) =>
      serviceExtras(cls, companion)
    case Term.Block(Seq(cls: Class, companion: Object)) if ScalametaUtil.isAbstract(cls) =>
      serviceExtras(cls, companion)
    case _ =>
      println(defn.getClass)
      abort(s"$invalid. $abstractOnly")
  }

  def serviceExtras(alg: Defn, companion: Object): Term.Block =
    Term.Block(Seq(alg, enrich(companion)))

  def enrich(companion: Object): Object = companion match {
    case q"..$mods object $ename extends $template" =>
      template match {
        case template"{ ..$earlyInit } with ..$inits { $self => ..$stats }" =>
          val enrichedTemplate =
            template"{ ..$earlyInit } with ..$inits { $self => ..${enrich(stats)} }"
          val result = q"..$mods object $ename extends $enrichedTemplate"
          println(result)
          result
      }
  }

  def enrich(members: Seq[Stat]): Seq[Stat] =
    members ++ Seq(
      q"""
         trait ServiceHandler
            extends Handler[_root_.scala.concurrent.Future]
            with _root_.com.trueaccord.scalapb.grpc.AbstractService {
               override def serviceCompanion = ServiceHandler
            }
       """,
      q"""
         object ServiceHandler extends _root_.com.trueaccord.scalapb.grpc.ServiceCompanion[ServiceHandler] {
            implicit def serviceCompanion: _root_.com.trueaccord.scalapb.grpc.ServiceCompanion[ServiceHandler] = this
            lazy val javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = ???
         }
       """
    )

}

private[internal] object errors {
  val invalid = "Invalid use of `@service`"
  val abstractOnly =
    "`@service` can only annotate a trait or abstract class already annotated with @free"
}
// $COVERAGE-ON$
