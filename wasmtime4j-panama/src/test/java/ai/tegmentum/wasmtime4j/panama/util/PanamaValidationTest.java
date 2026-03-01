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
package ai.tegmentum.wasmtime4j.panama.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaValidation} utility class.
 *
 * <p>Tests only the Panama-specific {@code requireValidHandle(MemorySegment, String)} method. All
 * other validation methods are provided by {@link ai.tegmentum.wasmtime4j.util.Validation} and
 * tested in {@code ValidationTest}.
 */
@DisplayName("PanamaValidation Tests")
class PanamaValidationTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaValidationTest.class.getName());

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaValidation should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaValidation.class.getModifiers()),
          "PanamaValidation should be final");
      LOGGER.info("Verified PanamaValidation is final");
    }

    @Test
    @DisplayName("Constructor should throw AssertionError")
    void constructorShouldThrowAssertionError() throws Exception {
      final var constructor = PanamaValidation.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      final var exception =
          assertThrows(
              java.lang.reflect.InvocationTargetException.class,
              constructor::newInstance,
              "Constructor should throw via reflection");
      assertTrue(exception.getCause() instanceof AssertionError, "Cause should be AssertionError");
      LOGGER.info("Verified constructor throws AssertionError");
    }
  }

  @Nested
  @DisplayName("requireValidHandle MemorySegment Tests")
  class RequireValidHandleMemorySegmentTests {

    @Test
    @DisplayName("requireValidHandle should accept valid non-null segment")
    void requireValidHandleShouldAcceptValidSegment() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment segment = arena.allocate(8);
        assertDoesNotThrow(
            () -> PanamaValidation.requireValidHandle(segment, "testHandle"),
            "Should accept valid non-null, non-NULL segment");
        LOGGER.info("Valid segment accepted: " + segment);
      }
    }

    @Test
    @DisplayName("requireValidHandle should throw for null segment")
    void requireValidHandleShouldThrowForNullSegment() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> PanamaValidation.requireValidHandle((MemorySegment) null, "handleName"),
              "Should throw for null segment");
      assertTrue(
          ex.getMessage().contains("handleName"),
          "Message should contain handle name, got: " + ex.getMessage());
      LOGGER.info("Null segment threw: " + ex.getMessage());
    }

    @Test
    @DisplayName("requireValidHandle should throw for MemorySegment.NULL")
    void requireValidHandleShouldThrowForNullSegmentValue() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> PanamaValidation.requireValidHandle(MemorySegment.NULL, "handleName"),
              "Should throw for MemorySegment.NULL");
      assertTrue(
          ex.getMessage().contains("null pointer"),
          "Message should indicate null pointer, got: " + ex.getMessage());
      LOGGER.info("MemorySegment.NULL threw: " + ex.getMessage());
    }
  }
}
