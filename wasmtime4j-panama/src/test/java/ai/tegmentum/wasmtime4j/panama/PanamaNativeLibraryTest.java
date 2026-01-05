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

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaNativeLibrary} class.
 *
 * <p>PanamaNativeLibrary is a helper class for finding native functions.
 */
@DisplayName("PanamaNativeLibrary Tests")
class PanamaNativeLibraryTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be package-private final class")
    void shouldBePackagePrivateFinalClass() {
      assertTrue(
          !Modifier.isPublic(PanamaNativeLibrary.class.getModifiers()),
          "PanamaNativeLibrary should be package-private");
      assertTrue(
          Modifier.isFinal(PanamaNativeLibrary.class.getModifiers()),
          "PanamaNativeLibrary should be final");
    }
  }

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have findFunction method")
    void shouldHaveFindFunctionMethod() throws NoSuchMethodException {
      final Method method =
          PanamaNativeLibrary.class.getDeclaredMethod(
              "findFunction", String.class, FunctionDescriptor.class);
      assertNotNull(method, "findFunction method should exist");
      assertEquals(MethodHandle.class, method.getReturnType(), "Should return MethodHandle");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = PanamaNativeLibrary.class.getDeclaredConstructor();
      assertNotNull(constructor, "Private constructor should exist");
      assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
    }
  }
}
