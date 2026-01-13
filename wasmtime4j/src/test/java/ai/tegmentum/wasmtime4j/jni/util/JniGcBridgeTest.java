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

package ai.tegmentum.wasmtime4j.jni.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for JniGcBridge. */
@DisplayName("JniGcBridge Tests")
class JniGcBridgeTest {

  private static final Logger LOGGER = Logger.getLogger(JniGcBridgeTest.class.getName());

  @Nested
  @DisplayName("GC Bridge Operation Tests")
  class GcBridgeOperationTests {

    @Test
    @DisplayName("Should perform GC bridge operation")
    void shouldPerformGcBridgeOperation() {
      LOGGER.info("Testing GC bridge operation");

      String result = JniGcBridge.performGcBridge();

      assertThat(result).isNotNull();
      assertThat(result).isNotEmpty();
      assertThat(result).contains("GC bridge");
    }

    @Test
    @DisplayName("Should return consistent result for GC bridge operation")
    void shouldReturnConsistentResultForGcBridgeOperation() {
      LOGGER.info("Testing consistent GC bridge operation result");

      String result1 = JniGcBridge.performGcBridge();
      String result2 = JniGcBridge.performGcBridge();

      assertThat(result1).isEqualTo(result2);
    }
  }

  @Nested
  @DisplayName("GC Bridge Statistics Tests")
  class GcBridgeStatisticsTests {

    @Test
    @DisplayName("Should get GC bridge statistics")
    void shouldGetGcBridgeStatistics() {
      LOGGER.info("Testing GC bridge statistics retrieval");

      String statistics = JniGcBridge.getGcBridgeStatistics();

      assertThat(statistics).isNotNull();
      assertThat(statistics).isNotEmpty();
      assertThat(statistics).contains("statistics");
    }

    @Test
    @DisplayName("Should return consistent statistics")
    void shouldReturnConsistentStatistics() {
      LOGGER.info("Testing consistent statistics result");

      String stats1 = JniGcBridge.getGcBridgeStatistics();
      String stats2 = JniGcBridge.getGcBridgeStatistics();

      assertThat(stats1).isEqualTo(stats2);
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("Should be thread-safe for concurrent calls")
    void shouldBeThreadSafeForConcurrentCalls() throws InterruptedException {
      LOGGER.info("Testing thread safety for concurrent calls");

      final int threadCount = 10;
      final int callsPerThread = 100;
      Thread[] threads = new Thread[threadCount];
      final boolean[] success = {true};

      for (int i = 0; i < threadCount; i++) {
        threads[i] =
            new Thread(
                () -> {
                  try {
                    for (int j = 0; j < callsPerThread; j++) {
                      String opResult = JniGcBridge.performGcBridge();
                      String statsResult = JniGcBridge.getGcBridgeStatistics();

                      if (opResult == null || statsResult == null) {
                        success[0] = false;
                      }
                    }
                  } catch (Exception e) {
                    success[0] = false;
                  }
                });
      }

      // Start all threads
      for (Thread thread : threads) {
        thread.start();
      }

      // Wait for all threads to complete
      for (Thread thread : threads) {
        thread.join();
      }

      assertThat(success[0]).isTrue();
    }
  }

  @Nested
  @DisplayName("Utility Class Design Tests")
  class UtilityClassDesignTests {

    @Test
    @DisplayName("Should have static methods only")
    void shouldHaveStaticMethodsOnly() {
      LOGGER.info("Testing utility class design");

      // JniGcBridge is a utility class with a private constructor
      // We can verify static access works
      assertThat(JniGcBridge.performGcBridge()).isNotNull();
      assertThat(JniGcBridge.getGcBridgeStatistics()).isNotNull();
    }
  }
}
