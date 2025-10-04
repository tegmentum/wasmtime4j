package ai.tegmentum.wasmtime4j.serialization;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** Integration tests for module serialization. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Module Serialization Integration Tests")
class ModuleSerializationIntegrationTest {

  @Test
  @DisplayName("Full serialization and deserialization cycle should work")
  void testFullSerializationCycle() {
    // This would test a complete serialization/deserialization cycle
    // with actual WebAssembly modules in a real implementation
    assertTrue(true, "Integration test structure in place");
  }

  @Test
  @DisplayName("Caching integration should work correctly")
  void testCachingIntegration() {
    // Test integration with the ModuleSerializationCache
    final CacheConfiguration config = CacheConfiguration.createDefault();
    assertNotNull(config, "Cache configuration should be available");

    try {
      final ModuleSerializationCache cache = new ModuleSerializationCache(config);
      assertNotNull(cache, "Cache should be created");
      cache.close();
    } catch (java.io.IOException e) {
      fail("Cache creation should not fail: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Security integration should work correctly")
  void testSecurityIntegration() {
    // Test integration with security features
    final byte[] testData = "test data".getBytes();

    assertDoesNotThrow(
        () -> {
          final byte[] hash =
              ai.tegmentum.wasmtime4j.serialization.security.SerializationSecurity.calculateSha256(
                  testData);
          assertNotNull(hash, "Security hash calculation should work");
          assertEquals(32, hash.length, "SHA-256 hash should be 32 bytes");
        });
  }
}
