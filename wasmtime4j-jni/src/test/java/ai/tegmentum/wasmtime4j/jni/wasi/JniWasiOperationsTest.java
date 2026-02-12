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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.wasi.permission.WasiPermissionManager;
import ai.tegmentum.wasmtime4j.wasi.permission.WasiResourceLimits;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive reflection-based tests for JNI WASI Operations classes.
 *
 * <p>Tests: WasiAdvancedFileOperations, WasiPermissionManager, WasiProcessOperations
 *
 * <p>These tests verify class structure, method signatures, and field declarations without loading
 * native libraries.
 */
@DisplayName("JNI WASI Operations Tests")
class JniWasiOperationsTest {

  // =========================================================================
  // WasiAdvancedFileOperations Tests
  // =========================================================================

  @Nested
  @DisplayName("WasiAdvancedFileOperations Class Tests")
  class WasiAdvancedFileOperationsClassTests {

    @Test
    @DisplayName("WasiAdvancedFileOperations should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiAdvancedFileOperations.class.getModifiers()),
          "WasiAdvancedFileOperations should be final");
    }

    @Test
    @DisplayName("WasiAdvancedFileOperations should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(WasiAdvancedFileOperations.class.getModifiers()),
          "WasiAdvancedFileOperations should be public");
    }

    @Test
    @DisplayName("Should have required constructor")
    void shouldHaveRequiredConstructor() throws Exception {
      final Constructor<?> constructor =
          WasiAdvancedFileOperations.class.getDeclaredConstructor(
              WasiContext.class, WasiFileSystem.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("Should have LOGGER field")
    void shouldHaveLoggerField() throws Exception {
      final Field loggerField = WasiAdvancedFileOperations.class.getDeclaredField("LOGGER");
      assertNotNull(loggerField, "LOGGER field should exist");
      assertTrue(Modifier.isPrivate(loggerField.getModifiers()), "LOGGER should be private");
      assertTrue(Modifier.isStatic(loggerField.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(loggerField.getModifiers()), "LOGGER should be final");
      assertEquals(Logger.class, loggerField.getType(), "LOGGER should be Logger type");
    }

    @Test
    @DisplayName("Should have wasiContext field")
    void shouldHaveWasiContextField() throws Exception {
      final Field field = WasiAdvancedFileOperations.class.getDeclaredField("wasiContext");
      assertNotNull(field, "wasiContext field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "wasiContext should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "wasiContext should be final");
      assertEquals(WasiContext.class, field.getType(), "Should be WasiContext type");
    }

    @Test
    @DisplayName("Should have fileSystem field")
    void shouldHaveFileSystemField() throws Exception {
      final Field field = WasiAdvancedFileOperations.class.getDeclaredField("fileSystem");
      assertNotNull(field, "fileSystem field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "fileSystem should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "fileSystem should be final");
      assertEquals(WasiFileSystem.class, field.getType(), "Should be WasiFileSystem type");
    }

    @Test
    @DisplayName("Should have asyncExecutor field")
    void shouldHaveAsyncExecutorField() throws Exception {
      final Field field = WasiAdvancedFileOperations.class.getDeclaredField("asyncExecutor");
      assertNotNull(field, "asyncExecutor field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "asyncExecutor should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "asyncExecutor should be final");
      assertEquals(ExecutorService.class, field.getType(), "Should be ExecutorService type");
    }

    @Test
    @DisplayName("Should have createSymbolicLink method")
    void shouldHaveCreateSymbolicLinkMethod() throws Exception {
      final Method method =
          WasiAdvancedFileOperations.class.getDeclaredMethod(
              "createSymbolicLink", String.class, String.class);
      assertNotNull(method, "createSymbolicLink method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have readSymbolicLink method")
    void shouldHaveReadSymbolicLinkMethod() throws Exception {
      final Method method =
          WasiAdvancedFileOperations.class.getDeclaredMethod("readSymbolicLink", String.class);
      assertNotNull(method, "readSymbolicLink method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("Should have createHardLink method")
    void shouldHaveCreateHardLinkMethod() throws Exception {
      final Method method =
          WasiAdvancedFileOperations.class.getDeclaredMethod(
              "createHardLink", String.class, String.class);
      assertNotNull(method, "createHardLink method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have setFilePermissions method")
    void shouldHaveSetFilePermissionsMethod() throws Exception {
      final Method method =
          WasiAdvancedFileOperations.class.getDeclaredMethod(
              "setFilePermissions", String.class, int.class);
      assertNotNull(method, "setFilePermissions method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have getFilePermissions method")
    void shouldHaveGetFilePermissionsMethod() throws Exception {
      final Method method =
          WasiAdvancedFileOperations.class.getDeclaredMethod("getFilePermissions", String.class);
      assertNotNull(method, "getFilePermissions method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("Should have listDirectoryRecursive method")
    void shouldHaveListDirectoryRecursiveMethod() throws Exception {
      final Method method =
          WasiAdvancedFileOperations.class.getDeclaredMethod(
              "listDirectoryRecursive", String.class, int.class);
      assertNotNull(method, "listDirectoryRecursive method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("Should have async file operation methods")
    void shouldHaveAsyncFileOperationMethods() throws Exception {
      // copyFileAsync
      final Method copyAsync =
          WasiAdvancedFileOperations.class.getDeclaredMethod(
              "copyFileAsync", String.class, String.class);
      assertNotNull(copyAsync, "copyFileAsync method should exist");
      assertTrue(Modifier.isPublic(copyAsync.getModifiers()), "Should be public");
      assertEquals(
          CompletableFuture.class, copyAsync.getReturnType(), "Should return CompletableFuture");

      // moveFileAsync
      final Method moveAsync =
          WasiAdvancedFileOperations.class.getDeclaredMethod(
              "moveFileAsync", String.class, String.class);
      assertNotNull(moveAsync, "moveFileAsync method should exist");
      assertEquals(
          CompletableFuture.class, moveAsync.getReturnType(), "Should return CompletableFuture");

      // deleteDirectoryTreeAsync
      final Method deleteAsync =
          WasiAdvancedFileOperations.class.getDeclaredMethod(
              "deleteDirectoryTreeAsync", String.class);
      assertNotNull(deleteAsync, "deleteDirectoryTreeAsync method should exist");
      assertEquals(
          CompletableFuture.class, deleteAsync.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("Should have close method")
    void shouldHaveCloseMethod() throws Exception {
      final Method method = WasiAdvancedFileOperations.class.getDeclaredMethod("close");
      assertNotNull(method, "close method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // =========================================================================
  // WasiPermissionManager Tests
  // =========================================================================

  @Nested
  @DisplayName("WasiPermissionManager Class Tests")
  class WasiPermissionManagerClassTests {

    @Test
    @DisplayName("WasiPermissionManager should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiPermissionManager.class.getModifiers()),
          "WasiPermissionManager should be final");
    }

    @Test
    @DisplayName("Should have private constructor taking Builder")
    void shouldHavePrivateConstructor() throws Exception {
      // WasiPermissionManager uses Builder pattern, constructor takes Builder
      final Class<?> builderClass =
          Class.forName(WasiPermissionManager.class.getName() + "$Builder");
      final Constructor<?> constructor =
          WasiPermissionManager.class.getDeclaredConstructor(builderClass);
      assertNotNull(constructor, "Constructor taking Builder should exist");
      assertTrue(
          Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private for Builder pattern");
    }

    @Test
    @DisplayName("Should have Builder inner class")
    void shouldHaveBuilderClass() throws Exception {
      final Class<?> builderClass =
          Class.forName(WasiPermissionManager.class.getName() + "$Builder");
      assertNotNull(builderClass, "Builder inner class should exist");
      assertTrue(Modifier.isPublic(builderClass.getModifiers()), "Builder should be public");
      assertTrue(Modifier.isStatic(builderClass.getModifiers()), "Builder should be static");
      assertTrue(Modifier.isFinal(builderClass.getModifiers()), "Builder should be final");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws Exception {
      final Class<?> builderClass =
          Class.forName(WasiPermissionManager.class.getName() + "$Builder");
      final Method buildMethod = builderClass.getDeclaredMethod("build");
      assertNotNull(buildMethod, "build method should exist");
      assertTrue(Modifier.isPublic(buildMethod.getModifiers()), "build should be public");
      assertEquals(
          WasiPermissionManager.class,
          buildMethod.getReturnType(),
          "build should return WasiPermissionManager");
    }

    @Test
    @DisplayName("Builder should have configuration methods")
    void builderShouldHaveConfigurationMethods() throws Exception {
      final Class<?> builderClass =
          Class.forName(WasiPermissionManager.class.getName() + "$Builder");

      // Check for actual builder methods
      final Set<String> expectedMethods = new HashSet<>();
      expectedMethods.add("withPathPermission");
      expectedMethods.add("withPathPermissions");
      expectedMethods.add("withGlobalPermission");
      expectedMethods.add("withEnvironmentVariable");
      expectedMethods.add("withResourceLimits");
      expectedMethods.add("withDangerousOperations");
      expectedMethods.add("withStrictPathValidation");
      expectedMethods.add("build");

      final Method[] methods = builderClass.getDeclaredMethods();
      final Set<String> methodNames = new HashSet<>();
      for (final Method method : methods) {
        methodNames.add(method.getName());
      }

      for (final String expected : expectedMethods) {
        assertTrue(methodNames.contains(expected), "Builder should have " + expected + " method");
      }
    }

    @Test
    @DisplayName("Should have static factory methods")
    void shouldHaveStaticFactoryMethods() throws Exception {
      // defaultManager factory
      final Method defaultMethod = WasiPermissionManager.class.getDeclaredMethod("defaultManager");
      assertNotNull(defaultMethod, "defaultManager method should exist");
      assertTrue(Modifier.isPublic(defaultMethod.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(defaultMethod.getModifiers()), "Should be static");
      assertEquals(
          WasiPermissionManager.class,
          defaultMethod.getReturnType(),
          "Should return WasiPermissionManager");

      // restrictiveManager factory
      final Method restrictiveMethod =
          WasiPermissionManager.class.getDeclaredMethod("restrictiveManager");
      assertNotNull(restrictiveMethod, "restrictiveManager method should exist");
      assertTrue(Modifier.isStatic(restrictiveMethod.getModifiers()), "Should be static");

      // permissiveManager factory
      final Method permissiveMethod =
          WasiPermissionManager.class.getDeclaredMethod("permissiveManager");
      assertNotNull(permissiveMethod, "permissiveManager method should exist");
      assertTrue(Modifier.isStatic(permissiveMethod.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("Should have static builder method")
    void shouldHaveStaticBuilderMethod() throws Exception {
      final Method builderMethod = WasiPermissionManager.class.getDeclaredMethod("builder");
      assertNotNull(builderMethod, "builder method should exist");
      assertTrue(Modifier.isPublic(builderMethod.getModifiers()), "builder should be public");
      assertTrue(Modifier.isStatic(builderMethod.getModifiers()), "builder should be static");

      // Return type should be the Builder class
      final Class<?> builderClass =
          Class.forName(WasiPermissionManager.class.getName() + "$Builder");
      assertEquals(builderClass, builderMethod.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Should have permission validation methods")
    void shouldHavePermissionValidationMethods() throws Exception {
      // validateFileSystemAccess(Path)
      final Method validateFs1 =
          WasiPermissionManager.class.getDeclaredMethod("validateFileSystemAccess", Path.class);
      assertNotNull(validateFs1, "validateFileSystemAccess(Path) method should exist");
      assertTrue(Modifier.isPublic(validateFs1.getModifiers()), "Should be public");
      assertEquals(void.class, validateFs1.getReturnType(), "Should return void");

      // validateFileSystemAccess(Path, WasiFileOperation)
      final Method validateFs2 =
          WasiPermissionManager.class.getDeclaredMethod(
              "validateFileSystemAccess", Path.class, WasiFileOperation.class);
      assertNotNull(
          validateFs2, "validateFileSystemAccess(Path, WasiFileOperation) method should exist");
      assertTrue(Modifier.isPublic(validateFs2.getModifiers()), "Should be public");
      assertEquals(void.class, validateFs2.getReturnType(), "Should return void");

      // validateEnvironmentAccess
      final Method validateEnv =
          WasiPermissionManager.class.getDeclaredMethod("validateEnvironmentAccess", String.class);
      assertNotNull(validateEnv, "validateEnvironmentAccess method should exist");
      assertTrue(Modifier.isPublic(validateEnv.getModifiers()), "Should be public");
      assertEquals(void.class, validateEnv.getReturnType(), "Should return void");

      // areDangerousOperationsAllowed
      final Method isDangerousAllowed =
          WasiPermissionManager.class.getDeclaredMethod("areDangerousOperationsAllowed");
      assertNotNull(isDangerousAllowed, "areDangerousOperationsAllowed method should exist");
      assertTrue(Modifier.isPublic(isDangerousAllowed.getModifiers()), "Should be public");
      assertEquals(boolean.class, isDangerousAllowed.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have resource limits field")
    void shouldHaveResourceLimitsField() throws Exception {
      final Field resourceLimitsField =
          WasiPermissionManager.class.getDeclaredField("resourceLimits");
      assertNotNull(resourceLimitsField, "resourceLimits field should exist");
      assertTrue(Modifier.isPrivate(resourceLimitsField.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(resourceLimitsField.getModifiers()), "Should be final");
      assertEquals(
          WasiResourceLimits.class,
          resourceLimitsField.getType(),
          "Should be WasiResourceLimits type");

      // Check allowDangerousOperations field
      final Field dangerousField =
          WasiPermissionManager.class.getDeclaredField("allowDangerousOperations");
      assertNotNull(dangerousField, "allowDangerousOperations field should exist");
      assertTrue(Modifier.isPrivate(dangerousField.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(dangerousField.getModifiers()), "Should be final");
      assertEquals(boolean.class, dangerousField.getType(), "Should be boolean type");

      // Check strictPathValidation field
      final Field strictField =
          WasiPermissionManager.class.getDeclaredField("strictPathValidation");
      assertNotNull(strictField, "strictPathValidation field should exist");
      assertTrue(Modifier.isPrivate(strictField.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(strictField.getModifiers()), "Should be final");
      assertEquals(boolean.class, strictField.getType(), "Should be boolean type");
    }
  }

  // =========================================================================
  // WasiProcessOperations Tests
  // =========================================================================

  @Nested
  @DisplayName("WasiProcessOperations Class Tests")
  class WasiProcessOperationsClassTests {

    @Test
    @DisplayName("WasiProcessOperations should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiProcessOperations.class.getModifiers()),
          "WasiProcessOperations should be final");
    }

    @Test
    @DisplayName("Should have required constructor")
    void shouldHaveRequiredConstructor() throws Exception {
      final Constructor<?> constructor =
          WasiProcessOperations.class.getDeclaredConstructor(WasiContext.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("Should have LOGGER field")
    void shouldHaveLoggerField() throws Exception {
      final Field loggerField = WasiProcessOperations.class.getDeclaredField("LOGGER");
      assertNotNull(loggerField, "LOGGER field should exist");
      assertTrue(Modifier.isPrivate(loggerField.getModifiers()), "LOGGER should be private");
      assertTrue(Modifier.isStatic(loggerField.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(loggerField.getModifiers()), "LOGGER should be final");
    }

    @Test
    @DisplayName("Should have MAX constants")
    void shouldHaveMaxConstants() throws Exception {
      // MAX_CHILD_PROCESSES
      final Field maxProcesses =
          WasiProcessOperations.class.getDeclaredField("MAX_CHILD_PROCESSES");
      assertNotNull(maxProcesses, "MAX_CHILD_PROCESSES field should exist");
      assertTrue(Modifier.isPrivate(maxProcesses.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(maxProcesses.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(maxProcesses.getModifiers()), "Should be final");
      assertEquals(int.class, maxProcesses.getType(), "Should be int type");

      // MAX_WAIT_TIME_SECONDS
      final Field maxWait = WasiProcessOperations.class.getDeclaredField("MAX_WAIT_TIME_SECONDS");
      assertNotNull(maxWait, "MAX_WAIT_TIME_SECONDS field should exist");
      assertTrue(Modifier.isPrivate(maxWait.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(maxWait.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(maxWait.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have process tracking fields")
    void shouldHaveProcessTrackingFields() throws Exception {
      // processHandleGenerator
      final Field handleGen =
          WasiProcessOperations.class.getDeclaredField("processHandleGenerator");
      assertNotNull(handleGen, "processHandleGenerator field should exist");
      assertTrue(Modifier.isPrivate(handleGen.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(handleGen.getModifiers()), "Should be final");

      // childProcesses
      final Field childProcesses = WasiProcessOperations.class.getDeclaredField("childProcesses");
      assertNotNull(childProcesses, "childProcesses field should exist");
      assertTrue(Modifier.isPrivate(childProcesses.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(childProcesses.getModifiers()), "Should be final");
      assertEquals(Map.class, childProcesses.getType(), "Should be Map type");
    }

    @Test
    @DisplayName("Should have getCurrentProcessId method")
    void shouldHaveGetCurrentProcessIdMethod() throws Exception {
      final Method method = WasiProcessOperations.class.getDeclaredMethod("getCurrentProcessId");
      assertNotNull(method, "getCurrentProcessId method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("Should have spawnProcess method")
    void shouldHaveSpawnProcessMethod() throws Exception {
      final Method method =
          WasiProcessOperations.class.getDeclaredMethod(
              "spawnProcess", String.class, List.class, Map.class, String.class);
      assertNotNull(method, "spawnProcess method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("Should have waitForProcess method")
    void shouldHaveWaitForProcessMethod() throws Exception {
      final Method method =
          WasiProcessOperations.class.getDeclaredMethod("waitForProcess", long.class, int.class);
      assertNotNull(method, "waitForProcess method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("Should have terminateProcess method")
    void shouldHaveTerminateProcessMethod() throws Exception {
      final Method method =
          WasiProcessOperations.class.getDeclaredMethod("terminateProcess", long.class, int.class);
      assertNotNull(method, "terminateProcess method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have environment variable methods")
    void shouldHaveEnvironmentVariableMethods() throws Exception {
      // getEnvironmentVariable
      final Method getEnv =
          WasiProcessOperations.class.getDeclaredMethod("getEnvironmentVariable", String.class);
      assertNotNull(getEnv, "getEnvironmentVariable method should exist");
      assertTrue(Modifier.isPublic(getEnv.getModifiers()), "Should be public");
      assertEquals(String.class, getEnv.getReturnType(), "Should return String");

      // setEnvironmentVariable
      final Method setEnv =
          WasiProcessOperations.class.getDeclaredMethod(
              "setEnvironmentVariable", String.class, String.class);
      assertNotNull(setEnv, "setEnvironmentVariable method should exist");
      assertTrue(Modifier.isPublic(setEnv.getModifiers()), "Should be public");
      assertEquals(void.class, setEnv.getReturnType(), "Should return void");

      // getAllEnvironmentVariables
      final Method getAllEnv =
          WasiProcessOperations.class.getDeclaredMethod("getAllEnvironmentVariables");
      assertNotNull(getAllEnv, "getAllEnvironmentVariables method should exist");
      assertTrue(Modifier.isPublic(getAllEnv.getModifiers()), "Should be public");
      assertEquals(Map.class, getAllEnv.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("Should have raiseSignal method")
    void shouldHaveRaiseSignalMethod() throws Exception {
      final Method method = WasiProcessOperations.class.getDeclaredMethod("raiseSignal", int.class);
      assertNotNull(method, "raiseSignal method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have process info methods")
    void shouldHaveProcessInfoMethods() throws Exception {
      // getProcessInfo
      final Method getInfo =
          WasiProcessOperations.class.getDeclaredMethod("getProcessInfo", long.class);
      assertNotNull(getInfo, "getProcessInfo method should exist");
      assertTrue(Modifier.isPublic(getInfo.getModifiers()), "Should be public");

      // getAllChildProcesses
      final Method getAll = WasiProcessOperations.class.getDeclaredMethod("getAllChildProcesses");
      assertNotNull(getAll, "getAllChildProcesses method should exist");
      assertTrue(Modifier.isPublic(getAll.getModifiers()), "Should be public");
      assertEquals(List.class, getAll.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("Should have ProcessInfo inner class")
    void shouldHaveProcessInfoClass() throws Exception {
      final Class<?> infoClass =
          Class.forName(WasiProcessOperations.class.getName() + "$ProcessInfo");
      assertNotNull(infoClass, "ProcessInfo inner class should exist");
      assertTrue(Modifier.isPublic(infoClass.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(infoClass.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(infoClass.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("ProcessInfo should have required fields")
    void processInfoShouldHaveRequiredFields() throws Exception {
      final Class<?> infoClass =
          Class.forName(WasiProcessOperations.class.getName() + "$ProcessInfo");

      // Check required fields
      final String[] requiredFields = {
        "handle", "process", "command", "arguments", "environment",
        "workingDirectory", "startTime", "finished", "terminated", "exitCode"
      };

      for (final String fieldName : requiredFields) {
        final Field field = infoClass.getDeclaredField(fieldName);
        assertNotNull(field, fieldName + " field should exist");
      }
    }

    @Test
    @DisplayName("ProcessInfo should have required methods")
    void processInfoShouldHaveRequiredMethods() throws Exception {
      final Class<?> infoClass =
          Class.forName(WasiProcessOperations.class.getName() + "$ProcessInfo");

      // isAlive method
      final Method isAlive = infoClass.getDeclaredMethod("isAlive");
      assertNotNull(isAlive, "isAlive method should exist");
      assertEquals(boolean.class, isAlive.getReturnType(), "Should return boolean");

      // getPid method
      final Method getPid = infoClass.getDeclaredMethod("getPid");
      assertNotNull(getPid, "getPid method should exist");
      assertEquals(long.class, getPid.getReturnType(), "Should return long");

      // toString method
      final Method toString = infoClass.getDeclaredMethod("toString");
      assertNotNull(toString, "toString method should exist");
      assertEquals(String.class, toString.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("Should have close method")
    void shouldHaveCloseMethod() throws Exception {
      final Method method = WasiProcessOperations.class.getDeclaredMethod("close");
      assertNotNull(method, "close method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // =========================================================================
  // Cross-Cutting Tests
  // =========================================================================

  @Nested
  @DisplayName("Cross-Cutting WASI Operations Tests")
  class CrossCuttingWasiOperationsTests {

    @Test
    @DisplayName("All WASI operation classes should be final")
    void allClassesShouldBeFinal() {
      assertTrue(
          Modifier.isFinal(WasiAdvancedFileOperations.class.getModifiers()),
          "WasiAdvancedFileOperations should be final");
      assertTrue(
          Modifier.isFinal(WasiPermissionManager.class.getModifiers()),
          "WasiPermissionManager should be final");
      assertTrue(
          Modifier.isFinal(WasiProcessOperations.class.getModifiers()),
          "WasiProcessOperations should be final");
    }

    @Test
    @DisplayName("All WASI operation classes should be public")
    void allClassesShouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiAdvancedFileOperations.class.getModifiers()),
          "WasiAdvancedFileOperations should be public");
      assertTrue(
          Modifier.isPublic(WasiPermissionManager.class.getModifiers()),
          "WasiPermissionManager should be public");
      assertTrue(
          Modifier.isPublic(WasiProcessOperations.class.getModifiers()),
          "WasiProcessOperations should be public");
    }

    @Test
    @DisplayName("All main classes should have no abstract methods")
    void allClassesShouldHaveNoAbstractMethods() {
      final Class<?>[] classes = {
        WasiAdvancedFileOperations.class, WasiPermissionManager.class, WasiProcessOperations.class
      };

      for (final Class<?> clazz : classes) {
        assertFalse(
            Modifier.isAbstract(clazz.getModifiers()),
            clazz.getSimpleName() + " should not be abstract");

        for (final Method method : clazz.getDeclaredMethods()) {
          assertFalse(
              Modifier.isAbstract(method.getModifiers()),
              clazz.getSimpleName() + "." + method.getName() + " should not be abstract");
        }
      }
    }

    @Test
    @DisplayName("All main classes should be in expected packages")
    void allClassesShouldBeInExpectedPackages() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.wasi",
          WasiAdvancedFileOperations.class.getPackage().getName(),
          "WasiAdvancedFileOperations should be in wasi package");
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.wasi.permission",
          WasiPermissionManager.class.getPackage().getName(),
          "WasiPermissionManager should be in wasi.permission package");
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.wasi",
          WasiProcessOperations.class.getPackage().getName(),
          "WasiProcessOperations should be in wasi package");
    }
  }
}
