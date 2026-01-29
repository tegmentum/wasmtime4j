package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasmRuntime} interface.
 *
 * <p>This test class verifies the structure and contract of the WasmRuntime interface, which is the
 * primary entry point for interacting with WebAssembly modules.
 */
@DisplayName("WasmRuntime Interface Tests")
class WasmRuntimeTest {

  @Nested
  @DisplayName("Interface Definition Tests")
  class InterfaceDefinitionTests {

    @Test
    @DisplayName("WasmRuntime should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasmRuntime.class.isInterface(), "WasmRuntime should be an interface");
    }

    @Test
    @DisplayName("WasmRuntime should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(WasmRuntime.class),
          "WasmRuntime should extend Closeable");
    }

    @Test
    @DisplayName("WasmRuntime should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasmRuntime.class.getModifiers()),
          "WasmRuntime should be a public interface");
    }
  }

  @Nested
  @DisplayName("Engine Creation Method Tests")
  class EngineCreationMethodTests {

    @Test
    @DisplayName("Should have createEngine() method")
    void shouldHaveCreateEngineMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createEngine");
      assertNotNull(method, "createEngine() method should exist");
      assertEquals(Engine.class, method.getReturnType(), "Should return Engine");
    }

    @Test
    @DisplayName("Should have createEngine(EngineConfig) method")
    void shouldHaveCreateEngineWithConfigMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createEngine", EngineConfig.class);
      assertNotNull(method, "createEngine(EngineConfig) method should exist");
      assertEquals(Engine.class, method.getReturnType(), "Should return Engine");
    }
  }

  @Nested
  @DisplayName("Module Compilation Method Tests")
  class ModuleCompilationMethodTests {

    @Test
    @DisplayName("Should have compileModule(Engine, byte[]) method")
    void shouldHaveCompileModuleMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("compileModule", Engine.class, byte[].class);
      assertNotNull(method, "compileModule(Engine, byte[]) method should exist");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("Should have compileModuleWat(Engine, String) method")
    void shouldHaveCompileModuleWatMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("compileModuleWat", Engine.class, String.class);
      assertNotNull(method, "compileModuleWat(Engine, String) method should exist");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }
  }

  @Nested
  @DisplayName("Store Creation Method Tests")
  class StoreCreationMethodTests {

    @Test
    @DisplayName("Should have createStore(Engine) method")
    void shouldHaveCreateStoreMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createStore", Engine.class);
      assertNotNull(method, "createStore(Engine) method should exist");
      assertEquals(Store.class, method.getReturnType(), "Should return Store");
    }

    @Test
    @DisplayName("Should have createStore(Engine, long, long, long) method")
    void shouldHaveCreateStoreWithLimitsMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod(
              "createStore", Engine.class, long.class, long.class, long.class);
      assertNotNull(method, "createStore(Engine, long, long, long) method should exist");
      assertEquals(Store.class, method.getReturnType(), "Should return Store");
    }

    @Test
    @DisplayName("Should have createStore(Engine, StoreLimits) method")
    void shouldHaveCreateStoreWithStoreLimitsMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("createStore", Engine.class, StoreLimits.class);
      assertNotNull(method, "createStore(Engine, StoreLimits) method should exist");
      assertEquals(Store.class, method.getReturnType(), "Should return Store");
    }
  }

  @Nested
  @DisplayName("Linker Method Tests")
  class LinkerMethodTests {

    @Test
    @DisplayName("Should have createLinker(Engine) method")
    void shouldHaveCreateLinkerMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createLinker", Engine.class);
      assertNotNull(method, "createLinker(Engine) method should exist");
      assertEquals(Linker.class, method.getReturnType(), "Should return Linker");
    }

    @Test
    @DisplayName("Should have createLinker(Engine, boolean, boolean) method")
    void shouldHaveCreateLinkerWithConfigMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("createLinker", Engine.class, boolean.class, boolean.class);
      assertNotNull(method, "createLinker(Engine, boolean, boolean) method should exist");
      assertEquals(Linker.class, method.getReturnType(), "Should return Linker");
    }

    @Test
    @DisplayName("Should have createComponentLinker(Engine) method")
    void shouldHaveCreateComponentLinkerMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createComponentLinker", Engine.class);
      assertNotNull(method, "createComponentLinker(Engine) method should exist");
      assertEquals(ComponentLinker.class, method.getReturnType(), "Should return ComponentLinker");
    }
  }

  @Nested
  @DisplayName("Instantiation Method Tests")
  class InstantiationMethodTests {

    @Test
    @DisplayName("Should have instantiate(Module) method")
    void shouldHaveInstantiateMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("instantiate", Module.class);
      assertNotNull(method, "instantiate(Module) method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }

    @Test
    @DisplayName("Should have instantiate(Module, ImportMap) method")
    void shouldHaveInstantiateWithImportsMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("instantiate", Module.class, ImportMap.class);
      assertNotNull(method, "instantiate(Module, ImportMap) method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }
  }

  @Nested
  @DisplayName("Tag Method Tests")
  class TagMethodTests {

    @Test
    @DisplayName("Should have createTag(Store, TagType) method")
    void shouldHaveCreateTagMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createTag", Store.class, TagType.class);
      assertNotNull(method, "createTag(Store, TagType) method should exist");
      assertEquals(Tag.class, method.getReturnType(), "Should return Tag");
    }
  }

  @Nested
  @DisplayName("Component Engine Method Tests")
  class ComponentEngineMethodTests {

    @Test
    @DisplayName("Should have createComponentEngine() method")
    void shouldHaveCreateComponentEngineMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createComponentEngine");
      assertNotNull(method, "createComponentEngine() method should exist");
      assertEquals(ComponentEngine.class, method.getReturnType(), "Should return ComponentEngine");
    }

    @Test
    @DisplayName("Should have createComponentEngine(ComponentEngineConfig) method")
    void shouldHaveCreateComponentEngineWithConfigMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("createComponentEngine", ComponentEngineConfig.class);
      assertNotNull(method, "createComponentEngine(ComponentEngineConfig) method should exist");
      assertEquals(ComponentEngine.class, method.getReturnType(), "Should return ComponentEngine");
    }
  }

  @Nested
  @DisplayName("Serialization Method Tests")
  class SerializationMethodTests {

    @Test
    @DisplayName("Should have createSerializer() method")
    void shouldHaveCreateSerializerMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createSerializer");
      assertNotNull(method, "createSerializer() method should exist");
      assertEquals(Serializer.class, method.getReturnType(), "Should return Serializer");
    }

    @Test
    @DisplayName("Should have createSerializer(long, boolean, int) method")
    void shouldHaveCreateSerializerWithConfigMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("createSerializer", long.class, boolean.class, int.class);
      assertNotNull(method, "createSerializer(long, boolean, int) method should exist");
      assertEquals(Serializer.class, method.getReturnType(), "Should return Serializer");
    }
  }

  @Nested
  @DisplayName("WASI Method Tests")
  class WasiMethodTests {

    @Test
    @DisplayName("Should have createWasiContext() method")
    void shouldHaveCreateWasiContextMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createWasiContext");
      assertNotNull(method, "createWasiContext() method should exist");
      assertEquals(WasiContext.class, method.getReturnType(), "Should return WasiContext");
    }

    @Test
    @DisplayName("Should have addWasiToLinker(Linker, WasiContext) method")
    void shouldHaveAddWasiToLinkerMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("addWasiToLinker", Linker.class, WasiContext.class);
      assertNotNull(method, "addWasiToLinker(Linker, WasiContext) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have addWasiPreview2ToLinker(Linker, WasiContext) method")
    void shouldHaveAddWasiPreview2ToLinkerMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("addWasiPreview2ToLinker", Linker.class, WasiContext.class);
      assertNotNull(method, "addWasiPreview2ToLinker(Linker, WasiContext) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have addComponentModelToLinker(Linker) method")
    void shouldHaveAddComponentModelToLinkerMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("addComponentModelToLinker", Linker.class);
      assertNotNull(method, "addComponentModelToLinker(Linker) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have createWasiLinker(Engine) method")
    void shouldHaveCreateWasiLinkerMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createWasiLinker", Engine.class);
      assertNotNull(method, "createWasiLinker(Engine) method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.wasi.WasiLinker.class,
          method.getReturnType(),
          "Should return WasiLinker");
    }
  }

  @Nested
  @DisplayName("GC and SIMD Method Tests")
  class GcAndSimdMethodTests {

    @Test
    @DisplayName("Should have getGcRuntime() method")
    void shouldHaveGetGcRuntimeMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("getGcRuntime");
      assertNotNull(method, "getGcRuntime() method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.gc.GcRuntime.class,
          method.getReturnType(),
          "Should return GcRuntime");
    }

    @Test
    @DisplayName("Should have getSimdOperations() method")
    void shouldHaveGetSimdOperationsMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("getSimdOperations");
      assertNotNull(method, "getSimdOperations() method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.simd.SimdOperations.class,
          method.getReturnType(),
          "Should return SimdOperations");
    }
  }

  @Nested
  @DisplayName("Neural Network Method Tests")
  class NeuralNetworkMethodTests {

    @Test
    @DisplayName("Should have createNnContext() method")
    void shouldHaveCreateNnContextMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("createNnContext");
      assertNotNull(method, "createNnContext() method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.wasi.nn.NnContext.class,
          method.getReturnType(),
          "Should return NnContext");
    }
  }

  @Nested
  @DisplayName("Deserialization Method Tests")
  class DeserializationMethodTests {

    @Test
    @DisplayName("Should have deserializeModule(Engine, byte[]) method")
    void shouldHaveDeserializeModuleMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("deserializeModule", Engine.class, byte[].class);
      assertNotNull(method, "deserializeModule(Engine, byte[]) method should exist");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("Should have deserializeModuleFile(Engine, Path) method")
    void shouldHaveDeserializeModuleFileMethod() throws NoSuchMethodException {
      final Method method =
          WasmRuntime.class.getMethod("deserializeModuleFile", Engine.class, Path.class);
      assertNotNull(method, "deserializeModuleFile(Engine, Path) method should exist");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }
  }

  @Nested
  @DisplayName("Runtime Information Method Tests")
  class RuntimeInformationMethodTests {

    @Test
    @DisplayName("Should have getRuntimeInfo() method")
    void shouldHaveGetRuntimeInfoMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("getRuntimeInfo");
      assertNotNull(method, "getRuntimeInfo() method should exist");
      assertEquals(RuntimeInfo.class, method.getReturnType(), "Should return RuntimeInfo");
    }

    @Test
    @DisplayName("Should have isValid() method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("isValid");
      assertNotNull(method, "isValid() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have getDebuggingCapabilities() method")
    void shouldHaveGetDebuggingCapabilitiesMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("getDebuggingCapabilities");
      assertNotNull(method, "getDebuggingCapabilities() method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("Should have supportsComponentModel() method")
    void shouldHaveSupportsComponentModelMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("supportsComponentModel");
      assertNotNull(method, "supportsComponentModel() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have isNnAvailable() method")
    void shouldHaveIsNnAvailableMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("isNnAvailable");
      assertNotNull(method, "isNnAvailable() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("Should have static builder() method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("builder");
      assertNotNull(method, "builder() method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder() should be static");
      assertEquals(
          WasmRuntimeBuilder.class, method.getReturnType(), "Should return WasmRuntimeBuilder");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("Should have close() method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = WasmRuntime.class.getMethod("close");
      assertNotNull(method, "close() method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
