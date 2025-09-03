/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.nativeloader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests for {@link NativeLibraryConfig}. */
final class NativeLibraryConfigTest {

  @Test
  void testDefaultConfig() {
    final NativeLibraryConfig config = NativeLibraryConfig.defaultConfig();

    assertNotNull(config, "Default config should not be null");
    assertEquals(
        NativeLibraryConfig.DEFAULT_LIBRARY_NAME,
        config.getLibraryName(),
        "Default library name should match constant");
    assertEquals(
        NativeLibraryConfig.DEFAULT_TEMP_FILE_PREFIX,
        config.getTempFilePrefix(),
        "Default temp file prefix should match constant");
    assertEquals(
        NativeLibraryConfig.DEFAULT_TEMP_DIR_SUFFIX,
        config.getTempDirSuffix(),
        "Default temp dir suffix should match constant");
  }

  @Test
  void testDefaultConstants() {
    assertEquals("wasmtime4j", NativeLibraryConfig.DEFAULT_LIBRARY_NAME);
    assertEquals("wasmtime4j-native-", NativeLibraryConfig.DEFAULT_TEMP_FILE_PREFIX);
    assertEquals("-wasmtime4j", NativeLibraryConfig.DEFAULT_TEMP_DIR_SUFFIX);
  }

  @Test
  void testConstructorWithValidParameters() {
    final NativeLibraryConfig config =
        new NativeLibraryConfig("testlib", "test-prefix-", "-test-suffix");

    assertEquals("testlib", config.getLibraryName());
    assertEquals("test-prefix-", config.getTempFilePrefix());
    assertEquals("-test-suffix", config.getTempDirSuffix());
  }

  @Test
  @SuppressWarnings("NullAway")
  void testConstructorWithNullLibraryNameThrows() {
    assertThrows(
        NullPointerException.class, () -> new NativeLibraryConfig(null, "prefix-", "-suffix"));
  }

  @Test
  @SuppressWarnings("NullAway")
  void testConstructorWithNullTempFilePrefixThrows() {
    assertThrows(
        NullPointerException.class, () -> new NativeLibraryConfig("testlib", null, "-suffix"));
  }

  @Test
  @SuppressWarnings("NullAway")
  void testConstructorWithNullTempDirSuffixThrows() {
    assertThrows(
        NullPointerException.class, () -> new NativeLibraryConfig("testlib", "prefix-", null));
  }

  @Test
  void testConstructorWithEmptyLibraryNameThrows() {
    assertThrows(
        IllegalArgumentException.class, () -> new NativeLibraryConfig("", "prefix-", "-suffix"));
  }

  @Test
  void testConstructorWithWhitespaceOnlyLibraryNameThrows() {
    assertThrows(
        IllegalArgumentException.class, () -> new NativeLibraryConfig("   ", "prefix-", "-suffix"));
  }

  @Test
  void testConstructorWithEmptyTempFilePrefixThrows() {
    assertThrows(
        IllegalArgumentException.class, () -> new NativeLibraryConfig("testlib", "", "-suffix"));
  }

  @Test
  void testConstructorWithEmptyTempDirSuffixThrows() {
    assertThrows(
        IllegalArgumentException.class, () -> new NativeLibraryConfig("testlib", "prefix-", ""));
  }

  @Test
  void testConstructorWithInvalidLibraryNameCharactersThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new NativeLibraryConfig("test/lib", "prefix-", "-suffix"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new NativeLibraryConfig("test\\lib", "prefix-", "-suffix"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new NativeLibraryConfig("test lib", "prefix-", "-suffix"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new NativeLibraryConfig("test.lib", "prefix-", "-suffix"));
  }

  @Test
  void testConstructorWithInvalidTempFilePrefixCharactersThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new NativeLibraryConfig("testlib", "test/prefix-", "-suffix"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new NativeLibraryConfig("testlib", "test\\prefix-", "-suffix"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new NativeLibraryConfig("testlib", "test prefix-", "-suffix"));
  }

  @Test
  void testConstructorWithInvalidTempDirSuffixCharactersThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new NativeLibraryConfig("testlib", "prefix-", "-test/suffix"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new NativeLibraryConfig("testlib", "prefix-", "-test\\suffix"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new NativeLibraryConfig("testlib", "prefix-", "-test suffix"));
  }

  @Test
  void testConstructorTrimsWhitespace() {
    final NativeLibraryConfig config =
        new NativeLibraryConfig("  testlib  ", "  prefix-  ", "  -suffix  ");

    assertEquals("testlib", config.getLibraryName(), "Library name should be trimmed");
    assertEquals("prefix-", config.getTempFilePrefix(), "Temp file prefix should be trimmed");
    assertEquals("-suffix", config.getTempDirSuffix(), "Temp dir suffix should be trimmed");
  }

  @Test
  void testValidCharacters() {
    // Test that alphanumeric, underscores, and dashes are allowed
    final NativeLibraryConfig config =
        new NativeLibraryConfig("test_lib-123", "prefix_123-", "-suffix_123");

    assertEquals("test_lib-123", config.getLibraryName());
    assertEquals("prefix_123-", config.getTempFilePrefix());
    assertEquals("-suffix_123", config.getTempDirSuffix());
  }

  @Test
  void testBuilderPattern() {
    final NativeLibraryConfig config =
        NativeLibraryConfig.builder()
            .libraryName("customlib")
            .tempFilePrefix("custom-prefix-")
            .tempDirSuffix("-custom-suffix")
            .build();

    assertEquals("customlib", config.getLibraryName());
    assertEquals("custom-prefix-", config.getTempFilePrefix());
    assertEquals("-custom-suffix", config.getTempDirSuffix());
  }

  @Test
  void testBuilderWithDefaults() {
    final NativeLibraryConfig config = NativeLibraryConfig.builder().build();

    assertEquals(NativeLibraryConfig.DEFAULT_LIBRARY_NAME, config.getLibraryName());
    assertEquals(NativeLibraryConfig.DEFAULT_TEMP_FILE_PREFIX, config.getTempFilePrefix());
    assertEquals(NativeLibraryConfig.DEFAULT_TEMP_DIR_SUFFIX, config.getTempDirSuffix());
  }

  @Test
  void testBuilderPartialConfiguration() {
    final NativeLibraryConfig config =
        NativeLibraryConfig.builder().libraryName("customlib").build();

    assertEquals("customlib", config.getLibraryName());
    assertEquals(NativeLibraryConfig.DEFAULT_TEMP_FILE_PREFIX, config.getTempFilePrefix());
    assertEquals(NativeLibraryConfig.DEFAULT_TEMP_DIR_SUFFIX, config.getTempDirSuffix());
  }

  @Test
  void testBuilderFluentInterface() {
    final NativeLibraryConfig.Builder builder = NativeLibraryConfig.builder();

    // Test that each method returns the builder for chaining
    final NativeLibraryConfig.Builder result1 = builder.libraryName("test");
    final NativeLibraryConfig.Builder result2 = result1.tempFilePrefix("prefix-");
    final NativeLibraryConfig.Builder result3 = result2.tempDirSuffix("-suffix");

    assertTrue(result1 == builder, "libraryName() should return same builder instance");
    assertTrue(result2 == builder, "tempFilePrefix() should return same builder instance");
    assertTrue(result3 == builder, "tempDirSuffix() should return same builder instance");
  }

  @Test
  void testEquals() {
    final NativeLibraryConfig config1 = new NativeLibraryConfig("testlib", "prefix-", "-suffix");
    final NativeLibraryConfig config2 = new NativeLibraryConfig("testlib", "prefix-", "-suffix");
    final NativeLibraryConfig config3 = new NativeLibraryConfig("otherlib", "prefix-", "-suffix");

    assertEquals(config1, config2, "Configs with same parameters should be equal");
    assertTrue(!config1.equals(config3), "Configs with different parameters should not be equal");
    assertTrue(!config1.equals(null), "Config should not equal null");
    assertTrue(!config1.equals("string"), "Config should not equal different type");
    assertEquals(config1, config1, "Config should equal itself");
  }

  @Test
  void testHashCode() {
    final NativeLibraryConfig config1 = new NativeLibraryConfig("testlib", "prefix-", "-suffix");
    final NativeLibraryConfig config2 = new NativeLibraryConfig("testlib", "prefix-", "-suffix");

    assertEquals(
        config1.hashCode(), config2.hashCode(), "Equal configs should have same hash code");
  }

  @Test
  void testToString() {
    final NativeLibraryConfig config = new NativeLibraryConfig("testlib", "prefix-", "-suffix");

    final String configString = config.toString();
    assertNotNull(configString, "toString should not return null");
    assertTrue(configString.contains("testlib"), "toString should contain library name");
    assertTrue(configString.contains("prefix-"), "toString should contain temp file prefix");
    assertTrue(configString.contains("-suffix"), "toString should contain temp dir suffix");
    assertTrue(configString.contains("NativeLibraryConfig"), "toString should contain class name");
  }

  @Test
  void testSuffixCanStartWithDash() {
    // Test that suffix can start with dash (common pattern)
    final NativeLibraryConfig config = new NativeLibraryConfig("testlib", "prefix-", "-suffix");

    assertEquals("-suffix", config.getTempDirSuffix());
  }

  @Test
  void testSuffixCanStartWithoutDash() {
    // Test that suffix doesn't need to start with dash
    final NativeLibraryConfig config = new NativeLibraryConfig("testlib", "prefix-", "suffix");

    assertEquals("suffix", config.getTempDirSuffix());
  }
}
