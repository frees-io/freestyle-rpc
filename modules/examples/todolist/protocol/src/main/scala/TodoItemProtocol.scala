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

trait TodoItemProtocol {

  case class TodoItemMessage(item: String, todoListId: Int, completed: Boolean, id: Int)

  case class TodoItemRequest(item: String, todoListId: Int)

  case class TodoItemList(list: List[TodoItemMessage])

  case class TodoItemResponse(msg: Option[TodoItemMessage])

  @service
  trait TodoItemRpcService[F[_]] {

    @rpc(Avro)
    def reset(empty: Empty.type): F[IntMessage]

    @rpc(Avro)
    def insert(item: TodoItemRequest): F[TodoItemResponse]

    @rpc(Avro)
    def retrieve(id: IntMessage): F[TodoItemResponse]

    @rpc(Avro)
    def list(empty: Empty.type): F[TodoItemList]

    @rpc(Avro)
    def update(item: TodoItemMessage): F[TodoItemResponse]

    @rpc(Avro)
    def destroy(id: IntMessage): F[IntMessage]

  }

}