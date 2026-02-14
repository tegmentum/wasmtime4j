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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.clocks.DateTime;
import ai.tegmentum.wasmtime4j.wasi.clocks.TimezoneDisplay;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.KeyValueErrorCode;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.KeyValueException;
import ai.tegmentum.wasmtime4j.wasi.nn.NnErrorCode;
import ai.tegmentum.wasmtime4j.wasi.nn.NnException;
import ai.tegmentum.wasmtime4j.wasi.nn.NnExecutionTarget;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphEncoding;
import ai.tegmentum.wasmtime4j.wasi.nn.NnTensorType;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive functional tests for WASI subsystem classes.
 *
 * <p>Tests cover: DateTime, TimezoneDisplay, KeyValue enums/exceptions, NN enums/exceptions. These
 * tests verify actual behavior, validation, and functional correctness beyond simple API structure
 * tests.
 *
 * @since 1.0.0
 */
@DisplayName("WASI Subsystem Integration Tests")
public final class WasiSubsystemIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiSubsystemIntegrationTest.class.getName());

  // ========================================================================
  // DateTime Functional Tests
  // ========================================================================

  @Nested
  @DisplayName("DateTime Functional Tests")
  class DateTimeFunctionalTests {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

      @Test
      @DisplayName("should create DateTime with valid seconds and nanoseconds")
      void shouldCreateDateTimeWithValidValues(final TestInfo testInfo) {
        LOGGER.info("Testing: " + testInfo.getDisplayName());

        final DateTime dateTime = new DateTime(1700000000L, 500000000);

        assertEquals(1700000000L, dateTime.getSeconds(), "Seconds should match");
        assertEquals(500000000, dateTime.getNanoseconds(), "Nanoseconds should match");

        LOGGER.info("Created DateTime: " + dateTime);
      }

      @Test
      @DisplayName("should create DateTime with zero values")
      void shouldCreateDateTimeWithZeroValues(final TestInfo testInfo) {
        LOGGER.info("Testing: " + testInfo.getDisplayName());

        final DateTime dateTime = new DateTime(0L, 0);

        assertEquals(0L, dateTime.getSeconds(), "Seconds should be 0");
        assertEquals(0, dateTime.getNanoseconds(), "Nanoseconds should be 0");

        LOGGER.info("Created epoch DateTime: " + dateTime);
      }

      @Test
      @DisplayName("should create DateTime with negative seconds (before epoch)")
      void shouldCreateDateTimeWithNegativeSeconds(final TestInfo testInfo) {
        LOGGER.info("Testing: " + testInfo.getDisplayName());

        final DateTime dateTime = new DateTime(-1000L, 123456789);

        assertEquals(-1000L, dateTime.getSeconds(), "Negative seconds should be allowed");
        assertEquals(123456789, dateTime.getNanoseconds(), "Nanoseconds should match");

        LOGGER.info("Created pre-epoch DateTime: " + dateTime);
      }

      @Test
      @DisplayName("should create DateTime with max nanoseconds (999999999)")
      void shouldCreateDateTimeWithMaxNanoseconds(final TestInfo testInfo) {
        LOGGER.info("Testing: " + testInfo.getDisplayName());

        final DateTime dateTime = new DateTime(1000L, 999999999);

        assertEquals(999999999, dateTime.getNanoseconds(), "Max nanoseconds should be allowed");

        LOGGER.info("Created DateTime with max nanoseconds: " + dateTime);
      }

      @Test
      @DisplayName("should reject nanoseconds at 1 billion")
      void shouldRejectNanosecondsAtOneBillion(final TestInfo testInfo) {
        LOGGER.info("Testing: " + testInfo.getDisplayName());

        final IllegalArgumentException exception =
            assertThrows(
                IllegalArgumentException.class,
                () -> new DateTime(1000L, 1000000000),
                "Should reject nanoseconds >= 1 billion");

        assertTrue(
            exception.getMessage().contains("1000000000"),
            "Exception message should contain the invalid value");

        LOGGER.info("Rejected invalid nanoseconds: " + exception.getMessage());
      }

      @Test
      @DisplayName("should reject negative nanoseconds")
      void shouldRejectNegativeNanoseconds(final TestInfo testInfo) {
        LOGGER.info("Testing: " + testInfo.getDisplayName());

        final IllegalArgumentException exception =
            assertThrows(
                IllegalArgumentException.class,
                () -> new DateTime(1000L, -1),
                "Should reject negative nanoseconds");

        assertTrue(
            exception.getMessage().contains("-1"),
            "Exception message should contain the invalid value");

        LOGGER.info("Rejected negative nanoseconds: " + exception.getMessage());
      }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

      @Test
      @DisplayName("should be equal for same values")
      void shouldBeEqualForSameValues(final TestInfo testInfo) {
        LOGGER.info("Testing: " + testInfo.getDisplayName());

        final DateTime dt1 = new DateTime(1700000000L, 500000000);
        final DateTime dt2 = new DateTime(1700000000L, 500000000);

        assertEquals(dt1, dt2, "DateTimes with same values should be equal");
        assertEquals(dt1.hashCode(), dt2.hashCode(), "Hash codes should match for equal objects");

        LOGGER.info("Equality verified for: " + dt1);
      }

      @Test
      @DisplayName("should not be equal for different seconds")
      void shouldNotBeEqualForDifferentSeconds(final TestInfo testInfo) {
        LOGGER.info("Testing: " + testInfo.getDisplayName());

        final DateTime dt1 = new DateTime(1700000000L, 500000000);
        final DateTime dt2 = new DateTime(1700000001L, 500000000);

        assertNotEquals(dt1, dt2, "DateTimes with different seconds should not be equal");

        LOGGER.info("Inequality verified for different seconds");
      }

      @Test
      @DisplayName("should not be equal for different nanoseconds")
      void shouldNotBeEqualForDifferentNanoseconds(final TestInfo testInfo) {
        LOGGER.info("Testing: " + testInfo.getDisplayName());

        final DateTime dt1 = new DateTime(1700000000L, 500000000);
        final DateTime dt2 = new DateTime(1700000000L, 500000001);

        assertNotEquals(dt1, dt2, "DateTimes with different nanoseconds should not be equal");

        LOGGER.info("Inequality verified for different nanoseconds");
      }

      @Test
      @DisplayName("should handle equality with self")
      void shouldHandleEqualityWithSelf(final TestInfo testInfo) {
        LOGGER.info("Testing: " + testInfo.getDisplayName());

        final DateTime dt = new DateTime(1700000000L, 500000000);

        assertEquals(dt, dt, "DateTime should be equal to itself");

        LOGGER.info("Self-equality verified");
      }

      @Test
      @DisplayName("should handle equality with null")
      void shouldHandleEqualityWithNull(final TestInfo testInfo) {
        LOGGER.info("Testing: " + testInfo.getDisplayName());

        final DateTime dt = new DateTime(1700000000L, 500000000);

        assertNotEquals(null, dt, "DateTime should not be equal to null");

        LOGGER.info("Null inequality verified");
      }

      @Test
      @DisplayName("should handle equality with different type")
      void shouldHandleEqualityWithDifferentType(final TestInfo testInfo) {
        LOGGER.info("Testing: " + testInfo.getDisplayName());

        final DateTime dt = new DateTime(1700000000L, 500000000);

        assertNotEquals("not a datetime", dt, "DateTime should not be equal to String");

        LOGGER.info("Different type inequality verified");
      }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

      @Test
      @DisplayName("toString should include seconds and nanoseconds")
      void toStringShouldIncludeSecondsAndNanoseconds(final TestInfo testInfo) {
        LOGGER.info("Testing: " + testInfo.getDisplayName());

        final DateTime dt = new DateTime(1700000000L, 500000000);
        final String str = dt.toString();

        assertNotNull(str, "toString should not return null");
        assertTrue(str.contains("1700000000"), "toString should contain seconds");
        assertTrue(str.contains("500000000"), "toString should contain nanoseconds");

        LOGGER.info("toString result: " + str);
      }
    }
  }

  // ========================================================================
  // TimezoneDisplay Functional Tests
  // ========================================================================

  @Nested
  @DisplayName("TimezoneDisplay Functional Tests")
  class TimezoneDisplayFunctionalTests {

    @Test
    @DisplayName("should create TimezoneDisplay with UTC values")
    void shouldCreateTimezoneDisplayWithUtcValues(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final TimezoneDisplay tz = new TimezoneDisplay(0, "UTC", false);

      assertEquals(0, tz.getUtcOffsetSeconds(), "UTC offset should be 0");
      assertEquals("UTC", tz.getName(), "Name should be UTC");
      assertFalse(tz.isInDaylightSavingTime(), "Should not be in DST");

      LOGGER.info("Created UTC TimezoneDisplay: " + tz);
    }

    @Test
    @DisplayName("should create TimezoneDisplay with positive offset (east of UTC)")
    void shouldCreateTimezoneDisplayWithPositiveOffset(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // +5:30 for India Standard Time
      final int offsetSeconds = 5 * 3600 + 30 * 60;
      final TimezoneDisplay tz = new TimezoneDisplay(offsetSeconds, "IST", false);

      assertEquals(offsetSeconds, tz.getUtcOffsetSeconds(), "Offset should match");
      assertEquals("IST", tz.getName(), "Name should match");

      LOGGER.info("Created IST TimezoneDisplay: " + tz);
    }

    @Test
    @DisplayName("should create TimezoneDisplay with negative offset (west of UTC)")
    void shouldCreateTimezoneDisplayWithNegativeOffset(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // -5:00 for EST
      final int offsetSeconds = -5 * 3600;
      final TimezoneDisplay tz = new TimezoneDisplay(offsetSeconds, "EST", false);

      assertEquals(offsetSeconds, tz.getUtcOffsetSeconds(), "Negative offset should be allowed");
      assertEquals("EST", tz.getName(), "Name should match");

      LOGGER.info("Created EST TimezoneDisplay: " + tz);
    }

    @Test
    @DisplayName("should create TimezoneDisplay with DST active")
    void shouldCreateTimezoneDisplayWithDstActive(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // -4:00 for EDT (Eastern Daylight Time)
      final int offsetSeconds = -4 * 3600;
      final TimezoneDisplay tz = new TimezoneDisplay(offsetSeconds, "EDT", true);

      assertTrue(tz.isInDaylightSavingTime(), "DST should be active");
      assertEquals("EDT", tz.getName(), "Name should match");

      LOGGER.info("Created EDT TimezoneDisplay with DST: " + tz);
    }
  }

  // ========================================================================
  // WASI KeyValue Enums Functional Tests
  // ========================================================================

  @Nested
  @DisplayName("WASI KeyValue Enums Functional Tests")
  class WasiKeyValueEnumsFunctionalTests {

    @Test
    @DisplayName("KeyValueErrorCode should have all expected codes")
    void keyValueErrorCodeShouldHaveAllExpectedCodes(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Set<String> expected =
          Set.of("KEY_NOT_FOUND", "CAPACITY_EXCEEDED", "INTERNAL_ERROR", "TIMEOUT");

      final Set<String> actual = new HashSet<>();
      for (final KeyValueErrorCode code : KeyValueErrorCode.values()) {
        actual.add(code.name());
      }

      for (final String exp : expected) {
        assertTrue(actual.contains(exp), "Should contain error code: " + exp);
      }

      LOGGER.info("Found " + actual.size() + " error codes: " + actual);
    }
  }

  // ========================================================================
  // WASI KeyValue Exception Tests
  // ========================================================================

  @Nested
  @DisplayName("WASI KeyValue Exception Tests")
  class WasiKeyValueExceptionTests {

    @Test
    @DisplayName("should create KeyValueException with message")
    void shouldCreateKeyValueExceptionWithMessage(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final KeyValueException ex = new KeyValueException("Test KV error");

      assertEquals("Test KV error", ex.getMessage(), "Message should match");

      LOGGER.info("Created KeyValueException: " + ex.getMessage());
    }

    @Test
    @DisplayName("should create KeyValueException with error code")
    void shouldCreateKeyValueExceptionWithErrorCode(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final KeyValueException ex =
          new KeyValueException("Key missing", KeyValueErrorCode.KEY_NOT_FOUND);

      assertEquals(KeyValueErrorCode.KEY_NOT_FOUND, ex.getErrorCode(), "Error code should match");
      assertEquals("Key missing", ex.getMessage(), "Message should match");

      LOGGER.info("Created KeyValueException with code: " + ex.getErrorCode());
    }
  }

  // ========================================================================
  // WASI NN Enums Functional Tests
  // ========================================================================

  @Nested
  @DisplayName("WASI NN Enums Functional Tests")
  class WasiNnEnumsFunctionalTests {

    @Test
    @DisplayName("NnTensorType should have all expected types")
    void nnTensorTypeShouldHaveAllExpectedTypes(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Actual values: FP16, FP32, FP64, BF16, U8, I32, I64
      final Set<String> expected = Set.of("FP32", "FP64", "I32", "I64", "U8", "FP16");

      final Set<String> actual = new HashSet<>();
      for (final NnTensorType type : NnTensorType.values()) {
        actual.add(type.name());
      }

      for (final String exp : expected) {
        assertTrue(actual.contains(exp), "Should contain tensor type: " + exp);
      }

      LOGGER.info("Found " + actual.size() + " tensor types: " + actual);
    }

    @Test
    @DisplayName("NnGraphEncoding should have all expected encodings")
    void nnGraphEncodingShouldHaveAllExpectedEncodings(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Set<String> expected = Set.of("ONNX", "OPENVINO", "PYTORCH", "TENSORFLOW");

      final Set<String> actual = new HashSet<>();
      for (final NnGraphEncoding encoding : NnGraphEncoding.values()) {
        actual.add(encoding.name());
      }

      for (final String exp : expected) {
        assertTrue(actual.contains(exp), "Should contain graph encoding: " + exp);
      }

      LOGGER.info("Found " + actual.size() + " graph encodings: " + actual);
    }

    @Test
    @DisplayName("NnExecutionTarget should have all expected targets")
    void nnExecutionTargetShouldHaveAllExpectedTargets(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Set<String> expected = Set.of("CPU", "GPU", "TPU");

      final Set<String> actual = new HashSet<>();
      for (final NnExecutionTarget target : NnExecutionTarget.values()) {
        actual.add(target.name());
      }

      for (final String exp : expected) {
        assertTrue(actual.contains(exp), "Should contain execution target: " + exp);
      }

      LOGGER.info("Found " + actual.size() + " execution targets: " + actual);
    }

    @Test
    @DisplayName("NnErrorCode should have all expected codes")
    void nnErrorCodeShouldHaveAllExpectedCodes(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Actual values: INVALID_ARGUMENT, INVALID_ENCODING, RUNTIME_ERROR, UNSUPPORTED_OPERATION,
      // etc.
      final Set<String> expected =
          Set.of("INVALID_ARGUMENT", "INVALID_ENCODING", "RUNTIME_ERROR", "UNSUPPORTED_OPERATION");

      final Set<String> actual = new HashSet<>();
      for (final NnErrorCode code : NnErrorCode.values()) {
        actual.add(code.name());
      }

      for (final String exp : expected) {
        assertTrue(actual.contains(exp), "Should contain error code: " + exp);
      }

      LOGGER.info("Found " + actual.size() + " error codes: " + actual);
    }
  }

  // ========================================================================
  // WASI NN Exception Tests
  // ========================================================================

  @Nested
  @DisplayName("WASI NN Exception Tests")
  class WasiNnExceptionTests {

    @Test
    @DisplayName("should create NnException with message")
    void shouldCreateNnExceptionWithMessage(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final NnException ex = new NnException("Test NN error");

      assertEquals("Test NN error", ex.getMessage(), "Message should match");

      LOGGER.info("Created NnException: " + ex.getMessage());
    }

    @Test
    @DisplayName("should create NnException with error code")
    void shouldCreateNnExceptionWithErrorCode(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final NnException ex = new NnException(NnErrorCode.INVALID_ARGUMENT, "Bad input");

      assertEquals(NnErrorCode.INVALID_ARGUMENT, ex.getErrorCode(), "Error code should match");
      assertTrue(ex.getMessage().contains("Bad input"), "Message should contain description");

      LOGGER.info("Created NnException with code: " + ex.getErrorCode());
    }
  }
}
