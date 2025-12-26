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

package ai.tegmentum.wasmtime4j.wasi.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.WasiResource;
import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiDescriptor} interface.
 *
 * <p>WasiDescriptor represents a WASI Preview 2 filesystem descriptor that provides access to
 * filesystem objects such as files, directories, and special files.
 */
@DisplayName("WasiDescriptor Tests")
class WasiDescriptorTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiDescriptor.class.isInterface(), "WasiDescriptor should be an interface");
    }

    @Test
    @DisplayName("should extend WasiResource")
    void shouldExtendWasiResource() {
      assertTrue(
          WasiResource.class.isAssignableFrom(WasiDescriptor.class),
          "WasiDescriptor should extend WasiResource");
    }
  }

  @Nested
  @DisplayName("Stream Method Tests")
  class StreamMethodTests {

    @Test
    @DisplayName("should have readViaStream method")
    void shouldHaveReadViaStreamMethod() throws NoSuchMethodException {
      final Method method = WasiDescriptor.class.getMethod("readViaStream", long.class);
      assertEquals(WasiInputStream.class, method.getReturnType(), "Should return WasiInputStream");
    }

    @Test
    @DisplayName("should have writeViaStream method")
    void shouldHaveWriteViaStreamMethod() throws NoSuchMethodException {
      final Method method = WasiDescriptor.class.getMethod("writeViaStream", long.class);
      assertEquals(
          WasiOutputStream.class, method.getReturnType(), "Should return WasiOutputStream");
    }

    @Test
    @DisplayName("should have appendViaStream method")
    void shouldHaveAppendViaStreamMethod() throws NoSuchMethodException {
      final Method method = WasiDescriptor.class.getMethod("appendViaStream");
      assertEquals(
          WasiOutputStream.class, method.getReturnType(), "Should return WasiOutputStream");
    }
  }

  @Nested
  @DisplayName("Descriptor Info Method Tests")
  class DescriptorInfoMethodTests {

    @Test
    @DisplayName("should have getDescriptorType method")
    void shouldHaveGetDescriptorTypeMethod() throws NoSuchMethodException {
      final Method method = WasiDescriptor.class.getMethod("getDescriptorType");
      assertEquals(DescriptorType.class, method.getReturnType(), "Should return DescriptorType");
    }

    @Test
    @DisplayName("should have getFlags method")
    void shouldHaveGetFlagsMethod() throws NoSuchMethodException {
      final Method method = WasiDescriptor.class.getMethod("getFlags");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }
  }

  @Nested
  @DisplayName("File Operation Method Tests")
  class FileOperationMethodTests {

    @Test
    @DisplayName("should have setSize method")
    void shouldHaveSetSizeMethod() throws NoSuchMethodException {
      final Method method = WasiDescriptor.class.getMethod("setSize", long.class);
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have syncData method")
    void shouldHaveSyncDataMethod() throws NoSuchMethodException {
      final Method method = WasiDescriptor.class.getMethod("syncData");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have sync method")
    void shouldHaveSyncMethod() throws NoSuchMethodException {
      final Method method = WasiDescriptor.class.getMethod("sync");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Directory Operation Method Tests")
  class DirectoryOperationMethodTests {

    @Test
    @DisplayName("should have openAt method")
    void shouldHaveOpenAtMethod() throws NoSuchMethodException {
      final Method method =
          WasiDescriptor.class.getMethod("openAt", String.class, Set.class, Set.class, Set.class);
      assertEquals(WasiDescriptor.class, method.getReturnType(), "Should return WasiDescriptor");
    }

    @Test
    @DisplayName("should have createDirectoryAt method")
    void shouldHaveCreateDirectoryAtMethod() throws NoSuchMethodException {
      final Method method = WasiDescriptor.class.getMethod("createDirectoryAt", String.class);
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have readDirectory method")
    void shouldHaveReadDirectoryMethod() throws NoSuchMethodException {
      final Method method = WasiDescriptor.class.getMethod("readDirectory");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have removeDirectoryAt method")
    void shouldHaveRemoveDirectoryAtMethod() throws NoSuchMethodException {
      final Method method = WasiDescriptor.class.getMethod("removeDirectoryAt", String.class);
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Link Operation Method Tests")
  class LinkOperationMethodTests {

    @Test
    @DisplayName("should have readLinkAt method")
    void shouldHaveReadLinkAtMethod() throws NoSuchMethodException {
      final Method method = WasiDescriptor.class.getMethod("readLinkAt", String.class);
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have symlinkAt method")
    void shouldHaveSymlinkAtMethod() throws NoSuchMethodException {
      final Method method = WasiDescriptor.class.getMethod("symlinkAt", String.class, String.class);
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have linkAt method")
    void shouldHaveLinkAtMethod() throws NoSuchMethodException {
      final Method method =
          WasiDescriptor.class.getMethod(
              "linkAt", Set.class, String.class, WasiDescriptor.class, String.class);
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("File Management Method Tests")
  class FileManagementMethodTests {

    @Test
    @DisplayName("should have unlinkFileAt method")
    void shouldHaveUnlinkFileAtMethod() throws NoSuchMethodException {
      final Method method = WasiDescriptor.class.getMethod("unlinkFileAt", String.class);
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have renameAt method")
    void shouldHaveRenameAtMethod() throws NoSuchMethodException {
      final Method method =
          WasiDescriptor.class.getMethod(
              "renameAt", String.class, WasiDescriptor.class, String.class);
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Comparison Method Tests")
  class ComparisonMethodTests {

    @Test
    @DisplayName("should have isSameObject method")
    void shouldHaveIsSameObjectMethod() throws NoSuchMethodException {
      final Method method = WasiDescriptor.class.getMethod("isSameObject", WasiDescriptor.class);
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("WASI Specification Compliance Tests")
  class WasiSpecificationComplianceTests {

    @Test
    @DisplayName("should have all WASI Preview 2 descriptor methods")
    void shouldHaveAllWasiPreview2DescriptorMethods() {
      // Core WASI Preview 2 descriptor operations
      final String[] expectedMethods = {
        "readViaStream",
        "writeViaStream",
        "appendViaStream",
        "getDescriptorType",
        "getFlags",
        "setSize",
        "syncData",
        "sync",
        "openAt",
        "createDirectoryAt",
        "readDirectory",
        "readLinkAt",
        "unlinkFileAt",
        "removeDirectoryAt",
        "renameAt",
        "symlinkAt",
        "linkAt",
        "isSameObject"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(WasiDescriptor.class, methodName), "Should have WASI method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("stream methods should take offset parameter")
    void streamMethodsShouldTakeOffsetParameter() throws NoSuchMethodException {
      final Method readStream = WasiDescriptor.class.getMethod("readViaStream", long.class);
      final Method writeStream = WasiDescriptor.class.getMethod("writeViaStream", long.class);

      assertEquals(1, readStream.getParameterCount(), "readViaStream should have 1 parameter");
      assertEquals(1, writeStream.getParameterCount(), "writeViaStream should have 1 parameter");
    }

    @Test
    @DisplayName("openAt should take all required parameters")
    void openAtShouldTakeAllRequiredParameters() throws NoSuchMethodException {
      final Method openAt =
          WasiDescriptor.class.getMethod("openAt", String.class, Set.class, Set.class, Set.class);

      assertEquals(4, openAt.getParameterCount(), "openAt should have 4 parameters");
      final Class<?>[] paramTypes = openAt.getParameterTypes();
      assertEquals(String.class, paramTypes[0], "First param should be String (path)");
      assertEquals(Set.class, paramTypes[1], "Second param should be Set (pathFlags)");
      assertEquals(Set.class, paramTypes[2], "Third param should be Set (openFlags)");
      assertEquals(Set.class, paramTypes[3], "Fourth param should be Set (descriptorFlags)");
    }
  }

  @Nested
  @DisplayName("Resource Pattern Tests")
  class ResourcePatternTests {

    @Test
    @DisplayName("should extend WasiResource for lifecycle management")
    void shouldExtendWasiResourceForLifecycleManagement() {
      final Class<?>[] interfaces = WasiDescriptor.class.getInterfaces();
      boolean extendsWasiResource = false;
      for (final Class<?> iface : interfaces) {
        if (iface == WasiResource.class) {
          extendsWasiResource = true;
          break;
        }
      }
      assertTrue(extendsWasiResource, "Should extend WasiResource");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Documentation Tests")
  class UsagePatternDocumentationTests {

    @Test
    @DisplayName("should support file reading pattern")
    void shouldSupportFileReadingPattern() {
      // Documents the expected usage: descriptor.readViaStream(offset) -> read data
      assertTrue(hasMethod(WasiDescriptor.class, "readViaStream"), "Need readViaStream");
      assertTrue(hasMethod(WasiDescriptor.class, "getDescriptorType"), "Need getDescriptorType");
    }

    @Test
    @DisplayName("should support file writing pattern")
    void shouldSupportFileWritingPattern() {
      // Documents the expected usage: descriptor.writeViaStream(offset) -> write data
      assertTrue(hasMethod(WasiDescriptor.class, "writeViaStream"), "Need writeViaStream");
      assertTrue(hasMethod(WasiDescriptor.class, "appendViaStream"), "Need appendViaStream");
    }

    @Test
    @DisplayName("should support directory traversal pattern")
    void shouldSupportDirectoryTraversalPattern() {
      // Documents: descriptor.readDirectory() -> list entries -> openAt for each
      assertTrue(hasMethod(WasiDescriptor.class, "readDirectory"), "Need readDirectory");
      assertTrue(hasMethod(WasiDescriptor.class, "openAt"), "Need openAt");
    }

    @Test
    @DisplayName("should support file sync pattern")
    void shouldSupportFileSyncPattern() {
      // Documents: write data -> syncData() or sync()
      assertTrue(hasMethod(WasiDescriptor.class, "syncData"), "Need syncData");
      assertTrue(hasMethod(WasiDescriptor.class, "sync"), "Need sync");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }
}
