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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ExecutionQuotas interface.
 *
 * <p>This test class verifies the interface structure, methods, and nested interfaces for
 * ExecutionQuotas using reflection-based testing.
 */
@DisplayName("ExecutionQuotas Tests")
class ExecutionQuotasTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ExecutionQuotas should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ExecutionQuotas.class.isInterface(), "ExecutionQuotas should be an interface");
    }

    @Test
    @DisplayName("ExecutionQuotas should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionQuotas.class.getModifiers()),
          "ExecutionQuotas should be public");
    }

    @Test
    @DisplayName("ExecutionQuotas should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = ExecutionQuotas.class.getInterfaces();
      assertEquals(0, interfaces.length, "ExecutionQuotas should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getFuelQuota method")
    void shouldHaveGetFuelQuotaMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("getFuelQuota");
      assertNotNull(method, "getFuelQuota method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have setFuelQuota method")
    void shouldHaveSetFuelQuotaMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("setFuelQuota", long.class);
      assertNotNull(method, "setFuelQuota method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "setFuelQuota should have 1 parameter");
    }

    @Test
    @DisplayName("should have getRemainingFuel method")
    void shouldHaveGetRemainingFuelMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("getRemainingFuel");
      assertNotNull(method, "getRemainingFuel method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getMemoryQuota method")
    void shouldHaveGetMemoryQuotaMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("getMemoryQuota");
      assertNotNull(method, "getMemoryQuota method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have setMemoryQuota method")
    void shouldHaveSetMemoryQuotaMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("setMemoryQuota", long.class);
      assertNotNull(method, "setMemoryQuota method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getTimeQuota method")
    void shouldHaveGetTimeQuotaMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("getTimeQuota");
      assertNotNull(method, "getTimeQuota method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have setTimeQuota method")
    void shouldHaveSetTimeQuotaMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("setTimeQuota", long.class);
      assertNotNull(method, "setTimeQuota method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getRemainingTime method")
    void shouldHaveGetRemainingTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("getRemainingTime");
      assertNotNull(method, "getRemainingTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getInstructionQuota method")
    void shouldHaveGetInstructionQuotaMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("getInstructionQuota");
      assertNotNull(method, "getInstructionQuota method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have setInstructionQuota method")
    void shouldHaveSetInstructionQuotaMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("setInstructionQuota", long.class);
      assertNotNull(method, "setInstructionQuota method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getRemainingInstructions method")
    void shouldHaveGetRemainingInstructionsMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("getRemainingInstructions");
      assertNotNull(method, "getRemainingInstructions method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have isQuotaExceeded method")
    void shouldHaveIsQuotaExceededMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("isQuotaExceeded");
      assertNotNull(method, "isQuotaExceeded method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getUsage method")
    void shouldHaveGetUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("getUsage");
      assertNotNull(method, "getUsage method should exist");
      assertEquals(
          ExecutionQuotas.QuotaUsage.class,
          method.getReturnType(),
          "Return type should be QuotaUsage");
    }

    @Test
    @DisplayName("should have reset method")
    void shouldHaveResetMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("reset");
      assertNotNull(method, "reset method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // QuotaUsage Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("QuotaUsage Interface Tests")
  class QuotaUsageTests {

    @Test
    @DisplayName("QuotaUsage should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ExecutionQuotas.QuotaUsage.class.isInterface(), "QuotaUsage should be an interface");
      assertTrue(
          ExecutionQuotas.QuotaUsage.class.isMemberClass(), "QuotaUsage should be a member class");
    }

    @Test
    @DisplayName("QuotaUsage should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionQuotas.QuotaUsage.class.getModifiers()),
          "QuotaUsage should be public");
    }

    @Test
    @DisplayName("QuotaUsage should have getFuelUsage method")
    void shouldHaveGetFuelUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.QuotaUsage.class.getMethod("getFuelUsage");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("QuotaUsage should have getMemoryUsage method")
    void shouldHaveGetMemoryUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.QuotaUsage.class.getMethod("getMemoryUsage");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("QuotaUsage should have getTimeUsage method")
    void shouldHaveGetTimeUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.QuotaUsage.class.getMethod("getTimeUsage");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("QuotaUsage should have getInstructionUsage method")
    void shouldHaveGetInstructionUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.QuotaUsage.class.getMethod("getInstructionUsage");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("QuotaUsage should have getMaxUsage method")
    void shouldHaveGetMaxUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.QuotaUsage.class.getMethod("getMaxUsage");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("ExecutionQuotas should have at least 14 methods")
    void shouldHaveAtLeastFourteenMethods() {
      Method[] methods = ExecutionQuotas.class.getDeclaredMethods();
      assertTrue(
          methods.length >= 14,
          "ExecutionQuotas should have at least 14 methods, found: " + methods.length);
    }

    @Test
    @DisplayName("QuotaUsage should have 5 methods")
    void quotaUsageShouldHaveFiveMethods() {
      Method[] methods = ExecutionQuotas.QuotaUsage.class.getDeclaredMethods();
      assertEquals(5, methods.length, "QuotaUsage should have 5 methods");
    }

    @Test
    @DisplayName("All methods should be public and abstract")
    void allMethodsShouldBePublicAbstract() {
      Method[] methods = ExecutionQuotas.class.getDeclaredMethods();
      for (Method method : methods) {
        int modifiers = method.getModifiers();
        assertTrue(
            Modifier.isPublic(modifiers) && Modifier.isAbstract(modifiers),
            method.getName() + " should be public and abstract");
      }
    }
  }

  // ========================================================================
  // Nested Interface Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Interface Count Tests")
  class NestedInterfaceCountTests {

    @Test
    @DisplayName("ExecutionQuotas should have 1 nested interface")
    void shouldHaveOneNestedInterface() {
      Class<?>[] nestedClasses = ExecutionQuotas.class.getDeclaredClasses();
      int interfaceCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isInterface() && !nested.isAnnotation()) {
          interfaceCount++;
        }
      }
      assertEquals(1, interfaceCount, "ExecutionQuotas should have 1 nested interface");
    }
  }
}
