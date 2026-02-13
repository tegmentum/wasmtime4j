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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiDirectoryHandle;
import ai.tegmentum.wasmtime4j.wasi.WasiFileHandle;
import ai.tegmentum.wasmtime4j.wasi.WasiFileStats;
import ai.tegmentum.wasmtime4j.wasi.WasiFilesystem;
import ai.tegmentum.wasmtime4j.wasi.WasiOpenFlags;
import ai.tegmentum.wasmtime4j.wasi.WasiPermissions;
import ai.tegmentum.wasmtime4j.wasi.WasiRights;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaWasiFilesystem} class.
 *
 * <p>This test class verifies the Panama WASI filesystem implementation including class structure,
 * method signatures, field declarations, and interface compliance using reflection.
 */
@DisplayName("PanamaWasiFilesystem Tests")
class PanamaWasiFilesystemTest {

  // ========================================================================
  // Class Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaWasiFilesystem should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaWasiFilesystem.class.getModifiers()),
          "PanamaWasiFilesystem should be final");
    }

    @Test
    @DisplayName("PanamaWasiFilesystem should be a public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiFilesystem.class.getModifiers()),
          "PanamaWasiFilesystem should be public");
    }

    @Test
    @DisplayName("PanamaWasiFilesystem should implement WasiFilesystem interface")
    void shouldImplementWasiFilesystemInterface() {
      Class<?>[] interfaces = PanamaWasiFilesystem.class.getInterfaces();
      boolean implementsWasiFilesystem = Arrays.asList(interfaces).contains(WasiFilesystem.class);
      assertTrue(
          implementsWasiFilesystem,
          "PanamaWasiFilesystem should implement WasiFilesystem interface");
    }

    @Test
    @DisplayName("PanamaWasiFilesystem should be in the correct package")
    void shouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.panama",
          PanamaWasiFilesystem.class.getPackage().getName(),
          "PanamaWasiFilesystem should be in ai.tegmentum.wasmtime4j.panama package");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have LOGGER static field")
    void shouldHaveLoggerField() throws NoSuchFieldException {
      Field field = PanamaWasiFilesystem.class.getDeclaredField("LOGGER");
      assertNotNull(field, "LOGGER field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
      assertEquals(Logger.class, field.getType(), "LOGGER should be of type Logger");
    }

    @Test
    @DisplayName("should have rootPath field")
    void shouldHaveRootPathField() throws NoSuchFieldException {
      Field field = PanamaWasiFilesystem.class.getDeclaredField("rootPath");
      assertNotNull(field, "rootPath field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "rootPath should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "rootPath should be private");
      assertEquals(Path.class, field.getType(), "rootPath should be of type Path");
    }

    @Test
    @DisplayName("should have fdCounter field")
    void shouldHaveFdCounterField() throws NoSuchFieldException {
      Field field = PanamaWasiFilesystem.class.getDeclaredField("fdCounter");
      assertNotNull(field, "fdCounter field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "fdCounter should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "fdCounter should be private");
      assertEquals(
          AtomicInteger.class, field.getType(), "fdCounter should be of type AtomicInteger");
    }

    @Test
    @DisplayName("should have openFiles field")
    void shouldHaveOpenFilesField() throws NoSuchFieldException {
      Field field = PanamaWasiFilesystem.class.getDeclaredField("openFiles");
      assertNotNull(field, "openFiles field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "openFiles should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "openFiles should be private");
      assertEquals(Map.class, field.getType(), "openFiles should be of type Map");
    }

    @Test
    @DisplayName("should have openDirectories field")
    void shouldHaveOpenDirectoriesField() throws NoSuchFieldException {
      Field field = PanamaWasiFilesystem.class.getDeclaredField("openDirectories");
      assertNotNull(field, "openDirectories field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "openDirectories should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "openDirectories should be private");
      assertEquals(Map.class, field.getType(), "openDirectories should be of type Map");
    }

    @Test
    @DisplayName("should have currentWorkingDirectory field")
    void shouldHaveCurrentWorkingDirectoryField() throws NoSuchFieldException {
      Field field = PanamaWasiFilesystem.class.getDeclaredField("currentWorkingDirectory");
      assertNotNull(field, "currentWorkingDirectory field should exist");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "currentWorkingDirectory should be private");
      assertTrue(
          Modifier.isVolatile(field.getModifiers()), "currentWorkingDirectory should be volatile");
      assertEquals(
          String.class, field.getType(), "currentWorkingDirectory should be of type String");
    }

    @Test
    @DisplayName("should have resourceHandle field")
    void shouldHaveResourceHandleField() throws NoSuchFieldException {
      Field field = PanamaWasiFilesystem.class.getDeclaredField("resourceHandle");
      assertNotNull(field, "resourceHandle field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "resourceHandle should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "resourceHandle should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with Path parameter")
    void shouldHavePathConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = PanamaWasiFilesystem.class.getConstructor(Path.class);
      assertNotNull(constructor, "Path constructor should exist");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()), "Path constructor should be public");
      assertEquals(1, constructor.getParameterCount(), "Constructor should have 1 parameter");
      assertEquals(Path.class, constructor.getParameterTypes()[0], "Parameter should be Path");
    }
  }

  // ========================================================================
  // File Operation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("File Operation Method Tests")
  class FileOperationMethodTests {

    @Test
    @DisplayName("should have openFile method")
    void shouldHaveOpenFileMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiFilesystem.class.getMethod(
              "openFile", String.class, WasiOpenFlags.class, WasiRights.class);
      assertNotNull(method, "openFile method should exist");
      assertEquals(
          WasiFileHandle.class, method.getReturnType(), "openFile should return WasiFileHandle");
      assertEquals(3, method.getParameterCount(), "openFile should have 3 parameters");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "openFile should throw WasmException");
    }

    @Test
    @DisplayName("should have closeFile method")
    void shouldHaveCloseFileMethod() throws NoSuchMethodException {
      Method method = PanamaWasiFilesystem.class.getMethod("closeFile", WasiFileHandle.class);
      assertNotNull(method, "closeFile method should exist");
      assertEquals(void.class, method.getReturnType(), "closeFile should return void");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "closeFile should throw WasmException");
    }

    @Test
    @DisplayName("should have readFile method")
    void shouldHaveReadFileMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiFilesystem.class.getMethod(
              "readFile", WasiFileHandle.class, ByteBuffer.class, long.class);
      assertNotNull(method, "readFile method should exist");
      assertEquals(long.class, method.getReturnType(), "readFile should return long");
      assertEquals(3, method.getParameterCount(), "readFile should have 3 parameters");
    }

    @Test
    @DisplayName("should have writeFile method")
    void shouldHaveWriteFileMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiFilesystem.class.getMethod(
              "writeFile", WasiFileHandle.class, ByteBuffer.class, long.class);
      assertNotNull(method, "writeFile method should exist");
      assertEquals(long.class, method.getReturnType(), "writeFile should return long");
      assertEquals(3, method.getParameterCount(), "writeFile should have 3 parameters");
    }

    @Test
    @DisplayName("should have syncFile method")
    void shouldHaveSyncFileMethod() throws NoSuchMethodException {
      Method method = PanamaWasiFilesystem.class.getMethod("syncFile", WasiFileHandle.class);
      assertNotNull(method, "syncFile method should exist");
      assertEquals(void.class, method.getReturnType(), "syncFile should return void");
    }
  }

  // ========================================================================
  // Directory Operation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Directory Operation Method Tests")
  class DirectoryOperationMethodTests {

    @Test
    @DisplayName("should have openDirectory method")
    void shouldHaveOpenDirectoryMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiFilesystem.class.getMethod("openDirectory", String.class, WasiRights.class);
      assertNotNull(method, "openDirectory method should exist");
      assertEquals(
          WasiDirectoryHandle.class,
          method.getReturnType(),
          "openDirectory should return WasiDirectoryHandle");
      assertEquals(2, method.getParameterCount(), "openDirectory should have 2 parameters");
    }

    @Test
    @DisplayName("should have readDirectory method")
    void shouldHaveReadDirectoryMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiFilesystem.class.getMethod("readDirectory", WasiDirectoryHandle.class);
      assertNotNull(method, "readDirectory method should exist");
      assertEquals(List.class, method.getReturnType(), "readDirectory should return List");
    }

    @Test
    @DisplayName("should have createDirectory method")
    void shouldHaveCreateDirectoryMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiFilesystem.class.getMethod(
              "createDirectory", String.class, WasiPermissions.class);
      assertNotNull(method, "createDirectory method should exist");
      assertEquals(void.class, method.getReturnType(), "createDirectory should return void");
    }

    @Test
    @DisplayName("should have removeDirectory method")
    void shouldHaveRemoveDirectoryMethod() throws NoSuchMethodException {
      Method method = PanamaWasiFilesystem.class.getMethod("removeDirectory", String.class);
      assertNotNull(method, "removeDirectory method should exist");
      assertEquals(void.class, method.getReturnType(), "removeDirectory should return void");
    }
  }

  // ========================================================================
  // File Stats Method Tests
  // ========================================================================

  @Nested
  @DisplayName("File Stats Method Tests")
  class FileStatsMethodTests {

    @Test
    @DisplayName("should have getFileStats method")
    void shouldHaveGetFileStatsMethod() throws NoSuchMethodException {
      Method method = PanamaWasiFilesystem.class.getMethod("getFileStats", String.class);
      assertNotNull(method, "getFileStats method should exist");
      assertEquals(
          WasiFileStats.class, method.getReturnType(), "getFileStats should return WasiFileStats");
    }

    @Test
    @DisplayName("should have setFileStats method")
    void shouldHaveSetFileStatsMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiFilesystem.class.getMethod("setFileStats", String.class, WasiFileStats.class);
      assertNotNull(method, "setFileStats method should exist");
      assertEquals(void.class, method.getReturnType(), "setFileStats should return void");
    }

    @Test
    @DisplayName("should have setFilePermissions method")
    void shouldHaveSetFilePermissionsMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiFilesystem.class.getMethod(
              "setFilePermissions", String.class, WasiPermissions.class);
      assertNotNull(method, "setFilePermissions method should exist");
      assertEquals(void.class, method.getReturnType(), "setFilePermissions should return void");
    }
  }

  // ========================================================================
  // Path Operation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Path Operation Method Tests")
  class PathOperationMethodTests {

    @Test
    @DisplayName("should have canonicalizePath method")
    void shouldHaveCanonicalizePathMethod() throws NoSuchMethodException {
      Method method = PanamaWasiFilesystem.class.getMethod("canonicalizePath", String.class);
      assertNotNull(method, "canonicalizePath method should exist");
      assertEquals(String.class, method.getReturnType(), "canonicalizePath should return String");
    }

    @Test
    @DisplayName("should have symlinkCreate method")
    void shouldHaveSymlinkCreateMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiFilesystem.class.getMethod("symlinkCreate", String.class, String.class);
      assertNotNull(method, "symlinkCreate method should exist");
      assertEquals(void.class, method.getReturnType(), "symlinkCreate should return void");
    }

    @Test
    @DisplayName("should have readSymlink method")
    void shouldHaveReadSymlinkMethod() throws NoSuchMethodException {
      Method method = PanamaWasiFilesystem.class.getMethod("readSymlink", String.class);
      assertNotNull(method, "readSymlink method should exist");
      assertEquals(String.class, method.getReturnType(), "readSymlink should return String");
    }

    @Test
    @DisplayName("should have rename method")
    void shouldHaveRenameMethod() throws NoSuchMethodException {
      Method method = PanamaWasiFilesystem.class.getMethod("rename", String.class, String.class);
      assertNotNull(method, "rename method should exist");
      assertEquals(void.class, method.getReturnType(), "rename should return void");
    }

    @Test
    @DisplayName("should have unlink method")
    void shouldHaveUnlinkMethod() throws NoSuchMethodException {
      Method method = PanamaWasiFilesystem.class.getMethod("unlink", String.class);
      assertNotNull(method, "unlink method should exist");
      assertEquals(void.class, method.getReturnType(), "unlink should return void");
    }
  }

  // ========================================================================
  // Working Directory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Working Directory Method Tests")
  class WorkingDirectoryMethodTests {

    @Test
    @DisplayName("should have getCurrentWorkingDirectory method")
    void shouldHaveGetCurrentWorkingDirectoryMethod() throws NoSuchMethodException {
      Method method = PanamaWasiFilesystem.class.getMethod("getCurrentWorkingDirectory");
      assertNotNull(method, "getCurrentWorkingDirectory method should exist");
      assertEquals(
          String.class, method.getReturnType(), "getCurrentWorkingDirectory should return String");
      assertEquals(
          0, method.getParameterCount(), "getCurrentWorkingDirectory should have no parameters");
    }

    @Test
    @DisplayName("should have setCurrentWorkingDirectory method")
    void shouldHaveSetCurrentWorkingDirectoryMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiFilesystem.class.getMethod("setCurrentWorkingDirectory", String.class);
      assertNotNull(method, "setCurrentWorkingDirectory method should exist");
      assertEquals(
          void.class, method.getReturnType(), "setCurrentWorkingDirectory should return void");
    }
  }

  // ========================================================================
  // Lifecycle Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = PanamaWasiFilesystem.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "close should throw WasmException");
    }
  }

  // ========================================================================
  // Accessor Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getRootPath method")
    void shouldHaveGetRootPathMethod() throws NoSuchMethodException {
      Method method = PanamaWasiFilesystem.class.getMethod("getRootPath");
      assertNotNull(method, "getRootPath method should exist");
      assertEquals(Path.class, method.getReturnType(), "getRootPath should return Path");
      assertEquals(0, method.getParameterCount(), "getRootPath should have no parameters");
    }

    @Test
    @DisplayName("should have getOpenFileCount method")
    void shouldHaveGetOpenFileCountMethod() throws NoSuchMethodException {
      Method method = PanamaWasiFilesystem.class.getMethod("getOpenFileCount");
      assertNotNull(method, "getOpenFileCount method should exist");
      assertEquals(int.class, method.getReturnType(), "getOpenFileCount should return int");
      assertEquals(0, method.getParameterCount(), "getOpenFileCount should have no parameters");
    }

    @Test
    @DisplayName("should have getOpenDirectoryCount method")
    void shouldHaveGetOpenDirectoryCountMethod() throws NoSuchMethodException {
      Method method = PanamaWasiFilesystem.class.getMethod("getOpenDirectoryCount");
      assertNotNull(method, "getOpenDirectoryCount method should exist");
      assertEquals(int.class, method.getReturnType(), "getOpenDirectoryCount should return int");
      assertEquals(
          0, method.getParameterCount(), "getOpenDirectoryCount should have no parameters");
    }
  }

  // ========================================================================
  // Private Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Private Method Tests")
  class PrivateMethodTests {

    @Test
    @DisplayName("should have private resolvePath method")
    void shouldHaveResolvePathMethod() throws NoSuchMethodException {
      Method method = PanamaWasiFilesystem.class.getDeclaredMethod("resolvePath", String.class);
      assertNotNull(method, "resolvePath method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "resolvePath should be private");
      assertEquals(Path.class, method.getReturnType(), "resolvePath should return Path");
    }

    @Test
    @DisplayName("should have private validatePathWithinSandbox method")
    void shouldHaveValidatePathWithinSandboxMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiFilesystem.class.getDeclaredMethod("validatePathWithinSandbox", Path.class);
      assertNotNull(method, "validatePathWithinSandbox method should exist");
      assertTrue(
          Modifier.isPrivate(method.getModifiers()), "validatePathWithinSandbox should be private");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "validatePathWithinSandbox should throw WasmException");
    }

    @Test
    @DisplayName("should have private convertOpenFlags method")
    void shouldHaveConvertOpenFlagsMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiFilesystem.class.getDeclaredMethod("convertOpenFlags", WasiOpenFlags.class);
      assertNotNull(method, "convertOpenFlags method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "convertOpenFlags should be private");
    }

    @Test
    @DisplayName("should have private ensureNotClosed method")
    void shouldHaveEnsureNotClosedMethod() throws NoSuchMethodException {
      Method method = PanamaWasiFilesystem.class.getDeclaredMethod("ensureNotClosed");
      assertNotNull(method, "ensureNotClosed method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "ensureNotClosed should be private");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "ensureNotClosed should throw WasmException");
    }
  }

  // ========================================================================
  // Interface Compliance Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("should implement all WasiFilesystem interface methods")
    void shouldImplementAllInterfaceMethods() {
      Method[] interfaceMethods = WasiFilesystem.class.getDeclaredMethods();
      Class<?> implClass = PanamaWasiFilesystem.class;

      for (Method interfaceMethod : interfaceMethods) {
        if (Modifier.isAbstract(interfaceMethod.getModifiers())) {
          try {
            Method implMethod =
                implClass.getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
            assertNotNull(
                implMethod, "Implementation should have method: " + interfaceMethod.getName());
            assertFalse(
                Modifier.isAbstract(implMethod.getModifiers()),
                "Method " + interfaceMethod.getName() + " should not be abstract");
          } catch (NoSuchMethodException e) {
            // May be a default method
          }
        }
      }
    }
  }

  // ========================================================================
  // Thread Safety Tests
  // ========================================================================

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("fdCounter field should use AtomicInteger for thread safety")
    void fdCounterShouldUseAtomicInteger() throws NoSuchFieldException {
      Field field = PanamaWasiFilesystem.class.getDeclaredField("fdCounter");
      assertEquals(
          AtomicInteger.class,
          field.getType(),
          "fdCounter field should be AtomicInteger for thread-safe file descriptor allocation");
    }

    @Test
    @DisplayName("currentWorkingDirectory field should be volatile")
    void currentWorkingDirectoryShouldBeVolatile() throws NoSuchFieldException {
      Field field = PanamaWasiFilesystem.class.getDeclaredField("currentWorkingDirectory");
      assertTrue(
          Modifier.isVolatile(field.getModifiers()),
          "currentWorkingDirectory should be volatile for thread visibility");
    }

    @Test
    @DisplayName("resourceHandle field should be final for thread safety")
    void resourceHandleFieldShouldBeFinal() throws NoSuchFieldException {
      Field field = PanamaWasiFilesystem.class.getDeclaredField("resourceHandle");
      assertTrue(
          Modifier.isFinal(field.getModifiers()),
          "resourceHandle should be final for thread safety");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected number of public methods")
    void shouldHaveExpectedPublicMethods() {
      long publicMethodCount =
          Arrays.stream(PanamaWasiFilesystem.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();
      assertTrue(
          publicMethodCount >= 15,
          "PanamaWasiFilesystem should have at least 15 public methods, found: "
              + publicMethodCount);
    }

    @Test
    @DisplayName("should have expected number of private methods")
    void shouldHaveExpectedPrivateMethods() {
      long privateMethodCount =
          Arrays.stream(PanamaWasiFilesystem.class.getDeclaredMethods())
              .filter(m -> Modifier.isPrivate(m.getModifiers()))
              .count();
      assertTrue(
          privateMethodCount >= 3,
          "PanamaWasiFilesystem should have at least 3 private helper methods, found: "
              + privateMethodCount);
    }
  }
}
