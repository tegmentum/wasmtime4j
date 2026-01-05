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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasmRuntime interface.
 *
 * <p>WasmRuntime is the primary entry point for WebAssembly runtime operations. This test verifies
 * the interface structure, method signatures, and API conformance.
 */
@DisplayName("WasmRuntime Interface Tests")
class WasmRuntimeTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasmRuntime.class.isInterface(), "WasmRuntime should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasmRuntime.class.getModifiers()), "WasmRuntime should be public");
    }

    @Test
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(WasmRuntime.class),
          "WasmRuntime should extend Closeable");
    }
  }

  // ========================================================================
  // Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have builder static method")
    void shouldHaveBuilderStaticMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(
          WasmRuntimeBuilder.class,
          method.getReturnType(),
          "builder should return WasmRuntimeBuilder");
    }
  }

  // ========================================================================
  // Engine Creation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Engine Creation Method Tests")
  class EngineCreationMethodTests {

    @Test
    @DisplayName("should have createEngine method")
    void shouldHaveCreateEngineMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createEngine");
      assertNotNull(method, "createEngine method should exist");
      assertEquals(Engine.class, method.getReturnType(), "createEngine should return Engine");
    }

    @Test
    @DisplayName("createEngine should declare WasmException")
    void createEngineShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createEngine");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          Arrays.asList(exceptionTypes).contains(WasmException.class),
          "createEngine should declare WasmException");
    }

    @Test
    @DisplayName("should have createEngine method with EngineConfig parameter")
    void shouldHaveCreateEngineWithConfigMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createEngine", EngineConfig.class);
      assertNotNull(method, "createEngine(EngineConfig) method should exist");
      assertEquals(Engine.class, method.getReturnType(), "createEngine should return Engine");
    }
  }

  // ========================================================================
  // Module Compilation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Module Compilation Method Tests")
  class ModuleCompilationMethodTests {

    @Test
    @DisplayName("should have compileModule method")
    void shouldHaveCompileModuleMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("compileModule", Engine.class, byte[].class);
      assertNotNull(method, "compileModule method should exist");
      assertEquals(Module.class, method.getReturnType(), "compileModule should return Module");
    }

    @Test
    @DisplayName("compileModule should declare WasmException")
    void compileModuleShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("compileModule", Engine.class, byte[].class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          Arrays.asList(exceptionTypes).contains(WasmException.class),
          "compileModule should declare WasmException");
    }

    @Test
    @DisplayName("should have compileModuleWat method")
    void shouldHaveCompileModuleWatMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("compileModuleWat", Engine.class, String.class);
      assertNotNull(method, "compileModuleWat method should exist");
      assertEquals(Module.class, method.getReturnType(), "compileModuleWat should return Module");
    }

    @Test
    @DisplayName("compileModuleWat should declare WasmException")
    void compileModuleWatShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("compileModuleWat", Engine.class, String.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          Arrays.asList(exceptionTypes).contains(WasmException.class),
          "compileModuleWat should declare WasmException");
    }
  }

  // ========================================================================
  // Store Creation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Store Creation Method Tests")
  class StoreCreationMethodTests {

    @Test
    @DisplayName("should have createStore method with Engine parameter")
    void shouldHaveCreateStoreMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createStore", Engine.class);
      assertNotNull(method, "createStore method should exist");
      assertEquals(Store.class, method.getReturnType(), "createStore should return Store");
    }

    @Test
    @DisplayName("createStore should declare WasmException")
    void createStoreShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createStore", Engine.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          Arrays.asList(exceptionTypes).contains(WasmException.class),
          "createStore should declare WasmException");
    }

    @Test
    @DisplayName("should have createStore method with fuel and limits")
    void shouldHaveCreateStoreWithLimitsMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod(
              "createStore", Engine.class, long.class, long.class, long.class);
      assertNotNull(method, "createStore with limits method should exist");
      assertEquals(Store.class, method.getReturnType(), "createStore should return Store");
    }

    @Test
    @DisplayName("should have createStore method with StoreLimits")
    void shouldHaveCreateStoreWithStoreLimitsMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("createStore", Engine.class, StoreLimits.class);
      assertNotNull(method, "createStore with StoreLimits method should exist");
      assertEquals(Store.class, method.getReturnType(), "createStore should return Store");
    }
  }

  // ========================================================================
  // Tag Creation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Tag Creation Method Tests")
  class TagCreationMethodTests {

    @Test
    @DisplayName("should have createTag method")
    void shouldHaveCreateTagMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createTag", Store.class, TagType.class);
      assertNotNull(method, "createTag method should exist");
      assertEquals(Tag.class, method.getReturnType(), "createTag should return Tag");
    }

    @Test
    @DisplayName("createTag should declare WasmException")
    void createTagShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createTag", Store.class, TagType.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          Arrays.asList(exceptionTypes).contains(WasmException.class),
          "createTag should declare WasmException");
    }
  }

  // ========================================================================
  // Linker Creation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Linker Creation Method Tests")
  class LinkerCreationMethodTests {

    @Test
    @DisplayName("should have createLinker method with Engine parameter")
    void shouldHaveCreateLinkerMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createLinker", Engine.class);
      assertNotNull(method, "createLinker method should exist");
      assertEquals(Linker.class, method.getReturnType(), "createLinker should return Linker");
    }

    @Test
    @DisplayName("createLinker should be generic")
    void createLinkerShouldBeGeneric() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createLinker", Engine.class);
      TypeVariable<?>[] typeParameters = method.getTypeParameters();
      assertEquals(1, typeParameters.length, "createLinker should have one type parameter");
      assertEquals("T", typeParameters[0].getName(), "Type parameter should be named T");
    }

    @Test
    @DisplayName("should have createLinker method with configuration options")
    void shouldHaveCreateLinkerWithOptionsMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("createLinker", Engine.class, boolean.class, boolean.class);
      assertNotNull(method, "createLinker with options method should exist");
      assertEquals(Linker.class, method.getReturnType(), "createLinker should return Linker");
    }

    @Test
    @DisplayName("should have createComponentLinker method")
    void shouldHaveCreateComponentLinkerMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createComponentLinker", Engine.class);
      assertNotNull(method, "createComponentLinker method should exist");
      assertEquals(
          ComponentLinker.class,
          method.getReturnType(),
          "createComponentLinker should return ComponentLinker");
    }
  }

  // ========================================================================
  // Instance Creation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Instance Creation Method Tests")
  class InstanceCreationMethodTests {

    @Test
    @DisplayName("should have instantiate method with Module parameter")
    void shouldHaveInstantiateMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("instantiate", Module.class);
      assertNotNull(method, "instantiate method should exist");
      assertEquals(Instance.class, method.getReturnType(), "instantiate should return Instance");
    }

    @Test
    @DisplayName("instantiate should declare WasmException")
    void instantiateShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("instantiate", Module.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          Arrays.asList(exceptionTypes).contains(WasmException.class),
          "instantiate should declare WasmException");
    }

    @Test
    @DisplayName("should have instantiate method with ImportMap")
    void shouldHaveInstantiateWithImportsMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("instantiate", Module.class, ImportMap.class);
      assertNotNull(method, "instantiate with imports method should exist");
      assertEquals(Instance.class, method.getReturnType(), "instantiate should return Instance");
    }
  }

  // ========================================================================
  // Component Engine Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Component Engine Method Tests")
  class ComponentEngineMethodTests {

    @Test
    @DisplayName("should have createComponentEngine method")
    void shouldHaveCreateComponentEngineMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createComponentEngine");
      assertNotNull(method, "createComponentEngine method should exist");
      assertEquals(
          ComponentEngine.class,
          method.getReturnType(),
          "createComponentEngine should return ComponentEngine");
    }

    @Test
    @DisplayName("should have createComponentEngine method with config")
    void shouldHaveCreateComponentEngineWithConfigMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("createComponentEngine", ComponentEngineConfig.class);
      assertNotNull(method, "createComponentEngine with config method should exist");
      assertEquals(
          ComponentEngine.class,
          method.getReturnType(),
          "createComponentEngine should return ComponentEngine");
    }

    @Test
    @DisplayName("should have supportsComponentModel method")
    void shouldHaveSupportsComponentModelMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("supportsComponentModel");
      assertNotNull(method, "supportsComponentModel method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "supportsComponentModel should return boolean");
    }
  }

  // ========================================================================
  // Runtime Info Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Runtime Info Method Tests")
  class RuntimeInfoMethodTests {

    @Test
    @DisplayName("should have getRuntimeInfo method")
    void shouldHaveGetRuntimeInfoMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("getRuntimeInfo");
      assertNotNull(method, "getRuntimeInfo method should exist");
      assertEquals(
          RuntimeInfo.class, method.getReturnType(), "getRuntimeInfo should return RuntimeInfo");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("should have getDebuggingCapabilities method")
    void shouldHaveGetDebuggingCapabilitiesMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("getDebuggingCapabilities");
      assertNotNull(method, "getDebuggingCapabilities method should exist");
      assertEquals(
          String.class, method.getReturnType(), "getDebuggingCapabilities should return String");
    }
  }

  // ========================================================================
  // Serialization Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Serialization Method Tests")
  class SerializationMethodTests {

    @Test
    @DisplayName("should have deserializeModule method")
    void shouldHaveDeserializeModuleMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("deserializeModule", Engine.class, byte[].class);
      assertNotNull(method, "deserializeModule method should exist");
      assertEquals(Module.class, method.getReturnType(), "deserializeModule should return Module");
    }

    @Test
    @DisplayName("should have deserializeModuleFile method")
    void shouldHaveDeserializeModuleFileMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("deserializeModuleFile", Engine.class, Path.class);
      assertNotNull(method, "deserializeModuleFile method should exist");
      assertEquals(
          Module.class, method.getReturnType(), "deserializeModuleFile should return Module");
    }

    @Test
    @DisplayName("should have createSerializer method")
    void shouldHaveCreateSerializerMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createSerializer");
      assertNotNull(method, "createSerializer method should exist");
      assertEquals(
          Serializer.class, method.getReturnType(), "createSerializer should return Serializer");
    }

    @Test
    @DisplayName("should have createSerializer method with configuration")
    void shouldHaveCreateSerializerWithConfigMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("createSerializer", long.class, boolean.class, int.class);
      assertNotNull(method, "createSerializer with config method should exist");
      assertEquals(
          Serializer.class, method.getReturnType(), "createSerializer should return Serializer");
    }
  }

  // ========================================================================
  // WASI Method Tests
  // ========================================================================

  @Nested
  @DisplayName("WASI Method Tests")
  class WasiMethodTests {

    @Test
    @DisplayName("should have createWasiContext method")
    void shouldHaveCreateWasiContextMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createWasiContext");
      assertNotNull(method, "createWasiContext method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "createWasiContext should return WasiContext");
    }

    @Test
    @DisplayName("should have addWasiToLinker method")
    void shouldHaveAddWasiToLinkerMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("addWasiToLinker", Linker.class, WasiContext.class);
      assertNotNull(method, "addWasiToLinker method should exist");
      assertEquals(void.class, method.getReturnType(), "addWasiToLinker should return void");
    }

    @Test
    @DisplayName("should have addWasiPreview2ToLinker method")
    void shouldHaveAddWasiPreview2ToLinkerMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("addWasiPreview2ToLinker", Linker.class, WasiContext.class);
      assertNotNull(method, "addWasiPreview2ToLinker method should exist");
      assertEquals(
          void.class, method.getReturnType(), "addWasiPreview2ToLinker should return void");
    }

    @Test
    @DisplayName("should have addComponentModelToLinker method")
    void shouldHaveAddComponentModelToLinkerMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("addComponentModelToLinker", Linker.class);
      assertNotNull(method, "addComponentModelToLinker method should exist");
      assertEquals(
          void.class, method.getReturnType(), "addComponentModelToLinker should return void");
    }

    @Test
    @DisplayName("should have createWasiLinker method with Engine")
    void shouldHaveCreateWasiLinkerMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createWasiLinker", Engine.class);
      assertNotNull(method, "createWasiLinker method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.wasi.WasiLinker.class,
          method.getReturnType(),
          "createWasiLinker should return WasiLinker");
    }

    @Test
    @DisplayName("should have createWasiLinker method with Engine and WasiConfig")
    void shouldHaveCreateWasiLinkerWithConfigMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod(
              "createWasiLinker", Engine.class, ai.tegmentum.wasmtime4j.wasi.WasiConfig.class);
      assertNotNull(method, "createWasiLinker with config method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.wasi.WasiLinker.class,
          method.getReturnType(),
          "createWasiLinker should return WasiLinker");
    }
  }

  // ========================================================================
  // Advanced Features Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Advanced Features Method Tests")
  class AdvancedFeaturesMethodTests {

    @Test
    @DisplayName("should have getGcRuntime method")
    void shouldHaveGetGcRuntimeMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("getGcRuntime");
      assertNotNull(method, "getGcRuntime method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.gc.GcRuntime.class,
          method.getReturnType(),
          "getGcRuntime should return GcRuntime");
    }

    @Test
    @DisplayName("getGcRuntime should declare WasmException")
    void getGcRuntimeShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("getGcRuntime");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          Arrays.asList(exceptionTypes).contains(WasmException.class),
          "getGcRuntime should declare WasmException");
    }

    @Test
    @DisplayName("should have getSimdOperations method")
    void shouldHaveGetSimdOperationsMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("getSimdOperations");
      assertNotNull(method, "getSimdOperations method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.simd.SimdOperations.class,
          method.getReturnType(),
          "getSimdOperations should return SimdOperations");
    }

    @Test
    @DisplayName("getSimdOperations should declare WasmException")
    void getSimdOperationsShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("getSimdOperations");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          Arrays.asList(exceptionTypes).contains(WasmException.class),
          "getSimdOperations should declare WasmException");
    }

    @Test
    @DisplayName("should have createNnContext method")
    void shouldHaveCreateNnContextMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createNnContext");
      assertNotNull(method, "createNnContext method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.wasi.nn.NnContext.class,
          method.getReturnType(),
          "createNnContext should return NnContext");
    }

    @Test
    @DisplayName("createNnContext should declare NnException")
    void createNnContextShouldDeclareNnException() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createNnContext");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          Arrays.asList(exceptionTypes).contains(ai.tegmentum.wasmtime4j.wasi.nn.NnException.class),
          "createNnContext should declare NnException");
    }

    @Test
    @DisplayName("should have isNnAvailable method")
    void shouldHaveIsNnAvailableMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("isNnAvailable");
      assertNotNull(method, "isNnAvailable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isNnAvailable should return boolean");
    }
  }

  // ========================================================================
  // Close Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Close Method Tests")
  class CloseMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }

    @Test
    @DisplayName("close should not declare any checked exceptions")
    void closeShouldNotDeclareCheckedExceptions() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("close");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(0, exceptionTypes.length, "close should not declare checked exceptions");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods in WasmRuntime")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "builder",
              "createEngine",
              "compileModule",
              "compileModuleWat",
              "createStore",
              "createTag",
              "createLinker",
              "createComponentLinker",
              "instantiate",
              "createComponentEngine",
              "getRuntimeInfo",
              "isValid",
              "deserializeModule",
              "deserializeModuleFile",
              "createSerializer",
              "getDebuggingCapabilities",
              "createWasiContext",
              "addWasiToLinker",
              "addWasiPreview2ToLinker",
              "addComponentModelToLinker",
              "createWasiLinker",
              "supportsComponentModel",
              "getGcRuntime",
              "getSimdOperations",
              "createNnContext",
              "isNnAvailable",
              "close");

      Set<String> actualMethods =
          Arrays.stream(WasmRuntime.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "WasmRuntime should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have correct number of declared methods")
    void shouldHaveCorrectMethodCount() {
      // WasmRuntime has multiple overloaded methods, so we verify it has at least 27 methods
      int methodCount = WasmRuntime.class.getDeclaredMethods().length;
      assertTrue(
          methodCount >= 27,
          "WasmRuntime should have at least 27 declared methods, found: " + methodCount);
    }
  }

  // ========================================================================
  // Parameter and Return Type Validation Tests
  // ========================================================================

  @Nested
  @DisplayName("Parameter and Return Type Validation Tests")
  class ParameterValidationTests {

    @Test
    @DisplayName("createStore with limits should have correct parameter types")
    void createStoreWithLimitsShouldHaveCorrectParameterTypes() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod(
              "createStore", Engine.class, long.class, long.class, long.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(4, paramTypes.length, "Should have 4 parameters");
      assertEquals(Engine.class, paramTypes[0], "First parameter should be Engine");
      assertEquals(long.class, paramTypes[1], "Second parameter should be long (fuelLimit)");
      assertEquals(long.class, paramTypes[2], "Third parameter should be long (memoryLimitBytes)");
      assertEquals(
          long.class, paramTypes[3], "Fourth parameter should be long (executionTimeoutSeconds)");
    }

    @Test
    @DisplayName("createSerializer with config should have correct parameter types")
    void createSerializerWithConfigShouldHaveCorrectParameterTypes() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("createSerializer", long.class, boolean.class, int.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(3, paramTypes.length, "Should have 3 parameters");
      assertEquals(long.class, paramTypes[0], "First parameter should be long (maxCacheSize)");
      assertEquals(
          boolean.class, paramTypes[1], "Second parameter should be boolean (enableCompression)");
      assertEquals(int.class, paramTypes[2], "Third parameter should be int (compressionLevel)");
    }

    @Test
    @DisplayName("createLinker with options should have correct parameter types")
    void createLinkerWithOptionsShouldHaveCorrectParameterTypes() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("createLinker", Engine.class, boolean.class, boolean.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(3, paramTypes.length, "Should have 3 parameters");
      assertEquals(Engine.class, paramTypes[0], "First parameter should be Engine");
      assertEquals(
          boolean.class, paramTypes[1], "Second parameter should be boolean (allowUnknownExports)");
      assertEquals(
          boolean.class, paramTypes[2], "Third parameter should be boolean (allowShadowing)");
    }
  }

  // ========================================================================
  // Generic Method Signature Tests
  // ========================================================================

  @Nested
  @DisplayName("Generic Method Signature Tests")
  class GenericMethodSignatureTests {

    @Test
    @DisplayName("createLinker should return generic Linker<T>")
    void createLinkerShouldReturnGenericLinker() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createLinker", Engine.class);
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Linker.class, paramType.getRawType(), "Raw type should be Linker");
    }

    @Test
    @DisplayName("createComponentLinker should return generic ComponentLinker<T>")
    void createComponentLinkerShouldReturnGenericComponentLinker() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createComponentLinker", Engine.class);
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(
          ComponentLinker.class, paramType.getRawType(), "Raw type should be ComponentLinker");
    }
  }
}
