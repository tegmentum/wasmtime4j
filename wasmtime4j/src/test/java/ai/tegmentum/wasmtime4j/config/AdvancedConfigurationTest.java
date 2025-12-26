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

package ai.tegmentum.wasmtime4j.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the AdvancedConfiguration interface.
 *
 * <p>This test class verifies the interface structure and method signatures for
 * AdvancedConfiguration using reflection-based testing.
 */
@DisplayName("AdvancedConfiguration Tests")
class AdvancedConfigurationTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("AdvancedConfiguration should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          AdvancedConfiguration.class.isInterface(),
          "AdvancedConfiguration should be an interface");
    }

    @Test
    @DisplayName("AdvancedConfiguration should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(AdvancedConfiguration.class.getModifiers()),
          "AdvancedConfiguration should be public");
    }

    @Test
    @DisplayName("AdvancedConfiguration should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = AdvancedConfiguration.class.getInterfaces();
      assertEquals(
          0, interfaces.length, "AdvancedConfiguration should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getConfigurationName method")
    void shouldHaveGetConfigurationNameMethod() throws NoSuchMethodException {
      Method method = AdvancedConfiguration.class.getMethod("getConfigurationName");
      assertNotNull(method, "getConfigurationName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getConfigurationType method")
    void shouldHaveGetConfigurationTypeMethod() throws NoSuchMethodException {
      Method method = AdvancedConfiguration.class.getMethod("getConfigurationType");
      assertNotNull(method, "getConfigurationType method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      Method method = AdvancedConfiguration.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getValue method")
    void shouldHaveGetValueMethod() throws NoSuchMethodException {
      Method method = AdvancedConfiguration.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have exactly 4 declared methods")
    void shouldHaveExactlyFourDeclaredMethods() {
      Method[] methods = AdvancedConfiguration.class.getDeclaredMethods();
      assertEquals(
          4, methods.length, "AdvancedConfiguration should have exactly 4 declared methods");
    }
  }
}
