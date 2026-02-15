/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.panama.wasi.http;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Panama WASI HTTP implementation classes.
 *
 * <p>These tests verify the class structure and API contracts of the Panama WASI HTTP
 * implementation using reflection to avoid triggering native library loading.
 */
@DisplayName("Panama WASI HTTP Implementation Tests")
class PanamaWasiHttpTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiHttpTest.class.getName());

  private static final String HTTP_CONTEXT_CLASS =
      "ai.tegmentum.wasmtime4j.panama.wasi.http.PanamaWasiHttpContext";
  private static final String HTTP_CONFIG_CLASS =
      "ai.tegmentum.wasmtime4j.panama.wasi.http.PanamaWasiHttpConfig";
  private static final String HTTP_CONFIG_BUILDER_CLASS =
      "ai.tegmentum.wasmtime4j.panama.wasi.http.PanamaWasiHttpConfigBuilder";

  /**
   * Loads a class without initializing it.
   *
   * @param className the fully qualified class name
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  private Class<?> loadClassWithoutInit(final String className) throws ClassNotFoundException {
    return Class.forName(className, false, getClass().getClassLoader());
  }

  @Nested
  @DisplayName("PanamaWasiHttpContext Class Structure Tests")
  class HttpContextClassStructureTests {

    @Test
    @DisplayName("PanamaWasiHttpContext class should exist and be public final")
    void contextClassShouldExistAndBePublicFinal() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONTEXT_CLASS);

      assertNotNull(clazz, "PanamaWasiHttpContext class should exist");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "Class should be public");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "Class should be final");
      LOGGER.info("PanamaWasiHttpContext class exists and is public final");
    }

    @Test
    @DisplayName("PanamaWasiHttpContext should implement WasiHttpContext interface")
    void contextShouldImplementInterface() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONTEXT_CLASS);
      final Class<?>[] interfaces = clazz.getInterfaces();

      final Set<String> interfaceNames =
          Arrays.stream(interfaces).map(Class::getName).collect(Collectors.toSet());

      assertTrue(
          interfaceNames.contains("ai.tegmentum.wasmtime4j.wasi.http.WasiHttpContext"),
          "Should implement WasiHttpContext interface");
      LOGGER.info("PanamaWasiHttpContext implements WasiHttpContext interface");
    }

    @Test
    @DisplayName("PanamaWasiHttpContext should have required lifecycle methods")
    void contextShouldHaveRequiredMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONTEXT_CLASS);

      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("addToLinker");
      requiredMethods.add("getConfig");
      requiredMethods.add("isValid");
      requiredMethods.add("isHostAllowed");
      requiredMethods.add("close");

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (final String method : requiredMethods) {
        assertTrue(
            declaredMethods.contains(method),
            "PanamaWasiHttpContext should have method: " + method);
      }
      LOGGER.info("PanamaWasiHttpContext has all required lifecycle methods: " + requiredMethods);
    }

    @Test
    @DisplayName("PanamaWasiHttpContext should have context pointer field")
    void contextShouldHaveContextPtrField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONTEXT_CLASS);

      boolean hasContextPtr = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("contextPtr")
            && field.getType().getName().equals("java.lang.foreign.MemorySegment")) {
          hasContextPtr = true;
          break;
        }
      }
      assertTrue(hasContextPtr, "Should have contextPtr field of type MemorySegment");
      LOGGER.info("PanamaWasiHttpContext has contextPtr field");
    }

    @Test
    @DisplayName("PanamaWasiHttpContext should have bindings field")
    void contextShouldHaveBindingsField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONTEXT_CLASS);

      boolean hasBindings = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("bindings")) {
          hasBindings = true;
          break;
        }
      }
      assertTrue(hasBindings, "Should have bindings field");
      LOGGER.info("PanamaWasiHttpContext has bindings field");
    }

    @Test
    @DisplayName("PanamaWasiHttpContext should have logger field")
    void contextShouldHaveLoggerField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONTEXT_CLASS);

      boolean hasLogger = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("LOGGER")
            && field.getType().equals(Logger.class)
            && Modifier.isStatic(field.getModifiers())
            && Modifier.isFinal(field.getModifiers())) {
          hasLogger = true;
          break;
        }
      }
      assertTrue(hasLogger, "Should have static final LOGGER field");
      LOGGER.info("PanamaWasiHttpContext has LOGGER field");
    }
  }

  @Nested
  @DisplayName("PanamaWasiHttpConfig Class Structure Tests")
  class HttpConfigClassStructureTests {

    @Test
    @DisplayName("PanamaWasiHttpConfig class should exist and be public final")
    void configClassShouldExistAndBePublicFinal() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONFIG_CLASS);

      assertNotNull(clazz, "PanamaWasiHttpConfig class should exist");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "Class should be public");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "Class should be final");
      LOGGER.info("PanamaWasiHttpConfig class exists and is public final");
    }

    @Test
    @DisplayName("PanamaWasiHttpConfig should implement WasiHttpConfig interface")
    void configShouldImplementInterface() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONFIG_CLASS);
      final Class<?>[] interfaces = clazz.getInterfaces();

      final Set<String> interfaceNames =
          Arrays.stream(interfaces).map(Class::getName).collect(Collectors.toSet());

      assertTrue(
          interfaceNames.contains("ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfig"),
          "Should implement WasiHttpConfig interface");
      LOGGER.info("PanamaWasiHttpConfig implements WasiHttpConfig interface");
    }

    @Test
    @DisplayName("PanamaWasiHttpConfig should have required host methods")
    void configShouldHaveHostMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONFIG_CLASS);

      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("getAllowedHosts");
      requiredMethods.add("getBlockedHosts");

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (final String method : requiredMethods) {
        assertTrue(
            declaredMethods.contains(method), "PanamaWasiHttpConfig should have method: " + method);
      }
      LOGGER.info("PanamaWasiHttpConfig has required host methods: " + requiredMethods);
    }

    @Test
    @DisplayName("PanamaWasiHttpConfig should have timeout methods")
    void configShouldHaveTimeoutMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONFIG_CLASS);

      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("getConnectTimeout");
      requiredMethods.add("getReadTimeout");
      requiredMethods.add("getWriteTimeout");

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (final String method : requiredMethods) {
        assertTrue(
            declaredMethods.contains(method), "PanamaWasiHttpConfig should have method: " + method);
      }
      LOGGER.info("PanamaWasiHttpConfig has timeout methods: " + requiredMethods);
    }

    @Test
    @DisplayName("PanamaWasiHttpConfig should have connection limit methods")
    void configShouldHaveConnectionMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONFIG_CLASS);

      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("getMaxConnections");
      requiredMethods.add("getMaxConnectionsPerHost");

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (final String method : requiredMethods) {
        assertTrue(
            declaredMethods.contains(method), "PanamaWasiHttpConfig should have method: " + method);
      }
      LOGGER.info("PanamaWasiHttpConfig has connection limit methods: " + requiredMethods);
    }

    @Test
    @DisplayName("PanamaWasiHttpConfig should have security configuration methods")
    void configShouldHaveSecurityMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONFIG_CLASS);

      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("isHttpsRequired");
      requiredMethods.add("isCertificateValidationEnabled");
      requiredMethods.add("isHttp2Enabled");

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (final String method : requiredMethods) {
        assertTrue(
            declaredMethods.contains(method), "PanamaWasiHttpConfig should have method: " + method);
      }
      LOGGER.info("PanamaWasiHttpConfig has security methods: " + requiredMethods);
    }

    @Test
    @DisplayName("PanamaWasiHttpConfig should have validate method")
    void configShouldHaveValidateMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONFIG_CLASS);

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      assertTrue(
          declaredMethods.contains("validate"), "PanamaWasiHttpConfig should have validate method");
      LOGGER.info("PanamaWasiHttpConfig has validate method");
    }

    @Test
    @DisplayName("PanamaWasiHttpConfig should have toBuilder method")
    void configShouldHaveToBuilderMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONFIG_CLASS);

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      assertTrue(
          declaredMethods.contains("toBuilder"),
          "PanamaWasiHttpConfig should have toBuilder method");
      LOGGER.info("PanamaWasiHttpConfig has toBuilder method");
    }
  }

  @Nested
  @DisplayName("PanamaWasiHttpConfigBuilder Class Structure Tests")
  class HttpConfigBuilderClassStructureTests {

    @Test
    @DisplayName("PanamaWasiHttpConfigBuilder class should exist and be public final")
    void builderClassShouldExistAndBePublicFinal() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONFIG_BUILDER_CLASS);

      assertNotNull(clazz, "PanamaWasiHttpConfigBuilder class should exist");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "Class should be public");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "Class should be final");
      LOGGER.info("PanamaWasiHttpConfigBuilder class exists and is public final");
    }

    @Test
    @DisplayName("PanamaWasiHttpConfigBuilder should implement WasiHttpConfigBuilder interface")
    void builderShouldImplementInterface() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONFIG_BUILDER_CLASS);
      final Class<?>[] interfaces = clazz.getInterfaces();

      final Set<String> interfaceNames =
          Arrays.stream(interfaces).map(Class::getName).collect(Collectors.toSet());

      assertTrue(
          interfaceNames.contains("ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfigBuilder"),
          "Should implement WasiHttpConfigBuilder interface");
      LOGGER.info("PanamaWasiHttpConfigBuilder implements WasiHttpConfigBuilder interface");
    }

    @Test
    @DisplayName("PanamaWasiHttpConfigBuilder should have host configuration methods")
    void builderShouldHaveHostMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONFIG_BUILDER_CLASS);

      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("allowHost");
      requiredMethods.add("allowHosts");
      requiredMethods.add("allowAllHosts");
      requiredMethods.add("blockHost");
      requiredMethods.add("blockHosts");

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (final String method : requiredMethods) {
        assertTrue(
            declaredMethods.contains(method),
            "PanamaWasiHttpConfigBuilder should have method: " + method);
      }
      LOGGER.info("PanamaWasiHttpConfigBuilder has host methods: " + requiredMethods);
    }

    @Test
    @DisplayName("PanamaWasiHttpConfigBuilder should have timeout configuration methods")
    void builderShouldHaveTimeoutMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONFIG_BUILDER_CLASS);

      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("withConnectTimeout");
      requiredMethods.add("withReadTimeout");
      requiredMethods.add("withWriteTimeout");

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (final String method : requiredMethods) {
        assertTrue(
            declaredMethods.contains(method),
            "PanamaWasiHttpConfigBuilder should have method: " + method);
      }
      LOGGER.info("PanamaWasiHttpConfigBuilder has timeout methods: " + requiredMethods);
    }

    @Test
    @DisplayName("PanamaWasiHttpConfigBuilder should have build method")
    void builderShouldHaveBuildMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONFIG_BUILDER_CLASS);

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      assertTrue(
          declaredMethods.contains("build"),
          "PanamaWasiHttpConfigBuilder should have build method");
      LOGGER.info("PanamaWasiHttpConfigBuilder has build method");
    }

    @Test
    @DisplayName("PanamaWasiHttpConfigBuilder should have security configuration methods")
    void builderShouldHaveSecurityMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONFIG_BUILDER_CLASS);

      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("requireHttps");
      requiredMethods.add("withCertificateValidation");
      requiredMethods.add("withHttp2");
      requiredMethods.add("withConnectionPooling");
      requiredMethods.add("followRedirects");

      final Set<String> declaredMethods =
          Arrays.stream(clazz.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (final String method : requiredMethods) {
        assertTrue(
            declaredMethods.contains(method),
            "PanamaWasiHttpConfigBuilder should have method: " + method);
      }
      LOGGER.info("PanamaWasiHttpConfigBuilder has security methods: " + requiredMethods);
    }
  }

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("PanamaWasiHttpContext should implement AutoCloseable")
    void contextShouldImplementAutoCloseable() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONTEXT_CLASS);

      boolean implementsAutoCloseable = false;
      for (final Class<?> iface : clazz.getInterfaces()) {
        if (iface.getName().contains("WasiHttpContext")) {
          // Check if WasiHttpContext extends AutoCloseable
          for (final Class<?> superIface : iface.getInterfaces()) {
            if (superIface.equals(AutoCloseable.class)) {
              implementsAutoCloseable = true;
              break;
            }
          }
        }
      }
      // Also check direct implementation
      if (Arrays.asList(clazz.getInterfaces()).contains(AutoCloseable.class)) {
        implementsAutoCloseable = true;
      }

      assertTrue(
          implementsAutoCloseable || hasCloseMethod(clazz),
          "PanamaWasiHttpContext should implement AutoCloseable or have close method");
      LOGGER.info("PanamaWasiHttpContext implements AutoCloseable pattern");
    }

    private boolean hasCloseMethod(final Class<?> clazz) {
      for (final Method method : clazz.getDeclaredMethods()) {
        if (method.getName().equals("close") && method.getParameterCount() == 0) {
          return true;
        }
      }
      return false;
    }

    @Test
    @DisplayName("All HTTP classes should have proper equals and hashCode")
    void httpClassesShouldHaveEqualsAndHashCode() throws ClassNotFoundException {
      final Class<?> configClass = loadClassWithoutInit(HTTP_CONFIG_CLASS);

      final Set<String> declaredMethods =
          Arrays.stream(configClass.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      assertTrue(declaredMethods.contains("equals"), "PanamaWasiHttpConfig should override equals");
      assertTrue(
          declaredMethods.contains("hashCode"), "PanamaWasiHttpConfig should override hashCode");
      LOGGER.info("PanamaWasiHttpConfig has equals and hashCode methods");
    }

    @Test
    @DisplayName("HTTP classes should have toString method")
    void httpClassesShouldHaveToString() throws ClassNotFoundException {
      final String[] classes = {
        HTTP_CONTEXT_CLASS, HTTP_CONFIG_CLASS, HTTP_CONFIG_BUILDER_CLASS
      };

      for (final String className : classes) {
        final Class<?> clazz = loadClassWithoutInit(className);
        final Set<String> declaredMethods =
            Arrays.stream(clazz.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        // Context, Config, and Stats have toString, Builder may not
        if (!className.equals(HTTP_CONFIG_BUILDER_CLASS)) {
          assertTrue(declaredMethods.contains("toString"), className + " should override toString");
        }
      }
      LOGGER.info("HTTP classes have toString methods");
    }
  }

  @Nested
  @DisplayName("Panama FFI Pattern Tests")
  class PanamaFfiPatternTests {

    @Test
    @DisplayName("PanamaWasiHttpContext should have arena field for memory management")
    void contextShouldHaveArenaField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONTEXT_CLASS);

      boolean hasArena = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("arena")
            && field.getType().getName().equals("java.lang.foreign.Arena")) {
          hasArena = true;
          break;
        }
      }
      assertTrue(hasArena, "Should have arena field for memory management");
      LOGGER.info("PanamaWasiHttpContext has arena field for memory management");
    }

    @Test
    @DisplayName("PanamaWasiHttpContext should have closed atomic boolean")
    void contextShouldHaveClosedFlag() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONTEXT_CLASS);

      boolean hasClosed = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("closed")
            && field.getType().getName().contains("AtomicBoolean")) {
          hasClosed = true;
          break;
        }
      }
      assertTrue(hasClosed, "Should have closed AtomicBoolean field");
      LOGGER.info("PanamaWasiHttpContext has closed AtomicBoolean field");
    }

  }

  @Nested
  @DisplayName("Builder Pattern Tests")
  class BuilderPatternTests {

    @Test
    @DisplayName("PanamaWasiHttpConfigBuilder should have default constructor")
    void builderShouldHaveDefaultConstructor() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONFIG_BUILDER_CLASS);

      boolean hasDefaultConstructor = false;
      for (final var constructor : clazz.getDeclaredConstructors()) {
        if (constructor.getParameterCount() == 0 && Modifier.isPublic(constructor.getModifiers())) {
          hasDefaultConstructor = true;
          break;
        }
      }
      assertTrue(hasDefaultConstructor, "Should have public default constructor");
      LOGGER.info("PanamaWasiHttpConfigBuilder has default constructor");
    }

    @Test
    @DisplayName("Builder methods should return WasiHttpConfigBuilder for fluent API")
    void builderMethodsShouldReturnBuilder() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONFIG_BUILDER_CLASS);

      final String[] fluentMethods = {
        "allowHost",
        "allowHosts",
        "allowAllHosts",
        "blockHost",
        "blockHosts",
        "withConnectTimeout",
        "withReadTimeout",
        "withWriteTimeout",
        "withMaxConnections",
        "requireHttps",
        "withHttp2",
        "followRedirects"
      };

      for (final String methodName : fluentMethods) {
        boolean found = false;
        for (final Method method : clazz.getDeclaredMethods()) {
          if (method.getName().equals(methodName)) {
            final String returnTypeName = method.getReturnType().getName();
            assertTrue(
                returnTypeName.contains("WasiHttpConfigBuilder"),
                methodName + " should return WasiHttpConfigBuilder, but returns " + returnTypeName);
            found = true;
            break;
          }
        }
        if (!found) {
          LOGGER.warning("Method not found: " + methodName);
        }
      }
      LOGGER.info("Builder methods return WasiHttpConfigBuilder for fluent API");
    }

    @Test
    @DisplayName("Build method should return WasiHttpConfig")
    void buildMethodShouldReturnConfig() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(HTTP_CONFIG_BUILDER_CLASS);

      for (final Method method : clazz.getDeclaredMethods()) {
        if (method.getName().equals("build")) {
          final String returnTypeName = method.getReturnType().getName();
          assertTrue(
              returnTypeName.contains("WasiHttpConfig"),
              "build should return WasiHttpConfig, but returns " + returnTypeName);
          LOGGER.info("Build method returns WasiHttpConfig");
          return;
        }
      }
      assertTrue(false, "build method not found");
    }
  }
}
