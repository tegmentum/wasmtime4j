package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link Engine} interface.
 *
 * <p>This test class verifies the structure and contract of the Engine interface, which represents
 * a WebAssembly compilation engine.
 */
@DisplayName("Engine Interface Tests")
class EngineTest {

  @Nested
  @DisplayName("Interface Definition Tests")
  class InterfaceDefinitionTests {

    @Test
    @DisplayName("Engine should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Engine.class.isInterface(), "Engine should be an interface");
    }

    @Test
    @DisplayName("Engine should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(Closeable.class.isAssignableFrom(Engine.class), "Engine should extend Closeable");
    }

    @Test
    @DisplayName("Engine should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(Engine.class.getModifiers()), "Engine should be a public interface");
    }
  }

  @Nested
  @DisplayName("Store Creation Method Tests")
  class StoreCreationMethodTests {

    @Test
    @DisplayName("Should have createStore() method")
    void shouldHaveCreateStoreMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("createStore");
      assertNotNull(method, "createStore() method should exist");
      assertEquals(Store.class, method.getReturnType(), "Should return Store");
    }

    @Test
    @DisplayName("Should have createStore(Object) method")
    void shouldHaveCreateStoreWithDataMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("createStore", Object.class);
      assertNotNull(method, "createStore(Object) method should exist");
      assertEquals(Store.class, method.getReturnType(), "Should return Store");
    }
  }

  @Nested
  @DisplayName("Module Compilation Method Tests")
  class ModuleCompilationMethodTests {

    @Test
    @DisplayName("Should have compileModule(byte[]) method")
    void shouldHaveCompileModuleMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("compileModule", byte[].class);
      assertNotNull(method, "compileModule(byte[]) method should exist");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("Should have compileWat(String) method")
    void shouldHaveCompileWatMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("compileWat", String.class);
      assertNotNull(method, "compileWat(String) method should exist");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("Should have precompileModule(byte[]) method")
    void shouldHavePrecompileModuleMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("precompileModule", byte[].class);
      assertNotNull(method, "precompileModule(byte[]) method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
    }

    @Test
    @DisplayName("Should have compileFromStream(InputStream) method")
    void shouldHaveCompileFromStreamMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("compileFromStream", InputStream.class);
      assertNotNull(method, "compileFromStream(InputStream) method should exist");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean hasIoException = false;
      for (final Class<?> exceptionType : exceptionTypes) {
        if (exceptionType == IOException.class) {
          hasIoException = true;
          break;
        }
      }
      assertTrue(hasIoException, "compileFromStream should throw IOException");
    }
  }

  @Nested
  @DisplayName("Epoch Method Tests")
  class EpochMethodTests {

    @Test
    @DisplayName("Should have incrementEpoch() method")
    void shouldHaveIncrementEpochMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("incrementEpoch");
      assertNotNull(method, "incrementEpoch() method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have isEpochInterruptionEnabled() method")
    void shouldHaveIsEpochInterruptionEnabledMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("isEpochInterruptionEnabled");
      assertNotNull(method, "isEpochInterruptionEnabled() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Configuration Method Tests")
  class ConfigurationMethodTests {

    @Test
    @DisplayName("Should have getConfig() method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("getConfig");
      assertNotNull(method, "getConfig() method should exist");
      assertEquals(EngineConfig.class, method.getReturnType(), "Should return EngineConfig");
    }

    @Test
    @DisplayName("Should have getRuntime() method")
    void shouldHaveGetRuntimeMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("getRuntime");
      assertNotNull(method, "getRuntime() method should exist");
      assertEquals(WasmRuntime.class, method.getReturnType(), "Should return WasmRuntime");
    }
  }

  @Nested
  @DisplayName("Validity Method Tests")
  class ValidityMethodTests {

    @Test
    @DisplayName("Should have isValid() method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("isValid");
      assertNotNull(method, "isValid() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have close() method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("close");
      assertNotNull(method, "close() method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Feature Support Method Tests")
  class FeatureSupportMethodTests {

    @Test
    @DisplayName("Should have supportsFeature(WasmFeature) method")
    void shouldHaveSupportsFeatureMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("supportsFeature", WasmFeature.class);
      assertNotNull(method, "supportsFeature(WasmFeature) method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have isFuelEnabled() method")
    void shouldHaveIsFuelEnabledMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("isFuelEnabled");
      assertNotNull(method, "isFuelEnabled() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have isCoredumpOnTrapEnabled() method")
    void shouldHaveIsCoredumpOnTrapEnabledMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("isCoredumpOnTrapEnabled");
      assertNotNull(method, "isCoredumpOnTrapEnabled() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have isAsync() method")
    void shouldHaveIsAsyncMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("isAsync");
      assertNotNull(method, "isAsync() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Resource Limit Method Tests")
  class ResourceLimitMethodTests {

    @Test
    @DisplayName("Should have getMemoryLimitPages() method")
    void shouldHaveGetMemoryLimitPagesMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("getMemoryLimitPages");
      assertNotNull(method, "getMemoryLimitPages() method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("Should have getStackSizeLimit() method")
    void shouldHaveGetStackSizeLimitMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("getStackSizeLimit");
      assertNotNull(method, "getStackSizeLimit() method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("Should have getMaxInstances() method")
    void shouldHaveGetMaxInstancesMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("getMaxInstances");
      assertNotNull(method, "getMaxInstances() method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("Should have getReferenceCount() method")
    void shouldHaveGetReferenceCountMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("getReferenceCount");
      assertNotNull(method, "getReferenceCount() method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Precompilation Method Tests")
  class PrecompilationMethodTests {

    @Test
    @DisplayName("Should have detectPrecompiled(byte[]) method")
    void shouldHaveDetectPrecompiledMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("detectPrecompiled", byte[].class);
      assertNotNull(method, "detectPrecompiled(byte[]) method should exist");
      assertEquals(Precompiled.class, method.getReturnType(), "Should return Precompiled");
    }

    @Test
    @DisplayName("Should have same(Engine) method")
    void shouldHaveSameMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("same", Engine.class);
      assertNotNull(method, "same(Engine) method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("Should have isPulley() default method")
    void shouldHaveIsPulleyDefaultMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("isPulley");
      assertNotNull(method, "isPulley() method should exist");
      assertTrue(method.isDefault(), "isPulley() should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have precompileCompatibilityHash() default method")
    void shouldHavePrecompileCompatibilityHashDefaultMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("precompileCompatibilityHash");
      assertNotNull(method, "precompileCompatibilityHash() method should exist");
      assertTrue(method.isDefault(), "precompileCompatibilityHash() should be a default method");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
    }

    @Test
    @DisplayName("Should have captureStatistics() default method")
    void shouldHaveCaptureStatisticsDefaultMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("captureStatistics");
      assertNotNull(method, "captureStatistics() method should exist");
      assertTrue(method.isDefault(), "captureStatistics() should be a default method");
    }

    @Test
    @DisplayName("Should have getPoolingAllocatorMetrics() default method")
    void shouldHaveGetPoolingAllocatorMetricsDefaultMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("getPoolingAllocatorMetrics");
      assertNotNull(method, "getPoolingAllocatorMetrics() method should exist");
      assertTrue(method.isDefault(), "getPoolingAllocatorMetrics() should be a default method");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("Should have static create() method")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("create");
      assertNotNull(method, "create() method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create() should be static");
      assertEquals(Engine.class, method.getReturnType(), "Should return Engine");
    }

    @Test
    @DisplayName("Should have static create(EngineConfig) method")
    void shouldHaveStaticCreateWithConfigMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("create", EngineConfig.class);
      assertNotNull(method, "create(EngineConfig) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create(EngineConfig) should be static");
      assertEquals(Engine.class, method.getReturnType(), "Should return Engine");
    }

    @Test
    @DisplayName("Should have static builder() method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      final Method method = Engine.class.getMethod("builder");
      assertNotNull(method, "builder() method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder() should be static");
      assertEquals(EngineConfig.class, method.getReturnType(), "Should return EngineConfig");
    }
  }
}
