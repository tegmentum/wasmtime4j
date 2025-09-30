package ai.tegmentum.wasmtime4j.testsuite;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Represents a single WebAssembly test case with comprehensive metadata. */
public final class WebAssemblyTestCase {

  private final String testId;
  private final String testName;
  private final TestCategory category;
  private final Path testFilePath;
  private final String description;
  private final TestExpectedResult expected;
  private final List<String> tags;
  private final Map<String, Object> metadata;
  private final List<WebAssemblyTestCase> subTests;
  private final TestComplexity complexity;
  private final long estimatedExecutionTimeMs;

  private WebAssemblyTestCase(final Builder builder) {
    this.testId = builder.testId;
    this.testName = builder.testName;
    this.category = builder.category;
    this.testFilePath = builder.testFilePath;
    this.description = builder.description;
    this.expected = builder.expected;
    this.tags = List.copyOf(builder.tags);
    this.metadata = Map.copyOf(builder.metadata);
    this.subTests = List.copyOf(builder.subTests);
    this.complexity = builder.complexity;
    this.estimatedExecutionTimeMs = builder.estimatedExecutionTimeMs;
  }

  // Getters
  public String getTestId() {
    return testId;
  }

  public String getTestName() {
    return testName;
  }

  public TestCategory getCategory() {
    return category;
  }

  public Path getTestFilePath() {
    return testFilePath;
  }

  public String getDescription() {
    return description;
  }

  public TestExpectedResult getExpected() {
    return expected;
  }

  public List<String> getTags() {
    return tags;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public List<WebAssemblyTestCase> getSubTests() {
    return subTests;
  }

  public TestComplexity getComplexity() {
    return complexity;
  }

  public long getEstimatedExecutionTimeMs() {
    return estimatedExecutionTimeMs;
  }

  /**
   * Checks if this test case has any sub-tests.
   *
   * @return true if sub-tests exist
   */
  public boolean hasSubTests() {
    return !subTests.isEmpty();
  }

  /**
   * Checks if this test case has the specified tag.
   *
   * @param tag tag to check
   * @return true if tag exists
   */
  public boolean hasTag(final String tag) {
    return tags.contains(tag);
  }

  /**
   * Gets metadata value by key.
   *
   * @param key metadata key
   * @param <T> expected value type
   * @return metadata value or null if not found
   */
  @SuppressWarnings("unchecked")
  public <T> T getMetadataValue(final String key) {
    return (T) metadata.get(key);
  }

  /**
   * Creates a new builder for WebAssemblyTestCase.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final WebAssemblyTestCase that = (WebAssemblyTestCase) o;
    return Objects.equals(testId, that.testId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(testId);
  }

  @Override
  public String toString() {
    return "WebAssemblyTestCase{"
        + "testId='"
        + testId
        + '\''
        + ", testName='"
        + testName
        + '\''
        + ", category="
        + category
        + ", expected="
        + expected
        + ", complexity="
        + complexity
        + ", tags="
        + tags.size()
        + ", subTests="
        + subTests.size()
        + '}';
  }

  /** Builder for WebAssemblyTestCase. */
  public static final class Builder {
    private String testId;
    private String testName;
    private TestCategory category = TestCategory.CUSTOM;
    private Path testFilePath;
    private String description = "";
    private TestExpectedResult expected = TestExpectedResult.PASS;
    private List<String> tags = List.of();
    private Map<String, Object> metadata = Map.of();
    private List<WebAssemblyTestCase> subTests = List.of();
    private TestComplexity complexity = TestComplexity.SIMPLE;
    private long estimatedExecutionTimeMs = 1000;

    public Builder testId(final String testId) {
      if (testId == null || testId.trim().isEmpty()) {
        throw new IllegalArgumentException("Test ID cannot be null or empty");
      }
      this.testId = testId.trim();
      return this;
    }

    public Builder testName(final String testName) {
      if (testName == null || testName.trim().isEmpty()) {
        throw new IllegalArgumentException("Test name cannot be null or empty");
      }
      this.testName = testName.trim();
      return this;
    }

    public Builder category(final TestCategory category) {
      if (category == null) {
        throw new IllegalArgumentException("Category cannot be null");
      }
      this.category = category;
      return this;
    }

    public Builder testFilePath(final Path testFilePath) {
      this.testFilePath = testFilePath;
      return this;
    }

    public Builder description(final String description) {
      this.description = description != null ? description : "";
      return this;
    }

    public Builder expected(final TestExpectedResult expected) {
      if (expected == null) {
        throw new IllegalArgumentException("Expected result cannot be null");
      }
      this.expected = expected;
      return this;
    }

    public Builder tags(final List<String> tags) {
      this.tags = tags != null ? List.copyOf(tags) : List.of();
      return this;
    }

    public Builder metadata(final Map<String, Object> metadata) {
      this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
      return this;
    }

    public Builder subTests(final List<WebAssemblyTestCase> subTests) {
      this.subTests = subTests != null ? List.copyOf(subTests) : List.of();
      return this;
    }

    public Builder complexity(final TestComplexity complexity) {
      if (complexity == null) {
        throw new IllegalArgumentException("Complexity cannot be null");
      }
      this.complexity = complexity;
      return this;
    }

    public Builder estimatedExecutionTimeMs(final long estimatedExecutionTimeMs) {
      if (estimatedExecutionTimeMs < 0) {
        throw new IllegalArgumentException("Estimated execution time cannot be negative");
      }
      this.estimatedExecutionTimeMs = estimatedExecutionTimeMs;
      return this;
    }

    public WebAssemblyTestCase build() {
      if (testId == null || testId.trim().isEmpty()) {
        throw new IllegalStateException("Test ID must be set");
      }
      if (testName == null || testName.trim().isEmpty()) {
        throw new IllegalStateException("Test name must be set");
      }

      // Auto-infer complexity based on sub-tests and tags
      if (complexity == TestComplexity.SIMPLE && determineComplexity() != TestComplexity.SIMPLE) {
        this.complexity = determineComplexity();
      }

      return new WebAssemblyTestCase(this);
    }

    private TestComplexity determineComplexity() {
      if (!subTests.isEmpty()) {
        return subTests.size() > 10 ? TestComplexity.COMPLEX : TestComplexity.MODERATE;
      }

      final int complexityScore = calculateComplexityScore();
      if (complexityScore > 5) {
        return TestComplexity.COMPLEX;
      } else if (complexityScore > 2) {
        return TestComplexity.MODERATE;
      }
      return TestComplexity.SIMPLE;
    }

    private int calculateComplexityScore() {
      int score = 0;

      // Score based on tags indicating complex features
      for (final String tag : tags) {
        switch (tag.toLowerCase()) {
          case "simd", "threading", "atomic", "gc", "component" -> score += 2;
          case "memory", "table", "import", "export" -> score += 1;
          default -> {
            // Other tags don't affect complexity score
          }
        }
      }

      // Score based on estimated execution time
      if (estimatedExecutionTimeMs > 10000) {
        score += 2;
      } else if (estimatedExecutionTimeMs > 5000) {
        score += 1;
      }

      return score;
    }
  }
}
