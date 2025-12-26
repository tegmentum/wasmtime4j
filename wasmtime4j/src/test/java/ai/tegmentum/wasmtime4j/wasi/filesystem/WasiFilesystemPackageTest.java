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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiResource;
import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the WASI Filesystem package interfaces and enums.
 *
 * <p>This test class verifies the interface structure and method signatures for the WASI filesystem
 * API using reflection-based testing.
 */
@DisplayName("WASI Filesystem Package Tests")
class WasiFilesystemPackageTest {

  // ========================================================================
  // DescriptorType Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("DescriptorType Enum Tests")
  class DescriptorTypeTests {

    @Test
    @DisplayName("DescriptorType should be an enum")
    void shouldBeAnEnum() {
      assertTrue(DescriptorType.class.isEnum(), "DescriptorType should be an enum");
    }

    @Test
    @DisplayName("DescriptorType should be a public enum")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(DescriptorType.class.getModifiers()),
          "DescriptorType should be public");
    }

    @Test
    @DisplayName("DescriptorType should have exactly 8 values")
    void shouldHaveExactValueCount() {
      DescriptorType[] values = DescriptorType.values();
      assertEquals(8, values.length, "DescriptorType should have exactly 8 values");
    }

    @Test
    @DisplayName("DescriptorType should have UNKNOWN value")
    void shouldHaveUnknownValue() {
      assertNotNull(DescriptorType.valueOf("UNKNOWN"), "UNKNOWN value should exist");
    }

    @Test
    @DisplayName("DescriptorType should have BLOCK_DEVICE value")
    void shouldHaveBlockDeviceValue() {
      assertNotNull(DescriptorType.valueOf("BLOCK_DEVICE"), "BLOCK_DEVICE value should exist");
    }

    @Test
    @DisplayName("DescriptorType should have CHARACTER_DEVICE value")
    void shouldHaveCharacterDeviceValue() {
      assertNotNull(
          DescriptorType.valueOf("CHARACTER_DEVICE"), "CHARACTER_DEVICE value should exist");
    }

    @Test
    @DisplayName("DescriptorType should have DIRECTORY value")
    void shouldHaveDirectoryValue() {
      assertNotNull(DescriptorType.valueOf("DIRECTORY"), "DIRECTORY value should exist");
    }

    @Test
    @DisplayName("DescriptorType should have FIFO value")
    void shouldHaveFifoValue() {
      assertNotNull(DescriptorType.valueOf("FIFO"), "FIFO value should exist");
    }

    @Test
    @DisplayName("DescriptorType should have SYMBOLIC_LINK value")
    void shouldHaveSymbolicLinkValue() {
      assertNotNull(DescriptorType.valueOf("SYMBOLIC_LINK"), "SYMBOLIC_LINK value should exist");
    }

    @Test
    @DisplayName("DescriptorType should have REGULAR_FILE value")
    void shouldHaveRegularFileValue() {
      assertNotNull(DescriptorType.valueOf("REGULAR_FILE"), "REGULAR_FILE value should exist");
    }

    @Test
    @DisplayName("DescriptorType should have SOCKET value")
    void shouldHaveSocketValue() {
      assertNotNull(DescriptorType.valueOf("SOCKET"), "SOCKET value should exist");
    }
  }

  // ========================================================================
  // DescriptorFlags Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("DescriptorFlags Enum Tests")
  class DescriptorFlagsTests {

    @Test
    @DisplayName("DescriptorFlags should be an enum")
    void shouldBeAnEnum() {
      assertTrue(DescriptorFlags.class.isEnum(), "DescriptorFlags should be an enum");
    }

    @Test
    @DisplayName("DescriptorFlags should be a public enum")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(DescriptorFlags.class.getModifiers()),
          "DescriptorFlags should be public");
    }

    @Test
    @DisplayName("DescriptorFlags should have at least READ and WRITE flags")
    void shouldHaveBasicFlags() {
      DescriptorFlags[] values = DescriptorFlags.values();
      assertTrue(values.length >= 2, "DescriptorFlags should have at least 2 values");

      boolean hasRead = false;
      boolean hasWrite = false;
      for (DescriptorFlags flag : values) {
        if (flag.name().contains("READ")) {
          hasRead = true;
        }
        if (flag.name().contains("WRITE")) {
          hasWrite = true;
        }
      }
      assertTrue(hasRead, "Should have a READ flag");
      assertTrue(hasWrite, "Should have a WRITE flag");
    }
  }

  // ========================================================================
  // OpenFlags Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("OpenFlags Enum Tests")
  class OpenFlagsTests {

    @Test
    @DisplayName("OpenFlags should be an enum")
    void shouldBeAnEnum() {
      assertTrue(OpenFlags.class.isEnum(), "OpenFlags should be an enum");
    }

    @Test
    @DisplayName("OpenFlags should be a public enum")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(OpenFlags.class.getModifiers()), "OpenFlags should be public");
    }

    @Test
    @DisplayName("OpenFlags should have CREATE flag")
    void shouldHaveCreateFlag() {
      OpenFlags[] values = OpenFlags.values();
      boolean hasCreate = false;
      for (OpenFlags flag : values) {
        if (flag.name().equals("CREATE")) {
          hasCreate = true;
          break;
        }
      }
      assertTrue(hasCreate, "Should have CREATE flag");
    }
  }

  // ========================================================================
  // PathFlags Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("PathFlags Enum Tests")
  class PathFlagsTests {

    @Test
    @DisplayName("PathFlags should be an enum")
    void shouldBeAnEnum() {
      assertTrue(PathFlags.class.isEnum(), "PathFlags should be an enum");
    }

    @Test
    @DisplayName("PathFlags should be a public enum")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(PathFlags.class.getModifiers()), "PathFlags should be public");
    }

    @Test
    @DisplayName("PathFlags should have at least one flag for symlink handling")
    void shouldHaveSymlinkFlag() {
      PathFlags[] values = PathFlags.values();
      assertTrue(values.length >= 1, "PathFlags should have at least 1 value");
    }
  }

  // ========================================================================
  // WasiDescriptor Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiDescriptor Interface Tests")
  class WasiDescriptorTests {

    @Test
    @DisplayName("WasiDescriptor should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiDescriptor.class.isInterface(), "WasiDescriptor should be an interface");
    }

    @Test
    @DisplayName("WasiDescriptor should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiDescriptor.class.getModifiers()),
          "WasiDescriptor should be public");
    }

    @Test
    @DisplayName("WasiDescriptor should extend WasiResource")
    void shouldExtendWasiResource() {
      Class<?>[] interfaces = WasiDescriptor.class.getInterfaces();
      assertEquals(1, interfaces.length, "WasiDescriptor should extend 1 interface");
      assertEquals(WasiResource.class, interfaces[0], "WasiDescriptor should extend WasiResource");
    }

    @Test
    @DisplayName("should have readViaStream method returning WasiInputStream")
    void shouldHaveReadViaStreamMethod() throws NoSuchMethodException {
      Method method = WasiDescriptor.class.getMethod("readViaStream", long.class);
      assertNotNull(method, "readViaStream method should exist");
      assertEquals(
          WasiInputStream.class, method.getReturnType(), "Return type should be WasiInputStream");
      assertEquals(1, method.getParameterCount(), "readViaStream should have 1 parameter");
      assertEquals(long.class, method.getParameterTypes()[0], "Parameter should be long");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "readViaStream should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have writeViaStream method returning WasiOutputStream")
    void shouldHaveWriteViaStreamMethod() throws NoSuchMethodException {
      Method method = WasiDescriptor.class.getMethod("writeViaStream", long.class);
      assertNotNull(method, "writeViaStream method should exist");
      assertEquals(
          WasiOutputStream.class, method.getReturnType(), "Return type should be WasiOutputStream");
      assertEquals(1, method.getParameterCount(), "writeViaStream should have 1 parameter");
      assertEquals(long.class, method.getParameterTypes()[0], "Parameter should be long");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "writeViaStream should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have appendViaStream method returning WasiOutputStream")
    void shouldHaveAppendViaStreamMethod() throws NoSuchMethodException {
      Method method = WasiDescriptor.class.getMethod("appendViaStream");
      assertNotNull(method, "appendViaStream method should exist");
      assertEquals(
          WasiOutputStream.class, method.getReturnType(), "Return type should be WasiOutputStream");
      assertEquals(0, method.getParameterCount(), "appendViaStream should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "appendViaStream should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have getDescriptorType method returning DescriptorType")
    void shouldHaveGetDescriptorTypeMethod() throws NoSuchMethodException {
      Method method = WasiDescriptor.class.getMethod("getDescriptorType");
      assertNotNull(method, "getDescriptorType method should exist");
      assertEquals(
          DescriptorType.class, method.getReturnType(), "Return type should be DescriptorType");
      assertEquals(0, method.getParameterCount(), "getDescriptorType should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "getDescriptorType should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have getFlags method returning Set<DescriptorFlags>")
    void shouldHaveGetFlagsMethod() throws NoSuchMethodException {
      Method method = WasiDescriptor.class.getMethod("getFlags");
      assertNotNull(method, "getFlags method should exist");
      assertEquals(Set.class, method.getReturnType(), "Return type should be Set");
      assertEquals(0, method.getParameterCount(), "getFlags should have no parameters");

      // Verify generic type
      Type genericReturnType = method.getGenericReturnType();
      assertTrue(
          genericReturnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
      assertEquals(
          DescriptorFlags.class,
          parameterizedType.getActualTypeArguments()[0],
          "Set should contain DescriptorFlags");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "getFlags should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have setSize method with long parameter")
    void shouldHaveSetSizeMethod() throws NoSuchMethodException {
      Method method = WasiDescriptor.class.getMethod("setSize", long.class);
      assertNotNull(method, "setSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "setSize should have 1 parameter");
      assertEquals(long.class, method.getParameterTypes()[0], "Parameter should be long");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "setSize should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have syncData method")
    void shouldHaveSyncDataMethod() throws NoSuchMethodException {
      Method method = WasiDescriptor.class.getMethod("syncData");
      assertNotNull(method, "syncData method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "syncData should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "syncData should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have sync method")
    void shouldHaveSyncMethod() throws NoSuchMethodException {
      Method method = WasiDescriptor.class.getMethod("sync");
      assertNotNull(method, "sync method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "sync should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "sync should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have openAt method with path and flags parameters")
    void shouldHaveOpenAtMethod() throws NoSuchMethodException {
      Method method =
          WasiDescriptor.class.getMethod("openAt", String.class, Set.class, Set.class, Set.class);
      assertNotNull(method, "openAt method should exist");
      assertEquals(
          WasiDescriptor.class, method.getReturnType(), "Return type should be WasiDescriptor");
      assertEquals(4, method.getParameterCount(), "openAt should have 4 parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "openAt should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have createDirectoryAt method with String parameter")
    void shouldHaveCreateDirectoryAtMethod() throws NoSuchMethodException {
      Method method = WasiDescriptor.class.getMethod("createDirectoryAt", String.class);
      assertNotNull(method, "createDirectoryAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "createDirectoryAt should have 1 parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "createDirectoryAt should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have readDirectory method returning List<String>")
    void shouldHaveReadDirectoryMethod() throws NoSuchMethodException {
      Method method = WasiDescriptor.class.getMethod("readDirectory");
      assertNotNull(method, "readDirectory method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
      assertEquals(0, method.getParameterCount(), "readDirectory should have no parameters");

      // Verify generic type
      Type genericReturnType = method.getGenericReturnType();
      assertTrue(
          genericReturnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
      assertEquals(
          String.class,
          parameterizedType.getActualTypeArguments()[0],
          "List should contain String");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "readDirectory should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have readLinkAt method returning String")
    void shouldHaveReadLinkAtMethod() throws NoSuchMethodException {
      Method method = WasiDescriptor.class.getMethod("readLinkAt", String.class);
      assertNotNull(method, "readLinkAt method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
      assertEquals(1, method.getParameterCount(), "readLinkAt should have 1 parameter");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "readLinkAt should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have unlinkFileAt method with String parameter")
    void shouldHaveUnlinkFileAtMethod() throws NoSuchMethodException {
      Method method = WasiDescriptor.class.getMethod("unlinkFileAt", String.class);
      assertNotNull(method, "unlinkFileAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "unlinkFileAt should have 1 parameter");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "unlinkFileAt should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have removeDirectoryAt method with String parameter")
    void shouldHaveRemoveDirectoryAtMethod() throws NoSuchMethodException {
      Method method = WasiDescriptor.class.getMethod("removeDirectoryAt", String.class);
      assertNotNull(method, "removeDirectoryAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "removeDirectoryAt should have 1 parameter");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "removeDirectoryAt should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have renameAt method with path and descriptor parameters")
    void shouldHaveRenameAtMethod() throws NoSuchMethodException {
      Method method =
          WasiDescriptor.class.getMethod(
              "renameAt", String.class, WasiDescriptor.class, String.class);
      assertNotNull(method, "renameAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(3, method.getParameterCount(), "renameAt should have 3 parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "renameAt should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have symlinkAt method with path parameters")
    void shouldHaveSymlinkAtMethod() throws NoSuchMethodException {
      Method method = WasiDescriptor.class.getMethod("symlinkAt", String.class, String.class);
      assertNotNull(method, "symlinkAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(2, method.getParameterCount(), "symlinkAt should have 2 parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "symlinkAt should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have linkAt method with flags and path parameters")
    void shouldHaveLinkAtMethod() throws NoSuchMethodException {
      Method method =
          WasiDescriptor.class.getMethod(
              "linkAt", Set.class, String.class, WasiDescriptor.class, String.class);
      assertNotNull(method, "linkAt method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(4, method.getParameterCount(), "linkAt should have 4 parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "linkAt should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have isSameObject method returning boolean")
    void shouldHaveIsSameObjectMethod() throws NoSuchMethodException {
      Method method = WasiDescriptor.class.getMethod("isSameObject", WasiDescriptor.class);
      assertNotNull(method, "isSameObject method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(1, method.getParameterCount(), "isSameObject should have 1 parameter");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "isSameObject should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("All WasiDescriptor declared methods should be public and abstract")
    void allMethodsShouldBePublicAbstract() {
      Method[] methods = WasiDescriptor.class.getDeclaredMethods();

      for (Method method : methods) {
        int modifiers = method.getModifiers();
        assertTrue(
            Modifier.isPublic(modifiers) && Modifier.isAbstract(modifiers),
            "Method " + method.getName() + " should be public and abstract");
      }
    }
  }

  // ========================================================================
  // Package Completeness Tests
  // ========================================================================

  @Nested
  @DisplayName("Package Completeness Tests")
  class PackageCompletenessTests {

    @Test
    @DisplayName("All WASI Filesystem classes should be loadable")
    void allClassesShouldBeLoadable() {
      assertNotNull(DescriptorType.class, "DescriptorType should be loadable");
      assertNotNull(DescriptorFlags.class, "DescriptorFlags should be loadable");
      assertNotNull(OpenFlags.class, "OpenFlags should be loadable");
      assertNotNull(PathFlags.class, "PathFlags should be loadable");
      assertNotNull(WasiDescriptor.class, "WasiDescriptor should be loadable");
    }

    @Test
    @DisplayName("WasiDescriptor should inherit from WasiResource")
    void wasiDescriptorShouldInheritFromWasiResource() {
      assertTrue(
          WasiResource.class.isAssignableFrom(WasiDescriptor.class),
          "WasiDescriptor should inherit from WasiResource");
    }

    @Test
    @DisplayName("WasiDescriptor should have expected method count")
    void wasiDescriptorShouldHaveExpectedMethodCount() {
      Method[] methods = WasiDescriptor.class.getDeclaredMethods();
      assertTrue(
          methods.length >= 15,
          "WasiDescriptor should have at least 15 declared methods, has " + methods.length);
    }
  }

  // ========================================================================
  // Type Safety Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Safety Tests")
  class TypeSafetyTests {

    @Test
    @DisplayName("openAt method should use Set for flags")
    void openAtShouldUseSetForFlags() throws NoSuchMethodException {
      Method method =
          WasiDescriptor.class.getMethod("openAt", String.class, Set.class, Set.class, Set.class);
      assertEquals(Set.class, method.getParameterTypes()[1], "pathFlags should be Set");
      assertEquals(Set.class, method.getParameterTypes()[2], "openFlags should be Set");
      assertEquals(Set.class, method.getParameterTypes()[3], "descriptorFlags should be Set");
    }

    @Test
    @DisplayName("getFlags should return Set for type safety")
    void getFlagsShouldReturnSet() throws NoSuchMethodException {
      Method method = WasiDescriptor.class.getMethod("getFlags");
      assertEquals(Set.class, method.getReturnType(), "getFlags should return Set");
    }
  }
}
