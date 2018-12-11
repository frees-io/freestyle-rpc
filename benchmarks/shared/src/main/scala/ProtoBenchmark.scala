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

package mu.rpc.benchmarks

import cats.effect.{IO, Resource}
import cats.syntax.functor._
import mu.rpc.protocol.Empty
import java.util.concurrent.TimeUnit

import mu.rpc.benchmarks.shared.Utils._
import mu.rpc.benchmarks.shared.models._
import mu.rpc.benchmarks.shared.protocols.PersonServicePB
import mu.rpc.benchmarks.shared.Runtime
import mu.rpc.benchmarks.shared.server._
import mu.rpc.testing.servers.ServerChannel
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class ProtoBenchmark extends Runtime {

  implicit val handler: ProtoHandler[IO] = new ProtoHandler[IO]
  val sc: ServerChannel                  = ServerChannel(PersonServicePB.bindService[IO])
  val clientIO: Resource[IO, PersonServicePB.Client[IO]] = PersonServicePB.clientFromChannel[IO](IO(sc.channel))

  @TearDown
  def shutdown(): Unit = IO(sc.shutdown()).void.unsafeRunSync()

  @Benchmark
  def listPersons: PersonList = clientIO.use(_.listPersons(Empty)).unsafeRunTimed(defaultTimeOut).get

  @Benchmark
  def getPerson: Person = clientIO.use(_.getPerson(PersonId("1"))).unsafeRunTimed(defaultTimeOut).get

  @Benchmark
  def getPersonLinks: PersonLinkList =
    clientIO.use(_.getPersonLinks(PersonId("1"))).unsafeRunTimed(defaultTimeOut).get

  @Benchmark
  def createPerson: Person =
    clientIO.use(_.createPerson(person)).unsafeRunTimed(defaultTimeOut).get

  @Benchmark
  def programComposition: PersonAggregation = {

    def clientProgram: IO[PersonAggregation] = clientIO.use { client =>
      for {
        personList <- client.listPersons(Empty)
        p1         <- client.getPerson(PersonId("1"))
        p2         <- client.getPerson(PersonId("2"))
        p3         <- client.getPerson(PersonId("3"))
        p4         <- client.getPerson(PersonId("4"))
        p1Links    <- client.getPersonLinks(PersonId(p1.id))
        p3Links    <- client.getPersonLinks(PersonId(p3.id))
        pNew       <- client.createPerson(person)
      } yield (p1, p2, p3, p4, p1Links, p3Links, personList.add(pNew))
    }

    clientProgram.unsafeRunTimed(defaultTimeOut).get
  }

}
