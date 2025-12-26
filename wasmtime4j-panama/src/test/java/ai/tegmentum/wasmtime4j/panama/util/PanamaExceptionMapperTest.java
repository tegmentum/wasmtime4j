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

package ai.tegmentum.wasmtime4j.panama.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.MemorySegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaExceptionMapper} class.
 *
 * <p>This test class verifies the exception mapping functionality for Panama FFI operations.
 */
@DisplayName("PanamaExceptionMapper Tests")
class PanamaExceptionMapperTest {

  private PanamaExceptionMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new PanamaExceptionMapper();
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaExceptionMapper should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaExceptionMapper.class.getModifiers()),
          "PanamaExceptionMapper should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should create mapper")
    void constructorShouldCreateMapper() {
      final PanamaExceptionMapper newMapper = new PanamaExceptionMapper();
      assertNotNull(newMapper, "Mapper should be created");
    }
  }

  @Nested
  @DisplayName("mapException Tests - WasmException passthrough")
  class MapExceptionWasmExceptionTests {

    @Test
    @DisplayName("mapException should return same WasmException")
    void mapExceptionShouldReturnSameWasmException() {
      final WasmException original = new WasmException("Original error");

      final WasmException result = PanamaExceptionMapper.mapException(original);

      assertSame(original, result, "Should return same exception");
    }

    @Test
    @DisplayName("mapException should return same CompilationException")
    void mapExceptionShouldReturnSameCompilationException() {
      final CompilationException original = new CompilationException("Compilation error");

      final WasmException result = PanamaExceptionMapper.mapException(original);

      assertSame(original, result, "Should return same exception");
    }

    @Test
    @DisplayName("mapException should return same RuntimeException")
    void mapExceptionShouldReturnSameRuntimeException() {
      final RuntimeException original = new RuntimeException("Runtime error");

      final WasmException result = PanamaExceptionMapper.mapException(original);

      assertSame(original, result, "Should return same exception");
    }

    @Test
    @DisplayName("mapException should return same ValidationException")
    void mapExceptionShouldReturnSameValidationException() {
      final ValidationException original = new ValidationException("Validation error");

      final WasmException result = PanamaExceptionMapper.mapException(original);

      assertSame(original, result, "Should return same exception");
    }
  }

  @Nested
  @DisplayName("mapException Tests - Java exception mapping")
  class MapExceptionJavaExceptionTests {

    @Test
    @DisplayName("mapException should handle null exception")
    void mapExceptionShouldHandleNullException() {
      final WasmException result = PanamaExceptionMapper.mapException(null);

      assertNotNull(result, "Should return exception for null");
      assertTrue(result.getMessage().contains("Unknown"), "Should mention unknown error");
    }

    @Test
    @DisplayName("mapException should map OutOfMemoryError")
    void mapExceptionShouldMapOutOfMemoryError() {
      final OutOfMemoryError oom = new OutOfMemoryError("Heap exhausted");

      final WasmException result = PanamaExceptionMapper.mapException(oom);

      assertNotNull(result, "Result should not be null");
      assertTrue(
          result.getMessage().toLowerCase().contains("memory"), "Should mention memory");
      assertEquals(oom, result.getCause(), "Should preserve cause");
    }

    @Test
    @DisplayName("mapException should map IllegalArgumentException to ValidationException")
    void mapExceptionShouldMapIllegalArgumentExceptionToValidationException() {
      final IllegalArgumentException iae = new IllegalArgumentException("Invalid param");

      final WasmException result = PanamaExceptionMapper.mapException(iae);

      assertTrue(result instanceof ValidationException, "Should be ValidationException");
      assertEquals(iae, result.getCause(), "Should preserve cause");
    }

    @Test
    @DisplayName("mapException should map IllegalStateException to RuntimeException")
    void mapExceptionShouldMapIllegalStateExceptionToRuntimeException() {
      final IllegalStateException ise = new IllegalStateException("Bad state");

      final WasmException result = PanamaExceptionMapper.mapException(ise);

      assertTrue(result instanceof RuntimeException, "Should be RuntimeException");
      assertEquals(ise, result.getCause(), "Should preserve cause");
    }

    @Test
    @DisplayName("mapException should map SecurityException")
    void mapExceptionShouldMapSecurityException() {
      final SecurityException se = new SecurityException("Access denied");

      final WasmException result = PanamaExceptionMapper.mapException(se);

      assertNotNull(result, "Result should not be null");
      assertTrue(
          result.getMessage().toLowerCase().contains("security"), "Should mention security");
      assertEquals(se, result.getCause(), "Should preserve cause");
    }

    @Test
    @DisplayName("mapException should map UnsupportedOperationException")
    void mapExceptionShouldMapUnsupportedOperationException() {
      final UnsupportedOperationException uoe = new UnsupportedOperationException("Not supported");

      final WasmException result = PanamaExceptionMapper.mapException(uoe);

      assertNotNull(result, "Result should not be null");
      assertTrue(
          result.getMessage().toLowerCase().contains("unsupported"), "Should mention unsupported");
      assertEquals(uoe, result.getCause(), "Should preserve cause");
    }
  }

  @Nested
  @DisplayName("mapException Tests - Message pattern matching")
  class MapExceptionMessagePatternTests {

    @Test
    @DisplayName("mapException should detect compilation errors in message")
    void mapExceptionShouldDetectCompilationErrorsInMessage() {
      final Exception e = new Exception("Module compilation failed at line 10");

      final WasmException result = PanamaExceptionMapper.mapException(e);

      assertTrue(result instanceof CompilationException, "Should be CompilationException");
    }

    @Test
    @DisplayName("mapException should detect validation errors in message")
    void mapExceptionShouldDetectValidationErrorsInMessage() {
      final Exception e = new Exception("Validation of module failed");

      final WasmException result = PanamaExceptionMapper.mapException(e);

      assertTrue(result instanceof ValidationException, "Should be ValidationException");
    }

    @Test
    @DisplayName("mapException should detect runtime errors in message")
    void mapExceptionShouldDetectRuntimeErrorsInMessage() {
      final Exception e = new Exception("Runtime execution error occurred");

      final WasmException result = PanamaExceptionMapper.mapException(e);

      assertTrue(result instanceof RuntimeException, "Should be RuntimeException");
    }

    @Test
    @DisplayName("mapException should detect memory errors in message")
    void mapExceptionShouldDetectMemoryErrorsInMessage() {
      final Exception e = new Exception("Memory allocation error");

      final WasmException result = PanamaExceptionMapper.mapException(e);

      assertNotNull(result, "Result should not be null");
      assertTrue(result.getMessage().toLowerCase().contains("memory"), "Should mention memory");
    }

    @Test
    @DisplayName("mapException should detect function errors in message")
    void mapExceptionShouldDetectFunctionErrorsInMessage() {
      final Exception e = new Exception("Function call failed unexpectedly");

      final WasmException result = PanamaExceptionMapper.mapException(e);

      assertTrue(result instanceof RuntimeException, "Should be RuntimeException");
    }

    @Test
    @DisplayName("mapException should handle generic exception")
    void mapExceptionShouldHandleGenericException() {
      final Exception e = new Exception("Some unknown error");

      final WasmException result = PanamaExceptionMapper.mapException(e);

      assertNotNull(result, "Result should not be null");
      assertTrue(result instanceof WasmException, "Should be WasmException");
      assertTrue(result.getMessage().contains("Native runtime error"), "Should be native error");
    }
  }

  @Nested
  @DisplayName("mapNativeError(int, String) Tests")
  class MapNativeErrorCodeTests {

    @Test
    @DisplayName("mapNativeError should handle success code with warning")
    void mapNativeErrorShouldHandleSuccessCodeWithWarning() {
      final WasmException result = mapper.mapNativeError(0, "Success");

      assertNotNull(result, "Result should not be null");
      assertTrue(result.getMessage().contains("success"), "Should mention success");
    }

    @Test
    @DisplayName("mapNativeError should map code 1 to CompilationException")
    void mapNativeErrorShouldMapCode1ToCompilationException() {
      final WasmException result = mapper.mapNativeError(1, "Compile failed");

      assertTrue(result instanceof CompilationException, "Should be CompilationException");
    }

    @Test
    @DisplayName("mapNativeError should map code 2 to ValidationException")
    void mapNativeErrorShouldMapCode2ToValidationException() {
      final WasmException result = mapper.mapNativeError(2, "Validation failed");

      assertTrue(result instanceof ValidationException, "Should be ValidationException");
    }

    @Test
    @DisplayName("mapNativeError should map code 3 to RuntimeException")
    void mapNativeErrorShouldMapCode3ToRuntimeException() {
      final WasmException result = mapper.mapNativeError(3, "Runtime failed");

      assertTrue(result instanceof RuntimeException, "Should be RuntimeException");
    }

    @Test
    @DisplayName("mapNativeError should map code 4 to memory error")
    void mapNativeErrorShouldMapCode4ToMemoryError() {
      final WasmException result = mapper.mapNativeError(4, "Memory failed");

      assertNotNull(result, "Result should not be null");
      assertTrue(
          result.getMessage().toLowerCase().contains("memory"), "Should mention memory");
    }

    @Test
    @DisplayName("mapNativeError should map code 5 to function call error")
    void mapNativeErrorShouldMapCode5ToFunctionCallError() {
      final WasmException result = mapper.mapNativeError(5, "Call failed");

      assertTrue(result instanceof RuntimeException, "Should be RuntimeException");
    }

    @Test
    @DisplayName("mapNativeError should map code 6 to resource limit error")
    void mapNativeErrorShouldMapCode6ToResourceLimitError() {
      final WasmException result = mapper.mapNativeError(6, "Limit exceeded");

      assertNotNull(result, "Result should not be null");
      assertTrue(result.getMessage().contains("limit"), "Should mention limit");
    }

    @Test
    @DisplayName("mapNativeError should map code 7 to import error")
    void mapNativeErrorShouldMapCode7ToImportError() {
      final WasmException result = mapper.mapNativeError(7, "Import failed");

      assertNotNull(result, "Result should not be null");
      assertTrue(result.getMessage().toLowerCase().contains("import"), "Should mention import");
    }

    @Test
    @DisplayName("mapNativeError should map code 8 to export error")
    void mapNativeErrorShouldMapCode8ToExportError() {
      final WasmException result = mapper.mapNativeError(8, "Export not found");

      assertNotNull(result, "Result should not be null");
      assertTrue(result.getMessage().toLowerCase().contains("export"), "Should mention export");
    }

    @Test
    @DisplayName("mapNativeError should map unknown code to generic error")
    void mapNativeErrorShouldMapUnknownCodeToGenericError() {
      final WasmException result = mapper.mapNativeError(999, "Unknown");

      assertNotNull(result, "Result should not be null");
      assertTrue(result.getMessage().contains("999"), "Should contain error code");
    }

    @Test
    @DisplayName("mapNativeError should use default message for null")
    void mapNativeErrorShouldUseDefaultMessageForNull() {
      final WasmException result = mapper.mapNativeError(1, null);

      assertNotNull(result, "Result should not be null");
      assertTrue(result.getMessage().contains("1"), "Should contain error code");
    }
  }

  @Nested
  @DisplayName("mapNativeError(MemorySegment) Tests")
  class MapNativeErrorPointerTests {

    @Test
    @DisplayName("mapNativeError should return null for null pointer")
    void mapNativeErrorShouldReturnNullForNullPointer() {
      final WasmException result = mapper.mapNativeError((MemorySegment) null);

      assertNull(result, "Should return null for null pointer");
    }

    @Test
    @DisplayName("mapNativeError should return null for NULL segment")
    void mapNativeErrorShouldReturnNullForNullSegment() {
      final WasmException result = mapper.mapNativeError(MemorySegment.NULL);

      assertNull(result, "Should return null for NULL segment");
    }
  }

  @Nested
  @DisplayName("createXxxException Tests")
  class CreateExceptionTests {

    @Test
    @DisplayName("createCompilationException should create with prefix")
    void createCompilationExceptionShouldCreateWithPrefix() {
      final CompilationException result =
          mapper.createCompilationException("Failed to compile", null);

      assertNotNull(result, "Result should not be null");
      assertTrue(result.getMessage().contains("Panama FFI"), "Should contain prefix");
      assertTrue(result.getMessage().contains("Failed to compile"), "Should contain message");
    }

    @Test
    @DisplayName("createCompilationException should preserve cause")
    void createCompilationExceptionShouldPreserveCause() {
      final Throwable cause = new Exception("Root cause");
      final CompilationException result =
          mapper.createCompilationException("Failed to compile", cause);

      assertEquals(cause, result.getCause(), "Should preserve cause");
    }

    @Test
    @DisplayName("createRuntimeException should create with prefix")
    void createRuntimeExceptionShouldCreateWithPrefix() {
      final RuntimeException result = mapper.createRuntimeException("Runtime error", null);

      assertNotNull(result, "Result should not be null");
      assertTrue(result.getMessage().contains("Panama FFI"), "Should contain prefix");
    }

    @Test
    @DisplayName("createValidationException should create with prefix")
    void createValidationExceptionShouldCreateWithPrefix() {
      final ValidationException result = mapper.createValidationException("Validation error", null);

      assertNotNull(result, "Result should not be null");
      assertTrue(result.getMessage().contains("Panama FFI"), "Should contain prefix");
    }

    @Test
    @DisplayName("createWasmException should create with prefix")
    void createWasmExceptionShouldCreateWithPrefix() {
      final WasmException result = mapper.createWasmException("Generic error", null);

      assertNotNull(result, "Result should not be null");
      assertTrue(result.getMessage().contains("Panama FFI"), "Should contain prefix");
    }
  }

  @Nested
  @DisplayName("isRecoverableError Tests")
  class IsRecoverableErrorTests {

    @Test
    @DisplayName("isRecoverableError should return false for null")
    void isRecoverableErrorShouldReturnFalseForNull() {
      assertFalse(mapper.isRecoverableError(null), "Should return false for null");
    }

    @Test
    @DisplayName("isRecoverableError should return false for ValidationException")
    void isRecoverableErrorShouldReturnFalseForValidationException() {
      final ValidationException ve = new ValidationException("Validation error");

      assertFalse(mapper.isRecoverableError(ve), "ValidationException should not be recoverable");
    }

    @Test
    @DisplayName("isRecoverableError should return false for CompilationException")
    void isRecoverableErrorShouldReturnFalseForCompilationException() {
      final CompilationException ce = new CompilationException("Compilation error");

      assertFalse(mapper.isRecoverableError(ce), "CompilationException should not be recoverable");
    }

    @Test
    @DisplayName("isRecoverableError should return true for 'not found' error")
    void isRecoverableErrorShouldReturnTrueForNotFoundError() {
      final WasmException we = new WasmException("Export not found");

      assertTrue(mapper.isRecoverableError(we), "'Not found' error should be recoverable");
    }

    @Test
    @DisplayName("isRecoverableError should return true for 'unavailable' error")
    void isRecoverableErrorShouldReturnTrueForUnavailableError() {
      final WasmException we = new WasmException("Resource unavailable");

      assertTrue(mapper.isRecoverableError(we), "'Unavailable' error should be recoverable");
    }

    @Test
    @DisplayName("isRecoverableError should return true for 'timeout' error")
    void isRecoverableErrorShouldReturnTrueForTimeoutError() {
      final WasmException we = new WasmException("Operation timeout");

      assertTrue(mapper.isRecoverableError(we), "'Timeout' error should be recoverable");
    }

    @Test
    @DisplayName("isRecoverableError should return false for 'out of memory' error")
    void isRecoverableErrorShouldReturnFalseForOutOfMemoryError() {
      final WasmException we = new WasmException("Out of memory");

      assertFalse(mapper.isRecoverableError(we), "'Out of memory' error should not be recoverable");
    }

    @Test
    @DisplayName("isRecoverableError should return false for 'corruption' error")
    void isRecoverableErrorShouldReturnFalseForCorruptionError() {
      final WasmException we = new WasmException("Memory corruption detected");

      assertFalse(mapper.isRecoverableError(we), "'Corruption' error should not be recoverable");
    }

    @Test
    @DisplayName("isRecoverableError should return false for 'invalid' error")
    void isRecoverableErrorShouldReturnFalseForInvalidError() {
      final WasmException we = new WasmException("Invalid state");

      assertFalse(mapper.isRecoverableError(we), "'Invalid' error should not be recoverable");
    }

    @Test
    @DisplayName("isRecoverableError should return true for RuntimeException")
    void isRecoverableErrorShouldReturnTrueForRuntimeException() {
      final RuntimeException re = new RuntimeException("Generic runtime error");

      assertTrue(mapper.isRecoverableError(re), "RuntimeException might be recoverable");
    }
  }
}
