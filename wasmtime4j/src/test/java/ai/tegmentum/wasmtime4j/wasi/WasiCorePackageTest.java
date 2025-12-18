/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 */

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for the WASI Core Package (ai.tegmentum.wasmtime4j.wasi).
 *
 * <p>This test class verifies the API structure, method signatures, and contracts for all classes
 * and interfaces in the WASI root package.
 */
@DisplayName("WASI Core Package Tests")
class WasiCorePackageTest {

  // ========================================================================
  // WasiProcessId Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiProcessId Tests")
  class WasiProcessIdTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiProcessId.class.getModifiers()),
          "WasiProcessId should be a final class");
    }

    @Test
    @DisplayName("should have static of factory method")
    void shouldHaveOfFactoryMethod() throws NoSuchMethodException {
      Method method = WasiProcessId.class.getMethod("of", long.class);
      assertNotNull(method, "of method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(
          WasiProcessId.class, method.getReturnType(), "Return type should be WasiProcessId");
    }

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = WasiProcessId.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should create process id with valid value")
    void shouldCreateProcessIdWithValidValue() {
      WasiProcessId pid = WasiProcessId.of(12345L);
      assertEquals(12345L, pid.getId(), "Process ID should match");
    }

    @Test
    @DisplayName("should throw exception for negative process id")
    void shouldThrowExceptionForNegativeProcessId() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiProcessId.of(-1L),
          "Should throw exception for negative ID");
    }

    @Test
    @DisplayName("should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      WasiProcessId pid1 = WasiProcessId.of(100L);
      WasiProcessId pid2 = WasiProcessId.of(100L);
      WasiProcessId pid3 = WasiProcessId.of(200L);

      assertEquals(pid1, pid2, "Equal IDs should be equal");
      assertNotEquals(pid1, pid3, "Different IDs should not be equal");
    }

    @Test
    @DisplayName("should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      WasiProcessId pid1 = WasiProcessId.of(100L);
      WasiProcessId pid2 = WasiProcessId.of(100L);

      assertEquals(pid1.hashCode(), pid2.hashCode(), "Equal objects should have equal hashcodes");
    }

    @Test
    @DisplayName("should implement toString")
    void shouldImplementToString() {
      WasiProcessId pid = WasiProcessId.of(12345L);
      String str = pid.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("12345"), "toString should contain the ID value");
    }
  }

  // ========================================================================
  // WasiFileType Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiFileType Tests")
  class WasiFileTypeTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiFileType.class.isEnum(), "WasiFileType should be an enum");
    }

    @Test
    @DisplayName("should have all expected enum values")
    void shouldHaveAllExpectedEnumValues() {
      WasiFileType[] values = WasiFileType.values();
      assertEquals(8, values.length, "Should have 8 file type values");

      // Verify specific values exist
      assertNotNull(WasiFileType.valueOf("UNKNOWN"));
      assertNotNull(WasiFileType.valueOf("BLOCK_DEVICE"));
      assertNotNull(WasiFileType.valueOf("CHARACTER_DEVICE"));
      assertNotNull(WasiFileType.valueOf("DIRECTORY"));
      assertNotNull(WasiFileType.valueOf("REGULAR_FILE"));
      assertNotNull(WasiFileType.valueOf("SOCKET_STREAM"));
      assertNotNull(WasiFileType.valueOf("SOCKET_DGRAM"));
      assertNotNull(WasiFileType.valueOf("SYMBOLIC_LINK"));
    }

    @Test
    @DisplayName("should have getValue method")
    void shouldHaveGetValueMethod() throws NoSuchMethodException {
      Method method = WasiFileType.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have fromValue static method")
    void shouldHaveFromValueMethod() throws NoSuchMethodException {
      Method method = WasiFileType.class.getMethod("fromValue", int.class);
      assertNotNull(method, "fromValue method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromValue should be static");
      assertEquals(
          WasiFileType.class, method.getReturnType(), "Return type should be WasiFileType");
    }

    @Test
    @DisplayName("should convert from value correctly")
    void shouldConvertFromValueCorrectly() {
      assertEquals(WasiFileType.UNKNOWN, WasiFileType.fromValue(0));
      assertEquals(WasiFileType.BLOCK_DEVICE, WasiFileType.fromValue(1));
      assertEquals(WasiFileType.DIRECTORY, WasiFileType.fromValue(3));
      assertEquals(WasiFileType.REGULAR_FILE, WasiFileType.fromValue(4));
    }

    @Test
    @DisplayName("should throw exception for invalid value")
    void shouldThrowExceptionForInvalidValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiFileType.fromValue(99),
          "Should throw exception for invalid value");
    }

    @Test
    @DisplayName("should have isRegularFile method")
    void shouldHaveIsRegularFileMethod() {
      assertTrue(
          WasiFileType.REGULAR_FILE.isRegularFile(),
          "REGULAR_FILE should return true for isRegularFile");
      assertFalse(
          WasiFileType.DIRECTORY.isRegularFile(),
          "DIRECTORY should return false for isRegularFile");
    }

    @Test
    @DisplayName("should have isDirectory method")
    void shouldHaveIsDirectoryMethod() {
      assertTrue(
          WasiFileType.DIRECTORY.isDirectory(), "DIRECTORY should return true for isDirectory");
      assertFalse(
          WasiFileType.REGULAR_FILE.isDirectory(),
          "REGULAR_FILE should return false for isDirectory");
    }

    @Test
    @DisplayName("should have isSymbolicLink method")
    void shouldHaveIsSymbolicLinkMethod() {
      assertTrue(
          WasiFileType.SYMBOLIC_LINK.isSymbolicLink(),
          "SYMBOLIC_LINK should return true for isSymbolicLink");
      assertFalse(
          WasiFileType.REGULAR_FILE.isSymbolicLink(),
          "REGULAR_FILE should return false for isSymbolicLink");
    }

    @Test
    @DisplayName("should have isDevice method")
    void shouldHaveIsDeviceMethod() {
      assertTrue(
          WasiFileType.BLOCK_DEVICE.isDevice(), "BLOCK_DEVICE should return true for isDevice");
      assertTrue(
          WasiFileType.CHARACTER_DEVICE.isDevice(),
          "CHARACTER_DEVICE should return true for isDevice");
      assertFalse(
          WasiFileType.REGULAR_FILE.isDevice(), "REGULAR_FILE should return false for isDevice");
    }

    @Test
    @DisplayName("should have isSocket method")
    void shouldHaveIsSocketMethod() {
      assertTrue(
          WasiFileType.SOCKET_STREAM.isSocket(), "SOCKET_STREAM should return true for isSocket");
      assertTrue(
          WasiFileType.SOCKET_DGRAM.isSocket(), "SOCKET_DGRAM should return true for isSocket");
      assertFalse(
          WasiFileType.REGULAR_FILE.isSocket(), "REGULAR_FILE should return false for isSocket");
    }

    @Test
    @DisplayName("should have isSpecialFile method")
    void shouldHaveIsSpecialFileMethod() {
      assertTrue(WasiFileType.BLOCK_DEVICE.isSpecialFile(), "BLOCK_DEVICE should be special file");
      assertTrue(
          WasiFileType.SOCKET_STREAM.isSpecialFile(), "SOCKET_STREAM should be special file");
      assertFalse(
          WasiFileType.REGULAR_FILE.isSpecialFile(), "REGULAR_FILE should not be special file");
    }
  }

  // ========================================================================
  // WasiVersion Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiVersion Tests")
  class WasiVersionTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiVersion.class.isEnum(), "WasiVersion should be an enum");
    }

    @Test
    @DisplayName("should have PREVIEW_1 and PREVIEW_2 values")
    void shouldHavePreviewValues() {
      assertNotNull(WasiVersion.valueOf("PREVIEW_1"), "PREVIEW_1 should exist");
      assertNotNull(WasiVersion.valueOf("PREVIEW_2"), "PREVIEW_2 should exist");
      assertEquals(2, WasiVersion.values().length, "Should have 2 version values");
    }

    @Test
    @DisplayName("should have getVersion method")
    void shouldHaveGetVersionMethod() throws NoSuchMethodException {
      Method method = WasiVersion.class.getMethod("getVersion");
      assertNotNull(method, "getVersion method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getImportNamespace method")
    void shouldHaveGetImportNamespaceMethod() throws NoSuchMethodException {
      Method method = WasiVersion.class.getMethod("getImportNamespace");
      assertNotNull(method, "getImportNamespace method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should return correct version strings")
    void shouldReturnCorrectVersionStrings() {
      assertEquals("0.1.0", WasiVersion.PREVIEW_1.getVersion());
      assertEquals("0.2.0", WasiVersion.PREVIEW_2.getVersion());
    }

    @Test
    @DisplayName("should return correct import namespaces")
    void shouldReturnCorrectImportNamespaces() {
      assertEquals("wasi_unstable", WasiVersion.PREVIEW_1.getImportNamespace());
      assertEquals("wasi", WasiVersion.PREVIEW_2.getImportNamespace());
    }

    @Test
    @DisplayName("should have supportsAsyncOperations method")
    void shouldHaveSupportsAsyncOperationsMethod() {
      assertFalse(
          WasiVersion.PREVIEW_1.supportsAsyncOperations(), "PREVIEW_1 should not support async");
      assertTrue(WasiVersion.PREVIEW_2.supportsAsyncOperations(), "PREVIEW_2 should support async");
    }

    @Test
    @DisplayName("should have supportsComponentModel method")
    void shouldHaveSupportsComponentModelMethod() {
      assertFalse(
          WasiVersion.PREVIEW_1.supportsComponentModel(),
          "PREVIEW_1 should not support component model");
      assertTrue(
          WasiVersion.PREVIEW_2.supportsComponentModel(),
          "PREVIEW_2 should support component model");
    }

    @Test
    @DisplayName("should have supportsWitInterfaces method")
    void shouldHaveSupportsWitInterfacesMethod() {
      assertFalse(
          WasiVersion.PREVIEW_1.supportsWitInterfaces(), "PREVIEW_1 should not support WIT");
      assertTrue(WasiVersion.PREVIEW_2.supportsWitInterfaces(), "PREVIEW_2 should support WIT");
    }

    @Test
    @DisplayName("should have getDefault static method")
    void shouldHaveGetDefaultMethod() {
      assertEquals(WasiVersion.PREVIEW_1, WasiVersion.getDefault(), "Default should be PREVIEW_1");
    }

    @Test
    @DisplayName("should have getLatest static method")
    void shouldHaveGetLatestMethod() {
      assertEquals(WasiVersion.PREVIEW_2, WasiVersion.getLatest(), "Latest should be PREVIEW_2");
    }

    @Test
    @DisplayName("should have fromVersionString static method")
    void shouldHaveFromVersionStringMethod() {
      assertEquals(WasiVersion.PREVIEW_1, WasiVersion.fromVersionString("0.1.0"));
      assertEquals(WasiVersion.PREVIEW_2, WasiVersion.fromVersionString("0.2.0"));
    }

    @Test
    @DisplayName("should throw exception for invalid version string")
    void shouldThrowExceptionForInvalidVersionString() {
      assertThrows(IllegalArgumentException.class, () -> WasiVersion.fromVersionString("9.9.9"));
      assertThrows(IllegalArgumentException.class, () -> WasiVersion.fromVersionString(null));
      assertThrows(IllegalArgumentException.class, () -> WasiVersion.fromVersionString(""));
    }

    @Test
    @DisplayName("should have isCompatibleWith method")
    void shouldHaveIsCompatibleWithMethod() {
      assertTrue(
          WasiVersion.PREVIEW_1.isCompatibleWith(WasiVersion.PREVIEW_1),
          "PREVIEW_1 should be compatible with itself");
      assertFalse(
          WasiVersion.PREVIEW_1.isCompatibleWith(WasiVersion.PREVIEW_2),
          "PREVIEW_1 should not be compatible with PREVIEW_2");
    }
  }

  // ========================================================================
  // WasiRuntimeType Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiRuntimeType Tests")
  class WasiRuntimeTypeTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiRuntimeType.class.isEnum(), "WasiRuntimeType should be an enum");
    }

    @Test
    @DisplayName("should have JNI and PANAMA values")
    void shouldHaveJniAndPanamaValues() {
      assertNotNull(WasiRuntimeType.valueOf("JNI"), "JNI should exist");
      assertNotNull(WasiRuntimeType.valueOf("PANAMA"), "PANAMA should exist");
      assertEquals(2, WasiRuntimeType.values().length, "Should have 2 runtime type values");
    }
  }

  // ========================================================================
  // WasiInstanceState Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiInstanceState Tests")
  class WasiInstanceStateTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiInstanceState.class.isEnum(), "WasiInstanceState should be an enum");
    }

    @Test
    @DisplayName("should have isActive method")
    void shouldHaveIsActiveMethod() throws NoSuchMethodException {
      Method method = WasiInstanceState.class.getMethod("isActive");
      assertNotNull(method, "isActive method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isTerminal method")
    void shouldHaveIsTerminalMethod() throws NoSuchMethodException {
      Method method = WasiInstanceState.class.getMethod("isTerminal");
      assertNotNull(method, "isTerminal method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isCallable method")
    void shouldHaveIsCallableMethod() throws NoSuchMethodException {
      Method method = WasiInstanceState.class.getMethod("isCallable");
      assertNotNull(method, "isCallable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getDescription method")
    void shouldHaveGetDescriptionMethod() throws NoSuchMethodException {
      Method method = WasiInstanceState.class.getMethod("getDescription");
      assertNotNull(method, "getDescription method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }
  }

  // ========================================================================
  // WasiResourceState Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiResourceState Tests")
  class WasiResourceStateTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiResourceState.class.isEnum(), "WasiResourceState should be an enum");
    }

    @Test
    @DisplayName("should have isUsable method")
    void shouldHaveIsUsableMethod() throws NoSuchMethodException {
      Method method = WasiResourceState.class.getMethod("isUsable");
      assertNotNull(method, "isUsable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isTerminal method")
    void shouldHaveIsTerminalMethod() throws NoSuchMethodException {
      Method method = WasiResourceState.class.getMethod("isTerminal");
      assertNotNull(method, "isTerminal method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }
  }

  // ========================================================================
  // WasiOpenFlags Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiOpenFlags Tests")
  class WasiOpenFlagsTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiOpenFlags.class.isEnum(), "WasiOpenFlags should be an enum");
    }

    @Test
    @DisplayName("should have getValue method")
    void shouldHaveGetValueMethod() throws NoSuchMethodException {
      Method method = WasiOpenFlags.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have combine method returning WasiOpenFlagsSet")
    void shouldHaveCombineMethod() throws NoSuchMethodException {
      Method method = WasiOpenFlags.class.getMethod("combine", WasiOpenFlags.class);
      assertNotNull(method, "combine method should exist");
      assertFalse(Modifier.isStatic(method.getModifiers()), "combine should be instance method");
      assertEquals(
          WasiOpenFlags.WasiOpenFlagsSet.class,
          method.getReturnType(),
          "Return type should be WasiOpenFlagsSet");
    }

    @Test
    @DisplayName("should have of static method taking varargs")
    void shouldHaveOfMethod() throws NoSuchMethodException {
      Method method = WasiOpenFlags.class.getMethod("of", WasiOpenFlags[].class);
      assertNotNull(method, "of method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(
          WasiOpenFlags.WasiOpenFlagsSet.class,
          method.getReturnType(),
          "Return type should be WasiOpenFlagsSet");
    }

    @Test
    @DisplayName("should have WasiOpenFlagsSet inner class")
    void shouldHaveWasiOpenFlagsSetInnerClass() {
      Class<?> innerClass = WasiOpenFlags.WasiOpenFlagsSet.class;
      assertNotNull(innerClass, "WasiOpenFlagsSet inner class should exist");
      assertTrue(Modifier.isStatic(innerClass.getModifiers()), "WasiOpenFlagsSet should be static");
    }
  }

  // ========================================================================
  // WasiRights Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiRights Tests")
  class WasiRightsTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiRights.class.isEnum(), "WasiRights should be an enum");
    }

    @Test
    @DisplayName("should have getValue method")
    void shouldHaveGetValueMethod() throws NoSuchMethodException {
      Method method = WasiRights.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have combine method returning WasiRightsSet")
    void shouldHaveCombineMethod() throws NoSuchMethodException {
      Method method = WasiRights.class.getMethod("combine", WasiRights.class);
      assertNotNull(method, "combine method should exist");
      assertFalse(Modifier.isStatic(method.getModifiers()), "combine should be instance method");
      assertEquals(
          WasiRights.WasiRightsSet.class,
          method.getReturnType(),
          "Return type should be WasiRightsSet");
    }

    @Test
    @DisplayName("should have of static method taking varargs")
    void shouldHaveOfMethod() throws NoSuchMethodException {
      Method method = WasiRights.class.getMethod("of", WasiRights[].class);
      assertNotNull(method, "of method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(
          WasiRights.WasiRightsSet.class,
          method.getReturnType(),
          "Return type should be WasiRightsSet");
    }

    @Test
    @DisplayName("should have WasiRightsSet inner class")
    void shouldHaveWasiRightsSetInnerClass() {
      Class<?> innerClass = WasiRights.WasiRightsSet.class;
      assertNotNull(innerClass, "WasiRightsSet inner class should exist");
      assertTrue(Modifier.isStatic(innerClass.getModifiers()), "WasiRightsSet should be static");
    }
  }

  // ========================================================================
  // WasiSignal Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiSignal Tests")
  class WasiSignalTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiSignal.class.isEnum(), "WasiSignal should be an enum");
    }

    @Test
    @DisplayName("should have getCode method")
    void shouldHaveGetCodeMethod() throws NoSuchMethodException {
      Method method = WasiSignal.class.getMethod("getCode");
      assertNotNull(method, "getCode method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have fromCode static method")
    void shouldHaveFromCodeMethod() throws NoSuchMethodException {
      Method method = WasiSignal.class.getMethod("fromCode", int.class);
      assertNotNull(method, "fromCode method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromCode should be static");
      assertEquals(WasiSignal.class, method.getReturnType(), "Return type should be WasiSignal");
    }
  }

  // ========================================================================
  // WasiFactory Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiFactory Tests")
  class WasiFactoryTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiFactory.class.getModifiers()),
          "WasiFactory should be a final class");
    }

    @Test
    @DisplayName("should have createContext no-arg method")
    void shouldHaveCreateContextNoArgMethod() throws NoSuchMethodException {
      Method method = WasiFactory.class.getMethod("createContext");
      assertNotNull(method, "createContext method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "createContext should be static");
      assertEquals(WasiContext.class, method.getReturnType(), "Return type should be WasiContext");
    }

    @Test
    @DisplayName("should have createContext with runtime type method")
    void shouldHaveCreateContextWithRuntimeTypeMethod() throws NoSuchMethodException {
      Method method = WasiFactory.class.getMethod("createContext", WasiRuntimeType.class);
      assertNotNull(method, "createContext method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "createContext should be static");
      assertEquals(WasiContext.class, method.getReturnType(), "Return type should be WasiContext");
    }

    @Test
    @DisplayName("should have getSelectedRuntimeType method")
    void shouldHaveGetSelectedRuntimeTypeMethod() throws NoSuchMethodException {
      Method method = WasiFactory.class.getMethod("getSelectedRuntimeType");
      assertNotNull(method, "getSelectedRuntimeType method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "getSelectedRuntimeType should be static");
      assertEquals(
          WasiRuntimeType.class, method.getReturnType(), "Return type should be WasiRuntimeType");
    }

    @Test
    @DisplayName("should have isRuntimeAvailable method")
    void shouldHaveIsRuntimeAvailableMethod() throws NoSuchMethodException {
      Method method = WasiFactory.class.getMethod("isRuntimeAvailable", WasiRuntimeType.class);
      assertNotNull(method, "isRuntimeAvailable method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "isRuntimeAvailable should be static");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getJavaVersion method")
    void shouldHaveGetJavaVersionMethod() throws NoSuchMethodException {
      Method method = WasiFactory.class.getMethod("getJavaVersion");
      assertNotNull(method, "getJavaVersion method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getJavaVersion should be static");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }
  }

  // ========================================================================
  // WasiRuntimeInfo Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiRuntimeInfo Tests")
  class WasiRuntimeInfoTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiRuntimeInfo.class.getModifiers()),
          "WasiRuntimeInfo should be a final class");
    }

    @Test
    @DisplayName("should have getRuntimeType method")
    void shouldHaveGetRuntimeTypeMethod() throws NoSuchMethodException {
      Method method = WasiRuntimeInfo.class.getMethod("getRuntimeType");
      assertNotNull(method, "getRuntimeType method should exist");
      assertEquals(
          WasiRuntimeType.class, method.getReturnType(), "Return type should be WasiRuntimeType");
    }

    @Test
    @DisplayName("should have getVersion method")
    void shouldHaveGetVersionMethod() throws NoSuchMethodException {
      Method method = WasiRuntimeInfo.class.getMethod("getVersion");
      assertNotNull(method, "getVersion method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getWasmtimeVersion method")
    void shouldHaveGetWasmtimeVersionMethod() throws NoSuchMethodException {
      Method method = WasiRuntimeInfo.class.getMethod("getWasmtimeVersion");
      assertNotNull(method, "getWasmtimeVersion method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }
  }

  // ========================================================================
  // WasiConfig Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiConfig Tests")
  class WasiConfigTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiConfig.class.isInterface(), "WasiConfig should be an interface");
    }

    @Test
    @DisplayName("should have builder static method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      Method method = WasiConfig.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(
          WasiConfigBuilder.class,
          method.getReturnType(),
          "Return type should be WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have defaultConfig static method")
    void shouldHaveDefaultConfigMethod() throws NoSuchMethodException {
      Method method = WasiConfig.class.getMethod("defaultConfig");
      assertNotNull(method, "defaultConfig method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "defaultConfig should be static");
      assertEquals(WasiConfig.class, method.getReturnType(), "Return type should be WasiConfig");
    }

    @Test
    @DisplayName("should have getEnvironment method")
    void shouldHaveGetEnvironmentMethod() throws NoSuchMethodException {
      Method method = WasiConfig.class.getMethod("getEnvironment");
      assertNotNull(method, "getEnvironment method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
    }

    @Test
    @DisplayName("should have getArguments method")
    void shouldHaveGetArgumentsMethod() throws NoSuchMethodException {
      Method method = WasiConfig.class.getMethod("getArguments");
      assertNotNull(method, "getArguments method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getPreopenDirectories method")
    void shouldHaveGetPreopenDirectoriesMethod() throws NoSuchMethodException {
      Method method = WasiConfig.class.getMethod("getPreopenDirectories");
      assertNotNull(method, "getPreopenDirectories method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
    }

    @Test
    @DisplayName("should have getWasiVersion method")
    void shouldHaveGetWasiVersionMethod() throws NoSuchMethodException {
      Method method = WasiConfig.class.getMethod("getWasiVersion");
      assertNotNull(method, "getWasiVersion method should exist");
      assertEquals(WasiVersion.class, method.getReturnType(), "Return type should be WasiVersion");
    }

    @Test
    @DisplayName("should have isValidationEnabled method")
    void shouldHaveIsValidationEnabledMethod() throws NoSuchMethodException {
      Method method = WasiConfig.class.getMethod("isValidationEnabled");
      assertNotNull(method, "isValidationEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isStrictModeEnabled method")
    void shouldHaveIsStrictModeEnabledMethod() throws NoSuchMethodException {
      Method method = WasiConfig.class.getMethod("isStrictModeEnabled");
      assertNotNull(method, "isStrictModeEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      Method method = WasiConfig.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // WasiContext Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiContext Tests")
  class WasiContextTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiContext.class.isInterface(), "WasiContext should be an interface");
    }

    @Test
    @DisplayName("should have createComponent method")
    void shouldHaveCreateComponentMethod() throws NoSuchMethodException {
      Method method = WasiContext.class.getMethod("createComponent", byte[].class);
      assertNotNull(method, "createComponent method should exist");
      assertEquals(
          WasiComponent.class, method.getReturnType(), "Return type should be WasiComponent");
    }

    @Test
    @DisplayName("should have getRuntimeInfo method")
    void shouldHaveGetRuntimeInfoMethod() throws NoSuchMethodException {
      Method method = WasiContext.class.getMethod("getRuntimeInfo");
      assertNotNull(method, "getRuntimeInfo method should exist");
      assertEquals(
          WasiRuntimeInfo.class, method.getReturnType(), "Return type should be WasiRuntimeInfo");
    }

    @Test
    @DisplayName("should have getFilesystem method")
    void shouldHaveGetFilesystemMethod() throws NoSuchMethodException {
      Method method = WasiContext.class.getMethod("getFilesystem");
      assertNotNull(method, "getFilesystem method should exist");
      assertEquals(
          WasiFilesystem.class, method.getReturnType(), "Return type should be WasiFilesystem");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = WasiContext.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = WasiContext.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // WasiLinker Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiLinker Tests")
  class WasiLinkerTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiLinker.class.isInterface(), "WasiLinker should be an interface");
    }

    @Test
    @DisplayName("should have allowDirectoryAccess method with permissions")
    void shouldHaveAllowDirectoryAccessWithPermissionsMethod() throws NoSuchMethodException {
      Method method =
          WasiLinker.class.getMethod(
              "allowDirectoryAccess", Path.class, String.class, WasiPermissions.class);
      assertNotNull(method, "allowDirectoryAccess method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have allowDirectoryAccess method without permissions")
    void shouldHaveAllowDirectoryAccessMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("allowDirectoryAccess", Path.class, String.class);
      assertNotNull(method, "allowDirectoryAccess method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have setEnvironmentVariable method")
    void shouldHaveSetEnvironmentVariableMethod() throws NoSuchMethodException {
      Method method =
          WasiLinker.class.getMethod("setEnvironmentVariable", String.class, String.class);
      assertNotNull(method, "setEnvironmentVariable method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have setArguments method")
    void shouldHaveSetArgumentsMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("setArguments", List.class);
      assertNotNull(method, "setArguments method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have configureStdin method")
    void shouldHaveConfigureStdinMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("configureStdin", WasiStdioConfig.class);
      assertNotNull(method, "configureStdin method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have configureStdout method")
    void shouldHaveConfigureStdoutMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("configureStdout", WasiStdioConfig.class);
      assertNotNull(method, "configureStdout method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have enableNetworkAccess method")
    void shouldHaveEnableNetworkAccessMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("enableNetworkAccess");
      assertNotNull(method, "enableNetworkAccess method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = WasiLinker.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have create static method")
    void shouldHaveCreateMethod() throws NoSuchMethodException {
      Method method =
          WasiLinker.class.getMethod(
              "create", ai.tegmentum.wasmtime4j.Engine.class, WasiConfig.class);
      assertNotNull(method, "create method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(WasiLinker.class, method.getReturnType(), "Return type should be WasiLinker");
    }
  }

  // ========================================================================
  // WasiFilesystem Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiFilesystem Tests")
  class WasiFilesystemTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiFilesystem.class.isInterface(), "WasiFilesystem should be an interface");
    }

    @Test
    @DisplayName("should have openFile method")
    void shouldHaveOpenFileMethod() throws NoSuchMethodException {
      Method method =
          WasiFilesystem.class.getMethod(
              "openFile", String.class, WasiOpenFlags.class, WasiRights.class);
      assertNotNull(method, "openFile method should exist");
      assertEquals(
          WasiFileHandle.class, method.getReturnType(), "Return type should be WasiFileHandle");
    }

    @Test
    @DisplayName("should have closeFile method")
    void shouldHaveCloseFileMethod() throws NoSuchMethodException {
      Method method = WasiFilesystem.class.getMethod("closeFile", WasiFileHandle.class);
      assertNotNull(method, "closeFile method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have readFile method")
    void shouldHaveReadFileMethod() throws NoSuchMethodException {
      Method method =
          WasiFilesystem.class.getMethod(
              "readFile", WasiFileHandle.class, ByteBuffer.class, long.class);
      assertNotNull(method, "readFile method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have writeFile method")
    void shouldHaveWriteFileMethod() throws NoSuchMethodException {
      Method method =
          WasiFilesystem.class.getMethod(
              "writeFile", WasiFileHandle.class, ByteBuffer.class, long.class);
      assertNotNull(method, "writeFile method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have openDirectory method")
    void shouldHaveOpenDirectoryMethod() throws NoSuchMethodException {
      Method method =
          WasiFilesystem.class.getMethod("openDirectory", String.class, WasiRights.class);
      assertNotNull(method, "openDirectory method should exist");
      assertEquals(
          WasiDirectoryHandle.class,
          method.getReturnType(),
          "Return type should be WasiDirectoryHandle");
    }

    @Test
    @DisplayName("should have createDirectory method")
    void shouldHaveCreateDirectoryMethod() throws NoSuchMethodException {
      Method method =
          WasiFilesystem.class.getMethod("createDirectory", String.class, WasiPermissions.class);
      assertNotNull(method, "createDirectory method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getFileStats method")
    void shouldHaveGetFileStatsMethod() throws NoSuchMethodException {
      Method method = WasiFilesystem.class.getMethod("getFileStats", String.class);
      assertNotNull(method, "getFileStats method should exist");
      assertEquals(
          WasiFileStats.class, method.getReturnType(), "Return type should be WasiFileStats");
    }

    @Test
    @DisplayName("should have removeDirectory method")
    void shouldHaveRemoveDirectoryMethod() throws NoSuchMethodException {
      Method method = WasiFilesystem.class.getMethod("removeDirectory", String.class);
      assertNotNull(method, "removeDirectory method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have readDirectory method")
    void shouldHaveReadDirectoryMethod() throws NoSuchMethodException {
      Method method = WasiFilesystem.class.getMethod("readDirectory", WasiDirectoryHandle.class);
      assertNotNull(method, "readDirectory method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getCurrentWorkingDirectory method")
    void shouldHaveGetCurrentWorkingDirectoryMethod() throws NoSuchMethodException {
      Method method = WasiFilesystem.class.getMethod("getCurrentWorkingDirectory");
      assertNotNull(method, "getCurrentWorkingDirectory method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have unlink method")
    void shouldHaveUnlinkMethod() throws NoSuchMethodException {
      Method method = WasiFilesystem.class.getMethod("unlink", String.class);
      assertNotNull(method, "unlink method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have rename method")
    void shouldHaveRenameMethod() throws NoSuchMethodException {
      Method method = WasiFilesystem.class.getMethod("rename", String.class, String.class);
      assertNotNull(method, "rename method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // WasiFileStats Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiFileStats Tests")
  class WasiFileStatsTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiFileStats.class.isInterface(), "WasiFileStats should be an interface");
    }

    @Test
    @DisplayName("should have getDevice method")
    void shouldHaveGetDeviceMethod() throws NoSuchMethodException {
      Method method = WasiFileStats.class.getMethod("getDevice");
      assertNotNull(method, "getDevice method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getInode method")
    void shouldHaveGetInodeMethod() throws NoSuchMethodException {
      Method method = WasiFileStats.class.getMethod("getInode");
      assertNotNull(method, "getInode method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getFileType method")
    void shouldHaveGetFileTypeMethod() throws NoSuchMethodException {
      Method method = WasiFileStats.class.getMethod("getFileType");
      assertNotNull(method, "getFileType method should exist");
      assertEquals(
          WasiFileType.class, method.getReturnType(), "Return type should be WasiFileType");
    }

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      Method method = WasiFileStats.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getAccessTime method returning Instant")
    void shouldHaveGetAccessTimeMethod() throws NoSuchMethodException {
      Method method = WasiFileStats.class.getMethod("getAccessTime");
      assertNotNull(method, "getAccessTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Return type should be Instant");
    }

    @Test
    @DisplayName("should have getModificationTime method returning Instant")
    void shouldHaveGetModificationTimeMethod() throws NoSuchMethodException {
      Method method = WasiFileStats.class.getMethod("getModificationTime");
      assertNotNull(method, "getModificationTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Return type should be Instant");
    }

    @Test
    @DisplayName("should have isFile method")
    void shouldHaveIsFileMethod() throws NoSuchMethodException {
      Method method = WasiFileStats.class.getMethod("isFile");
      assertNotNull(method, "isFile method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isDirectory method")
    void shouldHaveIsDirectoryMethod() throws NoSuchMethodException {
      Method method = WasiFileStats.class.getMethod("isDirectory");
      assertNotNull(method, "isDirectory method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }
  }

  // ========================================================================
  // WasiPermissions Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiPermissions Tests")
  class WasiPermissionsTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiPermissions.class.isInterface(), "WasiPermissions should be an interface");
    }

    @Test
    @DisplayName("should have getMode method")
    void shouldHaveGetModeMethod() throws NoSuchMethodException {
      Method method = WasiPermissions.class.getMethod("getMode");
      assertNotNull(method, "getMode method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have isOwnerRead method")
    void shouldHaveIsOwnerReadMethod() throws NoSuchMethodException {
      Method method = WasiPermissions.class.getMethod("isOwnerRead");
      assertNotNull(method, "isOwnerRead method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isOwnerWrite method")
    void shouldHaveIsOwnerWriteMethod() throws NoSuchMethodException {
      Method method = WasiPermissions.class.getMethod("isOwnerWrite");
      assertNotNull(method, "isOwnerWrite method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isOwnerExecute method")
    void shouldHaveIsOwnerExecuteMethod() throws NoSuchMethodException {
      Method method = WasiPermissions.class.getMethod("isOwnerExecute");
      assertNotNull(method, "isOwnerExecute method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have of static method")
    void shouldHaveOfMethod() throws NoSuchMethodException {
      Method method = WasiPermissions.class.getMethod("of", int.class);
      assertNotNull(method, "of method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(
          WasiPermissions.class, method.getReturnType(), "Return type should be WasiPermissions");
    }

    @Test
    @DisplayName("should have defaultFilePermissions static method")
    void shouldHaveDefaultFilePermissionsMethod() throws NoSuchMethodException {
      Method method = WasiPermissions.class.getMethod("defaultFilePermissions");
      assertNotNull(method, "defaultFilePermissions method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "defaultFilePermissions should be static");
    }

    @Test
    @DisplayName("should have defaultDirectoryPermissions static method")
    void shouldHaveDefaultDirectoryPermissionsMethod() throws NoSuchMethodException {
      Method method = WasiPermissions.class.getMethod("defaultDirectoryPermissions");
      assertNotNull(method, "defaultDirectoryPermissions method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "defaultDirectoryPermissions should be static");
    }
  }

  // ========================================================================
  // WasiResourceLimits Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiResourceLimits Tests")
  class WasiResourceLimitsTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasiResourceLimits.class.isInterface(), "WasiResourceLimits should be an interface");
    }

    @Test
    @DisplayName("should have builder static method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimits.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
    }

    @Test
    @DisplayName("should have defaultLimits static method")
    void shouldHaveDefaultLimitsMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimits.class.getMethod("defaultLimits");
      assertNotNull(method, "defaultLimits method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "defaultLimits should be static");
    }

    @Test
    @DisplayName("should have unlimited static method")
    void shouldHaveUnlimitedMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimits.class.getMethod("unlimited");
      assertNotNull(method, "unlimited method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "unlimited should be static");
    }

    @Test
    @DisplayName("should have getMemoryLimit method")
    void shouldHaveGetMemoryLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimits.class.getMethod("getMemoryLimit");
      assertNotNull(method, "getMemoryLimit method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have isUnlimited method")
    void shouldHaveIsUnlimitedMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimits.class.getMethod("isUnlimited");
      assertNotNull(method, "isUnlimited method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimits.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // WasiStdioConfig Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiStdioConfig Tests")
  class WasiStdioConfigTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiStdioConfig.class.getModifiers()),
          "WasiStdioConfig should be a final class");
    }

    @Test
    @DisplayName("should have inherit static method")
    void shouldHaveInheritMethod() throws NoSuchMethodException {
      Method method = WasiStdioConfig.class.getMethod("inherit");
      assertNotNull(method, "inherit method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "inherit should be static");
      assertEquals(
          WasiStdioConfig.class, method.getReturnType(), "Return type should be WasiStdioConfig");
    }

    @Test
    @DisplayName("should have nulled static method")
    void shouldHaveNulledMethod() throws NoSuchMethodException {
      Method method = WasiStdioConfig.class.getMethod("nulled");
      assertNotNull(method, "nulled method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nulled should be static");
      assertEquals(
          WasiStdioConfig.class, method.getReturnType(), "Return type should be WasiStdioConfig");
    }

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      Method method = WasiStdioConfig.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
    }
  }

  // ========================================================================
  // WasiInstance Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiInstance Tests")
  class WasiInstanceTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiInstance.class.isInterface(), "WasiInstance should be an interface");
    }

    @Test
    @DisplayName("should have getId method returning long")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getComponent method")
    void shouldHaveGetComponentMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("getComponent");
      assertNotNull(method, "getComponent method should exist");
      assertEquals(
          WasiComponent.class, method.getReturnType(), "Return type should be WasiComponent");
    }

    @Test
    @DisplayName("should have getState method")
    void shouldHaveGetStateMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("getState");
      assertNotNull(method, "getState method should exist");
      assertEquals(
          WasiInstanceState.class,
          method.getReturnType(),
          "Return type should be WasiInstanceState");
    }

    @Test
    @DisplayName("should have call method")
    void shouldHaveCallMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("call", String.class, Object[].class);
      assertNotNull(method, "call method should exist");
      assertEquals(Object.class, method.getReturnType(), "Return type should be Object");
    }

    @Test
    @DisplayName("should have callAsync method")
    void shouldHaveCallAsyncMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("callAsync", String.class, Object[].class);
      assertNotNull(method, "callAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
    }

    @Test
    @DisplayName("should have suspend method")
    void shouldHaveSuspendMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("suspend");
      assertNotNull(method, "suspend method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have resume method")
    void shouldHaveResumeMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("resume");
      assertNotNull(method, "resume method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have terminate method")
    void shouldHaveTerminateMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("terminate");
      assertNotNull(method, "terminate method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // WasiComponent Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiComponent Tests")
  class WasiComponentTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiComponent.class.isInterface(), "WasiComponent should be an interface");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      Method method = WasiComponent.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getExports method")
    void shouldHaveGetExportsMethod() throws NoSuchMethodException {
      Method method = WasiComponent.class.getMethod("getExports");
      assertNotNull(method, "getExports method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getImports method")
    void shouldHaveGetImportsMethod() throws NoSuchMethodException {
      Method method = WasiComponent.class.getMethod("getImports");
      assertNotNull(method, "getImports method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have instantiate method")
    void shouldHaveInstantiateMethod() throws NoSuchMethodException {
      Method method = WasiComponent.class.getMethod("instantiate", WasiConfig.class);
      assertNotNull(method, "instantiate method should exist");
      assertEquals(
          WasiInstance.class, method.getReturnType(), "Return type should be WasiInstance");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      Method method = WasiComponent.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = WasiComponent.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = WasiComponent.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // WasiSecurityPolicy Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiSecurityPolicy Tests")
  class WasiSecurityPolicyTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasiSecurityPolicy.class.isInterface(), "WasiSecurityPolicy should be an interface");
    }

    @Test
    @DisplayName("should have builder static method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      Method method = WasiSecurityPolicy.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
    }

    @Test
    @DisplayName("should have permissive static method")
    void shouldHavePermissiveMethod() throws NoSuchMethodException {
      Method method = WasiSecurityPolicy.class.getMethod("permissive");
      assertNotNull(method, "permissive method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "permissive should be static");
    }

    @Test
    @DisplayName("should have restrictive static method")
    void shouldHaveRestrictiveMethod() throws NoSuchMethodException {
      Method method = WasiSecurityPolicy.class.getMethod("restrictive");
      assertNotNull(method, "restrictive method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "restrictive should be static");
    }

    @Test
    @DisplayName("should have isFileSystemAccessAllowed method")
    void shouldHaveIsFileSystemAccessAllowedMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicy.class.getMethod("isFileSystemAccessAllowed", Path.class, String.class);
      assertNotNull(method, "isFileSystemAccessAllowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isNetworkAccessAllowed method")
    void shouldHaveIsNetworkAccessAllowedMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicy.class.getMethod(
              "isNetworkAccessAllowed", String.class, int.class, String.class);
      assertNotNull(method, "isNetworkAccessAllowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      Method method = WasiSecurityPolicy.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getAllowedFileSystemOperations method")
    void shouldHaveGetAllowedFileSystemOperationsMethod() throws NoSuchMethodException {
      Method method = WasiSecurityPolicy.class.getMethod("getAllowedFileSystemOperations");
      assertNotNull(method, "getAllowedFileSystemOperations method should exist");
      assertEquals(Set.class, method.getReturnType(), "Return type should be Set");
    }

    @Test
    @DisplayName("should have getAllowedNetworkOperations method")
    void shouldHaveGetAllowedNetworkOperationsMethod() throws NoSuchMethodException {
      Method method = WasiSecurityPolicy.class.getMethod("getAllowedNetworkOperations");
      assertNotNull(method, "getAllowedNetworkOperations method should exist");
      assertEquals(Set.class, method.getReturnType(), "Return type should be Set");
    }

    @Test
    @DisplayName("should have getAllowedPaths method")
    void shouldHaveGetAllowedPathsMethod() throws NoSuchMethodException {
      Method method = WasiSecurityPolicy.class.getMethod("getAllowedPaths");
      assertNotNull(method, "getAllowedPaths method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have isEnvironmentVariableAllowed method")
    void shouldHaveIsEnvironmentVariableAllowedMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicy.class.getMethod("isEnvironmentVariableAllowed", String.class);
      assertNotNull(method, "isEnvironmentVariableAllowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isProcessSpawningAllowed method")
    void shouldHaveIsProcessSpawningAllowedMethod() throws NoSuchMethodException {
      Method method = WasiSecurityPolicy.class.getMethod("isProcessSpawningAllowed");
      assertNotNull(method, "isProcessSpawningAllowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isThreadingAllowed method")
    void shouldHaveIsThreadingAllowedMethod() throws NoSuchMethodException {
      Method method = WasiSecurityPolicy.class.getMethod("isThreadingAllowed");
      assertNotNull(method, "isThreadingAllowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }
  }
}
