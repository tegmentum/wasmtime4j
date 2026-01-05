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

import ai.tegmentum.wasmtime4j.profiler.Profiler;
import ai.tegmentum.wasmtime4j.profiler.ProfilerFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaProfilerProvider} class.
 *
 * <p>PanamaProfilerProvider is a ServiceLoader provider for Panama Profiler implementation.
 */
@DisplayName("PanamaProfilerProvider Tests")
class PanamaProfilerProviderTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaProfilerProvider.class.getModifiers()),
          "PanamaProfilerProvider should be public");
      assertTrue(
          Modifier.isFinal(PanamaProfilerProvider.class.getModifiers()),
          "PanamaProfilerProvider should be final");
    }

    @Test
    @DisplayName("should implement ProfilerProvider interface")
    void shouldImplementProfilerProviderInterface() {
      assertTrue(
          ProfilerFactory.ProfilerProvider.class.isAssignableFrom(PanamaProfilerProvider.class),
          "PanamaProfilerProvider should implement ProfilerProvider");
    }
  }

  @Nested
  @DisplayName("ProfilerProvider Method Tests")
  class ProfilerProviderMethodTests {

    @Test
    @DisplayName("should have create method")
    void shouldHaveCreateMethod() throws NoSuchMethodException {
      final Method method = PanamaProfilerProvider.class.getMethod("create");
      assertNotNull(method, "create method should exist");
      assertEquals(Profiler.class, method.getReturnType(), "Should return Profiler");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      var constructor = PanamaProfilerProvider.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should be instantiable")
    void shouldBeInstantiable() {
      PanamaProfilerProvider provider = new PanamaProfilerProvider();
      assertNotNull(provider, "Should be able to create instance");
    }
  }
}
