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

package ai.tegmentum.wasmtime4j.comparison.framework;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that automatically collects performance timing data for comparison tests.
 *
 * <p>This extension:
 *
 * <ul>
 *   <li>Records the start and end time of each test execution
 *   <li>Identifies the runtime type being tested (JNI or Panama)
 *   <li>Reports timing data to {@link PerformanceReporter}
 *   <li>Generates a performance comparison report after all tests complete
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @ExtendWith(PerformanceTimingExtension.class)
 * public class MyPerformanceTest extends DualRuntimeTest {
 *   // Test methods
 * }
 * }</pre>
 */
public class PerformanceTimingExtension
    implements BeforeTestExecutionCallback, AfterTestExecutionCallback, AfterAllCallback {

  private static final String START_TIME_KEY = "startTime";
  private static final String RUNTIME_TYPE_KEY = "runtimeType";
  private static final boolean TIMING_ENABLED =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.performance.timing", "true"));
  private static final boolean REPORT_ENABLED =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.performance.report", "true"));
  private static final String REPORT_PATH =
      System.getProperty(
          "wasmtime4j.performance.report.path",
          "target/performance-comparison-report.txt");

  @Override
  public void beforeTestExecution(final ExtensionContext context) {
    if (!TIMING_ENABLED) {
      return;
    }

    final ExtensionContext.Store store = getStore(context);
    store.put(START_TIME_KEY, System.nanoTime());

    // Try to get runtime from DualRuntimeTest first
    RuntimeType runtime = DualRuntimeTest.getCurrentRuntime();

    // Fallback to extracting from context
    if (runtime == null) {
      runtime = extractRuntimeFromContext(context);
    }

    if (runtime != null) {
      store.put(RUNTIME_TYPE_KEY, runtime);
    }
  }

  @Override
  public void afterTestExecution(final ExtensionContext context) {
    if (!TIMING_ENABLED) {
      return;
    }

    final ExtensionContext.Store store = getStore(context);
    final Long startTime = store.get(START_TIME_KEY, Long.class);
    final RuntimeType runtime = store.get(RUNTIME_TYPE_KEY, RuntimeType.class);

    if (startTime != null && runtime != null) {
      final long endTime = System.nanoTime();
      final long duration = endTime - startTime;

      final String testName = getTestName(context);
      PerformanceReporter.getInstance().recordTiming(testName, runtime, duration);
    }
  }

  @Override
  public void afterAll(final ExtensionContext context) {
    // Only generate report at the very end (root context), not after each test class
    if (!REPORT_ENABLED || context.getParent().isPresent()) {
      return;
    }

    final PerformanceReporter reporter = PerformanceReporter.getInstance();

    // Only generate report if we have data
    if (reporter.getTestCount() > 0) {
      // Print to console
      reporter.printReport();

      // Write to file
      try {
        reporter.writeReportToFile(REPORT_PATH);
        System.out.println("Performance report written to: " + Paths.get(REPORT_PATH).toAbsolutePath());
      } catch (final IOException e) {
        System.err.println("Failed to write performance report: " + e.getMessage());
      }
    }
  }

  private ExtensionContext.Store getStore(final ExtensionContext context) {
    return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestMethod()));
  }

  private String getTestName(final ExtensionContext context) {
    return context.getRequiredTestClass().getSimpleName()
        + "."
        + context.getRequiredTestMethod().getName();
  }

  private RuntimeType extractRuntimeFromContext(final ExtensionContext context) {
    // Try to extract runtime from display name (JUnit parameterized tests)
    final String displayName = context.getDisplayName();
    if (displayName.contains("JNI")) {
      return RuntimeType.JNI;
    } else if (displayName.contains("PANAMA") || displayName.contains("Panama")) {
      return RuntimeType.PANAMA;
    }

    // Try to get from system property
    final String runtimeProperty = System.getProperty("wasmtime4j.runtime");
    if (runtimeProperty != null) {
      if (runtimeProperty.equalsIgnoreCase("jni")) {
        return RuntimeType.JNI;
      } else if (runtimeProperty.equalsIgnoreCase("panama")) {
        return RuntimeType.PANAMA;
      }
    }

    return null;
  }
}
