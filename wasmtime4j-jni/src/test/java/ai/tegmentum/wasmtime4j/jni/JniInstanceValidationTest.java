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
package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Validation tests for {@link JniInstance}.
 *
 * <p>These tests exercise Java-side validation code paths that run before any native call is made.
 * Fake handles are used intentionally since all validation under test occurs in Java.
 */
@DisplayName("JniInstance Validation Tests")
final class JniInstanceValidationTest {

  private static final long VALID_HANDLE = 0x12345678L;

  private JniEngine testEngine;
  private JniStore testStore;
  private JniModule testModule;
  private JniInstance testInstance;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
    testStore = new JniStore(VALID_HANDLE, testEngine);
    testModule = new JniModule(VALID_HANDLE, testEngine);
    testInstance = new JniInstance(VALID_HANDLE, testModule, testStore);
  }

  @AfterEach
  void tearDown() {
    testInstance.markClosedForTesting();
    testModule.markClosedForTesting();
    testStore.markClosedForTesting();
    testEngine.markClosedForTesting();
  }

  @Nested
  @DisplayName("Constructor Validation")
  class ConstructorValidation {

    @Test
    @DisplayName("should reject null module")
    void shouldRejectNullModule() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> new JniInstance(VALID_HANDLE, null, testStore));
      assertTrue(e.getMessage().contains("module"), "Expected message to contain: module");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }

    @Test
    @DisplayName("should reject null store")
    void shouldRejectNullStore() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> new JniInstance(VALID_HANDLE, testModule, null));
      assertTrue(e.getMessage().contains("store"), "Expected message to contain: store");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }
  }

  @Nested
  @DisplayName("getFunction Validation")
  class GetFunctionValidation {

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testInstance.getFunction(null));
      assertTrue(e.getMessage().contains("name"), "Expected message to contain: name");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }

    @Test
    @DisplayName("should reject blank name")
    void shouldRejectBlankName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testInstance.getFunction("   "));
      assertTrue(e.getMessage().contains("name"), "Expected message to contain: name");
      assertTrue(
          e.getMessage().contains("must not be empty or whitespace-only"),
          "Expected message to contain: must not be empty or whitespace-only");
    }
  }

  @Nested
  @DisplayName("getMemory Validation")
  class GetMemoryValidation {

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testInstance.getMemory(null));
      assertTrue(e.getMessage().contains("name"), "Expected message to contain: name");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }

    @Test
    @DisplayName("should reject blank name")
    void shouldRejectBlankName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testInstance.getMemory("  "));
      assertTrue(e.getMessage().contains("name"), "Expected message to contain: name");
      assertTrue(
          e.getMessage().contains("must not be empty or whitespace-only"),
          "Expected message to contain: must not be empty or whitespace-only");
    }
  }

  @Nested
  @DisplayName("getTable Validation")
  class GetTableValidation {

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testInstance.getTable(null));
      assertTrue(e.getMessage().contains("name"), "Expected message to contain: name");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }

    @Test
    @DisplayName("should reject blank name")
    void shouldRejectBlankName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testInstance.getTable("  "));
      assertTrue(e.getMessage().contains("name"), "Expected message to contain: name");
      assertTrue(
          e.getMessage().contains("must not be empty or whitespace-only"),
          "Expected message to contain: must not be empty or whitespace-only");
    }
  }

  @Nested
  @DisplayName("getGlobal Validation")
  class GetGlobalValidation {

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testInstance.getGlobal(null));
      assertTrue(e.getMessage().contains("name"), "Expected message to contain: name");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }

    @Test
    @DisplayName("should reject blank name")
    void shouldRejectBlankName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testInstance.getGlobal("  "));
      assertTrue(e.getMessage().contains("name"), "Expected message to contain: name");
      assertTrue(
          e.getMessage().contains("must not be empty or whitespace-only"),
          "Expected message to contain: must not be empty or whitespace-only");
    }
  }

  @Nested
  @DisplayName("getTag Validation")
  class GetTagValidation {

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testInstance.getTag(null));
      assertTrue(e.getMessage().contains("name"), "Expected message to contain: name");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }
  }

  @Nested
  @DisplayName("getSharedMemory Validation")
  class GetSharedMemoryValidation {

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testInstance.getSharedMemory(null));
      assertTrue(e.getMessage().contains("name"), "Expected message to contain: name");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }
  }

  @Nested
  @DisplayName("hasExport Validation")
  class HasExportValidation {

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testInstance.hasExport(null));
      assertTrue(e.getMessage().contains("name"), "Expected message to contain: name");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }

    @Test
    @DisplayName("should reject blank name")
    void shouldRejectBlankName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testInstance.hasExport("  "));
      assertTrue(e.getMessage().contains("name"), "Expected message to contain: name");
      assertTrue(
          e.getMessage().contains("must not be empty or whitespace-only"),
          "Expected message to contain: must not be empty or whitespace-only");
    }
  }

  @Nested
  @DisplayName("getExport Validation")
  class GetExportValidation {

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testInstance.getExport(null));
      assertTrue(e.getMessage().contains("name"), "Expected message to contain: name");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }
  }

  @Nested
  @DisplayName("callFunction Validation")
  class CallFunctionValidation {

    @Test
    @DisplayName("should reject null function name")
    void shouldRejectNullFunctionName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testInstance.callFunction(null));
      assertTrue(
          e.getMessage().contains("functionName"), "Expected message to contain: functionName");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }

    @Test
    @DisplayName("should reject blank function name")
    void shouldRejectBlankFunctionName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testInstance.callFunction("  "));
      assertTrue(
          e.getMessage().contains("functionName"), "Expected message to contain: functionName");
      assertTrue(
          e.getMessage().contains("must not be empty or whitespace-only"),
          "Expected message to contain: must not be empty or whitespace-only");
    }

    @Test
    @DisplayName("should reject null params array")
    void shouldRejectNullParams() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> testInstance.callFunction("test", (ai.tegmentum.wasmtime4j.WasmValue[]) null));
      assertTrue(e.getMessage().contains("params"), "Expected message to contain: params");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }
  }

  @Nested
  @DisplayName("getExport with ModuleExport Validation")
  class GetExportWithModuleExportValidation {

    @Test
    @DisplayName("should reject null store")
    void shouldRejectNullStore() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testInstance.getExport(null, null));
      assertTrue(
          e.getMessage().contains("Store cannot be null"),
          "Expected message to contain: Store cannot be null");
    }

    @Test
    @DisplayName("should reject null moduleExport")
    void shouldRejectNullModuleExport() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> testInstance.getExport(testStore, null));
      assertTrue(
          e.getMessage().contains("ModuleExport cannot be null"),
          "Expected message to contain: ModuleExport cannot be null");
    }
  }
}
