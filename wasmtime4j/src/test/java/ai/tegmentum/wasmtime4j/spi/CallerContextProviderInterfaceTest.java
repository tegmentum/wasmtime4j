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

package ai.tegmentum.wasmtime4j.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Caller;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CallerContextProvider} interface.
 *
 * <p>CallerContextProvider is a service provider interface for accessing caller context in host
 * functions.
 */
@DisplayName("CallerContextProvider Interface Tests")
class CallerContextProviderInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          CallerContextProvider.class.isInterface(),
          "CallerContextProvider should be an interface");
    }

    @Test
    @DisplayName("should have getCurrentCaller method")
    void shouldHaveGetCurrentCallerMethod() throws NoSuchMethodException {
      final Method method = CallerContextProvider.class.getMethod("getCurrentCaller");
      assertNotNull(method, "getCurrentCaller method should exist");
      assertEquals(Caller.class, method.getReturnType(), "Should return Caller");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("getCurrentCaller should have no parameters")
    void getCurrentCallerShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = CallerContextProvider.class.getMethod("getCurrentCaller");
      assertEquals(0, method.getParameterCount(), "getCurrentCaller should have no parameters");
    }

    @Test
    @DisplayName("getCurrentCaller should not be default")
    void getCurrentCallerShouldNotBeDefault() throws NoSuchMethodException {
      final Method method = CallerContextProvider.class.getMethod("getCurrentCaller");
      assertFalse(method.isDefault(), "getCurrentCaller should not be a default method");
    }

    @Test
    @DisplayName("getCurrentCaller should have type parameter")
    void getCurrentCallerShouldHaveTypeParameter() throws NoSuchMethodException {
      final Method method = CallerContextProvider.class.getMethod("getCurrentCaller");
      final TypeVariable<?>[] typeParams = method.getTypeParameters();
      assertEquals(1, typeParams.length, "getCurrentCaller should have one type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }
  }

  @Nested
  @DisplayName("Functional Interface Tests")
  class FunctionalInterfaceTests {

    @Test
    @DisplayName("should have exactly one abstract method")
    void shouldHaveExactlyOneAbstractMethod() {
      int abstractMethodCount = 0;
      for (final Method method : CallerContextProvider.class.getDeclaredMethods()) {
        if (!method.isDefault() && !java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
          abstractMethodCount++;
        }
      }
      assertEquals(1, abstractMethodCount, "Should have exactly one abstract method");
    }
  }

  @Nested
  @DisplayName("SPI Convention Tests")
  class SpiConventionTests {

    @Test
    @DisplayName("interface should be in spi package")
    void interfaceShouldBeInSpiPackage() {
      final String packageName = CallerContextProvider.class.getPackage().getName();
      assertTrue(packageName.endsWith(".spi"), "Interface should be in an spi package");
    }

    @Test
    @DisplayName("interface should have Provider suffix")
    void interfaceShouldHaveProviderSuffix() {
      final String simpleName = CallerContextProvider.class.getSimpleName();
      assertTrue(simpleName.endsWith("Provider"), "Interface should have Provider suffix");
    }
  }
}
