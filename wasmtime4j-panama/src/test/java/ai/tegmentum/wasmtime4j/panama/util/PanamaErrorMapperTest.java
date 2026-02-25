package ai.tegmentum.wasmtime4j.panama.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.InstantiationException;
import ai.tegmentum.wasmtime4j.exception.LinkingException;
import ai.tegmentum.wasmtime4j.exception.ResourceException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasiException;
import ai.tegmentum.wasmtime4j.exception.WasmErrorCode;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import ai.tegmentum.wasmtime4j.exception.WasmSecurityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for {@link PanamaErrorMapper}. */
@DisplayName("PanamaErrorMapper Tests")
class PanamaErrorMapperTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaErrorMapper should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaErrorMapper.class.getModifiers()),
          "PanamaErrorMapper should be final");
    }

    @Test
    @DisplayName("PanamaErrorMapper should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final java.lang.reflect.Constructor<?> constructor =
          PanamaErrorMapper.class.getDeclaredConstructor();
      assertTrue(
          java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private");
    }
  }

  @Nested
  @DisplayName("Exception Type Mapping Tests")
  class ExceptionTypeMappingTests {

    @Test
    @DisplayName("Compilation error (-1) should map to CompilationException")
    void shouldMapCompilationError() {
      final WasmException ex = PanamaErrorMapper.mapNativeError(-1, "compile failed");
      assertNotNull(ex, "Exception should not be null");
      assertTrue(
          ex instanceof CompilationException,
          "Error code -1 should produce CompilationException, got: " + ex.getClass().getName());
    }

    @Test
    @DisplayName("Validation error (-2) should map to ValidationException")
    void shouldMapValidationError() {
      final WasmException ex = PanamaErrorMapper.mapNativeError(-2, "invalid module");
      assertTrue(
          ex instanceof ValidationException,
          "Error code -2 should produce ValidationException, got: " + ex.getClass().getName());
    }

    @Test
    @DisplayName("Instance error (-6) should map to InstantiationException")
    void shouldMapInstanceError() {
      final WasmException ex = PanamaErrorMapper.mapNativeError(-6, "instance failure");
      assertTrue(
          ex instanceof InstantiationException,
          "Error code -6 should produce InstantiationException, got: " + ex.getClass().getName());
    }

    @Test
    @DisplayName("Import/Export error (-9) should map to LinkingException")
    void shouldMapImportExportError() {
      final WasmException ex = PanamaErrorMapper.mapNativeError(-9, "import missing");
      assertTrue(
          ex instanceof LinkingException,
          "Error code -9 should produce LinkingException, got: " + ex.getClass().getName());
    }

    @Test
    @DisplayName("Resource error (-11) should map to ResourceException")
    void shouldMapResourceError() {
      final WasmException ex = PanamaErrorMapper.mapNativeError(-11, "resource exhausted");
      assertTrue(
          ex instanceof ResourceException,
          "Error code -11 should produce ResourceException, got: " + ex.getClass().getName());
    }

    @Test
    @DisplayName("WASI error (-15) should map to WasiException")
    void shouldMapWasiError() {
      final WasmException ex = PanamaErrorMapper.mapNativeError(-15, "wasi failure");
      assertTrue(
          ex instanceof WasiException,
          "Error code -15 should produce WasiException, got: " + ex.getClass().getName());
    }

    @Test
    @DisplayName("Security error (-16) should map to WasmSecurityException")
    void shouldMapSecurityError() {
      final WasmException ex = PanamaErrorMapper.mapNativeError(-16, "access denied");
      assertTrue(
          ex instanceof WasmSecurityException,
          "Error code -16 should produce WasmSecurityException, got: " + ex.getClass().getName());
    }

    @Test
    @DisplayName("Security violation (-22) should map to WasmSecurityException")
    void shouldMapSecurityViolation() {
      final WasmException ex = PanamaErrorMapper.mapNativeError(-22, "violation");
      assertTrue(
          ex instanceof WasmSecurityException,
          "Error code -22 should produce WasmSecurityException, got: " + ex.getClass().getName());
    }

    @ParameterizedTest(name = "Error code {0} should map to WasmRuntimeException")
    @ValueSource(
        ints = {
          -3, -4, -5, -7, -8, -10, -12, -13, -14, -17, -18, -19, -20, -21, -23, -24, -25, -26
        })
    @DisplayName("Runtime-category errors should map to WasmRuntimeException")
    void shouldMapRuntimeCategoryErrors(final int errorCode) {
      final WasmException ex = PanamaErrorMapper.mapNativeError(errorCode, "runtime issue");
      assertTrue(
          ex instanceof WasmRuntimeException,
          "Error code "
              + errorCode
              + " should produce WasmRuntimeException, got: "
              + ex.getClass().getName());
    }
  }

  @Nested
  @DisplayName("Context Message Tests")
  class ContextMessageTests {

    @Test
    @DisplayName("Should include context in exception message")
    void shouldIncludeContextInMessage() {
      final WasmException ex = PanamaErrorMapper.mapNativeError(-1, "Failed to compile WAT");
      assertTrue(
          ex.getMessage().contains("Failed to compile WAT"),
          "Should include context. Got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should work with null context")
    void shouldWorkWithNullContext() {
      final WasmException ex = PanamaErrorMapper.mapNativeError(-1);
      assertNotNull(ex, "Exception should not be null");
      assertTrue(
          ex instanceof CompilationException,
          "Should still produce correct type with null context");
    }

    @Test
    @DisplayName("Should handle unknown error code gracefully")
    void shouldHandleUnknownErrorCode() {
      final WasmException ex = PanamaErrorMapper.mapNativeError(-999, "something broke");
      assertNotNull(ex, "Exception should not be null");
      assertTrue(
          ex.getMessage().contains("-999"),
          "Should include the error code in message. Got: " + ex.getMessage());
      assertTrue(
          ex.getMessage().contains("Unknown native error"),
          "Should indicate unknown. Got: " + ex.getMessage());
    }
  }

  @Nested
  @DisplayName("All Error Codes Coverage Tests")
  class AllErrorCodesCoverageTests {

    @ParameterizedTest(name = "Error code {0} should produce a non-null WasmException")
    @ValueSource(
        ints = {
          0, -1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13, -14, -15, -16, -17, -18, -19,
          -20, -21, -22, -23, -24, -25, -26
        })
    @DisplayName("All 27 error codes should produce non-null exceptions")
    void allCodesShouldMapToExceptions(final int errorCode) {
      final WasmException ex = PanamaErrorMapper.mapNativeError(errorCode, "test");
      assertNotNull(ex, "Error code " + errorCode + " should produce a non-null exception");
    }

    @Test
    @DisplayName("All WasmErrorCode values should be handled")
    void allWasmErrorCodesShouldBeHandled() {
      for (final WasmErrorCode errorCode : WasmErrorCode.values()) {
        final WasmException ex =
            PanamaErrorMapper.mapNativeError(errorCode.getCode(), "coverage test");
        assertNotNull(
            ex,
            "WasmErrorCode."
                + errorCode.name()
                + " (code "
                + errorCode.getCode()
                + ") should produce a non-null exception");
      }
    }
  }

  @Nested
  @DisplayName("getErrorDescription Tests")
  class GetErrorDescriptionTests {

    @Test
    @DisplayName("Should return description for known code")
    void shouldReturnDescriptionForKnownCode() {
      final String desc = PanamaErrorMapper.getErrorDescription(-1);
      assertEquals(
          WasmErrorCode.COMPILATION_ERROR.getDescription(),
          desc,
          "Should return the WasmErrorCode description");
    }

    @Test
    @DisplayName("Should return default message for unknown code")
    void shouldReturnDefaultForUnknownCode() {
      final String desc = PanamaErrorMapper.getErrorDescription(-999);
      assertTrue(
          desc.contains("-999"), "Should include error code in default message. Got: " + desc);
    }
  }
}
