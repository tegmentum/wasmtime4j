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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentEngineHealthCheckResult} interface.
 *
 * <p>ComponentEngineHealthCheckResult provides health check results for the component engine.
 */
@DisplayName("ComponentEngineHealthCheckResult Tests")
class ComponentEngineHealthCheckResultTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentEngineHealthCheckResult.class.getModifiers()),
          "ComponentEngineHealthCheckResult should be public");
      assertTrue(
          ComponentEngineHealthCheckResult.class.isInterface(),
          "ComponentEngineHealthCheckResult should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineHealthCheckResult.class.getMethod("getStatus");
      assertNotNull(method, "getStatus method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getMessage method")
    void shouldHaveGetMessageMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineHealthCheckResult.class.getMethod("getMessage");
      assertNotNull(method, "getMessage method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getTimestamp method")
    void shouldHaveGetTimestampMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineHealthCheckResult.class.getMethod("getTimestamp");
      assertNotNull(method, "getTimestamp method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getDuration method")
    void shouldHaveGetDurationMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineHealthCheckResult.class.getMethod("getDuration");
      assertNotNull(method, "getDuration method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Stub Implementation Tests")
  class StubImplementationTests {

    @Test
    @DisplayName("should support healthy status implementation")
    void shouldSupportHealthyStatusImplementation() {
      final ComponentEngineHealthCheckResult result = createHealthyResult();

      assertEquals("HEALTHY", result.getStatus(), "Status should be HEALTHY");
      assertEquals("Engine is operating normally", result.getMessage(), "Message should match");
      assertTrue(result.getTimestamp() > 0, "Timestamp should be positive");
      assertTrue(result.getDuration() >= 0, "Duration should be non-negative");
    }

    @Test
    @DisplayName("should support unhealthy status implementation")
    void shouldSupportUnhealthyStatusImplementation() {
      final ComponentEngineHealthCheckResult result = createUnhealthyResult();

      assertEquals("UNHEALTHY", result.getStatus(), "Status should be UNHEALTHY");
      assertEquals("Memory pressure detected", result.getMessage(), "Message should match");
      assertTrue(result.getTimestamp() > 0, "Timestamp should be positive");
      assertTrue(result.getDuration() >= 0, "Duration should be non-negative");
    }

    @Test
    @DisplayName("should support unknown status implementation")
    void shouldSupportUnknownStatusImplementation() {
      final ComponentEngineHealthCheckResult result = createUnknownResult();

      assertEquals("UNKNOWN", result.getStatus(), "Status should be UNKNOWN");
      assertEquals(
          "Unable to determine health status", result.getMessage(), "Message should match");
    }
  }

  @Nested
  @DisplayName("Status Value Tests")
  class StatusValueTests {

    @Test
    @DisplayName("status values should follow conventions")
    void statusValuesShouldFollowConventions() {
      final ComponentEngineHealthCheckResult healthy = createHealthyResult();
      final ComponentEngineHealthCheckResult unhealthy = createUnhealthyResult();
      final ComponentEngineHealthCheckResult unknown = createUnknownResult();

      // Status should be uppercase by convention
      assertTrue(
          healthy.getStatus().equals(healthy.getStatus().toUpperCase()),
          "Status should be uppercase");
      assertTrue(
          unhealthy.getStatus().equals(unhealthy.getStatus().toUpperCase()),
          "Status should be uppercase");
      assertTrue(
          unknown.getStatus().equals(unknown.getStatus().toUpperCase()),
          "Status should be uppercase");
    }
  }

  @Nested
  @DisplayName("Timestamp Tests")
  class TimestampTests {

    @Test
    @DisplayName("timestamp should be in milliseconds")
    void timestampShouldBeInMilliseconds() {
      final long beforeCheck = System.currentTimeMillis();
      final ComponentEngineHealthCheckResult result = createHealthyResult();
      final long afterCheck = System.currentTimeMillis();

      assertTrue(result.getTimestamp() >= beforeCheck, "Timestamp should be >= check start time");
      assertTrue(result.getTimestamp() <= afterCheck, "Timestamp should be <= check end time");
    }
  }

  @Nested
  @DisplayName("Duration Tests")
  class DurationTests {

    @Test
    @DisplayName("duration should be in milliseconds")
    void durationShouldBeInMilliseconds() {
      final ComponentEngineHealthCheckResult result = createHealthyResultWithDuration(100);

      assertEquals(100, result.getDuration(), "Duration should be 100ms");
    }

    @Test
    @DisplayName("duration should be non-negative")
    void durationShouldBeNonNegative() {
      final ComponentEngineHealthCheckResult result = createHealthyResult();

      assertTrue(result.getDuration() >= 0, "Duration should be non-negative");
    }

    @Test
    @DisplayName("should handle zero duration")
    void shouldHandleZeroDuration() {
      final ComponentEngineHealthCheckResult result = createHealthyResultWithDuration(0);

      assertEquals(0, result.getDuration(), "Zero duration should work");
    }

    @Test
    @DisplayName("should handle large duration values")
    void shouldHandleLargeDurationValues() {
      final ComponentEngineHealthCheckResult result =
          createHealthyResultWithDuration(Long.MAX_VALUE);

      assertEquals(Long.MAX_VALUE, result.getDuration(), "Should handle max long");
    }
  }

  @Nested
  @DisplayName("Message Tests")
  class MessageTests {

    @Test
    @DisplayName("message should be descriptive for healthy status")
    void messageShouldBeDescriptiveForHealthyStatus() {
      final ComponentEngineHealthCheckResult result = createHealthyResult();

      assertNotNull(result.getMessage(), "Message should not be null");
      assertTrue(result.getMessage().length() > 0, "Message should not be empty");
    }

    @Test
    @DisplayName("message should explain issue for unhealthy status")
    void messageShouldExplainIssueForUnhealthyStatus() {
      final ComponentEngineHealthCheckResult result = createUnhealthyResult();

      assertNotNull(result.getMessage(), "Message should not be null");
      assertTrue(result.getMessage().length() > 0, "Message should not be empty");
    }
  }

  /**
   * Creates a healthy health check result.
   *
   * @return a healthy result
   */
  private ComponentEngineHealthCheckResult createHealthyResult() {
    return createHealthyResultWithDuration(10);
  }

  /**
   * Creates a healthy health check result with specified duration.
   *
   * @param duration the duration in milliseconds
   * @return a healthy result
   */
  private ComponentEngineHealthCheckResult createHealthyResultWithDuration(final long duration) {
    return new ComponentEngineHealthCheckResult() {
      private final long timestamp = System.currentTimeMillis();

      @Override
      public String getStatus() {
        return "HEALTHY";
      }

      @Override
      public String getMessage() {
        return "Engine is operating normally";
      }

      @Override
      public long getTimestamp() {
        return timestamp;
      }

      @Override
      public long getDuration() {
        return duration;
      }
    };
  }

  /**
   * Creates an unhealthy health check result.
   *
   * @return an unhealthy result
   */
  private ComponentEngineHealthCheckResult createUnhealthyResult() {
    return new ComponentEngineHealthCheckResult() {
      private final long timestamp = System.currentTimeMillis();

      @Override
      public String getStatus() {
        return "UNHEALTHY";
      }

      @Override
      public String getMessage() {
        return "Memory pressure detected";
      }

      @Override
      public long getTimestamp() {
        return timestamp;
      }

      @Override
      public long getDuration() {
        return 50;
      }
    };
  }

  /**
   * Creates an unknown health check result.
   *
   * @return an unknown result
   */
  private ComponentEngineHealthCheckResult createUnknownResult() {
    return new ComponentEngineHealthCheckResult() {
      private final long timestamp = System.currentTimeMillis();

      @Override
      public String getStatus() {
        return "UNKNOWN";
      }

      @Override
      public String getMessage() {
        return "Unable to determine health status";
      }

      @Override
      public long getTimestamp() {
        return timestamp;
      }

      @Override
      public long getDuration() {
        return 0;
      }
    };
  }
}
