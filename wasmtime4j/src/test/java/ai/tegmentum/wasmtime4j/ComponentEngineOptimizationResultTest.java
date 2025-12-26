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
 * Tests for {@link ComponentEngineOptimizationResult} interface.
 *
 * <p>ComponentEngineOptimizationResult provides optimization results for the component engine.
 */
@DisplayName("ComponentEngineOptimizationResult Tests")
class ComponentEngineOptimizationResultTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentEngineOptimizationResult.class.getModifiers()),
          "ComponentEngineOptimizationResult should be public");
      assertTrue(
          ComponentEngineOptimizationResult.class.isInterface(),
          "ComponentEngineOptimizationResult should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineOptimizationResult.class.getMethod("getStatus");
      assertNotNull(method, "getStatus method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getMessage method")
    void shouldHaveGetMessageMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineOptimizationResult.class.getMethod("getMessage");
      assertNotNull(method, "getMessage method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getMetrics method")
    void shouldHaveGetMetricsMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineOptimizationResult.class.getMethod("getMetrics");
      assertNotNull(method, "getMetrics method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getDuration method")
    void shouldHaveGetDurationMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineOptimizationResult.class.getMethod("getDuration");
      assertNotNull(method, "getDuration method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Stub Implementation Tests")
  class StubImplementationTests {

    @Test
    @DisplayName("should support SUCCESS status implementation")
    void shouldSupportSuccessStatusImplementation() {
      final ComponentEngineOptimizationResult result = createSuccessResult();

      assertEquals("SUCCESS", result.getStatus(), "Status should be SUCCESS");
      assertEquals(
          "Optimization completed successfully", result.getMessage(), "Message should match");
      assertNotNull(result.getMetrics(), "Metrics should not be null");
      assertTrue(result.getDuration() >= 0, "Duration should be non-negative");
    }

    @Test
    @DisplayName("should support FAILED status implementation")
    void shouldSupportFailedStatusImplementation() {
      final ComponentEngineOptimizationResult result = createFailedResult();

      assertEquals("FAILED", result.getStatus(), "Status should be FAILED");
      assertEquals(
          "Optimization failed: out of memory", result.getMessage(), "Message should match");
    }

    @Test
    @DisplayName("should support PARTIAL status implementation")
    void shouldSupportPartialStatusImplementation() {
      final ComponentEngineOptimizationResult result = createPartialResult();

      assertEquals("PARTIAL", result.getStatus(), "Status should be PARTIAL");
      assertEquals("Optimization partially completed", result.getMessage(), "Message should match");
    }
  }

  @Nested
  @DisplayName("Status Value Tests")
  class StatusValueTests {

    @Test
    @DisplayName("should support standard status values")
    void shouldSupportStandardStatusValues() {
      final ComponentEngineOptimizationResult success = createSuccessResult();
      final ComponentEngineOptimizationResult failed = createFailedResult();
      final ComponentEngineOptimizationResult partial = createPartialResult();

      assertEquals("SUCCESS", success.getStatus(), "SUCCESS status should work");
      assertEquals("FAILED", failed.getStatus(), "FAILED status should work");
      assertEquals("PARTIAL", partial.getStatus(), "PARTIAL status should work");
    }

    @Test
    @DisplayName("status values should be uppercase")
    void statusValuesShouldBeUppercase() {
      final ComponentEngineOptimizationResult result = createSuccessResult();

      assertEquals(
          result.getStatus().toUpperCase(), result.getStatus(), "Status should be uppercase");
    }
  }

  @Nested
  @DisplayName("Message Tests")
  class MessageTests {

    @Test
    @DisplayName("message should be descriptive for success")
    void messageShouldBeDescriptiveForSuccess() {
      final ComponentEngineOptimizationResult result = createSuccessResult();

      assertNotNull(result.getMessage(), "Message should not be null");
      assertTrue(result.getMessage().length() > 0, "Message should not be empty");
    }

    @Test
    @DisplayName("message should explain failure")
    void messageShouldExplainFailure() {
      final ComponentEngineOptimizationResult result = createFailedResult();

      assertTrue(
          result.getMessage().contains("failed") || result.getMessage().contains("error"),
          "Failure message should explain the issue");
    }

    @Test
    @DisplayName("should handle detailed messages")
    void shouldHandleDetailedMessages() {
      final ComponentEngineOptimizationResult result =
          createResultWithMessage(
              "Optimization completed: 15 components optimized, 2 skipped, 3 warnings");

      assertTrue(result.getMessage().contains("15 components"), "Should contain details");
      assertTrue(result.getMessage().contains("2 skipped"), "Should contain skip count");
      assertTrue(result.getMessage().contains("3 warnings"), "Should contain warning count");
    }
  }

  @Nested
  @DisplayName("Metrics Tests")
  class MetricsTests {

    @Test
    @DisplayName("should return metrics string")
    void shouldReturnMetricsString() {
      final ComponentEngineOptimizationResult result = createSuccessResult();

      assertNotNull(result.getMetrics(), "Metrics should not be null");
    }

    @Test
    @DisplayName("should include relevant metrics")
    void shouldIncludeRelevantMetrics() {
      final ComponentEngineOptimizationResult result = createDetailedResult();

      final String metrics = result.getMetrics();
      assertTrue(metrics.contains("memory"), "Should contain memory metrics");
      assertTrue(metrics.contains("time"), "Should contain time metrics");
    }

    @Test
    @DisplayName("should handle empty metrics")
    void shouldHandleEmptyMetrics() {
      final ComponentEngineOptimizationResult result = createResultWithMetrics("");

      assertEquals("", result.getMetrics(), "Empty metrics should work");
    }
  }

  @Nested
  @DisplayName("Duration Tests")
  class DurationTests {

    @Test
    @DisplayName("should return duration in milliseconds")
    void shouldReturnDurationInMilliseconds() {
      final ComponentEngineOptimizationResult result = createResultWithDuration(1500);

      assertEquals(1500, result.getDuration(), "Duration should be 1500ms");
    }

    @Test
    @DisplayName("should handle zero duration")
    void shouldHandleZeroDuration() {
      final ComponentEngineOptimizationResult result = createResultWithDuration(0);

      assertEquals(0, result.getDuration(), "Zero duration should work");
    }

    @Test
    @DisplayName("should handle large duration values")
    void shouldHandleLargeDurationValues() {
      final ComponentEngineOptimizationResult result = createResultWithDuration(Long.MAX_VALUE);

      assertEquals(Long.MAX_VALUE, result.getDuration(), "Should handle max long");
    }

    @Test
    @DisplayName("should be non-negative")
    void shouldBeNonNegative() {
      final ComponentEngineOptimizationResult result = createSuccessResult();

      assertTrue(result.getDuration() >= 0, "Duration should be non-negative");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle special characters in message")
    void shouldHandleSpecialCharactersInMessage() {
      final String specialMessage =
          "Optimization failed: \"Invalid syntax\" at line 5\n\tStack trace...";
      final ComponentEngineOptimizationResult result = createResultWithMessage(specialMessage);

      assertEquals(specialMessage, result.getMessage(), "Should handle special characters");
    }

    @Test
    @DisplayName("should handle unicode in metrics")
    void shouldHandleUnicodeInMetrics() {
      final String unicodeMetrics = "メモリ使用量: 256MB";
      final ComponentEngineOptimizationResult result = createResultWithMetrics(unicodeMetrics);

      assertEquals(unicodeMetrics, result.getMetrics(), "Should handle unicode");
    }
  }

  /**
   * Creates a success optimization result.
   *
   * @return success result
   */
  private ComponentEngineOptimizationResult createSuccessResult() {
    return new ComponentEngineOptimizationResult() {
      @Override
      public String getStatus() {
        return "SUCCESS";
      }

      @Override
      public String getMessage() {
        return "Optimization completed successfully";
      }

      @Override
      public String getMetrics() {
        return "memory_saved=50MB, time_improved=15%";
      }

      @Override
      public long getDuration() {
        return 500;
      }
    };
  }

  /**
   * Creates a failed optimization result.
   *
   * @return failed result
   */
  private ComponentEngineOptimizationResult createFailedResult() {
    return new ComponentEngineOptimizationResult() {
      @Override
      public String getStatus() {
        return "FAILED";
      }

      @Override
      public String getMessage() {
        return "Optimization failed: out of memory";
      }

      @Override
      public String getMetrics() {
        return "";
      }

      @Override
      public long getDuration() {
        return 100;
      }
    };
  }

  /**
   * Creates a partial optimization result.
   *
   * @return partial result
   */
  private ComponentEngineOptimizationResult createPartialResult() {
    return new ComponentEngineOptimizationResult() {
      @Override
      public String getStatus() {
        return "PARTIAL";
      }

      @Override
      public String getMessage() {
        return "Optimization partially completed";
      }

      @Override
      public String getMetrics() {
        return "optimized=8, skipped=2";
      }

      @Override
      public long getDuration() {
        return 300;
      }
    };
  }

  /**
   * Creates a detailed optimization result.
   *
   * @return detailed result
   */
  private ComponentEngineOptimizationResult createDetailedResult() {
    return new ComponentEngineOptimizationResult() {
      @Override
      public String getStatus() {
        return "SUCCESS";
      }

      @Override
      public String getMessage() {
        return "Optimization completed with detailed metrics";
      }

      @Override
      public String getMetrics() {
        return "memory=256MB, time=1.5s, components=10";
      }

      @Override
      public long getDuration() {
        return 1500;
      }
    };
  }

  /**
   * Creates result with specified duration.
   *
   * @param duration the duration
   * @return result with specified duration
   */
  private ComponentEngineOptimizationResult createResultWithDuration(final long duration) {
    return new ComponentEngineOptimizationResult() {
      @Override
      public String getStatus() {
        return "SUCCESS";
      }

      @Override
      public String getMessage() {
        return "Completed";
      }

      @Override
      public String getMetrics() {
        return "";
      }

      @Override
      public long getDuration() {
        return duration;
      }
    };
  }

  /**
   * Creates result with specified message.
   *
   * @param message the message
   * @return result with specified message
   */
  private ComponentEngineOptimizationResult createResultWithMessage(final String message) {
    return new ComponentEngineOptimizationResult() {
      @Override
      public String getStatus() {
        return "SUCCESS";
      }

      @Override
      public String getMessage() {
        return message;
      }

      @Override
      public String getMetrics() {
        return "";
      }

      @Override
      public long getDuration() {
        return 100;
      }
    };
  }

  /**
   * Creates result with specified metrics.
   *
   * @param metrics the metrics
   * @return result with specified metrics
   */
  private ComponentEngineOptimizationResult createResultWithMetrics(final String metrics) {
    return new ComponentEngineOptimizationResult() {
      @Override
      public String getStatus() {
        return "SUCCESS";
      }

      @Override
      public String getMessage() {
        return "Completed";
      }

      @Override
      public String getMetrics() {
        return metrics;
      }

      @Override
      public long getDuration() {
        return 100;
      }
    };
  }
}
