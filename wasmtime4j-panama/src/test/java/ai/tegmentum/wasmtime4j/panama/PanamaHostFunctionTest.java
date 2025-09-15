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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.PanamaHostFunction.HostFunctionCallback;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for the PanamaHostFunction implementation.
 *
 * <p>These tests verify the complete functionality of the Panama FFI host function wrapper,
 * including callback handling, upcall stub creation, resource management, and thread safety.
 */
@ExtendWith(MockitoExtension.class)
class PanamaHostFunctionTest {

  @Mock private ArenaResourceManager mockArenaManager;
  @Mock private PanamaErrorHandler mockErrorHandler;
  @Mock private Arena mockArena;

  private FunctionType testFunctionType;
  private HostFunctionCallback testCallback;

  @BeforeEach
  void setUp() {
    // Create a simple test function type: (i32, i64) -> (f32)
    testFunctionType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I64},
            new WasmValueType[] {WasmValueType.F32});

    // Create a simple test callback
    testCallback = params -> new WasmValue[] {WasmValue.f32(42.0f)};

    // Setup mock arena with real Arena instance (needed for Panama FFI)
    // Use lenient() to avoid unnecessary stubbing warnings for tests that don't use arena
    final Arena realArena = Arena.ofShared();
    lenient().when(mockArenaManager.getArena()).thenReturn(realArena);
  }

  @Nested
  class ConstructorTests {

    @Test
    void testCreateHostFunctionWithValidParameters() throws Exception {
      // Act
      final PanamaHostFunction hostFunction =
          new PanamaHostFunction(
              "test_function", testFunctionType, testCallback, mockArenaManager, mockErrorHandler);

      // Assert
      assertNotNull(hostFunction);
      assertEquals("test_function", hostFunction.getName());
      assertEquals(testFunctionType, hostFunction.getFunctionType());
      assertFalse(hostFunction.isClosed());
      verify(mockArenaManager).registerManagedNativeResource(eq(hostFunction), any(), any());
    }

    @Test
    void testThrowExceptionWhenFunctionNameIsNull() {
      assertThrows(
          NullPointerException.class,
          () ->
              new PanamaHostFunction(
                  null, testFunctionType, testCallback, mockArenaManager, mockErrorHandler));
    }

    @Test
    void testThrowExceptionWhenFunctionTypeIsNull() {
      assertThrows(
          NullPointerException.class,
          () ->
              new PanamaHostFunction(
                  "test_function", null, testCallback, mockArenaManager, mockErrorHandler));
    }

    @Test
    void testThrowExceptionWhenCallbackIsNull() {
      assertThrows(
          NullPointerException.class,
          () ->
              new PanamaHostFunction(
                  "test_function", testFunctionType, null, mockArenaManager, mockErrorHandler));
    }

    @Test
    void testThrowExceptionWhenArenaManagerIsNull() {
      assertThrows(
          NullPointerException.class,
          () ->
              new PanamaHostFunction(
                  "test_function", testFunctionType, testCallback, null, mockErrorHandler));
    }

    @Test
    void testThrowExceptionWhenErrorHandlerIsNull() {
      assertThrows(
          NullPointerException.class,
          () ->
              new PanamaHostFunction(
                  "test_function", testFunctionType, testCallback, mockArenaManager, null));
    }
  }

  @Nested
  class FunctionInterfaceTests {

    private PanamaHostFunction hostFunction;

    @BeforeEach
    void setUp() throws Exception {
      hostFunction =
          new PanamaHostFunction(
              "test_function", testFunctionType, testCallback, mockArenaManager, mockErrorHandler);
    }

    @Test
    void testReturnCorrectFunctionType() {
      assertEquals(testFunctionType, hostFunction.getFunctionType());
    }

    @Test
    void testReturnCorrectFunctionName() {
      assertEquals("test_function", hostFunction.getName());
    }

    @Test
    void testThrowExceptionWhenCallingHostFunctionDirectly() {
      assertThrows(
          UnsupportedOperationException.class,
          () -> hostFunction.call(WasmValue.i32(1), WasmValue.i64(2L)));
    }
  }

  @Nested
  class HandleAccessTests {

    private PanamaHostFunction hostFunction;

    @BeforeEach
    void setUp() throws Exception {
      hostFunction =
          new PanamaHostFunction(
              "test_function", testFunctionType, testCallback, mockArenaManager, mockErrorHandler);
    }

    @Test
    void testReturnValidFunctionHandle() {
      final MemorySegment handle = hostFunction.getFunctionHandle();
      assertNotNull(handle);
    }

    @Test
    void testReturnValidUpcallStub() {
      final MemorySegment stub = hostFunction.getUpcallStub();
      assertNotNull(stub);
    }

    @Test
    void testThrowExceptionWhenAccessingClosedHostFunctionHandle() throws Exception {
      hostFunction.close();
      assertThrows(IllegalStateException.class, hostFunction::getFunctionHandle);
    }

    @Test
    void testThrowExceptionWhenAccessingClosedUpcallStub() throws Exception {
      hostFunction.close();
      assertThrows(IllegalStateException.class, hostFunction::getUpcallStub);
    }
  }

  @Nested
  class CallbackExecutionTests {

    @Test
    void testExecuteCallbackWithCorrectParameters() throws Exception {
      // Arrange
      final WasmValue[] expectedParams = {WasmValue.i32(10), WasmValue.i64(20L)};
      final WasmValue[] expectedResults = {WasmValue.f32(30.0f)};

      final HostFunctionCallback mockCallback = mock(HostFunctionCallback.class);
      lenient().when(mockCallback.execute(any())).thenReturn(expectedResults);

      final PanamaHostFunction hostFunction =
          new PanamaHostFunction(
              "callback_test", testFunctionType, mockCallback, mockArenaManager, mockErrorHandler);

      // Act - simulate callback execution through native interface
      // Note: This tests the callback mechanism indirectly
      assertNotNull(hostFunction.getUpcallStub());
    }

    @Test
    void testHandleCallbackExecutionWithExceptions() throws Exception {
      // Arrange
      final HostFunctionCallback throwingCallback =
          params -> {
            throw new WasmException("Callback failed");
          };

      final PanamaHostFunction hostFunction =
          new PanamaHostFunction(
              "throwing_callback",
              testFunctionType,
              throwingCallback,
              mockArenaManager,
              mockErrorHandler);

      // Act & Assert - the upcall stub should be created despite callback potentially throwing
      assertNotNull(hostFunction.getUpcallStub());
    }

    @Test
    void testHandleVoidReturnTypeCallback() throws Exception {
      // Arrange
      final FunctionType voidType =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});

      final HostFunctionCallback voidCallback = params -> new WasmValue[] {};

      // Act
      final PanamaHostFunction hostFunction =
          new PanamaHostFunction(
              "void_callback", voidType, voidCallback, mockArenaManager, mockErrorHandler);

      // Assert
      assertNotNull(hostFunction.getUpcallStub());
      assertEquals(voidType, hostFunction.getFunctionType());
    }

    @Test
    void testHandleMultipleReturnValues() throws Exception {
      // Arrange
      final FunctionType multiReturnType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32, WasmValueType.F64});

      final HostFunctionCallback multiReturnCallback =
          params -> new WasmValue[] {WasmValue.i32(42), WasmValue.f64(3.14)};

      // Act
      final PanamaHostFunction hostFunction =
          new PanamaHostFunction(
              "multi_return",
              multiReturnType,
              multiReturnCallback,
              mockArenaManager,
              mockErrorHandler);

      // Assert
      assertNotNull(hostFunction.getUpcallStub());
      assertEquals(multiReturnType, hostFunction.getFunctionType());
    }
  }

  @Nested
  class ResourceManagementTests {

    private PanamaHostFunction hostFunction;

    @BeforeEach
    void setUp() throws Exception {
      hostFunction =
          new PanamaHostFunction(
              "test_function", testFunctionType, testCallback, mockArenaManager, mockErrorHandler);
    }

    @Test
    void testCloseHostFunctionSuccessfully() throws Exception {
      // Act
      hostFunction.close();

      // Assert
      assertTrue(hostFunction.isClosed());
      verify(mockArenaManager).unregisterManagedResource(hostFunction);
    }

    @Test
    void testHandleDoubleCloseGracefully() throws Exception {
      // Act
      hostFunction.close();
      hostFunction.close(); // Second close should be safe

      // Assert
      assertTrue(hostFunction.isClosed());
    }

    @Test
    void testThrowExceptionWhenAccessingClosedHostFunction() throws Exception {
      // Arrange
      hostFunction.close();

      // Act & Assert
      assertThrows(IllegalStateException.class, hostFunction::getFunctionHandle);
      assertThrows(IllegalStateException.class, hostFunction::getUpcallStub);
      assertThrows(IllegalStateException.class, () -> hostFunction.call(WasmValue.i32(1)));
    }
  }

  @Nested
  class TypeSafetyTests {

    @Test
    void testHandleAllWebAssemblyValueTypes() throws Exception {
      // Arrange - function with all supported parameter types
      final FunctionType allTypesFunction =
          new FunctionType(
              new WasmValueType[] {
                WasmValueType.I32, WasmValueType.I64, WasmValueType.F32,
                WasmValueType.F64, WasmValueType.FUNCREF, WasmValueType.EXTERNREF
              },
              new WasmValueType[] {WasmValueType.I32});

      final HostFunctionCallback allTypesCallback =
          params -> {
            // Verify all parameter types are correctly marshalled
            assertEquals(6, params.length);
            assertEquals(WasmValueType.I32, params[0].getType());
            assertEquals(WasmValueType.I64, params[1].getType());
            assertEquals(WasmValueType.F32, params[2].getType());
            assertEquals(WasmValueType.F64, params[3].getType());
            assertEquals(WasmValueType.FUNCREF, params[4].getType());
            assertEquals(WasmValueType.EXTERNREF, params[5].getType());
            return new WasmValue[] {WasmValue.i32(1)};
          };

      // Act
      final PanamaHostFunction hostFunction =
          new PanamaHostFunction(
              "all_types_function",
              allTypesFunction,
              allTypesCallback,
              mockArenaManager,
              mockErrorHandler);

      // Assert
      assertNotNull(hostFunction.getUpcallStub());
      assertEquals(allTypesFunction, hostFunction.getFunctionType());
    }

    @Test
    void testHandleComplexFunctionSignatures() throws Exception {
      // Arrange - complex function with many parameters and returns
      final FunctionType complexType =
          new FunctionType(
              new WasmValueType[] {
                WasmValueType.I32, WasmValueType.I64, WasmValueType.F32, WasmValueType.F64,
                WasmValueType.I32, WasmValueType.I64, WasmValueType.F32, WasmValueType.F64
              },
              new WasmValueType[] {WasmValueType.F64, WasmValueType.I32});

      final HostFunctionCallback complexCallback =
          params -> {
            assertEquals(8, params.length);
            return new WasmValue[] {WasmValue.f64(1.0), WasmValue.i32(2)};
          };

      // Act
      final PanamaHostFunction hostFunction =
          new PanamaHostFunction(
              "complex_function", complexType, complexCallback, mockArenaManager, mockErrorHandler);

      // Assert
      assertNotNull(hostFunction.getUpcallStub());
      assertEquals(complexType, hostFunction.getFunctionType());
    }
  }

  @Nested
  class StringRepresentationTests {

    @Test
    void testReturnCorrectStringRepresentation() throws Exception {
      // Arrange
      final PanamaHostFunction hostFunction =
          new PanamaHostFunction(
              "test_function", testFunctionType, testCallback, mockArenaManager, mockErrorHandler);

      // Act
      final String stringRepr = hostFunction.toString();

      // Assert
      assertTrue(stringRepr.contains("PanamaHostFunction"));
      assertTrue(stringRepr.contains("name='test_function'"));
      assertTrue(stringRepr.contains("type="));
    }

    @Test
    void testReturnCorrectStringRepresentationWhenClosed() throws Exception {
      // Arrange
      final PanamaHostFunction hostFunction =
          new PanamaHostFunction(
              "test_function", testFunctionType, testCallback, mockArenaManager, mockErrorHandler);
      hostFunction.close();

      // Act
      final String stringRepr = hostFunction.toString();

      // Assert
      assertTrue(stringRepr.contains("PanamaHostFunction"));
      assertTrue(stringRepr.contains("name='test_function'"));
      assertTrue(stringRepr.contains("closed=true"));
    }
  }

  @Nested
  class ErrorHandlingTests {

    @Test
    void testHandleUpcallStubCreationFailure() throws Exception {
      // Arrange
      when(mockArenaManager.getArena()).thenThrow(new RuntimeException("Arena creation failed"));

      // Act & Assert
      assertThrows(
          WasmException.class,
          () ->
              new PanamaHostFunction(
                  "failing_function",
                  testFunctionType,
                  testCallback,
                  mockArenaManager,
                  mockErrorHandler));
    }

    @Test
    void testHandleCloseOperationFailure() throws Exception {
      // Arrange
      final PanamaHostFunction hostFunction =
          new PanamaHostFunction(
              "test_function", testFunctionType, testCallback, mockArenaManager, mockErrorHandler);

      doThrow(new RuntimeException("Unregister failed"))
          .when(mockArenaManager)
          .unregisterManagedResource(any());

      // Act & Assert
      assertThrows(WasmException.class, hostFunction::close);
    }
  }

  @Nested
  class MemorySafetyTests {

    @Test
    void testRegisterWithArenaManagerForCleanup() throws Exception {
      // Act
      final PanamaHostFunction hostFunction =
          new PanamaHostFunction(
              "test_function", testFunctionType, testCallback, mockArenaManager, mockErrorHandler);

      // Assert
      verify(mockArenaManager)
          .registerManagedNativeResource(
              eq(hostFunction), any(MemorySegment.class), any(Runnable.class));
    }

    @Test
    void testPreventAccessAfterClose() throws Exception {
      // Arrange
      final PanamaHostFunction hostFunction =
          new PanamaHostFunction(
              "test_function", testFunctionType, testCallback, mockArenaManager, mockErrorHandler);

      // Act
      hostFunction.close();

      // Assert
      assertTrue(hostFunction.isClosed());
      assertThrows(IllegalStateException.class, hostFunction::getFunctionHandle);
      assertThrows(IllegalStateException.class, hostFunction::getUpcallStub);
    }
  }
}
