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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ComponentOrchestrator} and {@link ComponentOrchestrationConfig} interfaces.
 *
 * <p>This test class verifies the interface structure and method signatures for the component
 * orchestration APIs.
 */
@DisplayName("ComponentOrchestrator and ComponentOrchestrationConfig Tests")
class ComponentOrchestratorTest {

  // ========================================================================
  // ComponentOrchestrator Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("ComponentOrchestrator Interface Structure Tests")
  class ComponentOrchestratorStructureTests {

    @Test
    @DisplayName("ComponentOrchestrator should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ComponentOrchestrator.class.isInterface(),
          "ComponentOrchestrator should be an interface");
    }

    @Test
    @DisplayName("ComponentOrchestrator should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ComponentOrchestrator.class.getModifiers()),
          "ComponentOrchestrator should be public");
    }
  }

  @Nested
  @DisplayName("ComponentOrchestrator Method Tests")
  class ComponentOrchestratorMethodTests {

    @Test
    @DisplayName("should have start method with ComponentOrchestrationConfig parameter")
    void shouldHaveStartMethod() throws NoSuchMethodException {
      Method method =
          ComponentOrchestrator.class.getMethod("start", ComponentOrchestrationConfig.class);
      assertNotNull(method, "start method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "start should have 1 parameter");
      assertEquals(
          ComponentOrchestrationConfig.class,
          method.getParameterTypes()[0],
          "Parameter should be ComponentOrchestrationConfig");
    }

    @Test
    @DisplayName("should have stop method")
    void shouldHaveStopMethod() throws NoSuchMethodException {
      Method method = ComponentOrchestrator.class.getMethod("stop");
      assertNotNull(method, "stop method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "stop should have no parameters");
    }

    @Test
    @DisplayName("should have isRunning method")
    void shouldHaveIsRunningMethod() throws NoSuchMethodException {
      Method method = ComponentOrchestrator.class.getMethod("isRunning");
      assertNotNull(method, "isRunning method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(0, method.getParameterCount(), "isRunning should have no parameters");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      Method method = ComponentOrchestrator.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
      assertEquals(0, method.getParameterCount(), "getStatistics should have no parameters");
    }

    @Test
    @DisplayName("ComponentOrchestrator should have exactly 4 declared methods")
    void shouldHaveExactlyFourMethods() {
      Method[] methods = ComponentOrchestrator.class.getDeclaredMethods();
      assertEquals(4, methods.length, "ComponentOrchestrator should have exactly 4 methods");
    }
  }

  // ========================================================================
  // ComponentOrchestrationConfig Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("ComponentOrchestrationConfig Interface Structure Tests")
  class ComponentOrchestrationConfigStructureTests {

    @Test
    @DisplayName("ComponentOrchestrationConfig should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ComponentOrchestrationConfig.class.isInterface(),
          "ComponentOrchestrationConfig should be an interface");
    }

    @Test
    @DisplayName("ComponentOrchestrationConfig should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ComponentOrchestrationConfig.class.getModifiers()),
          "ComponentOrchestrationConfig should be public");
    }
  }

  @Nested
  @DisplayName("ComponentOrchestrationConfig Method Tests")
  class ComponentOrchestrationConfigMethodTests {

    @Test
    @DisplayName("should have getOrchestrationMode method")
    void shouldHaveGetOrchestrationModeMethod() throws NoSuchMethodException {
      Method method = ComponentOrchestrationConfig.class.getMethod("getOrchestrationMode");
      assertNotNull(method, "getOrchestrationMode method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
      assertEquals(0, method.getParameterCount(), "getOrchestrationMode should have no parameters");
    }

    @Test
    @DisplayName("should have getCoordinationTimeout method")
    void shouldHaveGetCoordinationTimeoutMethod() throws NoSuchMethodException {
      Method method = ComponentOrchestrationConfig.class.getMethod("getCoordinationTimeout");
      assertNotNull(method, "getCoordinationTimeout method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertEquals(
          0, method.getParameterCount(), "getCoordinationTimeout should have no parameters");
    }

    @Test
    @DisplayName("should have isOrchestrationEnabled method")
    void shouldHaveIsOrchestrationEnabledMethod() throws NoSuchMethodException {
      Method method = ComponentOrchestrationConfig.class.getMethod("isOrchestrationEnabled");
      assertNotNull(method, "isOrchestrationEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(
          0, method.getParameterCount(), "isOrchestrationEnabled should have no parameters");
    }

    @Test
    @DisplayName("should have getMaxConcurrentComponents method")
    void shouldHaveGetMaxConcurrentComponentsMethod() throws NoSuchMethodException {
      Method method = ComponentOrchestrationConfig.class.getMethod("getMaxConcurrentComponents");
      assertNotNull(method, "getMaxConcurrentComponents method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
      assertEquals(
          0, method.getParameterCount(), "getMaxConcurrentComponents should have no parameters");
    }

    @Test
    @DisplayName("ComponentOrchestrationConfig should have exactly 4 declared methods")
    void shouldHaveExactlyFourMethods() {
      Method[] methods = ComponentOrchestrationConfig.class.getDeclaredMethods();
      assertEquals(4, methods.length, "ComponentOrchestrationConfig should have exactly 4 methods");
    }
  }

  // ========================================================================
  // Integration Tests
  // ========================================================================

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("ComponentOrchestrator start method should accept ComponentOrchestrationConfig")
    void startMethodShouldAcceptOrchestrationConfig() throws NoSuchMethodException {
      Method startMethod =
          ComponentOrchestrator.class.getMethod("start", ComponentOrchestrationConfig.class);

      Class<?>[] parameterTypes = startMethod.getParameterTypes();
      assertEquals(1, parameterTypes.length, "start should have exactly 1 parameter");
      assertTrue(
          ComponentOrchestrationConfig.class.isAssignableFrom(parameterTypes[0]),
          "Parameter should be assignable to ComponentOrchestrationConfig");
    }

    @Test
    @DisplayName("Both interfaces should be in the same package")
    void bothInterfacesShouldBeInSamePackage() {
      String orchestratorPackage = ComponentOrchestrator.class.getPackage().getName();
      String configPackage = ComponentOrchestrationConfig.class.getPackage().getName();

      assertEquals(
          orchestratorPackage,
          configPackage,
          "ComponentOrchestrator and ComponentOrchestrationConfig should be in the same package");
      assertEquals(
          "ai.tegmentum.wasmtime4j",
          orchestratorPackage,
          "Package should be ai.tegmentum.wasmtime4j");
    }

    @Test
    @DisplayName("ComponentOrchestrator should not extend any interface")
    void orchestratorShouldNotExtendAnyInterface() {
      Class<?>[] interfaces = ComponentOrchestrator.class.getInterfaces();
      assertEquals(
          0, interfaces.length, "ComponentOrchestrator should not extend any other interface");
    }

    @Test
    @DisplayName("ComponentOrchestrationConfig should not extend any interface")
    void configShouldNotExtendAnyInterface() {
      Class<?>[] interfaces = ComponentOrchestrationConfig.class.getInterfaces();
      assertEquals(
          0,
          interfaces.length,
          "ComponentOrchestrationConfig should not extend any other interface");
    }
  }

  // ========================================================================
  // Method Signature Verification Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Signature Verification Tests")
  class MethodSignatureVerificationTests {

    @Test
    @DisplayName("All ComponentOrchestrator methods should be public and abstract")
    void allOrchestratorMethodsShouldBePublicAbstract() {
      Method[] methods = ComponentOrchestrator.class.getDeclaredMethods();

      for (Method method : methods) {
        int modifiers = method.getModifiers();
        assertTrue(
            Modifier.isPublic(modifiers) && Modifier.isAbstract(modifiers),
            "Method " + method.getName() + " should be public and abstract");
      }
    }

    @Test
    @DisplayName("All ComponentOrchestrationConfig methods should be public and abstract")
    void allConfigMethodsShouldBePublicAbstract() {
      Method[] methods = ComponentOrchestrationConfig.class.getDeclaredMethods();

      for (Method method : methods) {
        int modifiers = method.getModifiers();
        assertTrue(
            Modifier.isPublic(modifiers) && Modifier.isAbstract(modifiers),
            "Method " + method.getName() + " should be public and abstract");
      }
    }

    @Test
    @DisplayName("No methods should throw checked exceptions")
    void noMethodsShouldThrowCheckedExceptions() {
      Method[] orchestratorMethods = ComponentOrchestrator.class.getDeclaredMethods();
      Method[] configMethods = ComponentOrchestrationConfig.class.getDeclaredMethods();

      for (Method method : orchestratorMethods) {
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        assertEquals(
            0,
            exceptionTypes.length,
            "Method " + method.getName() + " should not throw exceptions");
      }

      for (Method method : configMethods) {
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        assertEquals(
            0,
            exceptionTypes.length,
            "Method " + method.getName() + " should not throw exceptions");
      }
    }
  }
}
