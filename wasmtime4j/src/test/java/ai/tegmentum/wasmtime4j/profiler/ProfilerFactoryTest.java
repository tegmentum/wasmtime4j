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

package ai.tegmentum.wasmtime4j.profiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ProfilerFactory} class.
 *
 * <p>ProfilerFactory creates Profiler instances using ServiceLoader.
 */
@DisplayName("ProfilerFactory Tests")
class ProfilerFactoryTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(ProfilerFactory.class.getModifiers()),
          "ProfilerFactory should be a final class");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final var constructor = ProfilerFactory.class.getDeclaredConstructor();
      assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
    }

    @Test
    @DisplayName("should have static create method")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      final Method method = ProfilerFactory.class.getMethod("create");
      assertNotNull(method, "create() method should exist");
      assertEquals(Profiler.class, method.getReturnType(), "Should return Profiler");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create() should be static");
    }
  }

  @Nested
  @DisplayName("ProfilerProvider Interface Tests")
  class ProfilerProviderInterfaceTests {

    @Test
    @DisplayName("ProfilerProvider should be a nested interface")
    void profilerProviderShouldBeNestedInterface() {
      assertTrue(
          ProfilerFactory.ProfilerProvider.class.isInterface(),
          "ProfilerProvider should be an interface");
    }

    @Test
    @DisplayName("ProfilerProvider should have create method")
    void shouldHaveCreateMethod() throws NoSuchMethodException {
      final Method method = ProfilerFactory.ProfilerProvider.class.getMethod("create");
      assertNotNull(method, "create method should exist");
      assertEquals(Profiler.class, method.getReturnType(), "Should return Profiler");
    }
  }
}
