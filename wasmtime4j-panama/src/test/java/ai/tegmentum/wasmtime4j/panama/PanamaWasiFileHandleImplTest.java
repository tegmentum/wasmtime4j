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

import ai.tegmentum.wasmtime4j.wasi.WasiFileHandle;
import ai.tegmentum.wasmtime4j.wasi.WasiOpenFlags;
import ai.tegmentum.wasmtime4j.wasi.WasiRights;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiFileHandleImpl} class.
 *
 * <p>PanamaWasiFileHandleImpl is the Panama implementation of WasiFileHandle.
 */
@DisplayName("PanamaWasiFileHandleImpl Tests")
class PanamaWasiFileHandleImplTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be package-private final class")
    void shouldBePackagePrivateFinalClass() {
      assertTrue(
          !Modifier.isPublic(PanamaWasiFileHandleImpl.class.getModifiers()),
          "PanamaWasiFileHandleImpl should be package-private");
      assertTrue(
          Modifier.isFinal(PanamaWasiFileHandleImpl.class.getModifiers()),
          "PanamaWasiFileHandleImpl should be final");
    }

    @Test
    @DisplayName("should implement WasiFileHandle interface")
    void shouldImplementWasiFileHandleInterface() {
      assertTrue(
          WasiFileHandle.class.isAssignableFrom(PanamaWasiFileHandleImpl.class),
          "PanamaWasiFileHandleImpl should implement WasiFileHandle");
    }
  }

  @Nested
  @DisplayName("Descriptor Method Tests")
  class DescriptorMethodTests {

    @Test
    @DisplayName("should have getFileDescriptor method")
    void shouldHaveGetFileDescriptorMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiFileHandleImpl.class.getMethod("getFileDescriptor");
      assertNotNull(method, "getFileDescriptor method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getPath method")
    void shouldHaveGetPathMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiFileHandleImpl.class.getMethod("getPath");
      assertNotNull(method, "getPath method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Rights and Flags Method Tests")
  class RightsAndFlagsMethodTests {

    @Test
    @DisplayName("should have getRights method")
    void shouldHaveGetRightsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiFileHandleImpl.class.getMethod("getRights");
      assertNotNull(method, "getRights method should exist");
      assertEquals(WasiRights.class, method.getReturnType(), "Should return WasiRights");
    }

    @Test
    @DisplayName("should have getOpenFlags method")
    void shouldHaveGetOpenFlagsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiFileHandleImpl.class.getMethod("getOpenFlags");
      assertNotNull(method, "getOpenFlags method should exist");
      assertEquals(WasiOpenFlags.class, method.getReturnType(), "Should return WasiOpenFlags");
    }
  }

  @Nested
  @DisplayName("Validity Method Tests")
  class ValidityMethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiFileHandleImpl.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Position Method Tests")
  class PositionMethodTests {

    @Test
    @DisplayName("should have getPosition method")
    void shouldHaveGetPositionMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiFileHandleImpl.class.getMethod("getPosition");
      assertNotNull(method, "getPosition method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have setPosition method")
    void shouldHaveSetPositionMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiFileHandleImpl.class.getMethod("setPosition", long.class);
      assertNotNull(method, "setPosition method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Size Method Tests")
  class SizeMethodTests {

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiFileHandleImpl.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have setSize method")
    void shouldHaveSetSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiFileHandleImpl.class.getMethod("setSize", long.class);
      assertNotNull(method, "setSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Internal Method Tests")
  class InternalMethodTests {

    @Test
    @DisplayName("should have getChannel method")
    void shouldHaveGetChannelMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiFileHandleImpl.class.getDeclaredMethod("getChannel");
      assertNotNull(method, "getChannel method should exist");
      assertEquals(FileChannel.class, method.getReturnType(), "Should return FileChannel");
    }

    @Test
    @DisplayName("should have getResolvedPath method")
    void shouldHaveGetResolvedPathMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiFileHandleImpl.class.getDeclaredMethod("getResolvedPath");
      assertNotNull(method, "getResolvedPath method should exist");
      assertEquals(Path.class, method.getReturnType(), "Should return Path");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiFileHandleImpl.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private constructor with 6 parameters")
    void shouldHavePackagePrivateConstructor() {
      boolean hasExpectedConstructor = false;
      for (var constructor : PanamaWasiFileHandleImpl.class.getDeclaredConstructors()) {
        if (constructor.getParameterCount() == 6
            && constructor.getParameterTypes()[0] == int.class
            && constructor.getParameterTypes()[1] == String.class
            && constructor.getParameterTypes()[2] == Path.class
            && constructor.getParameterTypes()[3] == FileChannel.class
            && constructor.getParameterTypes()[4] == WasiOpenFlags.class
            && constructor.getParameterTypes()[5] == WasiRights.class) {
          hasExpectedConstructor = true;
          break;
        }
      }
      assertTrue(
          hasExpectedConstructor,
          "Should have constructor with int, String, Path, FileChannel, WasiOpenFlags, WasiRights");
    }
  }
}
