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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ArrayRefPre} class.
 *
 * <p>ArrayRefPre caches type resolution for efficient repeated array allocation.
 */
@DisplayName("ArrayRefPre Tests")
class ArrayRefPreTest {

  private ArrayType createTestArrayType() {
    return ArrayType.builder("TestArray").elementType(FieldType.i32()).mutable(true).build();
  }

  @Nested
  @DisplayName("Creation Tests")
  class CreationTests {

    @Test
    @DisplayName("should create from ArrayType")
    void shouldCreateFromArrayType() {
      final ArrayType type = createTestArrayType();
      final ArrayRefPre pre = ArrayRefPre.create(type);
      assertNotNull(pre, "Should create ArrayRefPre");
      assertEquals(type, pre.getArrayType(), "Should store the array type");
    }

    @Test
    @DisplayName("should throw on null ArrayType")
    void shouldThrowOnNullArrayType() {
      assertThrows(IllegalArgumentException.class, () -> ArrayRefPre.create(null));
    }
  }

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("should be closeable")
    void shouldBeCloseable() {
      final ArrayRefPre pre = ArrayRefPre.create(createTestArrayType());
      pre.close(); // Should not throw
    }

    @Test
    @DisplayName("allocate should throw after close")
    void allocateShouldThrowAfterClose() {
      final ArrayRefPre pre = ArrayRefPre.create(createTestArrayType());
      pre.close();
      assertThrows(
          IllegalStateException.class,
          () -> pre.allocate(null, null),
          "Should throw IllegalStateException for use after close");
    }

    @Test
    @DisplayName("allocateDefault should throw after close")
    void allocateDefaultShouldThrowAfterClose() {
      final ArrayRefPre pre = ArrayRefPre.create(createTestArrayType());
      pre.close();
      assertThrows(
          IllegalStateException.class,
          () -> pre.allocateDefault(null, 10),
          "Should throw IllegalStateException for use after close");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("allocate should throw on null gcRuntime")
    void allocateShouldThrowOnNullGcRuntime() {
      final ArrayRefPre pre = ArrayRefPre.create(createTestArrayType());
      assertThrows(
          IllegalArgumentException.class,
          () -> pre.allocate(null, java.util.Collections.emptyList()));
    }

    @Test
    @DisplayName("allocate should throw on null elements")
    void allocateShouldThrowOnNullElements() {
      final ArrayRefPre pre = ArrayRefPre.create(createTestArrayType());
      assertThrows(IllegalArgumentException.class, () -> pre.allocate(null, null));
    }

    @Test
    @DisplayName("allocateDefault should throw on null gcRuntime")
    void allocateDefaultShouldThrowOnNullGcRuntime() {
      final ArrayRefPre pre = ArrayRefPre.create(createTestArrayType());
      assertThrows(IllegalArgumentException.class, () -> pre.allocateDefault(null, 10));
    }

    @Test
    @DisplayName("allocateDefault should throw on negative length")
    void allocateDefaultShouldThrowOnNegativeLength() {
      final ArrayRefPre pre = ArrayRefPre.create(createTestArrayType());
      assertThrows(
          IllegalArgumentException.class,
          () -> pre.allocateDefault(null, -1),
          "Should reject negative length");
    }
  }

  @Nested
  @DisplayName("AutoCloseable Tests")
  class AutoCloseableTests {

    @Test
    @DisplayName("should work with try-with-resources")
    void shouldWorkWithTryWithResources() {
      try (ArrayRefPre pre = ArrayRefPre.create(createTestArrayType())) {
        assertNotNull(pre.getArrayType());
      } // close() called automatically
    }
  }
}
