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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.config.EngineConfig;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Validation tests for {@link JniEngine} input checking.
 *
 * <p>These tests exercise Java-side validation that runs before any native call is made. Fake
 * handles are used intentionally — the validation logic under test never crosses the JNI boundary.
 */
@DisplayName("JniEngine Validation Tests")
final class JniEngineValidationTest {

  private static final long VALID_HANDLE = 0x12345678L;

  private JniEngine engine;

  @BeforeEach
  void setUp() {
    engine = new JniEngine(VALID_HANDLE);
  }

  @AfterEach
  void tearDown() {
    engine.markClosedForTesting();
  }

  @Nested
  @DisplayName("Constructor Validation")
  class ConstructorValidation {

    @Test
    @DisplayName("Zero handle should be rejected as null pointer")
    void zeroHandleShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> new JniEngine(0L));
      assertTrue(
          e.getMessage().contains("nativeHandle"), "Expected message to contain: nativeHandle");
      assertTrue(
          e.getMessage().contains("null pointer"), "Expected message to contain: null pointer");
    }

    @Test
    @DisplayName("Negative handle should be rejected as invalid")
    void negativeHandleShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> new JniEngine(-1L));
      assertTrue(
          e.getMessage().contains("nativeHandle"), "Expected message to contain: nativeHandle");
      assertTrue(
          e.getMessage().contains("negative value"), "Expected message to contain: negative value");
    }

    @Test
    @DisplayName("Large negative handle should be rejected")
    void largeNegativeHandleShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> new JniEngine(Long.MIN_VALUE));
      assertTrue(
          e.getMessage().contains("invalid native handle"),
          "Expected message to contain: invalid native handle");
    }
  }

  @Nested
  @DisplayName("compileModule Validation")
  class CompileModuleValidation {

    @Test
    @DisplayName("Null bytes should be rejected")
    void nullBytesShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> engine.compileModule(null));
      assertTrue(
          e.getMessage().contains("wasmBytes cannot be null"),
          "Expected message: wasmBytes cannot be null");
    }

    @Test
    @DisplayName("Empty bytes should be rejected")
    void emptyBytesShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> engine.compileModule(new byte[0]));
      assertTrue(
          e.getMessage().contains("wasmBytes cannot be empty"),
          "Expected message: wasmBytes cannot be empty");
    }
  }

  @Nested
  @DisplayName("compileModuleWithDwarf Validation")
  class CompileModuleWithDwarfValidation {

    @Test
    @DisplayName("Null wasmBytes should be rejected")
    void nullWasmBytesShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> engine.compileModuleWithDwarf(null, new byte[] {1}));
      assertTrue(
          e.getMessage().contains("wasmBytes cannot be null or empty"),
          "Expected message: wasmBytes cannot be null or empty");
    }

    @Test
    @DisplayName("Empty wasmBytes should be rejected")
    void emptyWasmBytesShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> engine.compileModuleWithDwarf(new byte[0], new byte[] {1}));
      assertTrue(
          e.getMessage().contains("wasmBytes cannot be null or empty"),
          "Expected message: wasmBytes cannot be null or empty");
    }

    @Test
    @DisplayName("Null dwarfPackage should be rejected")
    void nullDwarfPackageShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> engine.compileModuleWithDwarf(new byte[] {1}, null));
      assertTrue(
          e.getMessage().contains("dwarfPackage cannot be null or empty"),
          "Expected message: dwarfPackage cannot be null or empty");
    }

    @Test
    @DisplayName("Empty dwarfPackage should be rejected")
    void emptyDwarfPackageShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> engine.compileModuleWithDwarf(new byte[] {1}, new byte[0]));
      assertTrue(
          e.getMessage().contains("dwarfPackage cannot be null or empty"),
          "Expected message: dwarfPackage cannot be null or empty");
    }
  }

  @Nested
  @DisplayName("compileWat Validation")
  class CompileWatValidation {

    @Test
    @DisplayName("Null wat should be rejected")
    void nullWatShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> engine.compileWat(null));
      assertTrue(
          e.getMessage().contains("wat cannot be null"), "Expected message: wat cannot be null");
    }

    @Test
    @DisplayName("Empty wat should be rejected")
    void emptyWatShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> engine.compileWat(""));
      assertTrue(
          e.getMessage().contains("wat cannot be empty"), "Expected message: wat cannot be empty");
    }
  }

  @Nested
  @DisplayName("precompileModule Validation")
  class PrecompileModuleValidation {

    @Test
    @DisplayName("Null bytes should be rejected")
    void nullBytesShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> engine.precompileModule(null));
      assertTrue(
          e.getMessage().contains("wasmBytes cannot be null"),
          "Expected message: wasmBytes cannot be null");
    }

    @Test
    @DisplayName("Empty bytes should be rejected")
    void emptyBytesShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> engine.precompileModule(new byte[0]));
      assertTrue(
          e.getMessage().contains("wasmBytes cannot be empty"),
          "Expected message: wasmBytes cannot be empty");
    }
  }

  @Nested
  @DisplayName("precompileComponent Validation")
  class PrecompileComponentValidation {

    @Test
    @DisplayName("Null bytes should be rejected")
    void nullBytesShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> engine.precompileComponent(null));
      assertTrue(
          e.getMessage().contains("wasmBytes cannot be null"),
          "Expected message: wasmBytes cannot be null");
    }

    @Test
    @DisplayName("Empty bytes should be rejected")
    void emptyBytesShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> engine.precompileComponent(new byte[0]));
      assertTrue(
          e.getMessage().contains("wasmBytes cannot be empty"),
          "Expected message: wasmBytes cannot be empty");
    }
  }

  @Nested
  @DisplayName("createSharedMemory Validation")
  class CreateSharedMemoryValidation {

    @Test
    @DisplayName("Negative initial pages should be rejected")
    void negativeInitialPagesShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> engine.createSharedMemory(-1, 10));
      assertTrue(
          e.getMessage().contains("Initial pages"), "Expected message to contain: Initial pages");
      assertTrue(e.getMessage().contains("negative"), "Expected message to contain: negative");
    }

    @Test
    @DisplayName("Zero max pages should be rejected")
    void zeroMaxPagesShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> engine.createSharedMemory(0, 0));
      assertTrue(
          e.getMessage().contains("positive maximum page count"),
          "Expected message to contain: positive maximum page count");
    }

    @Test
    @DisplayName("Negative max pages should be rejected")
    void negativeMaxPagesShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> engine.createSharedMemory(0, -1));
      assertTrue(
          e.getMessage().contains("positive maximum page count"),
          "Expected message to contain: positive maximum page count");
    }

    @Test
    @DisplayName("Max pages less than initial pages should be rejected")
    void maxPagesLessThanInitialShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> engine.createSharedMemory(10, 5));
      assertTrue(
          e.getMessage().contains("cannot be less than initial pages"),
          "Expected message to contain: cannot be less than initial pages");
    }
  }

  @Nested
  @DisplayName("supportsFeature Validation")
  class SupportsFeatureValidation {

    @Test
    @DisplayName("Null feature should return false without exception")
    void nullFeatureShouldReturnFalse() {
      assertFalse(engine.supportsFeature(null));
    }
  }

  @Nested
  @DisplayName("same Validation")
  class SameValidation {

    @Test
    @DisplayName("Null engine should be rejected")
    void nullEngineShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> engine.same(null));
      assertTrue(
          e.getMessage().contains("other cannot be null"),
          "Expected message: other cannot be null");
    }
  }

  @Nested
  @DisplayName("detectHostFeature Validation")
  class DetectHostFeatureValidation {

    @Test
    @DisplayName("Null feature should be rejected")
    void nullFeatureShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> engine.detectHostFeature(null));
      assertTrue(
          e.getMessage().contains("feature cannot be null"),
          "Expected message: feature cannot be null");
    }
  }

  @Nested
  @DisplayName("detectPrecompiled Validation")
  class DetectPrecompiledValidation {

    @Test
    @DisplayName("Null bytes should be rejected")
    void nullBytesShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> engine.detectPrecompiled(null));
      assertTrue(
          e.getMessage().contains("bytes cannot be null"),
          "Expected message: bytes cannot be null");
    }

    @Test
    @DisplayName("Empty bytes should return null")
    void emptyBytesShouldReturnNull() {
      assertNull(engine.detectPrecompiled(new byte[0]));
    }
  }

  @Nested
  @DisplayName("createGuestProfiler Validation")
  class CreateGuestProfilerValidation {

    @Test
    @DisplayName("Null module name should be rejected")
    void nullModuleNameShouldBeRejected() {
      Map<String, ai.tegmentum.wasmtime4j.Module> modules = new java.util.HashMap<>();
      modules.put("mod", new JniModule(VALID_HANDLE, engine));
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> engine.createGuestProfiler(null, Duration.ofMillis(10), modules));
      assertTrue(
          e.getMessage().contains("moduleName cannot be null or empty"),
          "Expected message: moduleName cannot be null or empty");
    }

    @Test
    @DisplayName("Empty module name should be rejected")
    void emptyModuleNameShouldBeRejected() {
      Map<String, ai.tegmentum.wasmtime4j.Module> modules = new java.util.HashMap<>();
      modules.put("mod", new JniModule(VALID_HANDLE, engine));
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> engine.createGuestProfiler("", Duration.ofMillis(10), modules));
      assertTrue(
          e.getMessage().contains("moduleName cannot be null or empty"),
          "Expected message: moduleName cannot be null or empty");
    }

    @Test
    @DisplayName("Null interval should be rejected")
    void nullIntervalShouldBeRejected() {
      Map<String, ai.tegmentum.wasmtime4j.Module> modules = new java.util.HashMap<>();
      modules.put("mod", new JniModule(VALID_HANDLE, engine));
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> engine.createGuestProfiler("test", null, modules));
      assertTrue(
          e.getMessage().contains("interval cannot be null"),
          "Expected message: interval cannot be null");
    }

    @Test
    @DisplayName("Null modules should be rejected")
    void nullModulesShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> engine.createGuestProfiler("test", Duration.ofMillis(10), null));
      assertTrue(
          e.getMessage().contains("modules cannot be null or empty"),
          "Expected message: modules cannot be null or empty");
    }

    @Test
    @DisplayName("Empty modules should be rejected")
    void emptyModulesShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  engine.createGuestProfiler(
                      "test", Duration.ofMillis(10), Collections.emptyMap()));
      assertTrue(
          e.getMessage().contains("modules cannot be null or empty"),
          "Expected message: modules cannot be null or empty");
    }
  }

  @Nested
  @DisplayName("createComponentGuestProfiler Validation")
  class CreateComponentGuestProfilerValidation {

    @Test
    @DisplayName("Null component name should be rejected")
    void nullComponentNameShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> engine.createComponentGuestProfiler(null, Duration.ofMillis(10), null, null));
      assertTrue(
          e.getMessage().contains("componentName cannot be null or empty"),
          "Expected message: componentName cannot be null or empty");
    }

    @Test
    @DisplayName("Empty component name should be rejected")
    void emptyComponentNameShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> engine.createComponentGuestProfiler("", Duration.ofMillis(10), null, null));
      assertTrue(
          e.getMessage().contains("componentName cannot be null or empty"),
          "Expected message: componentName cannot be null or empty");
    }

    @Test
    @DisplayName("Null interval should be rejected")
    void nullIntervalShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> engine.createComponentGuestProfiler("test", null, null, null));
      assertTrue(
          e.getMessage().contains("interval cannot be null"),
          "Expected message: interval cannot be null");
    }

    @Test
    @DisplayName("Null component should be rejected")
    void nullComponentShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> engine.createComponentGuestProfiler("test", Duration.ofMillis(10), null, null));
      assertTrue(
          e.getMessage().contains("component cannot be null"),
          "Expected message: component cannot be null");
    }
  }

  @Nested
  @DisplayName("createWithConfig Validation")
  class CreateWithConfigValidation {

    @Test
    @DisplayName("Null config should be rejected")
    void nullConfigShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> JniEngine.createWithConfig(null, null));
      assertTrue(
          e.getMessage().contains("config cannot be null"),
          "Expected message: config cannot be null");
    }
  }

  @Nested
  @DisplayName("getConfig Validation")
  class GetConfigValidation {

    @Test
    @DisplayName("Should return default config when none was set")
    void shouldReturnDefaultConfigWhenNoneSet() {
      EngineConfig config = engine.getConfig();
      assertNotNull(config, "getConfig should return a non-null default EngineConfig");
    }

    @Test
    @DisplayName("Should return configured config when set")
    void shouldReturnConfiguredConfig() {
      EngineConfig customConfig = new EngineConfig();
      JniEngine configuredEngine = new JniEngine(VALID_HANDLE, null, customConfig);
      assertEquals(
          customConfig, configuredEngine.getConfig(), "Should return the configured config");
      configuredEngine.markClosedForTesting();
    }
  }

  @Nested
  @DisplayName("getResourceType Validation")
  class GetResourceTypeValidation {

    @Test
    @DisplayName("Should return JniEngine")
    void shouldReturnJniEngine() {
      assertEquals("JniEngine", engine.getResourceType());
    }
  }

  @Nested
  @DisplayName("isValid Validation")
  class IsValidValidation {

    @Test
    @DisplayName("Should return true when engine is not closed")
    void shouldReturnTrueWhenOpen() {
      assertTrue(engine.isValid());
    }

    @Test
    @DisplayName("Should return false when engine is closed")
    void shouldReturnFalseWhenClosed() {
      engine.markClosedForTesting();
      assertFalse(engine.isValid());
      // Re-create for tearDown
      engine = new JniEngine(VALID_HANDLE);
    }
  }
}
