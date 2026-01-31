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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmRuntimeBuilder}.
 *
 * <p>Verifies class structure, constructor, and build behavior (currently a stub).
 */
@DisplayName("WasmRuntimeBuilder Tests")
class WasmRuntimeBuilderTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasmRuntimeBuilder.class.getModifiers()),
          "WasmRuntimeBuilder should be a final class");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create instance with default constructor")
    void shouldCreateInstanceWithDefaultConstructor() {
      final WasmRuntimeBuilder builder = new WasmRuntimeBuilder();
      assertNotNull(builder, "WasmRuntimeBuilder should be created successfully");
    }
  }

  @Nested
  @DisplayName("Build Tests")
  class BuildTests {

    @Test
    @DisplayName("build should throw UnsupportedOperationException as stub")
    void buildShouldThrowUnsupportedOperationException() {
      final WasmRuntimeBuilder builder = new WasmRuntimeBuilder();
      assertThrows(
          UnsupportedOperationException.class,
          () -> builder.build(),
          "build() should throw UnsupportedOperationException");
    }
  }
}
