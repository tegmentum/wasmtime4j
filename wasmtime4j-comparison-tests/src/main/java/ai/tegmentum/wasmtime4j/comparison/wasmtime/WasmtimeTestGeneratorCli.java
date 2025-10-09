package ai.tegmentum.wasmtime4j.comparison.wasmtime;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

/**
 * Command-line tool to discover Wasmtime tests and generate equivalent Java tests.
 *
 * <p>Usage: java WasmtimeTestGeneratorCli [wasmtime-repo-path] [output-directory]
 */
public final class WasmtimeTestGeneratorCli {

  private static final Logger LOGGER = Logger.getLogger(WasmtimeTestGeneratorCli.class.getName());

  /**
   * Main entry point.
   *
   * @param args command line arguments
   */
  public static void main(final String[] args) {
    try {
      final Path wasmtimeRepoPath = getWasmtimeRepoPath(args);
      final Path outputDirectory = getOutputDirectory(args);

      LOGGER.info("Wasmtime repository: " + wasmtimeRepoPath);
      LOGGER.info("Output directory: " + outputDirectory);

      // Discover tests
      final WasmtimeTestDiscovery discovery = new WasmtimeTestDiscovery(wasmtimeRepoPath);
      final List<WasmtimeTestMetadata> tests = discovery.discoverAllTests();

      LOGGER.info("Discovered " + tests.size() + " Wasmtime tests");

      // Print test summary
      printTestSummary(tests);

      // Generate Java tests
      final EquivalentJavaTestGenerator generator =
          new EquivalentJavaTestGenerator(outputDirectory);
      final List<Path> generatedFiles = generator.generateAllTests(tests);

      LOGGER.info("\n========================================");
      LOGGER.info("Test generation complete!");
      LOGGER.info("Generated " + generatedFiles.size() + " Java test files");
      LOGGER.info("Output directory: " + outputDirectory);
      LOGGER.info("========================================\n");

      System.exit(0);

    } catch (final Exception e) {
      LOGGER.severe("Test generation failed: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static Path getWasmtimeRepoPath(final String[] args) {
    if (args.length > 0) {
      return Paths.get(args[0]);
    }

    // Try environment variable
    final String envPath = System.getenv("WASMTIME_REPO_PATH");
    if (envPath != null) {
      return Paths.get(envPath);
    }

    // Try default location
    final String userHome = System.getProperty("user.home");
    return Paths.get(userHome, "git", "wasmtime");
  }

  private static Path getOutputDirectory(final String[] args) {
    if (args.length > 1) {
      return Paths.get(args[1]);
    }

    // Default to src/test/java in current project
    return Paths.get("src/test/java");
  }

  private static void printTestSummary(final List<WasmtimeTestMetadata> tests) {
    LOGGER.info("\n========================================");
    LOGGER.info("Test Discovery Summary");
    LOGGER.info("========================================");

    // Count by category
    final java.util.Map<String, Long> categoryCounts =
        tests.stream()
            .collect(
                java.util.stream.Collectors.groupingBy(
                    WasmtimeTestMetadata::getCategory, java.util.stream.Collectors.counting()));

    categoryCounts.entrySet().stream()
        .sorted(java.util.Map.Entry.comparingByValue(java.util.Collections.reverseOrder()))
        .forEach(
            entry -> {
              LOGGER.info(String.format("  %-30s : %d tests", entry.getKey(), entry.getValue()));
            });

    // Count tests requiring special features
    final long wasiTests = tests.stream().filter(WasmtimeTestMetadata::requiresWasi).count();
    final long componentTests =
        tests.stream().filter(WasmtimeTestMetadata::requiresComponent).count();
    final long threadTests = tests.stream().filter(WasmtimeTestMetadata::requiresThreads).count();
    final long gcTests = tests.stream().filter(WasmtimeTestMetadata::requiresGc).count();

    LOGGER.info("\nFeature Requirements:");
    LOGGER.info("  WASI         : " + wasiTests + " tests");
    LOGGER.info("  Components   : " + componentTests + " tests");
    LOGGER.info("  Threads      : " + threadTests + " tests");
    LOGGER.info("  GC           : " + gcTests + " tests");
    LOGGER.info("========================================\n");
  }
}
