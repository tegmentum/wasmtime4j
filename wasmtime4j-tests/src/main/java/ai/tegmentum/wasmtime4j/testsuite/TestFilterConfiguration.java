package ai.tegmentum.wasmtime4j.testsuite;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Configuration for filtering WebAssembly test cases during discovery and execution. */
public final class TestFilterConfiguration {

  private final Set<TestCategory> includedCategories;
  private final Set<TestCategory> excludedCategories;
  private final List<String> includedTags;
  private final List<String> excludedTags;
  private final List<Pattern> testNamePatterns;
  private final List<Pattern> excludedNamePatterns;
  private final Set<TestComplexity> allowedComplexities;
  private final long maxExecutionTimeMs;
  private final boolean skipKnownFailures;
  private final List<Predicate<WebAssemblyTestCase>> customFilters;

  private TestFilterConfiguration(final Builder builder) {
    this.includedCategories = Set.copyOf(builder.includedCategories);
    this.excludedCategories = Set.copyOf(builder.excludedCategories);
    this.includedTags = List.copyOf(builder.includedTags);
    this.excludedTags = List.copyOf(builder.excludedTags);
    this.testNamePatterns = List.copyOf(builder.testNamePatterns);
    this.excludedNamePatterns = List.copyOf(builder.excludedNamePatterns);
    this.allowedComplexities = Set.copyOf(builder.allowedComplexities);
    this.maxExecutionTimeMs = builder.maxExecutionTimeMs;
    this.skipKnownFailures = builder.skipKnownFailures;
    this.customFilters = List.copyOf(builder.customFilters);
  }

  /**
   * Checks if the configuration is empty (no filters applied).
   *
   * @return true if no filters are configured
   */
  public boolean isEmpty() {
    return includedCategories.isEmpty()
        && excludedCategories.isEmpty()
        && includedTags.isEmpty()
        && excludedTags.isEmpty()
        && testNamePatterns.isEmpty()
        && excludedNamePatterns.isEmpty()
        && allowedComplexities.containsAll(EnumSet.allOf(TestComplexity.class))
        && maxExecutionTimeMs <= 0
        && !skipKnownFailures
        && customFilters.isEmpty();
  }

  /**
   * Checks if a test case matches the filter criteria.
   *
   * @param testCase test case to check
   * @return true if test case passes all filters
   */
  public boolean matches(final WebAssemblyTestCase testCase) {
    if (testCase == null) {
      return false;
    }

    // Check category inclusion
    if (!includedCategories.isEmpty() && !includedCategories.contains(testCase.getCategory())) {
      return false;
    }

    // Check category exclusion
    if (excludedCategories.contains(testCase.getCategory())) {
      return false;
    }

    // Check tag inclusion
    if (!includedTags.isEmpty()) {
      final boolean hasIncludedTag = includedTags.stream().anyMatch(testCase::hasTag);
      if (!hasIncludedTag) {
        return false;
      }
    }

    // Check tag exclusion
    if (excludedTags.stream().anyMatch(testCase::hasTag)) {
      return false;
    }

    // Check test name patterns
    if (!testNamePatterns.isEmpty()) {
      final boolean matchesPattern =
          testNamePatterns.stream()
              .anyMatch(pattern -> pattern.matcher(testCase.getTestName()).matches());
      if (!matchesPattern) {
        return false;
      }
    }

    // Check excluded name patterns
    if (excludedNamePatterns.stream()
        .anyMatch(pattern -> pattern.matcher(testCase.getTestName()).matches())) {
      return false;
    }

    // Check complexity
    if (!allowedComplexities.contains(testCase.getComplexity())) {
      return false;
    }

    // Check execution time limit
    if (maxExecutionTimeMs > 0 && testCase.getEstimatedExecutionTimeMs() > maxExecutionTimeMs) {
      return false;
    }

    // Check known failures
    if (skipKnownFailures && testCase.getExpected() == TestExpectedResult.FAIL) {
      return false;
    }

    // Apply custom filters
    for (final Predicate<WebAssemblyTestCase> filter : customFilters) {
      if (!filter.test(testCase)) {
        return false;
      }
    }

    return true;
  }

  // Getters
  public Set<TestCategory> getIncludedCategories() {
    return includedCategories;
  }

  public Set<TestCategory> getExcludedCategories() {
    return excludedCategories;
  }

  public List<String> getIncludedTags() {
    return includedTags;
  }

  public List<String> getExcludedTags() {
    return excludedTags;
  }

  public List<Pattern> getTestNamePatterns() {
    return testNamePatterns;
  }

  public List<Pattern> getExcludedNamePatterns() {
    return excludedNamePatterns;
  }

  public Set<TestComplexity> getAllowedComplexities() {
    return allowedComplexities;
  }

  public long getMaxExecutionTimeMs() {
    return maxExecutionTimeMs;
  }

  public boolean isSkipKnownFailures() {
    return skipKnownFailures;
  }

  public List<Predicate<WebAssemblyTestCase>> getCustomFilters() {
    return customFilters;
  }

  /**
   * Creates a new builder for TestFilterConfiguration.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a default filter configuration (no filtering).
   *
   * @return default configuration
   */
  public static TestFilterConfiguration noFiltering() {
    return builder().build();
  }

  /**
   * Creates a CI-optimized filter configuration.
   *
   * @return CI configuration
   */
  public static TestFilterConfiguration forCI() {
    return builder()
        .allowedComplexities(EnumSet.of(TestComplexity.SIMPLE, TestComplexity.MODERATE))
        .maxExecutionTimeMs(30000) // 30 seconds max
        .skipKnownFailures(true)
        .excludedCategories(
            Set.of(
                TestCategory.WASMTIME_PERFORMANCE,
                TestCategory.JAVA_PERFORMANCE,
                TestCategory.STRESS))
        .build();
  }

  /** Builder for TestFilterConfiguration. */
  public static final class Builder {
    private Set<TestCategory> includedCategories = Collections.emptySet();
    private Set<TestCategory> excludedCategories = Collections.emptySet();
    private List<String> includedTags = Collections.emptyList();
    private List<String> excludedTags = Collections.emptyList();
    private List<Pattern> testNamePatterns = Collections.emptyList();
    private List<Pattern> excludedNamePatterns = Collections.emptyList();
    private Set<TestComplexity> allowedComplexities = EnumSet.allOf(TestComplexity.class);
    private long maxExecutionTimeMs = 0; // No limit
    private boolean skipKnownFailures = false;
    private List<Predicate<WebAssemblyTestCase>> customFilters = Collections.emptyList();

    public Builder includedCategories(final Set<TestCategory> categories) {
      this.includedCategories =
          categories != null ? Set.copyOf(categories) : Collections.emptySet();
      return this;
    }

    public Builder excludedCategories(final Set<TestCategory> categories) {
      this.excludedCategories =
          categories != null ? Set.copyOf(categories) : Collections.emptySet();
      return this;
    }

    public Builder includedTags(final List<String> tags) {
      this.includedTags = tags != null ? List.copyOf(tags) : Collections.emptyList();
      return this;
    }

    public Builder excludedTags(final List<String> tags) {
      this.excludedTags = tags != null ? List.copyOf(tags) : Collections.emptyList();
      return this;
    }

    public Builder testNamePatterns(final List<String> patterns) {
      if (patterns == null || patterns.isEmpty()) {
        this.testNamePatterns = Collections.emptyList();
        return this;
      }

      this.testNamePatterns = patterns.stream().map(Pattern::compile).toList();
      return this;
    }

    public Builder excludedNamePatterns(final List<String> patterns) {
      if (patterns == null || patterns.isEmpty()) {
        this.excludedNamePatterns = Collections.emptyList();
        return this;
      }

      this.excludedNamePatterns = patterns.stream().map(Pattern::compile).toList();
      return this;
    }

    public Builder allowedComplexities(final Set<TestComplexity> complexities) {
      this.allowedComplexities =
          complexities != null ? Set.copyOf(complexities) : EnumSet.allOf(TestComplexity.class);
      return this;
    }

    public Builder maxExecutionTimeMs(final long maxExecutionTimeMs) {
      this.maxExecutionTimeMs = Math.max(0, maxExecutionTimeMs);
      return this;
    }

    public Builder skipKnownFailures(final boolean skipKnownFailures) {
      this.skipKnownFailures = skipKnownFailures;
      return this;
    }

    public Builder customFilters(final List<Predicate<WebAssemblyTestCase>> filters) {
      this.customFilters = filters != null ? List.copyOf(filters) : Collections.emptyList();
      return this;
    }

    public TestFilterConfiguration build() {
      return new TestFilterConfiguration(this);
    }
  }
}
