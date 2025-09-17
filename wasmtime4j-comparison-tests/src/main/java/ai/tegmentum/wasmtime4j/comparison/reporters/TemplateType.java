package ai.tegmentum.wasmtime4j.comparison.reporters;

/** Types of report templates. */
enum TemplateType {
  /** HTML template for web-based reports. */
  HTML,

  /** JSON template for structured data export. */
  JSON,

  /** CSV template for tabular data export. */
  CSV,

  /** Console template for command-line output. */
  CONSOLE,

  /** Plain text template for simple reports. */
  TEXT
}
