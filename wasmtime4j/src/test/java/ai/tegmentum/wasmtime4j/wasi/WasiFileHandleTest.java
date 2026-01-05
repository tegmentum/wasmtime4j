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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiFileHandle} interface.
 *
 * <p>WasiFileHandle represents an open file descriptor within the WASI sandbox.
 */
@DisplayName("WasiFileHandle Tests")
class WasiFileHandleTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(WasiFileHandle.class.getModifiers()),
          "WasiFileHandle should be public");
      assertTrue(WasiFileHandle.class.isInterface(), "WasiFileHandle should be an interface");
    }

    @Test
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(WasiFileHandle.class),
          "WasiFileHandle should extend Closeable");
    }
  }

  @Nested
  @DisplayName("File Descriptor Method Tests")
  class FileDescriptorMethodTests {

    @Test
    @DisplayName("should have getFileDescriptor method")
    void shouldHaveGetFileDescriptorMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandle.class.getMethod("getFileDescriptor");
      assertNotNull(method, "getFileDescriptor method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getPath method")
    void shouldHaveGetPathMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandle.class.getMethod("getPath");
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
      final Method method = WasiFileHandle.class.getMethod("getRights");
      assertNotNull(method, "getRights method should exist");
      assertEquals(WasiRights.class, method.getReturnType(), "Should return WasiRights");
    }

    @Test
    @DisplayName("should have getOpenFlags method")
    void shouldHaveGetOpenFlagsMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandle.class.getMethod("getOpenFlags");
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
      final Method method = WasiFileHandle.class.getMethod("isValid");
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
      final Method method = WasiFileHandle.class.getMethod("getPosition");
      assertNotNull(method, "getPosition method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have setPosition method")
    void shouldHaveSetPositionMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandle.class.getMethod("setPosition", long.class);
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
      final Method method = WasiFileHandle.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have setSize method")
    void shouldHaveSetSizeMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandle.class.getMethod("setSize", long.class);
      assertNotNull(method, "setSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Close Method Tests")
  class CloseMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandle.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
