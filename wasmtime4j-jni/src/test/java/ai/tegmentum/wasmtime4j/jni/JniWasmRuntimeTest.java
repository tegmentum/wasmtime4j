package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.WasmRuntime;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for JniWasmRuntime class.
 *
 * <p>These tests verify the class structure, method signatures, and behavior without triggering
 * native library initialization.
 *
 * @since 1.0.0
 */
@DisplayName("JniWasmRuntime Tests")
class JniWasmRuntimeTest {

  private static final String CLASS_NAME = "ai.tegmentum.wasmtime4j.jni.JniWasmRuntime";
  private static final String JNI_RESOURCE_CLASS = "ai.tegmentum.wasmtime4j.jni.util.JniResource";

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      assertThat(Modifier.isFinal(clazz.getModifiers()))
          .as("JniWasmRuntime should be final")
          .isTrue();
    }

    @Test
    @DisplayName("should implement WasmRuntime interface")
    void shouldImplementWasmRuntimeInterface() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      assertThat(WasmRuntime.class.isAssignableFrom(clazz))
          .as("JniWasmRuntime should implement WasmRuntime")
          .isTrue();
    }

    @Test
    @DisplayName("should extend JniResource")
    void shouldExtendJniResource() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> resourceClass = Class.forName(JNI_RESOURCE_CLASS);
      assertThat(resourceClass.isAssignableFrom(clazz))
          .as("JniWasmRuntime should extend JniResource")
          .isTrue();
    }

    @Test
    @DisplayName("should have Logger field")
    void shouldHaveLoggerField() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Field loggerField = clazz.getDeclaredField("LOGGER");

      assertThat(Modifier.isPrivate(loggerField.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(loggerField.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(loggerField.getModifiers())).isTrue();
      assertThat(loggerField.getType()).isEqualTo(Logger.class);
    }

    @Test
    @DisplayName("should have resourceCache field")
    void shouldHaveResourceCacheField() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Field field = clazz.getDeclaredField("resourceCache");

      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have concurrencyManager field")
    void shouldHaveConcurrencyManagerField() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Field field = clazz.getDeclaredField("concurrencyManager");

      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have phantomManager field")
    void shouldHavePhantomManagerField() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Field field = clazz.getDeclaredField("phantomManager");

      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have defaultGcRuntime field with volatile modifier")
    void shouldHaveDefaultGcRuntimeField() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Field field = clazz.getDeclaredField("defaultGcRuntime");

      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isVolatile(field.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have gcRuntimeLock field")
    void shouldHaveGcRuntimeLockField() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Field field = clazz.getDeclaredField("gcRuntimeLock");

      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(Object.class);
    }

    @Test
    @DisplayName("should have defaultSimdOperations field with volatile modifier")
    void shouldHaveDefaultSimdOperationsField() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Field field = clazz.getDeclaredField("defaultSimdOperations");

      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isVolatile(field.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have simdOperationsLock field")
    void shouldHaveSimdOperationsLockField() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Field field = clazz.getDeclaredField("simdOperationsLock");

      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(Object.class);
    }
  }

  @Nested
  @DisplayName("Engine Creation Methods Tests")
  class EngineCreationMethodsTests {

    @Test
    @DisplayName("should have parameterless createEngine method")
    void shouldHaveCreateEngineMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("createEngine");

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.Engine");
    }

    @Test
    @DisplayName("should have createEngine method with config")
    void shouldHaveCreateEngineWithConfigMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> configClass = Class.forName("ai.tegmentum.wasmtime4j.EngineConfig");
      Method method = clazz.getMethod("createEngine", configClass);

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.Engine");
    }
  }

  @Nested
  @DisplayName("Store Creation Methods Tests")
  class StoreCreationMethodsTests {

    @Test
    @DisplayName("should have createStore method with engine")
    void shouldHaveCreateStoreMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> engineClass = Class.forName("ai.tegmentum.wasmtime4j.Engine");
      Method method = clazz.getMethod("createStore", engineClass);

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.Store");
    }

    @Test
    @DisplayName("should have createStore method with limits")
    void shouldHaveCreateStoreWithLimitsMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> engineClass = Class.forName("ai.tegmentum.wasmtime4j.Engine");
      Class<?> limitsClass = Class.forName("ai.tegmentum.wasmtime4j.StoreLimits");
      Method method = clazz.getMethod("createStore", engineClass, limitsClass);

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.Store");
    }

    @Test
    @DisplayName("should have createStore method with resource limits")
    void shouldHaveCreateStoreWithResourceLimitsMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> engineClass = Class.forName("ai.tegmentum.wasmtime4j.Engine");
      Method method =
          clazz.getMethod("createStore", engineClass, long.class, long.class, long.class);

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.Store");
    }
  }

  @Nested
  @DisplayName("Module Compilation Methods Tests")
  class ModuleCompilationMethodsTests {

    @Test
    @DisplayName("should have compileModuleWat method")
    void shouldHaveCompileModuleWatMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> engineClass = Class.forName("ai.tegmentum.wasmtime4j.Engine");
      Method method = clazz.getMethod("compileModuleWat", engineClass, String.class);

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.Module");
    }

    @Test
    @DisplayName("should have compileModule method")
    void shouldHaveCompileModuleMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> engineClass = Class.forName("ai.tegmentum.wasmtime4j.Engine");
      Method method = clazz.getMethod("compileModule", engineClass, byte[].class);

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.Module");
    }
  }

  @Nested
  @DisplayName("Instance Methods Tests")
  class InstanceMethodsTests {

    @Test
    @DisplayName("should have instantiate method with module only")
    void shouldHaveInstantiateMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> moduleClass = Class.forName("ai.tegmentum.wasmtime4j.Module");
      Method method = clazz.getMethod("instantiate", moduleClass);

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.Instance");
    }

    @Test
    @DisplayName("should have instantiate method with imports")
    void shouldHaveInstantiateWithImportsMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> moduleClass = Class.forName("ai.tegmentum.wasmtime4j.Module");
      Class<?> importsClass = Class.forName("ai.tegmentum.wasmtime4j.ImportMap");
      Method method = clazz.getMethod("instantiate", moduleClass, importsClass);

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.Instance");
    }
  }

  @Nested
  @DisplayName("GC Runtime Methods Tests")
  class GcRuntimeMethodsTests {

    @Test
    @DisplayName("should have createGcRuntime method")
    void shouldHaveCreateGcRuntimeMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> engineClass = Class.forName("ai.tegmentum.wasmtime4j.Engine");
      Method method = clazz.getMethod("createGcRuntime", engineClass);

      assertThat(method.getReturnType().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.gc.GcRuntime");
    }

    @Test
    @DisplayName("should have getGcRuntime method")
    void shouldHaveGetGcRuntimeMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getGcRuntime");

      assertThat(method.getReturnType().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.gc.GcRuntime");
    }
  }

  @Nested
  @DisplayName("SIMD Operations Methods Tests")
  class SimdOperationsMethodsTests {

    @Test
    @DisplayName("should have getSimdOperations method")
    void shouldHaveGetSimdOperationsMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getSimdOperations");

      assertThat(method.getReturnType().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.simd.SimdOperations");
    }

    @Test
    @DisplayName("should have isSimdSupported method")
    void shouldHaveIsSimdSupportedMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("isSimdSupported");

      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("should have getSimdCapabilities method")
    void shouldHaveGetSimdCapabilitiesMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getSimdCapabilities");

      assertThat(method.getReturnType()).isEqualTo(String.class);
    }
  }

  @Nested
  @DisplayName("Tag Methods Tests")
  class TagMethodsTests {

    @Test
    @DisplayName("should have createTag method")
    void shouldHaveCreateTagMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> storeClass = Class.forName("ai.tegmentum.wasmtime4j.Store");
      Class<?> tagTypeClass = Class.forName("ai.tegmentum.wasmtime4j.type.TagType");
      Method method = clazz.getMethod("createTag", storeClass, tagTypeClass);

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.Tag");
    }
  }

  @Nested
  @DisplayName("Runtime Info Methods Tests")
  class RuntimeInfoMethodsTests {

    @Test
    @DisplayName("should have getRuntimeInfo method")
    void shouldHaveGetRuntimeInfoMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getRuntimeInfo");

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.RuntimeInfo");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("isValid");

      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }
  }

  @Nested
  @DisplayName("Debugging Methods Tests")
  class DebuggingMethodsTests {

    @Test
    @DisplayName("should have createDebugger method")
    void shouldHaveCreateDebuggerMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> engineClass = Class.forName("ai.tegmentum.wasmtime4j.Engine");
      Method method = clazz.getMethod("createDebugger", engineClass);

      assertThat(method.getReturnType().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.debug.Debugger");
    }

    @Test
    @DisplayName("should have isDebuggingSupported method")
    void shouldHaveIsDebuggingSupportedMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("isDebuggingSupported");

      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("should have getDebuggingCapabilities method")
    void shouldHaveGetDebuggingCapabilitiesMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getDebuggingCapabilities");

      assertThat(method.getReturnType()).isEqualTo(String.class);
    }
  }

  @Nested
  @DisplayName("Serialization Methods Tests")
  class SerializationMethodsTests {

    @Test
    @DisplayName("should have deserializeModule method")
    void shouldHaveDeserializeModuleMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> engineClass = Class.forName("ai.tegmentum.wasmtime4j.Engine");
      Method method = clazz.getMethod("deserializeModule", engineClass, byte[].class);

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.Module");
    }

    @Test
    @DisplayName("should have deserializeModuleFile method")
    void shouldHaveDeserializeModuleFileMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> engineClass = Class.forName("ai.tegmentum.wasmtime4j.Engine");
      Method method =
          clazz.getMethod("deserializeModuleFile", engineClass, java.nio.file.Path.class);

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.Module");
    }

    @Test
    @DisplayName("should have createSerializer method")
    void shouldHaveCreateSerializerMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("createSerializer");

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.Serializer");
    }

    @Test
    @DisplayName("should have createSerializer method with config")
    void shouldHaveCreateSerializerWithConfigMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("createSerializer", long.class, boolean.class, int.class);

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.Serializer");
    }
  }

  @Nested
  @DisplayName("WASI Methods Tests")
  class WasiMethodsTests {

    @Test
    @DisplayName("should have createWasiContext method")
    void shouldHaveCreateWasiContextMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("createWasiContext");

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.WasiContext");
    }

    @Test
    @DisplayName("should have createWasiLinker method")
    void shouldHaveCreateWasiLinkerMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> engineClass = Class.forName("ai.tegmentum.wasmtime4j.Engine");
      Method method = clazz.getMethod("createWasiLinker", engineClass);

      assertThat(method.getReturnType().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.wasi.WasiLinker");
    }

    @Test
    @DisplayName("should have createWasiLinker method with config")
    void shouldHaveCreateWasiLinkerWithConfigMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> engineClass = Class.forName("ai.tegmentum.wasmtime4j.Engine");
      Class<?> configClass = Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiConfig");
      Method method = clazz.getMethod("createWasiLinker", engineClass, configClass);

      assertThat(method.getReturnType().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.wasi.WasiLinker");
    }
  }

  @Nested
  @DisplayName("Linker Methods Tests")
  class LinkerMethodsTests {

    @Test
    @DisplayName("should have createLinker method")
    void shouldHaveCreateLinkerMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> engineClass = Class.forName("ai.tegmentum.wasmtime4j.Engine");
      Method method = clazz.getMethod("createLinker", engineClass);

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.Linker");
    }

    @Test
    @DisplayName("should have createLinker method with config options")
    void shouldHaveCreateLinkerWithConfigMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> engineClass = Class.forName("ai.tegmentum.wasmtime4j.Engine");
      Method method = clazz.getMethod("createLinker", engineClass, boolean.class, boolean.class);

      assertThat(method.getReturnType().getName()).isEqualTo("ai.tegmentum.wasmtime4j.Linker");
    }

    @Test
    @DisplayName("should have addWasiToLinker method")
    void shouldHaveAddWasiToLinkerMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> linkerClass = Class.forName("ai.tegmentum.wasmtime4j.Linker");
      Class<?> contextClass = Class.forName("ai.tegmentum.wasmtime4j.WasiContext");
      Method method = clazz.getMethod("addWasiToLinker", linkerClass, contextClass);

      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("should have addWasiPreview2ToLinker method")
    void shouldHaveAddWasiPreview2ToLinkerMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> linkerClass = Class.forName("ai.tegmentum.wasmtime4j.Linker");
      Class<?> contextClass = Class.forName("ai.tegmentum.wasmtime4j.WasiContext");
      Method method = clazz.getMethod("addWasiPreview2ToLinker", linkerClass, contextClass);

      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("should have addComponentModelToLinker method")
    void shouldHaveAddComponentModelToLinkerMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> linkerClass = Class.forName("ai.tegmentum.wasmtime4j.Linker");
      Method method = clazz.getMethod("addComponentModelToLinker", linkerClass);

      assertThat(method.getReturnType()).isEqualTo(void.class);
    }
  }

  @Nested
  @DisplayName("Component Methods Tests")
  class ComponentMethodsTests {

    @Test
    @DisplayName("should have createComponentEngine method")
    void shouldHaveCreateComponentEngineMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("createComponentEngine");

      assertThat(method.getReturnType().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.component.ComponentEngine");
    }

    @Test
    @DisplayName("should have createComponentEngine method with config")
    void shouldHaveCreateComponentEngineWithConfigMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> configClass = Class.forName("ai.tegmentum.wasmtime4j.component.ComponentEngineConfig");
      Method method = clazz.getMethod("createComponentEngine", configClass);

      assertThat(method.getReturnType().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.component.ComponentEngine");
    }

    @Test
    @DisplayName("should have createComponentLinker method")
    void shouldHaveCreateComponentLinkerMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> engineClass = Class.forName("ai.tegmentum.wasmtime4j.Engine");
      Method method = clazz.getMethod("createComponentLinker", engineClass);

      assertThat(method.getReturnType().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.component.ComponentLinker");
    }

    @Test
    @DisplayName("should have supportsComponentModel method")
    void shouldHaveSupportsComponentModelMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("supportsComponentModel");

      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }
  }

  @Nested
  @DisplayName("WASI-NN Methods Tests")
  class WasiNnMethodsTests {

    @Test
    @DisplayName("should have createNnContext method")
    void shouldHaveCreateNnContextMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("createNnContext");

      assertThat(method.getReturnType().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.wasi.nn.NnContext");
    }

    @Test
    @DisplayName("should have isNnAvailable method")
    void shouldHaveIsNnAvailableMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("isNnAvailable");

      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }
  }

  @Nested
  @DisplayName("Native Methods Tests")
  class NativeMethodsTests {

    @Test
    @DisplayName("should have nativeCreateRuntime method")
    void shouldHaveNativeCreateRuntimeMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeCreateRuntime");

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
      assertThat(method.getReturnType()).isEqualTo(long.class);
    }

    @Test
    @DisplayName("should have nativeCreateEngine method")
    void shouldHaveNativeCreateEngineMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeCreateEngine", long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeCompileModule method")
    void shouldHaveNativeCompileModuleMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeCompileModule", long.class, byte[].class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeInstantiateModule method")
    void shouldHaveNativeInstantiateModuleMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeInstantiateModule", long.class, long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeGetWasmtimeVersion method")
    void shouldHaveNativeGetWasmtimeVersionMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeGetWasmtimeVersion");

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
      assertThat(method.getReturnType()).isEqualTo(String.class);
    }

    @Test
    @DisplayName("should have nativeDestroyRuntime method")
    void shouldHaveNativeDestroyRuntimeMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeDestroyRuntime", long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }
  }

  @Nested
  @DisplayName("SIMD Native Methods Tests")
  class SimdNativeMethodsTests {

    @Test
    @DisplayName("should have nativeIsSimdSupported method")
    void shouldHaveNativeIsSimdSupportedMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeIsSimdSupported", long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeGetSimdCapabilities method")
    void shouldHaveNativeGetSimdCapabilitiesMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeGetSimdCapabilities", long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeSimdAdd method")
    void shouldHaveNativeSimdAddMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method =
          clazz.getDeclaredMethod("nativeSimdAdd", long.class, byte[].class, byte[].class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeSimdSubtract method")
    void shouldHaveNativeSimdSubtractMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method =
          clazz.getDeclaredMethod("nativeSimdSubtract", long.class, byte[].class, byte[].class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeSimdMultiply method")
    void shouldHaveNativeSimdMultiplyMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method =
          clazz.getDeclaredMethod("nativeSimdMultiply", long.class, byte[].class, byte[].class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeSimdDivide method")
    void shouldHaveNativeSimdDivideMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method =
          clazz.getDeclaredMethod("nativeSimdDivide", long.class, byte[].class, byte[].class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeSimdFma method")
    void shouldHaveNativeSimdFmaMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method =
          clazz.getDeclaredMethod(
              "nativeSimdFma", long.class, byte[].class, byte[].class, byte[].class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("WASI Native Methods Tests")
  class WasiNativeMethodsTests {

    @Test
    @DisplayName("should have nativeCreateWasiContext method")
    void shouldHaveNativeCreateWasiContextMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeCreateWasiContext", long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeCreateLinker method")
    void shouldHaveNativeCreateLinkerMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeCreateLinker", long.class, long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeAddWasiToLinker method")
    void shouldHaveNativeAddWasiToLinkerMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method =
          clazz.getDeclaredMethod("nativeAddWasiToLinker", long.class, long.class, long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeSupportsComponentModel method")
    void shouldHaveNativeSupportsComponentModelMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeSupportsComponentModel", long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("Store Native Methods Tests")
  class StoreNativeMethodsTests {

    @Test
    @DisplayName("should have nativeCreateStoreWithLimits method")
    void shouldHaveNativeCreateStoreWithLimitsMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method =
          clazz.getDeclaredMethod(
              "nativeCreateStoreWithLimits", long.class, long.class, long.class, long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeCreateStoreWithResourceLimits method")
    void shouldHaveNativeCreateStoreWithResourceLimitsMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method =
          clazz.getDeclaredMethod(
              "nativeCreateStoreWithResourceLimits",
              long.class,
              long.class,
              long.class,
              long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("Serialization Native Methods Tests")
  class SerializationNativeMethodsTests {

    @Test
    @DisplayName("should have nativeDeserializeModule method")
    void shouldHaveNativeDeserializeModuleMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeDeserializeModule", long.class, byte[].class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeCreateSerializer method")
    void shouldHaveNativeCreateSerializerMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeCreateSerializer");

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeCreateSerializerWithConfig method")
    void shouldHaveNativeCreateSerializerWithConfigMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method =
          clazz.getDeclaredMethod(
              "nativeCreateSerializerWithConfig", long.class, boolean.class, int.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("WASI-NN Native Methods Tests")
  class WasiNnNativeMethodsTests {

    @Test
    @DisplayName("should have nativeCreateNnContext method")
    void shouldHaveNativeCreateNnContextMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeCreateNnContext");

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeIsNnAvailable method")
    void shouldHaveNativeIsNnAvailableMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeIsNnAvailable");

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("Tag Native Methods Tests")
  class TagNativeMethodsTests {

    @Test
    @DisplayName("should have nativeCreateTag method")
    void shouldHaveNativeCreateTagMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method =
          clazz.getDeclaredMethod("nativeCreateTag", long.class, int[].class, int[].class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("Protected Methods Tests")
  class ProtectedMethodsTests {

    @Test
    @DisplayName("should have doClose method")
    void shouldHaveDoCloseMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("doClose");

      assertThat(Modifier.isProtected(method.getModifiers())).isTrue();
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("should have getResourceType method")
    void shouldHaveGetResourceTypeMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("getResourceType");

      assertThat(Modifier.isProtected(method.getModifiers())).isTrue();
      assertThat(method.getReturnType()).isEqualTo(String.class);
    }
  }

  @Nested
  @DisplayName("Private Methods Tests")
  class PrivateMethodsTests {

    @Test
    @DisplayName("should have initializeRuntime static method")
    void shouldHaveInitializeRuntimeMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("initializeRuntime");

      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
      assertThat(method.getReturnType()).isEqualTo(long.class);
    }

    @Test
    @DisplayName("should have validateRuntimeState method")
    void shouldHaveValidateRuntimeStateMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("validateRuntimeState");

      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }
  }

  @Nested
  @DisplayName("Native Method Count Tests")
  class NativeMethodCountTests {

    @Test
    @DisplayName("should have significant number of native methods")
    void shouldHaveSignificantNumberOfNativeMethods() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      long nativeMethodCount =
          Arrays.stream(clazz.getDeclaredMethods())
              .filter(m -> Modifier.isNative(m.getModifiers()))
              .count();

      // JniWasmRuntime has many native methods for SIMD, WASI, serialization, etc.
      assertThat(nativeMethodCount)
          .as("Should have at least 30 native methods")
          .isGreaterThanOrEqualTo(30);
    }

    @Test
    @DisplayName("should have SIMD native methods")
    void shouldHaveSimdNativeMethods() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      List<String> simdMethods =
          Arrays.asList(
              "nativeSimdAdd",
              "nativeSimdSubtract",
              "nativeSimdMultiply",
              "nativeSimdDivide",
              "nativeSimdAnd",
              "nativeSimdOr",
              "nativeSimdXor",
              "nativeSimdNot",
              "nativeSimdFma",
              "nativeSimdSqrt",
              "nativeSimdShuffle");

      for (String methodName : simdMethods) {
        boolean hasMethod =
            Arrays.stream(clazz.getDeclaredMethods())
                .anyMatch(
                    m -> m.getName().equals(methodName) && Modifier.isNative(m.getModifiers()));
        assertThat(hasMethod).as("Should have native SIMD method: " + methodName).isTrue();
      }
    }
  }
}
