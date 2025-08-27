package ai.tegmentum.wasmtime4j.benchmarks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * Main entry point for running Wasmtime4j benchmarks.
 *
 * <p>This class provides a convenient way to execute benchmarks with various configurations and
 * generate comprehensive performance reports. It supports running individual benchmark categories
 * or complete benchmark suites.
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Run all benchmarks
 * java -cp target/wasmtime4j-benchmarks.jar ai.tegmentum.wasmtime4j.benchmarks.BenchmarkRunner
 *
 * // Run specific benchmark category
 * java -cp target/wasmtime4j-benchmarks.jar ai.tegmentum.wasmtime4j.benchmarks.BenchmarkRunner runtime
 *
 * // Run with custom parameters
 * java -cp target/wasmtime4j-benchmarks.jar ai.tegmentum.wasmtime4j.benchmarks.BenchmarkRunner \
 *   --iterations 10 --warmup 5 --forks 3
 * }</pre>
 */
public final class BenchmarkRunner {

  /** Benchmark category definitions. */
  public enum BenchmarkCategory {
    /** Runtime initialization and engine creation benchmarks. */
    RUNTIME(".*RuntimeInitializationBenchmark.*", "Runtime Initialization"),

    /** WebAssembly module compilation and instantiation benchmarks. */
    MODULE(".*ModuleOperationBenchmark.*", "Module Operations"),

    /** WebAssembly function execution benchmarks. */
    FUNCTION(".*FunctionExecutionBenchmark.*", "Function Execution"),

    /** WebAssembly memory operation benchmarks. */
    MEMORY(".*MemoryOperationBenchmark.*", "Memory Operations"),

    /** Direct JNI vs Panama comparison benchmarks. */
    COMPARISON(".*ComparisonBenchmark.*", "JNI vs Panama Comparison"),

    /** All benchmarks. */
    ALL(".*", "All Benchmarks");

    private final String pattern;
    private final String description;

    BenchmarkCategory(final String pattern, final String description) {
      this.pattern = pattern;
      this.description = description;
    }

    public String getPattern() {
      return pattern;
    }

    public String getDescription() {
      return description;
    }
  }

  /** Benchmark configuration presets. */
  public enum BenchmarkProfile {
    /** Quick benchmarks for development and testing. */
    QUICK(1, 1, 1, TimeValue.seconds(1)),

    /** Standard benchmarks for regular performance monitoring. */
    STANDARD(5, 3, 2, TimeValue.seconds(2)),

    /** Production benchmarks for official performance reports. */
    PRODUCTION(10, 5, 3, TimeValue.seconds(3)),

    /** Comprehensive benchmarks for detailed analysis. */
    COMPREHENSIVE(15, 8, 5, TimeValue.seconds(5));

    private final int iterations;
    private final int warmupIterations;
    private final int forks;
    private final TimeValue timePerIteration;

    BenchmarkProfile(
        final int iterations,
        final int warmupIterations,
        final int forks,
        final TimeValue timePerIteration) {
      this.iterations = iterations;
      this.warmupIterations = warmupIterations;
      this.forks = forks;
      this.timePerIteration = timePerIteration;
    }

    public int getIterations() {
      return iterations;
    }

    public int getWarmupIterations() {
      return warmupIterations;
    }

    public int getForks() {
      return forks;
    }

    public TimeValue getTimePerIteration() {
      return timePerIteration;
    }
  }

  private BenchmarkRunner() {
    // Utility class - prevent instantiation
  }

  /**
   * Main entry point for benchmark execution.
   *
   * @param args command line arguments
   */
  public static void main(final String[] args) {
    try {
      final BenchmarkConfiguration config = parseArguments(args);
      printBenchmarkHeader(config);

      final Options options = buildJmhOptions(config);
      final Runner runner = new Runner(options);

      System.out.println("Starting benchmarks...");
      final Collection<RunResult> results = runner.run();

      generateReport(results, config);
      System.out.println("Benchmarks completed successfully!");

    } catch (final Exception e) {
      System.err.println("Benchmark execution failed: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Parses command line arguments to create benchmark configuration.
   *
   * @param args command line arguments
   * @return parsed configuration
   */
  private static BenchmarkConfiguration parseArguments(final String[] args) {
    final BenchmarkConfiguration config = new BenchmarkConfiguration();

    for (int i = 0; i < args.length; i++) {
      final String arg = args[i];

      switch (arg.toLowerCase()) {
        case "runtime":
          config.category = BenchmarkCategory.RUNTIME;
          break;
        case "module":
          config.category = BenchmarkCategory.MODULE;
          break;
        case "function":
          config.category = BenchmarkCategory.FUNCTION;
          break;
        case "memory":
          config.category = BenchmarkCategory.MEMORY;
          break;
        case "comparison":
          config.category = BenchmarkCategory.COMPARISON;
          break;
        case "all":
          config.category = BenchmarkCategory.ALL;
          break;
        case "--profile":
          if (i + 1 < args.length) {
            config.profile = BenchmarkProfile.valueOf(args[++i].toUpperCase());
          }
          break;
        case "--iterations":
          if (i + 1 < args.length) {
            config.customIterations = Integer.parseInt(args[++i]);
          }
          break;
        case "--warmup":
          if (i + 1 < args.length) {
            config.customWarmupIterations = Integer.parseInt(args[++i]);
          }
          break;
        case "--forks":
          if (i + 1 < args.length) {
            config.customForks = Integer.parseInt(args[++i]);
          }
          break;
        case "--output":
          if (i + 1 < args.length) {
            config.outputFile = args[++i];
          }
          break;
        case "--help":
          printUsage();
          System.exit(0);
          break;
        default:
          // Try to parse as category name if no other match
          try {
            config.category = BenchmarkCategory.valueOf(arg.toUpperCase());
          } catch (final IllegalArgumentException e) {
            System.err.println("Unknown argument: " + arg);
            printUsage();
            System.exit(1);
          }
          break;
      }
    }

    return config;
  }

  /**
   * Builds JMH options from configuration.
   *
   * @param config benchmark configuration
   * @return JMH options
   */
  private static Options buildJmhOptions(final BenchmarkConfiguration config) {
    ChainedOptionsBuilder builder =
        new OptionsBuilder()
            .include(config.category.getPattern())
            .mode(org.openjdk.jmh.annotations.Mode.Throughput)
            .timeUnit(TimeUnit.SECONDS)
            // Apply profile settings or custom settings
            .measurementIterations(
                config.customIterations != null
                    ? config.customIterations
                    : config.profile.getIterations())
            .warmupIterations(
                config.customWarmupIterations != null
                    ? config.customWarmupIterations
                    : config.profile.getWarmupIterations())
            .forks(config.customForks != null ? config.customForks : config.profile.getForks())
            .measurementTime(config.profile.getTimePerIteration())
            .warmupTime(config.profile.getTimePerIteration())
            .jvmArgs("-Xms2g", "-Xmx4g", "-XX:+UseG1GC");

    // Set output file if specified
    if (config.outputFile != null) {
      builder =
          builder
              .result(config.outputFile)
              .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON);
    }

    return builder.build();
  }

  /**
   * Prints benchmark header with configuration information.
   *
   * @param config benchmark configuration
   */
  private static void printBenchmarkHeader(final BenchmarkConfiguration config) {
    System.out.println("=".repeat(80));
    System.out.println("Wasmtime4j Performance Benchmarks");
    System.out.println("=".repeat(80));
    System.out.println("Category: " + config.category.getDescription());
    System.out.println("Profile: " + config.profile);
    System.out.println("Java Version: " + System.getProperty("java.version"));
    System.out.println(
        "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
    System.out.println("Architecture: " + System.getProperty("os.arch"));
    System.out.println("Available Processors: " + Runtime.getRuntime().availableProcessors());
    System.out.println("Max Memory: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "MB");
    System.out.println(
        "Start Time: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    System.out.println("=".repeat(80));
  }

  /**
   * Generates a summary report from benchmark results.
   *
   * @param results benchmark results
   * @param config benchmark configuration
   */
  private static void generateReport(
      final Collection<RunResult> results, final BenchmarkConfiguration config) {
    if (results.isEmpty()) {
      System.out.println("No benchmark results to report.");
      return;
    }

    System.out.println("\n" + "=".repeat(80));
    System.out.println("Benchmark Results Summary");
    System.out.println("=".repeat(80));

    for (final RunResult result : results) {
      final String benchmarkName = result.getPrimaryResult().getLabel();
      final double score = result.getPrimaryResult().getScore();
      final double error = result.getPrimaryResult().getScoreError();
      final String unit = result.getPrimaryResult().getScoreUnit();

      System.out.printf("%-50s: %10.2f ± %8.2f %s%n", benchmarkName, score, error, unit);
    }

    System.out.println("=".repeat(80));
    System.out.println("Total benchmarks executed: " + results.size());
    System.out.println(
        "Completed: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

    // Save detailed report if output file was specified
    if (config.outputFile != null) {
      System.out.println("Detailed results saved to: " + config.outputFile);
      saveSummaryReport(results, config);
    }
  }

  /**
   * Saves a human-readable summary report.
   *
   * @param results benchmark results
   * @param config benchmark configuration
   */
  private static void saveSummaryReport(
      final Collection<RunResult> results, final BenchmarkConfiguration config) {
    try {
      final String summaryFileName = config.outputFile.replace(".json", "_summary.txt");
      final Path summaryPath = Paths.get(summaryFileName);

      final StringBuilder report = new StringBuilder();
      report.append("Wasmtime4j Benchmark Summary Report\n");
      report.append("Generated: ").append(LocalDateTime.now()).append("\n");
      report.append("Category: ").append(config.category.getDescription()).append("\n");
      report.append("Profile: ").append(config.profile).append("\n\n");

      for (final RunResult result : results) {
        report.append(
            String.format(
                "%-50s: %10.2f ± %8.2f %s%n",
                result.getPrimaryResult().getLabel(),
                result.getPrimaryResult().getScore(),
                result.getPrimaryResult().getScoreError(),
                result.getPrimaryResult().getScoreUnit()));
      }

      Files.write(summaryPath, report.toString().getBytes());
      System.out.println("Summary report saved to: " + summaryFileName);

    } catch (final IOException e) {
      System.err.println("Failed to save summary report: " + e.getMessage());
    }
  }

  /** Prints usage information. */
  private static void printUsage() {
    System.out.println("Wasmtime4j Benchmark Runner");
    System.out.println(
        "\nUsage: java -cp wasmtime4j-benchmarks.jar BenchmarkRunner [options] [category]");
    System.out.println("\nCategories:");
    for (final BenchmarkCategory category : BenchmarkCategory.values()) {
      System.out.println(
          String.format("  %-12s - %s", category.name().toLowerCase(), category.getDescription()));
    }
    System.out.println("\nOptions:");
    System.out.println(
        "  --profile <profile>     Benchmark profile: "
            + Arrays.toString(BenchmarkProfile.values()));
    System.out.println("  --iterations <n>        Number of measurement iterations");
    System.out.println("  --warmup <n>            Number of warmup iterations");
    System.out.println("  --forks <n>             Number of benchmark forks");
    System.out.println("  --output <file>         Output file for detailed results (JSON format)");
    System.out.println("  --help                  Show this help message");
    System.out.println("\nExamples:");
    System.out.println("  java -cp wasmtime4j-benchmarks.jar BenchmarkRunner");
    System.out.println("  java -cp wasmtime4j-benchmarks.jar BenchmarkRunner runtime");
    System.out.println(
        "  java -cp wasmtime4j-benchmarks.jar BenchmarkRunner --profile QUICK comparison");
    System.out.println(
        "  java -cp wasmtime4j-benchmarks.jar BenchmarkRunner --output results.json all");
  }

  /** Benchmark configuration holder. */
  private static final class BenchmarkConfiguration {
    BenchmarkCategory category = BenchmarkCategory.ALL;
    BenchmarkProfile profile = BenchmarkProfile.STANDARD;
    Integer customIterations;
    Integer customWarmupIterations;
    Integer customForks;
    String outputFile;
  }
}
