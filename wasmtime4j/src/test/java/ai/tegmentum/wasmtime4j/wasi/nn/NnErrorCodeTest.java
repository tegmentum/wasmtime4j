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

package ai.tegmentum.wasmtime4j.wasi.nn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NnErrorCode} enum.
 *
 * <p>NnErrorCode represents error codes for WASI-NN operations per the WASI-NN specification.
 */
@DisplayName("NnErrorCode Tests")
class NnErrorCodeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(NnErrorCode.class.isEnum(), "NnErrorCode should be an enum");
    }

    @Test
    @DisplayName("should have exactly 9 values")
    void shouldHaveExactlyNineValues() {
      final NnErrorCode[] values = NnErrorCode.values();
      assertEquals(9, values.length, "Should have exactly 9 error codes");
    }

    @Test
    @DisplayName("should have INVALID_ARGUMENT value")
    void shouldHaveInvalidArgumentValue() {
      assertNotNull(NnErrorCode.valueOf("INVALID_ARGUMENT"), "Should have INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("should have INVALID_ENCODING value")
    void shouldHaveInvalidEncodingValue() {
      assertNotNull(NnErrorCode.valueOf("INVALID_ENCODING"), "Should have INVALID_ENCODING");
    }

    @Test
    @DisplayName("should have TIMEOUT value")
    void shouldHaveTimeoutValue() {
      assertNotNull(NnErrorCode.valueOf("TIMEOUT"), "Should have TIMEOUT");
    }

    @Test
    @DisplayName("should have RUNTIME_ERROR value")
    void shouldHaveRuntimeErrorValue() {
      assertNotNull(NnErrorCode.valueOf("RUNTIME_ERROR"), "Should have RUNTIME_ERROR");
    }

    @Test
    @DisplayName("should have UNSUPPORTED_OPERATION value")
    void shouldHaveUnsupportedOperationValue() {
      assertNotNull(
          NnErrorCode.valueOf("UNSUPPORTED_OPERATION"), "Should have UNSUPPORTED_OPERATION");
    }

    @Test
    @DisplayName("should have TOO_LARGE value")
    void shouldHaveTooLargeValue() {
      assertNotNull(NnErrorCode.valueOf("TOO_LARGE"), "Should have TOO_LARGE");
    }

    @Test
    @DisplayName("should have NOT_FOUND value")
    void shouldHaveNotFoundValue() {
      assertNotNull(NnErrorCode.valueOf("NOT_FOUND"), "Should have NOT_FOUND");
    }

    @Test
    @DisplayName("should have SECURITY value")
    void shouldHaveSecurityValue() {
      assertNotNull(NnErrorCode.valueOf("SECURITY"), "Should have SECURITY");
    }

    @Test
    @DisplayName("should have UNKNOWN value")
    void shouldHaveUnknownValue() {
      assertNotNull(NnErrorCode.valueOf("UNKNOWN"), "Should have UNKNOWN");
    }
  }

  @Nested
  @DisplayName("getWasiName Method Tests")
  class GetWasiNameTests {

    @Test
    @DisplayName("should return invalid-argument for INVALID_ARGUMENT")
    void shouldReturnInvalidArgumentForInvalidArgument() {
      assertEquals(
          "invalid-argument", NnErrorCode.INVALID_ARGUMENT.getWasiName(), "INVALID_ARGUMENT name");
    }

    @Test
    @DisplayName("should return invalid-encoding for INVALID_ENCODING")
    void shouldReturnInvalidEncodingForInvalidEncoding() {
      assertEquals(
          "invalid-encoding", NnErrorCode.INVALID_ENCODING.getWasiName(), "INVALID_ENCODING name");
    }

    @Test
    @DisplayName("should return timeout for TIMEOUT")
    void shouldReturnTimeoutForTimeout() {
      assertEquals("timeout", NnErrorCode.TIMEOUT.getWasiName(), "TIMEOUT name");
    }

    @Test
    @DisplayName("should return runtime-error for RUNTIME_ERROR")
    void shouldReturnRuntimeErrorForRuntimeError() {
      assertEquals("runtime-error", NnErrorCode.RUNTIME_ERROR.getWasiName(), "RUNTIME_ERROR name");
    }

    @Test
    @DisplayName("should return unsupported-operation for UNSUPPORTED_OPERATION")
    void shouldReturnUnsupportedOperationForUnsupportedOperation() {
      assertEquals(
          "unsupported-operation",
          NnErrorCode.UNSUPPORTED_OPERATION.getWasiName(),
          "UNSUPPORTED_OPERATION name");
    }

    @Test
    @DisplayName("should return too-large for TOO_LARGE")
    void shouldReturnTooLargeForTooLarge() {
      assertEquals("too-large", NnErrorCode.TOO_LARGE.getWasiName(), "TOO_LARGE name");
    }

    @Test
    @DisplayName("should return not-found for NOT_FOUND")
    void shouldReturnNotFoundForNotFound() {
      assertEquals("not-found", NnErrorCode.NOT_FOUND.getWasiName(), "NOT_FOUND name");
    }

    @Test
    @DisplayName("should return security for SECURITY")
    void shouldReturnSecurityForSecurity() {
      assertEquals("security", NnErrorCode.SECURITY.getWasiName(), "SECURITY name");
    }

    @Test
    @DisplayName("should return unknown for UNKNOWN")
    void shouldReturnUnknownForUnknown() {
      assertEquals("unknown", NnErrorCode.UNKNOWN.getWasiName(), "UNKNOWN name");
    }
  }

  @Nested
  @DisplayName("getDescription Method Tests")
  class GetDescriptionTests {

    @Test
    @DisplayName("should return meaningful description for INVALID_ARGUMENT")
    void shouldReturnMeaningfulDescriptionForInvalidArgument() {
      final String description = NnErrorCode.INVALID_ARGUMENT.getDescription();
      assertNotNull(description, "Description should not be null");
      assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("should return meaningful description for TIMEOUT")
    void shouldReturnMeaningfulDescriptionForTimeout() {
      final String description = NnErrorCode.TIMEOUT.getDescription();
      assertTrue(
          description.toLowerCase().contains("timeout")
              || description.toLowerCase().contains("time"),
          "TIMEOUT description should mention timeout");
    }

    @Test
    @DisplayName("should return meaningful description for RUNTIME_ERROR")
    void shouldReturnMeaningfulDescriptionForRuntimeError() {
      final String description = NnErrorCode.RUNTIME_ERROR.getDescription();
      assertTrue(
          description.toLowerCase().contains("runtime")
              || description.toLowerCase().contains("error"),
          "RUNTIME_ERROR description should mention runtime or error");
    }

    @Test
    @DisplayName("should return meaningful description for TOO_LARGE")
    void shouldReturnMeaningfulDescriptionForTooLarge() {
      final String description = NnErrorCode.TOO_LARGE.getDescription();
      assertTrue(
          description.toLowerCase().contains("large")
              || description.toLowerCase().contains("memory"),
          "TOO_LARGE description should mention large or memory");
    }
  }

  @Nested
  @DisplayName("fromWasiName Method Tests")
  class FromWasiNameTests {

    @Test
    @DisplayName("should parse invalid-argument")
    void shouldParseInvalidArgument() {
      assertEquals(
          NnErrorCode.INVALID_ARGUMENT,
          NnErrorCode.fromWasiName("invalid-argument"),
          "Should parse invalid-argument");
    }

    @Test
    @DisplayName("should parse invalid-encoding")
    void shouldParseInvalidEncoding() {
      assertEquals(
          NnErrorCode.INVALID_ENCODING,
          NnErrorCode.fromWasiName("invalid-encoding"),
          "Should parse invalid-encoding");
    }

    @Test
    @DisplayName("should parse timeout")
    void shouldParseTimeout() {
      assertEquals(
          NnErrorCode.TIMEOUT, NnErrorCode.fromWasiName("timeout"), "Should parse timeout");
    }

    @Test
    @DisplayName("should parse runtime-error")
    void shouldParseRuntimeError() {
      assertEquals(
          NnErrorCode.RUNTIME_ERROR,
          NnErrorCode.fromWasiName("runtime-error"),
          "Should parse runtime-error");
    }

    @Test
    @DisplayName("should parse unsupported-operation")
    void shouldParseUnsupportedOperation() {
      assertEquals(
          NnErrorCode.UNSUPPORTED_OPERATION,
          NnErrorCode.fromWasiName("unsupported-operation"),
          "Should parse unsupported-operation");
    }

    @Test
    @DisplayName("should parse too-large")
    void shouldParseTooLarge() {
      assertEquals(
          NnErrorCode.TOO_LARGE, NnErrorCode.fromWasiName("too-large"), "Should parse too-large");
    }

    @Test
    @DisplayName("should parse not-found")
    void shouldParseNotFound() {
      assertEquals(
          NnErrorCode.NOT_FOUND, NnErrorCode.fromWasiName("not-found"), "Should parse not-found");
    }

    @Test
    @DisplayName("should parse security")
    void shouldParseSecurity() {
      assertEquals(
          NnErrorCode.SECURITY, NnErrorCode.fromWasiName("security"), "Should parse security");
    }

    @Test
    @DisplayName("should parse unknown")
    void shouldParseUnknown() {
      assertEquals(
          NnErrorCode.UNKNOWN, NnErrorCode.fromWasiName("unknown"), "Should parse unknown");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for unknown name")
    void shouldThrowIllegalArgumentExceptionForUnknownName() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> NnErrorCode.fromWasiName("nonexistent"));

      assertTrue(ex.getMessage().contains("Unknown"), "Exception should mention Unknown");
    }

    @Test
    @DisplayName("should be case sensitive")
    void shouldBeCaseSensitive() {
      assertThrows(
          IllegalArgumentException.class,
          () -> NnErrorCode.fromWasiName("TIMEOUT"),
          "Should be case sensitive");
    }
  }

  @Nested
  @DisplayName("getNativeCode Method Tests")
  class GetNativeCodeTests {

    @Test
    @DisplayName("should return ordinal as native code")
    void shouldReturnOrdinalAsNativeCode() {
      for (final NnErrorCode code : NnErrorCode.values()) {
        assertEquals(
            code.ordinal(), code.getNativeCode(), code.name() + " native code should be ordinal");
      }
    }

    @Test
    @DisplayName("should have unique native codes")
    void shouldHaveUniqueNativeCodes() {
      final Set<Integer> codes = new HashSet<>();
      for (final NnErrorCode code : NnErrorCode.values()) {
        assertTrue(codes.add(code.getNativeCode()), "Native code should be unique: " + code);
      }
    }
  }

  @Nested
  @DisplayName("fromNativeCode Method Tests")
  class FromNativeCodeTests {

    @Test
    @DisplayName("should parse all valid codes")
    void shouldParseAllValidCodes() {
      for (final NnErrorCode expected : NnErrorCode.values()) {
        final NnErrorCode actual = NnErrorCode.fromNativeCode(expected.ordinal());
        assertEquals(expected, actual, "Should parse code " + expected.ordinal());
      }
    }

    @Test
    @DisplayName("should return UNKNOWN for negative code")
    void shouldReturnUnknownForNegativeCode() {
      // Per implementation, invalid codes return UNKNOWN instead of throwing
      assertEquals(NnErrorCode.UNKNOWN, NnErrorCode.fromNativeCode(-1), "Negative code -> UNKNOWN");
    }

    @Test
    @DisplayName("should return UNKNOWN for code out of range")
    void shouldReturnUnknownForCodeOutOfRange() {
      // Per implementation, invalid codes return UNKNOWN instead of throwing
      assertEquals(
          NnErrorCode.UNKNOWN, NnErrorCode.fromNativeCode(100), "Out of range code -> UNKNOWN");
    }

    @Test
    @DisplayName("should round trip from error code to native code and back")
    void shouldRoundTripFromErrorCodeToNativeCodeAndBack() {
      for (final NnErrorCode original : NnErrorCode.values()) {
        final int code = original.getNativeCode();
        final NnErrorCode roundTripped = NnErrorCode.fromNativeCode(code);
        assertEquals(original, roundTripped, "Should round trip: " + original);
      }
    }
  }

  @Nested
  @DisplayName("toString Method Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return wasi name and description")
    void shouldReturnWasiNameAndDescription() {
      for (final NnErrorCode code : NnErrorCode.values()) {
        final String str = code.toString();
        assertTrue(str.contains(code.getWasiName()), "toString should contain WASI name");
        assertTrue(str.contains(code.getDescription()), "toString should contain description");
      }
    }

    @Test
    @DisplayName("should format with colon separator")
    void shouldFormatWithColonSeparator() {
      final String str = NnErrorCode.TIMEOUT.toString();
      assertTrue(str.contains(":"), "toString should contain colon separator");
      assertEquals("timeout: Operation exceeded timeout limit", str, "TIMEOUT toString");
    }
  }

  @Nested
  @DisplayName("WASI-NN Specification Compliance Tests")
  class WasiNnSpecificationComplianceTests {

    @Test
    @DisplayName("should cover all WASI-NN error codes")
    void shouldCoverAllWasiNnErrorCodes() {
      // Per WASI-NN specification: error enum
      final String[] expectedCodes = {
        "invalid-argument",
        "invalid-encoding",
        "timeout",
        "runtime-error",
        "unsupported-operation",
        "too-large",
        "not-found",
        "security",
        "unknown"
      };

      for (final String expectedName : expectedCodes) {
        assertNotNull(NnErrorCode.fromWasiName(expectedName), "Should have code: " + expectedName);
      }

      assertEquals(
          expectedCodes.length, NnErrorCode.values().length, "Should have exact count of codes");
    }
  }

  @Nested
  @DisplayName("Error Category Tests")
  class ErrorCategoryTests {

    @Test
    @DisplayName("should have input validation errors")
    void shouldHaveInputValidationErrors() {
      final Set<NnErrorCode> inputErrors =
          Set.of(NnErrorCode.INVALID_ARGUMENT, NnErrorCode.INVALID_ENCODING);

      for (final NnErrorCode error : inputErrors) {
        assertNotNull(error, "Should have input error: " + error);
      }
    }

    @Test
    @DisplayName("should have runtime errors")
    void shouldHaveRuntimeErrors() {
      final Set<NnErrorCode> runtimeErrors = Set.of(NnErrorCode.RUNTIME_ERROR, NnErrorCode.TIMEOUT);

      for (final NnErrorCode error : runtimeErrors) {
        assertNotNull(error, "Should have runtime error: " + error);
      }
    }

    @Test
    @DisplayName("should have resource errors")
    void shouldHaveResourceErrors() {
      final Set<NnErrorCode> resourceErrors = Set.of(NnErrorCode.TOO_LARGE, NnErrorCode.NOT_FOUND);

      for (final NnErrorCode error : resourceErrors) {
        assertNotNull(error, "Should have resource error: " + error);
      }
    }

    @Test
    @DisplayName("should have capability errors")
    void shouldHaveCapabilityErrors() {
      final Set<NnErrorCode> capabilityErrors =
          Set.of(NnErrorCode.UNSUPPORTED_OPERATION, NnErrorCode.SECURITY);

      for (final NnErrorCode error : capabilityErrors) {
        assertNotNull(error, "Should have capability error: " + error);
      }
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support error handling pattern")
    void shouldSupportErrorHandlingPattern() {
      final NnErrorCode error = NnErrorCode.TIMEOUT;

      final boolean isRetryable =
          error == NnErrorCode.TIMEOUT || error == NnErrorCode.RUNTIME_ERROR;

      assertTrue(isRetryable, "TIMEOUT should be retryable");
    }

    @Test
    @DisplayName("should support switch statement")
    void shouldSupportSwitchStatement() {
      final NnErrorCode error = NnErrorCode.NOT_FOUND;

      final String userMessage;
      switch (error) {
        case INVALID_ARGUMENT:
          userMessage = "Invalid input provided";
          break;
        case NOT_FOUND:
          userMessage = "Model not found";
          break;
        case TOO_LARGE:
          userMessage = "Model too large for memory";
          break;
        case TIMEOUT:
          userMessage = "Operation timed out, please retry";
          break;
        default:
          userMessage = "An error occurred: " + error.getDescription();
      }

      assertEquals("Model not found", userMessage, "NOT_FOUND message");
    }

    @Test
    @DisplayName("should support error categorization")
    void shouldSupportErrorCategorization() {
      final boolean isUserError =
          NnErrorCode.INVALID_ARGUMENT.ordinal() < NnErrorCode.TIMEOUT.ordinal();

      assertTrue(isUserError, "INVALID_ARGUMENT should be before TIMEOUT in ordinal order");
    }
  }
}
