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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Store;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Validation tests for {@link JniModule}.
 *
 * <p>These tests exercise Java-side validation code paths that run before any native call is made.
 * Fake handles are used intentionally since all validation under test occurs in Java.
 */
@DisplayName("JniModule Validation Tests")
final class JniModuleValidationTest {

  private static final long VALID_HANDLE = 0x12345678L;

  private JniEngine testEngine;
  private JniModule testModule;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
    testModule = new JniModule(VALID_HANDLE, testEngine);
  }

  @AfterEach
  void tearDown() {
    testModule.markClosedForTesting();
    testEngine.markClosedForTesting();
  }

  @Nested
  @DisplayName("getEngine")
  class GetEngine {

    @Test
    @DisplayName("should return the engine passed to constructor")
    void shouldReturnConstructorEngine() {
      assertSame(testEngine, testModule.getEngine());
    }
  }

  @Nested
  @DisplayName("instantiate")
  class Instantiate {

    @Test
    @DisplayName("should reject null store")
    void shouldRejectNullStore() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testModule.instantiate(null));
      assertTrue(
          e.getMessage().contains("store cannot be null"),
          "Expected message to contain: store cannot be null");
    }

    @Test
    @DisplayName("should reject null imports")
    void shouldRejectNullImports() {
      JniStore jniStore = new JniStore(VALID_HANDLE, testEngine);
      try {
        IllegalArgumentException e =
            assertThrows(
                IllegalArgumentException.class, () -> testModule.instantiate(jniStore, null));
        assertTrue(
            e.getMessage().contains("imports cannot be null"),
            "Expected message to contain: imports cannot be null");
      } finally {
        jniStore.markClosedForTesting();
      }
    }

    @Test
    @DisplayName("should reject non-JniStore instance")
    void shouldRejectNonJniStore() {
      Store nonJniStore = createFakeStore();
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testModule.instantiate(nonJniStore));
      assertTrue(
          e.getMessage().contains("store must be a JniStore instance"),
          "Expected message to contain: store must be a JniStore instance");
    }
  }

  @Nested
  @DisplayName("getResourceType")
  class GetResourceType {

    @Test
    @DisplayName("should return JniModule")
    void shouldReturnJniModule() {
      assertEquals("JniModule", testModule.getResourceType());
    }
  }

  @Nested
  @DisplayName("isValid")
  class IsValid {

    @Test
    @DisplayName("should return true when not closed")
    void shouldReturnTrueWhenOpen() {
      assertTrue(testModule.isValid());
    }
  }

  @Nested
  @DisplayName("hasExport")
  class HasExport {

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testModule.hasExport(null));
      assertTrue(
          e.getMessage().contains("cannot be null"), "Expected message to contain: cannot be null");
    }
  }

  @Nested
  @DisplayName("hasImport")
  class HasImport {

    @Test
    @DisplayName("should reject null module name")
    void shouldRejectNullModuleName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testModule.hasImport(null, "field"));
      assertTrue(
          e.getMessage().contains("Module name cannot be null"),
          "Expected message to contain: Module name cannot be null");
    }

    @Test
    @DisplayName("should reject null field name")
    void shouldRejectNullFieldName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testModule.hasImport("module", null));
      assertTrue(
          e.getMessage().contains("Field name cannot be null"),
          "Expected message to contain: Field name cannot be null");
    }
  }

  @Nested
  @DisplayName("serialize")
  class Serialize {

    @Test
    @DisplayName("should throw when module is closed")
    void shouldThrowWhenClosed() {
      JniModule closedModule = new JniModule(VALID_HANDLE, testEngine);
      closedModule.markClosedForTesting();
      assertThrows(IllegalStateException.class, closedModule::serialize);
    }
  }

  @Nested
  @DisplayName("validateImports")
  class ValidateImports {

    @Test
    @DisplayName("should reject null imports")
    void shouldRejectNullImports() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testModule.validateImports(null));
      assertTrue(
          e.getMessage().contains("imports cannot be null"),
          "Expected message to contain: imports cannot be null");
    }
  }

  @Nested
  @DisplayName("same")
  class Same {

    @Test
    @DisplayName("should reject null other module")
    void shouldRejectNullOther() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testModule.same(null));
      assertTrue(
          e.getMessage().contains("other cannot be null"),
          "Expected message to contain: other cannot be null");
    }
  }

  @Nested
  @DisplayName("getExportIndex")
  class GetExportIndex {

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testModule.getExportIndex(null));
      assertTrue(
          e.getMessage().contains("name cannot be null"),
          "Expected message to contain: name cannot be null");
    }
  }

  @Nested
  @DisplayName("getModuleExport")
  class GetModuleExport {

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testModule.getModuleExport(null));
      assertTrue(
          e.getMessage().contains("name cannot be null"),
          "Expected message to contain: name cannot be null");
    }
  }

  @Nested
  @DisplayName("validateImportsDetailed")
  class ValidateImportsDetailed {

    @Test
    @DisplayName("should reject null imports")
    void shouldRejectNullImports() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> testModule.validateImportsDetailed(null));
      assertTrue(
          e.getMessage().contains("imports cannot be null"),
          "Expected message to contain: imports cannot be null");
    }
  }

  /**
   * Creates a fake Store proxy for testing non-JniStore rejection. The proxy implements Store but
   * is not a JniStore, which triggers the type check in JniModule.instantiate().
   */
  private static Store createFakeStore() {
    return (Store)
        Proxy.newProxyInstance(
            Store.class.getClassLoader(),
            new Class<?>[] {Store.class},
            (proxy, method, args) -> null);
  }
}
