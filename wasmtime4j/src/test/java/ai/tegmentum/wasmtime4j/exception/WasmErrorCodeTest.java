package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for {@link WasmErrorCode}. */
@DisplayName("WasmErrorCode Tests")
class WasmErrorCodeTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("Should have exactly 28 variants (SUCCESS + 27 error codes)")
    void shouldHaveCorrectNumberOfVariants() {
      assertEquals(
          28,
          WasmErrorCode.values().length,
          "Should have 28 variants matching Rust ErrorCode enum");
    }

    @ParameterizedTest(name = "WasmErrorCode.{0} should have code {1}")
    @CsvSource({
      "SUCCESS, 0",
      "COMPILATION_ERROR, -1",
      "VALIDATION_ERROR, -2",
      "RUNTIME_ERROR, -3",
      "ENGINE_CONFIG_ERROR, -4",
      "STORE_ERROR, -5",
      "INSTANCE_ERROR, -6",
      "MEMORY_ERROR, -7",
      "FUNCTION_ERROR, -8",
      "IMPORT_EXPORT_ERROR, -9",
      "TYPE_ERROR, -10",
      "RESOURCE_ERROR, -11",
      "IO_ERROR, -12",
      "INVALID_PARAMETER_ERROR, -13",
      "CONCURRENCY_ERROR, -14",
      "WASI_ERROR, -15",
      "SECURITY_ERROR, -16",
      "COMPONENT_ERROR, -17",
      "INTERFACE_ERROR, -18",
      "NETWORK_ERROR, -19",
      "PROCESS_ERROR, -20",
      "INTERNAL_ERROR, -21",
      "SECURITY_VIOLATION, -22",
      "INVALID_DATA, -23",
      "IO_OPERATION_ERROR, -24",
      "UNSUPPORTED_OPERATION, -25",
      "WOULD_BLOCK, -26",
      "WASI_EXIT, -27"
    })
    @DisplayName("Should have correct integer code")
    void shouldHaveCorrectCode(final String name, final int expectedCode) {
      final WasmErrorCode errorCode = WasmErrorCode.valueOf(name);
      assertEquals(
          expectedCode,
          errorCode.getCode(),
          "WasmErrorCode." + name + " should have code " + expectedCode);
    }

    @Test
    @DisplayName("All variants should have non-null descriptions")
    void allVariantsShouldHaveDescriptions() {
      for (final WasmErrorCode errorCode : WasmErrorCode.values()) {
        assertNotNull(
            errorCode.getDescription(),
            "WasmErrorCode." + errorCode.name() + " should have a description");
      }
    }

    @Test
    @DisplayName("All variants should have non-empty descriptions")
    void allVariantsShouldHaveNonEmptyDescriptions() {
      for (final WasmErrorCode errorCode : WasmErrorCode.values()) {
        assertNotNull(errorCode.getDescription(), errorCode.name() + " description is null");
        assertEquals(
            false,
            errorCode.getDescription().isEmpty(),
            "WasmErrorCode." + errorCode.name() + " should have a non-empty description");
      }
    }

    @Test
    @DisplayName("All integer codes should be unique")
    void allCodesShouldBeUnique() {
      final WasmErrorCode[] values = WasmErrorCode.values();
      for (int i = 0; i < values.length; i++) {
        for (int j = i + 1; j < values.length; j++) {
          assertEquals(
              false,
              values[i].getCode() == values[j].getCode(),
              "WasmErrorCode."
                  + values[i].name()
                  + " and WasmErrorCode."
                  + values[j].name()
                  + " have the same code: "
                  + values[i].getCode());
        }
      }
    }
  }

  @Nested
  @DisplayName("fromCode Tests")
  class FromCodeTests {

    @ParameterizedTest(name = "fromCode({0}) should return non-null")
    @ValueSource(
        ints = {
          0, -1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13, -14, -15, -16, -17, -18, -19,
          -20, -21, -22, -23, -24, -25, -26, -27
        })
    @DisplayName("Should find all valid codes")
    void shouldFindAllValidCodes(final int code) {
      final WasmErrorCode result = WasmErrorCode.fromCode(code);
      assertNotNull(result, "fromCode(" + code + ") should return a WasmErrorCode");
      assertEquals(code, result.getCode(), "Round-trip code should match");
    }

    @ParameterizedTest(name = "fromCode({0}) should return null for unknown code")
    @ValueSource(ints = {1, 2, -28, -100, -999, Integer.MIN_VALUE, Integer.MAX_VALUE})
    @DisplayName("Should return null for unknown codes")
    void shouldReturnNullForUnknownCodes(final int code) {
      assertNull(
          WasmErrorCode.fromCode(code),
          "fromCode(" + code + ") should return null for unknown code");
    }

    @Test
    @DisplayName("fromCode should correctly map SecurityError to -16")
    void shouldMapSecurityErrorCorrectly() {
      final WasmErrorCode result = WasmErrorCode.fromCode(-16);
      assertEquals(
          WasmErrorCode.SECURITY_ERROR,
          result,
          "Code -16 should map to SECURITY_ERROR, not COMPONENT_ERROR");
    }

    @Test
    @DisplayName("fromCode should correctly map ComponentError to -17")
    void shouldMapComponentErrorCorrectly() {
      final WasmErrorCode result = WasmErrorCode.fromCode(-17);
      assertEquals(
          WasmErrorCode.COMPONENT_ERROR,
          result,
          "Code -17 should map to COMPONENT_ERROR, not INTERFACE_ERROR");
    }

    @Test
    @DisplayName("fromCode should correctly map InterfaceError to -18")
    void shouldMapInterfaceErrorCorrectly() {
      final WasmErrorCode result = WasmErrorCode.fromCode(-18);
      assertEquals(
          WasmErrorCode.INTERFACE_ERROR,
          result,
          "Code -18 should map to INTERFACE_ERROR, not INTERNAL_ERROR");
    }

    @Test
    @DisplayName("fromCode should correctly map InternalError to -21")
    void shouldMapInternalErrorCorrectly() {
      final WasmErrorCode result = WasmErrorCode.fromCode(-21);
      assertEquals(WasmErrorCode.INTERNAL_ERROR, result, "Code -21 should map to INTERNAL_ERROR");
    }
  }

  @Nested
  @DisplayName("Rust Alignment Tests")
  class RustAlignmentTests {

    @Test
    @DisplayName("Error codes should form contiguous range from 0 to -27")
    void errorCodesShouldBeContiguous() {
      for (int code = 0; code >= -27; code--) {
        final WasmErrorCode result = WasmErrorCode.fromCode(code);
        assertNotNull(
            result,
            "Error code " + code + " should have a WasmErrorCode mapping (contiguous range)");
      }
    }

    @Test
    @DisplayName("SUCCESS should be the only non-negative code")
    void successShouldBeOnlyNonNegativeCode() {
      for (final WasmErrorCode errorCode : WasmErrorCode.values()) {
        if (errorCode != WasmErrorCode.SUCCESS) {
          assertEquals(
              true,
              errorCode.getCode() < 0,
              "WasmErrorCode."
                  + errorCode.name()
                  + " should have a negative code, but has "
                  + errorCode.getCode());
        }
      }
      assertEquals(0, WasmErrorCode.SUCCESS.getCode(), "SUCCESS should have code 0");
    }
  }
}
