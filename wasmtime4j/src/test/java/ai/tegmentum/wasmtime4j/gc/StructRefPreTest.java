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
 * Tests for {@link StructRefPre} class.
 *
 * <p>StructRefPre caches type resolution for efficient repeated struct allocation.
 */
@DisplayName("StructRefPre Tests")
class StructRefPreTest {

  private StructType createTestStructType() {
    return StructType.builder("TestStruct")
        .addField("x", FieldType.f64(), true)
        .addField("y", FieldType.f64(), true)
        .build();
  }

  @Nested
  @DisplayName("Creation Tests")
  class CreationTests {

    @Test
    @DisplayName("should create from StructType")
    void shouldCreateFromStructType() {
      final StructType type = createTestStructType();
      final StructRefPre pre = StructRefPre.create(type);
      assertNotNull(pre, "Should create StructRefPre");
      assertEquals(type, pre.getStructType(), "Should store the struct type");
    }

    @Test
    @DisplayName("should throw on null StructType")
    void shouldThrowOnNullStructType() {
      assertThrows(IllegalArgumentException.class, () -> StructRefPre.create(null));
    }
  }

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("should be closeable")
    void shouldBeCloseable() {
      final StructRefPre pre = StructRefPre.create(createTestStructType());
      pre.close(); // Should not throw
    }

    @Test
    @DisplayName("allocate should throw after close")
    void allocateShouldThrowAfterClose() {
      final StructRefPre pre = StructRefPre.create(createTestStructType());
      pre.close();
      assertThrows(
          IllegalStateException.class,
          () -> pre.allocate(null, null),
          "Should throw IllegalStateException for use after close");
    }

    @Test
    @DisplayName("allocateDefault should throw after close")
    void allocateDefaultShouldThrowAfterClose() {
      final StructRefPre pre = StructRefPre.create(createTestStructType());
      pre.close();
      assertThrows(
          IllegalStateException.class,
          () -> pre.allocateDefault(null),
          "Should throw IllegalStateException for use after close");
    }

    @Test
    @DisplayName("allocateAsync should throw after close")
    void allocateAsyncShouldThrowAfterClose() {
      final StructRefPre pre = StructRefPre.create(createTestStructType());
      pre.close();
      assertThrows(
          IllegalStateException.class,
          () -> pre.allocateAsync(null, null),
          "Should throw IllegalStateException for use after close");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("allocate should throw on null gcRuntime")
    void allocateShouldThrowOnNullGcRuntime() {
      final StructRefPre pre = StructRefPre.create(createTestStructType());
      assertThrows(
          IllegalArgumentException.class,
          () -> pre.allocate(null, java.util.Collections.emptyList()));
    }

    @Test
    @DisplayName("allocate should throw on null fieldValues")
    void allocateShouldThrowOnNullFieldValues() {
      final StructRefPre pre = StructRefPre.create(createTestStructType());
      // gcRuntime is also null but IllegalArgumentException for gcRuntime is checked first
      assertThrows(IllegalArgumentException.class, () -> pre.allocate(null, null));
    }

    @Test
    @DisplayName("allocateDefault should throw on null gcRuntime")
    void allocateDefaultShouldThrowOnNullGcRuntime() {
      final StructRefPre pre = StructRefPre.create(createTestStructType());
      assertThrows(IllegalArgumentException.class, () -> pre.allocateDefault(null));
    }

    @Test
    @DisplayName("allocateAsync should throw on null gcRuntime")
    void allocateAsyncShouldThrowOnNullGcRuntime() {
      final StructRefPre pre = StructRefPre.create(createTestStructType());
      assertThrows(
          IllegalArgumentException.class,
          () -> pre.allocateAsync(null, java.util.Collections.emptyList()));
    }

    @Test
    @DisplayName("allocateAsync should throw on null fieldValues")
    void allocateAsyncShouldThrowOnNullFieldValues() {
      final StructRefPre pre = StructRefPre.create(createTestStructType());
      assertThrows(IllegalArgumentException.class, () -> pre.allocateAsync(null, null));
    }
  }

  @Nested
  @DisplayName("AutoCloseable Tests")
  class AutoCloseableTests {

    @Test
    @DisplayName("should work with try-with-resources")
    void shouldWorkWithTryWithResources() {
      try (StructRefPre pre = StructRefPre.create(createTestStructType())) {
        assertNotNull(pre.getStructType());
      } // close() called automatically
    }
  }
}
