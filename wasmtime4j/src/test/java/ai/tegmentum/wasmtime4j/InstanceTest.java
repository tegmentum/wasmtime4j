package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link Instance} interface.
 *
 * <p>This test class verifies the structure and contract of the Instance interface, which
 * represents an instantiated WebAssembly module.
 */
@DisplayName("Instance Interface Tests")
class InstanceTest {

  @Nested
  @DisplayName("Interface Definition Tests")
  class InterfaceDefinitionTests {

    @Test
    @DisplayName("Instance should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Instance.class.isInterface(), "Instance should be an interface");
    }

    @Test
    @DisplayName("Instance should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(Instance.class), "Instance should extend Closeable");
    }

    @Test
    @DisplayName("Instance should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(Instance.class.getModifiers()),
          "Instance should be a public interface");
    }
  }

  @Nested
  @DisplayName("Function Retrieval Method Tests")
  class FunctionRetrievalMethodTests {

    @Test
    @DisplayName("Should have getFunction(String) method")
    void shouldHaveGetFunctionByNameMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getFunction", String.class);
      assertNotNull(method, "getFunction(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getFunction(int) method")
    void shouldHaveGetFunctionByIndexMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getFunction", int.class);
      assertNotNull(method, "getFunction(int) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getTypedFunc default method")
    void shouldHaveGetTypedFuncMethod() throws NoSuchMethodException {
      final Method method =
          Instance.class.getMethod("getTypedFunc", String.class, WasmValueType[].class);
      assertNotNull(method, "getTypedFunc(String, WasmValueType[]) method should exist");
      assertTrue(method.isDefault(), "getTypedFunc should be a default method");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Global Retrieval Method Tests")
  class GlobalRetrievalMethodTests {

    @Test
    @DisplayName("Should have getGlobal(String) method")
    void shouldHaveGetGlobalByNameMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getGlobal", String.class);
      assertNotNull(method, "getGlobal(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getGlobal(int) method")
    void shouldHaveGetGlobalByIndexMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getGlobal", int.class);
      assertNotNull(method, "getGlobal(int) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Memory Retrieval Method Tests")
  class MemoryRetrievalMethodTests {

    @Test
    @DisplayName("Should have getMemory(String) method")
    void shouldHaveGetMemoryByNameMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getMemory", String.class);
      assertNotNull(method, "getMemory(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getMemory(int) method")
    void shouldHaveGetMemoryByIndexMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getMemory", int.class);
      assertNotNull(method, "getMemory(int) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getDefaultMemory() method")
    void shouldHaveGetDefaultMemoryMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getDefaultMemory");
      assertNotNull(method, "getDefaultMemory() method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Table Retrieval Method Tests")
  class TableRetrievalMethodTests {

    @Test
    @DisplayName("Should have getTable(String) method")
    void shouldHaveGetTableByNameMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getTable", String.class);
      assertNotNull(method, "getTable(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getTable(int) method")
    void shouldHaveGetTableByIndexMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getTable", int.class);
      assertNotNull(method, "getTable(int) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Tag Retrieval Method Tests")
  class TagRetrievalMethodTests {

    @Test
    @DisplayName("Should have getTag(String) default method")
    void shouldHaveGetTagMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getTag", String.class);
      assertNotNull(method, "getTag(String) method should exist");
      assertTrue(method.isDefault(), "getTag should be a default method");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Export Method Tests")
  class ExportMethodTests {

    @Test
    @DisplayName("Should have getExportNames() method")
    void shouldHaveGetExportNamesMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getExportNames");
      assertNotNull(method, "getExportNames() method should exist");
      assertEquals(String[].class, method.getReturnType(), "Should return String[]");
    }

    @Test
    @DisplayName("Should have getExportDescriptors() method")
    void shouldHaveGetExportDescriptorsMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getExportDescriptors");
      assertNotNull(method, "getExportDescriptors() method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("Should have getExportDescriptor(String) method")
    void shouldHaveGetExportDescriptorMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getExportDescriptor", String.class);
      assertNotNull(method, "getExportDescriptor(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have hasExport(String) method")
    void shouldHaveHasExportMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("hasExport", String.class);
      assertNotNull(method, "hasExport(String) method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have getAllExports() method")
    void shouldHaveGetAllExportsMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getAllExports");
      assertNotNull(method, "getAllExports() method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("Type Introspection Method Tests")
  class TypeIntrospectionMethodTests {

    @Test
    @DisplayName("Should have getFunctionType(String) method")
    void shouldHaveGetFunctionTypeMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getFunctionType", String.class);
      assertNotNull(method, "getFunctionType(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getGlobalType(String) method")
    void shouldHaveGetGlobalTypeMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getGlobalType", String.class);
      assertNotNull(method, "getGlobalType(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getMemoryType(String) method")
    void shouldHaveGetMemoryTypeMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getMemoryType", String.class);
      assertNotNull(method, "getMemoryType(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have getTableType(String) method")
    void shouldHaveGetTableTypeMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getTableType", String.class);
      assertNotNull(method, "getTableType(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Module and Store Method Tests")
  class ModuleAndStoreMethodTests {

    @Test
    @DisplayName("Should have getModule() method")
    void shouldHaveGetModuleMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getModule");
      assertNotNull(method, "getModule() method should exist");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("Should have getStore() method")
    void shouldHaveGetStoreMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getStore");
      assertNotNull(method, "getStore() method should exist");
      assertEquals(Store.class, method.getReturnType(), "Should return Store");
    }
  }

  @Nested
  @DisplayName("Function Call Method Tests")
  class FunctionCallMethodTests {

    @Test
    @DisplayName("Should have callFunction(String, WasmValue...) method")
    void shouldHaveCallFunctionMethod() throws NoSuchMethodException {
      final Method method =
          Instance.class.getMethod("callFunction", String.class, WasmValue[].class);
      assertNotNull(method, "callFunction(String, WasmValue...) method should exist");
      assertEquals(WasmValue[].class, method.getReturnType(), "Should return WasmValue[]");
    }

    @Test
    @DisplayName("Should have callI32Function(String, int...) method")
    void shouldHaveCallI32FunctionWithParamsMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("callI32Function", String.class, int[].class);
      assertNotNull(method, "callI32Function(String, int...) method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("Should have callI32Function(String) method")
    void shouldHaveCallI32FunctionNoParamsMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("callI32Function", String.class);
      assertNotNull(method, "callI32Function(String) method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Statistics and State Method Tests")
  class StatisticsAndStateMethodTests {

    @Test
    @DisplayName("Should have getStatistics() method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics() method should exist");
      assertEquals(
          InstanceStatistics.class, method.getReturnType(), "Should return InstanceStatistics");
    }

    @Test
    @DisplayName("Should have getState() method")
    void shouldHaveGetStateMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getState");
      assertNotNull(method, "getState() method should exist");
      assertEquals(InstanceState.class, method.getReturnType(), "Should return InstanceState");
    }

    @Test
    @DisplayName("Should have getCreatedAtMicros() method")
    void shouldHaveGetCreatedAtMicrosMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getCreatedAtMicros");
      assertNotNull(method, "getCreatedAtMicros() method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("Should have getMetadataExportCount() method")
    void shouldHaveGetMetadataExportCountMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("getMetadataExportCount");
      assertNotNull(method, "getMetadataExportCount() method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("Should have isValid() method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("isValid");
      assertNotNull(method, "isValid() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have dispose() method")
    void shouldHaveDisposeMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("dispose");
      assertNotNull(method, "dispose() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have isDisposed() method")
    void shouldHaveIsDisposedMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("isDisposed");
      assertNotNull(method, "isDisposed() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have cleanup() method")
    void shouldHaveCleanupMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("cleanup");
      assertNotNull(method, "cleanup() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have close() method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("close");
      assertNotNull(method, "close() method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Import Method Tests")
  class ImportMethodTests {

    @Test
    @DisplayName("Should have setImports(Map) method")
    void shouldHaveSetImportsMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("setImports", Map.class);
      assertNotNull(method, "setImports(Map) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("Should have static create(Store, Module) method")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("create", Store.class, Module.class);
      assertNotNull(method, "create(Store, Module) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }

    @Test
    @DisplayName("Should have static createAsync(Store, Module) method")
    void shouldHaveStaticCreateAsyncMethod() throws NoSuchMethodException {
      final Method method = Instance.class.getMethod("createAsync", Store.class, Module.class);
      assertNotNull(method, "createAsync(Store, Module) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "createAsync should be static");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }
}
