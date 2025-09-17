package ai.tegmentum.wasmtime4j.comparison.reporters;

/** Types of template components. */
enum ComponentType {
  /** Report header with title and basic info. */
  HEADER,

  /** Title page for formal reports. */
  TITLE_PAGE,

  /** Table of contents for navigation. */
  TABLE_OF_CONTENTS,

  /** Executive summary for high-level overview. */
  EXECUTIVE_SUMMARY,

  /** Summary section with key metrics. */
  SUMMARY,

  /** Report metadata and execution info. */
  METADATA,

  /** Behavioral analysis results. */
  BEHAVIORAL,

  /** Performance analysis results. */
  PERFORMANCE,

  /** Coverage analysis results. */
  COVERAGE,

  /** Key insights and patterns. */
  INSIGHTS,

  /** Recommendations and action items. */
  RECOMMENDATIONS,

  /** Appendix with additional data. */
  APPENDIX,

  /** Report footer. */
  FOOTER,

  /** Custom component type. */
  CUSTOM
}