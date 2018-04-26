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

package examples.todolist
package protocol

import freestyle.rpc.protocol._

trait TagProtocol {

  case class IntMessage(value: Int)

  case class TagRequest(name: String)

  case class Tag(name: String, id: Int)

  case class TagList(list: List[Tag])

  case class TagResponse(tag: Option[Tag])

  @service
  trait TagRpcService[F[_]] {

    @rpc(Avro)
    def reset(empty: Empty.type): F[IntMessage]

    @rpc(Avro)
    def insert(tagRequest: TagRequest): F[TagResponse]

    @rpc(Avro)
    def retrieve(id: IntMessage): F[TagResponse]

    @rpc(Avro)
    def list(empty: Empty.type): F[TagList]

    @rpc(Avro)
    def update(tag: Tag): F[TagResponse]

    @rpc(Avro)
    def destroy(id: IntMessage): F[IntMessage]

  }

}
