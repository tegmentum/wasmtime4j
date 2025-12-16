package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link WasiFilesystem} interface.
 *
 * <p>Tests verify:
 *
 * <ul>
 *   <li>Interface contract and method signatures
 *   <li>File operation method declarations
 *   <li>Directory operation method declarations
 *   <li>Path manipulation method declarations
 * </ul>
 *
 * <p>Note: These tests focus on the interface contract and expected behavior.
 * Implementation-specific tests are in the JNI/Panama modules.
 */
@DisplayName("WasiFilesystem Tests")
class WasiFilesystemTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("WasiFilesystem should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiFilesystem.class.isInterface(), "WasiFilesystem should be an interface");
    }

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      for (java.lang.reflect.Method method : WasiFilesystem.class.getDeclaredMethods()) {
        assertTrue(
            java.lang.reflect.Modifier.isPublic(method.getModifiers()),
            "Method " + method.getName() + " should be public");
      }
    }
  }

  @Nested
  @DisplayName("File Operation Method Tests")
  class FileOperationMethodTests {

    @Test
    @DisplayName("should have openFile method")
    void shouldHaveOpenFileMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod(
              "openFile", String.class, WasiOpenFlags.class, WasiRights.class),
          "WasiFilesystem should have openFile method");
    }

    @Test
    @DisplayName("openFile should return WasiFileHandle")
    void openFileShouldReturnWasiFileHandle() throws NoSuchMethodException {
      Class<?> returnType =
          WasiFilesystem.class
              .getMethod("openFile", String.class, WasiOpenFlags.class, WasiRights.class)
              .getReturnType();
      assertEquals(WasiFileHandle.class, returnType, "openFile should return WasiFileHandle");
    }

    @Test
    @DisplayName("should have closeFile method")
    void shouldHaveCloseFileMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod("closeFile", WasiFileHandle.class),
          "WasiFilesystem should have closeFile method");
    }

    @Test
    @DisplayName("closeFile should return void")
    void closeFileShouldReturnVoid() throws NoSuchMethodException {
      Class<?> returnType =
          WasiFilesystem.class.getMethod("closeFile", WasiFileHandle.class).getReturnType();
      assertEquals(void.class, returnType, "closeFile should return void");
    }

    @Test
    @DisplayName("should have readFile method")
    void shouldHaveReadFileMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod(
              "readFile", WasiFileHandle.class, ByteBuffer.class, long.class),
          "WasiFilesystem should have readFile method");
    }

    @Test
    @DisplayName("readFile should return long")
    void readFileShouldReturnLong() throws NoSuchMethodException {
      Class<?> returnType =
          WasiFilesystem.class
              .getMethod("readFile", WasiFileHandle.class, ByteBuffer.class, long.class)
              .getReturnType();
      assertEquals(long.class, returnType, "readFile should return long (bytes read)");
    }

    @Test
    @DisplayName("should have writeFile method")
    void shouldHaveWriteFileMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod(
              "writeFile", WasiFileHandle.class, ByteBuffer.class, long.class),
          "WasiFilesystem should have writeFile method");
    }

    @Test
    @DisplayName("writeFile should return long")
    void writeFileShouldReturnLong() throws NoSuchMethodException {
      Class<?> returnType =
          WasiFilesystem.class
              .getMethod("writeFile", WasiFileHandle.class, ByteBuffer.class, long.class)
              .getReturnType();
      assertEquals(long.class, returnType, "writeFile should return long (bytes written)");
    }

    @Test
    @DisplayName("should have syncFile method")
    void shouldHaveSyncFileMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod("syncFile", WasiFileHandle.class),
          "WasiFilesystem should have syncFile method");
    }
  }

  @Nested
  @DisplayName("Directory Operation Method Tests")
  class DirectoryOperationMethodTests {

    @Test
    @DisplayName("should have openDirectory method")
    void shouldHaveOpenDirectoryMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod("openDirectory", String.class, WasiRights.class),
          "WasiFilesystem should have openDirectory method");
    }

    @Test
    @DisplayName("openDirectory should return WasiDirectoryHandle")
    void openDirectoryShouldReturnWasiDirectoryHandle() throws NoSuchMethodException {
      Class<?> returnType =
          WasiFilesystem.class
              .getMethod("openDirectory", String.class, WasiRights.class)
              .getReturnType();
      assertEquals(
          WasiDirectoryHandle.class, returnType, "openDirectory should return WasiDirectoryHandle");
    }

    @Test
    @DisplayName("should have readDirectory method")
    void shouldHaveReadDirectoryMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod("readDirectory", WasiDirectoryHandle.class),
          "WasiFilesystem should have readDirectory method");
    }

    @Test
    @DisplayName("readDirectory should return List")
    void readDirectoryShouldReturnList() throws NoSuchMethodException {
      Class<?> returnType =
          WasiFilesystem.class
              .getMethod("readDirectory", WasiDirectoryHandle.class)
              .getReturnType();
      assertEquals(List.class, returnType, "readDirectory should return List");
    }

    @Test
    @DisplayName("should have createDirectory method")
    void shouldHaveCreateDirectoryMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod("createDirectory", String.class, WasiPermissions.class),
          "WasiFilesystem should have createDirectory method");
    }

    @Test
    @DisplayName("createDirectory should return void")
    void createDirectoryShouldReturnVoid() throws NoSuchMethodException {
      Class<?> returnType =
          WasiFilesystem.class
              .getMethod("createDirectory", String.class, WasiPermissions.class)
              .getReturnType();
      assertEquals(void.class, returnType, "createDirectory should return void");
    }

    @Test
    @DisplayName("should have removeDirectory method")
    void shouldHaveRemoveDirectoryMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod("removeDirectory", String.class),
          "WasiFilesystem should have removeDirectory method");
    }
  }

  @Nested
  @DisplayName("File Stats Method Tests")
  class FileStatsMethodTests {

    @Test
    @DisplayName("should have getFileStats method")
    void shouldHaveGetFileStatsMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod("getFileStats", String.class),
          "WasiFilesystem should have getFileStats method");
    }

    @Test
    @DisplayName("getFileStats should return WasiFileStats")
    void getFileStatsShouldReturnWasiFileStats() throws NoSuchMethodException {
      Class<?> returnType =
          WasiFilesystem.class.getMethod("getFileStats", String.class).getReturnType();
      assertEquals(WasiFileStats.class, returnType, "getFileStats should return WasiFileStats");
    }

    @Test
    @DisplayName("should have setFileStats method")
    void shouldHaveSetFileStatsMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod("setFileStats", String.class, WasiFileStats.class),
          "WasiFilesystem should have setFileStats method");
    }

    @Test
    @DisplayName("should have setFilePermissions method")
    void shouldHaveSetFilePermissionsMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod("setFilePermissions", String.class, WasiPermissions.class),
          "WasiFilesystem should have setFilePermissions method");
    }
  }

  @Nested
  @DisplayName("Path Manipulation Method Tests")
  class PathManipulationMethodTests {

    @Test
    @DisplayName("should have canonicalizePath method")
    void shouldHaveCanonicalizePathMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod("canonicalizePath", String.class),
          "WasiFilesystem should have canonicalizePath method");
    }

    @Test
    @DisplayName("canonicalizePath should return String")
    void canonicalizePathShouldReturnString() throws NoSuchMethodException {
      Class<?> returnType =
          WasiFilesystem.class.getMethod("canonicalizePath", String.class).getReturnType();
      assertEquals(String.class, returnType, "canonicalizePath should return String");
    }

    @Test
    @DisplayName("should have rename method")
    void shouldHaveRenameMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod("rename", String.class, String.class),
          "WasiFilesystem should have rename method");
    }

    @Test
    @DisplayName("should have unlink method")
    void shouldHaveUnlinkMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod("unlink", String.class),
          "WasiFilesystem should have unlink method");
    }
  }

  @Nested
  @DisplayName("Symlink Method Tests")
  class SymlinkMethodTests {

    @Test
    @DisplayName("should have symlinkCreate method")
    void shouldHaveSymlinkCreateMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod("symlinkCreate", String.class, String.class),
          "WasiFilesystem should have symlinkCreate method");
    }

    @Test
    @DisplayName("should have readSymlink method")
    void shouldHaveReadSymlinkMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod("readSymlink", String.class),
          "WasiFilesystem should have readSymlink method");
    }

    @Test
    @DisplayName("readSymlink should return String")
    void readSymlinkShouldReturnString() throws NoSuchMethodException {
      Class<?> returnType =
          WasiFilesystem.class.getMethod("readSymlink", String.class).getReturnType();
      assertEquals(String.class, returnType, "readSymlink should return String");
    }
  }

  @Nested
  @DisplayName("Working Directory Method Tests")
  class WorkingDirectoryMethodTests {

    @Test
    @DisplayName("should have getCurrentWorkingDirectory method")
    void shouldHaveGetCurrentWorkingDirectoryMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod("getCurrentWorkingDirectory"),
          "WasiFilesystem should have getCurrentWorkingDirectory method");
    }

    @Test
    @DisplayName("getCurrentWorkingDirectory should return String")
    void getCurrentWorkingDirectoryShouldReturnString() throws NoSuchMethodException {
      Class<?> returnType =
          WasiFilesystem.class.getMethod("getCurrentWorkingDirectory").getReturnType();
      assertEquals(String.class, returnType, "getCurrentWorkingDirectory should return String");
    }

    @Test
    @DisplayName("should have setCurrentWorkingDirectory method")
    void shouldHaveSetCurrentWorkingDirectoryMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiFilesystem.class.getMethod("setCurrentWorkingDirectory", String.class),
          "WasiFilesystem should have setCurrentWorkingDirectory method");
    }

    @Test
    @DisplayName("setCurrentWorkingDirectory should return void")
    void setCurrentWorkingDirectoryShouldReturnVoid() throws NoSuchMethodException {
      Class<?> returnType =
          WasiFilesystem.class
              .getMethod("setCurrentWorkingDirectory", String.class)
              .getReturnType();
      assertEquals(void.class, returnType, "setCurrentWorkingDirectory should return void");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("openFile should declare WasmException")
    void openFileShouldDeclareWasmException() throws NoSuchMethodException {
      Class<?>[] exceptionTypes =
          WasiFilesystem.class
              .getMethod("openFile", String.class, WasiOpenFlags.class, WasiRights.class)
              .getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "openFile should declare at least one exception");
    }

    @Test
    @DisplayName("readFile should declare WasmException")
    void readFileShouldDeclareWasmException() throws NoSuchMethodException {
      Class<?>[] exceptionTypes =
          WasiFilesystem.class
              .getMethod("readFile", WasiFileHandle.class, ByteBuffer.class, long.class)
              .getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "readFile should declare at least one exception");
    }

    @Test
    @DisplayName("writeFile should declare WasmException")
    void writeFileShouldDeclareWasmException() throws NoSuchMethodException {
      Class<?>[] exceptionTypes =
          WasiFilesystem.class
              .getMethod("writeFile", WasiFileHandle.class, ByteBuffer.class, long.class)
              .getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "writeFile should declare at least one exception");
    }

    @Test
    @DisplayName("createDirectory should declare WasmException")
    void createDirectoryShouldDeclareWasmException() throws NoSuchMethodException {
      Class<?>[] exceptionTypes =
          WasiFilesystem.class
              .getMethod("createDirectory", String.class, WasiPermissions.class)
              .getExceptionTypes();
      assertTrue(
          exceptionTypes.length > 0, "createDirectory should declare at least one exception");
    }
  }

  @Nested
  @DisplayName("Related Type Tests")
  class RelatedTypeTests {

    @Test
    @DisplayName("WasiFileHandle should exist")
    void wasiFileHandleShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiFileHandle");
        assertNotNull(clazz, "WasiFileHandle class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("WasiFileHandle class should exist", e);
      }
    }

    @Test
    @DisplayName("WasiDirectoryHandle should exist")
    void wasiDirectoryHandleShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiDirectoryHandle");
        assertNotNull(clazz, "WasiDirectoryHandle class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("WasiDirectoryHandle class should exist", e);
      }
    }

    @Test
    @DisplayName("WasiOpenFlags should exist")
    void wasiOpenFlagsShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiOpenFlags");
        assertNotNull(clazz, "WasiOpenFlags class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("WasiOpenFlags class should exist", e);
      }
    }

    @Test
    @DisplayName("WasiRights should exist")
    void wasiRightsShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiRights");
        assertNotNull(clazz, "WasiRights class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("WasiRights class should exist", e);
      }
    }

    @Test
    @DisplayName("WasiPermissions should exist")
    void wasiPermissionsShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiPermissions");
        assertNotNull(clazz, "WasiPermissions class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("WasiPermissions class should exist", e);
      }
    }

    @Test
    @DisplayName("WasiFileStats should exist")
    void wasiFileStatsShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiFileStats");
        assertNotNull(clazz, "WasiFileStats class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("WasiFileStats class should exist", e);
      }
    }

    @Test
    @DisplayName("WasiDirEntry should exist")
    void wasiDirEntryShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiDirEntry");
        assertNotNull(clazz, "WasiDirEntry class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("WasiDirEntry class should exist", e);
      }
    }
  }

  @Nested
  @DisplayName("Interface Method Count Tests")
  class InterfaceMethodCountTests {

    @Test
    @DisplayName("interface should have expected method count")
    void interfaceShouldHaveExpectedMethodCount() {
      int methodCount = WasiFilesystem.class.getDeclaredMethods().length;
      // openFile, closeFile, readFile, writeFile,
      // openDirectory, readDirectory, createDirectory, removeDirectory,
      // getFileStats, setFileStats, setFilePermissions,
      // canonicalizePath, symlinkCreate, readSymlink, rename, unlink,
      // syncFile, getCurrentWorkingDirectory, setCurrentWorkingDirectory = 19 methods
      assertEquals(19, methodCount, "WasiFilesystem should have 19 declared methods");
    }
  }
}
