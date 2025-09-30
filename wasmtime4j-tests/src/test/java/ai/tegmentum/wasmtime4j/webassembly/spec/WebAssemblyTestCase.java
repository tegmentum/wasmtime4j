/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 *
 * This software is the confidential and proprietary information of Tegmentum AI.
 * You may not disclose such confidential information and may only use it in
 * accordance with the terms of the license agreement you entered into with
 * Tegmentum AI.
 */
package ai.tegmentum.wasmtime4j.webassembly.spec;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a WebAssembly test case with comprehensive metadata and expectations. Encapsulates all
 * information needed to execute and validate a WebAssembly test.
 *
 * <p>This class provides comprehensive test case information including:
 *
 * <ul>
 *   <li>Test identification and categorization
 *   <li>WebAssembly module location and metadata
 *   <li>Expected behavior and validation criteria
 *   <li>Test execution parameters and configuration
 * </ul>
 *
 * @since 1.0.0
 */
public final class WebAssemblyTestCase {

  private final String name;
  private final TestType type;
  private final Path wasmPath;
  private final URI testUri;
  private final ExpectedBehavior expectedBehavior;
  private final List<ExpectedResult> expectedResults;
  private final TestMetadata metadata;
  private final String displayName;

  private WebAssemblyTestCase(final Builder builder) {
    this.name = Objects.requireNonNull(builder.name, "name");
    this.type = Objects.requireNonNull(builder.type, "type");
    this.wasmPath = Objects.requireNonNull(builder.wasmPath, "wasmPath");
    this.testUri = builder.testUri != null ? builder.testUri : wasmPath.toUri();
    this.expectedBehavior =
        builder.expectedBehavior != null
            ? builder.expectedBehavior
            : ExpectedBehavior.defaultBehavior();
    this.expectedResults =
        Collections.unmodifiableList(
            builder.expectedResults != null ? builder.expectedResults : Collections.emptyList());
    this.metadata = builder.metadata != null ? builder.metadata : TestMetadata.empty();
    this.displayName = generateDisplayName();
  }

  /**
   * Returns the test case name.
   *
   * @return test case name
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the test case type.
   *
   * @return test case type
   */
  public TestType getType() {
    return type;
  }

  /**
   * Returns the path to the WebAssembly module file.
   *
   * @return WebAssembly module path
   */
  public Path getWasmPath() {
    return wasmPath;
  }

  /**
   * Returns the test case URI for JUnit integration.
   *
   * @return test case URI
   */
  public URI getTestUri() {
    return testUri;
  }

  /**
   * Returns the expected behavior for this test case.
   *
   * @return expected behavior
   */
  public ExpectedBehavior getExpectedBehavior() {
    return expectedBehavior;
  }

  /**
   * Returns the expected results for this test case.
   *
   * @return list of expected results
   */
  public List<ExpectedResult> getExpectedResults() {
    return expectedResults;
  }

  /**
   * Returns the test metadata.
   *
   * @return test metadata
   */
  public TestMetadata getMetadata() {
    return metadata;
  }

  /**
   * Returns the display name for test reporting.
   *
   * @return display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Returns whether this test case expects successful execution.
   *
   * @return true if test expects success, false otherwise
   */
  public boolean expectsSuccess() {
    return expectedBehavior.expectsSuccess();
  }

  /**
   * Returns whether this test case expects an exception.
   *
   * @return true if test expects exception, false otherwise
   */
  public boolean expectsException() {
    return expectedBehavior.expectsException();
  }

  /**
   * Returns whether this test case is enabled for execution.
   *
   * @return true if test is enabled, false otherwise
   */
  public boolean isEnabled() {
    return metadata.isEnabled();
  }

  /**
   * Returns the test timeout in milliseconds.
   *
   * @return test timeout in milliseconds
   */
  public long getTimeoutMillis() {
    return metadata.getTimeoutMillis();
  }

  private String generateDisplayName() {
    final StringBuilder displayName = new StringBuilder();

    // Add test type prefix
    displayName.append("[").append(type.name()).append("] ");

    // Add test name
    displayName.append(name);

    // Add additional context from metadata
    final String description = metadata.getDescription();
    if (description != null && !description.isEmpty()) {
      displayName.append(" - ").append(description);
    }

    return displayName.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final WebAssemblyTestCase other = (WebAssemblyTestCase) obj;
    return Objects.equals(name, other.name)
        && type == other.type
        && Objects.equals(wasmPath, other.wasmPath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, wasmPath);
  }

  @Override
  public String toString() {
    return "WebAssemblyTestCase{"
        + "name='"
        + name
        + '\''
        + ", type="
        + type
        + ", wasmPath="
        + wasmPath
        + ", displayName='"
        + displayName
        + '\''
        + '}';
  }

  /** Builder for creating WebAssembly test cases. */
  public static final class Builder {
    private String name;
    private TestType type;
    private Path wasmPath;
    private URI testUri;
    private ExpectedBehavior expectedBehavior;
    private List<ExpectedResult> expectedResults;
    private TestMetadata metadata;

    /**
     * Sets the test case name.
     *
     * @param name the test case name
     * @return this builder
     */
    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the test case type.
     *
     * @param type the test case type
     * @return this builder
     */
    public Builder type(final TestType type) {
      this.type = type;
      return this;
    }

    /**
     * Sets the WebAssembly module path.
     *
     * @param wasmPath the WebAssembly module path
     * @return this builder
     */
    public Builder wasmPath(final Path wasmPath) {
      this.wasmPath = wasmPath;
      return this;
    }

    /**
     * Sets the test case URI.
     *
     * @param testUri the test case URI
     * @return this builder
     */
    public Builder testUri(final URI testUri) {
      this.testUri = testUri;
      return this;
    }

    /**
     * Sets the expected behavior.
     *
     * @param expectedBehavior the expected behavior
     * @return this builder
     */
    public Builder expectedBehavior(final ExpectedBehavior expectedBehavior) {
      this.expectedBehavior = expectedBehavior;
      return this;
    }

    /**
     * Sets the expected results.
     *
     * @param expectedResults the expected results
     * @return this builder
     */
    public Builder expectedResults(final List<ExpectedResult> expectedResults) {
      this.expectedResults = expectedResults != null ? List.copyOf(expectedResults) : null;
      return this;
    }

    /**
     * Sets the test metadata.
     *
     * @param metadata the test metadata
     * @return this builder
     */
    public Builder metadata(final TestMetadata metadata) {
      this.metadata = metadata;
      return this;
    }

    /**
     * Builds the WebAssembly test case.
     *
     * @return the constructed test case
     * @throws IllegalArgumentException if required fields are missing
     */
    public WebAssemblyTestCase build() {
      return new WebAssemblyTestCase(this);
    }
  }
}
