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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.wasi.clocks.JniWasiTimezone;
import ai.tegmentum.wasmtime4j.jni.wasi.io.JniWasiPollable;
import ai.tegmentum.wasmtime4j.wasi.clocks.DateTime;
import ai.tegmentum.wasmtime4j.wasi.clocks.TimezoneDisplay;
import ai.tegmentum.wasmtime4j.wasi.clocks.WasiTimezone;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for JNI WASI implementation classes.
 *
 * <p>This test class verifies the structure and API conformance of the JNI-based WASI
 * implementation classes including JniWasiTimezone and JniWasiPollable.
 */
@DisplayName("JNI WASI Classes Tests")
class JniWasiClassesTest {

  @Nested
  @DisplayName("JniWasiTimezone Tests")
  class JniWasiTimezoneTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(JniWasiTimezone.class.getModifiers()),
          "JniWasiTimezone should be public");
      assertTrue(
          Modifier.isFinal(JniWasiTimezone.class.getModifiers()),
          "JniWasiTimezone should be final");
    }

    @Test
    @DisplayName("should implement WasiTimezone interface")
    void shouldImplementWasiTimezoneInterface() {
      assertTrue(
          WasiTimezone.class.isAssignableFrom(JniWasiTimezone.class),
          "JniWasiTimezone should implement WasiTimezone");
    }

    @Test
    @DisplayName("should have constructor with contextHandle")
    void shouldHaveConstructorWithContextHandle() throws NoSuchMethodException {
      final Constructor<?> constructor = JniWasiTimezone.class.getConstructor(long.class);
      assertNotNull(constructor, "Constructor with contextHandle should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have display method")
    void shouldHaveDisplayMethod() throws NoSuchMethodException {
      final Method method = JniWasiTimezone.class.getMethod("display", DateTime.class);
      assertNotNull(method, "display method should exist");
      assertEquals(TimezoneDisplay.class, method.getReturnType(), "Should return TimezoneDisplay");
    }

    @Test
    @DisplayName("should have utcOffset method")
    void shouldHaveUtcOffsetMethod() throws NoSuchMethodException {
      final Method method = JniWasiTimezone.class.getMethod("utcOffset", DateTime.class);
      assertNotNull(method, "utcOffset method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("JniWasiPollable Tests")
  class JniWasiPollableTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(JniWasiPollable.class.getModifiers()),
          "JniWasiPollable should be public");
      assertTrue(
          Modifier.isFinal(JniWasiPollable.class.getModifiers()),
          "JniWasiPollable should be final");
    }

    @Test
    @DisplayName("should extend JniResource")
    void shouldExtendJniResource() {
      assertTrue(
          JniResource.class.isAssignableFrom(JniWasiPollable.class),
          "JniWasiPollable should extend JniResource");
    }

    @Test
    @DisplayName("should implement WasiPollable interface")
    void shouldImplementWasiPollableInterface() {
      assertTrue(
          WasiPollable.class.isAssignableFrom(JniWasiPollable.class),
          "JniWasiPollable should implement WasiPollable");
    }

    @Test
    @DisplayName("should have constructor with context and pollable handles")
    void shouldHaveConstructorWithHandles() throws NoSuchMethodException {
      final Constructor<?> constructor =
          JniWasiPollable.class.getConstructor(long.class, long.class);
      assertNotNull(constructor, "Constructor with handles should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have block method")
    void shouldHaveBlockMethod() throws NoSuchMethodException {
      final Method method = JniWasiPollable.class.getMethod("block");
      assertNotNull(method, "block method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have ready method")
    void shouldHaveReadyMethod() throws NoSuchMethodException {
      final Method method = JniWasiPollable.class.getMethod("ready");
      assertNotNull(method, "ready method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      final Method method = JniWasiPollable.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = JniWasiPollable.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = JniWasiPollable.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getAvailableOperations method")
    void shouldHaveGetAvailableOperationsMethod() throws NoSuchMethodException {
      final Method method = JniWasiPollable.class.getMethod("getAvailableOperations");
      assertNotNull(method, "getAvailableOperations method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have invoke method")
    void shouldHaveInvokeMethod() throws NoSuchMethodException {
      final Method method = JniWasiPollable.class.getMethod("invoke", String.class, Object[].class);
      assertNotNull(method, "invoke method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = JniWasiPollable.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
