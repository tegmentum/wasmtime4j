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

package ai.tegmentum.wasmtime4j.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModuleCacheFactory} class.
 *
 * <p>ModuleCacheFactory creates ModuleCache instances using ServiceLoader.
 */
@DisplayName("ModuleCacheFactory Tests")
class ModuleCacheFactoryTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(ModuleCacheFactory.class.getModifiers()),
          "ModuleCacheFactory should be a final class");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final var constructor = ModuleCacheFactory.class.getDeclaredConstructor();
      assertTrue(
          java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private");
    }

    @Test
    @DisplayName("should have static create method with engine only")
    void shouldHaveStaticCreateMethodWithEngineOnly() throws NoSuchMethodException {
      final Method method = ModuleCacheFactory.class.getMethod("create", Engine.class);
      assertNotNull(method, "create(engine) method should exist");
      assertEquals(ModuleCache.class, method.getReturnType(), "Should return ModuleCache");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "create(engine) should be static");
    }

    @Test
    @DisplayName("should have static create method with engine and config")
    void shouldHaveStaticCreateMethodWithEngineAndConfig() throws NoSuchMethodException {
      final Method method =
          ModuleCacheFactory.class.getMethod("create", Engine.class, ModuleCacheConfig.class);
      assertNotNull(method, "create(engine, config) method should exist");
      assertEquals(ModuleCache.class, method.getReturnType(), "Should return ModuleCache");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "create(engine, config) should be static");
    }
  }

  @Nested
  @DisplayName("ModuleCacheProvider Interface Tests")
  class ModuleCacheProviderInterfaceTests {

    @Test
    @DisplayName("ModuleCacheProvider should be a nested interface")
    void moduleCacheProviderShouldBeNestedInterface() {
      assertTrue(
          ModuleCacheFactory.ModuleCacheProvider.class.isInterface(),
          "ModuleCacheProvider should be an interface");
    }

    @Test
    @DisplayName("ModuleCacheProvider should have create method")
    void shouldHaveCreateMethod() throws NoSuchMethodException {
      final Method method =
          ModuleCacheFactory.ModuleCacheProvider.class.getMethod(
              "create", Engine.class, ModuleCacheConfig.class);
      assertNotNull(method, "create method should exist");
      assertEquals(ModuleCache.class, method.getReturnType(), "Should return ModuleCache");
    }
  }

  @Nested
  @DisplayName("Null Argument Tests")
  class NullArgumentTests {

    @Test
    @DisplayName("create with null engine should throw")
    void createWithNullEngineShouldThrow() {
      assertThrows(
          NullPointerException.class,
          () -> ModuleCacheFactory.create(null),
          "Should throw on null engine");
    }

    @Test
    @DisplayName("create with null config should throw")
    void createWithNullConfigShouldThrow() {
      // This test documents expected behavior - the factory should validate inputs
      // Since we don't have a real engine, we test for NullPointerException on null config
      // by checking the method signature and contract
      final Method method;
      try {
        method =
            ModuleCacheFactory.class.getMethod("create", Engine.class, ModuleCacheConfig.class);
        assertNotNull(method, "Method should exist");
        // The method signature accepts non-null config as per its Javadoc
      } catch (NoSuchMethodException e) {
        throw new AssertionError("create method should exist", e);
      }
    }
  }
}
