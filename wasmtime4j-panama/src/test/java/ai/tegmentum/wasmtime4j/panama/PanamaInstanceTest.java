package ai.tegmentum.wasmtime4j.panama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.InstancePre;
import ai.tegmentum.wasmtime4j.InstanceState;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for {@link PanamaInstance}, {@link PanamaInstanceGlobal}, and {@link
 * PanamaInstancePre}.
 *
 * <p>These tests use reflection to verify class structure, field types, and method signatures
 * without requiring native library initialization. This approach ensures tests remain fast and
 * don't rely on platform-specific native code.
 */
@DisplayName("Panama Instance Tests")
class PanamaInstanceTest {

  @Nested
  @DisplayName("PanamaInstance Class Structure Tests")
  class PanamaInstanceClassStructureTests {

    @Test
    @DisplayName("PanamaInstance should implement Instance interface")
    void panamaInstanceShouldImplementInstanceInterface() {
      assertThat(Instance.class.isAssignableFrom(PanamaInstance.class)).isTrue();
    }

    @Test
    @DisplayName("PanamaInstance should be final class")
    void panamaInstanceShouldBeFinalClass() {
      assertThat(Modifier.isFinal(PanamaInstance.class.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("PanamaInstance should have Logger field")
    void panamaInstanceShouldHaveLoggerField() throws NoSuchFieldException {
      final Field loggerField = PanamaInstance.class.getDeclaredField("LOGGER");
      assertThat(Modifier.isStatic(loggerField.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(loggerField.getModifiers())).isTrue();
      assertThat(loggerField.getType()).isEqualTo(Logger.class);
    }

    @Test
    @DisplayName("PanamaInstance should have module field")
    void panamaInstanceShouldHaveModuleField() throws NoSuchFieldException {
      final Field moduleField = PanamaInstance.class.getDeclaredField("module");
      assertThat(Modifier.isPrivate(moduleField.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(moduleField.getModifiers())).isTrue();
      assertThat(moduleField.getType()).isEqualTo(PanamaModule.class);
    }

    @Test
    @DisplayName("PanamaInstance should have store field")
    void panamaInstanceShouldHaveStoreField() throws NoSuchFieldException {
      final Field storeField = PanamaInstance.class.getDeclaredField("store");
      assertThat(Modifier.isPrivate(storeField.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(storeField.getModifiers())).isTrue();
      assertThat(storeField.getType()).isEqualTo(PanamaStore.class);
    }

    @Test
    @DisplayName("PanamaInstance should have callArena field")
    void panamaInstanceShouldHaveCallArenaField() throws NoSuchFieldException {
      final Field callArenaField = PanamaInstance.class.getDeclaredField("callArena");
      assertThat(Modifier.isPrivate(callArenaField.getModifiers())).isTrue();
      assertThat(Modifier.isVolatile(callArenaField.getModifiers())).isTrue();
      assertThat(callArenaField.getType()).isEqualTo(Arena.class);
    }

    @Test
    @DisplayName("PanamaInstance should have nativeInstance field")
    void panamaInstanceShouldHaveNativeInstanceField() throws NoSuchFieldException {
      final Field nativeInstanceField = PanamaInstance.class.getDeclaredField("nativeInstance");
      assertThat(Modifier.isPrivate(nativeInstanceField.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(nativeInstanceField.getModifiers())).isTrue();
      assertThat(nativeInstanceField.getType()).isEqualTo(MemorySegment.class);
    }

    @Test
    @DisplayName("PanamaInstance should have disposed field")
    void panamaInstanceShouldHaveDisposedField() throws NoSuchFieldException {
      final Field disposedField = PanamaInstance.class.getDeclaredField("disposed");
      assertThat(Modifier.isPrivate(disposedField.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(disposedField.getModifiers())).isTrue();
      assertThat(disposedField.getType()).isEqualTo(AtomicBoolean.class);
    }

    @Test
    @DisplayName("PanamaInstance should have closed field with volatile modifier")
    void panamaInstanceShouldHaveClosedField() throws NoSuchFieldException {
      final Field closedField = PanamaInstance.class.getDeclaredField("closed");
      assertThat(Modifier.isPrivate(closedField.getModifiers())).isTrue();
      assertThat(Modifier.isVolatile(closedField.getModifiers())).isTrue();
      assertThat(closedField.getType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("PanamaInstance should have createdAtMicros field")
    void panamaInstanceShouldHaveCreatedAtMicrosField() throws NoSuchFieldException {
      final Field createdAtField = PanamaInstance.class.getDeclaredField("createdAtMicros");
      assertThat(Modifier.isPrivate(createdAtField.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(createdAtField.getModifiers())).isTrue();
      assertThat(createdAtField.getType()).isEqualTo(long.class);
    }
  }

  @Nested
  @DisplayName("PanamaInstance Method Tests")
  class PanamaInstanceMethodTests {

    @Test
    @DisplayName("PanamaInstance should have getFunction method with String parameter")
    void panamaInstanceShouldHaveGetFunctionByNameMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getFunction", String.class);
      assertThat(method.getReturnType().getName()).contains("Optional");
    }

    @Test
    @DisplayName("PanamaInstance should have getFunction method with int parameter")
    void panamaInstanceShouldHaveGetFunctionByIndexMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getFunction", int.class);
      assertThat(method.getReturnType().getName()).contains("Optional");
    }

    @Test
    @DisplayName("PanamaInstance should have getGlobal method with String parameter")
    void panamaInstanceShouldHaveGetGlobalByNameMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getGlobal", String.class);
      assertThat(method.getReturnType().getName()).contains("Optional");
    }

    @Test
    @DisplayName("PanamaInstance should have getMemory method with String parameter")
    void panamaInstanceShouldHaveGetMemoryByNameMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getMemory", String.class);
      assertThat(method.getReturnType().getName()).contains("Optional");
    }

    @Test
    @DisplayName("PanamaInstance should have getTable method with String parameter")
    void panamaInstanceShouldHaveGetTableByNameMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getTable", String.class);
      assertThat(method.getReturnType().getName()).contains("Optional");
    }

    @Test
    @DisplayName("PanamaInstance should have getDefaultMemory method")
    void panamaInstanceShouldHaveGetDefaultMemoryMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getDefaultMemory");
      assertThat(method.getReturnType().getName()).contains("Optional");
    }

    @Test
    @DisplayName("PanamaInstance should have getExportNames method")
    void panamaInstanceShouldHaveGetExportNamesMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getExportNames");
      assertThat(method.getReturnType()).isEqualTo(String[].class);
    }

    @Test
    @DisplayName("PanamaInstance should have hasExport method")
    void panamaInstanceShouldHaveHasExportMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("hasExport", String.class);
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("PanamaInstance should have hasFunction method")
    void panamaInstanceShouldHaveHasFunctionMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("hasFunction", String.class);
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("PanamaInstance should have hasMemory method")
    void panamaInstanceShouldHaveHasMemoryMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("hasMemory", String.class);
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("PanamaInstance should have hasTable method")
    void panamaInstanceShouldHaveHasTableMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("hasTable", String.class);
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("PanamaInstance should have hasGlobal method")
    void panamaInstanceShouldHaveHasGlobalMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("hasGlobal", String.class);
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("PanamaInstance should have close method")
    void panamaInstanceShouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("close");
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("PanamaInstance should have isValid method")
    void panamaInstanceShouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("isValid");
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("PanamaInstance should have getState method")
    void panamaInstanceShouldHaveGetStateMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getState");
      assertThat(method.getReturnType()).isEqualTo(InstanceState.class);
    }

    @Test
    @DisplayName("PanamaInstance should have getModule method")
    void panamaInstanceShouldHaveGetModuleMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getModule");
      assertThat(method.getReturnType().getName()).contains("Module");
    }

    @Test
    @DisplayName("PanamaInstance should have getStore method")
    void panamaInstanceShouldHaveGetStoreMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getStore");
      assertThat(method.getReturnType().getName()).contains("Store");
    }

    @Test
    @DisplayName("PanamaInstance should have callFunction method")
    void panamaInstanceShouldHaveCallFunctionMethod() throws NoSuchMethodException {
      final Method method =
          PanamaInstance.class.getMethod(
              "callFunction", String.class, ai.tegmentum.wasmtime4j.WasmValue[].class);
      assertThat(method.getReturnType()).isEqualTo(ai.tegmentum.wasmtime4j.WasmValue[].class);
    }

    @Test
    @DisplayName("PanamaInstance should have callI32Function method with varargs")
    void panamaInstanceShouldHaveCallI32FunctionWithVarargs() throws NoSuchMethodException {
      final Method method =
          PanamaInstance.class.getMethod("callI32Function", String.class, int[].class);
      assertThat(method.getReturnType()).isEqualTo(int.class);
    }

    @Test
    @DisplayName("PanamaInstance should have callI32Function method without params")
    void panamaInstanceShouldHaveCallI32FunctionNoParams() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("callI32Function", String.class);
      assertThat(method.getReturnType()).isEqualTo(int.class);
    }

    @Test
    @DisplayName("PanamaInstance should have getAllExports method")
    void panamaInstanceShouldHaveGetAllExportsMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getAllExports");
      assertThat(method.getReturnType().getName()).contains("Map");
    }

    @Test
    @DisplayName("PanamaInstance should have getStatistics method")
    void panamaInstanceShouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getStatistics");
      assertThat(method.getReturnType().getName()).contains("InstanceStatistics");
    }

    @Test
    @DisplayName("PanamaInstance should have getExportDescriptors method")
    void panamaInstanceShouldHaveGetExportDescriptorsMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getExportDescriptors");
      assertThat(method.getReturnType().getName()).contains("List");
    }
  }

  @Nested
  @DisplayName("PanamaInstance Validation Tests")
  class PanamaInstanceValidationTests {

    @Test
    @DisplayName("PanamaInstance constructor should reject null module")
    void panamaInstanceConstructorShouldRejectNullModule() {
      assertThatThrownBy(() -> new PanamaInstance(null, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Module cannot be null");
    }
  }

  @Nested
  @DisplayName("PanamaInstanceGlobal Class Structure Tests")
  class PanamaInstanceGlobalClassStructureTests {

    @Test
    @DisplayName("PanamaInstanceGlobal should implement WasmGlobal interface")
    void panamaInstanceGlobalShouldImplementWasmGlobalInterface() {
      assertThat(WasmGlobal.class.isAssignableFrom(PanamaInstanceGlobal.class)).isTrue();
    }

    @Test
    @DisplayName("PanamaInstanceGlobal should implement AutoCloseable interface")
    void panamaInstanceGlobalShouldImplementAutoCloseableInterface() {
      assertThat(AutoCloseable.class.isAssignableFrom(PanamaInstanceGlobal.class)).isTrue();
    }

    @Test
    @DisplayName("PanamaInstanceGlobal should be final class")
    void panamaInstanceGlobalShouldBeFinalClass() {
      assertThat(Modifier.isFinal(PanamaInstanceGlobal.class.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("PanamaInstanceGlobal should have instance field")
    void panamaInstanceGlobalShouldHaveInstanceField() throws NoSuchFieldException {
      final Field field = PanamaInstanceGlobal.class.getDeclaredField("instance");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(PanamaInstance.class);
    }

    @Test
    @DisplayName("PanamaInstanceGlobal should have store field")
    void panamaInstanceGlobalShouldHaveStoreField() throws NoSuchFieldException {
      final Field field = PanamaInstanceGlobal.class.getDeclaredField("store");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(PanamaStore.class);
    }

    @Test
    @DisplayName("PanamaInstanceGlobal should have name field")
    void panamaInstanceGlobalShouldHaveNameField() throws NoSuchFieldException {
      final Field field = PanamaInstanceGlobal.class.getDeclaredField("name");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(String.class);
    }

    @Test
    @DisplayName("PanamaInstanceGlobal should have type field")
    void panamaInstanceGlobalShouldHaveTypeField() throws NoSuchFieldException {
      final Field field = PanamaInstanceGlobal.class.getDeclaredField("type");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(WasmValueType.class);
    }

    @Test
    @DisplayName("PanamaInstanceGlobal should have mutable field")
    void panamaInstanceGlobalShouldHaveMutableField() throws NoSuchFieldException {
      final Field field = PanamaInstanceGlobal.class.getDeclaredField("mutable");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("PanamaInstanceGlobal should have arena field")
    void panamaInstanceGlobalShouldHaveArenaField() throws NoSuchFieldException {
      final Field field = PanamaInstanceGlobal.class.getDeclaredField("arena");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(Arena.class);
    }

    @Test
    @DisplayName("PanamaInstanceGlobal should have closed field")
    void panamaInstanceGlobalShouldHaveClosedField() throws NoSuchFieldException {
      final Field field = PanamaInstanceGlobal.class.getDeclaredField("closed");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isVolatile(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(boolean.class);
    }
  }

  @Nested
  @DisplayName("PanamaInstanceGlobal Method Tests")
  class PanamaInstanceGlobalMethodTests {

    @Test
    @DisplayName("PanamaInstanceGlobal should have get method")
    void panamaInstanceGlobalShouldHaveGetMethod() throws NoSuchMethodException {
      final Method method = PanamaInstanceGlobal.class.getMethod("get");
      assertThat(method.getReturnType()).isEqualTo(ai.tegmentum.wasmtime4j.WasmValue.class);
    }

    @Test
    @DisplayName("PanamaInstanceGlobal should have set method")
    void panamaInstanceGlobalShouldHaveSetMethod() throws NoSuchMethodException {
      final Method method =
          PanamaInstanceGlobal.class.getMethod("set", ai.tegmentum.wasmtime4j.WasmValue.class);
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("PanamaInstanceGlobal should have getType method")
    void panamaInstanceGlobalShouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = PanamaInstanceGlobal.class.getMethod("getType");
      assertThat(method.getReturnType()).isEqualTo(WasmValueType.class);
    }

    @Test
    @DisplayName("PanamaInstanceGlobal should have isMutable method")
    void panamaInstanceGlobalShouldHaveIsMutableMethod() throws NoSuchMethodException {
      final Method method = PanamaInstanceGlobal.class.getMethod("isMutable");
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("PanamaInstanceGlobal should have getGlobalType method")
    void panamaInstanceGlobalShouldHaveGetGlobalTypeMethod() throws NoSuchMethodException {
      final Method method = PanamaInstanceGlobal.class.getMethod("getGlobalType");
      assertThat(method.getReturnType()).isEqualTo(ai.tegmentum.wasmtime4j.type.GlobalType.class);
    }

    @Test
    @DisplayName("PanamaInstanceGlobal should have close method")
    void panamaInstanceGlobalShouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaInstanceGlobal.class.getMethod("close");
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }
  }

  @Nested
  @DisplayName("PanamaInstancePre Class Structure Tests")
  class PanamaInstancePreClassStructureTests {

    @Test
    @DisplayName("PanamaInstancePre should implement InstancePre interface")
    void panamaInstancePreShouldImplementInstancePreInterface() {
      assertThat(InstancePre.class.isAssignableFrom(PanamaInstancePre.class)).isTrue();
    }

    @Test
    @DisplayName("PanamaInstancePre should be final class")
    void panamaInstancePreShouldBeFinalClass() {
      assertThat(Modifier.isFinal(PanamaInstancePre.class.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("PanamaInstancePre should have Logger field")
    void panamaInstancePreShouldHaveLoggerField() throws NoSuchFieldException {
      final Field loggerField = PanamaInstancePre.class.getDeclaredField("LOGGER");
      assertThat(Modifier.isStatic(loggerField.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(loggerField.getModifiers())).isTrue();
      assertThat(loggerField.getType()).isEqualTo(Logger.class);
    }

    @Test
    @DisplayName("PanamaInstancePre should have nativeInstancePre field")
    void panamaInstancePreShouldHaveNativeInstancePreField() throws NoSuchFieldException {
      final Field field = PanamaInstancePre.class.getDeclaredField("nativeInstancePre");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(MemorySegment.class);
    }

    @Test
    @DisplayName("PanamaInstancePre should have module field")
    void panamaInstancePreShouldHaveModuleField() throws NoSuchFieldException {
      final Field field = PanamaInstancePre.class.getDeclaredField("module");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType().getName()).contains("Module");
    }

    @Test
    @DisplayName("PanamaInstancePre should have engine field")
    void panamaInstancePreShouldHaveEngineField() throws NoSuchFieldException {
      final Field field = PanamaInstancePre.class.getDeclaredField("engine");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType().getName()).contains("Engine");
    }

    @Test
    @DisplayName("PanamaInstancePre should have creationTime field")
    void panamaInstancePreShouldHaveCreationTimeField() throws NoSuchFieldException {
      final Field field = PanamaInstancePre.class.getDeclaredField("creationTime");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(Instant.class);
    }

    @Test
    @DisplayName("PanamaInstancePre should have closed field")
    void panamaInstancePreShouldHaveClosedField() throws NoSuchFieldException {
      final Field field = PanamaInstancePre.class.getDeclaredField("closed");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(AtomicBoolean.class);
    }
  }

  @Nested
  @DisplayName("PanamaInstancePre Method Tests")
  class PanamaInstancePreMethodTests {

    @Test
    @DisplayName("PanamaInstancePre should have instantiate method with Store parameter")
    void panamaInstancePreShouldHaveInstantiateMethod() throws NoSuchMethodException {
      final Method method =
          PanamaInstancePre.class.getMethod("instantiate", ai.tegmentum.wasmtime4j.Store.class);
      assertThat(method.getReturnType()).isEqualTo(Instance.class);
    }

    @Test
    @DisplayName("PanamaInstancePre should have instantiate method with Store and ImportMap")
    void panamaInstancePreShouldHaveInstantiateWithImportsMethod() throws NoSuchMethodException {
      final Method method =
          PanamaInstancePre.class.getMethod(
              "instantiate",
              ai.tegmentum.wasmtime4j.Store.class,
              ai.tegmentum.wasmtime4j.ImportMap.class);
      assertThat(method.getReturnType()).isEqualTo(Instance.class);
    }

    @Test
    @DisplayName("PanamaInstancePre should have instantiateAsync method")
    void panamaInstancePreShouldHaveInstantiateAsyncMethod() throws NoSuchMethodException {
      final Method method =
          PanamaInstancePre.class.getMethod(
              "instantiateAsync", ai.tegmentum.wasmtime4j.Store.class);
      assertThat(method.getReturnType()).isEqualTo(CompletableFuture.class);
    }

    @Test
    @DisplayName("PanamaInstancePre should have instantiateAsync method with imports")
    void panamaInstancePreShouldHaveInstantiateAsyncWithImportsMethod()
        throws NoSuchMethodException {
      final Method method =
          PanamaInstancePre.class.getMethod(
              "instantiateAsync",
              ai.tegmentum.wasmtime4j.Store.class,
              ai.tegmentum.wasmtime4j.ImportMap.class);
      assertThat(method.getReturnType()).isEqualTo(CompletableFuture.class);
    }

    @Test
    @DisplayName("PanamaInstancePre should have getModule method")
    void panamaInstancePreShouldHaveGetModuleMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("getModule");
      assertThat(method.getReturnType().getName()).contains("Module");
    }

    @Test
    @DisplayName("PanamaInstancePre should have getEngine method")
    void panamaInstancePreShouldHaveGetEngineMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("getEngine");
      assertThat(method.getReturnType().getName()).contains("Engine");
    }

    @Test
    @DisplayName("PanamaInstancePre should have isValid method")
    void panamaInstancePreShouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("isValid");
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("PanamaInstancePre should have getInstanceCount method")
    void panamaInstancePreShouldHaveGetInstanceCountMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("getInstanceCount");
      assertThat(method.getReturnType()).isEqualTo(long.class);
    }

    @Test
    @DisplayName("PanamaInstancePre should have getStatistics method")
    void panamaInstancePreShouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("getStatistics");
      assertThat(method.getReturnType().getName()).contains("PreInstantiationStatistics");
    }

    @Test
    @DisplayName("PanamaInstancePre should have close method")
    void panamaInstancePreShouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("close");
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("PanamaInstancePre should have getNativeInstancePre method")
    void panamaInstancePreShouldHaveGetNativeInstancePreMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("getNativeInstancePre");
      assertThat(method.getReturnType()).isEqualTo(MemorySegment.class);
    }

    @Test
    @DisplayName("PanamaInstancePre should have toString method")
    void panamaInstancePreShouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("toString");
      assertThat(method.getReturnType()).isEqualTo(String.class);
    }
  }

  @Nested
  @DisplayName("PanamaInstancePre Validation Tests")
  class PanamaInstancePreValidationTests {

    @Test
    @DisplayName("PanamaInstancePre constructor should reject null native instance")
    void panamaInstancePreConstructorShouldRejectNullNativeInstance() {
      assertThatThrownBy(() -> new PanamaInstancePre(null, null, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Native InstancePre cannot be null");
    }

    @Test
    @DisplayName("PanamaInstancePre constructor should reject MemorySegment.NULL")
    void panamaInstancePreConstructorShouldRejectNullSegment() {
      assertThatThrownBy(() -> new PanamaInstancePre(MemorySegment.NULL, null, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Native InstancePre cannot be null");
    }
  }

  @Nested
  @DisplayName("Instance State Tests")
  class InstanceStateTests {

    @Test
    @DisplayName("InstanceState enum should have CREATED value")
    void instanceStateShouldHaveCreatedValue() {
      assertThat(InstanceState.CREATED).isNotNull();
    }

    @Test
    @DisplayName("InstanceState enum should have DISPOSED value")
    void instanceStateShouldHaveDisposedValue() {
      assertThat(InstanceState.DISPOSED).isNotNull();
    }

    @Test
    @DisplayName("InstanceState values should be distinct")
    void instanceStateValuesShouldBeDistinct() {
      assertThat(InstanceState.CREATED).isNotEqualTo(InstanceState.DISPOSED);
    }
  }

  @Nested
  @DisplayName("WasmValueType Coverage Tests")
  class WasmValueTypeCoverageTests {

    @Test
    @DisplayName("WasmValueType should have I32 value")
    void wasmValueTypeShouldHaveI32Value() {
      assertThat(WasmValueType.I32).isNotNull();
    }

    @Test
    @DisplayName("WasmValueType should have I64 value")
    void wasmValueTypeShouldHaveI64Value() {
      assertThat(WasmValueType.I64).isNotNull();
    }

    @Test
    @DisplayName("WasmValueType should have F32 value")
    void wasmValueTypeShouldHaveF32Value() {
      assertThat(WasmValueType.F32).isNotNull();
    }

    @Test
    @DisplayName("WasmValueType should have F64 value")
    void wasmValueTypeShouldHaveF64Value() {
      assertThat(WasmValueType.F64).isNotNull();
    }

    @Test
    @DisplayName("WasmValueType should have V128 value")
    void wasmValueTypeShouldHaveV128Value() {
      assertThat(WasmValueType.V128).isNotNull();
    }

    @Test
    @DisplayName("WasmValueType should have FUNCREF value")
    void wasmValueTypeShouldHaveFuncrefValue() {
      assertThat(WasmValueType.FUNCREF).isNotNull();
    }

    @Test
    @DisplayName("WasmValueType should have EXTERNREF value")
    void wasmValueTypeShouldHaveExternrefValue() {
      assertThat(WasmValueType.EXTERNREF).isNotNull();
    }

    @Test
    @DisplayName("WasmValueType toNativeTypeCode should return correct codes")
    void wasmValueTypeToNativeTypeCodeShouldWork() {
      // Verify the method exists and works
      assertThat(WasmValueType.I32.toNativeTypeCode()).isGreaterThanOrEqualTo(0);
      assertThat(WasmValueType.I64.toNativeTypeCode()).isGreaterThanOrEqualTo(0);
      assertThat(WasmValueType.F32.toNativeTypeCode()).isGreaterThanOrEqualTo(0);
      assertThat(WasmValueType.F64.toNativeTypeCode()).isGreaterThanOrEqualTo(0);
    }
  }

  @Nested
  @DisplayName("Memory Delegation Methods Tests")
  class MemoryDelegationMethodsTests {

    @Test
    @DisplayName("PanamaInstance should have getMemorySize package-private method")
    void panamaInstanceShouldHaveGetMemorySizeMethod() {
      final boolean methodExists =
          Arrays.stream(PanamaInstance.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("getMemorySize"));
      assertThat(methodExists).isTrue();
    }

    @Test
    @DisplayName("PanamaInstance should have growMemory package-private method")
    void panamaInstanceShouldHaveGrowMemoryMethod() {
      final boolean methodExists =
          Arrays.stream(PanamaInstance.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("growMemory"));
      assertThat(methodExists).isTrue();
    }

    @Test
    @DisplayName("PanamaInstance should have readMemoryBytes package-private method")
    void panamaInstanceShouldHaveReadMemoryBytesMethod() {
      final boolean methodExists =
          Arrays.stream(PanamaInstance.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("readMemoryBytes"));
      assertThat(methodExists).isTrue();
    }

    @Test
    @DisplayName("PanamaInstance should have writeMemoryBytes package-private method")
    void panamaInstanceShouldHaveWriteMemoryBytesMethod() {
      final boolean methodExists =
          Arrays.stream(PanamaInstance.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("writeMemoryBytes"));
      assertThat(methodExists).isTrue();
    }

    @Test
    @DisplayName("PanamaInstance should have getMemoryBuffer package-private method")
    void panamaInstanceShouldHaveGetMemoryBufferMethod() {
      final boolean methodExists =
          Arrays.stream(PanamaInstance.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("getMemoryBuffer"));
      assertThat(methodExists).isTrue();
    }
  }

  @Nested
  @DisplayName("Count Methods Tests")
  class CountMethodsTests {

    @Test
    @DisplayName("PanamaInstance should have getFunctionCount method")
    void panamaInstanceShouldHaveGetFunctionCountMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getFunctionCount");
      assertThat(method.getReturnType()).isEqualTo(int.class);
    }

    @Test
    @DisplayName("PanamaInstance should have getMemoryCount method")
    void panamaInstanceShouldHaveGetMemoryCountMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getMemoryCount");
      assertThat(method.getReturnType()).isEqualTo(int.class);
    }

    @Test
    @DisplayName("PanamaInstance should have getTableCount method")
    void panamaInstanceShouldHaveGetTableCountMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getTableCount");
      assertThat(method.getReturnType()).isEqualTo(int.class);
    }

    @Test
    @DisplayName("PanamaInstance should have getGlobalCount method")
    void panamaInstanceShouldHaveGetGlobalCountMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getGlobalCount");
      assertThat(method.getReturnType()).isEqualTo(int.class);
    }

    @Test
    @DisplayName("PanamaInstance should have getMetadataExportCount method")
    void panamaInstanceShouldHaveGetMetadataExportCountMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getMetadataExportCount");
      assertThat(method.getReturnType()).isEqualTo(int.class);
    }
  }

  @Nested
  @DisplayName("Names List Methods Tests")
  class NamesListMethodsTests {

    @Test
    @DisplayName("PanamaInstance should have getFunctionNames method")
    void panamaInstanceShouldHaveGetFunctionNamesMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getFunctionNames");
      assertThat(method.getReturnType().getName()).contains("List");
    }

    @Test
    @DisplayName("PanamaInstance should have getMemoryNames method")
    void panamaInstanceShouldHaveGetMemoryNamesMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getMemoryNames");
      assertThat(method.getReturnType().getName()).contains("List");
    }

    @Test
    @DisplayName("PanamaInstance should have getTableNames method")
    void panamaInstanceShouldHaveGetTableNamesMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getTableNames");
      assertThat(method.getReturnType().getName()).contains("List");
    }

    @Test
    @DisplayName("PanamaInstance should have getGlobalNames method")
    void panamaInstanceShouldHaveGetGlobalNamesMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getGlobalNames");
      assertThat(method.getReturnType().getName()).contains("List");
    }
  }

  @Nested
  @DisplayName("Type Lookup Methods Tests")
  class TypeLookupMethodsTests {

    @Test
    @DisplayName("PanamaInstance should have getFunctionType method")
    void panamaInstanceShouldHaveGetFunctionTypeMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getFunctionType", String.class);
      assertThat(method.getReturnType().getName()).contains("Optional");
    }

    @Test
    @DisplayName("PanamaInstance should have getGlobalType method")
    void panamaInstanceShouldHaveGetGlobalTypeMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getGlobalType", String.class);
      assertThat(method.getReturnType().getName()).contains("Optional");
    }

    @Test
    @DisplayName("PanamaInstance should have getMemoryType method")
    void panamaInstanceShouldHaveGetMemoryTypeMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getMemoryType", String.class);
      assertThat(method.getReturnType().getName()).contains("Optional");
    }

    @Test
    @DisplayName("PanamaInstance should have getTableType method")
    void panamaInstanceShouldHaveGetTableTypeMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getTableType", String.class);
      assertThat(method.getReturnType().getName()).contains("Optional");
    }

    @Test
    @DisplayName("PanamaInstance should have getExportDescriptor method")
    void panamaInstanceShouldHaveGetExportDescriptorMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getExportDescriptor", String.class);
      assertThat(method.getReturnType().getName()).contains("Optional");
    }
  }

  @Nested
  @DisplayName("Resource Lifecycle Methods Tests")
  class ResourceLifecycleMethodsTests {

    @Test
    @DisplayName("PanamaInstance should have dispose method")
    void panamaInstanceShouldHaveDisposeMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("dispose");
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("PanamaInstance should have isDisposed method")
    void panamaInstanceShouldHaveIsDisposedMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("isDisposed");
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("PanamaInstance should have cleanup method")
    void panamaInstanceShouldHaveCleanupMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("cleanup");
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("PanamaInstance should have getCreatedAtMicros method")
    void panamaInstanceShouldHaveGetCreatedAtMicrosMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getCreatedAtMicros");
      assertThat(method.getReturnType()).isEqualTo(long.class);
    }
  }

  @Nested
  @DisplayName("Native Access Methods Tests")
  class NativeAccessMethodsTests {

    @Test
    @DisplayName("PanamaInstance should have getNativeInstance method")
    void panamaInstanceShouldHaveGetNativeInstanceMethod() throws NoSuchMethodException {
      final Method method = PanamaInstance.class.getMethod("getNativeInstance");
      assertThat(method.getReturnType()).isEqualTo(MemorySegment.class);
    }

    @Test
    @DisplayName("PanamaInstance should have getNativeStore package-private method")
    void panamaInstanceShouldHaveGetNativeStoreMethod() {
      final boolean methodExists =
          Arrays.stream(PanamaInstance.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("getNativeStore"));
      assertThat(methodExists).isTrue();
    }
  }

  @Nested
  @DisplayName("PreInstantiationStatistics Tests")
  class PreInstantiationStatisticsTests {

    @Test
    @DisplayName("PreInstantiationStatistics should have builder method")
    void preInstantiationStatisticsShouldHaveBuilderMethod() throws NoSuchMethodException {
      final Class<?> statsClass = ai.tegmentum.wasmtime4j.PreInstantiationStatistics.class;
      final Method method = statsClass.getMethod("builder");
      assertThat(method).isNotNull();
    }

    @Test
    @DisplayName("PreInstantiationStatistics builder should support creationTime")
    void preInstantiationStatisticsBuilderShouldSupportCreationTime() {
      final ai.tegmentum.wasmtime4j.PreInstantiationStatistics stats =
          ai.tegmentum.wasmtime4j.PreInstantiationStatistics.builder()
              .creationTime(Instant.now())
              .build();
      assertThat(stats).isNotNull();
    }

    @Test
    @DisplayName("PreInstantiationStatistics builder should support preparationTime")
    void preInstantiationStatisticsBuilderShouldSupportPreparationTime() {
      final ai.tegmentum.wasmtime4j.PreInstantiationStatistics stats =
          ai.tegmentum.wasmtime4j.PreInstantiationStatistics.builder()
              .preparationTime(Duration.ofMillis(100))
              .build();
      assertThat(stats).isNotNull();
    }

    @Test
    @DisplayName("PreInstantiationStatistics builder should support instancesCreated")
    void preInstantiationStatisticsBuilderShouldSupportInstancesCreated() {
      final ai.tegmentum.wasmtime4j.PreInstantiationStatistics stats =
          ai.tegmentum.wasmtime4j.PreInstantiationStatistics.builder().instancesCreated(5L).build();
      assertThat(stats).isNotNull();
    }

    @Test
    @DisplayName("PreInstantiationStatistics builder should support averageInstantiationTime")
    void preInstantiationStatisticsBuilderShouldSupportAverageInstantiationTime() {
      final ai.tegmentum.wasmtime4j.PreInstantiationStatistics stats =
          ai.tegmentum.wasmtime4j.PreInstantiationStatistics.builder()
              .averageInstantiationTime(Duration.ofNanos(50000))
              .build();
      assertThat(stats).isNotNull();
    }
  }

  /** Close safety tests for PanamaInstance requiring native library. */
  @Nested
  @DisplayName("Closed Instance Detection Tests")
  class ClosedInstanceDetectionTests {

    private static final Logger CLOSE_LOGGER = Logger.getLogger("ClosedInstanceDetectionTests");

    private static boolean nativeAvailable;

    static {
      try {
        new PanamaEngine().close();
        nativeAvailable = true;
      } catch (final Exception | UnsatisfiedLinkError e) {
        nativeAvailable = false;
      }
    }

    @Test
    @DisplayName("method on closed instance should throw IllegalStateException")
    void methodOnClosedInstanceShouldThrow() throws Exception {
      assumeTrue(nativeAvailable, "Native library not available");

      final Path wasmPath =
          Paths.get(getClass().getClassLoader().getResource("wasm/exports-test.wasm").toURI());
      final byte[] wasmBytes = Files.readAllBytes(wasmPath);

      final PanamaEngine engine = new PanamaEngine();
      final PanamaStore store = new PanamaStore(engine);
      final PanamaModule module = new PanamaModule(engine, wasmBytes);
      final PanamaInstance instance = new PanamaInstance(module, store);

      instance.close();
      CLOSE_LOGGER.info("Instance closed, attempting operations");

      assertThrows(
          IllegalStateException.class,
          () -> instance.getFunction("add"),
          "getFunction() on closed instance should throw IllegalStateException");
      assertThrows(
          IllegalStateException.class,
          () -> instance.getMemory("memory"),
          "getMemory() on closed instance should throw IllegalStateException");
      CLOSE_LOGGER.info("IllegalStateException thrown as expected for closed instance operations");

      store.close();
      module.close();
      engine.close();
    }

    @Test
    @DisplayName("double close should be safe")
    void doubleCloseShouldBeSafe() throws Exception {
      assumeTrue(nativeAvailable, "Native library not available");

      final Path wasmPath =
          Paths.get(getClass().getClassLoader().getResource("wasm/exports-test.wasm").toURI());
      final byte[] wasmBytes = Files.readAllBytes(wasmPath);

      final PanamaEngine engine = new PanamaEngine();
      final PanamaStore store = new PanamaStore(engine);
      final PanamaModule module = new PanamaModule(engine, wasmBytes);
      final PanamaInstance instance = new PanamaInstance(module, store);

      instance.close();
      CLOSE_LOGGER.info("First close completed");

      assertDoesNotThrow(instance::close, "Second close should not throw");
      CLOSE_LOGGER.info("Second close completed without exception");

      assertThrows(
          IllegalStateException.class,
          () -> instance.getFunction("add"),
          "getFunction() after double close should still throw");
      CLOSE_LOGGER.info("IllegalStateException confirmed after double close");

      store.close();
      module.close();
      engine.close();
    }
  }
}
