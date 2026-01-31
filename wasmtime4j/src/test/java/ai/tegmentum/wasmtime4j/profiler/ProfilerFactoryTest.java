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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ProfilerFactory} - ServiceLoader-based factory for Profiler instances.
 *
 * <p>Validates utility class structure and ServiceLoader-based factory behavior. No native runtime
 * providers are available in the test environment, so create() should throw WasmException.
 */
@DisplayName("ProfilerFactory Tests")
class ProfilerFactoryTest {

  @Nested
  @DisplayName("Utility Class Structure Tests")
  class UtilityClassStructureTests {

    @Test
    @DisplayName("class should be final")
    void classShouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(ProfilerFactory.class.getModifiers()),
          "ProfilerFactory should be a final class");
    }

    @Test
    @DisplayName("constructor should be private")
    void constructorShouldBePrivate() throws NoSuchMethodException {
      final java.lang.reflect.Constructor<ProfilerFactory> constructor =
          ProfilerFactory.class.getDeclaredConstructor();
      assertTrue(
          java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
          "ProfilerFactory constructor should be private");
    }
  }

  @Nested
  @DisplayName("Create Method Tests")
  class CreateMethodTests {

    @Test
    @DisplayName("create should throw WasmException when no provider available")
    void createShouldThrowWhenNoProviderAvailable() {
      final WasmException ex =
          assertThrows(
              WasmException.class,
              ProfilerFactory::create,
              "create() should throw WasmException when no ProfilerProvider is on classpath");
      assertNotNull(ex.getMessage(), "Exception should have a descriptive message");
      assertTrue(
          ex.getMessage().contains("No Profiler implementation found"),
          "Exception message should indicate missing implementation, got: " + ex.getMessage());
    }
  }

  @Nested
  @DisplayName("ProfilerProvider Interface Tests")
  class ProfilerProviderInterfaceTests {

    @Test
    @DisplayName("ProfilerProvider should be a public interface")
    void profilerProviderShouldBePublicInterface() {
      assertTrue(
          ProfilerFactory.ProfilerProvider.class.isInterface(),
          "ProfilerProvider should be an interface");
    }

    @Test
    @DisplayName("ProfilerProvider should have create method")
    void profilerProviderShouldHaveCreateMethod() throws NoSuchMethodException {
      final java.lang.reflect.Method createMethod =
          ProfilerFactory.ProfilerProvider.class.getDeclaredMethod("create");
      assertNotNull(createMethod, "ProfilerProvider should have a create() method");
      assertTrue(
          Profiler.class.isAssignableFrom(createMethod.getReturnType()),
          "create() should return Profiler");
    }
  }
}
