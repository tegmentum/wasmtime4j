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

import ai.tegmentum.wasmtime4j.wasi.WasiDirEntry;
import ai.tegmentum.wasmtime4j.wasi.WasiFileType;
import ai.tegmentum.wasmtime4j.wasi.WasiPermissions;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiDirEntryImpl} class.
 *
 * <p>PanamaWasiDirEntryImpl is the Panama implementation of WasiDirEntry.
 */
@DisplayName("PanamaWasiDirEntryImpl Tests")
class PanamaWasiDirEntryImplTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be package-private final class")
    void shouldBePackagePrivateFinalClass() {
      assertTrue(
          !Modifier.isPublic(PanamaWasiDirEntryImpl.class.getModifiers()),
          "PanamaWasiDirEntryImpl should be package-private");
      assertTrue(
          Modifier.isFinal(PanamaWasiDirEntryImpl.class.getModifiers()),
          "PanamaWasiDirEntryImpl should be final");
    }

    @Test
    @DisplayName("should implement WasiDirEntry interface")
    void shouldImplementWasiDirEntryInterface() {
      assertTrue(
          WasiDirEntry.class.isAssignableFrom(PanamaWasiDirEntryImpl.class),
          "PanamaWasiDirEntryImpl should implement WasiDirEntry");
    }
  }

  @Nested
  @DisplayName("Basic Property Method Tests")
  class BasicPropertyMethodTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDirEntryImpl.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDirEntryImpl.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(WasiFileType.class, method.getReturnType(), "Should return WasiFileType");
    }

    @Test
    @DisplayName("should have getInode method")
    void shouldHaveGetInodeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDirEntryImpl.class.getMethod("getInode");
      assertNotNull(method, "getInode method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDirEntryImpl.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Timestamp Method Tests")
  class TimestampMethodTests {

    @Test
    @DisplayName("should have getAccessTime method")
    void shouldHaveGetAccessTimeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDirEntryImpl.class.getMethod("getAccessTime");
      assertNotNull(method, "getAccessTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have getModificationTime method")
    void shouldHaveGetModificationTimeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDirEntryImpl.class.getMethod("getModificationTime");
      assertNotNull(method, "getModificationTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have getCreationTime method")
    void shouldHaveGetCreationTimeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDirEntryImpl.class.getMethod("getCreationTime");
      assertNotNull(method, "getCreationTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }
  }

  @Nested
  @DisplayName("Permission Method Tests")
  class PermissionMethodTests {

    @Test
    @DisplayName("should have getPermissions method")
    void shouldHaveGetPermissionsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDirEntryImpl.class.getMethod("getPermissions");
      assertNotNull(method, "getPermissions method should exist");
      assertEquals(WasiPermissions.class, method.getReturnType(), "Should return WasiPermissions");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private constructor with 8 parameters")
    void shouldHavePackagePrivateConstructor() {
      boolean hasExpectedConstructor = false;
      for (var constructor : PanamaWasiDirEntryImpl.class.getDeclaredConstructors()) {
        if (constructor.getParameterCount() == 8
            && constructor.getParameterTypes()[0] == String.class
            && constructor.getParameterTypes()[1] == WasiFileType.class
            && constructor.getParameterTypes()[2] == long.class
            && constructor.getParameterTypes()[3] == long.class
            && constructor.getParameterTypes()[4] == Instant.class
            && constructor.getParameterTypes()[5] == Instant.class
            && constructor.getParameterTypes()[6] == Instant.class
            && constructor.getParameterTypes()[7] == WasiPermissions.class) {
          hasExpectedConstructor = true;
          break;
        }
      }
      assertTrue(
          hasExpectedConstructor,
          "Should have constructor with String, WasiFileType, long, long, Instant, Instant,"
              + " Instant, WasiPermissions");
    }
  }
}
