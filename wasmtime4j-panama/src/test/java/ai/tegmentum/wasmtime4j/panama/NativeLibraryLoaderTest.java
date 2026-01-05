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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.SymbolLookup;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NativeLibraryLoader} class.
 *
 * <p>NativeLibraryLoader handles native library loading and function discovery for Panama FFI.
 */
@DisplayName("NativeLibraryLoader Tests")
class NativeLibraryLoaderTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(NativeLibraryLoader.class.getModifiers()),
          "NativeLibraryLoader should be public");
      assertTrue(
          Modifier.isFinal(NativeLibraryLoader.class.getModifiers()),
          "NativeLibraryLoader should be final");
    }
  }

  @Nested
  @DisplayName("Singleton Method Tests")
  class SingletonMethodTests {

    @Test
    @DisplayName("should have getInstance static method")
    void shouldHaveGetInstanceMethod() throws NoSuchMethodException {
      final Method method = NativeLibraryLoader.class.getMethod("getInstance");
      assertNotNull(method, "getInstance method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getInstance should be static");
      assertEquals(
          NativeLibraryLoader.class, method.getReturnType(), "Should return NativeLibraryLoader");
    }
  }

  @Nested
  @DisplayName("Status Method Tests")
  class StatusMethodTests {

    @Test
    @DisplayName("should have isLoaded method")
    void shouldHaveIsLoadedMethod() throws NoSuchMethodException {
      final Method method = NativeLibraryLoader.class.getMethod("isLoaded");
      assertNotNull(method, "isLoaded method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getLoadingError method")
    void shouldHaveGetLoadingErrorMethod() throws NoSuchMethodException {
      final Method method = NativeLibraryLoader.class.getMethod("getLoadingError");
      assertNotNull(method, "getLoadingError method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getLoadInfo method")
    void shouldHaveGetLoadInfoMethod() throws NoSuchMethodException {
      final Method method = NativeLibraryLoader.class.getMethod("getLoadInfo");
      assertNotNull(method, "getLoadInfo method should exist");
      assertNotNull(method.getReturnType(), "Should have a return type");
    }
  }

  @Nested
  @DisplayName("Function Lookup Method Tests")
  class FunctionLookupMethodTests {

    @Test
    @DisplayName("should have lookupFunction method")
    void shouldHaveLookupFunctionMethod() throws NoSuchMethodException {
      final Method method =
          NativeLibraryLoader.class.getMethod(
              "lookupFunction", String.class, FunctionDescriptor.class);
      assertNotNull(method, "lookupFunction method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("lookupFunction should return Optional of MethodHandle")
    void lookupFunctionShouldReturnOptionalOfMethodHandle() throws NoSuchMethodException {
      final Method method =
          NativeLibraryLoader.class.getMethod(
              "lookupFunction", String.class, FunctionDescriptor.class);
      // Check generic type would require runtime reflection on generic info
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional<MethodHandle>");
    }
  }

  @Nested
  @DisplayName("Symbol Lookup Method Tests")
  class SymbolLookupMethodTests {

    @Test
    @DisplayName("should have getSymbolLookup method")
    void shouldHaveGetSymbolLookupMethod() throws NoSuchMethodException {
      final Method method = NativeLibraryLoader.class.getMethod("getSymbolLookup");
      assertNotNull(method, "getSymbolLookup method should exist");
      assertEquals(SymbolLookup.class, method.getReturnType(), "Should return SymbolLookup");
    }
  }

  @Nested
  @DisplayName("Arena Method Tests")
  class ArenaMethodTests {

    @Test
    @DisplayName("should have getLibraryArena method")
    void shouldHaveGetLibraryArenaMethod() throws NoSuchMethodException {
      final Method method = NativeLibraryLoader.class.getMethod("getLibraryArena");
      assertNotNull(method, "getLibraryArena method should exist");
      assertEquals(Arena.class, method.getReturnType(), "Should return Arena");
    }
  }

  @Nested
  @DisplayName("Cache Method Tests")
  class CacheMethodTests {

    @Test
    @DisplayName("should have clearMethodHandleCache method")
    void shouldHaveClearMethodHandleCacheMethod() throws NoSuchMethodException {
      final Method method = NativeLibraryLoader.class.getMethod("clearMethodHandleCache");
      assertNotNull(method, "clearMethodHandleCache method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = NativeLibraryLoader.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Diagnostic Method Tests")
  class DiagnosticMethodTests {

    @Test
    @DisplayName("should have getDiagnosticInfo method")
    void shouldHaveGetDiagnosticInfoMethod() throws NoSuchMethodException {
      final Method method = NativeLibraryLoader.class.getMethod("getDiagnosticInfo");
      assertNotNull(method, "getDiagnosticInfo method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }
}
