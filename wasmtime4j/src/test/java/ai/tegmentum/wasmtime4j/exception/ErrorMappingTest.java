package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for error mapping from native codes to Java exceptions.
 *
 * <p>These tests verify that native Wasmtime error codes and messages are correctly mapped to
 * specific Java exception types with proper context information and recovery suggestions.
 */
class ErrorMappingTest {

  @Nested
  @DisplayName("Compilation Error Mapping Tests")
  class CompilationErrorMappingTests {

    @Test
    @DisplayName("Should map compilation error with out of memory message")
    void shouldMapCompilationErrorWithOutOfMemory() {
      final String errorMessage = "Compilation failed: out of memory during optimization";
      final WasmException exception =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, errorMessage);

      assertInstanceOf(ModuleCompilationException.class, exception);
      final ModuleCompilationException compilationException =
          (ModuleCompilationException) exception;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY,
          compilationException.getErrorType());
      assertTrue(compilationException.getMessage().contains(errorMessage));
      assertNotNull(compilationException.getRecoverySuggestion());
    }

    @Test
    @DisplayName("Should map compilation error with function complexity message")
    void shouldMapCompilationErrorWithFunctionComplexity() {
      final String errorMessage = "Function too complex for compiler in function: test_func";
      final WasmException exception =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, errorMessage);

      assertInstanceOf(ModuleCompilationException.class, exception);
      final ModuleCompilationException compilationException =
          (ModuleCompilationException) exception;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.FUNCTION_TOO_COMPLEX,
          compilationException.getErrorType());
      assertEquals("test_func", compilationException.getFunctionName());
    }

    @Test
    @DisplayName("Should map compilation error with optimization failure")
    void shouldMapCompilationErrorWithOptimizationFailure() {
      final String errorMessage = "Optimization pass failed during register allocation";
      final WasmException exception =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, errorMessage);

      assertInstanceOf(ModuleCompilationException.class, exception);
      final ModuleCompilationException compilationException =
          (ModuleCompilationException) exception;
      assertEquals(
          ModuleCompilationException.CompilationErrorType.REGISTER_ALLOCATION_FAILED,
          compilationException.getErrorType());
      assertEquals(
          ModuleCompilationException.CompilationPhase.REGISTER_ALLOCATION,
          compilationException.getPhase());
    }
  }

  @Nested
  @DisplayName("Validation Error Mapping Tests")
  class ValidationErrorMappingTests {

    @Test
    @DisplayName("Should map validation error with invalid magic number")
    void shouldMapValidationErrorWithInvalidMagic() {
      final String errorMessage = "Invalid magic number in WebAssembly module";
      final WasmException exception =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, errorMessage);

      assertInstanceOf(ModuleValidationException.class, exception);
      final ModuleValidationException validationException = (ModuleValidationException) exception;
      assertEquals(
          ModuleValidationException.ValidationErrorType.INVALID_MAGIC_NUMBER,
          validationException.getErrorType());
      assertTrue(
          validationException.getRecoverySuggestion().contains("valid WebAssembly bytecode"));
    }

    @Test
    @DisplayName("Should map validation error with type mismatch")
    void shouldMapValidationErrorWithTypeMismatch() {
      final String errorMessage =
          "Type mismatch in function signature at section: code offset: 1024";
      final WasmException exception =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, errorMessage);

      assertInstanceOf(ModuleValidationException.class, exception);
      final ModuleValidationException validationException = (ModuleValidationException) exception;
      assertEquals(
          ModuleValidationException.ValidationErrorType.TYPE_MISMATCH,
          validationException.getErrorType());
      assertEquals("code", validationException.getModuleSection());
      assertEquals(Integer.valueOf(1024), validationException.getByteOffset());
    }

    @Test
    @DisplayName("Should map validation error with unsupported feature")
    void shouldMapValidationErrorWithUnsupportedFeature() {
      final String errorMessage = "Unsupported WebAssembly feature: bulk memory operations";
      final WasmException exception =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, errorMessage);

      assertInstanceOf(ModuleValidationException.class, exception);
      final ModuleValidationException validationException = (ModuleValidationException) exception;
      assertEquals(
          ModuleValidationException.ValidationErrorType.UNSUPPORTED_FEATURE,
          validationException.getErrorType());
      assertTrue(validationException.getRecoverySuggestion().contains("engine configuration"));
    }
  }

  @Nested
  @DisplayName("Runtime Error Mapping Tests")
  class RuntimeErrorMappingTests {

    @Test
    @DisplayName("Should map runtime error to trap exception for stack overflow")
    void shouldMapRuntimeErrorToTrapForStackOverflow() {
      final String errorMessage = "trap: stack overflow in function: recursive_func offset: 512";
      final WasmException exception = ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, errorMessage);

      assertInstanceOf(TrapException.class, exception);
      final TrapException trapException = (TrapException) exception;
      assertEquals(TrapException.TrapType.STACK_OVERFLOW, trapException.getTrapType());
      assertEquals("recursive_func", trapException.getFunctionName());
      assertEquals(Integer.valueOf(512), trapException.getInstructionOffset());
      assertTrue(trapException.isResourceExhaustionError());
    }

    @Test
    @DisplayName("Should map runtime error to trap exception for memory out of bounds")
    void shouldMapRuntimeErrorToTrapForMemoryOutOfBounds() {
      final String errorMessage = "trap: memory out of bounds access at offset: 256";
      final WasmException exception = ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, errorMessage);

      assertInstanceOf(TrapException.class, exception);
      final TrapException trapException = (TrapException) exception;
      assertEquals(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, trapException.getTrapType());
      assertTrue(trapException.isBoundsError());
      assertTrue(trapException.isMemoryError());
    }

    @Test
    @DisplayName("Should map runtime error to trap exception for division by zero")
    void shouldMapRuntimeErrorToTrapForDivisionByZero() {
      final String errorMessage = "trap: integer division by zero in function: math_ops";
      final WasmException exception = ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, errorMessage);

      assertInstanceOf(TrapException.class, exception);
      final TrapException trapException = (TrapException) exception;
      assertEquals(TrapException.TrapType.INTEGER_DIVISION_BY_ZERO, trapException.getTrapType());
      assertTrue(trapException.isArithmeticError());
      assertEquals("math_ops", trapException.getFunctionName());
    }

    @Test
    @DisplayName("Should map non-trap runtime error to RuntimeException")
    void shouldMapNonTrapRuntimeErrorToRuntimeException() {
      final String errorMessage = "Function execution failed: host function returned error";
      final WasmException exception = ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, errorMessage);

      assertInstanceOf(RuntimeException.class, exception);
      assertNotInstanceOf(TrapException.class, exception);
      final RuntimeException runtimeException = (RuntimeException) exception;
      assertEquals(
          RuntimeException.RuntimeErrorType.FUNCTION_EXECUTION_FAILED,
          runtimeException.getErrorType());
      assertTrue(runtimeException.isFunctionError());
    }
  }

  @Nested
  @DisplayName("Instantiation Error Mapping Tests")
  class InstantiationErrorMappingTests {

    @Test
    @DisplayName("Should map instantiation error with missing import")
    void shouldMapInstantiationErrorWithMissingImport() {
      final String errorMessage = "Import not found: env.console_log during import resolution";
      final WasmException exception =
          ErrorMapper.mapError(ErrorMapper.INSTANCE_ERROR, errorMessage);

      assertInstanceOf(ModuleInstantiationException.class, exception);
      final ModuleInstantiationException instantiationException =
          (ModuleInstantiationException) exception;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.MISSING_IMPORT,
          instantiationException.getErrorType());
      assertEquals(
          ModuleInstantiationException.InstantiationPhase.IMPORT_RESOLUTION,
          instantiationException.getPhase());
      assertTrue(instantiationException.isImportError());
    }

    @Test
    @DisplayName("Should map instantiation error with memory allocation failure")
    void shouldMapInstantiationErrorWithMemoryAllocation() {
      final String errorMessage = "Memory allocation failed: insufficient system memory";
      final WasmException exception =
          ErrorMapper.mapError(ErrorMapper.INSTANCE_ERROR, errorMessage);

      assertInstanceOf(ModuleInstantiationException.class, exception);
      final ModuleInstantiationException instantiationException =
          (ModuleInstantiationException) exception;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.MEMORY_ALLOCATION_FAILED,
          instantiationException.getErrorType());
      assertEquals(
          ModuleInstantiationException.InstantiationPhase.MEMORY_ALLOCATION,
          instantiationException.getPhase());
      assertTrue(instantiationException.isResourceError());
    }

    @Test
    @DisplayName("Should map instantiation error with start function failure")
    void shouldMapInstantiationErrorWithStartFunction() {
      final String errorMessage = "Start function execution failed: trap occurred";
      final WasmException exception =
          ErrorMapper.mapError(ErrorMapper.INSTANCE_ERROR, errorMessage);

      assertInstanceOf(ModuleInstantiationException.class, exception);
      final ModuleInstantiationException instantiationException =
          (ModuleInstantiationException) exception;
      assertEquals(
          ModuleInstantiationException.InstantiationErrorType.START_FUNCTION_FAILED,
          instantiationException.getErrorType());
      assertEquals(
          ModuleInstantiationException.InstantiationPhase.START_FUNCTION_EXEC,
          instantiationException.getPhase());
      assertTrue(instantiationException.isInitializationError());
    }
  }

  @Nested
  @DisplayName("Linking Error Mapping Tests")
  class LinkingErrorMappingTests {

    @Test
    @DisplayName("Should map linking error with import not found")
    void shouldMapLinkingErrorWithImportNotFound() {
      final String errorMessage = "Import not found: wasi_snapshot_preview1.fd_write";
      final WasmException exception =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, errorMessage);

      assertInstanceOf(LinkingException.class, exception);
      final LinkingException linkingException = (LinkingException) exception;
      assertEquals(
          LinkingException.LinkingErrorType.IMPORT_NOT_FOUND, linkingException.getErrorType());
      assertTrue(linkingException.isMissingItemError());
    }

    @Test
    @DisplayName("Should map linking error with function signature mismatch")
    void shouldMapLinkingErrorWithSignatureMismatch() {
      final String errorMessage = "Function signature mismatch for import: env.callback";
      final WasmException exception =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, errorMessage);

      assertInstanceOf(LinkingException.class, exception);
      final LinkingException linkingException = (LinkingException) exception;
      assertEquals(
          LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH,
          linkingException.getErrorType());
      assertTrue(linkingException.isTypeMismatchError());
    }

    @Test
    @DisplayName("Should map linking error with WASI import failure")
    void shouldMapLinkingErrorWithWasiImport() {
      final String errorMessage = "WASI import resolution failed: unsupported operation";
      final WasmException exception =
          ErrorMapper.mapError(ErrorMapper.IMPORT_EXPORT_ERROR, errorMessage);

      assertInstanceOf(LinkingException.class, exception);
      final LinkingException linkingException = (LinkingException) exception;
      assertEquals(
          LinkingException.LinkingErrorType.WASI_IMPORT_FAILED, linkingException.getErrorType());
      assertTrue(linkingException.isHostFunctionError());
    }
  }

  @Nested
  @DisplayName("WASI Error Mapping Tests")
  class WasiErrorMappingTests {

    @Test
    @DisplayName("Should map WASI error to file system exception for file not found")
    void shouldMapWasiErrorToFileSystemForNotFound() {
      final String errorMessage = "File not found: /path/to/file.txt errno: 44";
      final WasmException exception = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, errorMessage);

      assertInstanceOf(WasiFileSystemException.class, exception);
      final WasiFileSystemException fileSystemException = (WasiFileSystemException) exception;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.NOT_FOUND,
          fileSystemException.getFileSystemErrorType());
      assertEquals(Integer.valueOf(44), fileSystemException.getErrnoCode());
      assertTrue(fileSystemException.isExistenceError());
      assertFalse(fileSystemException.isTransientError());
    }

    @Test
    @DisplayName("Should map WASI error to file system exception for permission denied")
    void shouldMapWasiErrorToFileSystemForPermissionDenied() {
      final String errorMessage = "Permission denied accessing file: /root/secret.txt";
      final WasmException exception = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, errorMessage);

      assertInstanceOf(WasiFileSystemException.class, exception);
      final WasiFileSystemException fileSystemException = (WasiFileSystemException) exception;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.PERMISSION_DENIED,
          fileSystemException.getFileSystemErrorType());
      assertTrue(fileSystemException.isPermissionError());
      assertEquals(WasiException.ErrorCategory.FILE_SYSTEM, fileSystemException.getCategory());
    }

    @Test
    @DisplayName("Should map WASI error to file system exception for I/O error")
    void shouldMapWasiErrorToFileSystemForIoError() {
      final String errorMessage = "I/O error reading from file descriptor";
      final WasmException exception = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, errorMessage);

      assertInstanceOf(WasiFileSystemException.class, exception);
      final WasiFileSystemException fileSystemException = (WasiFileSystemException) exception;
      assertEquals(
          WasiFileSystemException.FileSystemErrorType.IO_ERROR,
          fileSystemException.getFileSystemErrorType());
      assertTrue(fileSystemException.isTransientError());
      assertTrue(fileSystemException.isRetryable());
    }

    @Test
    @DisplayName("Should map WASI error to generic WasiException for network errors")
    void shouldMapWasiErrorToGenericForNetwork() {
      final String errorMessage = "Network connection failed: timeout";
      final WasmException exception = ErrorMapper.mapError(ErrorMapper.WASI_ERROR, errorMessage);

      assertInstanceOf(WasiException.class, exception);
      assertNotInstanceOf(WasiFileSystemException.class, exception);
      final WasiException wasiException = (WasiException) exception;
      assertEquals(WasiException.ErrorCategory.NETWORK, wasiException.getCategory());
      assertTrue(wasiException.isNetworkError());
    }
  }

  @Nested
  @DisplayName("Error Chain and Context Tests")
  class ErrorChainAndContextTests {

    @Test
    @DisplayName("Should preserve error chain with underlying cause")
    void shouldPreserveErrorChainWithCause() {
      final RuntimeException originalCause = new RuntimeException("Original error");
      final String errorMessage = "trap: stack overflow";
      final WasmException exception =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, errorMessage, originalCause);

      assertInstanceOf(TrapException.class, exception);
      assertEquals(originalCause, exception.getCause());
      assertTrue(exception.getMessage().contains(errorMessage));
    }

    @Test
    @DisplayName("Should handle null error message gracefully")
    void shouldHandleNullErrorMessageGracefully() {
      final WasmException exception = ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, null);

      assertInstanceOf(WasmException.class, exception);
      assertTrue(exception.getMessage().contains("Unknown error"));
    }

    @Test
    @DisplayName("Should handle empty error message gracefully")
    void shouldHandleEmptyErrorMessageGracefully() {
      final WasmException exception = ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "");

      assertInstanceOf(WasmException.class, exception);
      assertTrue(exception.getMessage().contains("Unknown error"));
    }

    @Test
    @DisplayName("Should handle unknown error codes gracefully")
    void shouldHandleUnknownErrorCodesGracefully() {
      final String errorMessage = "Unknown error occurred";
      final WasmException exception = ErrorMapper.mapError(-999, errorMessage);

      assertInstanceOf(WasmException.class, exception);
      assertTrue(exception.getMessage().contains("Error code -999"));
      assertTrue(exception.getMessage().contains(errorMessage));
    }

    @Test
    @DisplayName("Should extract context information from complex error messages")
    void shouldExtractContextFromComplexErrorMessages() {
      final String errorMessage =
          "trap: memory out of bounds in function: buffer_ops offset: 2048 section: code";
      final WasmException exception = ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, errorMessage);

      assertInstanceOf(TrapException.class, exception);
      final TrapException trapException = (TrapException) exception;
      assertEquals(TrapException.TrapType.MEMORY_OUT_OF_BOUNDS, trapException.getTrapType());
      assertEquals("buffer_ops", trapException.getFunctionName());
      assertEquals(Integer.valueOf(2048), trapException.getInstructionOffset());
    }
  }

  @Nested
  @DisplayName("Error Type Classification Tests")
  class ErrorTypeClassificationTests {

    @Test
    @DisplayName("Should classify compilation errors correctly")
    void shouldClassifyCompilationErrorsCorrectly() {
      final WasmException memoryError =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "out of memory during compilation");
      final WasmException complexityError =
          ErrorMapper.mapError(
              ErrorMapper.COMPILATION_ERROR, "function too complex for compilation");
      final WasmException featureError =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "unsupported WebAssembly feature");

      assertInstanceOf(ModuleCompilationException.class, memoryError);
      assertInstanceOf(ModuleCompilationException.class, complexityError);
      assertInstanceOf(ModuleCompilationException.class, featureError);

      assertTrue(((ModuleCompilationException) memoryError).isResourceError());
      assertTrue(((ModuleCompilationException) complexityError).isComplexityError());
      assertTrue(((ModuleCompilationException) featureError).isFeatureError());
    }

    @Test
    @DisplayName("Should classify validation errors correctly")
    void shouldClassifyValidationErrorsCorrectly() {
      final WasmException structuralError =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "malformed WebAssembly module");
      final WasmException typeError =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "type mismatch in function signature");
      final WasmException importError =
          ErrorMapper.mapError(ErrorMapper.VALIDATION_ERROR, "invalid import declaration");

      assertInstanceOf(ModuleValidationException.class, structuralError);
      assertInstanceOf(ModuleValidationException.class, typeError);
      assertInstanceOf(ModuleValidationException.class, importError);

      assertTrue(((ModuleValidationException) structuralError).isStructuralError());
      assertTrue(((ModuleValidationException) typeError).isTypeError());
      assertTrue(((ModuleValidationException) importError).isImportExportError());
    }

    @Test
    @DisplayName("Should classify trap errors correctly")
    void shouldClassifyTrapErrorsCorrectly() {
      final WasmException boundsError =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: memory out of bounds");
      final WasmException arithmeticError =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: integer division by zero");
      final WasmException controlFlowError =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: indirect call to null");

      assertInstanceOf(TrapException.class, boundsError);
      assertInstanceOf(TrapException.class, arithmeticError);
      assertInstanceOf(TrapException.class, controlFlowError);

      assertTrue(((TrapException) boundsError).isBoundsError());
      assertTrue(((TrapException) arithmeticError).isArithmeticError());
      assertTrue(((TrapException) controlFlowError).isControlFlowError());
    }
  }

  @Nested
  @DisplayName("Recovery Suggestion Tests")
  class RecoverySuggestionTests {

    @Test
    @DisplayName("Should provide meaningful recovery suggestions for compilation errors")
    void shouldProvideRecoverySuggestionsForCompilationErrors() {
      final WasmException memoryError =
          ErrorMapper.mapError(ErrorMapper.COMPILATION_ERROR, "out of memory during compilation");

      assertInstanceOf(ModuleCompilationException.class, memoryError);
      final String suggestion = ((ModuleCompilationException) memoryError).getRecoverySuggestion();
      assertNotNull(suggestion);
      assertTrue(suggestion.contains("heap size") || suggestion.contains("module"));
    }

    @Test
    @DisplayName("Should provide meaningful recovery suggestions for trap errors")
    void shouldProvideRecoverySuggestionsForTrapErrors() {
      final WasmException stackOverflow =
          ErrorMapper.mapError(ErrorMapper.RUNTIME_ERROR, "trap: stack overflow");

      assertInstanceOf(TrapException.class, stackOverflow);
      final String suggestion = ((TrapException) stackOverflow).getRecoverySuggestion();
      assertNotNull(suggestion);
      assertTrue(suggestion.contains("recursion") || suggestion.contains("stack"));
    }

    @Test
    @DisplayName("Should provide meaningful recovery suggestions for WASI errors")
    void shouldProvideRecoverySuggestionsForWasiErrors() {
      final WasmException fileNotFound =
          ErrorMapper.mapError(ErrorMapper.WASI_ERROR, "file not found: missing.txt");

      assertInstanceOf(WasiFileSystemException.class, fileNotFound);
      // The WasiFileSystemException doesn't expose a direct recovery suggestion method
      // but the error categorization should guide recovery
      assertTrue(((WasiFileSystemException) fileNotFound).isExistenceError());
    }
  }

  @Nested
  @DisplayName("Error Code Constants Tests")
  class ErrorCodeConstantsTests {

    @Test
    @DisplayName("Should have correct error code constants")
    void shouldHaveCorrectErrorCodeConstants() {
      assertEquals(0, ErrorMapper.SUCCESS);
      assertEquals(-1, ErrorMapper.COMPILATION_ERROR);
      assertEquals(-2, ErrorMapper.VALIDATION_ERROR);
      assertEquals(-3, ErrorMapper.RUNTIME_ERROR);
      assertEquals(-4, ErrorMapper.ENGINE_CONFIG_ERROR);
      assertEquals(-5, ErrorMapper.STORE_ERROR);
      assertEquals(-6, ErrorMapper.INSTANCE_ERROR);
      assertEquals(-7, ErrorMapper.MEMORY_ERROR);
      assertEquals(-8, ErrorMapper.FUNCTION_ERROR);
      assertEquals(-9, ErrorMapper.IMPORT_EXPORT_ERROR);
      assertEquals(-10, ErrorMapper.TYPE_ERROR);
      assertEquals(-11, ErrorMapper.RESOURCE_ERROR);
      assertEquals(-12, ErrorMapper.IO_ERROR);
      assertEquals(-13, ErrorMapper.INVALID_PARAMETER_ERROR);
      assertEquals(-14, ErrorMapper.CONCURRENCY_ERROR);
      assertEquals(-15, ErrorMapper.WASI_ERROR);
      assertEquals(-16, ErrorMapper.COMPONENT_ERROR);
      assertEquals(-17, ErrorMapper.INTERFACE_ERROR);
      assertEquals(-18, ErrorMapper.INTERNAL_ERROR);
    }

    @Test
    @DisplayName("Should map success code to exception (edge case)")
    void shouldMapSuccessCodeToException() {
      final WasmException exception =
          ErrorMapper.mapError(ErrorMapper.SUCCESS, "This shouldn't happen");

      assertInstanceOf(WasmException.class, exception);
      assertTrue(exception.getMessage().contains("Success code received as error"));
    }
  }
}
