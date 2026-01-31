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

package ai.tegmentum.wasmtime4j.wasi.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiHttpFactory} utility class.
 *
 * <p>Verifies private constructor (utility class pattern), isAvailable(), getImplementationName(),
 * and createContext() null validation. Tests that do not require a native runtime.
 */
@DisplayName("WasiHttpFactory Tests")
class WasiHttpFactoryTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiHttpFactory.class.getModifiers()),
          "WasiHttpFactory should be a final class");
    }

    @Test
    @DisplayName("constructor should be private")
    void constructorShouldBePrivate() throws NoSuchMethodException {
      final Constructor<WasiHttpFactory> constructor =
          WasiHttpFactory.class.getDeclaredConstructor();
      assertTrue(
          Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private");
    }

    @Test
    @DisplayName("private constructor should be invocable via reflection")
    void privateConstructorShouldBeInvocableViaReflection() throws Exception {
      final Constructor<WasiHttpFactory> constructor =
          WasiHttpFactory.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      final WasiHttpFactory instance = constructor.newInstance();
      assertNotNull(instance, "Should be able to create instance via reflection");
    }
  }

  @Nested
  @DisplayName("IsAvailable Tests")
  class IsAvailableTests {

    @Test
    @DisplayName("isAvailable should return a boolean value")
    void isAvailableShouldReturnBoolean() {
      final boolean available = WasiHttpFactory.isAvailable();
      // In test environment without native implementations, this should be false
      // but the method itself should not throw
      assertFalse(
          available,
          "isAvailable should return false when no implementation is on classpath");
    }

    @Test
    @DisplayName("isAvailable should be consistent across multiple calls")
    void isAvailableShouldBeConsistent() {
      final boolean first = WasiHttpFactory.isAvailable();
      final boolean second = WasiHttpFactory.isAvailable();
      assertEquals(first, second, "isAvailable should return consistent results");
    }
  }

  @Nested
  @DisplayName("GetImplementationName Tests")
  class GetImplementationNameTests {

    @Test
    @DisplayName("getImplementationName should return a non-null string")
    void getImplementationNameShouldReturnNonNull() {
      final String name = WasiHttpFactory.getImplementationName();
      assertNotNull(name, "Implementation name should not be null");
    }

    @Test
    @DisplayName("getImplementationName should return None when no impl available")
    void getImplementationNameShouldReturnNoneWhenNoImpl() {
      final String name = WasiHttpFactory.getImplementationName();
      assertEquals(
          "None", name,
          "Implementation name should be 'None' when no implementation is available");
    }

    @Test
    @DisplayName("getImplementationName should return one of the expected values")
    void getImplementationNameShouldReturnExpectedValue() {
      final String name = WasiHttpFactory.getImplementationName();
      assertTrue(
          "Panama".equals(name) || "JNI".equals(name) || "None".equals(name),
          "Implementation name should be Panama, JNI, or None, got: " + name);
    }

    @Test
    @DisplayName("getImplementationName should be consistent across multiple calls")
    void getImplementationNameShouldBeConsistent() {
      final String first = WasiHttpFactory.getImplementationName();
      final String second = WasiHttpFactory.getImplementationName();
      assertEquals(
          first, second,
          "getImplementationName should return consistent results");
    }
  }

  @Nested
  @DisplayName("CreateContext Validation Tests")
  class CreateContextValidationTests {

    @Test
    @DisplayName("createContext with null config should throw IllegalArgumentException")
    void createContextWithNullConfigShouldThrow() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiHttpFactory.createContext(null),
          "createContext should throw for null config");
    }
  }

  @Nested
  @DisplayName("IsAvailable and GetImplementationName Consistency Tests")
  class ConsistencyTests {

    @Test
    @DisplayName("isAvailable false should correspond to None implementation name")
    void isAvailableFalseShouldCorrespondToNone() {
      final boolean available = WasiHttpFactory.isAvailable();
      final String name = WasiHttpFactory.getImplementationName();
      if (!available) {
        assertEquals(
            "None", name,
            "When isAvailable is false, implementation name should be None");
      } else {
        assertTrue(
            "Panama".equals(name) || "JNI".equals(name),
            "When isAvailable is true, name should be Panama or JNI, got: " + name);
      }
    }
  }
}
