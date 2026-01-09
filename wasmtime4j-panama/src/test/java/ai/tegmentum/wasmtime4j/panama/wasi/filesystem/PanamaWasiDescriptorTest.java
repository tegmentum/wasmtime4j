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

package ai.tegmentum.wasmtime4j.panama.wasi.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.util.PanamaResource;
import ai.tegmentum.wasmtime4j.wasi.filesystem.DescriptorType;
import ai.tegmentum.wasmtime4j.wasi.filesystem.WasiDescriptor;
import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiDescriptor} class.
 *
 * <p>PanamaWasiDescriptor is a Panama FFI implementation of the WasiDescriptor interface providing
 * access to WASI Preview 2 filesystem operations. Descriptors represent filesystem objects like
 * files and directories.
 */
@DisplayName("PanamaWasiDescriptor Tests")
class PanamaWasiDescriptorTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiDescriptor.class.getModifiers()),
          "PanamaWasiDescriptor should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiDescriptor.class.getModifiers()),
          "PanamaWasiDescriptor should be final");
    }

    @Test
    @DisplayName("should extend PanamaResource")
    void shouldExtendPanamaResource() {
      assertTrue(
          PanamaResource.class.isAssignableFrom(PanamaWasiDescriptor.class),
          "PanamaWasiDescriptor should extend PanamaResource");
    }

    @Test
    @DisplayName("should implement WasiDescriptor interface")
    void shouldImplementWasiDescriptorInterface() {
      assertTrue(
          WasiDescriptor.class.isAssignableFrom(PanamaWasiDescriptor.class),
          "PanamaWasiDescriptor should implement WasiDescriptor");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with context and descriptor handles")
    void shouldHaveConstructorWithHandles() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PanamaWasiDescriptor.class.getConstructor(MemorySegment.class, MemorySegment.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Stream Method Tests")
  class StreamMethodTests {

    @Test
    @DisplayName("should have readViaStream method")
    void shouldHaveReadViaStreamMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDescriptor.class.getMethod("readViaStream", long.class);
      assertNotNull(method, "readViaStream method should exist");
      assertEquals(WasiInputStream.class, method.getReturnType(), "Should return WasiInputStream");
    }

    @Test
    @DisplayName("should have writeViaStream method")
    void shouldHaveWriteViaStreamMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDescriptor.class.getMethod("writeViaStream", long.class);
      assertNotNull(method, "writeViaStream method should exist");
      assertEquals(
          WasiOutputStream.class, method.getReturnType(), "Should return WasiOutputStream");
    }

    @Test
    @DisplayName("should have appendViaStream method")
    void shouldHaveAppendViaStreamMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDescriptor.class.getMethod("appendViaStream");
      assertNotNull(method, "appendViaStream method should exist");
      assertEquals(
          WasiOutputStream.class, method.getReturnType(), "Should return WasiOutputStream");
    }
  }

  @Nested
  @DisplayName("Metadata Method Tests")
  class MetadataMethodTests {

    @Test
    @DisplayName("should have getDescriptorType method")
    void shouldHaveGetDescriptorTypeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDescriptor.class.getMethod("getDescriptorType");
      assertNotNull(method, "getDescriptorType method should exist");
      assertEquals(DescriptorType.class, method.getReturnType(), "Should return DescriptorType");
    }

    @Test
    @DisplayName("should have getFlags method")
    void shouldHaveGetFlagsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDescriptor.class.getMethod("getFlags");
      assertNotNull(method, "getFlags method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have setSize method")
    void shouldHaveSetSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDescriptor.class.getMethod("setSize", long.class);
      assertNotNull(method, "setSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Sync Method Tests")
  class SyncMethodTests {

    @Test
    @DisplayName("should have syncData method")
    void shouldHaveSyncDataMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDescriptor.class.getMethod("syncData");
      assertNotNull(method, "syncData method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have sync method")
    void shouldHaveSyncMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDescriptor.class.getMethod("sync");
      assertNotNull(method, "sync method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Directory Method Tests")
  class DirectoryMethodTests {

    @Test
    @DisplayName("should have openAt method")
    void shouldHaveOpenAtMethod() throws NoSuchMethodException {
      // Signature: openAt(String path, Set<PathFlags>, Set<OpenFlags>, Set<DescriptorFlags>)
      final Method method =
          PanamaWasiDescriptor.class.getMethod(
              "openAt", String.class, Set.class, Set.class, Set.class);
      assertNotNull(method, "openAt method should exist");
      assertEquals(WasiDescriptor.class, method.getReturnType(), "Should return WasiDescriptor");
    }

    @Test
    @DisplayName("should have createDirectoryAt method")
    void shouldHaveCreateDirectoryAtMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDescriptor.class.getMethod("createDirectoryAt", String.class);
      assertNotNull(method, "createDirectoryAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have readDirectory method")
    void shouldHaveReadDirectoryMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDescriptor.class.getMethod("readDirectory");
      assertNotNull(method, "readDirectory method should exist");
      // Returns DirectoryEntryStream in WASI spec
    }
  }

  @Nested
  @DisplayName("File Operation Method Tests")
  class FileOperationMethodTests {

    @Test
    @DisplayName("should have unlinkFileAt method")
    void shouldHaveUnlinkFileAtMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDescriptor.class.getMethod("unlinkFileAt", String.class);
      assertNotNull(method, "unlinkFileAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have removeDirectoryAt method")
    void shouldHaveRemoveDirectoryAtMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDescriptor.class.getMethod("removeDirectoryAt", String.class);
      assertNotNull(method, "removeDirectoryAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Path Operation Method Tests")
  class PathOperationMethodTests {

    @Test
    @DisplayName("should have renameAt method")
    void shouldHaveRenameAtMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiDescriptor.class.getMethod(
              "renameAt", String.class, WasiDescriptor.class, String.class);
      assertNotNull(method, "renameAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have symlinkAt method")
    void shouldHaveSymlinkAtMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiDescriptor.class.getMethod("symlinkAt", String.class, String.class);
      assertNotNull(method, "symlinkAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have linkAt method")
    void shouldHaveLinkAtMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiDescriptor.class.getMethod(
              "linkAt", Set.class, String.class, WasiDescriptor.class, String.class);
      assertNotNull(method, "linkAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have readLinkAt method")
    void shouldHaveReadLinkAtMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDescriptor.class.getMethod("readLinkAt", String.class);
      assertNotNull(method, "readLinkAt method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Utility Method Tests")
  class UtilityMethodTests {

    @Test
    @DisplayName("should have isSameObject method")
    void shouldHaveIsSameObjectMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiDescriptor.class.getMethod("isSameObject", WasiDescriptor.class);
      assertNotNull(method, "isSameObject method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiDescriptor.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
