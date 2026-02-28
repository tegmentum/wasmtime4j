/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.wasi.clocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WASI clocks package classes.
 *
 * <p>This test class validates the DateTime, TimezoneDisplay, and related clock classes.
 */
@DisplayName("WASI Clocks Integration Tests")
public class WasiClocksIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(WasiClocksIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting WASI Clocks Integration Tests");
  }

  @Nested
  @DisplayName("DateTime Tests")
  class DateTimeTests {

    @Test
    @DisplayName("Should create DateTime with valid values")
    void shouldCreateDateTimeWithValidValues() {
      LOGGER.info("Testing DateTime creation");

      DateTime dateTime = new DateTime(1000000000L, 500000000);

      assertEquals(1000000000L, dateTime.getSeconds(), "Seconds should match");
      assertEquals(500000000, dateTime.getNanoseconds(), "Nanoseconds should match");

      LOGGER.info("DateTime created: " + dateTime);
    }

    @Test
    @DisplayName("Should create DateTime with zero nanoseconds")
    void shouldCreateDateTimeWithZeroNanoseconds() {
      LOGGER.info("Testing DateTime with zero nanoseconds");

      DateTime dateTime = new DateTime(12345L, 0);

      assertEquals(12345L, dateTime.getSeconds(), "Seconds should match");
      assertEquals(0, dateTime.getNanoseconds(), "Nanoseconds should be 0");

      LOGGER.info("DateTime with zero nanoseconds verified");
    }

    @Test
    @DisplayName("Should create DateTime with max valid nanoseconds")
    void shouldCreateDateTimeWithMaxValidNanoseconds() {
      LOGGER.info("Testing DateTime with max valid nanoseconds");

      DateTime dateTime = new DateTime(0L, 999999999);

      assertEquals(999999999, dateTime.getNanoseconds(), "Max nanoseconds should be 999999999");

      LOGGER.info("DateTime with max nanoseconds verified");
    }

    @Test
    @DisplayName("Should reject nanoseconds >= 1 billion")
    void shouldRejectNanosecondsGreaterThanOrEqualTo1Billion() {
      LOGGER.info("Testing DateTime rejection of invalid nanoseconds");

      assertThrows(
          IllegalArgumentException.class,
          () -> new DateTime(0L, 1_000_000_000),
          "Should reject nanoseconds >= 1 billion");

      LOGGER.info("Invalid nanoseconds rejection verified");
    }

    @Test
    @DisplayName("Should reject negative nanoseconds")
    void shouldRejectNegativeNanoseconds() {
      LOGGER.info("Testing DateTime rejection of negative nanoseconds");

      assertThrows(
          IllegalArgumentException.class,
          () -> new DateTime(0L, -1),
          "Should reject negative nanoseconds");

      LOGGER.info("Negative nanoseconds rejection verified");
    }

    @Test
    @DisplayName("Should allow negative seconds")
    void shouldAllowNegativeSeconds() {
      LOGGER.info("Testing DateTime with negative seconds (before epoch)");

      DateTime dateTime = new DateTime(-1000L, 500);

      assertEquals(-1000L, dateTime.getSeconds(), "Negative seconds should be allowed");

      LOGGER.info("Negative seconds handling verified");
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      LOGGER.info("Testing DateTime equals");

      DateTime dt1 = new DateTime(1000L, 500);
      DateTime dt2 = new DateTime(1000L, 500);
      DateTime dt3 = new DateTime(1000L, 600);
      DateTime dt4 = new DateTime(2000L, 500);

      assertEquals(dt1, dt2, "Same values should be equal");
      assertNotEquals(dt1, dt3, "Different nanoseconds should not be equal");
      assertNotEquals(dt1, dt4, "Different seconds should not be equal");
      assertNotEquals(dt1, null, "Should not equal null");
      assertNotEquals(dt1, "string", "Should not equal different type");

      LOGGER.info("Equals implementation verified");
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      LOGGER.info("Testing DateTime hashCode");

      DateTime dt1 = new DateTime(1000L, 500);
      DateTime dt2 = new DateTime(1000L, 500);
      DateTime dt3 = new DateTime(1000L, 600);

      assertEquals(dt1.hashCode(), dt2.hashCode(), "Equal objects should have same hashCode");
      // Different objects may have same hashCode (not a requirement, but useful to check)
      assertNotEquals(
          dt1.hashCode(), dt3.hashCode(), "Different objects likely have different hashCode");

      LOGGER.info("HashCode implementation verified");
    }

    @Test
    @DisplayName("Should have meaningful toString")
    void shouldHaveMeaningfulToString() {
      LOGGER.info("Testing DateTime toString");

      DateTime dateTime = new DateTime(1000L, 500);
      String str = dateTime.toString();

      assertTrue(str.contains("1000"), "toString should contain seconds");
      assertTrue(str.contains("500"), "toString should contain nanoseconds");
      assertTrue(str.contains("DateTime"), "toString should contain class name");

      LOGGER.info("ToString verified: " + str);
    }

    @Test
    @DisplayName("Should handle epoch time correctly")
    void shouldHandleEpochTimeCorrectly() {
      LOGGER.info("Testing DateTime at Unix epoch");

      DateTime epoch = new DateTime(0L, 0);

      assertEquals(0L, epoch.getSeconds(), "Epoch seconds should be 0");
      assertEquals(0, epoch.getNanoseconds(), "Epoch nanoseconds should be 0");

      LOGGER.info("Epoch time handling verified");
    }

    @Test
    @DisplayName("Should handle large seconds values")
    void shouldHandleLargeSecondsValues() {
      LOGGER.info("Testing DateTime with large seconds");

      long largeSeconds = Long.MAX_VALUE;
      DateTime dateTime = new DateTime(largeSeconds, 0);

      assertEquals(largeSeconds, dateTime.getSeconds(), "Large seconds should be handled");

      LOGGER.info("Large seconds handling verified");
    }
  }

  @Nested
  @DisplayName("TimezoneDisplay Tests")
  class TimezoneDisplayTests {

    @Test
    @DisplayName("Should create TimezoneDisplay with valid values")
    void shouldCreateTimezoneDisplayWithValidValues() {
      LOGGER.info("Testing TimezoneDisplay creation");

      TimezoneDisplay tz = new TimezoneDisplay(-18000, "EST", false);

      assertEquals(-18000, tz.getUtcOffsetSeconds(), "Offset should match");
      assertEquals("EST", tz.getName(), "Name should match");
      assertFalse(tz.isInDaylightSavingTime(), "DST should be false");

      LOGGER.info("TimezoneDisplay created: " + tz);
    }

    @Test
    @DisplayName("Should create TimezoneDisplay with DST")
    void shouldCreateTimezoneDisplayWithDst() {
      LOGGER.info("Testing TimezoneDisplay with DST");

      TimezoneDisplay tz = new TimezoneDisplay(-14400, "EDT", true);

      assertEquals(-14400, tz.getUtcOffsetSeconds(), "Offset should match");
      assertEquals("EDT", tz.getName(), "Name should match");
      assertTrue(tz.isInDaylightSavingTime(), "DST should be true");

      LOGGER.info("TimezoneDisplay with DST verified");
    }

    @Test
    @DisplayName("Should create UTC timezone")
    void shouldCreateUtcTimezone() {
      LOGGER.info("Testing UTC TimezoneDisplay");

      TimezoneDisplay utc = new TimezoneDisplay(0, "UTC", false);

      assertEquals(0, utc.getUtcOffsetSeconds(), "UTC offset should be 0");
      assertEquals("UTC", utc.getName(), "Name should be UTC");
      assertFalse(utc.isInDaylightSavingTime(), "UTC should not have DST");

      LOGGER.info("UTC timezone verified");
    }

    @Test
    @DisplayName("Should accept positive offset (east of UTC)")
    void shouldAcceptPositiveOffset() {
      LOGGER.info("Testing positive offset");

      // Tokyo: UTC+9 = 32400 seconds
      TimezoneDisplay tokyo = new TimezoneDisplay(32400, "JST", false);

      assertEquals(32400, tokyo.getUtcOffsetSeconds(), "Tokyo offset should be +9 hours");

      LOGGER.info("Positive offset verified");
    }

    @Test
    @DisplayName("Should reject offset >= 86400")
    void shouldRejectOffsetGreaterThanOrEqualTo86400() {
      LOGGER.info("Testing offset validation");

      assertThrows(
          IllegalArgumentException.class,
          () -> new TimezoneDisplay(86400, "INVALID", false),
          "Should reject offset >= 86400");

      assertThrows(
          IllegalArgumentException.class,
          () -> new TimezoneDisplay(-86400, "INVALID", false),
          "Should reject offset <= -86400");

      LOGGER.info("Offset validation verified");
    }

    @Test
    @DisplayName("Should reject null name")
    void shouldRejectNullName() {
      LOGGER.info("Testing null name rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new TimezoneDisplay(0, null, false),
          "Should reject null name");

      LOGGER.info("Null name rejection verified");
    }

    @Test
    @DisplayName("Should accept max valid offset")
    void shouldAcceptMaxValidOffset() {
      LOGGER.info("Testing max valid offset");

      TimezoneDisplay tz = new TimezoneDisplay(86399, "MAX", false);

      assertEquals(86399, tz.getUtcOffsetSeconds(), "Max offset should be accepted");

      LOGGER.info("Max offset verified");
    }

    @Test
    @DisplayName("Should accept min valid offset")
    void shouldAcceptMinValidOffset() {
      LOGGER.info("Testing min valid offset");

      TimezoneDisplay tz = new TimezoneDisplay(-86399, "MIN", false);

      assertEquals(-86399, tz.getUtcOffsetSeconds(), "Min offset should be accepted");

      LOGGER.info("Min offset verified");
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      LOGGER.info("Testing TimezoneDisplay equals");

      TimezoneDisplay tz1 = new TimezoneDisplay(0, "UTC", false);
      TimezoneDisplay tz2 = new TimezoneDisplay(0, "UTC", false);
      assertEquals(tz1, tz2, "Same values should be equal");

      TimezoneDisplay tz3 = new TimezoneDisplay(0, "GMT", false);
      assertNotEquals(tz1, tz3, "Different names should not be equal");

      TimezoneDisplay tz4 = new TimezoneDisplay(3600, "UTC", false);
      assertNotEquals(tz1, tz4, "Different offsets should not be equal");

      TimezoneDisplay tz5 = new TimezoneDisplay(0, "UTC", true);
      assertNotEquals(tz1, tz5, "Different DST should not be equal");

      assertNotEquals(tz1, null, "Should not equal null");

      LOGGER.info("Equals implementation verified");
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      LOGGER.info("Testing TimezoneDisplay hashCode");

      TimezoneDisplay tz1 = new TimezoneDisplay(0, "UTC", false);
      TimezoneDisplay tz2 = new TimezoneDisplay(0, "UTC", false);

      assertEquals(tz1.hashCode(), tz2.hashCode(), "Equal objects should have same hashCode");

      LOGGER.info("HashCode implementation verified");
    }

    @Test
    @DisplayName("Should have meaningful toString")
    void shouldHaveMeaningfulToString() {
      LOGGER.info("Testing TimezoneDisplay toString");

      TimezoneDisplay tz = new TimezoneDisplay(-18000, "EST", false);
      String str = tz.toString();

      assertTrue(str.contains("-18000"), "toString should contain offset");
      assertTrue(str.contains("EST"), "toString should contain name");
      assertTrue(str.contains("false"), "toString should contain DST status");

      LOGGER.info("ToString verified: " + str);
    }

    @Test
    @DisplayName("Should accept empty name")
    void shouldAcceptEmptyName() {
      LOGGER.info("Testing empty name acceptance");

      TimezoneDisplay tz = new TimezoneDisplay(0, "", false);

      assertEquals("", tz.getName(), "Empty name should be accepted");

      LOGGER.info("Empty name acceptance verified");
    }
  }

  @Nested
  @DisplayName("Clock Interface Tests")
  class ClockInterfaceTests {

    @Test
    @DisplayName("Should define WasiWallClock interface")
    void shouldDefineWasiWallClockInterface() {
      LOGGER.info("Testing WasiWallClock interface definition");

      // Verify the interface exists and has expected methods
      assertNotNull(WasiWallClock.class, "WasiWallClock interface should exist");
      assertTrue(WasiWallClock.class.isInterface(), "WasiWallClock should be an interface");

      LOGGER.info("WasiWallClock interface verified");
    }

    @Test
    @DisplayName("Should define WasiMonotonicClock interface")
    void shouldDefineWasiMonotonicClockInterface() {
      LOGGER.info("Testing WasiMonotonicClock interface definition");

      assertNotNull(WasiMonotonicClock.class, "WasiMonotonicClock interface should exist");
      assertTrue(
          WasiMonotonicClock.class.isInterface(), "WasiMonotonicClock should be an interface");

      LOGGER.info("WasiMonotonicClock interface verified");
    }

    @Test
    @DisplayName("Should define WasiTimezone interface")
    void shouldDefineWasiTimezoneInterface() {
      LOGGER.info("Testing WasiTimezone interface definition");

      assertNotNull(WasiTimezone.class, "WasiTimezone interface should exist");
      assertTrue(WasiTimezone.class.isInterface(), "WasiTimezone should be an interface");

      LOGGER.info("WasiTimezone interface verified");
    }
  }
}
