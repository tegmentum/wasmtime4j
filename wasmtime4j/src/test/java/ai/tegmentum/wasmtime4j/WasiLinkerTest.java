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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link WasiLinker} WASI linking utility. */
@DisplayName("WasiLinker")
final class WasiLinkerTest {

  @Nested
  @DisplayName("addToLinker with linker and context")
  final class AddToLinkerWithContextTests {

    @Test
    @DisplayName("should reject null linker")
    void shouldRejectNullLinker() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiLinker.addToLinker(null, null),
              "Expected IllegalArgumentException for null linker");
      assertEquals(
          "Linker cannot be null",
          exception.getMessage(),
          "Exception message should indicate linker was null");
    }

  }

  @Nested
  @DisplayName("addPreview2ToLinker null checks")
  final class AddPreview2ToLinkerNullChecksTests {

    @Test
    @DisplayName("should reject null linker for preview2")
    void shouldRejectNullLinkerForPreview2() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiLinker.addPreview2ToLinker(null, null),
              "Expected IllegalArgumentException for null linker in preview2");
      assertEquals(
          "Linker cannot be null",
          exception.getMessage(),
          "Exception message should indicate linker was null for preview2");
    }

  }

  @Nested
  @DisplayName("createLinker null checks")
  final class CreateLinkerNullChecksTests {

    @Test
    @DisplayName("should reject null engine for createLinker")
    void shouldRejectNullEngineForCreateLinker() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiLinker.createLinker(null, null),
              "Expected IllegalArgumentException for null engine");
      assertEquals(
          "Engine cannot be null",
          exception.getMessage(),
          "Exception message should indicate engine was null");
    }
  }

  @Nested
  @DisplayName("createPreview2Linker null checks")
  final class CreatePreview2LinkerNullChecksTests {

    @Test
    @DisplayName("should reject null engine for createPreview2Linker")
    void shouldRejectNullEngineForCreatePreview2Linker() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiLinker.createPreview2Linker(null, null),
              "Expected IllegalArgumentException for null engine in preview2");
      assertEquals(
          "Engine cannot be null",
          exception.getMessage(),
          "Exception message should indicate engine was null for preview2 linker");
    }
  }

  @Nested
  @DisplayName("createFullLinker null checks")
  final class CreateFullLinkerNullChecksTests {

    @Test
    @DisplayName("should reject null engine for createFullLinker")
    void shouldRejectNullEngineForFullLinker() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiLinker.createFullLinker(null, null),
              "Expected IllegalArgumentException for null engine in full linker");
      assertEquals(
          "Engine cannot be null",
          exception.getMessage(),
          "Exception message should indicate engine was null for full linker");
    }
  }

  @Nested
  @DisplayName("hasWasiImports null checks")
  final class HasWasiImportsNullChecksTests {

    @Test
    @DisplayName("should reject null linker for hasWasiImports")
    void shouldRejectNullLinkerForHasWasiImports() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiLinker.hasWasiImports(null),
              "Expected IllegalArgumentException for null linker in hasWasiImports");
      assertEquals(
          "Linker cannot be null",
          exception.getMessage(),
          "Exception message should indicate linker was null for hasWasiImports");
    }
  }

  @Nested
  @DisplayName("hasWasiPreview2Imports null checks")
  final class HasWasiPreview2ImportsNullChecksTests {

    @Test
    @DisplayName("should reject null linker for hasWasiPreview2Imports")
    void shouldRejectNullLinkerForHasPreview2Imports() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiLinker.hasWasiPreview2Imports(null),
              "Expected IllegalArgumentException for null linker in hasWasiPreview2Imports");
      assertEquals(
          "Linker cannot be null",
          exception.getMessage(),
          "Exception message should indicate linker was null for hasWasiPreview2Imports");
    }
  }

  @Nested
  @DisplayName("hasComponentModelImports null checks")
  final class HasComponentModelImportsNullChecksTests {

    @Test
    @DisplayName("should reject null linker for hasComponentModelImports")
    void shouldRejectNullLinkerForHasComponentModelImports() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiLinker.hasComponentModelImports(null),
              "Expected IllegalArgumentException for null linker in hasComponentModelImports");
      assertEquals(
          "Linker cannot be null",
          exception.getMessage(),
          "Exception message should indicate linker was null for hasComponentModelImports");
    }
  }

  @Nested
  @DisplayName("addComponentModelToLinker null checks")
  final class AddComponentModelToLinkerNullChecksTests {

    @Test
    @DisplayName("should reject null linker for addComponentModelToLinker")
    void shouldRejectNullLinkerForAddComponentModel() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiLinker.addComponentModelToLinker(null),
              "Expected IllegalArgumentException for null linker in addComponentModelToLinker");
      assertEquals(
          "Linker cannot be null",
          exception.getMessage(),
          "Exception message should indicate linker was null for addComponentModelToLinker");
    }
  }

  @Nested
  @DisplayName("utility class constraints")
  final class UtilityClassConstraintsTests {

    @Test
    @DisplayName("should not be instantiable")
    void shouldNotBeInstantiable() {
      final Class<WasiLinker> clazz = WasiLinker.class;
      assertNotNull(clazz, "WasiLinker class should exist");
      assertDoesNotThrow(
          () -> {
            final java.lang.reflect.Constructor<WasiLinker> constructor =
                WasiLinker.class.getDeclaredConstructor();
            assertNotNull(constructor, "Private constructor should exist");
          },
          "WasiLinker should have a declared no-arg constructor");
    }
  }
}
