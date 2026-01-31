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

package ai.tegmentum.wasmtime4j.wasi.keyvalue;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link KeyValueEntry} class.
 *
 * <p>Verifies builder construction, field accessors, defensive copies on byte[] value, version
 * defaults, timestamp optionals, expiration logic, and equals/hashCode based on key+version.
 */
@DisplayName("KeyValueEntry Tests")
class KeyValueEntryTest {

  @Nested
  @DisplayName("Builder Construction Tests")
  class BuilderConstructionTests {

    @Test
    @DisplayName("should build with all fields set")
    void shouldBuildWithAllFieldsSet() {
      final Instant created = Instant.parse("2025-01-01T00:00:00Z");
      final Instant modified = Instant.parse("2025-01-02T00:00:00Z");
      final Instant expires = Instant.parse("2026-01-01T00:00:00Z");
      final byte[] value = {1, 2, 3};
      final KeyValueEntry entry =
          KeyValueEntry.builder("myKey", value)
              .version(5L)
              .createdAt(created)
              .modifiedAt(modified)
              .expiresAt(expires)
              .build();

      assertEquals("myKey", entry.getKey(), "Key should be 'myKey'");
      assertArrayEquals(value, entry.getValue(), "Value should match");
      assertEquals(5L, entry.getVersion(), "Version should be 5");
      assertTrue(entry.getCreatedAt().isPresent(), "CreatedAt should be present");
      assertEquals(created, entry.getCreatedAt().get(), "CreatedAt should match");
      assertTrue(entry.getModifiedAt().isPresent(), "ModifiedAt should be present");
      assertEquals(modified, entry.getModifiedAt().get(), "ModifiedAt should match");
      assertTrue(entry.getExpiresAt().isPresent(), "ExpiresAt should be present");
      assertEquals(expires, entry.getExpiresAt().get(), "ExpiresAt should match");
    }

    @Test
    @DisplayName("should build with only key and value (defaults)")
    void shouldBuildWithOnlyKeyAndValue() {
      final byte[] value = {42};
      final KeyValueEntry entry = KeyValueEntry.builder("key", value).build();

      assertEquals("key", entry.getKey(), "Key should be 'key'");
      assertArrayEquals(new byte[]{42}, entry.getValue(), "Value should match");
      assertEquals(1L, entry.getVersion(), "Default version should be 1");
      assertFalse(entry.getCreatedAt().isPresent(), "CreatedAt should not be present by default");
      assertFalse(
          entry.getModifiedAt().isPresent(), "ModifiedAt should not be present by default");
      assertFalse(entry.getExpiresAt().isPresent(), "ExpiresAt should not be present by default");
    }

    @Test
    @DisplayName("should build with null value")
    void shouldBuildWithNullValue() {
      final KeyValueEntry entry = KeyValueEntry.builder("key", null).build();
      assertNull(entry.getValue(), "Value should be null");
    }

    @Test
    @DisplayName("should build with null key")
    void shouldBuildWithNullKey() {
      final KeyValueEntry entry = KeyValueEntry.builder(null, new byte[]{1}).build();
      assertNull(entry.getKey(), "Key should be null");
    }
  }

  @Nested
  @DisplayName("Defensive Copy Tests")
  class DefensiveCopyTests {

    @Test
    @DisplayName("should defensively copy value on construction")
    void shouldDefensivelyCopyValueOnConstruction() {
      final byte[] value = {1, 2, 3};
      final KeyValueEntry entry = KeyValueEntry.builder("key", value).build();

      value[0] = 99;
      assertEquals(
          1, entry.getValue()[0],
          "Modifying original value should not affect entry");
    }

    @Test
    @DisplayName("should defensively copy value on retrieval")
    void shouldDefensivelyCopyValueOnRetrieval() {
      final byte[] value = {1, 2, 3};
      final KeyValueEntry entry = KeyValueEntry.builder("key", value).build();

      final byte[] retrieved = entry.getValue();
      retrieved[0] = 99;
      assertEquals(
          1, entry.getValue()[0],
          "Modifying retrieved value should not affect entry");
    }

    @Test
    @DisplayName("builder should defensively copy value")
    void builderShouldDefensivelyCopyValue() {
      final byte[] value = {10, 20, 30};
      final KeyValueEntry.Builder builder = KeyValueEntry.builder("key", value);
      value[0] = 99;
      final KeyValueEntry entry = builder.build();
      assertEquals(
          10, entry.getValue()[0],
          "Builder should defensively copy value from constructor");
    }
  }

  @Nested
  @DisplayName("Expiration Tests")
  class ExpirationTests {

    @Test
    @DisplayName("isExpired should return true for past expiration")
    void isExpiredShouldReturnTrueForPastExpiration() {
      final Instant pastExpiry = Instant.parse("2020-01-01T00:00:00Z");
      final KeyValueEntry entry =
          KeyValueEntry.builder("key", new byte[]{1})
              .expiresAt(pastExpiry)
              .build();
      assertTrue(entry.isExpired(), "Entry with past expiration should be expired");
    }

    @Test
    @DisplayName("isExpired should return false for future expiration")
    void isExpiredShouldReturnFalseForFutureExpiration() {
      final Instant futureExpiry = Instant.parse("2099-12-31T23:59:59Z");
      final KeyValueEntry entry =
          KeyValueEntry.builder("key", new byte[]{1})
              .expiresAt(futureExpiry)
              .build();
      assertFalse(entry.isExpired(), "Entry with future expiration should not be expired");
    }

    @Test
    @DisplayName("isExpired should return false when no expiration set")
    void isExpiredShouldReturnFalseWhenNoExpiration() {
      final KeyValueEntry entry = KeyValueEntry.builder("key", new byte[]{1}).build();
      assertFalse(entry.isExpired(), "Entry without expiration should not be expired");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("entries with same key and version should be equal")
    void sameKeyAndVersionShouldBeEqual() {
      final KeyValueEntry entry1 =
          KeyValueEntry.builder("key", new byte[]{1}).version(1).build();
      final KeyValueEntry entry2 =
          KeyValueEntry.builder("key", new byte[]{99}).version(1).build();

      assertEquals(entry1, entry2, "Entries with same key and version should be equal");
      assertEquals(
          entry1.hashCode(), entry2.hashCode(),
          "Entries with same key and version should have same hashCode");
    }

    @Test
    @DisplayName("entries with different keys should not be equal")
    void differentKeysShouldNotBeEqual() {
      final KeyValueEntry entry1 =
          KeyValueEntry.builder("key1", new byte[]{1}).build();
      final KeyValueEntry entry2 =
          KeyValueEntry.builder("key2", new byte[]{1}).build();
      assertNotEquals(entry1, entry2, "Entries with different keys should not be equal");
    }

    @Test
    @DisplayName("entries with different versions should not be equal")
    void differentVersionsShouldNotBeEqual() {
      final KeyValueEntry entry1 =
          KeyValueEntry.builder("key", new byte[]{1}).version(1).build();
      final KeyValueEntry entry2 =
          KeyValueEntry.builder("key", new byte[]{1}).version(2).build();
      assertNotEquals(entry1, entry2, "Entries with different versions should not be equal");
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      final KeyValueEntry entry = KeyValueEntry.builder("key", new byte[]{1}).build();
      assertNotEquals(null, entry, "Entry should not equal null");
    }

    @Test
    @DisplayName("should not equal different type")
    void shouldNotEqualDifferentType() {
      final KeyValueEntry entry = KeyValueEntry.builder("key", new byte[]{1}).build();
      assertNotEquals("key", entry, "Entry should not equal a String");
    }

    @Test
    @DisplayName("should equal itself")
    void shouldEqualItself() {
      final KeyValueEntry entry = KeyValueEntry.builder("key", new byte[]{1}).build();
      assertEquals(entry, entry, "Entry should equal itself");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain key and version")
    void toStringShouldContainKeyAndVersion() {
      final KeyValueEntry entry =
          KeyValueEntry.builder("myKey", new byte[]{1}).version(3).build();
      final String result = entry.toString();
      assertTrue(
          result.contains("myKey"),
          "toString should contain key: " + result);
      assertTrue(
          result.contains("3"),
          "toString should contain version: " + result);
    }

    @Test
    @DisplayName("toString should contain KeyValueEntry")
    void toStringShouldContainClassName() {
      final KeyValueEntry entry = KeyValueEntry.builder("key", new byte[]{}).build();
      final String result = entry.toString();
      assertTrue(
          result.contains("KeyValueEntry"),
          "toString should contain class name: " + result);
    }
  }
}
