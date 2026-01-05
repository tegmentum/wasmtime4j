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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.cache.ModuleCache;
import ai.tegmentum.wasmtime4j.cache.ModuleCacheConfig;
import ai.tegmentum.wasmtime4j.cache.ModuleCacheFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaModuleCacheProvider} class.
 *
 * <p>PanamaModuleCacheProvider provides ModuleCache for Panama Engine via ServiceLoader.
 */
@DisplayName("PanamaModuleCacheProvider Tests")
class PanamaModuleCacheProviderTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaModuleCacheProvider.class.getModifiers()),
          "PanamaModuleCacheProvider should be public");
      assertTrue(
          Modifier.isFinal(PanamaModuleCacheProvider.class.getModifiers()),
          "PanamaModuleCacheProvider should be final");
    }

    @Test
    @DisplayName("should implement ModuleCacheProvider interface")
    void shouldImplementModuleCacheProviderInterface() {
      assertTrue(
          ModuleCacheFactory.ModuleCacheProvider.class.isAssignableFrom(
              PanamaModuleCacheProvider.class),
          "PanamaModuleCacheProvider should implement ModuleCacheProvider");
    }
  }

  @Nested
  @DisplayName("ModuleCacheProvider Method Tests")
  class ModuleCacheProviderMethodTests {

    @Test
    @DisplayName("should have create method")
    void shouldHaveCreateMethod() throws NoSuchMethodException {
      final Method method =
          PanamaModuleCacheProvider.class.getMethod(
              "create", Engine.class, ModuleCacheConfig.class);
      assertNotNull(method, "create method should exist");
      assertEquals(ModuleCache.class, method.getReturnType(), "Should return ModuleCache");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      var constructor = PanamaModuleCacheProvider.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
    }

    @Test
    @DisplayName("should be instantiable")
    void shouldBeInstantiable() {
      PanamaModuleCacheProvider provider = new PanamaModuleCacheProvider();
      assertNotNull(provider, "Should be able to create instance");
    }
  }
}
