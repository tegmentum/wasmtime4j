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
import ai.tegmentum.wasmtime4j.jni.wasi.filesystem.JniWasiDescriptor;
import ai.tegmentum.wasmtime4j.jni.wasi.io.JniWasiPollable;
import ai.tegmentum.wasmtime4j.wasi.clocks.DateTime;
import ai.tegmentum.wasmtime4j.wasi.clocks.TimezoneDisplay;
import ai.tegmentum.wasmtime4j.wasi.clocks.WasiTimezone;
import ai.tegmentum.wasmtime4j.wasi.filesystem.DescriptorType;
import ai.tegmentum.wasmtime4j.wasi.filesystem.WasiDescriptor;
import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for JNI WASI implementation classes.
 *
 * <p>This test class verifies the structure and API conformance of the JNI-based WASI
 * implementation classes including JniWasiTimezone, JniWasiPollable, and JniWasiDescriptor.
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

  @Nested
  @DisplayName("JniWasiDescriptor Tests")
  class JniWasiDescriptorTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(JniWasiDescriptor.class.getModifiers()),
          "JniWasiDescriptor should be public");
      assertTrue(
          Modifier.isFinal(JniWasiDescriptor.class.getModifiers()),
          "JniWasiDescriptor should be final");
    }

    @Test
    @DisplayName("should extend JniResource")
    void shouldExtendJniResource() {
      assertTrue(
          JniResource.class.isAssignableFrom(JniWasiDescriptor.class),
          "JniWasiDescriptor should extend JniResource");
    }

    @Test
    @DisplayName("should implement WasiDescriptor interface")
    void shouldImplementWasiDescriptorInterface() {
      assertTrue(
          WasiDescriptor.class.isAssignableFrom(JniWasiDescriptor.class),
          "JniWasiDescriptor should implement WasiDescriptor");
    }

    @Test
    @DisplayName("should have constructor with context and descriptor handles")
    void shouldHaveConstructorWithHandles() throws NoSuchMethodException {
      final Constructor<?> constructor =
          JniWasiDescriptor.class.getConstructor(long.class, long.class);
      assertNotNull(constructor, "Constructor with handles should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have readViaStream method")
    void shouldHaveReadViaStreamMethod() throws NoSuchMethodException {
      final Method method = JniWasiDescriptor.class.getMethod("readViaStream", long.class);
      assertNotNull(method, "readViaStream method should exist");
      assertEquals(WasiInputStream.class, method.getReturnType(), "Should return WasiInputStream");
    }

    @Test
    @DisplayName("should have writeViaStream method")
    void shouldHaveWriteViaStreamMethod() throws NoSuchMethodException {
      final Method method = JniWasiDescriptor.class.getMethod("writeViaStream", long.class);
      assertNotNull(method, "writeViaStream method should exist");
      assertEquals(
          WasiOutputStream.class, method.getReturnType(), "Should return WasiOutputStream");
    }

    @Test
    @DisplayName("should have appendViaStream method")
    void shouldHaveAppendViaStreamMethod() throws NoSuchMethodException {
      final Method method = JniWasiDescriptor.class.getMethod("appendViaStream");
      assertNotNull(method, "appendViaStream method should exist");
      assertEquals(
          WasiOutputStream.class, method.getReturnType(), "Should return WasiOutputStream");
    }

    @Test
    @DisplayName("should have getDescriptorType method")
    void shouldHaveGetDescriptorTypeMethod() throws NoSuchMethodException {
      final Method method = JniWasiDescriptor.class.getMethod("getDescriptorType");
      assertNotNull(method, "getDescriptorType method should exist");
      assertEquals(DescriptorType.class, method.getReturnType(), "Should return DescriptorType");
    }

    @Test
    @DisplayName("should have getFlags method")
    void shouldHaveGetFlagsMethod() throws NoSuchMethodException {
      final Method method = JniWasiDescriptor.class.getMethod("getFlags");
      assertNotNull(method, "getFlags method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have setSize method")
    void shouldHaveSetSizeMethod() throws NoSuchMethodException {
      final Method method = JniWasiDescriptor.class.getMethod("setSize", long.class);
      assertNotNull(method, "setSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have syncData method")
    void shouldHaveSyncDataMethod() throws NoSuchMethodException {
      final Method method = JniWasiDescriptor.class.getMethod("syncData");
      assertNotNull(method, "syncData method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have sync method")
    void shouldHaveSyncMethod() throws NoSuchMethodException {
      final Method method = JniWasiDescriptor.class.getMethod("sync");
      assertNotNull(method, "sync method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have openAt method")
    void shouldHaveOpenAtMethod() throws NoSuchMethodException {
      final Method method =
          JniWasiDescriptor.class.getMethod(
              "openAt", String.class, Set.class, Set.class, Set.class);
      assertNotNull(method, "openAt method should exist");
      assertEquals(WasiDescriptor.class, method.getReturnType(), "Should return WasiDescriptor");
    }

    @Test
    @DisplayName("should have createDirectoryAt method")
    void shouldHaveCreateDirectoryAtMethod() throws NoSuchMethodException {
      final Method method = JniWasiDescriptor.class.getMethod("createDirectoryAt", String.class);
      assertNotNull(method, "createDirectoryAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have readDirectory method")
    void shouldHaveReadDirectoryMethod() throws NoSuchMethodException {
      final Method method = JniWasiDescriptor.class.getMethod("readDirectory");
      assertNotNull(method, "readDirectory method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have readLinkAt method")
    void shouldHaveReadLinkAtMethod() throws NoSuchMethodException {
      final Method method = JniWasiDescriptor.class.getMethod("readLinkAt", String.class);
      assertNotNull(method, "readLinkAt method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have unlinkFileAt method")
    void shouldHaveUnlinkFileAtMethod() throws NoSuchMethodException {
      final Method method = JniWasiDescriptor.class.getMethod("unlinkFileAt", String.class);
      assertNotNull(method, "unlinkFileAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have removeDirectoryAt method")
    void shouldHaveRemoveDirectoryAtMethod() throws NoSuchMethodException {
      final Method method = JniWasiDescriptor.class.getMethod("removeDirectoryAt", String.class);
      assertNotNull(method, "removeDirectoryAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have renameAt method")
    void shouldHaveRenameAtMethod() throws NoSuchMethodException {
      final Method method =
          JniWasiDescriptor.class.getMethod(
              "renameAt", String.class, WasiDescriptor.class, String.class);
      assertNotNull(method, "renameAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have symlinkAt method")
    void shouldHaveSymlinkAtMethod() throws NoSuchMethodException {
      final Method method =
          JniWasiDescriptor.class.getMethod("symlinkAt", String.class, String.class);
      assertNotNull(method, "symlinkAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have linkAt method")
    void shouldHaveLinkAtMethod() throws NoSuchMethodException {
      final Method method =
          JniWasiDescriptor.class.getMethod(
              "linkAt", Set.class, String.class, WasiDescriptor.class, String.class);
      assertNotNull(method, "linkAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have isSameObject method")
    void shouldHaveIsSameObjectMethod() throws NoSuchMethodException {
      final Method method = JniWasiDescriptor.class.getMethod("isSameObject", WasiDescriptor.class);
      assertNotNull(method, "isSameObject method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = JniWasiDescriptor.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
