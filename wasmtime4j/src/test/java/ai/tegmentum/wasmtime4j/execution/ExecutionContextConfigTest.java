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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ExecutionContextConfig interface.
 *
 * <p>This test class verifies the interface structure, methods, and nested types for
 * ExecutionContextConfig using reflection-based testing.
 */
@DisplayName("ExecutionContextConfig Tests")
class ExecutionContextConfigTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ExecutionContextConfig should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ExecutionContextConfig.class.isInterface(),
          "ExecutionContextConfig should be an interface");
    }

    @Test
    @DisplayName("ExecutionContextConfig should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionContextConfig.class.getModifiers()),
          "ExecutionContextConfig should be public");
    }

    @Test
    @DisplayName("ExecutionContextConfig should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = ExecutionContextConfig.class.getInterfaces();
      assertEquals(
          0, interfaces.length, "ExecutionContextConfig should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      Method method = ExecutionContextConfig.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getTimeout method")
    void shouldHaveGetTimeoutMethod() throws NoSuchMethodException {
      Method method = ExecutionContextConfig.class.getMethod("getTimeout");
      assertNotNull(method, "getTimeout method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have setTimeout method")
    void shouldHaveSetTimeoutMethod() throws NoSuchMethodException {
      Method method = ExecutionContextConfig.class.getMethod("setTimeout", long.class);
      assertNotNull(method, "setTimeout method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getMemoryLimit method")
    void shouldHaveGetMemoryLimitMethod() throws NoSuchMethodException {
      Method method = ExecutionContextConfig.class.getMethod("getMemoryLimit");
      assertNotNull(method, "getMemoryLimit method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have setMemoryLimit method")
    void shouldHaveSetMemoryLimitMethod() throws NoSuchMethodException {
      Method method = ExecutionContextConfig.class.getMethod("setMemoryLimit", long.class);
      assertNotNull(method, "setMemoryLimit method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have isDebuggingEnabled method")
    void shouldHaveIsDebuggingEnabledMethod() throws NoSuchMethodException {
      Method method = ExecutionContextConfig.class.getMethod("isDebuggingEnabled");
      assertNotNull(method, "isDebuggingEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have setDebuggingEnabled method")
    void shouldHaveSetDebuggingEnabledMethod() throws NoSuchMethodException {
      Method method = ExecutionContextConfig.class.getMethod("setDebuggingEnabled", boolean.class);
      assertNotNull(method, "setDebuggingEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have isProfilingEnabled method")
    void shouldHaveIsProfilingEnabledMethod() throws NoSuchMethodException {
      Method method = ExecutionContextConfig.class.getMethod("isProfilingEnabled");
      assertNotNull(method, "isProfilingEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have setProfilingEnabled method")
    void shouldHaveSetProfilingEnabledMethod() throws NoSuchMethodException {
      Method method = ExecutionContextConfig.class.getMethod("setProfilingEnabled", boolean.class);
      assertNotNull(method, "setProfilingEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getThreadPoolSize method")
    void shouldHaveGetThreadPoolSizeMethod() throws NoSuchMethodException {
      Method method = ExecutionContextConfig.class.getMethod("getThreadPoolSize");
      assertNotNull(method, "getThreadPoolSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have setThreadPoolSize method")
    void shouldHaveSetThreadPoolSizeMethod() throws NoSuchMethodException {
      Method method = ExecutionContextConfig.class.getMethod("setThreadPoolSize", int.class);
      assertNotNull(method, "setThreadPoolSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getSecurityConfig method")
    void shouldHaveGetSecurityConfigMethod() throws NoSuchMethodException {
      Method method = ExecutionContextConfig.class.getMethod("getSecurityConfig");
      assertNotNull(method, "getSecurityConfig method should exist");
      assertEquals(
          ExecutionContextConfig.SecurityConfig.class,
          method.getReturnType(),
          "Return type should be SecurityConfig");
    }

    @Test
    @DisplayName("should have setSecurityConfig method")
    void shouldHaveSetSecurityConfigMethod() throws NoSuchMethodException {
      Method method =
          ExecutionContextConfig.class.getMethod(
              "setSecurityConfig", ExecutionContextConfig.SecurityConfig.class);
      assertNotNull(method, "setSecurityConfig method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // SecurityConfig Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("SecurityConfig Interface Tests")
  class SecurityConfigTests {

    @Test
    @DisplayName("SecurityConfig should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ExecutionContextConfig.SecurityConfig.class.isInterface(),
          "SecurityConfig should be an interface");
      assertTrue(
          ExecutionContextConfig.SecurityConfig.class.isMemberClass(),
          "SecurityConfig should be a member class");
    }

    @Test
    @DisplayName("SecurityConfig should have isSandboxingEnabled method")
    void shouldHaveIsSandboxingEnabledMethod() throws NoSuchMethodException {
      Method method = ExecutionContextConfig.SecurityConfig.class.getMethod("isSandboxingEnabled");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("SecurityConfig should have getAllowedSystemCalls method")
    void shouldHaveGetAllowedSystemCallsMethod() throws NoSuchMethodException {
      Method method =
          ExecutionContextConfig.SecurityConfig.class.getMethod("getAllowedSystemCalls");
      assertEquals(Set.class, method.getReturnType(), "Return type should be Set");
    }

    @Test
    @DisplayName("SecurityConfig should have getSecurityLevel method")
    void shouldHaveGetSecurityLevelMethod() throws NoSuchMethodException {
      Method method = ExecutionContextConfig.SecurityConfig.class.getMethod("getSecurityLevel");
      assertEquals(
          ExecutionContextConfig.SecurityLevel.class,
          method.getReturnType(),
          "Return type should be SecurityLevel");
    }

    @Test
    @DisplayName("SecurityConfig should have isNetworkAccessAllowed method")
    void shouldHaveIsNetworkAccessAllowedMethod() throws NoSuchMethodException {
      Method method =
          ExecutionContextConfig.SecurityConfig.class.getMethod("isNetworkAccessAllowed");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("SecurityConfig should have isFileSystemAccessAllowed method")
    void shouldHaveIsFileSystemAccessAllowedMethod() throws NoSuchMethodException {
      Method method =
          ExecutionContextConfig.SecurityConfig.class.getMethod("isFileSystemAccessAllowed");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }
  }

  // ========================================================================
  // SecurityLevel Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("SecurityLevel Enum Tests")
  class SecurityLevelTests {

    @Test
    @DisplayName("SecurityLevel should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ExecutionContextConfig.SecurityLevel.class.isEnum(), "SecurityLevel should be an enum");
      assertTrue(
          ExecutionContextConfig.SecurityLevel.class.isMemberClass(),
          "SecurityLevel should be a member class");
    }

    @Test
    @DisplayName("SecurityLevel should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionContextConfig.SecurityLevel.class.getModifiers()),
          "SecurityLevel should be public");
    }

    @Test
    @DisplayName("SecurityLevel should have 4 values")
    void shouldHaveFourValues() {
      ExecutionContextConfig.SecurityLevel[] values = ExecutionContextConfig.SecurityLevel.values();
      assertEquals(4, values.length, "SecurityLevel should have 4 values");
    }

    @Test
    @DisplayName("SecurityLevel should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("LOW", "MEDIUM", "HIGH", "STRICT");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionContextConfig.SecurityLevel level :
          ExecutionContextConfig.SecurityLevel.values()) {
        actualNames.add(level.name());
      }
      assertEquals(expectedNames, actualNames, "SecurityLevel should have expected values");
    }
  }

  // ========================================================================
  // Nested Type Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Type Count Tests")
  class NestedTypeCountTests {

    @Test
    @DisplayName("ExecutionContextConfig should have 1 nested enum")
    void shouldHaveOneNestedEnum() {
      Class<?>[] nestedClasses = ExecutionContextConfig.class.getDeclaredClasses();
      int enumCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isEnum()) {
          enumCount++;
        }
      }
      assertEquals(1, enumCount, "ExecutionContextConfig should have 1 nested enum");
    }

    @Test
    @DisplayName("ExecutionContextConfig should have 1 nested interface")
    void shouldHaveOneNestedInterface() {
      Class<?>[] nestedClasses = ExecutionContextConfig.class.getDeclaredClasses();
      int interfaceCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isInterface() && !nested.isAnnotation()) {
          interfaceCount++;
        }
      }
      assertEquals(1, interfaceCount, "ExecutionContextConfig should have 1 nested interface");
    }
  }
}
