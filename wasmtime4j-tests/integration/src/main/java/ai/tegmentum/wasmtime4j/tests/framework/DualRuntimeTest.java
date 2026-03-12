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
package ai.tegmentum.wasmtime4j.tests.framework;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

/**
 * Base class for tests that should run against both JNI and Panama runtimes.
 *
 * <p>This class provides infrastructure for parameterized tests that verify behavior across both
 * runtime implementations, ensuring consistency and correctness.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @ParameterizedTest
 * @MethodSource("ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest#runtimeTypes")
 * void testSomething(RuntimeType runtime) {
 *     setRuntime(runtime);
 *     // Test code here
 * }
 * }</pre>
 */
@ExtendWith(DualRuntimeCleanupExtension.class)
public abstract class DualRuntimeTest {

  private static final ThreadLocal<RuntimeType> currentRuntime = new ThreadLocal<>();

  /**
   * Provides runtime types for parameterized tests.
   *
   * <p>Only includes runtimes that are actually available on the current system.
   *
   * @return stream of available runtime types
   */
  public static Stream<RuntimeType> runtimeTypes() {
    final Stream.Builder<RuntimeType> builder = Stream.builder();

    // Always try JNI first
    if (WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI)) {
      builder.add(RuntimeType.JNI);
    }

    // Add Panama if available (requires Java 23+)
    if (WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA)) {
      builder.add(RuntimeType.PANAMA);
    }

    return builder.build();
  }

  /**
   * JUnit 5 ArgumentsProvider that supplies both JNI and Panama runtime types for parameterized
   * tests.
   *
   * <p>Only includes runtimes that are actually available on the current system.
   */
  public static class RuntimeProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
      return runtimeTypes().map(Arguments::of);
    }
  }

  /**
   * Sets the system property to use the specified runtime type.
   *
   * @param runtime the runtime type to use
   */
  public static void setRuntime(final RuntimeType runtime) {
    System.setProperty(WasmRuntimeFactory.RUNTIME_PROPERTY, runtime.name().toLowerCase());
    // Clear the factory cache to ensure the new runtime is selected
    WasmRuntimeFactory.clearCache();
    // Store current runtime for performance tracking
    currentRuntime.set(runtime);
  }

  /**
   * Gets the currently configured runtime type for this thread.
   *
   * @return the current runtime type, or null if not set
   */
  public static RuntimeType getCurrentRuntime() {
    return currentRuntime.get();
  }

  /** Clears the runtime selection, reverting to automatic selection. */
  public static void clearRuntimeSelection() {
    System.clearProperty(WasmRuntimeFactory.RUNTIME_PROPERTY);
    WasmRuntimeFactory.clearCache();
  }

  /**
   * Gets a descriptive name for the runtime type suitable for test display.
   *
   * @param runtime the runtime type
   * @return a display name for the runtime
   */
  public static String getRuntimeDisplayName(final RuntimeType runtime) {
    switch (runtime) {
      case JNI:
        return "JNI";
      case PANAMA:
        return "Panama";
      default:
        return runtime.name();
    }
  }
}
