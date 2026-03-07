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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.type.TagType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExnRefPre} class.
 *
 * <p>ExnRefPre caches type resolution for efficient repeated exception allocation.
 */
@DisplayName("ExnRefPre Tests")
class ExnRefPreTest {

  private ExnType createTestExnType() {
    final FunctionType funcType =
        new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
    final TagType tagType = TagType.create(funcType);
    return new ExnType(tagType);
  }

  @Nested
  @DisplayName("Creation Tests")
  class CreationTests {

    @Test
    @DisplayName("should create ExnRefPre from ExnType via factory method")
    void shouldCreateFromExnType() {
      final ExnType type = createTestExnType();
      final ExnRefPre pre = ExnRefPre.create(type);
      assertNotNull(pre, "ExnRefPre.create should return a non-null instance");
      assertEquals(type, pre.getExnType(), "ExnRefPre should store the provided ExnType");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when ExnType is null")
    void shouldThrowOnNullExnType() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> ExnRefPre.create(null),
              "ExnRefPre.create(null) must throw IllegalArgumentException");
      assertNotNull(ex.getMessage(), "Exception message should not be null");
      assertTrue(
          ex.getMessage().contains("null"),
          "Exception message should mention null: " + ex.getMessage());
    }
  }

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("should be active immediately after creation")
    void shouldBeActiveAfterCreation() {
      final ExnRefPre pre = ExnRefPre.create(createTestExnType());
      assertTrue(pre.isActive(), "ExnRefPre should be active immediately after creation");
    }

    @Test
    @DisplayName("should be closeable without throwing")
    void shouldBeCloseable() {
      final ExnRefPre pre = ExnRefPre.create(createTestExnType());
      pre.close(); // Should not throw
      assertFalse(pre.isActive(), "ExnRefPre should not be active after close");
    }

    @Test
    @DisplayName("should allow close to be called multiple times without throwing")
    void shouldAllowMultipleCloses() {
      final ExnRefPre pre = ExnRefPre.create(createTestExnType());
      pre.close();
      pre.close(); // Second close should not throw
      assertFalse(pre.isActive(), "ExnRefPre should remain inactive after multiple closes");
    }

    @Test
    @DisplayName("allocate should throw IllegalStateException after close")
    void allocateShouldThrowAfterClose() {
      final ExnRefPre pre = ExnRefPre.create(createTestExnType());
      pre.close();
      final IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              () -> pre.allocate(null, null),
              "allocate on closed ExnRefPre must throw IllegalStateException");
      assertNotNull(ex.getMessage(), "Exception message should not be null");
      assertTrue(
          ex.getMessage().contains("closed"),
          "Exception message should mention closed: " + ex.getMessage());
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("allocate should throw IllegalArgumentException when store is null")
    void allocateShouldThrowOnNullStore() {
      final ExnRefPre pre = ExnRefPre.create(createTestExnType());
      assertThrows(
          IllegalArgumentException.class,
          () -> pre.allocate(null, null),
          "allocate with null store must throw IllegalArgumentException");
    }

    @Test
    @DisplayName("allocate should throw IllegalArgumentException when tag is null")
    void allocateShouldThrowOnNullTag() {
      final ExnRefPre pre = ExnRefPre.create(createTestExnType());
      // store is also null, but store null check comes first
      assertThrows(
          IllegalArgumentException.class,
          () -> pre.allocate(null, null),
          "allocate with null tag must throw IllegalArgumentException");
    }

    @Test
    @DisplayName("allocate should throw IllegalArgumentException when fields is null")
    void allocateShouldThrowOnNullFields() {
      final ExnRefPre pre = ExnRefPre.create(createTestExnType());
      // store and tag are also null, but store null check comes first
      assertThrows(
          IllegalArgumentException.class,
          () -> pre.allocate(null, null, (ai.tegmentum.wasmtime4j.WasmValue[]) null),
          "allocate with null fields must throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("AutoCloseable Tests")
  class AutoCloseableTests {

    @Test
    @DisplayName("should work correctly with try-with-resources pattern")
    void shouldWorkWithTryWithResources() {
      final ExnRefPre[] holder = new ExnRefPre[1];
      try (ExnRefPre pre = ExnRefPre.create(createTestExnType())) {
        holder[0] = pre;
        assertNotNull(pre.getExnType(), "ExnType should be accessible inside try block");
        assertTrue(pre.isActive(), "ExnRefPre should be active inside try block");
      } // close() called automatically
      assertFalse(
          holder[0].isActive(),
          "ExnRefPre should not be active after try-with-resources block exits");
    }
  }
}
