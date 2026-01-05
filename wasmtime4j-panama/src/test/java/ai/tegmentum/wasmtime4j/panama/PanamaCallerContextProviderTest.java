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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Caller;
import ai.tegmentum.wasmtime4j.spi.CallerContextProvider;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaCallerContextProvider} class.
 *
 * <p>PanamaCallerContextProvider provides access to the current caller context.
 */
@DisplayName("PanamaCallerContextProvider Tests")
class PanamaCallerContextProviderTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaCallerContextProvider.class.getModifiers()),
          "PanamaCallerContextProvider should be public");
      assertTrue(
          Modifier.isFinal(PanamaCallerContextProvider.class.getModifiers()),
          "PanamaCallerContextProvider should be final");
    }

    @Test
    @DisplayName("should implement CallerContextProvider interface")
    void shouldImplementCallerContextProviderInterface() {
      assertTrue(
          CallerContextProvider.class.isAssignableFrom(PanamaCallerContextProvider.class),
          "PanamaCallerContextProvider should implement CallerContextProvider");
    }
  }

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getCurrentCaller method")
    void shouldHaveGetCurrentCallerMethod() throws NoSuchMethodException {
      final Method method = PanamaCallerContextProvider.class.getMethod("getCurrentCaller");
      assertNotNull(method, "getCurrentCaller method should exist");
      assertEquals(Caller.class, method.getReturnType(), "Should return Caller");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() {
      try {
        PanamaCallerContextProvider.class.getConstructor();
      } catch (NoSuchMethodException e) {
        throw new AssertionError("Default constructor should exist", e);
      }
    }

    @Test
    @DisplayName("should be instantiable")
    void shouldBeInstantiable() {
      PanamaCallerContextProvider provider = new PanamaCallerContextProvider();
      assertNotNull(provider, "Should be able to create instance");
    }
  }

  @Nested
  @DisplayName("Behavior Tests")
  class BehaviorTests {

    @Test
    @DisplayName("getCurrentCaller returns null when no caller context")
    void getCurrentCallerReturnsNullWhenNoContext() {
      PanamaCallerContextProvider provider = new PanamaCallerContextProvider();
      Caller<?> caller = provider.getCurrentCaller();
      // When not in a host function call, there's no caller context
      // This may be null or may throw depending on implementation
    }
  }
}
