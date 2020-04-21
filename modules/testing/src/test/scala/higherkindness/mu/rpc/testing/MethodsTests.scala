/*
 * Copyright 2017-2020 47 Degrees <http://47deg.com>
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

package higherkindness.mu.rpc
package testing

import org.scalacheck.Gen
import org.scalatest._
import org.scalatestplus.scalacheck.Checkers
import org.scalacheck.Prop._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MethodsTests extends AnyWordSpec with Matchers with Checkers {

  "methods.voidMethod" should {

    "generate the method with the provided name" in {
      check {
        forAll(Gen.identifier) { methodName =>
          methods.voidMethod(Some(methodName)).getFullMethodName == methodName
        }
      }
    }

    "generate the method with an auto-generated name" in {
      methods.voidMethod().getFullMethodName.isEmpty shouldBe false
    }

  }

}
