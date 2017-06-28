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

package freestyle.rpc
package client

import freestyle.free
import io.grpc._
import io.grpc.stub.StreamObserver

@free
trait ClientCallsM {

  def async[I, O](call: ClientCall[I, O], param: I, observer: StreamObserver[O]): FS[Unit]

  def asyncStreamServer[I, O](
      call: ClientCall[I, O],
      param: I,
      responseObserver: StreamObserver[O]): FS[Unit]

  def asyncStreamClient[I, O](
      call: ClientCall[I, O],
      responseObserver: StreamObserver[O]): FS[StreamObserver[I]]

  def asyncStreamBidi[I, O](
      call: ClientCall[I, O],
      responseObserver: StreamObserver[O]): FS[StreamObserver[I]]

  def sync[I, O](call: ClientCall[I, O], param: I): FS[O]

  def syncC[I, O](
      channel: Channel,
      method: MethodDescriptor[I, O],
      callOptions: CallOptions,
      param: I): FS[O]

  def syncStreamServer[I, O](call: ClientCall[I, O], param: I): FS[Iterator[O]]

  def syncStreamServerC[I, O](
      channel: Channel,
      method: MethodDescriptor[I, O],
      callOptions: CallOptions,
      param: I): FS[Iterator[O]]

  def asyncM[I, O](call: ClientCall[I, O], param: I): FS[O]

}
