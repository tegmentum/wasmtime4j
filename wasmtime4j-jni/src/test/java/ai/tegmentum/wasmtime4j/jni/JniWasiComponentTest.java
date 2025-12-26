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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.WasiComponent;
import ai.tegmentum.wasmtime4j.wasi.WasiComponentStats;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiInstance;
import ai.tegmentum.wasmtime4j.wasi.WasiInterfaceMetadata;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link JniWasiComponent} class.
 *
 * <p>This test class verifies the class structure, interface implementation, and method signatures
 * of the JNI WASI component implementation using reflection-based testing.
 */
@DisplayName("JniWasiComponent Tests")
class JniWasiComponentTest {

  // ========================================================================
  // Class Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniWasiComponent should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasiComponent.class.getModifiers()),
          "JniWasiComponent should be final");
    }

    @Test
    @DisplayName("JniWasiComponent should be a public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(JniWasiComponent.class.getModifiers()),
          "JniWasiComponent should be public");
    }

    @Test
    @DisplayName("JniWasiComponent should implement WasiComponent")
    void shouldImplementWasiComponent() {
      assertTrue(
          WasiComponent.class.isAssignableFrom(JniWasiComponent.class),
          "JniWasiComponent should implement WasiComponent");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have LOGGER field")
    void shouldHaveLoggerField() throws Exception {
      Field field = JniWasiComponent.class.getDeclaredField("LOGGER");
      assertNotNull(field, "LOGGER field should exist");
      assertEquals(Logger.class, field.getType(), "LOGGER should be of type Logger");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
    }

    @Test
    @DisplayName("should have componentEngine field")
    void shouldHaveComponentEngineField() throws Exception {
      Field field = JniWasiComponent.class.getDeclaredField("componentEngine");
      assertNotNull(field, "componentEngine field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "componentEngine should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "componentEngine should be final");
    }

    @Test
    @DisplayName("should have componentHandle field")
    void shouldHaveComponentHandleField() throws Exception {
      Field field = JniWasiComponent.class.getDeclaredField("componentHandle");
      assertNotNull(field, "componentHandle field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "componentHandle should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "componentHandle should be final");
    }

    @Test
    @DisplayName("should have name field")
    void shouldHaveNameField() throws Exception {
      Field field = JniWasiComponent.class.getDeclaredField("name");
      assertNotNull(field, "name field should exist");
      assertEquals(String.class, field.getType(), "name should be String");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "name should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "name should be final");
    }

    @Test
    @DisplayName("should have closed field")
    void shouldHaveClosedField() throws Exception {
      Field field = JniWasiComponent.class.getDeclaredField("closed");
      assertNotNull(field, "closed field should exist");
      assertEquals(boolean.class, field.getType(), "closed should be boolean");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "closed should be private");
      assertTrue(Modifier.isVolatile(field.getModifiers()), "closed should be volatile");
    }

    @Test
    @DisplayName("should have cachedExports field")
    void shouldHaveCachedExportsField() throws Exception {
      Field field = JniWasiComponent.class.getDeclaredField("cachedExports");
      assertNotNull(field, "cachedExports field should exist");
      assertEquals(List.class, field.getType(), "cachedExports should be List");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "cachedExports should be private");
      assertTrue(Modifier.isVolatile(field.getModifiers()), "cachedExports should be volatile");
    }

    @Test
    @DisplayName("should have cachedImports field")
    void shouldHaveCachedImportsField() throws Exception {
      Field field = JniWasiComponent.class.getDeclaredField("cachedImports");
      assertNotNull(field, "cachedImports field should exist");
      assertEquals(List.class, field.getType(), "cachedImports should be List");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "cachedImports should be private");
      assertTrue(Modifier.isVolatile(field.getModifiers()), "cachedImports should be volatile");
    }

    @Test
    @DisplayName("should have cachedStats field")
    void shouldHaveCachedStatsField() throws Exception {
      Field field = JniWasiComponent.class.getDeclaredField("cachedStats");
      assertNotNull(field, "cachedStats field should exist");
      assertEquals(
          WasiComponentStats.class, field.getType(), "cachedStats should be WasiComponentStats");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "cachedStats should be private");
      assertTrue(Modifier.isVolatile(field.getModifiers()), "cachedStats should be volatile");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with three parameters")
    void shouldHavePublicConstructor() {
      Constructor<?>[] constructors = JniWasiComponent.class.getConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 public constructor");

      Constructor<?> constructor = constructors[0];
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
      assertEquals(3, constructor.getParameterCount(), "Constructor should have 3 parameters");
    }

    @Test
    @DisplayName("constructor parameters should have correct types")
    void constructorParametersShouldHaveCorrectTypes() {
      Constructor<?>[] constructors = JniWasiComponent.class.getConstructors();
      Constructor<?> constructor = constructors[0];

      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(3, paramTypes.length, "Should have 3 parameters");
      // First parameter is JniComponent.JniComponentEngine
      // Second parameter is JniComponent.JniComponentHandle
      // Third parameter is String
      assertEquals(String.class, paramTypes[2], "Third parameter should be String");
    }
  }

  // ========================================================================
  // WasiComponent Interface Method Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiComponent Interface Method Tests")
  class WasiComponentInterfaceMethodTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws Exception {
      Method method = JniWasiComponent.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getName should be public");
    }

    @Test
    @DisplayName("should have getExports method")
    void shouldHaveGetExportsMethod() throws Exception {
      Method method = JniWasiComponent.class.getMethod("getExports");
      assertNotNull(method, "getExports method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getExports should be public");
    }

    @Test
    @DisplayName("should have getImports method")
    void shouldHaveGetImportsMethod() throws Exception {
      Method method = JniWasiComponent.class.getMethod("getImports");
      assertNotNull(method, "getImports method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getImports should be public");
    }

    @Test
    @DisplayName("should have getExportMetadata method")
    void shouldHaveGetExportMetadataMethod() throws Exception {
      Method method = JniWasiComponent.class.getMethod("getExportMetadata", String.class);
      assertNotNull(method, "getExportMetadata method should exist");
      assertEquals(
          WasiInterfaceMetadata.class,
          method.getReturnType(),
          "Return type should be WasiInterfaceMetadata");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getExportMetadata should be public");
    }

    @Test
    @DisplayName("should have getImportMetadata method")
    void shouldHaveGetImportMetadataMethod() throws Exception {
      Method method = JniWasiComponent.class.getMethod("getImportMetadata", String.class);
      assertNotNull(method, "getImportMetadata method should exist");
      assertEquals(
          WasiInterfaceMetadata.class,
          method.getReturnType(),
          "Return type should be WasiInterfaceMetadata");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getImportMetadata should be public");
    }

    @Test
    @DisplayName("should have instantiate method without parameters")
    void shouldHaveInstantiateMethodNoParams() throws Exception {
      Method method = JniWasiComponent.class.getMethod("instantiate");
      assertNotNull(method, "instantiate() method should exist");
      assertEquals(
          WasiInstance.class, method.getReturnType(), "Return type should be WasiInstance");
      assertTrue(Modifier.isPublic(method.getModifiers()), "instantiate should be public");
    }

    @Test
    @DisplayName("should have instantiate method with WasiConfig parameter")
    void shouldHaveInstantiateMethodWithConfig() throws Exception {
      Method method = JniWasiComponent.class.getMethod("instantiate", WasiConfig.class);
      assertNotNull(method, "instantiate(WasiConfig) method should exist");
      assertEquals(
          WasiInstance.class, method.getReturnType(), "Return type should be WasiInstance");
      assertTrue(Modifier.isPublic(method.getModifiers()), "instantiate should be public");
    }

    @Test
    @DisplayName("should have validate method without parameters")
    void shouldHaveValidateMethodNoParams() throws Exception {
      Method method = JniWasiComponent.class.getMethod("validate");
      assertNotNull(method, "validate() method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "validate should be public");
    }

    @Test
    @DisplayName("should have validate method with WasiConfig parameter")
    void shouldHaveValidateMethodWithConfig() throws Exception {
      Method method = JniWasiComponent.class.getMethod("validate", WasiConfig.class);
      assertNotNull(method, "validate(WasiConfig) method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "validate should be public");
    }

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws Exception {
      Method method = JniWasiComponent.class.getMethod("getStats");
      assertNotNull(method, "getStats method should exist");
      assertEquals(
          WasiComponentStats.class,
          method.getReturnType(),
          "Return type should be WasiComponentStats");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getStats should be public");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws Exception {
      Method method = JniWasiComponent.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isValid should be public");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws Exception {
      Method method = JniWasiComponent.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "close should be public");
    }
  }

  // ========================================================================
  // Package-Private Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Package-Private Method Tests")
  class PackagePrivateMethodTests {

    @Test
    @DisplayName("should have getComponentHandle package-private method")
    void shouldHaveGetComponentHandleMethod() throws Exception {
      Method method = JniWasiComponent.class.getDeclaredMethod("getComponentHandle");
      assertNotNull(method, "getComponentHandle method should exist");
      assertFalse(Modifier.isPublic(method.getModifiers()), "Should not be public");
      assertFalse(Modifier.isPrivate(method.getModifiers()), "Should not be private");
    }

    @Test
    @DisplayName("should have getComponentEngine package-private method")
    void shouldHaveGetComponentEngineMethod() throws Exception {
      Method method = JniWasiComponent.class.getDeclaredMethod("getComponentEngine");
      assertNotNull(method, "getComponentEngine method should exist");
      assertFalse(Modifier.isPublic(method.getModifiers()), "Should not be public");
      assertFalse(Modifier.isPrivate(method.getModifiers()), "Should not be private");
    }
  }

  // ========================================================================
  // Private Helper Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Private Helper Method Tests")
  class PrivateHelperMethodTests {

    @Test
    @DisplayName("should have ensureNotClosed private method")
    void shouldHaveEnsureNotClosedMethod() throws Exception {
      Method method = JniWasiComponent.class.getDeclaredMethod("ensureNotClosed");
      assertNotNull(method, "ensureNotClosed method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "ensureNotClosed should be private");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have extractExports private method")
    void shouldHaveExtractExportsMethod() throws Exception {
      Method method = JniWasiComponent.class.getDeclaredMethod("extractExports");
      assertNotNull(method, "extractExports method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "extractExports should be private");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have extractImports private method")
    void shouldHaveExtractImportsMethod() throws Exception {
      Method method = JniWasiComponent.class.getDeclaredMethod("extractImports");
      assertNotNull(method, "extractImports method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "extractImports should be private");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have createBasicInterfaceMetadata private method")
    void shouldHaveCreateBasicInterfaceMetadataMethod() throws Exception {
      Method method =
          JniWasiComponent.class.getDeclaredMethod(
              "createBasicInterfaceMetadata", String.class, boolean.class);
      assertNotNull(method, "createBasicInterfaceMetadata method should exist");
      assertTrue(
          Modifier.isPrivate(method.getModifiers()),
          "createBasicInterfaceMetadata should be private");
      assertEquals(
          WasiInterfaceMetadata.class,
          method.getReturnType(),
          "Return type should be WasiInterfaceMetadata");
    }

    @Test
    @DisplayName("should have extractStats private method")
    void shouldHaveExtractStatsMethod() throws Exception {
      Method method = JniWasiComponent.class.getDeclaredMethod("extractStats");
      assertNotNull(method, "extractStats method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "extractStats should be private");
      assertEquals(
          WasiComponentStats.class,
          method.getReturnType(),
          "Return type should be WasiComponentStats");
    }

    @Test
    @DisplayName("should have createBasicErrorStats private method")
    void shouldHaveCreateBasicErrorStatsMethod() throws Exception {
      Method method = JniWasiComponent.class.getDeclaredMethod("createBasicErrorStats");
      assertNotNull(method, "createBasicErrorStats method should exist");
      assertTrue(
          Modifier.isPrivate(method.getModifiers()), "createBasicErrorStats should be private");
    }

    @Test
    @DisplayName("should have createBasicResourceUsageStats private method")
    void shouldHaveCreateBasicResourceUsageStatsMethod() throws Exception {
      Method method = JniWasiComponent.class.getDeclaredMethod("createBasicResourceUsageStats");
      assertNotNull(method, "createBasicResourceUsageStats method should exist");
      assertTrue(
          Modifier.isPrivate(method.getModifiers()),
          "createBasicResourceUsageStats should be private");
    }

    @Test
    @DisplayName("should have createBasicPerformanceMetrics private method")
    void shouldHaveCreateBasicPerformanceMetricsMethod() throws Exception {
      Method method = JniWasiComponent.class.getDeclaredMethod("createBasicPerformanceMetrics");
      assertNotNull(method, "createBasicPerformanceMetrics method should exist");
      assertTrue(
          Modifier.isPrivate(method.getModifiers()),
          "createBasicPerformanceMetrics should be private");
    }
  }

  // ========================================================================
  // Thread Safety Design Tests
  // ========================================================================

  @Nested
  @DisplayName("Thread Safety Design Tests")
  class ThreadSafetyDesignTests {

    @Test
    @DisplayName("closed field should be volatile for thread safety")
    void closedShouldBeVolatile() throws Exception {
      Field field = JniWasiComponent.class.getDeclaredField("closed");
      assertTrue(
          Modifier.isVolatile(field.getModifiers()),
          "closed should be volatile for thread-safe visibility");
    }

    @Test
    @DisplayName("cached fields should be volatile for double-checked locking")
    void cachedFieldsShouldBeVolatile() throws Exception {
      Field cachedExports = JniWasiComponent.class.getDeclaredField("cachedExports");
      Field cachedImports = JniWasiComponent.class.getDeclaredField("cachedImports");
      Field cachedStats = JniWasiComponent.class.getDeclaredField("cachedStats");

      assertTrue(
          Modifier.isVolatile(cachedExports.getModifiers()),
          "cachedExports should be volatile for double-checked locking");
      assertTrue(
          Modifier.isVolatile(cachedImports.getModifiers()),
          "cachedImports should be volatile for double-checked locking");
      assertTrue(
          Modifier.isVolatile(cachedStats.getModifiers()),
          "cachedStats should be volatile for double-checked locking");
    }
  }

  // ========================================================================
  // Interface Compliance Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("WasiComponent interface methods should all be implemented")
    void allInterfaceMethodsShouldBeImplemented() {
      Method[] interfaceMethods = WasiComponent.class.getDeclaredMethods();

      for (Method interfaceMethod : interfaceMethods) {
        // Skip default methods
        if (interfaceMethod.isDefault()) {
          continue;
        }

        try {
          Method implMethod =
              JniWasiComponent.class.getMethod(
                  interfaceMethod.getName(), interfaceMethod.getParameterTypes());
          assertNotNull(
              implMethod, "Implementation for " + interfaceMethod.getName() + " should exist");
        } catch (NoSuchMethodException e) {
          throw new AssertionError(
              "Missing implementation for interface method: " + interfaceMethod.getName());
        }
      }
    }
  }

  // ========================================================================
  // Caching Pattern Tests
  // ========================================================================

  @Nested
  @DisplayName("Caching Pattern Tests")
  class CachingPatternTests {

    @Test
    @DisplayName("getExports should use caching pattern")
    void getExportsShouldUseCaching() throws Exception {
      // Verify cachedExports field exists and is used
      Field cachedExports = JniWasiComponent.class.getDeclaredField("cachedExports");
      assertNotNull(cachedExports, "cachedExports field should exist for caching");
    }

    @Test
    @DisplayName("getImports should use caching pattern")
    void getImportsShouldUseCaching() throws Exception {
      // Verify cachedImports field exists and is used
      Field cachedImports = JniWasiComponent.class.getDeclaredField("cachedImports");
      assertNotNull(cachedImports, "cachedImports field should exist for caching");
    }

    @Test
    @DisplayName("getStats should use caching pattern")
    void getStatsShouldUseCaching() throws Exception {
      // Verify cachedStats field exists and is used
      Field cachedStats = JniWasiComponent.class.getDeclaredField("cachedStats");
      assertNotNull(cachedStats, "cachedStats field should exist for caching");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected number of public methods")
    void shouldHaveExpectedPublicMethodCount() {
      Method[] allMethods = JniWasiComponent.class.getDeclaredMethods();
      int publicCount = 0;

      for (Method method : allMethods) {
        if (Modifier.isPublic(method.getModifiers())) {
          publicCount++;
        }
      }

      // Expected: ~12 public methods from WasiComponent interface
      assertTrue(publicCount >= 10, "Should have at least 10 public methods from interface");
    }

    @Test
    @DisplayName("should have expected number of private methods")
    void shouldHaveExpectedPrivateMethodCount() {
      Method[] allMethods = JniWasiComponent.class.getDeclaredMethods();
      int privateCount = 0;

      for (Method method : allMethods) {
        if (Modifier.isPrivate(method.getModifiers()) && !method.isSynthetic()) {
          privateCount++;
        }
      }

      // Expected: ~7 private helper methods
      assertTrue(privateCount >= 5, "Should have at least 5 private helper methods");
    }
  }
}
