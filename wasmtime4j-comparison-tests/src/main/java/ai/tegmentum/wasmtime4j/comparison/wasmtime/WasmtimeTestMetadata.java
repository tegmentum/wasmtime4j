package ai.tegmentum.wasmtime4j.comparison.wasmtime;

import java.nio.file.Path;
import java.util.List;

/**
 * Metadata about a Wasmtime upstream test that should have an equivalent wasmtime4j test.
 *
 * <p>This class represents a single test from the upstream Wasmtime repository at
 * https://github.com/bytecodealliance/wasmtime that needs to be replicated in wasmtime4j to ensure
 * behavioral equivalence.
 */
public final class WasmtimeTestMetadata {

  private final String testName;
  private final String category;
  private final Path sourceFile;
  private final int lineNumber;
  private final String watCode;
  private final List<String> expectedResults;
  private final boolean requiresWasi;
  private final boolean requiresComponent;
  private final boolean requiresThreads;
  private final boolean requiresGc;

  private WasmtimeTestMetadata(final Builder builder) {
    this.testName = builder.testName;
    this.category = builder.category;
    this.sourceFile = builder.sourceFile;
    this.lineNumber = builder.lineNumber;
    this.watCode = builder.watCode;
    this.expectedResults = List.copyOf(builder.expectedResults);
    this.requiresWasi = builder.requiresWasi;
    this.requiresComponent = builder.requiresComponent;
    this.requiresThreads = builder.requiresThreads;
    this.requiresGc = builder.requiresGc;
  }

  public String getTestName() {
    return testName;
  }

  public String getCategory() {
    return category;
  }

  public Path getSourceFile() {
    return sourceFile;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public String getWatCode() {
    return watCode;
  }

  public List<String> getExpectedResults() {
    return expectedResults;
  }

  public boolean requiresWasi() {
    return requiresWasi;
  }

  public boolean requiresComponent() {
    return requiresComponent;
  }

  public boolean requiresThreads() {
    return requiresThreads;
  }

  public boolean requiresGc() {
    return requiresGc;
  }

  /**
   * Gets a unique identifier for this test.
   *
   * @return unique test identifier in format "category::testName"
   */
  public String getTestId() {
    return category + "::" + testName;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Builder for WasmtimeTestMetadata. */
  public static final class Builder {
    private String testName;
    private String category;
    private Path sourceFile;
    private int lineNumber;
    private String watCode;
    private List<String> expectedResults = List.of();
    private boolean requiresWasi;
    private boolean requiresComponent;
    private boolean requiresThreads;
    private boolean requiresGc;

    public Builder testName(final String testName) {
      this.testName = testName;
      return this;
    }

    public Builder category(final String category) {
      this.category = category;
      return this;
    }

    public Builder sourceFile(final Path sourceFile) {
      this.sourceFile = sourceFile;
      return this;
    }

    public Builder lineNumber(final int lineNumber) {
      this.lineNumber = lineNumber;
      return this;
    }

    public Builder watCode(final String watCode) {
      this.watCode = watCode;
      return this;
    }

    public Builder expectedResults(final List<String> expectedResults) {
      this.expectedResults = expectedResults;
      return this;
    }

    public Builder requiresWasi(final boolean requiresWasi) {
      this.requiresWasi = requiresWasi;
      return this;
    }

    public Builder requiresComponent(final boolean requiresComponent) {
      this.requiresComponent = requiresComponent;
      return this;
    }

    public Builder requiresThreads(final boolean requiresThreads) {
      this.requiresThreads = requiresThreads;
      return this;
    }

    public Builder requiresGc(final boolean requiresGc) {
      this.requiresGc = requiresGc;
      return this;
    }

    /**
     * Builds the WasmtimeTestMetadata instance.
     *
     * @return new WasmtimeTestMetadata instance
     */
    public WasmtimeTestMetadata build() {
      if (testName == null || testName.isEmpty()) {
        throw new IllegalArgumentException("testName is required");
      }
      if (category == null || category.isEmpty()) {
        throw new IllegalArgumentException("category is required");
      }
      if (sourceFile == null) {
        throw new IllegalArgumentException("sourceFile is required");
      }
      return new WasmtimeTestMetadata(this);
    }
  }
}
