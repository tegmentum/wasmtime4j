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
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ExecutionRequest interface.
 *
 * <p>This test class verifies the interface structure, methods, and nested types for
 * ExecutionRequest using reflection-based testing.
 */
@DisplayName("ExecutionRequest Tests")
class ExecutionRequestTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ExecutionRequest should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ExecutionRequest.class.isInterface(), "ExecutionRequest should be an interface");
    }

    @Test
    @DisplayName("ExecutionRequest should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionRequest.class.getModifiers()),
          "ExecutionRequest should be public");
    }

    @Test
    @DisplayName("ExecutionRequest should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = ExecutionRequest.class.getInterfaces();
      assertEquals(0, interfaces.length, "ExecutionRequest should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getFunctionName method")
    void shouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getFunctionName");
      assertNotNull(method, "getFunctionName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getArguments method")
    void shouldHaveGetArgumentsMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getArguments");
      assertNotNull(method, "getArguments method should exist");
      assertEquals(Object[].class, method.getReturnType(), "Return type should be Object[]");
    }

    @Test
    @DisplayName("should have getContext method")
    void shouldHaveGetContextMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getContext");
      assertNotNull(method, "getContext method should exist");
      assertEquals(
          ExecutionContext.class, method.getReturnType(), "Return type should be ExecutionContext");
    }

    @Test
    @DisplayName("should have getQuotas method")
    void shouldHaveGetQuotasMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getQuotas");
      assertNotNull(method, "getQuotas method should exist");
      assertEquals(
          ExecutionQuotas.class, method.getReturnType(), "Return type should be ExecutionQuotas");
    }

    @Test
    @DisplayName("should have getPolicy method")
    void shouldHaveGetPolicyMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getPolicy");
      assertNotNull(method, "getPolicy method should exist");
      assertEquals(
          ExecutionPolicy.class, method.getReturnType(), "Return type should be ExecutionPolicy");
    }

    @Test
    @DisplayName("should have getPriority method")
    void shouldHaveGetPriorityMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getPriority");
      assertNotNull(method, "getPriority method should exist");
      assertEquals(
          ExecutionRequest.RequestPriority.class,
          method.getReturnType(),
          "Return type should be RequestPriority");
    }

    @Test
    @DisplayName("should have getTimeout method")
    void shouldHaveGetTimeoutMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getTimeout");
      assertNotNull(method, "getTimeout method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getMetadata");
      assertNotNull(method, "getMetadata method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
    }

    @Test
    @DisplayName("should have getTimestamp method")
    void shouldHaveGetTimestampMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getTimestamp");
      assertNotNull(method, "getTimestamp method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getCallerInfo method")
    void shouldHaveGetCallerInfoMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getCallerInfo");
      assertNotNull(method, "getCallerInfo method should exist");
      assertEquals(
          ExecutionRequest.CallerInfo.class,
          method.getReturnType(),
          "Return type should be CallerInfo");
    }
  }

  // ========================================================================
  // RequestPriority Nested Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("RequestPriority Enum Tests")
  class RequestPriorityTests {

    @Test
    @DisplayName("RequestPriority should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ExecutionRequest.RequestPriority.class.isEnum(), "RequestPriority should be an enum");
      assertTrue(
          ExecutionRequest.RequestPriority.class.isMemberClass(),
          "RequestPriority should be a member class");
    }

    @Test
    @DisplayName("RequestPriority should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionRequest.RequestPriority.class.getModifiers()),
          "RequestPriority should be public");
    }

    @Test
    @DisplayName("RequestPriority should have 4 values")
    void shouldHaveFourValues() {
      ExecutionRequest.RequestPriority[] values = ExecutionRequest.RequestPriority.values();
      assertEquals(4, values.length, "RequestPriority should have 4 values");
    }

    @Test
    @DisplayName("RequestPriority should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("LOW", "NORMAL", "HIGH", "CRITICAL");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionRequest.RequestPriority priority : ExecutionRequest.RequestPriority.values()) {
        actualNames.add(priority.name());
      }
      assertEquals(expectedNames, actualNames, "RequestPriority should have expected values");
    }

    @Test
    @DisplayName("LOW should have ordinal 0")
    void lowShouldHaveOrdinalZero() {
      assertEquals(0, ExecutionRequest.RequestPriority.LOW.ordinal(), "LOW should have ordinal 0");
    }

    @Test
    @DisplayName("CRITICAL should have ordinal 3")
    void criticalShouldHaveOrdinalThree() {
      assertEquals(
          3, ExecutionRequest.RequestPriority.CRITICAL.ordinal(), "CRITICAL should have ordinal 3");
    }
  }

  // ========================================================================
  // CallerInfo Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("CallerInfo Interface Tests")
  class CallerInfoTests {

    @Test
    @DisplayName("CallerInfo should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ExecutionRequest.CallerInfo.class.isInterface(), "CallerInfo should be an interface");
      assertTrue(
          ExecutionRequest.CallerInfo.class.isMemberClass(), "CallerInfo should be a member class");
    }

    @Test
    @DisplayName("CallerInfo should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionRequest.CallerInfo.class.getModifiers()),
          "CallerInfo should be public");
    }

    @Test
    @DisplayName("CallerInfo should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.CallerInfo.class.getMethod("getId");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("CallerInfo should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.CallerInfo.class.getMethod("getType");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("CallerInfo should have getPermissions method")
    void shouldHaveGetPermissionsMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.CallerInfo.class.getMethod("getPermissions");
      assertEquals(Set.class, method.getReturnType(), "Return type should be Set");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("ExecutionRequest should have at least 11 methods")
    void shouldHaveAtLeastElevenMethods() {
      Method[] methods = ExecutionRequest.class.getDeclaredMethods();
      assertTrue(
          methods.length >= 11,
          "ExecutionRequest should have at least 11 methods, found: " + methods.length);
    }

    @Test
    @DisplayName("CallerInfo should have 3 methods")
    void callerInfoShouldHaveThreeMethods() {
      Method[] methods = ExecutionRequest.CallerInfo.class.getDeclaredMethods();
      assertEquals(3, methods.length, "CallerInfo should have 3 methods");
    }

    @Test
    @DisplayName("All methods should be public and abstract")
    void allMethodsShouldBePublicAbstract() {
      Method[] methods = ExecutionRequest.class.getDeclaredMethods();
      for (Method method : methods) {
        int modifiers = method.getModifiers();
        assertTrue(
            Modifier.isPublic(modifiers) && Modifier.isAbstract(modifiers),
            method.getName() + " should be public and abstract");
      }
    }
  }

  // ========================================================================
  // Nested Type Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Type Count Tests")
  class NestedTypeCountTests {

    @Test
    @DisplayName("ExecutionRequest should have 1 nested enum")
    void shouldHaveOneNestedEnum() {
      Class<?>[] nestedClasses = ExecutionRequest.class.getDeclaredClasses();
      int enumCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isEnum()) {
          enumCount++;
        }
      }
      assertEquals(1, enumCount, "ExecutionRequest should have 1 nested enum");
    }

    @Test
    @DisplayName("ExecutionRequest should have 1 nested interface")
    void shouldHaveOneNestedInterface() {
      Class<?>[] nestedClasses = ExecutionRequest.class.getDeclaredClasses();
      int interfaceCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isInterface() && !nested.isAnnotation()) {
          interfaceCount++;
        }
      }
      assertEquals(1, interfaceCount, "ExecutionRequest should have 1 nested interface");
    }
  }
}
