package ai.tegmentum.wasmtime4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Type adapter for converting between different WIT type definitions during interface evolution.
 *
 * <p>This interface provides comprehensive type adaptation capabilities for WIT interface
 * evolution, enabling safe conversion between different versions of types while maintaining
 * semantic correctness.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Bidirectional type conversion
 *   <li>Validation of conversion compatibility
 *   <li>Conversion metadata tracking
 *   <li>Error handling for incompatible conversions
 * </ul>
 *
 * @since 1.0.0
 */
public interface WitTypeAdapter {

  /**
   * Gets the source type name.
   *
   * @return source type name
   */
  String getSourceTypeName();

  /**
   * Gets the target type name.
   *
   * @return target type name
   */
  String getTargetTypeName();

  /**
   * Gets the adapter type indicating the kind of adaptation performed.
   *
   * @return adapter type
   */
  AdapterType getAdapterType();

  /**
   * Checks if conversion from source to target is supported.
   *
   * @return true if forward conversion is supported
   */
  boolean supportsForwardConversion();

  /**
   * Checks if conversion from target to source is supported.
   *
   * @return true if reverse conversion is supported
   */
  boolean supportsReverseConversion();

  /**
   * Converts a value from source type to target type.
   *
   * @param sourceValue the source value
   * @return converted target value
   * @throws WasmRuntimeException if conversion fails
   */
  WasmValue convertForward(WasmValue sourceValue);

  /**
   * Converts a value from target type to source type.
   *
   * @param targetValue the target value
   * @return converted source value
   * @throws WasmRuntimeException if conversion fails
   */
  WasmValue convertReverse(WasmValue targetValue);

  /**
   * Validates if a source value can be converted to target type.
   *
   * @param sourceValue the source value to validate
   * @return validation result
   */
  AdapterValidationResult validateForwardConversion(WasmValue sourceValue);

  /**
   * Validates if a target value can be converted to source type.
   *
   * @param targetValue the target value to validate
   * @return validation result
   */
  AdapterValidationResult validateReverseConversion(WasmValue targetValue);

  /**
   * Gets conversion metadata for the forward direction.
   *
   * @return forward conversion metadata
   */
  ConversionMetadata getForwardConversionMetadata();

  /**
   * Gets conversion metadata for the reverse direction.
   *
   * @return reverse conversion metadata
   */
  ConversionMetadata getReverseConversionMetadata();

  /**
   * Gets adapter statistics.
   *
   * @return adapter statistics
   */
  AdapterStatistics getStatistics();

  /**
   * Checks if the adapter is lossless in both directions.
   *
   * @return true if no data is lost in conversions
   */
  boolean isLossless();

  /**
   * Gets any limitations or warnings for this adapter.
   *
   * @return list of limitations
   */
  List<String> getLimitations();

  /**
   * Gets mapping information for complex type conversions.
   *
   * @return type mapping information
   */
  Optional<TypeMappingInfo> getTypeMappingInfo();

  /** Resets adapter statistics. */
  void resetStatistics();

  /** Type of adapter based on the conversion it performs. */
  enum AdapterType {
    /** Direct type conversion (e.g., u32 -> s32) */
    DIRECT_CONVERSION,
    /** Structural adaptation (e.g., record field changes) */
    STRUCTURAL_ADAPTATION,
    /** Wrapper/unwrapper (e.g., option -> direct value) */
    WRAPPER_ADAPTATION,
    /** List/array conversion. */
    COLLECTION_ADAPTATION,
    /** Variant/enum conversion. */
    VARIANT_ADAPTATION,
    /** Custom conversion with user-defined logic. */
    CUSTOM_CONVERSION
  }

  /** Result of adapter validation. */
  final class AdapterValidationResult {
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;
    private final Optional<String> suggestion;

    /**
     * Creates a validation result.
     *
     * @param valid whether validation passed
     * @param errors validation errors
     * @param warnings validation warnings
     * @param suggestion optional suggestion for improvement
     */
    public AdapterValidationResult(
        final boolean valid,
        final List<String> errors,
        final List<String> warnings,
        final Optional<String> suggestion) {
      this.valid = valid;
      this.errors = List.copyOf(errors);
      this.warnings = List.copyOf(warnings);
      this.suggestion = suggestion;
    }

    /**
     * Creates a successful validation result.
     *
     * @return successful validation result
     */
    public static AdapterValidationResult success() {
      return new AdapterValidationResult(true, List.of(), List.of(), Optional.empty());
    }

    /**
     * Creates a failed validation result.
     *
     * @param errors validation errors
     * @return failed validation result
     */
    public static AdapterValidationResult failure(final List<String> errors) {
      return new AdapterValidationResult(false, errors, List.of(), Optional.empty());
    }

    /**
     * Creates a validation result with warnings.
     *
     * @param warnings validation warnings
     * @param suggestion optional suggestion
     * @return validation result with warnings
     */
    public static AdapterValidationResult withWarnings(
        final List<String> warnings, final Optional<String> suggestion) {
      return new AdapterValidationResult(true, List.of(), warnings, suggestion);
    }

    /**
     * Checks if validation passed.
     *
     * @return true if valid
     */
    public boolean isValid() {
      return valid;
    }

    /**
     * Gets validation errors.
     *
     * @return list of errors
     */
    public List<String> getErrors() {
      return errors;
    }

    /**
     * Gets validation warnings.
     *
     * @return list of warnings
     */
    public List<String> getWarnings() {
      return warnings;
    }

    /**
     * Gets improvement suggestion.
     *
     * @return optional suggestion
     */
    public Optional<String> getSuggestion() {
      return suggestion;
    }

    /**
     * Checks if there are warnings.
     *
     * @return true if there are warnings
     */
    public boolean hasWarnings() {
      return !warnings.isEmpty();
    }

    @Override
    public String toString() {
      return "AdapterValidationResult{"
          + "valid="
          + valid
          + ", errors="
          + errors.size()
          + ", warnings="
          + warnings.size()
          + '}';
    }
  }

  /** Metadata about a type conversion. */
  interface ConversionMetadata {
    /**
     * Checks if the conversion is lossy.
     *
     * @return true if data may be lost
     */
    boolean isLossy();

    /**
     * Gets the estimated conversion cost.
     *
     * @return conversion cost
     */
    ConversionCost getCost();

    /**
     * Gets any preconditions for the conversion.
     *
     * @return list of preconditions
     */
    List<String> getPreconditions();

    /**
     * Gets any postconditions after conversion.
     *
     * @return list of postconditions
     */
    List<String> getPostconditions();

    /**
     * Gets additional conversion properties.
     *
     * @return conversion properties
     */
    Map<String, Object> getProperties();
  }

  /** Statistics about adapter usage. */
  interface AdapterStatistics {
    /**
     * Gets the total number of forward conversions performed.
     *
     * @return forward conversion count
     */
    long getForwardConversions();

    /**
     * Gets the total number of reverse conversions performed.
     *
     * @return reverse conversion count
     */
    long getReverseConversions();

    /**
     * Gets the number of successful conversions.
     *
     * @return successful conversion count
     */
    long getSuccessfulConversions();

    /**
     * Gets the number of failed conversions.
     *
     * @return failed conversion count
     */
    long getFailedConversions();

    /**
     * Gets the average conversion time in nanoseconds.
     *
     * @return average conversion time
     */
    double getAverageConversionTime();

    /**
     * Gets the total conversion time in nanoseconds.
     *
     * @return total conversion time
     */
    long getTotalConversionTime();

    /**
     * Gets the conversion success rate.
     *
     * @return success rate as percentage (0.0 to 1.0)
     */
    double getSuccessRate();

    /**
     * Gets the last conversion timestamp.
     *
     * @return last conversion time
     */
    Optional<java.time.Instant> getLastConversionTime();
  }

  /** Information about type mapping for complex conversions. */
  interface TypeMappingInfo {
    /**
     * Gets field mappings for record types.
     *
     * @return map of source field to target field
     */
    Map<String, String> getFieldMappings();

    /**
     * Gets variant case mappings.
     *
     * @return map of source case to target case
     */
    Map<String, String> getVariantMappings();

    /**
     * Gets enum value mappings.
     *
     * @return map of source enum value to target enum value
     */
    Map<String, String> getEnumMappings();

    /**
     * Gets default values for new fields.
     *
     * @return map of field name to default value
     */
    Map<String, WasmValue> getDefaultValues();

    /**
     * Gets field transformation functions.
     *
     * @return map of field name to transformation function
     */
    Map<String, String> getFieldTransformations();
  }

  /** Cost estimation for type conversions. */
  enum ConversionCost {
    /** Very low cost - simple copy or reference. */
    VERY_LOW,
    /** Low cost - simple type conversion. */
    LOW,
    /** Medium cost - structural conversion. */
    MEDIUM,
    /** High cost - complex transformation. */
    HIGH,
    /** Very high cost - expensive operations. */
    VERY_HIGH
  }
}
