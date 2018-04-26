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

import examples.todolist.protocol.common._
import freestyle.rpc.protocol._

trait TodoListProtocol {

  case class TodoListMessage(title: String, tagId: Int, id: Int)

  case class TodoListRequest(title: String, tagId: Int)

  case class TodoListList(list: List[TodoListMessage])

  case class TodoListResponse(msg: Option[TodoListMessage])

  @service
  trait TodoListRpcService[F[_]] {

    @rpc(Avro)
    def reset(empty: Empty.type): F[IntMessage]

    @rpc(Avro)
    def insert(item: TodoListRequest): F[TodoListResponse]

    @rpc(Avro)
    def retrieve(id: IntMessage): F[TodoListResponse]

    @rpc(Avro)
    def list(empty: Empty.type): F[TodoListList]

    @rpc(Avro)
    def update(item: TodoListMessage): F[TodoListResponse]

    @rpc(Avro)
    def destroy(id: IntMessage): F[IntMessage]

  }

}
