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

package ai.tegmentum.wasmtime4j.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultGuestProfiler} class.
 *
 * <p>DefaultGuestProfiler provides a default implementation of GuestProfiler for profiling
 * WebAssembly guest execution with support for multiple export formats.
 */
@DisplayName("DefaultGuestProfiler Tests")
class DefaultGuestProfilerTest {

  /**
   * Loads the class without triggering static initialization.
   *
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  private Class<?> loadClassWithoutInit() throws ClassNotFoundException {
    return Class.forName(
        "ai.tegmentum.wasmtime4j.debug.DefaultGuestProfiler", false, getClass().getClassLoader());
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "DefaultGuestProfiler should be final");
    }

    @Test
    @DisplayName("should be package-private")
    void shouldBePackagePrivate() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      assertFalse(
          Modifier.isPublic(clazz.getModifiers()), "DefaultGuestProfiler should not be public");
      assertFalse(
          Modifier.isProtected(clazz.getModifiers()),
          "DefaultGuestProfiler should not be protected");
      assertFalse(
          Modifier.isPrivate(clazz.getModifiers()), "DefaultGuestProfiler should not be private");
    }

    @Test
    @DisplayName("should implement GuestProfiler interface")
    void shouldImplementGuestProfilerInterface() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean implementsGuestProfiler = false;
      for (final Class<?> iface : clazz.getInterfaces()) {
        if ("GuestProfiler".equals(iface.getSimpleName())) {
          implementsGuestProfiler = true;
          break;
        }
      }
      assertTrue(implementsGuestProfiler, "Should implement GuestProfiler");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with Store parameter")
    void shouldHaveStoreConstructor() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasStoreConstructor = false;

      for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
        final Class<?>[] params = constructor.getParameterTypes();
        if (params.length == 1 && "Store".equals(params[0].getSimpleName())) {
          hasStoreConstructor = true;
          break;
        }
      }

      assertTrue(hasStoreConstructor, "Should have constructor with Store parameter");
    }

    @Test
    @DisplayName("should have constructor with Store and ProfilerConfig parameters")
    void shouldHaveStoreAndConfigConstructor() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasConfigConstructor = false;

      for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
        final Class<?>[] params = constructor.getParameterTypes();
        if (params.length == 2
            && "Store".equals(params[0].getSimpleName())
            && "ProfilerConfig".equals(params[1].getSimpleName())) {
          hasConfigConstructor = true;
          break;
        }
      }

      assertTrue(hasConfigConstructor, "Should have constructor with Store and ProfilerConfig");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have start method")
    void shouldHaveStartMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasStart = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("start".equals(method.getName()) && method.getParameterCount() == 0) {
          hasStart = true;
          assertEquals(void.class, method.getReturnType(), "start should return void");
          break;
        }
      }

      assertTrue(hasStart, "Should have start method");
    }

    @Test
    @DisplayName("should have stop method")
    void shouldHaveStopMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasStop = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("stop".equals(method.getName()) && method.getParameterCount() == 0) {
          hasStop = true;
          assertEquals(void.class, method.getReturnType(), "stop should return void");
          break;
        }
      }

      assertTrue(hasStop, "Should have stop method");
    }

    @Test
    @DisplayName("should have isProfiling method")
    void shouldHaveIsProfilingMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasIsProfiling = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("isProfiling".equals(method.getName()) && method.getParameterCount() == 0) {
          hasIsProfiling = true;
          assertEquals(boolean.class, method.getReturnType(), "isProfiling should return boolean");
          break;
        }
      }

      assertTrue(hasIsProfiling, "Should have isProfiling method");
    }

    @Test
    @DisplayName("should have getProfileData method")
    void shouldHaveGetProfileDataMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasGetProfileData = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("getProfileData".equals(method.getName()) && method.getParameterCount() == 0) {
          hasGetProfileData = true;
          assertEquals(
              "ProfileData",
              method.getReturnType().getSimpleName(),
              "getProfileData should return ProfileData");
          break;
        }
      }

      assertTrue(hasGetProfileData, "Should have getProfileData method");
    }

    @Test
    @DisplayName("should have exportTo methods")
    void shouldHaveExportToMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      int exportToCount = 0;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("exportTo".equals(method.getName())) {
          exportToCount++;
        }
      }

      assertTrue(exportToCount >= 2, "Should have at least 2 exportTo methods (Path and Stream)");
    }

    @Test
    @DisplayName("should have reset method")
    void shouldHaveResetMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasReset = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("reset".equals(method.getName()) && method.getParameterCount() == 0) {
          hasReset = true;
          assertEquals(void.class, method.getReturnType(), "reset should return void");
          break;
        }
      }

      assertTrue(hasReset, "Should have reset method");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasClose = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("close".equals(method.getName()) && method.getParameterCount() == 0) {
          hasClose = true;
          assertEquals(void.class, method.getReturnType(), "close should return void");
          break;
        }
      }

      assertTrue(hasClose, "Should have close method");
    }
  }

  @Nested
  @DisplayName("Private Method Tests")
  class PrivateMethodTests {

    @Test
    @DisplayName("should have private export format methods")
    void shouldHavePrivateExportFormatMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasExportAsJson = false;
      boolean hasExportAsFlamegraph = false;
      boolean hasExportAsChromeTrace = false;
      boolean hasExportAsPprof = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if (Modifier.isPrivate(method.getModifiers())) {
          final String methodName = method.getName();
          if ("exportAsJson".equals(methodName)) {
            hasExportAsJson = true;
          }
          if ("exportAsFlamegraph".equals(methodName)) {
            hasExportAsFlamegraph = true;
          }
          if ("exportAsChromeTrace".equals(methodName)) {
            hasExportAsChromeTrace = true;
          }
          if ("exportAsPprof".equals(methodName)) {
            hasExportAsPprof = true;
          }
        }
      }

      assertTrue(hasExportAsJson, "Should have private exportAsJson method");
      assertTrue(hasExportAsFlamegraph, "Should have private exportAsFlamegraph method");
      assertTrue(hasExportAsChromeTrace, "Should have private exportAsChromeTrace method");
      assertTrue(hasExportAsPprof, "Should have private exportAsPprof method");
    }

    @Test
    @DisplayName("should have private escapeJson method")
    void shouldHavePrivateEscapeJsonMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasEscapeJson = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("escapeJson".equals(method.getName()) && Modifier.isPrivate(method.getModifiers())) {
          hasEscapeJson = true;
          assertEquals(String.class, method.getReturnType(), "escapeJson should return String");
          break;
        }
      }

      assertTrue(hasEscapeJson, "Should have private escapeJson method");
    }
  }

  @Nested
  @DisplayName("ProfileFormat Integration Tests")
  class ProfileFormatIntegrationTests {

    @Test
    @DisplayName("ProfileFormat enum should exist with expected values")
    void profileFormatEnumShouldExistWithExpectedValues() throws ClassNotFoundException {
      // ProfileFormat is nested inside GuestProfiler interface
      final Class<?> formatClass =
          Class.forName(
              "ai.tegmentum.wasmtime4j.debug.GuestProfiler$ProfileFormat",
              false,
              getClass().getClassLoader());

      assertTrue(formatClass.isEnum(), "ProfileFormat should be an enum");

      final Object[] constants = formatClass.getEnumConstants();
      assertNotNull(constants, "ProfileFormat should have enum constants");
      assertTrue(constants.length >= 4, "ProfileFormat should have at least 4 values");

      boolean hasJson = false;
      boolean hasFlamegraph = false;
      boolean hasChromeTrace = false;
      boolean hasPprof = false;

      for (final Object constant : constants) {
        final String name = constant.toString();
        if ("JSON".equals(name)) {
          hasJson = true;
        }
        if ("FLAMEGRAPH".equals(name)) {
          hasFlamegraph = true;
        }
        if ("CHROME_TRACE".equals(name)) {
          hasChromeTrace = true;
        }
        if ("PPROF".equals(name)) {
          hasPprof = true;
        }
      }

      assertTrue(hasJson, "Should have JSON format");
      assertTrue(hasFlamegraph, "Should have FLAMEGRAPH format");
      assertTrue(hasChromeTrace, "Should have CHROME_TRACE format");
      assertTrue(hasPprof, "Should have PPROF format");
    }
  }

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have profiling state field")
    void shouldHaveProfilingStateField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasProfilingField = false;

      for (final java.lang.reflect.Field field : clazz.getDeclaredFields()) {
        if ("profiling".equals(field.getName())) {
          hasProfilingField = true;
          assertTrue(
              Modifier.isVolatile(field.getModifiers()), "profiling field should be volatile");
          break;
        }
      }

      assertTrue(hasProfilingField, "Should have profiling field");
    }

    @Test
    @DisplayName("should have store field")
    void shouldHaveStoreField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasStoreField = false;

      for (final java.lang.reflect.Field field : clazz.getDeclaredFields()) {
        if ("store".equals(field.getName())) {
          hasStoreField = true;
          assertTrue(Modifier.isPrivate(field.getModifiers()), "store field should be private");
          assertTrue(Modifier.isFinal(field.getModifiers()), "store field should be final");
          break;
        }
      }

      assertTrue(hasStoreField, "Should have store field");
    }

    @Test
    @DisplayName("should have functionProfiles list field")
    void shouldHaveFunctionProfilesField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasFunctionProfilesField = false;

      for (final java.lang.reflect.Field field : clazz.getDeclaredFields()) {
        if ("functionProfiles".equals(field.getName())) {
          hasFunctionProfilesField = true;
          break;
        }
      }

      assertTrue(hasFunctionProfilesField, "Should have functionProfiles field");
    }

    @Test
    @DisplayName("should have customMetrics map field")
    void shouldHaveCustomMetricsField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasCustomMetricsField = false;

      for (final java.lang.reflect.Field field : clazz.getDeclaredFields()) {
        if ("customMetrics".equals(field.getName())) {
          hasCustomMetricsField = true;
          break;
        }
      }

      assertTrue(hasCustomMetricsField, "Should have customMetrics field");
    }
  }
}
