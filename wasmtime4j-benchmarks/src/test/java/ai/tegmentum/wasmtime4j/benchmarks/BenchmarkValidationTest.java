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
package ai.tegmentum.wasmtime4j.benchmarks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.State;

/** Validates that all benchmark classes are properly structured for JMH execution. */
@DisplayName("Benchmark Validation")
final class BenchmarkValidationTest {

  /** All known benchmark classes that should be validated. */
  private static final List<Class<?>> BENCHMARK_CLASSES =
      List.of(
          RuntimeInitializationBenchmark.class,
          ModuleOperationBenchmark.class,
          FunctionExecutionBenchmark.class,
          MemoryOperationBenchmark.class,
          GlobalOperationsBenchmark.class,
          BulkMemoryOperationsBenchmark.class,
          BulkMemoryPatternBenchmark.class,
          BulkMemoryThroughputBenchmark.class,
          SharedMemoryBenchmark.class,
          PoolingAllocatorPerformanceBenchmark.class,
          MultiValuePerformanceBenchmark.class,
          LinkerBenchmark.class,
          WasiBenchmark.class,
          ComparisonBenchmark.class,
          PanamaVsJniBenchmark.class,
          NativeLoaderComparisonBenchmark.class,
          ConcurrencyBenchmark.class,
          PerformanceOptimizationBenchmark.class,
          PerformanceProfilingIntegration.class,
          MemoryAccessOverheadBenchmark.class,
          GlobalAccessOverheadBenchmark.class,
          TableAccessOverheadBenchmark.class,
          AtomicAccessOverheadBenchmark.class,
          TypedFuncOverheadBenchmark.class,
          PanamaFuncBenchmark.class,
          PanamaGlobalBenchmark.class,
          PanamaTableBenchmark.class,
          PanamaStoreFuelBenchmark.class);

  static Stream<Class<?>> benchmarkClasses() {
    return BENCHMARK_CLASSES.stream();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("benchmarkClasses")
  @DisplayName("Benchmark class extends BenchmarkBase or has JMH annotations")
  void benchmarkExtendsBaseOrHasAnnotations(Class<?> benchmarkClass) {
    boolean extendsBenchmarkBase = BenchmarkBase.class.isAssignableFrom(benchmarkClass);
    boolean hasStateAnnotation = hasStateAnnotation(benchmarkClass);

    assertTrue(
        extendsBenchmarkBase || hasStateAnnotation,
        benchmarkClass.getSimpleName() + " must extend BenchmarkBase or have @State annotation");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("benchmarkClasses")
  @DisplayName("Benchmark class has at least one @Benchmark method")
  void benchmarkHasBenchmarkMethods(Class<?> benchmarkClass) {
    long benchmarkMethodCount = countBenchmarkMethods(benchmarkClass);

    assertTrue(
        benchmarkMethodCount > 0,
        benchmarkClass.getSimpleName()
            + " must have at least one method annotated with @Benchmark");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("benchmarkClasses")
  @DisplayName("Benchmark class has @State annotation (direct or inherited)")
  void benchmarkHasStateAnnotation(Class<?> benchmarkClass) {
    assertTrue(
        hasStateAnnotation(benchmarkClass),
        benchmarkClass.getSimpleName()
            + " must have @State annotation (directly or inherited from superclass)");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("benchmarkClasses")
  @DisplayName("Benchmark class is instantiable (public no-arg constructor or @State class)")
  void benchmarkIsInstantiable(Class<?> benchmarkClass) {
    boolean hasPublicNoArgConstructor = false;
    try {
      Constructor<?> constructor = benchmarkClass.getDeclaredConstructor();
      hasPublicNoArgConstructor = Modifier.isPublic(constructor.getModifiers());
    } catch (NoSuchMethodException e) {
      // No explicit no-arg constructor; check if compiler-generated default is available
      // (only if no other constructors are declared)
      hasPublicNoArgConstructor =
          benchmarkClass.getDeclaredConstructors().length == 0
              || Arrays.stream(benchmarkClass.getDeclaredConstructors())
                  .anyMatch(c -> c.getParameterCount() == 0 && Modifier.isPublic(c.getModifiers()));
    }

    boolean isStateClass = hasStateAnnotation(benchmarkClass);

    assertTrue(
        hasPublicNoArgConstructor || isStateClass,
        benchmarkClass.getSimpleName()
            + " must have a public no-arg constructor or be a @State class");
  }

  @Test
  @DisplayName("No concrete BenchmarkBase subclasses are missing from the validation list")
  void noMissingBenchmarkClasses() {
    List<Class<?>> discoveredClasses = discoverConcreteBenchmarkBaseSubclasses();

    Set<String> knownClassNames =
        BENCHMARK_CLASSES.stream().map(Class::getName).collect(Collectors.toSet());

    List<String> missingClasses =
        discoveredClasses.stream()
            .map(Class::getName)
            .filter(name -> !knownClassNames.contains(name))
            .collect(Collectors.toList());

    assertTrue(
        missingClasses.isEmpty(),
        "Found concrete BenchmarkBase subclasses not in the validation list: " + missingClasses);
  }

  @Test
  @DisplayName("Validation list contains exactly 28 benchmark classes")
  void validationListSize() {
    assertTrue(
        BENCHMARK_CLASSES.size() == 28,
        "Expected 28 benchmark classes but found " + BENCHMARK_CLASSES.size());
  }

  /** Checks whether the given class or any of its superclasses has the {@link State} annotation. */
  private static boolean hasStateAnnotation(Class<?> clazz) {
    Class<?> current = clazz;
    while (current != null && current != Object.class) {
      if (current.isAnnotationPresent(State.class)) {
        return true;
      }
      current = current.getSuperclass();
    }
    return false;
  }

  /** Counts methods annotated with {@link Benchmark} in the given class and its superclasses. */
  private static long countBenchmarkMethods(Class<?> clazz) {
    long count = 0;
    Class<?> current = clazz;
    while (current != null && current != Object.class) {
      for (Method method : current.getDeclaredMethods()) {
        if (method.isAnnotationPresent(Benchmark.class)) {
          count++;
        }
      }
      current = current.getSuperclass();
    }
    return count;
  }

  /**
   * Discovers all concrete classes in the benchmark package that extend {@link BenchmarkBase} by
   * scanning both the main and test class output directories.
   */
  private static List<Class<?>> discoverConcreteBenchmarkBaseSubclasses() {
    List<Class<?>> result = new ArrayList<>();
    String packageName = BenchmarkBase.class.getPackageName();
    String packagePath = packageName.replace('.', '/');

    try {
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      java.util.Enumeration<URL> urls = classLoader.getResources(packagePath);

      while (urls.hasMoreElements()) {
        URL packageUrl = urls.nextElement();
        if (!"file".equals(packageUrl.getProtocol())) {
          continue;
        }

        File packageDir = new File(packageUrl.toURI());
        if (!packageDir.isDirectory()) {
          continue;
        }

        File[] classFiles =
            packageDir.listFiles((dir, name) -> name.endsWith(".class") && !name.contains("$"));
        if (classFiles == null) {
          continue;
        }

        for (File classFile : classFiles) {
          String className = packageName + "." + classFile.getName().replace(".class", "");
          try {
            Class<?> clazz = Class.forName(className);
            if (BenchmarkBase.class.isAssignableFrom(clazz)
                && !Modifier.isAbstract(clazz.getModifiers())
                && clazz != BenchmarkBase.class) {
              result.add(clazz);
            }
          } catch (ClassNotFoundException e) {
            // Skip classes that cannot be loaded
          }
        }
      }
    } catch (Exception e) {
      fail("Error discovering benchmark classes: " + e.getMessage());
    }

    assertFalse(result.isEmpty(), "Should discover at least one concrete BenchmarkBase subclass");
    return result;
  }
}
