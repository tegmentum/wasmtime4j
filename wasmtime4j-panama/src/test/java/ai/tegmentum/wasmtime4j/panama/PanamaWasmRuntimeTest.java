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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ComponentEngine;
import ai.tegmentum.wasmtime4j.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeInfo;
import ai.tegmentum.wasmtime4j.Serializer;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.StoreLimits;
import ai.tegmentum.wasmtime4j.Tag;
import ai.tegmentum.wasmtime4j.TagType;
import ai.tegmentum.wasmtime4j.WasiContext;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasmRuntime} class.
 *
 * <p>PanamaWasmRuntime provides WebAssembly runtime functionality using Panama FFI.
 */
@DisplayName("PanamaWasmRuntime Tests")
class PanamaWasmRuntimeTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasmRuntime.class.getModifiers()),
          "PanamaWasmRuntime should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasmRuntime.class.getModifiers()),
          "PanamaWasmRuntime should be final");
    }

    @Test
    @DisplayName("should implement WasmRuntime interface")
    void shouldImplementWasmRuntimeInterface() {
      assertTrue(
          WasmRuntime.class.isAssignableFrom(PanamaWasmRuntime.class),
          "PanamaWasmRuntime should implement WasmRuntime");
    }

    @Test
    @DisplayName("should implement AutoCloseable interface")
    void shouldImplementAutoCloseableInterface() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaWasmRuntime.class),
          "PanamaWasmRuntime should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Engine Method Tests")
  class EngineMethodTests {

    @Test
    @DisplayName("should have createEngine method")
    void shouldHaveCreateEngineMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmRuntime.class.getMethod("createEngine");
      assertNotNull(method, "createEngine method should exist");
      assertEquals(Engine.class, method.getReturnType(), "Should return Engine");
    }

    @Test
    @DisplayName("should have createEngine method with config")
    void shouldHaveCreateEngineMethodWithConfig() throws NoSuchMethodException {
      final Method method = PanamaWasmRuntime.class.getMethod("createEngine", EngineConfig.class);
      assertNotNull(method, "createEngine method should exist");
      assertEquals(Engine.class, method.getReturnType(), "Should return Engine");
    }

    @Test
    @DisplayName("should have createComponentEngine method")
    void shouldHaveCreateComponentEngineMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmRuntime.class.getMethod("createComponentEngine");
      assertNotNull(method, "createComponentEngine method should exist");
      assertEquals(ComponentEngine.class, method.getReturnType(), "Should return ComponentEngine");
    }

    @Test
    @DisplayName("should have createComponentEngine method with config")
    void shouldHaveCreateComponentEngineMethodWithConfig() throws NoSuchMethodException {
      final Method method =
          PanamaWasmRuntime.class.getMethod("createComponentEngine", ComponentEngineConfig.class);
      assertNotNull(method, "createComponentEngine method should exist");
      assertEquals(ComponentEngine.class, method.getReturnType(), "Should return ComponentEngine");
    }
  }

  @Nested
  @DisplayName("Module Method Tests")
  class ModuleMethodTests {

    @Test
    @DisplayName("should have compileModule method")
    void shouldHaveCompileModuleMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasmRuntime.class.getMethod("compileModule", Engine.class, byte[].class);
      assertNotNull(method, "compileModule method should exist");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("should have compileModuleWat method")
    void shouldHaveCompileModuleWatMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasmRuntime.class.getMethod("compileModuleWat", Engine.class, String.class);
      assertNotNull(method, "compileModuleWat method should exist");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("should have deserializeModule method")
    void shouldHaveDeserializeModuleMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasmRuntime.class.getMethod("deserializeModule", Engine.class, byte[].class);
      assertNotNull(method, "deserializeModule method should exist");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("should have deserializeModuleFile method")
    void shouldHaveDeserializeModuleFileMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasmRuntime.class.getMethod("deserializeModuleFile", Engine.class, Path.class);
      assertNotNull(method, "deserializeModuleFile method should exist");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }
  }

  @Nested
  @DisplayName("Store Method Tests")
  class StoreMethodTests {

    @Test
    @DisplayName("should have createStore method")
    void shouldHaveCreateStoreMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmRuntime.class.getMethod("createStore", Engine.class);
      assertNotNull(method, "createStore method should exist");
      assertEquals(Store.class, method.getReturnType(), "Should return Store");
    }

    @Test
    @DisplayName("should have createStore method with limits")
    void shouldHaveCreateStoreMethodWithLimits() throws NoSuchMethodException {
      final Method method =
          PanamaWasmRuntime.class.getMethod(
              "createStore", Engine.class, long.class, long.class, long.class);
      assertNotNull(method, "createStore method should exist");
      assertEquals(Store.class, method.getReturnType(), "Should return Store");
    }

    @Test
    @DisplayName("should have createStore method with StoreLimits")
    void shouldHaveCreateStoreMethodWithStoreLimits() throws NoSuchMethodException {
      final Method method =
          PanamaWasmRuntime.class.getMethod("createStore", Engine.class, StoreLimits.class);
      assertNotNull(method, "createStore method should exist");
      assertEquals(Store.class, method.getReturnType(), "Should return Store");
    }
  }

  @Nested
  @DisplayName("Linker Method Tests")
  class LinkerMethodTests {

    @Test
    @DisplayName("should have createLinker method")
    void shouldHaveCreateLinkerMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmRuntime.class.getMethod("createLinker", Engine.class);
      assertNotNull(method, "createLinker method should exist");
      assertEquals(Linker.class, method.getReturnType(), "Should return Linker");
    }

    @Test
    @DisplayName("should have createLinker method with options")
    void shouldHaveCreateLinkerMethodWithOptions() throws NoSuchMethodException {
      final Method method =
          PanamaWasmRuntime.class.getMethod(
              "createLinker", Engine.class, boolean.class, boolean.class);
      assertNotNull(method, "createLinker method should exist");
      assertEquals(Linker.class, method.getReturnType(), "Should return Linker");
    }
  }

  @Nested
  @DisplayName("Instance Method Tests")
  class InstanceMethodTests {

    @Test
    @DisplayName("should have instantiate method")
    void shouldHaveInstantiateMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmRuntime.class.getMethod("instantiate", Module.class);
      assertNotNull(method, "instantiate method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }

    @Test
    @DisplayName("should have instantiate method with imports")
    void shouldHaveInstantiateMethodWithImports() throws NoSuchMethodException {
      final Method method =
          PanamaWasmRuntime.class.getMethod("instantiate", Module.class, ImportMap.class);
      assertNotNull(method, "instantiate method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }
  }

  @Nested
  @DisplayName("Tag Method Tests")
  class TagMethodTests {

    @Test
    @DisplayName("should have createTag method")
    void shouldHaveCreateTagMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasmRuntime.class.getMethod("createTag", Store.class, TagType.class);
      assertNotNull(method, "createTag method should exist");
      assertEquals(Tag.class, method.getReturnType(), "Should return Tag");
    }
  }

  @Nested
  @DisplayName("WASI Method Tests")
  class WasiMethodTests {

    @Test
    @DisplayName("should have createWasiContext method")
    void shouldHaveCreateWasiContextMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmRuntime.class.getMethod("createWasiContext");
      assertNotNull(method, "createWasiContext method should exist");
      assertEquals(WasiContext.class, method.getReturnType(), "Should return WasiContext");
    }

    @Test
    @DisplayName("should have addWasiToLinker method")
    void shouldHaveAddWasiToLinkerMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasmRuntime.class.getMethod("addWasiToLinker", Linker.class, WasiContext.class);
      assertNotNull(method, "addWasiToLinker method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Serializer Method Tests")
  class SerializerMethodTests {

    @Test
    @DisplayName("should have createSerializer method")
    void shouldHaveCreateSerializerMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmRuntime.class.getMethod("createSerializer");
      assertNotNull(method, "createSerializer method should exist");
      assertEquals(Serializer.class, method.getReturnType(), "Should return Serializer");
    }

    @Test
    @DisplayName("should have createSerializer method with options")
    void shouldHaveCreateSerializerMethodWithOptions() throws NoSuchMethodException {
      final Method method =
          PanamaWasmRuntime.class.getMethod(
              "createSerializer", long.class, boolean.class, int.class);
      assertNotNull(method, "createSerializer method should exist");
      assertEquals(Serializer.class, method.getReturnType(), "Should return Serializer");
    }
  }

  @Nested
  @DisplayName("Runtime Info Method Tests")
  class RuntimeInfoMethodTests {

    @Test
    @DisplayName("should have getRuntimeInfo method")
    void shouldHaveGetRuntimeInfoMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmRuntime.class.getMethod("getRuntimeInfo");
      assertNotNull(method, "getRuntimeInfo method should exist");
      assertEquals(RuntimeInfo.class, method.getReturnType(), "Should return RuntimeInfo");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmRuntime.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have supportsComponentModel method")
    void shouldHaveSupportsComponentModelMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmRuntime.class.getMethod("supportsComponentModel");
      assertNotNull(method, "supportsComponentModel method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmRuntime.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public default constructor")
    void shouldHavePublicDefaultConstructor() throws NoSuchMethodException {
      var constructor = PanamaWasmRuntime.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }
}
