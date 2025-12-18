/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 */

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.WasiComponent;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiFactory;
import ai.tegmentum.wasmtime4j.wasi.WasiFilesystem;
import ai.tegmentum.wasmtime4j.wasi.WasiInstance;
import ai.tegmentum.wasmtime4j.wasi.WasiRuntimeInfo;
import ai.tegmentum.wasmtime4j.wasi.WasiRuntimeType;
import ai.tegmentum.wasmtime4j.wasi.WasiVersion;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WASI package core interfaces and classes.
 *
 * <p>This test suite validates the API contracts for the WebAssembly System Interface (WASI)
 * implementation, including WasiContext, WasiConfig, WasiFactory, WasiComponent, WasiInstance,
 * WasiFilesystem, WasiRuntimeType, WasiVersion, and WasiRuntimeInfo.
 */
@DisplayName("WASI Package Tests")
class WasiPackageTest {

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
    @DisplayName("should have JNI constant")
    void shouldHaveJniConstant() {
      WasiRuntimeType jni = WasiRuntimeType.JNI;
      assertNotNull(jni, "JNI constant should exist");
      assertEquals("JNI", jni.name(), "JNI constant should have correct name");
    }

    @Test
    @DisplayName("should have PANAMA constant")
    void shouldHavePanamaConstant() {
      WasiRuntimeType panama = WasiRuntimeType.PANAMA;
      assertNotNull(panama, "PANAMA constant should exist");
      assertEquals("PANAMA", panama.name(), "PANAMA constant should have correct name");
    }

    @Test
    @DisplayName("should have exactly two values")
    void shouldHaveExactlyTwoValues() {
      assertEquals(2, WasiRuntimeType.values().length, "Should have exactly 2 runtime types");
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
    @DisplayName("should have PREVIEW_1 constant")
    void shouldHavePreview1Constant() {
      WasiVersion preview1 = WasiVersion.PREVIEW_1;
      assertNotNull(preview1, "PREVIEW_1 constant should exist");
      assertEquals("PREVIEW_1", preview1.name(), "PREVIEW_1 constant should have correct name");
    }

    @Test
    @DisplayName("should have PREVIEW_2 constant")
    void shouldHavePreview2Constant() {
      WasiVersion preview2 = WasiVersion.PREVIEW_2;
      assertNotNull(preview2, "PREVIEW_2 constant should exist");
      assertEquals("PREVIEW_2", preview2.name(), "PREVIEW_2 constant should have correct name");
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
    @DisplayName("should have supportsAsyncOperations method")
    void shouldHaveSupportsAsyncOperationsMethod() throws NoSuchMethodException {
      Method method = WasiVersion.class.getMethod("supportsAsyncOperations");
      assertNotNull(method, "supportsAsyncOperations method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have supportsComponentModel method")
    void shouldHaveSupportsComponentModelMethod() throws NoSuchMethodException {
      Method method = WasiVersion.class.getMethod("supportsComponentModel");
      assertNotNull(method, "supportsComponentModel method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have supportsWitInterfaces method")
    void shouldHaveSupportsWitInterfacesMethod() throws NoSuchMethodException {
      Method method = WasiVersion.class.getMethod("supportsWitInterfaces");
      assertNotNull(method, "supportsWitInterfaces method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have supportsStreamOperations method")
    void shouldHaveSupportsStreamOperationsMethod() throws NoSuchMethodException {
      Method method = WasiVersion.class.getMethod("supportsStreamOperations");
      assertNotNull(method, "supportsStreamOperations method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have supportsHttpOperations method")
    void shouldHaveSupportsHttpOperationsMethod() throws NoSuchMethodException {
      Method method = WasiVersion.class.getMethod("supportsHttpOperations");
      assertNotNull(method, "supportsHttpOperations method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getDefault static method")
    void shouldHaveGetDefaultStaticMethod() throws NoSuchMethodException {
      Method method = WasiVersion.class.getMethod("getDefault");
      assertNotNull(method, "getDefault method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getDefault should be static");
      assertEquals(WasiVersion.class, method.getReturnType(), "Return type should be WasiVersion");
    }

    @Test
    @DisplayName("should have getLatest static method")
    void shouldHaveGetLatestStaticMethod() throws NoSuchMethodException {
      Method method = WasiVersion.class.getMethod("getLatest");
      assertNotNull(method, "getLatest method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getLatest should be static");
      assertEquals(WasiVersion.class, method.getReturnType(), "Return type should be WasiVersion");
    }

    @Test
    @DisplayName("should have fromVersionString static method")
    void shouldHaveFromVersionStringStaticMethod() throws NoSuchMethodException {
      Method method = WasiVersion.class.getMethod("fromVersionString", String.class);
      assertNotNull(method, "fromVersionString method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromVersionString should be static");
      assertEquals(WasiVersion.class, method.getReturnType(), "Return type should be WasiVersion");
    }

    @Test
    @DisplayName("should have isCompatibleWith method")
    void shouldHaveIsCompatibleWithMethod() throws NoSuchMethodException {
      Method method = WasiVersion.class.getMethod("isCompatibleWith", WasiVersion.class);
      assertNotNull(method, "isCompatibleWith method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("PREVIEW_1 should return correct version string")
    void preview1ShouldReturnCorrectVersionString() {
      assertEquals("0.1.0", WasiVersion.PREVIEW_1.getVersion());
    }

    @Test
    @DisplayName("PREVIEW_2 should return correct version string")
    void preview2ShouldReturnCorrectVersionString() {
      assertEquals("0.2.0", WasiVersion.PREVIEW_2.getVersion());
    }

    @Test
    @DisplayName("PREVIEW_1 should not support async operations")
    void preview1ShouldNotSupportAsyncOperations() {
      assertFalse(WasiVersion.PREVIEW_1.supportsAsyncOperations());
    }

    @Test
    @DisplayName("PREVIEW_2 should support async operations")
    void preview2ShouldSupportAsyncOperations() {
      assertTrue(WasiVersion.PREVIEW_2.supportsAsyncOperations());
    }

    @Test
    @DisplayName("getDefault should return PREVIEW_1")
    void getDefaultShouldReturnPreview1() {
      assertEquals(WasiVersion.PREVIEW_1, WasiVersion.getDefault());
    }

    @Test
    @DisplayName("getLatest should return PREVIEW_2")
    void getLatestShouldReturnPreview2() {
      assertEquals(WasiVersion.PREVIEW_2, WasiVersion.getLatest());
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
    void shouldBeAFinalClass() {
      assertTrue(Modifier.isFinal(WasiRuntimeInfo.class.getModifiers()),
          "WasiRuntimeInfo should be final");
    }

    @Test
    @DisplayName("should have constructor with runtimeType, version, wasmtimeVersion")
    void shouldHaveConstructor() throws NoSuchMethodException {
      var constructor = WasiRuntimeInfo.class.getConstructor(
          WasiRuntimeType.class, String.class, String.class);
      assertNotNull(constructor, "Constructor should exist");
    }

    @Test
    @DisplayName("should have getRuntimeType method")
    void shouldHaveGetRuntimeTypeMethod() throws NoSuchMethodException {
      Method method = WasiRuntimeInfo.class.getMethod("getRuntimeType");
      assertNotNull(method, "getRuntimeType method should exist");
      assertEquals(WasiRuntimeType.class, method.getReturnType(),
          "Return type should be WasiRuntimeType");
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

    @Test
    @DisplayName("should override toString")
    void shouldOverrideToString() throws NoSuchMethodException {
      Method method = WasiRuntimeInfo.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(WasiRuntimeInfo.class, method.getDeclaringClass(),
          "toString should be declared in WasiRuntimeInfo");
    }

    @Test
    @DisplayName("should override equals")
    void shouldOverrideEquals() throws NoSuchMethodException {
      Method method = WasiRuntimeInfo.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(WasiRuntimeInfo.class, method.getDeclaringClass(),
          "equals should be declared in WasiRuntimeInfo");
    }

    @Test
    @DisplayName("should override hashCode")
    void shouldOverrideHashCode() throws NoSuchMethodException {
      Method method = WasiRuntimeInfo.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(WasiRuntimeInfo.class, method.getDeclaringClass(),
          "hashCode should be declared in WasiRuntimeInfo");
    }
  }

  // ========================================================================
  // WasiFactory Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiFactory Tests")
  class WasiFactoryTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertFalse(WasiFactory.class.isInterface(), "WasiFactory should be a class");
      assertFalse(WasiFactory.class.isEnum(), "WasiFactory should not be an enum");
    }

    @Test
    @DisplayName("should have createContext static method")
    void shouldHaveCreateContextMethod() throws NoSuchMethodException {
      Method method = WasiFactory.class.getMethod("createContext");
      assertNotNull(method, "createContext method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "createContext should be static");
      assertEquals(WasiContext.class, method.getReturnType(),
          "Return type should be WasiContext");
    }

    @Test
    @DisplayName("should have createContext with runtime type method")
    void shouldHaveCreateContextWithRuntimeTypeMethod() throws NoSuchMethodException {
      Method method = WasiFactory.class.getMethod("createContext", WasiRuntimeType.class);
      assertNotNull(method, "createContext(WasiRuntimeType) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "createContext should be static");
      assertEquals(WasiContext.class, method.getReturnType(),
          "Return type should be WasiContext");
    }

    @Test
    @DisplayName("should have getSelectedRuntimeType static method")
    void shouldHaveGetSelectedRuntimeTypeMethod() throws NoSuchMethodException {
      Method method = WasiFactory.class.getMethod("getSelectedRuntimeType");
      assertNotNull(method, "getSelectedRuntimeType method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()),
          "getSelectedRuntimeType should be static");
      assertEquals(WasiRuntimeType.class, method.getReturnType(),
          "Return type should be WasiRuntimeType");
    }

    @Test
    @DisplayName("should have isRuntimeAvailable static method")
    void shouldHaveIsRuntimeAvailableMethod() throws NoSuchMethodException {
      Method method = WasiFactory.class.getMethod("isRuntimeAvailable", WasiRuntimeType.class);
      assertNotNull(method, "isRuntimeAvailable method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "isRuntimeAvailable should be static");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getJavaVersion static method")
    void shouldHaveGetJavaVersionMethod() throws NoSuchMethodException {
      Method method = WasiFactory.class.getMethod("getJavaVersion");
      assertNotNull(method, "getJavaVersion method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getJavaVersion should be static");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have WASI_RUNTIME_PROPERTY constant")
    void shouldHaveWasiRuntimePropertyConstant() throws NoSuchFieldException {
      var field = WasiFactory.class.getField("WASI_RUNTIME_PROPERTY");
      assertNotNull(field, "WASI_RUNTIME_PROPERTY field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(String.class, field.getType(), "Should be String type");
    }

    @Test
    @DisplayName("should have WASI_RUNTIME_JNI constant")
    void shouldHaveWasiRuntimeJniConstant() throws NoSuchFieldException {
      var field = WasiFactory.class.getField("WASI_RUNTIME_JNI");
      assertNotNull(field, "WASI_RUNTIME_JNI field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(String.class, field.getType(), "Should be String type");
    }

    @Test
    @DisplayName("should have WASI_RUNTIME_PANAMA constant")
    void shouldHaveWasiRuntimePanamaConstant() throws NoSuchFieldException {
      var field = WasiFactory.class.getField("WASI_RUNTIME_PANAMA");
      assertNotNull(field, "WASI_RUNTIME_PANAMA field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(String.class, field.getType(), "Should be String type");
    }
  }

  // ========================================================================
  // WasiContext Tests
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
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(Closeable.class.isAssignableFrom(WasiContext.class),
          "WasiContext should extend Closeable");
    }

    @Test
    @DisplayName("should have createComponent method")
    void shouldHaveCreateComponentMethod() throws NoSuchMethodException {
      Method method = WasiContext.class.getMethod("createComponent", byte[].class);
      assertNotNull(method, "createComponent method should exist");
      assertEquals(WasiComponent.class, method.getReturnType(),
          "Return type should be WasiComponent");
    }

    @Test
    @DisplayName("should have getRuntimeInfo method")
    void shouldHaveGetRuntimeInfoMethod() throws NoSuchMethodException {
      Method method = WasiContext.class.getMethod("getRuntimeInfo");
      assertNotNull(method, "getRuntimeInfo method should exist");
      assertEquals(WasiRuntimeInfo.class, method.getReturnType(),
          "Return type should be WasiRuntimeInfo");
    }

    @Test
    @DisplayName("should have getFilesystem method")
    void shouldHaveGetFilesystemMethod() throws NoSuchMethodException {
      Method method = WasiContext.class.getMethod("getFilesystem");
      assertNotNull(method, "getFilesystem method should exist");
      assertEquals(WasiFilesystem.class, method.getReturnType(),
          "Return type should be WasiFilesystem");
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

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods = Set.of(
          "createComponent",
          "getRuntimeInfo",
          "getFilesystem",
          "isValid",
          "close");

      Set<String> actualMethods = Arrays.stream(WasiContext.class.getDeclaredMethods())
          .map(Method::getName)
          .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected),
            "WasiContext should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // WasiConfig Tests
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
    void shouldHaveBuilderStaticMethod() throws NoSuchMethodException {
      Method method = WasiConfig.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
    }

    @Test
    @DisplayName("should have defaultConfig static method")
    void shouldHaveDefaultConfigStaticMethod() throws NoSuchMethodException {
      Method method = WasiConfig.class.getMethod("defaultConfig");
      assertNotNull(method, "defaultConfig method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "defaultConfig should be static");
      assertEquals(WasiConfig.class, method.getReturnType(),
          "Return type should be WasiConfig");
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
    @DisplayName("should have getWorkingDirectory method")
    void shouldHaveGetWorkingDirectoryMethod() throws NoSuchMethodException {
      Method method = WasiConfig.class.getMethod("getWorkingDirectory");
      assertNotNull(method, "getWorkingDirectory method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have getMemoryLimit method")
    void shouldHaveGetMemoryLimitMethod() throws NoSuchMethodException {
      Method method = WasiConfig.class.getMethod("getMemoryLimit");
      assertNotNull(method, "getMemoryLimit method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have getExecutionTimeout method")
    void shouldHaveGetExecutionTimeoutMethod() throws NoSuchMethodException {
      Method method = WasiConfig.class.getMethod("getExecutionTimeout");
      assertNotNull(method, "getExecutionTimeout method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
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
    @DisplayName("should have getWasiVersion method")
    void shouldHaveGetWasiVersionMethod() throws NoSuchMethodException {
      Method method = WasiConfig.class.getMethod("getWasiVersion");
      assertNotNull(method, "getWasiVersion method should exist");
      assertEquals(WasiVersion.class, method.getReturnType(),
          "Return type should be WasiVersion");
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
  // WasiComponent Tests
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
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(Closeable.class.isAssignableFrom(WasiComponent.class),
          "WasiComponent should extend Closeable");
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
    @DisplayName("should have getExportMetadata method")
    void shouldHaveGetExportMetadataMethod() throws NoSuchMethodException {
      Method method = WasiComponent.class.getMethod("getExportMetadata", String.class);
      assertNotNull(method, "getExportMetadata method should exist");
    }

    @Test
    @DisplayName("should have getImportMetadata method")
    void shouldHaveGetImportMetadataMethod() throws NoSuchMethodException {
      Method method = WasiComponent.class.getMethod("getImportMetadata", String.class);
      assertNotNull(method, "getImportMetadata method should exist");
    }

    @Test
    @DisplayName("should have instantiate method without parameters")
    void shouldHaveInstantiateMethodWithoutParams() throws NoSuchMethodException {
      Method method = WasiComponent.class.getMethod("instantiate");
      assertNotNull(method, "instantiate() method should exist");
      assertEquals(WasiInstance.class, method.getReturnType(),
          "Return type should be WasiInstance");
    }

    @Test
    @DisplayName("should have instantiate method with config parameter")
    void shouldHaveInstantiateMethodWithConfig() throws NoSuchMethodException {
      Method method = WasiComponent.class.getMethod("instantiate", WasiConfig.class);
      assertNotNull(method, "instantiate(WasiConfig) method should exist");
      assertEquals(WasiInstance.class, method.getReturnType(),
          "Return type should be WasiInstance");
    }

    @Test
    @DisplayName("should have validate method without parameters")
    void shouldHaveValidateMethodWithoutParams() throws NoSuchMethodException {
      Method method = WasiComponent.class.getMethod("validate");
      assertNotNull(method, "validate() method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have validate method with config parameter")
    void shouldHaveValidateMethodWithConfig() throws NoSuchMethodException {
      Method method = WasiComponent.class.getMethod("validate", WasiConfig.class);
      assertNotNull(method, "validate(WasiConfig) method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      Method method = WasiComponent.class.getMethod("getStats");
      assertNotNull(method, "getStats method should exist");
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
  // WasiInstance Tests
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
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(Closeable.class.isAssignableFrom(WasiInstance.class),
          "WasiInstance should extend Closeable");
    }

    @Test
    @DisplayName("should have getId method")
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
      assertEquals(WasiComponent.class, method.getReturnType(),
          "Return type should be WasiComponent");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(WasiConfig.class, method.getReturnType(),
          "Return type should be WasiConfig");
    }

    @Test
    @DisplayName("should have getState method")
    void shouldHaveGetStateMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("getState");
      assertNotNull(method, "getState method should exist");
    }

    @Test
    @DisplayName("should have getCreatedAt method")
    void shouldHaveGetCreatedAtMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("getCreatedAt");
      assertNotNull(method, "getCreatedAt method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Return type should be Instant");
    }

    @Test
    @DisplayName("should have getLastActivityAt method")
    void shouldHaveGetLastActivityAtMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("getLastActivityAt");
      assertNotNull(method, "getLastActivityAt method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have call method with varargs")
    void shouldHaveCallMethodWithVarargs() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("call", String.class, Object[].class);
      assertNotNull(method, "call method should exist");
      assertEquals(Object.class, method.getReturnType(), "Return type should be Object");
    }

    @Test
    @DisplayName("should have call method with timeout")
    void shouldHaveCallMethodWithTimeout() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("call", String.class, Duration.class,
          Object[].class);
      assertNotNull(method, "call(String, Duration, Object...) method should exist");
      assertEquals(Object.class, method.getReturnType(), "Return type should be Object");
    }

    @Test
    @DisplayName("should have callAsync method")
    void shouldHaveCallAsyncMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("callAsync", String.class, Object[].class);
      assertNotNull(method, "callAsync method should exist");
      assertEquals(CompletableFuture.class, method.getReturnType(),
          "Return type should be CompletableFuture");
    }

    @Test
    @DisplayName("should have getExportedFunctions method")
    void shouldHaveGetExportedFunctionsMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("getExportedFunctions");
      assertNotNull(method, "getExportedFunctions method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getExportedInterfaces method")
    void shouldHaveGetExportedInterfacesMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("getExportedInterfaces");
      assertNotNull(method, "getExportedInterfaces method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getFunctionMetadata method")
    void shouldHaveGetFunctionMetadataMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("getFunctionMetadata", String.class);
      assertNotNull(method, "getFunctionMetadata method should exist");
    }

    @Test
    @DisplayName("should have getResources method without parameters")
    void shouldHaveGetResourcesMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("getResources");
      assertNotNull(method, "getResources() method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getResources method with type parameter")
    void shouldHaveGetResourcesMethodWithType() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("getResources", String.class);
      assertNotNull(method, "getResources(String) method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getResource method")
    void shouldHaveGetResourceMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("getResource", long.class);
      assertNotNull(method, "getResource method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have createResource method")
    void shouldHaveCreateResourceMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("createResource", String.class, Object[].class);
      assertNotNull(method, "createResource method should exist");
    }

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("getStats");
      assertNotNull(method, "getStats method should exist");
    }

    @Test
    @DisplayName("should have getMemoryInfo method")
    void shouldHaveGetMemoryInfoMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("getMemoryInfo");
      assertNotNull(method, "getMemoryInfo method should exist");
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
    @DisplayName("should have isExecuting method")
    void shouldHaveIsExecutingMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("isExecuting");
      assertNotNull(method, "isExecuting method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have setProperty method")
    void shouldHaveSetPropertyMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("setProperty", String.class, Object.class);
      assertNotNull(method, "setProperty method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getProperty method")
    void shouldHaveGetPropertyMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("getProperty", String.class);
      assertNotNull(method, "getProperty method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have getProperties method")
    void shouldHaveGetPropertiesMethod() throws NoSuchMethodException {
      Method method = WasiInstance.class.getMethod("getProperties");
      assertNotNull(method, "getProperties method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
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
  // WasiFilesystem Tests
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
      // Method signature: openFile(String path, WasiOpenFlags flags, WasiRights rights)
      Method[] methods = WasiFilesystem.class.getMethods();
      boolean found = Arrays.stream(methods)
          .anyMatch(m -> m.getName().equals("openFile") && m.getParameterCount() == 3);
      assertTrue(found, "openFile method should exist with 3 parameters");
    }

    @Test
    @DisplayName("should have closeFile method")
    void shouldHaveCloseFileMethod() throws NoSuchMethodException {
      Method[] methods = WasiFilesystem.class.getMethods();
      boolean found = Arrays.stream(methods)
          .anyMatch(m -> m.getName().equals("closeFile") && m.getParameterCount() == 1);
      assertTrue(found, "closeFile method should exist");
    }

    @Test
    @DisplayName("should have readFile method")
    void shouldHaveReadFileMethod() throws NoSuchMethodException {
      Method[] methods = WasiFilesystem.class.getMethods();
      boolean found = Arrays.stream(methods)
          .anyMatch(m -> m.getName().equals("readFile") && m.getParameterCount() == 3);
      assertTrue(found, "readFile method should exist with 3 parameters");
    }

    @Test
    @DisplayName("should have writeFile method")
    void shouldHaveWriteFileMethod() throws NoSuchMethodException {
      Method[] methods = WasiFilesystem.class.getMethods();
      boolean found = Arrays.stream(methods)
          .anyMatch(m -> m.getName().equals("writeFile") && m.getParameterCount() == 3);
      assertTrue(found, "writeFile method should exist with 3 parameters");
    }

    @Test
    @DisplayName("should have openDirectory method")
    void shouldHaveOpenDirectoryMethod() throws NoSuchMethodException {
      Method[] methods = WasiFilesystem.class.getMethods();
      boolean found = Arrays.stream(methods)
          .anyMatch(m -> m.getName().equals("openDirectory") && m.getParameterCount() == 2);
      assertTrue(found, "openDirectory method should exist with 2 parameters");
    }

    @Test
    @DisplayName("should have readDirectory method")
    void shouldHaveReadDirectoryMethod() throws NoSuchMethodException {
      Method[] methods = WasiFilesystem.class.getMethods();
      boolean found = Arrays.stream(methods)
          .anyMatch(m -> m.getName().equals("readDirectory") && m.getParameterCount() == 1);
      assertTrue(found, "readDirectory method should exist");
    }

    @Test
    @DisplayName("should have createDirectory method")
    void shouldHaveCreateDirectoryMethod() throws NoSuchMethodException {
      Method[] methods = WasiFilesystem.class.getMethods();
      boolean found = Arrays.stream(methods)
          .anyMatch(m -> m.getName().equals("createDirectory") && m.getParameterCount() == 2);
      assertTrue(found, "createDirectory method should exist with 2 parameters");
    }

    @Test
    @DisplayName("should have removeDirectory method")
    void shouldHaveRemoveDirectoryMethod() throws NoSuchMethodException {
      Method method = WasiFilesystem.class.getMethod("removeDirectory", String.class);
      assertNotNull(method, "removeDirectory method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getFileStats method")
    void shouldHaveGetFileStatsMethod() throws NoSuchMethodException {
      Method method = WasiFilesystem.class.getMethod("getFileStats", String.class);
      assertNotNull(method, "getFileStats method should exist");
    }

    @Test
    @DisplayName("should have canonicalizePath method")
    void shouldHaveCanonicalizePath() throws NoSuchMethodException {
      Method method = WasiFilesystem.class.getMethod("canonicalizePath", String.class);
      assertNotNull(method, "canonicalizePath method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have symlinkCreate method")
    void shouldHaveSymlinkCreateMethod() throws NoSuchMethodException {
      Method method = WasiFilesystem.class.getMethod("symlinkCreate", String.class, String.class);
      assertNotNull(method, "symlinkCreate method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have readSymlink method")
    void shouldHaveReadSymlinkMethod() throws NoSuchMethodException {
      Method method = WasiFilesystem.class.getMethod("readSymlink", String.class);
      assertNotNull(method, "readSymlink method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have rename method")
    void shouldHaveRenameMethod() throws NoSuchMethodException {
      Method method = WasiFilesystem.class.getMethod("rename", String.class, String.class);
      assertNotNull(method, "rename method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have unlink method")
    void shouldHaveUnlinkMethod() throws NoSuchMethodException {
      Method method = WasiFilesystem.class.getMethod("unlink", String.class);
      assertNotNull(method, "unlink method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getCurrentWorkingDirectory method")
    void shouldHaveGetCurrentWorkingDirectoryMethod() throws NoSuchMethodException {
      Method method = WasiFilesystem.class.getMethod("getCurrentWorkingDirectory");
      assertNotNull(method, "getCurrentWorkingDirectory method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have setCurrentWorkingDirectory method")
    void shouldHaveSetCurrentWorkingDirectoryMethod() throws NoSuchMethodException {
      Method method = WasiFilesystem.class.getMethod("setCurrentWorkingDirectory", String.class);
      assertNotNull(method, "setCurrentWorkingDirectory method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have all expected filesystem methods")
    void shouldHaveAllExpectedFilesystemMethods() {
      Set<String> expectedMethods = Set.of(
          "openFile",
          "closeFile",
          "readFile",
          "writeFile",
          "openDirectory",
          "readDirectory",
          "createDirectory",
          "removeDirectory",
          "getFileStats",
          "setFileStats",
          "setFilePermissions",
          "canonicalizePath",
          "symlinkCreate",
          "readSymlink",
          "rename",
          "unlink",
          "syncFile",
          "getCurrentWorkingDirectory",
          "setCurrentWorkingDirectory");

      Set<String> actualMethods = Arrays.stream(WasiFilesystem.class.getDeclaredMethods())
          .map(Method::getName)
          .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected),
            "WasiFilesystem should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // Method Count and Completeness Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count and Completeness Tests")
  class MethodCountTests {

    @Test
    @DisplayName("WasiContext should have at least 5 methods")
    void wasiContextShouldHaveMinimumMethods() {
      int methodCount = WasiContext.class.getDeclaredMethods().length;
      assertTrue(methodCount >= 5,
          "WasiContext should have at least 5 methods, found: " + methodCount);
    }

    @Test
    @DisplayName("WasiConfig should have at least 10 methods")
    void wasiConfigShouldHaveMinimumMethods() {
      int methodCount = WasiConfig.class.getDeclaredMethods().length;
      assertTrue(methodCount >= 10,
          "WasiConfig should have at least 10 methods, found: " + methodCount);
    }

    @Test
    @DisplayName("WasiComponent should have at least 10 methods")
    void wasiComponentShouldHaveMinimumMethods() {
      int methodCount = WasiComponent.class.getDeclaredMethods().length;
      assertTrue(methodCount >= 10,
          "WasiComponent should have at least 10 methods, found: " + methodCount);
    }

    @Test
    @DisplayName("WasiInstance should have at least 20 methods")
    void wasiInstanceShouldHaveMinimumMethods() {
      int methodCount = WasiInstance.class.getDeclaredMethods().length;
      assertTrue(methodCount >= 20,
          "WasiInstance should have at least 20 methods, found: " + methodCount);
    }

    @Test
    @DisplayName("WasiFilesystem should have at least 15 methods")
    void wasiFilesystemShouldHaveMinimumMethods() {
      int methodCount = WasiFilesystem.class.getDeclaredMethods().length;
      assertTrue(methodCount >= 15,
          "WasiFilesystem should have at least 15 methods, found: " + methodCount);
    }

    @Test
    @DisplayName("WasiVersion should have at least 10 methods")
    void wasiVersionShouldHaveMinimumMethods() {
      int methodCount = WasiVersion.class.getDeclaredMethods().length;
      assertTrue(methodCount >= 10,
          "WasiVersion should have at least 10 methods, found: " + methodCount);
    }
  }
}
