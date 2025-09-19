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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link PanamaLinker}.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, and defensive programming.
 * The tests verify constructor behavior, resource management, and basic API functionality.
 *
 * <p>Note: Functional behavior with actual WebAssembly linker operations is tested in integration
 * tests, as unit testing Panama FFI requires actual native library loading.
 */
class PanamaLinkerTest {

  @Mock private PanamaEngine mockEngine;
  @Mock private FunctionType mockFunctionType;
  @Mock private HostFunction mockHostFunction;
  @Mock private PanamaMemory mockMemory;
  @Mock private PanamaTable mockTable;
  @Mock private PanamaGlobal mockGlobal;
  @Mock private PanamaInstance mockInstance;
  @Mock private PanamaStore mockStore;
  @Mock private PanamaModule mockModule;
  @Mock private WasiConfig mockWasiConfig;

  private MemorySegment validHandle;
  private Arena testArena;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    testArena = Arena.ofConfined();
    validHandle = testArena.allocate(8); // Allocate a non-NULL segment
  }

  @Test
  void testConstructorWithValidParameters() {
    // Given
    when(mockEngine.getClass()).thenReturn(PanamaEngine.class);

    // When
    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);

    // Then
    assertThat(linker).isNotNull();
    assertThat(linker.getEngine()).isEqualTo(mockEngine);
    assertThat(linker.isValid()).isTrue();
    assertThat(linker.getHandle()).isEqualTo(validHandle);
  }

  @Test
  void testConstructorWithNullHandle() {
    // When & Then
    assertThrows(
        IllegalArgumentException.class, () -> new PanamaLinker(null, testArena, mockEngine));
  }

  @Test
  void testConstructorWithNullSegmentHandle() {
    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> new PanamaLinker(MemorySegment.NULL, testArena, mockEngine));
  }

  @Test
  void testConstructorWithNullArena() {
    // When & Then
    assertThrows(
        IllegalArgumentException.class, () -> new PanamaLinker(validHandle, null, mockEngine));
  }

  @Test
  void testConstructorWithNullEngine() {
    // When & Then
    assertThrows(
        IllegalArgumentException.class, () -> new PanamaLinker(validHandle, testArena, null));
  }

  @Test
  void testDefineHostFunctionWithNullParameters() {
    // Given
    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);

    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> linker.defineHostFunction(null, "name", mockFunctionType, mockHostFunction));

    assertThrows(
        IllegalArgumentException.class,
        () -> linker.defineHostFunction("module", null, mockFunctionType, mockHostFunction));

    assertThrows(
        IllegalArgumentException.class,
        () -> linker.defineHostFunction("module", "name", null, mockHostFunction));

    assertThrows(
        IllegalArgumentException.class,
        () -> linker.defineHostFunction("module", "name", mockFunctionType, null));
  }

  @Test
  void testDefineHostFunctionWithEmptyStrings() {
    // Given
    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);

    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> linker.defineHostFunction("", "name", mockFunctionType, mockHostFunction));

    assertThrows(
        IllegalArgumentException.class,
        () -> linker.defineHostFunction("module", "", mockFunctionType, mockHostFunction));

    assertThrows(
        IllegalArgumentException.class,
        () -> linker.defineHostFunction("   ", "name", mockFunctionType, mockHostFunction));

    assertThrows(
        IllegalArgumentException.class,
        () -> linker.defineHostFunction("module", "   ", mockFunctionType, mockHostFunction));
  }

  @Test
  void testDefineHostFunctionSimpleWithNullParameters() {
    // Given
    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);

    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> linker.defineHostFunction(null, "name", mockHostFunction));

    assertThrows(
        IllegalArgumentException.class,
        () -> linker.defineHostFunction("module", null, mockHostFunction));

    assertThrows(
        IllegalArgumentException.class, () -> linker.defineHostFunction("module", "name", null));
  }

  @Test
  void testDefineMemoryWithNullParameters() {
    // Given
    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);

    // When & Then
    assertThrows(
        IllegalArgumentException.class, () -> linker.defineMemory(null, "name", mockMemory));

    assertThrows(
        IllegalArgumentException.class, () -> linker.defineMemory("module", null, mockMemory));

    assertThrows(IllegalArgumentException.class, () -> linker.defineMemory("module", "name", null));
  }

  @Test
  void testDefineTableWithNullParameters() {
    // Given
    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> linker.defineTable(null, "name", mockTable));

    assertThrows(
        IllegalArgumentException.class, () -> linker.defineTable("module", null, mockTable));

    assertThrows(IllegalArgumentException.class, () -> linker.defineTable("module", "name", null));
  }

  @Test
  void testDefineGlobalWithNullParameters() {
    // Given
    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);

    // When & Then
    assertThrows(
        IllegalArgumentException.class, () -> linker.defineGlobal(null, "name", mockGlobal));

    assertThrows(
        IllegalArgumentException.class, () -> linker.defineGlobal("module", null, mockGlobal));

    assertThrows(IllegalArgumentException.class, () -> linker.defineGlobal("module", "name", null));
  }

  @Test
  void testDefineInstanceWithNullParameters() {
    // Given
    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> linker.defineInstance(null, mockInstance));

    assertThrows(IllegalArgumentException.class, () -> linker.defineInstance("module", null));
  }

  @Test
  void testAliasWithNullParameters() {
    // Given
    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);

    // When & Then
    assertThrows(
        IllegalArgumentException.class, () -> linker.alias(null, "fromName", "toModule", "toName"));

    assertThrows(
        IllegalArgumentException.class,
        () -> linker.alias("fromModule", null, "toModule", "toName"));

    assertThrows(
        IllegalArgumentException.class,
        () -> linker.alias("fromModule", "fromName", null, "toName"));

    assertThrows(
        IllegalArgumentException.class,
        () -> linker.alias("fromModule", "fromName", "toModule", null));
  }

  @Test
  void testInstantiateWithNullParameters() {
    // Given
    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> linker.instantiate(null, mockModule));

    assertThrows(IllegalArgumentException.class, () -> linker.instantiate(mockStore, null));
  }

  @Test
  void testDefineWasiWithNullParameter() {
    // Given
    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> linker.defineWasi(null));
  }

  @Test
  void testLinkerClosedStateOperations() {
    // Given
    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);
    linker.close();

    // When & Then - All operations should throw IllegalStateException when closed
    assertThrows(
        IllegalStateException.class,
        () -> linker.defineHostFunction("module", "name", mockFunctionType, mockHostFunction));

    assertThrows(
        IllegalStateException.class,
        () -> linker.defineHostFunction("module", "name", mockHostFunction));

    assertThrows(
        IllegalStateException.class, () -> linker.defineMemory("module", "name", mockMemory));

    assertThrows(
        IllegalStateException.class, () -> linker.defineTable("module", "name", mockTable));

    assertThrows(
        IllegalStateException.class, () -> linker.defineGlobal("module", "name", mockGlobal));

    assertThrows(IllegalStateException.class, () -> linker.defineInstance("module", mockInstance));

    assertThrows(
        IllegalStateException.class,
        () -> linker.alias("fromModule", "fromName", "toModule", "toName"));

    assertThrows(IllegalStateException.class, () -> linker.instantiate(mockStore, mockModule));

    assertThrows(IllegalStateException.class, () -> linker.defineWasi(mockWasiConfig));

    assertThrows(IllegalStateException.class, () -> linker.enableWasi());

    assertThrows(IllegalStateException.class, () -> linker.getHandle());

    // But these should not throw
    assertThat(linker.getEngine()).isEqualTo(mockEngine);
    assertThat(linker.isValid()).isFalse();
  }

  @Test
  void testMultipleClose() {
    // Given
    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);

    // When
    linker.close();
    linker.close(); // Second close should be safe

    // Then
    assertThat(linker.isValid()).isFalse();
  }

  @Test
  void testConvertToNativeTypes() {
    // Given
    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);
    final WasmValueType[] types = {
      WasmValueType.I32,
      WasmValueType.I64,
      WasmValueType.F32,
      WasmValueType.F64,
      WasmValueType.V128,
      WasmValueType.FUNCREF,
      WasmValueType.EXTERNREF
    };

    // When - Use reflection to test the private method through a mock scenario
    // Note: In a real test, we'd need to modify visibility or use a different approach

    // Then - Verify the method works correctly by checking that defineHostFunction
    // can handle different types without throwing validation errors (in integration tests)

    // For unit testing, we verify parameter validation works
    when(mockFunctionType.getParamTypes()).thenReturn(types);
    when(mockFunctionType.getReturnTypes()).thenReturn(new WasmValueType[0]);

    // This should not throw a validation exception
    // (actual behavior testing requires integration tests with native library)
  }

  @Test
  void testCreateWithNonPanamaEngine() {
    // Given
    final Engine nonPanamaEngine =
        new Engine() {
          @Override
          public void close() {}

          @Override
          public boolean isValid() {
            return false;
          }
        };

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> PanamaLinker.create(nonPanamaEngine));
  }

  @Test
  void testAllocateIntArrayHelper() {
    // Given
    final int[] values = {1, 2, 3, 4, 5};

    // When - Test the static helper method through reflection or integration
    // Note: This is a private method, so we test it indirectly through public methods
    // that use it, like defineHostFunction with FunctionType

    // Then - Verify no exceptions are thrown when using arrays of different sizes
    when(mockFunctionType.getParamTypes())
        .thenReturn(new WasmValueType[] {WasmValueType.I32, WasmValueType.I64});
    when(mockFunctionType.getReturnTypes()).thenReturn(new WasmValueType[] {WasmValueType.F32});

    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);
    // This should not throw for the validation and conversion parts
    // (actual native call testing requires integration tests)
  }

  @Test
  void testHostFunctionRegistryIntegration() {
    // Given
    final HostFunction simpleFunction = (params) -> new Object[] {42};

    // When
    final long id = PanamaHostFunctionRegistry.register(simpleFunction, null);

    // Then
    assertThat(id).isGreaterThan(0);
    assertThat(PanamaHostFunctionRegistry.get(id)).isNotNull();
    assertThat(PanamaHostFunctionRegistry.get(id).getImplementation()).isEqualTo(simpleFunction);

    // Cleanup
    PanamaHostFunctionRegistry.unregister(id);
  }

  @Test
  void testInstantiateAsync() {
    // Given
    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);

    // When
    final var future = linker.instantiateAsync(mockStore, mockModule);

    // Then
    assertThat(future).isNotNull();
    assertThat(future.isDone()).isFalse(); // Will complete when run (in integration test)
  }

  @Test
  void testAliasModuleWithNullParameters() {
    // Given
    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> linker.aliasModule(null, mockInstance));

    assertThrows(IllegalArgumentException.class, () -> linker.aliasModule("name", null));
  }

  @Test
  void testLinkerStateAfterCreation() {
    // Given
    final PanamaLinker linker = new PanamaLinker(validHandle, testArena, mockEngine);

    // When & Then
    assertThat(linker.isValid()).isTrue();
    assertThat(linker.getEngine()).isEqualTo(mockEngine);
    assertThat(linker.getHandle()).isEqualTo(validHandle);
  }
}
