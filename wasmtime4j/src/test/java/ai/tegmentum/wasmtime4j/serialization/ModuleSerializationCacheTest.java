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

package ai.tegmentum.wasmtime4j.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link ModuleSerializationCache} class.
 *
 * <p>ModuleSerializationCache provides content-addressed caching for serialized WebAssembly modules
 * with multi-level storage (memory, disk, distributed) using SHA-256 content addressing.
 */
@DisplayName("ModuleSerializationCache Tests")
class ModuleSerializationCacheTest {

  @TempDir Path tempDir;

  private ModuleSerializationCache cache;
  private CacheConfiguration cacheConfig;

  @BeforeEach
  void setUp() throws IOException {
    cacheConfig =
        new CacheConfiguration.Builder()
            .setMaxMemoryEntries(100)
            .setMemoryCacheTtl(Duration.ofHours(1))
            .enableDiskCache(tempDir)
            .setDiskCacheTtl(Duration.ofDays(1))
            .build();

    cache = new ModuleSerializationCache(cacheConfig);
  }

  @AfterEach
  void tearDown() {
    if (cache != null) {
      cache.close();
    }
  }

  private SerializedModuleMetadata createTestMetadata() {
    return new SerializedModuleMetadata.Builder()
        .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
        .setSerializedSize(100)
        .setOriginalSize(200)
        .setSha256Hash("testhash")
        .build();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create cache with configuration")
    void shouldCreateCacheWithConfiguration() throws IOException {
      final CacheConfiguration config =
          new CacheConfiguration.Builder()
              .setMaxMemoryEntries(50)
              .setMemoryCacheTtl(Duration.ofMinutes(30))
              .build();

      try (final ModuleSerializationCache newCache = new ModuleSerializationCache(config)) {
        assertNotNull(newCache);
      }
    }

    @Test
    @DisplayName("should throw on null configuration")
    void shouldThrowOnNullConfiguration() {
      assertThrows(NullPointerException.class, () -> new ModuleSerializationCache(null));
    }

    @Test
    @DisplayName("should create disk cache directory")
    @SuppressWarnings("try")
    void shouldCreateDiskCacheDirectory() throws IOException {
      final Path cacheDir = tempDir.resolve("new-cache-dir");
      final CacheConfiguration config =
          new CacheConfiguration.Builder().enableDiskCache(cacheDir).build();

      try (ModuleSerializationCache ignored = new ModuleSerializationCache(config)) {
        assertTrue(cacheDir.toFile().exists());
      }
    }
  }

  @Nested
  @DisplayName("Store Tests")
  class StoreTests {

    @Test
    @DisplayName("store should return content hash")
    void storeShouldReturnContentHash() throws IOException {
      final byte[] moduleData = "test module data".getBytes();
      final SerializedModuleMetadata metadata = createTestMetadata();

      final String hash = cache.store(moduleData, metadata);

      assertNotNull(hash);
      assertEquals(64, hash.length()); // SHA-256 hex = 64 chars
    }

    @Test
    @DisplayName("store should throw on null module data")
    void storeShouldThrowOnNullModuleData() {
      assertThrows(NullPointerException.class, () -> cache.store(null, createTestMetadata()));
    }

    @Test
    @DisplayName("store should throw on null metadata")
    void storeShouldThrowOnNullMetadata() {
      final byte[] moduleData = "test".getBytes();
      assertThrows(NullPointerException.class, () -> cache.store(moduleData, null));
    }

    @Test
    @DisplayName("store should return same hash for same data")
    void storeShouldReturnSameHashForSameData() throws IOException {
      final byte[] moduleData = "test module data".getBytes();
      final SerializedModuleMetadata metadata = createTestMetadata();

      final String hash1 = cache.store(moduleData, metadata);
      final String hash2 = cache.store(moduleData, metadata);

      assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("store should return different hash for different data")
    void storeShouldReturnDifferentHashForDifferentData() throws IOException {
      final SerializedModuleMetadata metadata = createTestMetadata();

      final String hash1 = cache.store("data1".getBytes(), metadata);
      final String hash2 = cache.store("data2".getBytes(), metadata);

      assertFalse(hash1.equals(hash2));
    }
  }

  @Nested
  @DisplayName("Retrieve Tests")
  class RetrieveTests {

    @Test
    @DisplayName("retrieve should return stored entry")
    void retrieveShouldReturnStoredEntry() throws IOException {
      final byte[] moduleData = "test module data".getBytes();
      final SerializedModuleMetadata metadata = createTestMetadata();

      final String hash = cache.store(moduleData, metadata);
      final Optional<ModuleSerializationCache.CacheEntry> entry = cache.retrieve(hash);

      assertTrue(entry.isPresent());
      assertNotNull(entry.get().getModuleData());
      assertNotNull(entry.get().getMetadata());
    }

    @Test
    @DisplayName("retrieve should throw on null hash")
    void retrieveShouldThrowOnNullHash() {
      assertThrows(NullPointerException.class, () -> cache.retrieve(null));
    }

    @Test
    @DisplayName("retrieve should return empty for non-existent hash")
    void retrieveShouldReturnEmptyForNonExistentHash() {
      final Optional<ModuleSerializationCache.CacheEntry> entry =
          cache.retrieve("nonexistenthash1234567890abcdef1234567890abcdef1234567890abcd");

      assertFalse(entry.isPresent());
    }
  }

  @Nested
  @DisplayName("Contains Tests")
  class ContainsTests {

    @Test
    @DisplayName("contains should return true for stored entry")
    void containsShouldReturnTrueForStoredEntry() throws IOException {
      final byte[] moduleData = "test".getBytes();
      final String hash = cache.store(moduleData, createTestMetadata());

      assertTrue(cache.contains(hash));
    }

    @Test
    @DisplayName("contains should return false for non-existent hash")
    void containsShouldReturnFalseForNonExistentHash() {
      assertFalse(cache.contains("nonexistenthash1234567890abcdef1234567890abcdef1234567890abcd"));
    }

    @Test
    @DisplayName("contains should throw on null hash")
    void containsShouldThrowOnNullHash() {
      assertThrows(NullPointerException.class, () -> cache.contains(null));
    }
  }

  @Nested
  @DisplayName("Remove Tests")
  class RemoveTests {

    @Test
    @DisplayName("remove should return true for existing entry")
    void removeShouldReturnTrueForExistingEntry() throws IOException {
      final byte[] moduleData = "test".getBytes();
      final String hash = cache.store(moduleData, createTestMetadata());

      assertTrue(cache.remove(hash));
      assertFalse(cache.contains(hash));
    }

    @Test
    @DisplayName("remove should return false for non-existent entry")
    void removeShouldReturnFalseForNonExistentEntry() {
      assertFalse(cache.remove("nonexistenthash1234567890abcdef1234567890abcdef1234567890abcd"));
    }

    @Test
    @DisplayName("remove should throw on null hash")
    void removeShouldThrowOnNullHash() {
      assertThrows(NullPointerException.class, () -> cache.remove(null));
    }
  }

  @Nested
  @DisplayName("Clear Tests")
  class ClearTests {

    @Test
    @DisplayName("clear should remove all entries")
    void clearShouldRemoveAllEntries() throws IOException {
      final String hash1 = cache.store("data1".getBytes(), createTestMetadata());
      final String hash2 = cache.store("data2".getBytes(), createTestMetadata());

      cache.clear();

      assertFalse(cache.contains(hash1));
      assertFalse(cache.contains(hash2));
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("getStatistics should return cache statistics")
    void getStatisticsShouldReturnCacheStatistics() throws IOException {
      final CacheStatistics stats = cache.getStatistics();

      assertNotNull(stats);
      assertEquals(0L, stats.getMisses());
      assertEquals(0L, stats.getEvictions());
    }

    @Test
    @DisplayName("getStatistics should track memory hits")
    void getStatisticsShouldTrackMemoryHits() throws IOException {
      final byte[] moduleData = "test".getBytes();
      final String hash = cache.store(moduleData, createTestMetadata());

      // First retrieve - memory hit
      cache.retrieve(hash);

      final CacheStatistics stats = cache.getStatistics();
      assertTrue(stats.getMemoryHits() > 0);
    }

    @Test
    @DisplayName("getStatistics should track misses")
    void getStatisticsShouldTrackMisses() {
      // Retrieve non-existent entry
      cache.retrieve("nonexistenthash1234567890abcdef1234567890abcdef1234567890abcd");

      final CacheStatistics stats = cache.getStatistics();
      assertTrue(stats.getMisses() > 0);
    }
  }

  @Nested
  @DisplayName("Cache Entry Tests")
  class CacheEntryTests {

    @Test
    @DisplayName("CacheEntry should store module data")
    void cacheEntryShouldStoreModuleData() throws IOException {
      final byte[] moduleData = "test module".getBytes();
      final String hash = cache.store(moduleData, createTestMetadata());

      final Optional<ModuleSerializationCache.CacheEntry> entry = cache.retrieve(hash);

      assertTrue(entry.isPresent());
      assertNotNull(entry.get().getModuleData());
      assertEquals(moduleData.length, entry.get().getModuleData().length);
    }

    @Test
    @DisplayName("CacheEntry should return defensive copy of module data")
    void cacheEntryShouldReturnDefensiveCopyOfModuleData() throws IOException {
      final byte[] moduleData = new byte[] {1, 2, 3, 4, 5};
      final String hash = cache.store(moduleData, createTestMetadata());

      final ModuleSerializationCache.CacheEntry entry = cache.retrieve(hash).get();
      final byte[] retrieved = entry.getModuleData();
      retrieved[0] = 99;

      assertEquals(1, entry.getModuleData()[0]);
    }

    @Test
    @DisplayName("CacheEntry should track creation time")
    void cacheEntryShouldTrackCreationTime() throws IOException {
      final byte[] moduleData = "test".getBytes();
      final String hash = cache.store(moduleData, createTestMetadata());

      final ModuleSerializationCache.CacheEntry entry = cache.retrieve(hash).get();

      assertNotNull(entry.getCreationTime());
    }

    @Test
    @DisplayName("CacheEntry should track last access time")
    void cacheEntryShouldTrackLastAccessTime() throws IOException {
      final byte[] moduleData = "test".getBytes();
      final String hash = cache.store(moduleData, createTestMetadata());

      final ModuleSerializationCache.CacheEntry entry = cache.retrieve(hash).get();

      assertNotNull(entry.getLastAccessTime());
    }

    @Test
    @DisplayName("CacheEntry should update access time on access")
    void cacheEntryShouldUpdateAccessTimeOnAccess() throws IOException {
      final byte[] moduleData = "test".getBytes();
      final String hash = cache.store(moduleData, createTestMetadata());

      final ModuleSerializationCache.CacheEntry entry1 = cache.retrieve(hash).get();
      entry1.updateAccessTime();

      // Access time should be updated
      assertNotNull(entry1.getLastAccessTime());
    }

    @Test
    @DisplayName("CacheEntry isExpired should return false for fresh entry")
    void cacheEntryIsExpiredShouldReturnFalseForFreshEntry() throws IOException {
      final byte[] moduleData = "test".getBytes();
      final String hash = cache.store(moduleData, createTestMetadata());

      final ModuleSerializationCache.CacheEntry entry = cache.retrieve(hash).get();

      assertFalse(entry.isExpired(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("CacheEntry isExpired should return false when TTL is null")
    void cacheEntryIsExpiredShouldReturnFalseWhenTtlIsNull() throws IOException {
      final byte[] moduleData = "test".getBytes();
      final String hash = cache.store(moduleData, createTestMetadata());

      final ModuleSerializationCache.CacheEntry entry = cache.retrieve(hash).get();

      assertFalse(entry.isExpired(null));
    }
  }

  @Nested
  @DisplayName("Cache Warm Tests")
  class CacheWarmTests {

    @Test
    @DisplayName("warmCache should throw on null modules")
    void warmCacheShouldThrowOnNullModules() {
      assertThrows(NullPointerException.class, () -> cache.warmCache(null, Map.of()));
    }

    @Test
    @DisplayName("warmCache should throw on null metadata")
    void warmCacheShouldThrowOnNullMetadata() {
      assertThrows(NullPointerException.class, () -> cache.warmCache(Map.of(), null));
    }
  }

  @Nested
  @DisplayName("Close Tests")
  class CloseTests {

    @Test
    @DisplayName("close should prevent further operations")
    void closeShouldPreventFurtherOperations() throws IOException {
      cache.close();

      assertThrows(
          IllegalStateException.class, () -> cache.store("test".getBytes(), createTestMetadata()));
    }

    @Test
    @DisplayName("close should be idempotent")
    void closeShouldBeIdempotent() {
      cache.close();
      cache.close(); // Should not throw
    }
  }

  @Nested
  @DisplayName("DistributedCacheConnector Interface Tests")
  class DistributedCacheConnectorInterfaceTests {

    @Test
    @DisplayName("DistributedCacheConnector should be an interface")
    void distributedCacheConnectorShouldBeAnInterface() {
      assertTrue(ModuleSerializationCache.DistributedCacheConnector.class.isInterface());
    }

    @Test
    @DisplayName("DistributedCacheConnector should extend AutoCloseable")
    void distributedCacheConnectorShouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(
              ModuleSerializationCache.DistributedCacheConnector.class));
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("ModuleSerializationCache should be final")
    void moduleSerializationCacheShouldBeFinal() {
      assertTrue(java.lang.reflect.Modifier.isFinal(ModuleSerializationCache.class.getModifiers()));
    }

    @Test
    @DisplayName("CacheEntry should be final")
    void cacheEntryShouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(
              ModuleSerializationCache.CacheEntry.class.getModifiers()));
    }

    @Test
    @DisplayName("ModuleSerializationCache should implement AutoCloseable")
    void moduleSerializationCacheShouldImplementAutoCloseable() {
      assertTrue(AutoCloseable.class.isAssignableFrom(ModuleSerializationCache.class));
    }
  }
}
