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
package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ExnRef;
import java.lang.foreign.MemorySegment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaExnRef} class.
 *
 * <p>PanamaExnRef provides Panama FFI implementation of WebAssembly exception reference handling.
 */
@DisplayName("PanamaExnRef Tests")
class PanamaExnRefTest {

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("PanamaExnRef should implement ExnRef interface")
    void panamaExnRefShouldImplementExnRefInterface() {
      assertTrue(
          ExnRef.class.isAssignableFrom(PanamaExnRef.class),
          "PanamaExnRef must implement the ExnRef interface");
    }
  }

  @Nested
  @DisplayName("Constructor Validation Tests")
  class ConstructorValidationTests {

    @Test
    @DisplayName("constructor should reject null nativeHandle")
    void constructorShouldRejectNullNativeHandle() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaExnRef(null, MemorySegment.ofAddress(1L)),
          "Constructor should reject null nativeHandle");
    }

    @Test
    @DisplayName("constructor should reject NULL MemorySegment")
    void constructorShouldRejectNullMemorySegment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaExnRef(MemorySegment.NULL, MemorySegment.ofAddress(1L)),
          "Constructor should reject MemorySegment.NULL nativeHandle");
    }
  }
}
