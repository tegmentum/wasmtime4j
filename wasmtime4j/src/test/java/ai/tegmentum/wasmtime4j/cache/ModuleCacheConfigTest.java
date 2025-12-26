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

package ai.tegmentum.wasmtime4j.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModuleCacheConfig} class.
 *
 * <p>ModuleCacheConfig provides configuration for the WebAssembly module cache.
 */
@DisplayName("ModuleCacheConfig Tests")
class ModuleCacheConfigTest {

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("should create builder via static method")
    void shouldCreateBuilderViaStaticMethod() {
      final ModuleCacheConfig.Builder builder = ModuleCacheConfig.builder();
      assertNotNull(builder, "Builder should not be null");
    }

    @Test
    @DisplayName("should build with default values")
    void shouldBuildWithDefaultValues() {
      final ModuleCacheConfig config = ModuleCacheConfig.builder().build();
      assertNotNull(config, "Config should not be null");
      assertEquals(
          Paths.get("wasmtime4j_cache"), config.getCacheDir(), "Should have default cache dir");
      assertEquals(
          ModuleCacheConfig.DEFAULT_MAX_CACHE_SIZE,
          config.getMaxCacheSize(),
          "Should have default max cache size");
      assertEquals(
          ModuleCacheConfig.DEFAULT_MAX_ENTRIES,
          config.getMaxEntries(),
          "Should have default max entries");
      assertTrue(config.isCompressionEnabled(), "Compression should be enabled by default");
      assertEquals(
          ModuleCacheConfig.DEFAULT_COMPRESSION_LEVEL,
          config.getCompressionLevel(),
          "Should have default compression level");
    }

    @Test
    @DisplayName("should build with custom cache directory path")
    void shouldBuildWithCustomCacheDirectoryPath() {
      final Path customPath = Paths.get("/tmp/custom_cache");
      final ModuleCacheConfig config = ModuleCacheConfig.builder().cacheDir(customPath).build();
      assertEquals(customPath, config.getCacheDir(), "Cache dir should match");
    }

    @Test
    @DisplayName("should build with custom cache directory string")
    void shouldBuildWithCustomCacheDirectoryString() {
      final ModuleCacheConfig config =
          ModuleCacheConfig.builder().cacheDir("/tmp/custom_cache").build();
      assertEquals(Paths.get("/tmp/custom_cache"), config.getCacheDir(), "Cache dir should match");
    }

    @Test
    @DisplayName("should build with custom max cache size")
    void shouldBuildWithCustomMaxCacheSize() {
      final ModuleCacheConfig config =
          ModuleCacheConfig.builder().maxCacheSize(1024L * 1024L).build();
      assertEquals(1024L * 1024L, config.getMaxCacheSize(), "Max cache size should match");
    }

    @Test
    @DisplayName("should build with custom max entries")
    void shouldBuildWithCustomMaxEntries() {
      final ModuleCacheConfig config = ModuleCacheConfig.builder().maxEntries(500).build();
      assertEquals(500, config.getMaxEntries(), "Max entries should match");
    }

    @Test
    @DisplayName("should build with compression disabled")
    void shouldBuildWithCompressionDisabled() {
      final ModuleCacheConfig config =
          ModuleCacheConfig.builder().compressionEnabled(false).build();
      assertFalse(config.isCompressionEnabled(), "Compression should be disabled");
    }

    @Test
    @DisplayName("should build with custom compression level")
    void shouldBuildWithCustomCompressionLevel() {
      final ModuleCacheConfig config = ModuleCacheConfig.builder().compressionLevel(15).build();
      assertEquals(15, config.getCompressionLevel(), "Compression level should match");
    }

    @Test
    @DisplayName("should throw on null cache directory path")
    void shouldThrowOnNullCacheDirectoryPath() {
      assertThrows(
          NullPointerException.class,
          () -> ModuleCacheConfig.builder().cacheDir((Path) null),
          "Should throw on null cache dir");
    }

    @Test
    @DisplayName("should throw on null cache directory string")
    void shouldThrowOnNullCacheDirectoryString() {
      assertThrows(
          NullPointerException.class,
          () -> ModuleCacheConfig.builder().cacheDir((String) null),
          "Should throw on null cache dir string");
    }

    @Test
    @DisplayName("should throw on negative max cache size")
    void shouldThrowOnNegativeMaxCacheSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ModuleCacheConfig.builder().maxCacheSize(-1),
          "Should throw on negative max cache size");
    }

    @Test
    @DisplayName("should throw on non-positive max entries")
    void shouldThrowOnNonPositiveMaxEntries() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ModuleCacheConfig.builder().maxEntries(0),
          "Should throw on zero max entries");
    }

    @Test
    @DisplayName("should throw on compression level below 1")
    void shouldThrowOnCompressionLevelBelow1() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ModuleCacheConfig.builder().compressionLevel(0),
          "Should throw on compression level below 1");
    }

    @Test
    @DisplayName("should throw on compression level above 22")
    void shouldThrowOnCompressionLevelAbove22() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ModuleCacheConfig.builder().compressionLevel(23),
          "Should throw on compression level above 22");
    }
  }

  @Nested
  @DisplayName("Default Config Tests")
  class DefaultConfigTests {

    @Test
    @DisplayName("should create default config via static method")
    void shouldCreateDefaultConfigViaStaticMethod() {
      final ModuleCacheConfig config = ModuleCacheConfig.defaultConfig();
      assertNotNull(config, "Default config should not be null");
    }

    @Test
    @DisplayName("DEFAULT_MAX_CACHE_SIZE should be 512 MB")
    void defaultMaxCacheSizeShouldBe512MB() {
      assertEquals(
          512L * 1024L * 1024L,
          ModuleCacheConfig.DEFAULT_MAX_CACHE_SIZE,
          "DEFAULT_MAX_CACHE_SIZE should be 512 MB");
    }

    @Test
    @DisplayName("DEFAULT_MAX_ENTRIES should be 1000")
    void defaultMaxEntriesShouldBe1000() {
      assertEquals(
          1000, ModuleCacheConfig.DEFAULT_MAX_ENTRIES, "DEFAULT_MAX_ENTRIES should be 1000");
    }

    @Test
    @DisplayName("DEFAULT_COMPRESSION_LEVEL should be 6")
    void defaultCompressionLevelShouldBe6() {
      assertEquals(
          6, ModuleCacheConfig.DEFAULT_COMPRESSION_LEVEL, "DEFAULT_COMPRESSION_LEVEL should be 6");
    }

    @Test
    @DisplayName("DEFAULT_CACHE_DIR should be wasmtime4j_cache")
    void defaultCacheDirShouldBeWasmtime4jCache() {
      assertEquals(
          "wasmtime4j_cache",
          ModuleCacheConfig.DEFAULT_CACHE_DIR,
          "DEFAULT_CACHE_DIR should be wasmtime4j_cache");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final ModuleCacheConfig config = ModuleCacheConfig.builder().build();
      assertEquals(config, config, "Config should be equal to itself");
    }

    @Test
    @DisplayName("should be equal to config with same values")
    void shouldBeEqualToConfigWithSameValues() {
      final ModuleCacheConfig config1 =
          ModuleCacheConfig.builder()
              .cacheDir("/tmp/test")
              .maxCacheSize(1000)
              .maxEntries(100)
              .build();
      final ModuleCacheConfig config2 =
          ModuleCacheConfig.builder()
              .cacheDir("/tmp/test")
              .maxCacheSize(1000)
              .maxEntries(100)
              .build();
      assertEquals(config1, config2, "Configs with same values should be equal");
    }

    @Test
    @DisplayName("should not be equal to config with different values")
    void shouldNotBeEqualToConfigWithDifferentValues() {
      final ModuleCacheConfig config1 = ModuleCacheConfig.builder().maxCacheSize(1000).build();
      final ModuleCacheConfig config2 = ModuleCacheConfig.builder().maxCacheSize(2000).build();
      assertNotEquals(config1, config2, "Configs with different values should not be equal");
    }

    @Test
    @DisplayName("should have same hash code for equal configs")
    void shouldHaveSameHashCodeForEqualConfigs() {
      final ModuleCacheConfig config1 = ModuleCacheConfig.builder().maxEntries(500).build();
      final ModuleCacheConfig config2 = ModuleCacheConfig.builder().maxEntries(500).build();
      assertEquals(
          config1.hashCode(), config2.hashCode(), "Equal configs should have same hash code");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final ModuleCacheConfig config = ModuleCacheConfig.builder().build();
      assertNotEquals(null, config, "Config should not be equal to null");
    }

    @Test
    @DisplayName("should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      final ModuleCacheConfig config = ModuleCacheConfig.builder().build();
      assertNotEquals("not a config", config, "Config should not be equal to different type");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return meaningful string representation")
    void shouldReturnMeaningfulStringRepresentation() {
      final ModuleCacheConfig config =
          ModuleCacheConfig.builder()
              .cacheDir("/tmp/cache")
              .maxCacheSize(1024000)
              .maxEntries(100)
              .compressionEnabled(true)
              .compressionLevel(10)
              .build();
      final String str = config.toString();
      assertTrue(str.contains("ModuleCacheConfig"), "Should contain class name");
      assertTrue(str.contains("1024000"), "Should contain max cache size");
      assertTrue(str.contains("100"), "Should contain max entries");
      assertTrue(str.contains("compressionEnabled=true"), "Should contain compression flag");
      assertTrue(str.contains("compressionLevel=10"), "Should contain compression level");
    }
  }

  @Nested
  @DisplayName("Full Integration Tests")
  class FullIntegrationTests {

    @Test
    @DisplayName("should build complete config with all options")
    void shouldBuildCompleteConfigWithAllOptions() {
      final ModuleCacheConfig config =
          ModuleCacheConfig.builder()
              .cacheDir(Paths.get("/var/cache/wasm"))
              .maxCacheSize(1024L * 1024L * 1024L) // 1 GB
              .maxEntries(5000)
              .compressionEnabled(true)
              .compressionLevel(3)
              .build();

      assertEquals(Paths.get("/var/cache/wasm"), config.getCacheDir(), "Cache dir should match");
      assertEquals(1024L * 1024L * 1024L, config.getMaxCacheSize(), "Max cache size should match");
      assertEquals(5000, config.getMaxEntries(), "Max entries should match");
      assertTrue(config.isCompressionEnabled(), "Compression should be enabled");
      assertEquals(3, config.getCompressionLevel(), "Compression level should match");
    }

    @Test
    @DisplayName("should allow zero max cache size for unlimited")
    void shouldAllowZeroMaxCacheSizeForUnlimited() {
      final ModuleCacheConfig config = ModuleCacheConfig.builder().maxCacheSize(0).build();
      assertEquals(0, config.getMaxCacheSize(), "Max cache size should be 0");
    }
  }
}
