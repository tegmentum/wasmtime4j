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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GlobalRegistry} class.
 *
 * <p>GlobalRegistry manages cross-module global sharing in Panama FFI implementation.
 */
@DisplayName("GlobalRegistry Tests")
class GlobalRegistryTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(GlobalRegistry.class.getModifiers()),
          "GlobalRegistry should be public");
      assertTrue(
          Modifier.isFinal(GlobalRegistry.class.getModifiers()), "GlobalRegistry should be final");
    }

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(GlobalRegistry.class),
          "GlobalRegistry should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Registration Method Tests")
  class RegistrationMethodTests {

    @Test
    @DisplayName("should have registerGlobal method")
    void shouldHaveRegisterGlobalMethod() throws NoSuchMethodException {
      final Method method =
          GlobalRegistry.class.getMethod("registerGlobal", String.class, PanamaGlobal.class);
      assertNotNull(method, "registerGlobal method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have lookupGlobal method")
    void shouldHaveLookupGlobalMethod() throws NoSuchMethodException {
      final Method method = GlobalRegistry.class.getMethod("lookupGlobal", String.class);
      assertNotNull(method, "lookupGlobal method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have unregisterGlobal method")
    void shouldHaveUnregisterGlobalMethod() throws NoSuchMethodException {
      final Method method = GlobalRegistry.class.getMethod("unregisterGlobal", String.class);
      assertNotNull(method, "unregisterGlobal method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Query Method Tests")
  class QueryMethodTests {

    @Test
    @DisplayName("should have isRegistered method")
    void shouldHaveIsRegisteredMethod() throws NoSuchMethodException {
      final Method method = GlobalRegistry.class.getMethod("isRegistered", String.class);
      assertNotNull(method, "isRegistered method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getRegisteredCount method")
    void shouldHaveGetRegisteredCountMethod() throws NoSuchMethodException {
      final Method method = GlobalRegistry.class.getMethod("getRegisteredCount");
      assertNotNull(method, "getRegisteredCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws NoSuchMethodException {
      final Method method = GlobalRegistry.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have isClosed method")
    void shouldHaveIsClosedMethod() throws NoSuchMethodException {
      final Method method = GlobalRegistry.class.getMethod("isClosed");
      assertNotNull(method, "isClosed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = GlobalRegistry.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Deprecated Method Tests")
  class DeprecatedMethodTests {

    @Test
    @DisplayName("should have deprecated getRegistryPointer method")
    void shouldHaveDeprecatedGetRegistryPointerMethod() throws NoSuchMethodException {
      final Method method = GlobalRegistry.class.getMethod("getRegistryPointer");
      assertNotNull(method, "getRegistryPointer method should exist");
      assertTrue(
          method.isAnnotationPresent(Deprecated.class), "getRegistryPointer should be deprecated");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("constructor should require PanamaStore parameter")
    void constructorShouldRequirePanamaStoreParameter() {
      assertThrows(
          NoSuchMethodException.class,
          () -> GlobalRegistry.class.getConstructor(),
          "Should not have no-arg constructor");

      try {
        GlobalRegistry.class.getConstructor(PanamaStore.class);
      } catch (NoSuchMethodException e) {
        throw new AssertionError("Constructor with PanamaStore should exist", e);
      }
    }
  }

  @Nested
  @DisplayName("ToString Method Tests")
  class ToStringMethodTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = GlobalRegistry.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }
}
