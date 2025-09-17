package ai.tegmentum.wasmtime4j.comparison.reporters;

/** Format-specific validator interface. */
interface FormatValidator {
  /**
   * Validates data against a schema definition.
   *
   * @param data the data to validate
   * @param schema the schema definition
   * @return the validation result
   */
  ValidationResult validate(byte[] data, SchemaDefinition schema);
}
