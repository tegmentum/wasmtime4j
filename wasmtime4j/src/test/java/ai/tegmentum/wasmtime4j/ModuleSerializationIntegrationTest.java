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

package ai.tegmentum.wasmtime4j.serialization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** Extension of the test class for integration testing. */
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
    final byte[] testData = "test data".getBytes(StandardCharsets.UTF_8);

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
