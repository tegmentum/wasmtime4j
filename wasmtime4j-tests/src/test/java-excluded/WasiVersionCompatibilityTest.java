package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Comprehensive test for WASI version compatibility and backward compatibility.
 *
 * <p>This test verifies that:
 *
 * <ul>
 *   <li>WASI Preview 1 continues to work as expected
 *   <li>WASI Preview 2 provides enhanced functionality
 *   <li>Version selection works correctly
 *   <li>Configurations are properly validated
 *   <li>Both sync and async operations work
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("WASI Version Compatibility Test")
class WasiVersionCompatibilityTest {

  @Test
  @DisplayName("WASI Version enumeration should work correctly")
  void testWasiVersionEnumeration() {
    // Test version string parsing
    assertEquals(WasiVersion.PREVIEW_1, WasiVersion.fromVersionString("0.1.0"));
    assertEquals(WasiVersion.PREVIEW_2, WasiVersion.fromVersionString("0.2.0"));

    // Test version properties
    assertFalse(WasiVersion.PREVIEW_1.supportsAsyncOperations());
    assertTrue(WasiVersion.PREVIEW_2.supportsAsyncOperations());

    assertFalse(WasiVersion.PREVIEW_1.supportsComponentModel());
    assertTrue(WasiVersion.PREVIEW_2.supportsComponentModel());

    assertFalse(WasiVersion.PREVIEW_1.supportsWitInterfaces());
    assertTrue(WasiVersion.PREVIEW_2.supportsWitInterfaces());

    assertFalse(WasiVersion.PREVIEW_1.supportsStreamOperations());
    assertTrue(WasiVersion.PREVIEW_2.supportsStreamOperations());

    assertFalse(WasiVersion.PREVIEW_1.supportsHttpOperations());
    assertTrue(WasiVersion.PREVIEW_2.supportsHttpOperations());

    // Test defaults
    assertEquals(WasiVersion.PREVIEW_1, WasiVersion.getDefault());
    assertEquals(WasiVersion.PREVIEW_2, WasiVersion.getLatest());

    // Test compatibility
    assertTrue(WasiVersion.PREVIEW_1.isCompatibleWith(WasiVersion.PREVIEW_1));
    assertTrue(WasiVersion.PREVIEW_2.isCompatibleWith(WasiVersion.PREVIEW_2));
    assertFalse(WasiVersion.PREVIEW_1.isCompatibleWith(WasiVersion.PREVIEW_2));
    assertFalse(WasiVersion.PREVIEW_2.isCompatibleWith(WasiVersion.PREVIEW_1));
  }

  @Test
  @DisplayName("WASI Preview 1 configuration should work correctly")
  void testWasiPreview1Configuration() {
    final WasiConfig config =
        WasiConfig.builder()
            .withWasiVersion(WasiVersion.PREVIEW_1)
            .withEnvironment("TEST_VAR", "test_value")
            .withArgument("--test")
            .withMemoryLimit(64 * 1024 * 1024) // 64MB
            .withExecutionTimeout(Duration.ofSeconds(30))
            .withValidation(true)
            .build();

    assertNotNull(config);
    assertEquals(WasiVersion.PREVIEW_1, config.getWasiVersion());
    assertFalse(config.isAsyncOperationsEnabled());
    assertTrue(config.getMaxAsyncOperations().isEmpty());
    assertTrue(config.getAsyncOperationTimeout().isEmpty());

    assertTrue(config.getEnvironment().containsKey("TEST_VAR"));
    assertEquals("test_value", config.getEnvironment().get("TEST_VAR"));

    assertTrue(config.getArguments().contains("--test"));
    assertEquals(64 * 1024 * 1024L, config.getMemoryLimit().orElse(0L));
    assertEquals(Duration.ofSeconds(30), config.getExecutionTimeout().orElse(Duration.ZERO));
    assertTrue(config.isValidationEnabled());

    // Validation should pass
    assertDoesNotThrow(config::validate);
  }

  @Test
  @DisplayName("WASI Preview 2 configuration should work correctly")
  void testWasiPreview2Configuration() {
    final WasiConfig config =
        WasiConfig.builder()
            .withWasiVersion(WasiVersion.PREVIEW_2)
            .withEnvironment("TEST_VAR", "test_value")
            .withArgument("--test")
            .withAsyncOperations(true)
            .withMaxAsyncOperations(100)
            .withAsyncOperationTimeout(Duration.ofSeconds(10))
            .withMemoryLimit(128 * 1024 * 1024) // 128MB
            .withExecutionTimeout(Duration.ofMinutes(5))
            .withValidation(true)
            .build();

    assertNotNull(config);
    assertEquals(WasiVersion.PREVIEW_2, config.getWasiVersion());
    assertTrue(config.isAsyncOperationsEnabled());
    assertEquals(100, config.getMaxAsyncOperations().orElse(0));
    assertEquals(Duration.ofSeconds(10), config.getAsyncOperationTimeout().orElse(Duration.ZERO));

    assertTrue(config.getEnvironment().containsKey("TEST_VAR"));
    assertEquals("test_value", config.getEnvironment().get("TEST_VAR"));

    assertTrue(config.getArguments().contains("--test"));
    assertEquals(128 * 1024 * 1024L, config.getMemoryLimit().orElse(0L));
    assertEquals(Duration.ofMinutes(5), config.getExecutionTimeout().orElse(Duration.ZERO));
    assertTrue(config.isValidationEnabled());

    // Validation should pass
    assertDoesNotThrow(config::validate);
  }

  @Test
  @DisplayName("Context factory should work correctly")
  void testContextFactory() {
    // Test Preview 1 context creation
    final WasiConfig preview1Config =
        WasiConfig.builder().withWasiVersion(WasiVersion.PREVIEW_1).build();

    if (WasiContextFactory.isPreview2Supported()) {
      assertDoesNotThrow(() -> WasiContextFactory.createPreview1Context(preview1Config));
    }

    // Test Preview 2 context creation
    final WasiConfig preview2Config =
        WasiConfig.builder()
            .withWasiVersion(WasiVersion.PREVIEW_2)
            .withAsyncOperations(true)
            .build();

    if (WasiContextFactory.isPreview2Supported()) {
      assertDoesNotThrow(() -> WasiContextFactory.createPreview2Context(preview2Config));
    }

    // Test invalid configurations
    final WasiConfig invalidConfig1 =
        WasiConfig.builder().withWasiVersion(WasiVersion.PREVIEW_1).build();

    assertThrows(
        IllegalArgumentException.class,
        () -> WasiContextFactory.createPreview2Context(invalidConfig1));

    final WasiConfig invalidConfig2 =
        WasiConfig.builder().withWasiVersion(WasiVersion.PREVIEW_2).build();

    assertThrows(
        IllegalArgumentException.class,
        () -> WasiContextFactory.createPreview1Context(invalidConfig2));
  }

  @Test
  @DisplayName("Configuration validation should work correctly")
  void testConfigurationValidation() {
    // Valid configurations
    final WasiConfig validPreview1 =
        WasiConfig.builder().withWasiVersion(WasiVersion.PREVIEW_1).build();
    assertDoesNotThrow(validPreview1::validate);

    final WasiConfig validPreview2 =
        WasiConfig.builder()
            .withWasiVersion(WasiVersion.PREVIEW_2)
            .withAsyncOperations(true)
            .build();
    assertDoesNotThrow(validPreview2::validate);

    // Test builder validation
    final WasiConfigBuilder builder = WasiConfig.builder();
    assertDoesNotThrow(builder::validate);

    builder.withWasiVersion(WasiVersion.PREVIEW_2);
    assertDoesNotThrow(builder::validate);

    builder.withAsyncOperations(true);
    assertDoesNotThrow(builder::validate);
  }

  @Test
  @DisplayName("Version-specific features should work correctly")
  @EnabledIfSystemProperty(named = "wasmtime4j.test.native", matches = "true")
  void testVersionSpecificFeatures() {
    if (!WasiContextFactory.isPreview2Supported()) {
      // Skip if Preview 2 is not supported
      return;
    }

    // Test Preview 2 specific features
    final WasiConfig preview2Config =
        WasiConfig.builder()
            .withWasiVersion(WasiVersion.PREVIEW_2)
            .withAsyncOperations(true)
            .withMaxAsyncOperations(50)
            .withAsyncOperationTimeout(Duration.ofSeconds(5))
            .build();

    try (final WasiPreview2Context context =
        WasiContextFactory.createPreview2Context(preview2Config)) {
      assertNotNull(context);
      assertEquals(preview2Config, context.getConfig());

      // Test async random operations
      final ByteBuffer randomBuffer = ByteBuffer.allocate(32);
      final CompletableFuture<Void> randomFuture = context.getRandomBytesAsync(randomBuffer);
      assertDoesNotThrow(() -> randomFuture.get(5, TimeUnit.SECONDS));

      // Test async time operations
      final CompletableFuture<Long> timeFuture = context.getTimeAsync(0, 1000);
      assertDoesNotThrow(
          () -> {
            final Long time = timeFuture.get(5, TimeUnit.SECONDS);
            assertTrue(time > 0);
          });

      // Test pollable creation
      assertDoesNotThrow(
          () -> {
            final long resourceHandle = context.createResource("test", ByteBuffer.allocate(16));
            final long pollableHandle = context.createPollable(resourceHandle);
            assertTrue(pollableHandle > 0);
            context.destroyResource(resourceHandle);
          });
    }
  }

  @Test
  @DisplayName("Backward compatibility should be maintained")
  void testBackwardCompatibility() {
    // Test that Preview 1 style configuration still works
    final WasiConfig legacyConfig =
        WasiConfig.builder()
            .withEnvironment("LEGACY_VAR", "legacy_value")
            .withArgument("legacy-program")
            .withArgument("--legacy-flag")
            .withMemoryLimit(32 * 1024 * 1024)
            .withValidation(false)
            .build();

    // Should default to Preview 1
    assertEquals(WasiVersion.PREVIEW_1, legacyConfig.getWasiVersion());
    assertFalse(legacyConfig.isAsyncOperationsEnabled());

    // Should be valid
    assertDoesNotThrow(legacyConfig::validate);

    // Test configuration conversion
    final WasiConfigBuilder builderFromConfig = legacyConfig.toBuilder();
    assertNotNull(builderFromConfig);

    final WasiConfig rebuiltConfig = builderFromConfig.build();
    assertEquals(legacyConfig.getWasiVersion(), rebuiltConfig.getWasiVersion());
    assertEquals(legacyConfig.getEnvironment(), rebuiltConfig.getEnvironment());
    assertEquals(legacyConfig.getArguments(), rebuiltConfig.getArguments());
    assertEquals(legacyConfig.getMemoryLimit(), rebuiltConfig.getMemoryLimit());
    assertEquals(legacyConfig.isValidationEnabled(), rebuiltConfig.isValidationEnabled());
  }

  @Test
  @DisplayName("Configuration builder should work correctly")
  void testConfigurationBuilder() {
    final WasiConfigBuilder builder = WasiConfig.builder();

    // Test method chaining
    final WasiConfig config =
        builder
            .withWasiVersion(WasiVersion.PREVIEW_2)
            .withEnvironment("VAR1", "value1")
            .withEnvironment("VAR2", "value2")
            .withArgument("program")
            .withArgument("--flag")
            .withAsyncOperations(true)
            .withMaxAsyncOperations(200)
            .withAsyncOperationTimeout(Duration.ofSeconds(15))
            .withMemoryLimit(256 * 1024 * 1024)
            .withExecutionTimeout(Duration.ofMinutes(10))
            .withValidation(true)
            .withStrictMode(false)
            .build();

    assertNotNull(config);
    assertEquals(WasiVersion.PREVIEW_2, config.getWasiVersion());
    assertTrue(config.isAsyncOperationsEnabled());
    assertEquals(200, config.getMaxAsyncOperations().orElse(0));
    assertEquals(Duration.ofSeconds(15), config.getAsyncOperationTimeout().orElse(Duration.ZERO));
    assertEquals(256 * 1024 * 1024L, config.getMemoryLimit().orElse(0L));
    assertEquals(Duration.ofMinutes(10), config.getExecutionTimeout().orElse(Duration.ZERO));
    assertTrue(config.isValidationEnabled());
    assertFalse(config.isStrictModeEnabled());

    final Map<String, String> env = config.getEnvironment();
    assertEquals("value1", env.get("VAR1"));
    assertEquals("value2", env.get("VAR2"));

    final List<String> args = config.getArguments();
    assertTrue(args.contains("program"));
    assertTrue(args.contains("--flag"));
  }

  @Test
  @DisplayName("Invalid configurations should be rejected")
  void testInvalidConfigurations() {
    // Test invalid version string
    assertThrows(IllegalArgumentException.class, () -> WasiVersion.fromVersionString("invalid"));

    assertThrows(IllegalArgumentException.class, () -> WasiVersion.fromVersionString(null));

    assertThrows(IllegalArgumentException.class, () -> WasiVersion.fromVersionString(""));

    // Test null configuration
    assertThrows(IllegalArgumentException.class, () -> WasiContextFactory.createContext(null));

    assertThrows(
        IllegalArgumentException.class, () -> WasiContextFactory.createPreview1Context(null));

    assertThrows(
        IllegalArgumentException.class, () -> WasiContextFactory.createPreview2Context(null));
  }

  @Test
  @DisplayName("Runtime capabilities should be detected correctly")
  void testRuntimeCapabilities() {
    // Test capability detection
    final boolean preview2Supported = WasiContextFactory.isPreview2Supported();
    final boolean asyncSupported = WasiContextFactory.isAsyncOperationsSupported();

    // Async operations require Preview 2
    if (asyncSupported) {
      assertTrue(preview2Supported);
    }

    // Test recommended version
    final WasiVersion recommended = WasiContextFactory.getRecommendedVersion();
    assertNotNull(recommended);
    assertTrue(recommended == WasiVersion.PREVIEW_1 || recommended == WasiVersion.PREVIEW_2);

    if (preview2Supported) {
      assertEquals(WasiVersion.PREVIEW_2, recommended);
    } else {
      assertEquals(WasiVersion.PREVIEW_1, recommended);
    }
  }
}
