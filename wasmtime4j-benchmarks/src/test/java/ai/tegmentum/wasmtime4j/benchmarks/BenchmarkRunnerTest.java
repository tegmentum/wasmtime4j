package ai.tegmentum.wasmtime4j.benchmarks;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for BenchmarkRunner configuration and utility methods. */
final class BenchmarkRunnerTest {

  @Test
  void testBenchmarkCategoryValues() {
    // Test that all expected categories exist
    assertThat(BenchmarkRunner.BenchmarkCategory.values())
        .containsExactly(
            BenchmarkRunner.BenchmarkCategory.RUNTIME,
            BenchmarkRunner.BenchmarkCategory.MODULE,
            BenchmarkRunner.BenchmarkCategory.FUNCTION,
            BenchmarkRunner.BenchmarkCategory.MEMORY,
            BenchmarkRunner.BenchmarkCategory.COMPARISON,
            BenchmarkRunner.BenchmarkCategory.ALL);
  }

  @Test
  void testBenchmarkCategoryPatterns() {
    // Test that category patterns are correctly defined
    assertThat(BenchmarkRunner.BenchmarkCategory.RUNTIME.getPattern())
        .isEqualTo(".*RuntimeInitializationBenchmark.*");
    assertThat(BenchmarkRunner.BenchmarkCategory.MODULE.getPattern())
        .isEqualTo(".*ModuleOperationBenchmark.*");
    assertThat(BenchmarkRunner.BenchmarkCategory.FUNCTION.getPattern())
        .isEqualTo(".*FunctionExecutionBenchmark.*");
    assertThat(BenchmarkRunner.BenchmarkCategory.MEMORY.getPattern())
        .isEqualTo(".*MemoryOperationBenchmark.*");
    assertThat(BenchmarkRunner.BenchmarkCategory.COMPARISON.getPattern())
        .isEqualTo(".*ComparisonBenchmark.*");
    assertThat(BenchmarkRunner.BenchmarkCategory.ALL.getPattern()).isEqualTo(".*");
  }

  @Test
  void testBenchmarkCategoryDescriptions() {
    // Test that all categories have meaningful descriptions
    for (final BenchmarkRunner.BenchmarkCategory category :
        BenchmarkRunner.BenchmarkCategory.values()) {
      assertThat(category.getDescription()).isNotNull().isNotEmpty();
    }
  }

  @Test
  void testBenchmarkProfileValues() {
    // Test that all expected profiles exist
    assertThat(BenchmarkRunner.BenchmarkProfile.values())
        .containsExactly(
            BenchmarkRunner.BenchmarkProfile.QUICK,
            BenchmarkRunner.BenchmarkProfile.STANDARD,
            BenchmarkRunner.BenchmarkProfile.PRODUCTION,
            BenchmarkRunner.BenchmarkProfile.COMPREHENSIVE);
  }

  @Test
  void testBenchmarkProfileConfigurations() {
    // Test QUICK profile
    final BenchmarkRunner.BenchmarkProfile quick = BenchmarkRunner.BenchmarkProfile.QUICK;
    assertThat(quick.getIterations()).isEqualTo(1);
    assertThat(quick.getWarmupIterations()).isEqualTo(1);
    assertThat(quick.getForks()).isEqualTo(1);
    assertThat(quick.getTimePerIteration().getTime()).isEqualTo(1);

    // Test STANDARD profile
    final BenchmarkRunner.BenchmarkProfile standard = BenchmarkRunner.BenchmarkProfile.STANDARD;
    assertThat(standard.getIterations()).isEqualTo(5);
    assertThat(standard.getWarmupIterations()).isEqualTo(3);
    assertThat(standard.getForks()).isEqualTo(2);
    assertThat(standard.getTimePerIteration().getTime()).isEqualTo(2);

    // Test PRODUCTION profile
    final BenchmarkRunner.BenchmarkProfile production = BenchmarkRunner.BenchmarkProfile.PRODUCTION;
    assertThat(production.getIterations()).isEqualTo(10);
    assertThat(production.getWarmupIterations()).isEqualTo(5);
    assertThat(production.getForks()).isEqualTo(3);
    assertThat(production.getTimePerIteration().getTime()).isEqualTo(3);

    // Test COMPREHENSIVE profile
    final BenchmarkRunner.BenchmarkProfile comprehensive =
        BenchmarkRunner.BenchmarkProfile.COMPREHENSIVE;
    assertThat(comprehensive.getIterations()).isEqualTo(15);
    assertThat(comprehensive.getWarmupIterations()).isEqualTo(8);
    assertThat(comprehensive.getForks()).isEqualTo(5);
    assertThat(comprehensive.getTimePerIteration().getTime()).isEqualTo(5);
  }

  @Test
  void testProfileProgressiveComplexity() {
    // Test that profiles have increasing complexity
    final BenchmarkRunner.BenchmarkProfile[] profiles = {
      BenchmarkRunner.BenchmarkProfile.QUICK,
      BenchmarkRunner.BenchmarkProfile.STANDARD,
      BenchmarkRunner.BenchmarkProfile.PRODUCTION,
      BenchmarkRunner.BenchmarkProfile.COMPREHENSIVE
    };

    for (int i = 1; i < profiles.length; i++) {
      final BenchmarkRunner.BenchmarkProfile previous = profiles[i - 1];
      final BenchmarkRunner.BenchmarkProfile current = profiles[i];

      // Each profile should have more iterations than the previous
      assertThat(current.getIterations()).isGreaterThan(previous.getIterations());
      assertThat(current.getWarmupIterations()).isGreaterThan(previous.getWarmupIterations());
      assertThat(current.getForks()).isGreaterThanOrEqualTo(previous.getForks());
      assertThat(current.getTimePerIteration().getTime())
          .isGreaterThanOrEqualTo(previous.getTimePerIteration().getTime());
    }
  }
}
