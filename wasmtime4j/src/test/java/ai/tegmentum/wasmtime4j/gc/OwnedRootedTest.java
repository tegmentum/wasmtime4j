/*
 * Copyright 2025 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link OwnedRooted} class.
 *
 * <p>OwnedRooted is an owned rooted reference to a GC-managed WebAssembly object that implements
 * AutoCloseable for convenient try-with-resources usage.
 */
@DisplayName("OwnedRooted Tests")
class OwnedRootedTest {

  @Nested
  @DisplayName("Factory Method Null Validation Tests")
  class FactoryMethodNullTests {

    @Test
    @DisplayName("create with null store should throw NullPointerException")
    void createWithNullStoreShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> OwnedRooted.create(null, "value"),
          "create(null store, value) should throw NullPointerException");
    }

    @Test
    @DisplayName("create AnyRef with null store should throw NullPointerException")
    void createAnyRefWithNullStoreShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> OwnedRooted.create(null, AnyRef.nullRef()),
          "create(null store, AnyRef) should throw NullPointerException");
    }

    @Test
    @DisplayName("create EqRef with null store should throw NullPointerException")
    void createEqRefWithNullStoreShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> OwnedRooted.create(null, EqRef.nullRef()),
          "create(null store, EqRef) should throw NullPointerException");
    }

    @Test
    @DisplayName("create StructRef with null store should throw NullPointerException")
    void createStructRefWithNullStoreShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> OwnedRooted.create(null, StructRef.nullRef()),
          "create(null store, StructRef) should throw NullPointerException");
    }

    @Test
    @DisplayName("create ArrayRef with null store should throw NullPointerException")
    void createArrayRefWithNullStoreShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> OwnedRooted.create(null, ArrayRef.nullRef()),
          "create(null store, ArrayRef) should throw NullPointerException");
    }
  }
}
