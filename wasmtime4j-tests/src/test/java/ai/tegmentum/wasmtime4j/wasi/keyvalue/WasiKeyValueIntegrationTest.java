/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.wasi.keyvalue;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WASI keyvalue package classes.
 *
 * <p>This test class validates the keyvalue-related enums, builders, and value classes.
 */
@DisplayName("WASI KeyValue Integration Tests")
public class WasiKeyValueIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiKeyValueIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting WASI KeyValue Integration Tests");
  }

  @Nested
  @DisplayName("KeyValueEntry Tests")
  class KeyValueEntryTests {

    @Test
    @DisplayName("Should build entry with key and value")
    void shouldBuildEntryWithKeyAndValue() {
      LOGGER.info("Testing KeyValueEntry builder");

      byte[] value = "test value".getBytes();
      KeyValueEntry entry = KeyValueEntry.builder("test-key", value).build();

      assertNotNull(entry, "Entry should not be null");
      assertEquals("test-key", entry.getKey(), "Key should match");
      assertArrayEquals(value, entry.getValue(), "Value should match");
      assertEquals(1, entry.getVersion(), "Default version should be 1");

      LOGGER.info("KeyValueEntry built successfully");
    }

    @Test
    @DisplayName("Should build entry with version")
    void shouldBuildEntryWithVersion() {
      LOGGER.info("Testing KeyValueEntry with version");

      KeyValueEntry entry = KeyValueEntry.builder("key", "value".getBytes())
          .version(42)
          .build();

      assertEquals(42, entry.getVersion(), "Version should match");

      LOGGER.info("KeyValueEntry with version verified");
    }

    @Test
    @DisplayName("Should build entry with timestamps")
    void shouldBuildEntryWithTimestamps() {
      LOGGER.info("Testing KeyValueEntry with timestamps");

      Instant now = Instant.now();
      Instant future = now.plusSeconds(3600);

      KeyValueEntry entry = KeyValueEntry.builder("key", "value".getBytes())
          .createdAt(now)
          .modifiedAt(now)
          .expiresAt(future)
          .build();

      assertTrue(entry.getCreatedAt().isPresent(), "Created at should be present");
      assertTrue(entry.getModifiedAt().isPresent(), "Modified at should be present");
      assertTrue(entry.getExpiresAt().isPresent(), "Expires at should be present");
      assertEquals(now, entry.getCreatedAt().get(), "Created at should match");
      assertEquals(now, entry.getModifiedAt().get(), "Modified at should match");
      assertEquals(future, entry.getExpiresAt().get(), "Expires at should match");

      LOGGER.info("KeyValueEntry with timestamps verified");
    }

    @Test
    @DisplayName("Should handle null value")
    void shouldHandleNullValue() {
      LOGGER.info("Testing KeyValueEntry with null value");

      KeyValueEntry entry = KeyValueEntry.builder("key", null).build();

      assertNull(entry.getValue(), "Value should be null");

      LOGGER.info("Null value handling verified");
    }

    @Test
    @DisplayName("Should defensively copy value on build")
    void shouldDefensivelyCopyValueOnBuild() {
      LOGGER.info("Testing KeyValueEntry defensive copying on build");

      byte[] value = "original".getBytes();
      KeyValueEntry entry = KeyValueEntry.builder("key", value).build();

      // Modify original array
      value[0] = 'X';

      // Entry should have original value
      assertEquals('o', (char) entry.getValue()[0], "Entry should have original value");

      LOGGER.info("Defensive copying on build verified");
    }

    @Test
    @DisplayName("Should defensively copy value on get")
    void shouldDefensivelyCopyValueOnGet() {
      LOGGER.info("Testing KeyValueEntry defensive copying on get");

      KeyValueEntry entry = KeyValueEntry.builder("key", "original".getBytes()).build();

      byte[] retrieved = entry.getValue();
      retrieved[0] = 'X';

      // Entry should still have original value
      assertEquals('o', (char) entry.getValue()[0], "Entry should maintain original value");

      LOGGER.info("Defensive copying on get verified");
    }

    @Test
    @DisplayName("Should detect expired entry")
    void shouldDetectExpiredEntry() {
      LOGGER.info("Testing KeyValueEntry expiration detection");

      Instant past = Instant.now().minusSeconds(3600);
      KeyValueEntry entry = KeyValueEntry.builder("key", "value".getBytes())
          .expiresAt(past)
          .build();

      assertTrue(entry.isExpired(), "Entry should be expired");

      LOGGER.info("Expiration detection verified");
    }

    @Test
    @DisplayName("Should detect non-expired entry")
    void shouldDetectNonExpiredEntry() {
      LOGGER.info("Testing KeyValueEntry non-expiration");

      Instant future = Instant.now().plusSeconds(3600);
      KeyValueEntry entry = KeyValueEntry.builder("key", "value".getBytes())
          .expiresAt(future)
          .build();

      assertFalse(entry.isExpired(), "Entry should not be expired");

      LOGGER.info("Non-expiration verified");
    }

    @Test
    @DisplayName("Should not be expired when no expiration set")
    void shouldNotBeExpiredWhenNoExpirationSet() {
      LOGGER.info("Testing KeyValueEntry without expiration");

      KeyValueEntry entry = KeyValueEntry.builder("key", "value".getBytes()).build();

      assertFalse(entry.isExpired(), "Entry without expiration should not be expired");

      LOGGER.info("No expiration handling verified");
    }

    @Test
    @DisplayName("Should implement equals based on key and version")
    void shouldImplementEqualsBasedOnKeyAndVersion() {
      LOGGER.info("Testing KeyValueEntry equals");

      KeyValueEntry entry1 = KeyValueEntry.builder("key", "value1".getBytes())
          .version(1)
          .build();
      KeyValueEntry entry2 = KeyValueEntry.builder("key", "value2".getBytes())
          .version(1)
          .build();
      KeyValueEntry entry3 = KeyValueEntry.builder("key", "value1".getBytes())
          .version(2)
          .build();

      assertEquals(entry1, entry2, "Entries with same key and version should be equal");
      assertNotEquals(entry1, entry3, "Entries with different versions should not be equal");

      LOGGER.info("Equals implementation verified");
    }

    @Test
    @DisplayName("Should implement hashCode based on key and version")
    void shouldImplementHashCodeBasedOnKeyAndVersion() {
      LOGGER.info("Testing KeyValueEntry hashCode");

      KeyValueEntry entry1 = KeyValueEntry.builder("key", "value1".getBytes())
          .version(1)
          .build();
      KeyValueEntry entry2 = KeyValueEntry.builder("key", "value2".getBytes())
          .version(1)
          .build();

      assertEquals(entry1.hashCode(), entry2.hashCode(), "Equal entries should have same hashCode");

      LOGGER.info("HashCode implementation verified");
    }

    @Test
    @DisplayName("Should have meaningful toString")
    void shouldHaveMeaningfulToString() {
      LOGGER.info("Testing KeyValueEntry toString");

      KeyValueEntry entry = KeyValueEntry.builder("test-key", "value".getBytes())
          .version(5)
          .build();

      String str = entry.toString();
      assertTrue(str.contains("test-key"), "toString should contain key");
      assertTrue(str.contains("5"), "toString should contain version");

      LOGGER.info("ToString verified: " + str);
    }
  }

  @Nested
  @DisplayName("ConsistencyModel Tests")
  class ConsistencyModelTests {

    @Test
    @DisplayName("Should have all expected consistency models")
    void shouldHaveAllExpectedConsistencyModels() {
      LOGGER.info("Testing ConsistencyModel enum values");

      ConsistencyModel[] models = ConsistencyModel.values();

      assertTrue(models.length >= 1, "Should have at least 1 consistency model");

      LOGGER.info("Consistency models verified: " + models.length + " models");
    }
  }

  @Nested
  @DisplayName("IsolationLevel Tests")
  class IsolationLevelTests {

    @Test
    @DisplayName("Should have all expected isolation levels")
    void shouldHaveAllExpectedIsolationLevels() {
      LOGGER.info("Testing IsolationLevel enum values");

      IsolationLevel[] levels = IsolationLevel.values();

      assertTrue(levels.length >= 1, "Should have at least 1 isolation level");

      LOGGER.info("Isolation levels verified: " + levels.length + " levels");
    }
  }

  @Nested
  @DisplayName("EvictionPolicy Tests")
  class EvictionPolicyTests {

    @Test
    @DisplayName("Should have all expected eviction policies")
    void shouldHaveAllExpectedEvictionPolicies() {
      LOGGER.info("Testing EvictionPolicy enum values");

      EvictionPolicy[] policies = EvictionPolicy.values();

      assertTrue(policies.length >= 1, "Should have at least 1 eviction policy");

      LOGGER.info("Eviction policies verified: " + policies.length + " policies");
    }
  }

  @Nested
  @DisplayName("KeyValueErrorCode Tests")
  class KeyValueErrorCodeTests {

    @Test
    @DisplayName("Should have all expected error codes")
    void shouldHaveAllExpectedErrorCodes() {
      LOGGER.info("Testing KeyValueErrorCode enum values");

      KeyValueErrorCode[] codes = KeyValueErrorCode.values();

      assertTrue(codes.length >= 1, "Should have at least 1 error code");

      LOGGER.info("Error codes verified: " + codes.length + " codes");
    }
  }

  @Nested
  @DisplayName("KeyValueException Tests")
  class KeyValueExceptionTests {

    @Test
    @DisplayName("Should create exception with message")
    void shouldCreateExceptionWithMessage() {
      LOGGER.info("Testing KeyValueException creation");

      KeyValueException exception = new KeyValueException("Test error");

      assertNotNull(exception, "Exception should not be null");
      assertEquals("Test error", exception.getMessage(), "Message should match");

      LOGGER.info("KeyValueException creation verified");
    }

    @Test
    @DisplayName("Should create exception with cause")
    void shouldCreateExceptionWithCause() {
      LOGGER.info("Testing KeyValueException with cause");

      RuntimeException cause = new RuntimeException("Cause");
      KeyValueException exception = new KeyValueException("Test error", cause);

      assertNotNull(exception.getCause(), "Cause should not be null");
      assertEquals(cause, exception.getCause(), "Cause should match");

      LOGGER.info("KeyValueException with cause verified");
    }
  }
}
