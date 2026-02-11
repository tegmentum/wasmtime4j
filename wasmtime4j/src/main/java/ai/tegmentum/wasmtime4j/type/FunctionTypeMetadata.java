package ai.tegmentum.wasmtime4j.type;

import java.util.Map;
import java.util.Optional;

/**
 * Represents additional metadata associated with function types.
 *
 * <p>Function type metadata provides extended information about function signatures that goes
 * beyond basic parameter and return types, including:
 *
 * <ul>
 *   <li>Parameter names and documentation
 *   <li>Performance characteristics hints
 *   <li>Calling convention information
 *   <li>Type constraints and invariants
 * </ul>
 *
 * @since 1.1.0
 */
public interface FunctionTypeMetadata {

  /**
   * Gets the names of function parameters, if available.
   *
   * @return array of parameter names, or empty array if not available
   */
  String[] getParameterNames();

  /**
   * Gets the names of function results, if available.
   *
   * @return array of result names, or empty array if not available
   */
  String[] getResultNames();

  /**
   * Gets documentation for a specific parameter.
   *
   * @param parameterIndex the parameter index
   * @return parameter documentation, or empty if not available
   */
  Optional<String> getParameterDocumentation(final int parameterIndex);

  /**
   * Gets documentation for a specific result.
   *
   * @param resultIndex the result index
   * @return result documentation, or empty if not available
   */
  Optional<String> getResultDocumentation(final int resultIndex);

  /**
   * Gets the calling convention for this function type.
   *
   * @return the calling convention
   */
  CallingConvention getCallingConvention();

  /**
   * Gets performance hints associated with this function type.
   *
   * @return performance hints
   */
  PerformanceHints getPerformanceHints();

  /**
   * Gets additional custom attributes.
   *
   * @return map of custom attributes
   */
  Map<String, Object> getCustomAttributes();

  /**
   * Checks if this function type has specific metadata.
   *
   * @param metadataType the type of metadata to check for
   * @return true if the metadata is present
   */
  boolean hasMetadata(final MetadataType<?> metadataType);

  /**
   * Gets metadata of a specific type.
   *
   * @param metadataType the type of metadata to retrieve
   * @param <T> the metadata value type
   * @return the metadata value, or empty if not present
   */
  <T> Optional<T> getMetadata(final MetadataType<T> metadataType);

  /** Calling convention enumeration. */
  enum CallingConvention {
    /** Standard WebAssembly calling convention. */
    WASM_STANDARD,
    /** Fast calling convention for performance. */
    FAST_CALL,
    /** C-style calling convention. */
    C_CALL,
    /** System calling convention. */
    SYSTEM_CALL,
    /** Custom calling convention. */
    CUSTOM
  }

  /** Performance hints for function types. */
  interface PerformanceHints {
    /** Estimated execution cost (relative scale). */
    int getEstimatedCost();

    /** Whether this function is expected to be called frequently. */
    boolean isHotPath();

    /** Whether this function performs I/O operations. */
    boolean performsIO();

    /** Whether this function is pure (no side effects). */
    boolean isPure();

    /** Memory allocation behavior. */
    MemoryBehavior getMemoryBehavior();

    /** Memory allocation behavior of functions. */
    enum MemoryBehavior {
      /** Function doesn't allocate memory. */
      NO_ALLOCATION,
      /** Function allocates a small amount of memory. */
      LIGHT_ALLOCATION,
      /** Function allocates significant memory. */
      HEAVY_ALLOCATION,
      /** Memory allocation behavior is unknown. */
      UNKNOWN
    }
  }

  /** Type-safe metadata key. */
  class MetadataType<T> {
    private final String name;
    private final Class<T> valueType;

    public MetadataType(final String name, final Class<T> valueType) {
      this.name = name;
      this.valueType = valueType;
    }

    public String getName() {
      return name;
    }

    public Class<T> getValueType() {
      return valueType;
    }

    // Common metadata types
    public static final MetadataType<String> DOCUMENTATION =
        new MetadataType<>("documentation", String.class);
    public static final MetadataType<String> VERSION = new MetadataType<>("version", String.class);
    public static final MetadataType<Boolean> DEPRECATED =
        new MetadataType<>("deprecated", Boolean.class);
    public static final MetadataType<Integer> PRIORITY =
        new MetadataType<>("priority", Integer.class);
  }

  /**
   * Creates function type metadata.
   *
   * @return a metadata builder
   */
  static Builder builder() {
    return new Builder();
  }

  /** Builder for function type metadata. */
  class Builder {
    private String[] parameterNames = new String[0];
    private String[] resultNames = new String[0];
    private String[] parameterDocs = new String[0];
    private String[] resultDocs = new String[0];
    private CallingConvention callingConvention = CallingConvention.WASM_STANDARD;
    private PerformanceHints performanceHints = createDefaultPerformanceHints();
    private final Map<String, Object> customAttributes = new java.util.HashMap<>();
    private final Map<MetadataType<?>, Object> metadata = new java.util.HashMap<>();

    public Builder parameterNames(final String... names) {
      this.parameterNames = names.clone();
      return this;
    }

    public Builder resultNames(final String... names) {
      this.resultNames = names.clone();
      return this;
    }

    public Builder parameterDocumentation(final String... docs) {
      this.parameterDocs = docs.clone();
      return this;
    }

    public Builder resultDocumentation(final String... docs) {
      this.resultDocs = docs.clone();
      return this;
    }

    public Builder callingConvention(final CallingConvention convention) {
      this.callingConvention = convention;
      return this;
    }

    public Builder performanceHints(final PerformanceHints hints) {
      this.performanceHints = hints;
      return this;
    }

    public Builder customAttribute(final String key, final Object value) {
      this.customAttributes.put(key, value);
      return this;
    }

    public <T> Builder metadata(final MetadataType<T> type, final T value) {
      this.metadata.put(type, value);
      return this;
    }

    /**
     * Builds the function type metadata instance.
     *
     * @return the configured function type metadata
     */
    public FunctionTypeMetadata build() {
      return new FunctionTypeMetadata() {
        @Override
        public String[] getParameterNames() {
          return parameterNames.clone();
        }

        @Override
        public String[] getResultNames() {
          return resultNames.clone();
        }

        @Override
        public Optional<String> getParameterDocumentation(final int parameterIndex) {
          if (parameterIndex >= 0 && parameterIndex < parameterDocs.length) {
            return Optional.ofNullable(parameterDocs[parameterIndex]);
          }
          return Optional.empty();
        }

        @Override
        public Optional<String> getResultDocumentation(final int resultIndex) {
          if (resultIndex >= 0 && resultIndex < resultDocs.length) {
            return Optional.ofNullable(resultDocs[resultIndex]);
          }
          return Optional.empty();
        }

        @Override
        public CallingConvention getCallingConvention() {
          return callingConvention;
        }

        @Override
        public PerformanceHints getPerformanceHints() {
          return performanceHints;
        }

        @Override
        public Map<String, Object> getCustomAttributes() {
          return new java.util.HashMap<>(customAttributes);
        }

        @Override
        public boolean hasMetadata(final MetadataType<?> metadataType) {
          return metadata.containsKey(metadataType);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> getMetadata(final MetadataType<T> metadataType) {
          return Optional.ofNullable((T) metadata.get(metadataType));
        }
      };
    }

    private static PerformanceHints createDefaultPerformanceHints() {
      return new PerformanceHints() {
        @Override
        public int getEstimatedCost() {
          return 1; // Low cost by default
        }

        @Override
        public boolean isHotPath() {
          return false;
        }

        @Override
        public boolean performsIO() {
          return false;
        }

        @Override
        public boolean isPure() {
          return true;
        }

        @Override
        public MemoryBehavior getMemoryBehavior() {
          return MemoryBehavior.UNKNOWN;
        }
      };
    }
  }
}
