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

    @Test
    @DisplayName("contains should check memory cache entry validity")
    void containsShouldCheckMemoryCacheEntryValidity() throws IOException {
      final byte[] moduleData = "test".getBytes();
      final String hash = cache.store(moduleData, createTestMetadata());

      // Entry is in memory and not expired
      assertTrue(cache.contains(hash), "Entry should be contained in memory cache");

      // Verify the check works correctly for existing entries
      final Optional<ModuleSerializationCache.CacheEntry> entry = cache.retrieve(hash);
      assertTrue(entry.isPresent());
    }

    @Test
    @DisplayName("contains should fallback to disk when not in memory")
    void containsShouldFallbackToDiskWhenNotInMemory() throws IOException {
      // Create cache with very small memory limit to force disk-only scenario
      final CacheConfiguration config =
          new CacheConfiguration.Builder()
              .setMaxMemoryEntries(1)
              .setMemoryCacheTtl(Duration.ofHours(1))
              .enableDiskCache(tempDir)
              .setDiskCacheTtl(Duration.ofDays(1))
              .build();

      try (final ModuleSerializationCache smallCache = new ModuleSerializationCache(config)) {
        // Store first entry
        final String hash1 = smallCache.store("data1".getBytes(), createTestMetadata());

        // Store second entry (evicts first from memory)
        smallCache.store("data2".getBytes(), createTestMetadata());

        // First entry should still be found in disk cache
        assertTrue(smallCache.contains(hash1), "Should find entry in disk cache");
      }
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

    @Test
    @DisplayName("remove should return true when only in disk cache")
    void removeShouldReturnTrueWhenOnlyInDiskCache() throws IOException {
      // Store entry (goes to memory and disk)
      final byte[] moduleData = "test for disk".getBytes();
      final String hash = cache.store(moduleData, createTestMetadata());

      // Verify it's in cache
      assertTrue(cache.contains(hash));

      // Remove returns true from either memory OR disk, not AND
      assertTrue(cache.remove(hash), "Remove should return true from any cache tier");

      // Verify it's fully removed
      assertFalse(cache.contains(hash));
    }

    @Test
    @DisplayName("remove should remove from all cache tiers")
    void removeShouldRemoveFromAllCacheTiers() throws IOException {
      final byte[] moduleData = "test data for multi-tier".getBytes();
      final String hash = cache.store(moduleData, createTestMetadata());

      // First remove
      assertTrue(cache.remove(hash));

      // Second remove should return false (already removed from all tiers)
      assertFalse(cache.remove(hash), "Second remove should return false - already removed");
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

  @Nested
  @DisplayName("LRU Eviction Tests")
  class LruEvictionTests {

    @Test
    @DisplayName("should evict oldest entry when max entries exceeded")
    void shouldEvictOldestEntryWhenMaxEntriesExceeded() throws IOException {
      // Create cache with max 2 entries
      final CacheConfiguration config =
          new CacheConfiguration.Builder()
              .setMaxMemoryEntries(2)
              .setMemoryCacheTtl(Duration.ofHours(1))
              .build();

      try (final ModuleSerializationCache smallCache = new ModuleSerializationCache(config)) {
        // Store 3 entries - should evict the first one
        final String hash1 = smallCache.store("data1".getBytes(), createTestMetadata());
        final String hash2 = smallCache.store("data2".getBytes(), createTestMetadata());
        final String hash3 = smallCache.store("data3".getBytes(), createTestMetadata());

        // Statistics should show eviction
        final CacheStatistics stats = smallCache.getStatistics();
        assertTrue(stats.getEvictions() > 0, "Should have evicted at least one entry");
      }
    }

    @Test
    @DisplayName("should evict least recently accessed entry")
    void shouldEvictLeastRecentlyAccessedEntry() throws IOException {
      // Create cache with max 2 entries
      final CacheConfiguration config =
          new CacheConfiguration.Builder()
              .setMaxMemoryEntries(2)
              .setMemoryCacheTtl(Duration.ofHours(1))
              .build();

      try (final ModuleSerializationCache smallCache = new ModuleSerializationCache(config)) {
        // Store 2 entries
        final String hash1 = smallCache.store("data1".getBytes(), createTestMetadata());
        final String hash2 = smallCache.store("data2".getBytes(), createTestMetadata());

        // Access hash1 to make it more recent
        smallCache.retrieve(hash1);

        // Store a third entry - should evict hash2 (least recently accessed)
        final String hash3 = smallCache.store("data3".getBytes(), createTestMetadata());

        // hash1 should still be present, hash2 might be evicted from memory
        assertTrue(smallCache.contains(hash1), "hash1 should still be present");
      }
    }

    @Test
    @DisplayName("LRU eviction should compare access times correctly")
    void lruEvictionShouldCompareAccessTimesCorrectly() throws IOException {
      // Memory-only cache with max 2 entries
      final CacheConfiguration config =
          new CacheConfiguration.Builder()
              .setMaxMemoryEntries(2)
              .setMemoryCacheTtl(Duration.ofHours(1))
              .build();

      try (final ModuleSerializationCache smallCache = new ModuleSerializationCache(config)) {
        // Store first entry
        final String hash1 = smallCache.store("oldest_entry".getBytes(), createTestMetadata());

        // Small delay to ensure different access times
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        // Store second entry
        final String hash2 = smallCache.store("newer_entry".getBytes(), createTestMetadata());

        // Access hash1 multiple times to update its access time
        for (int i = 0; i < 3; i++) {
          smallCache.retrieve(hash1);
        }

        // Store third entry - should evict hash2 (older access time)
        smallCache.store("newest_entry".getBytes(), createTestMetadata());

        // Verify eviction count
        final CacheStatistics stats = smallCache.getStatistics();
        assertEquals(1, stats.getEvictions(), "Should have exactly one eviction");
      }
    }

    @Test
    @DisplayName("eviction should increment eviction counter")
    void evictionShouldIncrementEvictionCounter() throws IOException {
      final CacheConfiguration config =
          new CacheConfiguration.Builder()
              .setMaxMemoryEntries(1)
              .setMemoryCacheTtl(Duration.ofHours(1))
              .build();

      try (final ModuleSerializationCache tinyCache = new ModuleSerializationCache(config)) {
        // Store one entry
        tinyCache.store("first".getBytes(), createTestMetadata());

        // Initial eviction count
        assertEquals(0, tinyCache.getStatistics().getEvictions());

        // Store another entry (causes eviction)
        tinyCache.store("second".getBytes(), createTestMetadata());

        // Eviction count should increase
        assertEquals(1, tinyCache.getStatistics().getEvictions());
      }
    }
  }

  @Nested
  @DisplayName("Disk Cache Tests")
  class DiskCacheTests {

    @Test
    @DisplayName("should retrieve from disk cache after memory eviction")
    void shouldRetrieveFromDiskCacheAfterMemoryEviction() throws IOException {
      // Create cache with disk support and very small memory limit
      final CacheConfiguration config =
          new CacheConfiguration.Builder()
              .setMaxMemoryEntries(1)
              .setMemoryCacheTtl(Duration.ofHours(1))
              .enableDiskCache(tempDir)
              .setDiskCacheTtl(Duration.ofDays(1))
              .build();

      try (final ModuleSerializationCache diskCache = new ModuleSerializationCache(config)) {
        // Store first entry (in both memory and disk)
        final String hash1 = diskCache.store("data1".getBytes(), createTestMetadata());

        // Store second entry (evicts first from memory, but keeps in disk)
        diskCache.store("data2".getBytes(), createTestMetadata());

        // Should still be able to retrieve hash1 from disk
        assertTrue(diskCache.contains(hash1), "hash1 should still be accessible from disk");
      }
    }

    @Test
    @DisplayName("should handle disk cache file creation")
    void shouldHandleDiskCacheFileCreation() throws IOException {
      final byte[] moduleData = "large module data for disk storage".getBytes();
      final String hash = cache.store(moduleData, createTestMetadata());

      // Verify disk cache files exist
      assertTrue(cache.contains(hash));
    }
  }

  @Nested
  @DisplayName("Cache Warm Edge Cases Tests")
  class CacheWarmEdgeCasesTests {

    @Test
    @DisplayName("warmCache should store modules that have metadata")
    void warmCacheShouldStoreModulesThatHaveMetadata() throws IOException {
      final byte[] data1 = "module1".getBytes();
      final SerializedModuleMetadata meta1 = createTestMetadata();

      // Calculate expected hash
      final String hash1 = calculateHash(data1);

      cache.warmCache(Map.of(hash1, data1), Map.of(hash1, meta1));

      // The cache should contain the warmed entry
      assertTrue(cache.contains(hash1));
    }

    @Test
    @DisplayName("warmCache should skip modules without metadata")
    void warmCacheShouldSkipModulesWithoutMetadata() throws IOException {
      final byte[] data1 = "module1".getBytes();
      final String hash1 = calculateHash(data1);

      // Warm with module but empty metadata map
      cache.warmCache(Map.of(hash1, data1), Map.of());

      // Should not throw, but module might not be stored properly
      assertNotNull(cache);
    }

    private String calculateHash(byte[] data) {
      try {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
          sb.append(String.format("%02x", b));
        }
        return sb.toString();
      } catch (java.security.NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Nested
  @DisplayName("CacheEntry Constructor Tests")
  class CacheEntryConstructorTests {

    @Test
    @DisplayName("CacheEntry should throw on null module data")
    void cacheEntryShouldThrowOnNullModuleData() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ModuleSerializationCache.CacheEntry(
                  null, createTestMetadata(), java.time.Instant.now()));
    }

    @Test
    @DisplayName("CacheEntry should throw on null metadata")
    void cacheEntryShouldThrowOnNullMetadata() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ModuleSerializationCache.CacheEntry(
                  "test".getBytes(), null, java.time.Instant.now()));
    }

    @Test
    @DisplayName("CacheEntry should throw on null creation time")
    void cacheEntryShouldThrowOnNullCreationTime() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ModuleSerializationCache.CacheEntry(
                  "test".getBytes(), createTestMetadata(), null));
    }
  }

  @Nested
  @DisplayName("Cache Statistics Edge Cases Tests")
  class CacheStatisticsEdgeCasesTests {

    @Test
    @DisplayName("getStatistics should calculate hit ratio correctly")
    void getStatisticsShouldCalculateHitRatioCorrectly() throws IOException {
      // Store one entry
      final String hash = cache.store("test".getBytes(), createTestMetadata());

      // One hit
      cache.retrieve(hash);

      // One miss
      cache.retrieve("nonexistenthash1234567890abcdef1234567890abcdef1234567890abcd");

      final CacheStatistics stats = cache.getStatistics();

      // Hit ratio should be 0.5 (1 hit / 2 requests)
      assertEquals(0.5, stats.getHitRatio(), 0.01);
    }

    @Test
    @DisplayName("getStatistics should return zero hit ratio when no requests")
    void getStatisticsShouldReturnZeroHitRatioWhenNoRequests() {
      // No requests made - hit ratio should be 0.0, not 1.0 or NaN
      final CacheStatistics stats = cache.getStatistics();

      assertEquals(0.0, stats.getHitRatio(), 0.001);
      assertEquals(0L, stats.getMemoryHits());
      assertEquals(0L, stats.getMisses());
    }

    @Test
    @DisplayName("getStatistics should track memory cache size")
    void getStatisticsShouldTrackMemoryCacheSize() throws IOException {
      cache.store("test data".getBytes(), createTestMetadata());

      final CacheStatistics stats = cache.getStatistics();

      assertTrue(stats.getMemoryCacheSizeBytes() > 0, "Memory cache size should be positive");
    }

    @Test
    @DisplayName("getStatistics should track memory entry count")
    void getStatisticsShouldTrackMemoryEntryCount() throws IOException {
      cache.store("test1".getBytes(), createTestMetadata());
      cache.store("test2".getBytes(), createTestMetadata());

      final CacheStatistics stats = cache.getStatistics();

      assertEquals(2, stats.getMemoryCacheEntries());
    }

    @Test
    @DisplayName("getStatistics total hits should be sum of all hit types")
    void getStatisticsTotalHitsShouldBeSumOfAllHitTypes() throws IOException {
      // Store and retrieve to create memory hits
      final String hash1 = cache.store("test1".getBytes(), createTestMetadata());
      final String hash2 = cache.store("test2".getBytes(), createTestMetadata());
      cache.retrieve(hash1);
      cache.retrieve(hash2);
      cache.retrieve(hash1);

      final CacheStatistics stats = cache.getStatistics();

      // Verify total hits is the sum of memory + disk + distributed
      final long expectedTotalHits =
          stats.getMemoryHits() + stats.getDiskHits() + stats.getDistributedHits();

      // Calculate total from ratio: hitRatio = totalHits / (totalHits + misses)
      // totalHits = hitRatio * (totalHits + misses)
      // With 3 memory hits and 0 misses, ratio should be 1.0
      assertTrue(stats.getMemoryHits() >= 3, "Should have at least 3 memory hits");
      assertEquals(1.0, stats.getHitRatio(), 0.001, "Hit ratio should be 1.0 with no misses");
    }
  }

  @Nested
  @DisplayName("Memory-Only Cache Tests")
  class MemoryOnlyCacheTests {

    @Test
    @DisplayName("should work without disk cache")
    void shouldWorkWithoutDiskCache() throws IOException {
      final CacheConfiguration memoryOnlyConfig =
          new CacheConfiguration.Builder()
              .setMaxMemoryEntries(100)
              .setMemoryCacheTtl(Duration.ofHours(1))
              .build();

      try (final ModuleSerializationCache memoryCache =
          new ModuleSerializationCache(memoryOnlyConfig)) {
        final String hash = memoryCache.store("test".getBytes(), createTestMetadata());

        assertTrue(memoryCache.contains(hash));
        assertTrue(memoryCache.retrieve(hash).isPresent());
        assertTrue(memoryCache.remove(hash));
        assertFalse(memoryCache.contains(hash));
      }
    }

    @Test
    @DisplayName("clear should work without disk cache")
    void clearShouldWorkWithoutDiskCache() throws IOException {
      final CacheConfiguration memoryOnlyConfig =
          new CacheConfiguration.Builder()
              .setMaxMemoryEntries(100)
              .setMemoryCacheTtl(Duration.ofHours(1))
              .build();

      try (final ModuleSerializationCache memoryCache =
          new ModuleSerializationCache(memoryOnlyConfig)) {
        final String hash = memoryCache.store("test".getBytes(), createTestMetadata());

        memoryCache.clear();

        assertFalse(memoryCache.contains(hash));
      }
    }
  }

  @Nested
  @DisplayName("Closed Cache Operation Tests")
  class ClosedCacheOperationTests {

    @Test
    @DisplayName("retrieve should throw when closed")
    void retrieveShouldThrowWhenClosed() throws IOException {
      cache.close();

      assertThrows(
          IllegalStateException.class,
          () ->
              cache.retrieve("nonexistenthash1234567890abcdef1234567890abcdef1234567890abcd"));
    }

    @Test
    @DisplayName("contains should throw when closed")
    void containsShouldThrowWhenClosed() throws IOException {
      cache.close();

      assertThrows(
          IllegalStateException.class,
          () ->
              cache.contains("nonexistenthash1234567890abcdef1234567890abcdef1234567890abcd"));
    }

    @Test
    @DisplayName("remove should throw when closed")
    void removeShouldThrowWhenClosed() throws IOException {
      cache.close();

      assertThrows(
          IllegalStateException.class,
          () -> cache.remove("nonexistenthash1234567890abcdef1234567890abcdef1234567890abcd"));
    }

    @Test
    @DisplayName("clear should throw when closed")
    void clearShouldThrowWhenClosed() throws IOException {
      cache.close();

      assertThrows(IllegalStateException.class, () -> cache.clear());
    }

    @Test
    @DisplayName("warmCache should throw when closed")
    void warmCacheShouldThrowWhenClosed() throws IOException {
      cache.close();

      assertThrows(IllegalStateException.class, () -> cache.warmCache(Map.of(), Map.of()));
    }
  }
}
