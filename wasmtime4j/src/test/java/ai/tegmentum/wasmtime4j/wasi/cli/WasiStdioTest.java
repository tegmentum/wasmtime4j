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

package ai.tegmentum.wasmtime4j.wasi.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiStdio} interface.
 *
 * <p>WasiStdio provides access to standard input, output, and error streams for WASI CLI programs.
 */
@DisplayName("WasiStdio Tests")
class WasiStdioTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiStdio.class.isInterface(), "WasiStdio should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WasiStdio.class.getModifiers()), "WasiStdio should be public");
    }

    @Test
    @DisplayName("should have getStdin method")
    void shouldHaveGetStdinMethod() throws NoSuchMethodException {
      final Method method = WasiStdio.class.getMethod("getStdin");
      assertNotNull(method, "getStdin method should exist");
    }

    @Test
    @DisplayName("should have getStdout method")
    void shouldHaveGetStdoutMethod() throws NoSuchMethodException {
      final Method method = WasiStdio.class.getMethod("getStdout");
      assertNotNull(method, "getStdout method should exist");
    }

    @Test
    @DisplayName("should have getStderr method")
    void shouldHaveGetStderrMethod() throws NoSuchMethodException {
      final Method method = WasiStdio.class.getMethod("getStderr");
      assertNotNull(method, "getStderr method should exist");
    }
  }

  @Nested
  @DisplayName("Method Return Type Tests")
  class MethodReturnTypeTests {

    @Test
    @DisplayName("getStdin should return InputStream type")
    void getStdinShouldReturnInputStreamType() throws NoSuchMethodException {
      final Method method = WasiStdio.class.getMethod("getStdin");
      final String returnTypeName = method.getReturnType().getSimpleName();
      assertTrue(
          returnTypeName.contains("InputStream") || returnTypeName.contains("WasiInputStream"),
          "getStdin should return an input stream type");
    }

    @Test
    @DisplayName("getStdout should return OutputStream type")
    void getStdoutShouldReturnOutputStreamType() throws NoSuchMethodException {
      final Method method = WasiStdio.class.getMethod("getStdout");
      final String returnTypeName = method.getReturnType().getSimpleName();
      assertTrue(
          returnTypeName.contains("OutputStream") || returnTypeName.contains("WasiOutputStream"),
          "getStdout should return an output stream type");
    }

    @Test
    @DisplayName("getStderr should return OutputStream type")
    void getStderrShouldReturnOutputStreamType() throws NoSuchMethodException {
      final Method method = WasiStdio.class.getMethod("getStderr");
      final String returnTypeName = method.getReturnType().getSimpleName();
      assertTrue(
          returnTypeName.contains("OutputStream") || returnTypeName.contains("WasiOutputStream"),
          "getStderr should return an output stream type");
    }

    @Test
    @DisplayName("all methods should take no parameters")
    void allMethodsShouldTakeNoParameters() {
      for (final Method method : WasiStdio.class.getDeclaredMethods()) {
        if (!method.isSynthetic()) {
          assertEquals(
              0,
              method.getParameterCount(),
              "Method " + method.getName() + " should take no parameters");
        }
      }
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      for (final Method method : WasiStdio.class.getDeclaredMethods()) {
        if (!method.isSynthetic()) {
          assertTrue(
              Modifier.isPublic(method.getModifiers()),
              "Method " + method.getName() + " should be public");
        }
      }
    }

    @Test
    @DisplayName("interface should have expected method count")
    void interfaceShouldHaveExpectedMethodCount() {
      int methodCount = 0;
      for (final Method method : WasiStdio.class.getDeclaredMethods()) {
        if (!method.isSynthetic() && !Modifier.isStatic(method.getModifiers())) {
          methodCount++;
        }
      }
      assertTrue(methodCount >= 3, "WasiStdio should have at least 3 methods");
    }
  }
}
