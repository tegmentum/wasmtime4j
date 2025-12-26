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
 * Tests for the ResourceLimits interface.
 *
 * <p>This test class verifies the interface structure and method signatures for ResourceLimits
 * using reflection-based testing.
 */
@DisplayName("ResourceLimits Tests")
class ResourceLimitsTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ResourceLimits should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ResourceLimits.class.isInterface(), "ResourceLimits should be an interface");
    }

    @Test
    @DisplayName("ResourceLimits should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ResourceLimits.class.getModifiers()),
          "ResourceLimits should be public");
    }

    @Test
    @DisplayName("ResourceLimits should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = ResourceLimits.class.getInterfaces();
      assertEquals(0, interfaces.length, "ResourceLimits should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getMemoryLimitBytes method")
    void shouldHaveGetMemoryLimitBytesMethod() throws NoSuchMethodException {
      Method method = ResourceLimits.class.getMethod("getMemoryLimitBytes");
      assertNotNull(method, "getMemoryLimitBytes method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getExecutionTimeLimitMs method")
    void shouldHaveGetExecutionTimeLimitMsMethod() throws NoSuchMethodException {
      Method method = ResourceLimits.class.getMethod("getExecutionTimeLimitMs");
      assertNotNull(method, "getExecutionTimeLimitMs method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getMaxStackDepth method")
    void shouldHaveGetMaxStackDepthMethod() throws NoSuchMethodException {
      Method method = ResourceLimits.class.getMethod("getMaxStackDepth");
      assertNotNull(method, "getMaxStackDepth method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      Method method = ResourceLimits.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have exactly 4 declared methods")
    void shouldHaveExactlyFourDeclaredMethods() {
      Method[] methods = ResourceLimits.class.getDeclaredMethods();
      assertEquals(4, methods.length, "ResourceLimits should have exactly 4 declared methods");
    }
  }
}
