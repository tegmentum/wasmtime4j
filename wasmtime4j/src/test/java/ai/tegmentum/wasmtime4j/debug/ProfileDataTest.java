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

package ai.tegmentum.wasmtime4j.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for ProfileData class.
 *
 * <p>Verifies profiling data storage and the nested FunctionProfile class.
 */
@DisplayName("ProfileData Tests")
class ProfileDataTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create profile data with all values")
    void shouldCreateProfileDataWithAllValues() {
      Duration duration = Duration.ofSeconds(5);
      List<ProfileData.FunctionProfile> profiles = new ArrayList<>();
      profiles.add(
          new ProfileData.FunctionProfile(
              "test_func", 0, 100, Duration.ofMillis(500), Duration.ofMillis(300)));
      Map<String, Long> metrics = new HashMap<>();
      metrics.put("custom_metric", 42L);

      ProfileData data = new ProfileData(duration, 1000L, 50000L, 10, profiles, metrics);

      assertNotNull(data, "ProfileData should not be null");
      assertEquals(duration, data.getTotalDuration(), "Duration should match");
      assertEquals(1000L, data.getTotalFunctionCalls(), "Function calls should match");
      assertEquals(50000L, data.getTotalInstructions(), "Instructions should match");
      assertEquals(10, data.getMaxStackDepth(), "Max stack depth should match");
      assertEquals(1, data.getFunctionProfiles().size(), "Should have 1 function profile");
      assertEquals(1, data.getCustomMetrics().size(), "Should have 1 custom metric");
    }

    @Test
    @DisplayName("should create profile data with empty collections")
    void shouldCreateProfileDataWithEmptyCollections() {
      ProfileData data =
          new ProfileData(
              Duration.ZERO, 0L, 0L, 0, Collections.emptyList(), Collections.emptyMap());

      assertEquals(Duration.ZERO, data.getTotalDuration(), "Duration should be zero");
      assertEquals(0L, data.getTotalFunctionCalls(), "Function calls should be 0");
      assertEquals(0L, data.getTotalInstructions(), "Instructions should be 0");
      assertEquals(0, data.getMaxStackDepth(), "Max stack depth should be 0");
      assertTrue(data.getFunctionProfiles().isEmpty(), "Function profiles should be empty");
      assertTrue(data.getCustomMetrics().isEmpty(), "Custom metrics should be empty");
    }
  }

  @Nested
  @DisplayName("GetTotalDuration Tests")
  class GetTotalDurationTests {

    @Test
    @DisplayName("should return correct duration")
    void shouldReturnCorrectDuration() {
      Duration duration = Duration.ofMillis(1234);
      ProfileData data =
          new ProfileData(duration, 0L, 0L, 0, Collections.emptyList(), Collections.emptyMap());

      assertEquals(duration, data.getTotalDuration(), "Duration should match");
    }

    @Test
    @DisplayName("should return zero duration")
    void shouldReturnZeroDuration() {
      ProfileData data =
          new ProfileData(
              Duration.ZERO, 0L, 0L, 0, Collections.emptyList(), Collections.emptyMap());

      assertEquals(Duration.ZERO, data.getTotalDuration(), "Duration should be zero");
    }
  }

  @Nested
  @DisplayName("GetTotalFunctionCalls Tests")
  class GetTotalFunctionCallsTests {

    @Test
    @DisplayName("should return correct function call count")
    void shouldReturnCorrectFunctionCallCount() {
      ProfileData data =
          new ProfileData(
              Duration.ZERO, 12345L, 0L, 0, Collections.emptyList(), Collections.emptyMap());

      assertEquals(12345L, data.getTotalFunctionCalls(), "Function calls should match");
    }

    @Test
    @DisplayName("should return zero function calls")
    void shouldReturnZeroFunctionCalls() {
      ProfileData data =
          new ProfileData(
              Duration.ZERO, 0L, 0L, 0, Collections.emptyList(), Collections.emptyMap());

      assertEquals(0L, data.getTotalFunctionCalls(), "Function calls should be 0");
    }
  }

  @Nested
  @DisplayName("GetTotalInstructions Tests")
  class GetTotalInstructionsTests {

    @Test
    @DisplayName("should return correct instruction count")
    void shouldReturnCorrectInstructionCount() {
      ProfileData data =
          new ProfileData(
              Duration.ZERO, 0L, 999999L, 0, Collections.emptyList(), Collections.emptyMap());

      assertEquals(999999L, data.getTotalInstructions(), "Instructions should match");
    }
  }

  @Nested
  @DisplayName("GetMaxStackDepth Tests")
  class GetMaxStackDepthTests {

    @Test
    @DisplayName("should return correct max stack depth")
    void shouldReturnCorrectMaxStackDepth() {
      ProfileData data =
          new ProfileData(
              Duration.ZERO, 0L, 0L, 42, Collections.emptyList(), Collections.emptyMap());

      assertEquals(42, data.getMaxStackDepth(), "Max stack depth should match");
    }
  }

  @Nested
  @DisplayName("GetFunctionProfiles Tests")
  class GetFunctionProfilesTests {

    @Test
    @DisplayName("should return function profiles")
    void shouldReturnFunctionProfiles() {
      List<ProfileData.FunctionProfile> profiles = new ArrayList<>();
      profiles.add(
          new ProfileData.FunctionProfile(
              "func1", 0, 50, Duration.ofMillis(100), Duration.ofMillis(80)));
      profiles.add(
          new ProfileData.FunctionProfile(
              "func2", 1, 30, Duration.ofMillis(60), Duration.ofMillis(40)));

      ProfileData data =
          new ProfileData(Duration.ZERO, 0L, 0L, 0, profiles, Collections.emptyMap());

      List<ProfileData.FunctionProfile> result = data.getFunctionProfiles();
      assertEquals(2, result.size(), "Should have 2 profiles");
      assertEquals("func1", result.get(0).getFunctionName(), "First function name");
      assertEquals("func2", result.get(1).getFunctionName(), "Second function name");
    }

    @Test
    @DisplayName("should return unmodifiable list")
    void shouldReturnUnmodifiableList() {
      List<ProfileData.FunctionProfile> profiles = new ArrayList<>();
      profiles.add(
          new ProfileData.FunctionProfile(
              "func1", 0, 50, Duration.ofMillis(100), Duration.ofMillis(80)));

      ProfileData data =
          new ProfileData(Duration.ZERO, 0L, 0L, 0, profiles, Collections.emptyMap());

      List<ProfileData.FunctionProfile> result = data.getFunctionProfiles();
      assertThrows(
          UnsupportedOperationException.class,
          () ->
              result.add(
                  new ProfileData.FunctionProfile("func2", 1, 10, Duration.ZERO, Duration.ZERO)),
          "Should not allow modification");
    }
  }

  @Nested
  @DisplayName("GetCustomMetrics Tests")
  class GetCustomMetricsTests {

    @Test
    @DisplayName("should return custom metrics")
    void shouldReturnCustomMetrics() {
      Map<String, Long> metrics = new HashMap<>();
      metrics.put("metric1", 100L);
      metrics.put("metric2", 200L);

      ProfileData data =
          new ProfileData(Duration.ZERO, 0L, 0L, 0, Collections.emptyList(), metrics);

      Map<String, Long> result = data.getCustomMetrics();
      assertEquals(2, result.size(), "Should have 2 metrics");
      assertEquals(100L, result.get("metric1"), "Metric1 value");
      assertEquals(200L, result.get("metric2"), "Metric2 value");
    }

    @Test
    @DisplayName("should return unmodifiable map")
    void shouldReturnUnmodifiableMap() {
      Map<String, Long> metrics = new HashMap<>();
      metrics.put("metric1", 100L);

      ProfileData data =
          new ProfileData(Duration.ZERO, 0L, 0L, 0, Collections.emptyList(), metrics);

      Map<String, Long> result = data.getCustomMetrics();
      assertThrows(
          UnsupportedOperationException.class,
          () -> result.put("metric2", 200L),
          "Should not allow modification");
    }
  }

  @Nested
  @DisplayName("FunctionProfile Tests")
  class FunctionProfileTests {

    @Test
    @DisplayName("should create function profile with all values")
    void shouldCreateFunctionProfileWithAllValues() {
      Duration totalTime = Duration.ofMillis(500);
      Duration selfTime = Duration.ofMillis(300);

      ProfileData.FunctionProfile profile =
          new ProfileData.FunctionProfile("my_function", 5, 100, totalTime, selfTime);

      assertEquals("my_function", profile.getFunctionName(), "Function name should match");
      assertEquals(5, profile.getFunctionIndex(), "Function index should match");
      assertEquals(100, profile.getCallCount(), "Call count should match");
      assertEquals(totalTime, profile.getTotalTime(), "Total time should match");
      assertEquals(selfTime, profile.getSelfTime(), "Self time should match");
    }

    @Test
    @DisplayName("should create function profile with zero values")
    void shouldCreateFunctionProfileWithZeroValues() {
      ProfileData.FunctionProfile profile =
          new ProfileData.FunctionProfile("empty_func", 0, 0, Duration.ZERO, Duration.ZERO);

      assertEquals("empty_func", profile.getFunctionName(), "Function name should match");
      assertEquals(0, profile.getFunctionIndex(), "Function index should be 0");
      assertEquals(0, profile.getCallCount(), "Call count should be 0");
      assertEquals(Duration.ZERO, profile.getTotalTime(), "Total time should be zero");
      assertEquals(Duration.ZERO, profile.getSelfTime(), "Self time should be zero");
    }

    @Test
    @DisplayName("should produce meaningful toString")
    void shouldProduceMeaningfulToString() {
      ProfileData.FunctionProfile profile =
          new ProfileData.FunctionProfile(
              "test_func", 3, 50, Duration.ofMillis(200), Duration.ofMillis(150));

      String str = profile.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("test_func"), "Should contain function name");
      assertTrue(str.contains("3"), "Should contain function index");
      assertTrue(str.contains("50"), "Should contain call count");
    }

    @Test
    @DisplayName("should handle large call count")
    void shouldHandleLargeCallCount() {
      ProfileData.FunctionProfile profile =
          new ProfileData.FunctionProfile(
              "hot_func", 0, Long.MAX_VALUE, Duration.ofHours(1), Duration.ofMinutes(30));

      assertEquals(Long.MAX_VALUE, profile.getCallCount(), "Large call count should be handled");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should represent typical profiling session")
    void shouldRepresentTypicalProfilingSession() {
      List<ProfileData.FunctionProfile> profiles = new ArrayList<>();
      profiles.add(
          new ProfileData.FunctionProfile(
              "main", 0, 1, Duration.ofSeconds(2), Duration.ofMillis(100)));
      profiles.add(
          new ProfileData.FunctionProfile(
              "process_data", 1, 1000, Duration.ofSeconds(1), Duration.ofMillis(800)));
      profiles.add(
          new ProfileData.FunctionProfile(
              "helper", 2, 5000, Duration.ofMillis(500), Duration.ofMillis(500)));

      Map<String, Long> metrics = new HashMap<>();
      metrics.put("memory_allocations", 150L);
      metrics.put("gc_pauses", 3L);

      ProfileData data =
          new ProfileData(Duration.ofSeconds(2), 6001L, 100000L, 15, profiles, metrics);

      assertEquals(Duration.ofSeconds(2), data.getTotalDuration(), "Session duration");
      assertEquals(6001L, data.getTotalFunctionCalls(), "Total function calls");
      assertEquals(100000L, data.getTotalInstructions(), "Total instructions");
      assertEquals(15, data.getMaxStackDepth(), "Max stack depth");
      assertEquals(3, data.getFunctionProfiles().size(), "Function profiles count");
      assertEquals(2, data.getCustomMetrics().size(), "Custom metrics count");
    }

    @Test
    @DisplayName("should handle empty profiling session")
    void shouldHandleEmptyProfilingSession() {
      ProfileData data =
          new ProfileData(
              Duration.ofMillis(50), 0L, 0L, 0, Collections.emptyList(), Collections.emptyMap());

      assertEquals(Duration.ofMillis(50), data.getTotalDuration(), "Session duration");
      assertEquals(0L, data.getTotalFunctionCalls(), "No function calls");
      assertEquals(0L, data.getTotalInstructions(), "No instructions");
      assertEquals(0, data.getMaxStackDepth(), "No stack depth");
      assertTrue(data.getFunctionProfiles().isEmpty(), "No function profiles");
      assertTrue(data.getCustomMetrics().isEmpty(), "No custom metrics");
    }
  }
}
