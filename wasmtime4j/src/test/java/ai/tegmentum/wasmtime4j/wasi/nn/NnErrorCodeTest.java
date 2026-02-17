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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link NnErrorCode} enum.
 *
 * <p>Verifies WASI-NN error code values, name mappings, native codes, and string representations.
 */
@DisplayName("NnErrorCode Tests")
class NnErrorCodeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("NnErrorCode should be an enum")
    void shouldBeAnEnum() {
      assertTrue(NnErrorCode.class.isEnum(), "NnErrorCode should be an enum");
    }

    @Test
    @DisplayName("NnErrorCode should have exactly 9 values")
    void shouldHaveExactlyNineValues() {
      assertEquals(9, NnErrorCode.values().length, "Should have exactly 9 error code values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have INVALID_ARGUMENT value")
    void shouldHaveInvalidArgumentValue() {
      assertNotNull(NnErrorCode.INVALID_ARGUMENT, "INVALID_ARGUMENT should exist");
      assertEquals(
          "INVALID_ARGUMENT",
          NnErrorCode.INVALID_ARGUMENT.name(),
          "Name should be INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("should have INVALID_ENCODING value")
    void shouldHaveInvalidEncodingValue() {
      assertNotNull(NnErrorCode.INVALID_ENCODING, "INVALID_ENCODING should exist");
      assertEquals(
          "INVALID_ENCODING",
          NnErrorCode.INVALID_ENCODING.name(),
          "Name should be INVALID_ENCODING");
    }

    @Test
    @DisplayName("should have TIMEOUT value")
    void shouldHaveTimeoutValue() {
      assertNotNull(NnErrorCode.TIMEOUT, "TIMEOUT should exist");
      assertEquals("TIMEOUT", NnErrorCode.TIMEOUT.name(), "Name should be TIMEOUT");
    }

    @Test
    @DisplayName("should have RUNTIME_ERROR value")
    void shouldHaveRuntimeErrorValue() {
      assertNotNull(NnErrorCode.RUNTIME_ERROR, "RUNTIME_ERROR should exist");
      assertEquals(
          "RUNTIME_ERROR", NnErrorCode.RUNTIME_ERROR.name(), "Name should be RUNTIME_ERROR");
    }

    @Test
    @DisplayName("should have UNSUPPORTED_OPERATION value")
    void shouldHaveUnsupportedOperationValue() {
      assertNotNull(NnErrorCode.UNSUPPORTED_OPERATION, "UNSUPPORTED_OPERATION should exist");
      assertEquals(
          "UNSUPPORTED_OPERATION",
          NnErrorCode.UNSUPPORTED_OPERATION.name(),
          "Name should be UNSUPPORTED_OPERATION");
    }

    @Test
    @DisplayName("should have TOO_LARGE value")
    void shouldHaveTooLargeValue() {
      assertNotNull(NnErrorCode.TOO_LARGE, "TOO_LARGE should exist");
      assertEquals("TOO_LARGE", NnErrorCode.TOO_LARGE.name(), "Name should be TOO_LARGE");
    }

    @Test
    @DisplayName("should have NOT_FOUND value")
    void shouldHaveNotFoundValue() {
      assertNotNull(NnErrorCode.NOT_FOUND, "NOT_FOUND should exist");
      assertEquals("NOT_FOUND", NnErrorCode.NOT_FOUND.name(), "Name should be NOT_FOUND");
    }

    @Test
    @DisplayName("should have SECURITY value")
    void shouldHaveSecurityValue() {
      assertNotNull(NnErrorCode.SECURITY, "SECURITY should exist");
      assertEquals("SECURITY", NnErrorCode.SECURITY.name(), "Name should be SECURITY");
    }

    @Test
    @DisplayName("should have UNKNOWN value")
    void shouldHaveUnknownValue() {
      assertNotNull(NnErrorCode.UNKNOWN, "UNKNOWN should exist");
      assertEquals("UNKNOWN", NnErrorCode.UNKNOWN.name(), "Name should be UNKNOWN");
    }
  }

  @Nested
  @DisplayName("valueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return correct constant for each name")
    void valueOfShouldReturnCorrectConstant() {
      assertEquals(
          NnErrorCode.INVALID_ARGUMENT,
          NnErrorCode.valueOf("INVALID_ARGUMENT"),
          "Should return INVALID_ARGUMENT");
      assertEquals(
          NnErrorCode.INVALID_ENCODING,
          NnErrorCode.valueOf("INVALID_ENCODING"),
          "Should return INVALID_ENCODING");
      assertEquals(NnErrorCode.TIMEOUT, NnErrorCode.valueOf("TIMEOUT"), "Should return TIMEOUT");
      assertEquals(
          NnErrorCode.RUNTIME_ERROR,
          NnErrorCode.valueOf("RUNTIME_ERROR"),
          "Should return RUNTIME_ERROR");
      assertEquals(
          NnErrorCode.UNSUPPORTED_OPERATION,
          NnErrorCode.valueOf("UNSUPPORTED_OPERATION"),
          "Should return UNSUPPORTED_OPERATION");
      assertEquals(
          NnErrorCode.TOO_LARGE, NnErrorCode.valueOf("TOO_LARGE"), "Should return TOO_LARGE");
      assertEquals(
          NnErrorCode.NOT_FOUND, NnErrorCode.valueOf("NOT_FOUND"), "Should return NOT_FOUND");
      assertEquals(NnErrorCode.SECURITY, NnErrorCode.valueOf("SECURITY"), "Should return SECURITY");
      assertEquals(NnErrorCode.UNKNOWN, NnErrorCode.valueOf("UNKNOWN"), "Should return UNKNOWN");
    }

    @Test
    @DisplayName("valueOf should throw for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> NnErrorCode.valueOf("INVALID"),
          "Should throw for invalid enum name");
    }
  }

  @Nested
  @DisplayName("values() Tests")
  class ValuesTests {

    @Test
    @DisplayName("values() should return all enum constants")
    void valuesShouldReturnAllEnumConstants() {
      final NnErrorCode[] values = NnErrorCode.values();
      final Set<NnErrorCode> valueSet = new HashSet<>(Arrays.asList(values));

      assertTrue(
          valueSet.contains(NnErrorCode.INVALID_ARGUMENT), "Should contain INVALID_ARGUMENT");
      assertTrue(
          valueSet.contains(NnErrorCode.INVALID_ENCODING), "Should contain INVALID_ENCODING");
      assertTrue(valueSet.contains(NnErrorCode.TIMEOUT), "Should contain TIMEOUT");
      assertTrue(valueSet.contains(NnErrorCode.RUNTIME_ERROR), "Should contain RUNTIME_ERROR");
      assertTrue(
          valueSet.contains(NnErrorCode.UNSUPPORTED_OPERATION),
          "Should contain UNSUPPORTED_OPERATION");
      assertTrue(valueSet.contains(NnErrorCode.TOO_LARGE), "Should contain TOO_LARGE");
      assertTrue(valueSet.contains(NnErrorCode.NOT_FOUND), "Should contain NOT_FOUND");
      assertTrue(valueSet.contains(NnErrorCode.SECURITY), "Should contain SECURITY");
      assertTrue(valueSet.contains(NnErrorCode.UNKNOWN), "Should contain UNKNOWN");
    }

    @Test
    @DisplayName("values() should return new array each time")
    void valuesShouldReturnNewArrayEachTime() {
      final NnErrorCode[] first = NnErrorCode.values();
      final NnErrorCode[] second = NnErrorCode.values();

      assertTrue(first != second, "Should return new array each time");
      assertEquals(first.length, second.length, "Arrays should have same length");
    }
  }

  @Nested
  @DisplayName("GetWasiName Tests")
  class GetWasiNameTests {

    @Test
    @DisplayName("INVALID_ARGUMENT should have wasi name 'invalid-argument'")
    void invalidArgumentShouldHaveCorrectWasiName() {
      assertEquals(
          "invalid-argument",
          NnErrorCode.INVALID_ARGUMENT.getWasiName(),
          "INVALID_ARGUMENT wasi name should be 'invalid-argument'");
    }

    @Test
    @DisplayName("INVALID_ENCODING should have wasi name 'invalid-encoding'")
    void invalidEncodingShouldHaveCorrectWasiName() {
      assertEquals(
          "invalid-encoding",
          NnErrorCode.INVALID_ENCODING.getWasiName(),
          "INVALID_ENCODING wasi name should be 'invalid-encoding'");
    }

    @Test
    @DisplayName("TIMEOUT should have wasi name 'timeout'")
    void timeoutShouldHaveCorrectWasiName() {
      assertEquals(
          "timeout", NnErrorCode.TIMEOUT.getWasiName(), "TIMEOUT wasi name should be 'timeout'");
    }

    @Test
    @DisplayName("RUNTIME_ERROR should have wasi name 'runtime-error'")
    void runtimeErrorShouldHaveCorrectWasiName() {
      assertEquals(
          "runtime-error",
          NnErrorCode.RUNTIME_ERROR.getWasiName(),
          "RUNTIME_ERROR wasi name should be 'runtime-error'");
    }

    @Test
    @DisplayName("UNSUPPORTED_OPERATION should have wasi name 'unsupported-operation'")
    void unsupportedOperationShouldHaveCorrectWasiName() {
      assertEquals(
          "unsupported-operation",
          NnErrorCode.UNSUPPORTED_OPERATION.getWasiName(),
          "UNSUPPORTED_OPERATION wasi name should be 'unsupported-operation'");
    }

    @Test
    @DisplayName("TOO_LARGE should have wasi name 'too-large'")
    void tooLargeShouldHaveCorrectWasiName() {
      assertEquals(
          "too-large",
          NnErrorCode.TOO_LARGE.getWasiName(),
          "TOO_LARGE wasi name should be 'too-large'");
    }

    @Test
    @DisplayName("NOT_FOUND should have wasi name 'not-found'")
    void notFoundShouldHaveCorrectWasiName() {
      assertEquals(
          "not-found",
          NnErrorCode.NOT_FOUND.getWasiName(),
          "NOT_FOUND wasi name should be 'not-found'");
    }

    @Test
    @DisplayName("SECURITY should have wasi name 'security'")
    void securityShouldHaveCorrectWasiName() {
      assertEquals(
          "security",
          NnErrorCode.SECURITY.getWasiName(),
          "SECURITY wasi name should be 'security'");
    }

    @Test
    @DisplayName("UNKNOWN should have wasi name 'unknown'")
    void unknownShouldHaveCorrectWasiName() {
      assertEquals(
          "unknown", NnErrorCode.UNKNOWN.getWasiName(), "UNKNOWN wasi name should be 'unknown'");
    }
  }

  @Nested
  @DisplayName("GetDescription Tests")
  class GetDescriptionTests {

    @Test
    @DisplayName("each constant should have a non-empty description")
    void eachConstantShouldHaveNonEmptyDescription() {
      for (final NnErrorCode code : NnErrorCode.values()) {
        assertNotNull(code.getDescription(), "Description should not be null for " + code);
        assertFalse(code.getDescription().isEmpty(), "Description should not be empty for " + code);
      }
    }
  }

  @Nested
  @DisplayName("FromWasiName Tests")
  class FromWasiNameTests {

    @Test
    @DisplayName("should resolve valid wasi names to correct constants")
    void shouldResolveValidWasiNames() {
      assertEquals(
          NnErrorCode.INVALID_ARGUMENT,
          NnErrorCode.fromWasiName("invalid-argument"),
          "Should resolve 'invalid-argument'");
      assertEquals(
          NnErrorCode.INVALID_ENCODING,
          NnErrorCode.fromWasiName("invalid-encoding"),
          "Should resolve 'invalid-encoding'");
      assertEquals(
          NnErrorCode.TIMEOUT, NnErrorCode.fromWasiName("timeout"), "Should resolve 'timeout'");
      assertEquals(
          NnErrorCode.RUNTIME_ERROR,
          NnErrorCode.fromWasiName("runtime-error"),
          "Should resolve 'runtime-error'");
      assertEquals(
          NnErrorCode.UNSUPPORTED_OPERATION,
          NnErrorCode.fromWasiName("unsupported-operation"),
          "Should resolve 'unsupported-operation'");
      assertEquals(
          NnErrorCode.TOO_LARGE,
          NnErrorCode.fromWasiName("too-large"),
          "Should resolve 'too-large'");
      assertEquals(
          NnErrorCode.NOT_FOUND,
          NnErrorCode.fromWasiName("not-found"),
          "Should resolve 'not-found'");
      assertEquals(
          NnErrorCode.SECURITY, NnErrorCode.fromWasiName("security"), "Should resolve 'security'");
      assertEquals(
          NnErrorCode.UNKNOWN, NnErrorCode.fromWasiName("unknown"), "Should resolve 'unknown'");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid wasi name")
    void shouldThrowForInvalidWasiName() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> NnErrorCode.fromWasiName("not-a-real-error"),
              "Should throw for invalid wasi name");
      assertTrue(
          exception.getMessage().contains("not-a-real-error"),
          "Exception message should mention the invalid name: " + exception.getMessage());
    }
  }

  @Nested
  @DisplayName("GetNativeCode Tests")
  class GetNativeCodeTests {

    @Test
    @DisplayName("getNativeCode should return ordinal for each constant")
    void getNativeCodeShouldReturnOrdinal() {
      for (final NnErrorCode code : NnErrorCode.values()) {
        assertEquals(
            code.ordinal(),
            code.getNativeCode(),
            "getNativeCode() should return ordinal() for " + code);
      }
    }
  }

  @Nested
  @DisplayName("FromNativeCode Tests")
  class FromNativeCodeTests {

    @Test
    @DisplayName("should resolve valid native codes to correct constants")
    void shouldResolveValidNativeCodes() {
      for (final NnErrorCode code : NnErrorCode.values()) {
        assertSame(
            code,
            NnErrorCode.fromNativeCode(code.getNativeCode()),
            "Should resolve native code " + code.getNativeCode() + " to " + code);
      }
    }

    @Test
    @DisplayName("should return UNKNOWN for invalid native code")
    void shouldReturnUnknownForInvalidNativeCode() {
      assertEquals(
          NnErrorCode.UNKNOWN,
          NnErrorCode.fromNativeCode(999),
          "Should return UNKNOWN for out-of-range native code 999");
      assertEquals(
          NnErrorCode.UNKNOWN,
          NnErrorCode.fromNativeCode(-1),
          "Should return UNKNOWN for negative native code -1");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return 'wasiName: description' format")
    void toStringShouldReturnCorrectFormat() {
      for (final NnErrorCode code : NnErrorCode.values()) {
        final String expected = code.getWasiName() + ": " + code.getDescription();
        assertEquals(
            expected,
            code.toString(),
            "toString() for " + code + " should be 'wasiName: description'");
      }
    }
  }
}
