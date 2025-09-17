package ai.tegmentum.wasmtime4j.comparison.reporters;

/** Types of differences in a diff comparison. */
enum DiffType {
  /** Line exists in both sides and is identical. */
  UNCHANGED("diff-unchanged"),

  /** Line was added in the right side. */
  ADDED("diff-added"),

  /** Line was removed from the left side. */
  REMOVED("diff-removed"),

  /** Line exists in both sides but with different content. */
  MODIFIED("diff-modified");

  private final String cssClass;

  DiffType(final String cssClass) {
    this.cssClass = cssClass;
  }

  public String getCssClass() {
    return cssClass;
  }
}