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

package ai.tegmentum.wasmtime4j.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmRuntimeFactory} - factory for creating WebAssembly runtime instances.
 *
 * <p>Validates utility class structure, runtime selection logic, Java version detection, and cache
 * management. No native runtime implementations are available in the test environment.
 */
@DisplayName("WasmRuntimeFactory Tests")
class WasmRuntimeFactoryTest {

  @AfterEach
  void cleanUp() {
    WasmRuntimeFactory.clearCache();
    System.clearProperty(WasmRuntimeFactory.RUNTIME_PROPERTY);
  }

  @Nested
  @DisplayName("Utility Class Structure Tests")
  class UtilityClassStructureTests {

    @Test
    @DisplayName("class should be final")
    void classShouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasmRuntimeFactory.class.getModifiers()),
          "WasmRuntimeFactory should be a final class");
    }

    @Test
    @DisplayName("constructor should be private")
    void constructorShouldBePrivate() throws NoSuchMethodException {
      final java.lang.reflect.Constructor<WasmRuntimeFactory> constructor =
          WasmRuntimeFactory.class.getDeclaredConstructor();
      assertTrue(
          java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
          "WasmRuntimeFactory constructor should be private");
    }
  }

  @Nested
  @DisplayName("Constants Tests")
  class ConstantsTests {

    @Test
    @DisplayName("RUNTIME_PROPERTY should be correct")
    void runtimePropertyShouldBeCorrect() {
      assertEquals(
          "wasmtime4j.runtime",
          WasmRuntimeFactory.RUNTIME_PROPERTY,
          "RUNTIME_PROPERTY should be 'wasmtime4j.runtime'");
    }

    @Test
    @DisplayName("RUNTIME_JNI should be correct")
    void runtimeJniShouldBeCorrect() {
      assertEquals(
          "jni",
          WasmRuntimeFactory.RUNTIME_JNI,
          "RUNTIME_JNI should be 'jni'");
    }

    @Test
    @DisplayName("RUNTIME_PANAMA should be correct")
    void runtimePanamaShouldBeCorrect() {
      assertEquals(
          "panama",
          WasmRuntimeFactory.RUNTIME_PANAMA,
          "RUNTIME_PANAMA should be 'panama'");
    }
  }

  @Nested
  @DisplayName("GetSelectedRuntimeType Tests")
  class GetSelectedRuntimeTypeTests {

    @Test
    @DisplayName("getSelectedRuntimeType should return non-null")
    void getSelectedRuntimeTypeShouldReturnNonNull() {
      final RuntimeType type = WasmRuntimeFactory.getSelectedRuntimeType();
      assertNotNull(type, "getSelectedRuntimeType should return a non-null RuntimeType");
    }

    @Test
    @DisplayName("getSelectedRuntimeType should return valid enum value")
    void getSelectedRuntimeTypeShouldReturnValidEnum() {
      final RuntimeType type = WasmRuntimeFactory.getSelectedRuntimeType();
      assertTrue(
          type == RuntimeType.JNI || type == RuntimeType.PANAMA,
          "Selected runtime should be JNI or PANAMA, got: " + type);
    }

    @Test
    @DisplayName("system property override should select JNI")
    void systemPropertyOverrideShouldSelectJni() {
      System.setProperty(WasmRuntimeFactory.RUNTIME_PROPERTY, "jni");
      WasmRuntimeFactory.clearCache();
      final RuntimeType type = WasmRuntimeFactory.getSelectedRuntimeType();
      assertEquals(
          RuntimeType.JNI,
          type,
          "System property 'jni' should select JNI runtime");
    }

    @Test
    @DisplayName("system property override should select Panama")
    void systemPropertyOverrideShouldSelectPanama() {
      System.setProperty(WasmRuntimeFactory.RUNTIME_PROPERTY, "panama");
      WasmRuntimeFactory.clearCache();
      final RuntimeType type = WasmRuntimeFactory.getSelectedRuntimeType();
      assertEquals(
          RuntimeType.PANAMA,
          type,
          "System property 'panama' should select Panama runtime");
    }
  }

  @Nested
  @DisplayName("IsRuntimeAvailable Tests")
  class IsRuntimeAvailableTests {

    @Test
    @DisplayName("isRuntimeAvailable should not throw for JNI")
    void isRuntimeAvailableShouldNotThrowForJni() {
      // Should not throw even if runtime is not available
      final boolean available = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
      assertNotNull(Boolean.valueOf(available), "isRuntimeAvailable should return a boolean");
    }

    @Test
    @DisplayName("isRuntimeAvailable should not throw for Panama")
    void isRuntimeAvailableShouldNotThrowForPanama() {
      final boolean available = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);
      assertNotNull(Boolean.valueOf(available), "isRuntimeAvailable should return a boolean");
    }
  }

  @Nested
  @DisplayName("GetJavaVersion Tests")
  class GetJavaVersionTests {

    @Test
    @DisplayName("getJavaVersion should return positive value")
    void getJavaVersionShouldReturnPositiveValue() {
      final int version = WasmRuntimeFactory.getJavaVersion();
      assertTrue(version > 0, "Java version should be positive, got: " + version);
    }

    @Test
    @DisplayName("getJavaVersion should return reasonable version number")
    void getJavaVersionShouldReturnReasonableVersion() {
      final int version = WasmRuntimeFactory.getJavaVersion();
      assertTrue(
          version >= 8 && version <= 99,
          "Java version should be between 8 and 99, got: " + version);
    }
  }

  @Nested
  @DisplayName("ClearCache Tests")
  class ClearCacheTests {

    @Test
    @DisplayName("clearCache should not throw")
    void clearCacheShouldNotThrow() {
      WasmRuntimeFactory.clearCache();
      // If we get here without exception, the test passes
      assertNotNull(
          WasmRuntimeFactory.getSelectedRuntimeType(),
          "Runtime type should be selectable after cache clear");
    }

    @Test
    @DisplayName("clearCache should allow re-evaluation of runtime type")
    void clearCacheShouldAllowReEvaluation() {
      final RuntimeType before = WasmRuntimeFactory.getSelectedRuntimeType();
      WasmRuntimeFactory.clearCache();
      final RuntimeType after = WasmRuntimeFactory.getSelectedRuntimeType();
      assertNotNull(after, "Runtime type should be selectable after cache clear");
      assertEquals(
          before,
          after,
          "Re-evaluation with same conditions should produce same result");
    }
  }

  @Nested
  @DisplayName("Create Tests")
  class CreateTests {

    @Test
    @DisplayName("create should throw WasmException when no runtime available")
    void createShouldThrowWhenNoRuntimeAvailable() {
      // Without native implementations on classpath, create should fail
      assertThrows(
          WasmException.class,
          WasmRuntimeFactory::create,
          "create() should throw when no runtime implementation is available");
    }

    @Test
    @DisplayName("create with null should throw IllegalArgumentException")
    void createWithNullShouldThrowIllegalArgumentException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasmRuntimeFactory.create(null),
          "create(null) should throw IllegalArgumentException");
    }
  }
}
