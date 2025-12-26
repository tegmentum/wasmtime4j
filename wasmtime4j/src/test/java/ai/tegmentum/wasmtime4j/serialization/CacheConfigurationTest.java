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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CacheConfiguration} class.
 *
 * <p>CacheConfiguration defines the configuration parameters for multi-level caching including
 * memory, disk, and distributed cache settings with appropriate defaults for production use.
 */
@DisplayName("CacheConfiguration Tests")
class CacheConfigurationTest {

  @Nested
  @DisplayName("Default Configuration Tests")
  class DefaultConfigurationTests {

    @Test
    @DisplayName("should create default configuration with createDefault")
    void shouldCreateDefaultConfigurationWithCreateDefault() {
      final CacheConfiguration config = CacheConfiguration.createDefault();

      assertNotNull(config);
      assertEquals(1000, config.getMaxMemoryEntries());
      assertEquals(Duration.ofHours(1), config.getMemoryCacheTtl());
      assertEquals(128 * 1024 * 1024, config.getMaxMemoryUsageBytes());
      assertFalse(config.isDiskCacheEnabled());
      assertFalse(config.isDistributedCacheEnabled());
    }

    @Test
    @DisplayName("should create configuration with builder defaults")
    void shouldCreateConfigurationWithBuilderDefaults() {
      final CacheConfiguration config = new CacheConfiguration.Builder().build();

      assertEquals(1000, config.getMaxMemoryEntries());
      assertEquals(Duration.ofHours(1), config.getMemoryCacheTtl());
      assertEquals(128 * 1024 * 1024, config.getMaxMemoryUsageBytes());
      assertFalse(config.isDiskCacheEnabled());
      assertFalse(config.isDistributedCacheEnabled());
      assertFalse(config.isPreloadCacheOnStartup());
      assertEquals(Duration.ofMinutes(15), config.getMaintenanceInterval());
      assertEquals(1, config.getMaintenanceThreads());
      assertFalse(config.isEncryptCacheEntries());
      assertEquals("AES/GCM/NoPadding", config.getEncryptionAlgorithm());
    }
  }

  @Nested
  @DisplayName("Production Configuration Tests")
  class ProductionConfigurationTests {

    @Test
    @DisplayName("should create production configuration")
    void shouldCreateProductionConfiguration() {
      final CacheConfiguration config = CacheConfiguration.createProduction();

      assertNotNull(config);
      assertEquals(10_000, config.getMaxMemoryEntries());
      assertEquals(256 * 1024 * 1024, config.getMaxMemoryUsageBytes());
      assertEquals(Duration.ofHours(2), config.getMemoryCacheTtl());
      assertTrue(config.isDiskCacheEnabled());
      assertEquals(Duration.ofDays(7), config.getDiskCacheTtl());
      assertEquals(2L * 1024 * 1024 * 1024, config.getMaxDiskUsageBytes());
      assertEquals(Duration.ofMinutes(10), config.getMaintenanceInterval());
    }
  }

  @Nested
  @DisplayName("Memory-Only Configuration Tests")
  class MemoryOnlyConfigurationTests {

    @Test
    @DisplayName("should create memory-only configuration")
    void shouldCreateMemoryOnlyConfiguration() {
      final CacheConfiguration config = CacheConfiguration.createMemoryOnly();

      assertNotNull(config);
      assertEquals(1000, config.getMaxMemoryEntries());
      assertEquals(64 * 1024 * 1024, config.getMaxMemoryUsageBytes());
      assertEquals(Duration.ofMinutes(30), config.getMemoryCacheTtl());
      assertFalse(config.isDiskCacheEnabled());
      assertFalse(config.isDistributedCacheEnabled());
    }
  }

  @Nested
  @DisplayName("High Performance Configuration Tests")
  class HighPerformanceConfigurationTests {

    @Test
    @DisplayName("should create high performance configuration")
    void shouldCreateHighPerformanceConfiguration() {
      final CacheConfiguration config = CacheConfiguration.createHighPerformance();

      assertNotNull(config);
      assertEquals(50_000, config.getMaxMemoryEntries());
      assertEquals(1024 * 1024 * 1024, config.getMaxMemoryUsageBytes());
      assertEquals(Duration.ofHours(6), config.getMemoryCacheTtl());
      assertTrue(config.isDiskCacheEnabled());
      assertEquals(Duration.ofDays(30), config.getDiskCacheTtl());
      assertEquals(10L * 1024 * 1024 * 1024, config.getMaxDiskUsageBytes());
      assertEquals(Duration.ofMinutes(5), config.getMaintenanceInterval());
      assertEquals(2, config.getMaintenanceThreads());
      assertTrue(config.isPreloadCacheOnStartup());
    }
  }

  @Nested
  @DisplayName("Memory Cache Builder Tests")
  class MemoryCacheBuilderTests {

    @Test
    @DisplayName("should set max memory entries")
    void shouldSetMaxMemoryEntries() {
      final CacheConfiguration config =
          new CacheConfiguration.Builder().setMaxMemoryEntries(5000).build();

      assertEquals(5000, config.getMaxMemoryEntries());
    }

    @Test
    @DisplayName("should set max memory usage")
    void shouldSetMaxMemoryUsage() {
      final CacheConfiguration config =
          new CacheConfiguration.Builder().setMaxMemoryUsage(512 * 1024 * 1024).build();

      assertEquals(512 * 1024 * 1024, config.getMaxMemoryUsageBytes());
    }

    @Test
    @DisplayName("should set memory cache TTL")
    void shouldSetMemoryCacheTtl() {
      final CacheConfiguration config =
          new CacheConfiguration.Builder().setMemoryCacheTtl(Duration.ofMinutes(45)).build();

      assertEquals(Duration.ofMinutes(45), config.getMemoryCacheTtl());
    }
  }

  @Nested
  @DisplayName("Disk Cache Builder Tests")
  class DiskCacheBuilderTests {

    @Test
    @DisplayName("should enable disk cache")
    void shouldEnableDiskCache() {
      final Path cacheDir = Paths.get("/tmp/cache");
      final CacheConfiguration config =
          new CacheConfiguration.Builder().enableDiskCache(cacheDir).build();

      assertTrue(config.isDiskCacheEnabled());
      assertEquals(cacheDir, config.getDiskCacheDirectory());
    }

    @Test
    @DisplayName("should disable disk cache")
    void shouldDisableDiskCache() {
      final Path cacheDir = Paths.get("/tmp/cache");
      final CacheConfiguration config =
          new CacheConfiguration.Builder().enableDiskCache(cacheDir).disableDiskCache().build();

      assertFalse(config.isDiskCacheEnabled());
      assertNull(config.getDiskCacheDirectory());
    }

    @Test
    @DisplayName("should set disk cache TTL")
    void shouldSetDiskCacheTtl() {
      final CacheConfiguration config =
          new CacheConfiguration.Builder()
              .enableDiskCache(Paths.get("/tmp/cache"))
              .setDiskCacheTtl(Duration.ofDays(14))
              .build();

      assertEquals(Duration.ofDays(14), config.getDiskCacheTtl());
    }

    @Test
    @DisplayName("should set max disk usage")
    void shouldSetMaxDiskUsage() {
      final CacheConfiguration config =
          new CacheConfiguration.Builder().setMaxDiskUsage(5L * 1024 * 1024 * 1024).build();

      assertEquals(5L * 1024 * 1024 * 1024, config.getMaxDiskUsageBytes());
    }
  }

  @Nested
  @DisplayName("Distributed Cache Builder Tests")
  class DistributedCacheBuilderTests {

    @Test
    @DisplayName("should enable distributed cache")
    void shouldEnableDistributedCache() {
      final ModuleSerializationCache.DistributedCacheConnector connector = createMockConnector();

      final CacheConfiguration config =
          new CacheConfiguration.Builder().enableDistributedCache(connector).build();

      assertTrue(config.isDistributedCacheEnabled());
      assertNotNull(config.getDistributedCacheConnector());
    }

    @Test
    @DisplayName("should disable distributed cache")
    void shouldDisableDistributedCache() {
      final ModuleSerializationCache.DistributedCacheConnector connector = createMockConnector();

      final CacheConfiguration config =
          new CacheConfiguration.Builder()
              .enableDistributedCache(connector)
              .disableDistributedCache()
              .build();

      assertFalse(config.isDistributedCacheEnabled());
      assertNull(config.getDistributedCacheConnector());
    }

    @Test
    @DisplayName("should set distributed cache TTL")
    void shouldSetDistributedCacheTtl() {
      final CacheConfiguration config =
          new CacheConfiguration.Builder().setDistributedCacheTtl(Duration.ofHours(12)).build();

      assertEquals(Duration.ofHours(12), config.getDistributedCacheTtl());
    }

    private ModuleSerializationCache.DistributedCacheConnector createMockConnector() {
      return new ModuleSerializationCache.DistributedCacheConnector() {
        @Override
        public boolean isAvailable() {
          return true;
        }

        @Override
        public void store(final String key, final ModuleSerializationCache.CacheEntry entry)
            throws java.io.IOException {}

        @Override
        public java.util.Optional<ModuleSerializationCache.CacheEntry> retrieve(final String key)
            throws java.io.IOException {
          return java.util.Optional.empty();
        }

        @Override
        public boolean contains(final String key) throws java.io.IOException {
          return false;
        }

        @Override
        public boolean remove(final String key) throws java.io.IOException {
          return false;
        }

        @Override
        public void clear() throws java.io.IOException {}

        @Override
        public void close() throws java.io.IOException {}
      };
    }
  }

  @Nested
  @DisplayName("Performance Configuration Builder Tests")
  class PerformanceConfigurationBuilderTests {

    @Test
    @DisplayName("should enable cache preloading")
    void shouldEnableCachePreloading() {
      final CacheConfiguration config =
          new CacheConfiguration.Builder().enableCachePreloading().build();

      assertTrue(config.isPreloadCacheOnStartup());
    }

    @Test
    @DisplayName("should disable cache preloading")
    void shouldDisableCachePreloading() {
      final CacheConfiguration config =
          new CacheConfiguration.Builder().enableCachePreloading().disableCachePreloading().build();

      assertFalse(config.isPreloadCacheOnStartup());
    }

    @Test
    @DisplayName("should set maintenance interval")
    void shouldSetMaintenanceInterval() {
      final CacheConfiguration config =
          new CacheConfiguration.Builder().setMaintenanceInterval(Duration.ofMinutes(5)).build();

      assertEquals(Duration.ofMinutes(5), config.getMaintenanceInterval());
    }

    @Test
    @DisplayName("should set maintenance threads")
    void shouldSetMaintenanceThreads() {
      final CacheConfiguration config =
          new CacheConfiguration.Builder().setMaintenanceThreads(4).build();

      assertEquals(4, config.getMaintenanceThreads());
    }
  }

  @Nested
  @DisplayName("Security Configuration Builder Tests")
  class SecurityConfigurationBuilderTests {

    @Test
    @DisplayName("should enable encryption")
    void shouldEnableEncryption() {
      final CacheConfiguration config =
          new CacheConfiguration.Builder().enableEncryption("AES/CBC/PKCS5Padding").build();

      assertTrue(config.isEncryptCacheEntries());
      assertEquals("AES/CBC/PKCS5Padding", config.getEncryptionAlgorithm());
    }

    @Test
    @DisplayName("should disable encryption")
    void shouldDisableEncryption() {
      final CacheConfiguration config =
          new CacheConfiguration.Builder()
              .enableEncryption("AES/CBC/PKCS5Padding")
              .disableEncryption()
              .build();

      assertFalse(config.isEncryptCacheEntries());
    }
  }

  @Nested
  @DisplayName("Builder Validation Tests")
  class BuilderValidationTests {

    @Test
    @DisplayName("setMaxMemoryEntries should throw on non-positive value")
    void setMaxMemoryEntriesShouldThrowOnNonPositiveValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new CacheConfiguration.Builder().setMaxMemoryEntries(0));
    }

    @Test
    @DisplayName("setMaxMemoryUsage should throw on non-positive value")
    void setMaxMemoryUsageShouldThrowOnNonPositiveValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new CacheConfiguration.Builder().setMaxMemoryUsage(0));
    }

    @Test
    @DisplayName("setMemoryCacheTtl should throw on null")
    void setMemoryCacheTtlShouldThrowOnNull() {
      assertThrows(
          NullPointerException.class,
          () -> new CacheConfiguration.Builder().setMemoryCacheTtl(null));
    }

    @Test
    @DisplayName("enableDiskCache should throw on null directory")
    void enableDiskCacheShouldThrowOnNullDirectory() {
      assertThrows(
          NullPointerException.class, () -> new CacheConfiguration.Builder().enableDiskCache(null));
    }

    @Test
    @DisplayName("setMaxDiskUsage should throw on non-positive value")
    void setMaxDiskUsageShouldThrowOnNonPositiveValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new CacheConfiguration.Builder().setMaxDiskUsage(0));
    }

    @Test
    @DisplayName("enableDistributedCache should throw on null connector")
    void enableDistributedCacheShouldThrowOnNullConnector() {
      assertThrows(
          NullPointerException.class,
          () -> new CacheConfiguration.Builder().enableDistributedCache(null));
    }

    @Test
    @DisplayName("setDistributedCacheTtl should throw on null")
    void setDistributedCacheTtlShouldThrowOnNull() {
      assertThrows(
          NullPointerException.class,
          () -> new CacheConfiguration.Builder().setDistributedCacheTtl(null));
    }

    @Test
    @DisplayName("setMaintenanceInterval should throw on null")
    void setMaintenanceIntervalShouldThrowOnNull() {
      assertThrows(
          NullPointerException.class,
          () -> new CacheConfiguration.Builder().setMaintenanceInterval(null));
    }

    @Test
    @DisplayName("setMaintenanceThreads should throw on non-positive value")
    void setMaintenanceThreadsShouldThrowOnNonPositiveValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new CacheConfiguration.Builder().setMaintenanceThreads(0));
    }

    @Test
    @DisplayName("enableEncryption should throw on null algorithm")
    void enableEncryptionShouldThrowOnNullAlgorithm() {
      assertThrows(
          NullPointerException.class,
          () -> new CacheConfiguration.Builder().enableEncryption(null));
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string for default config")
    void toStringShouldReturnFormattedStringForDefaultConfig() {
      final CacheConfiguration config = CacheConfiguration.createDefault();
      final String result = config.toString();

      assertNotNull(result);
      assertTrue(result.contains("CacheConfiguration"));
      assertTrue(result.contains("1000 entries"));
      assertTrue(result.contains("MB"));
      assertTrue(result.contains("disabled"));
    }

    @Test
    @DisplayName("toString should show disk cache when enabled")
    void toStringShouldShowDiskCacheWhenEnabled() {
      final CacheConfiguration config = CacheConfiguration.createProduction();
      final String result = config.toString();

      assertNotNull(result);
      assertTrue(result.contains("wasmtime4j-cache"));
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("CacheConfiguration should be final")
    void cacheConfigurationShouldBeFinal() {
      assertTrue(java.lang.reflect.Modifier.isFinal(CacheConfiguration.class.getModifiers()));
    }

    @Test
    @DisplayName("Builder should be final")
    void builderShouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(CacheConfiguration.Builder.class.getModifiers()));
    }
  }
}
