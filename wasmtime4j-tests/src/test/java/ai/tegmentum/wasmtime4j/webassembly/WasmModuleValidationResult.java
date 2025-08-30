package ai.tegmentum.wasmtime4j.webassembly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of WebAssembly module validation with detailed information.
 */
public final class WasmModuleValidationResult {
    private final boolean valid;
    private final int moduleSize;
    private final List<String> errors;
    private final List<String> warnings;
    private final List<String> info;
    
    private WasmModuleValidationResult(final Builder builder) {
        this.valid = builder.valid;
        this.moduleSize = builder.moduleSize;
        this.errors = Collections.unmodifiableList(new ArrayList<>(builder.errors));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(builder.warnings));
        this.info = Collections.unmodifiableList(new ArrayList<>(builder.info));
    }
    
    /**
     * Checks if the module is valid.
     *
     * @return true if the module is valid
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Gets the size of the module in bytes.
     *
     * @return the module size, or -1 if not available
     */
    public int getModuleSize() {
        return moduleSize;
    }
    
    /**
     * Gets the list of validation errors.
     *
     * @return the list of errors
     */
    public List<String> getErrors() {
        return errors;
    }
    
    /**
     * Gets the list of validation warnings.
     *
     * @return the list of warnings
     */
    public List<String> getWarnings() {
        return warnings;
    }
    
    
    /**
     * Gets the list of informational messages.
     *
     * @return the list of informational messages
     */
    public List<String> getInfo() {
        return info;
    }
    
    /**
     * Checks if there are any errors.
     *
     * @return true if there are errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Checks if there are any warnings.
     *
     * @return true if there are warnings
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    /**
     * Checks if there is any informational content.
     *
     * @return true if there is informational content
     */
    public boolean hasInfo() {
        return !info.isEmpty();
    }
    
    /**
     * Gets a summary of the validation result.
     *
     * @return a summary string
     */
    public String getSummary() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Module validation: ").append(valid ? "VALID" : "INVALID");
        
        if (moduleSize >= 0) {
            sb.append(", size: ").append(moduleSize).append(" bytes");
        }
        
        if (!errors.isEmpty()) {
            sb.append(", errors: ").append(errors.size());
        }
        
        if (!warnings.isEmpty()) {
            sb.append(", warnings: ").append(warnings.size());
        }
        
        if (!info.isEmpty()) {
            sb.append(", info: ").append(info.size());
        }
        
        return sb.toString();
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        final WasmModuleValidationResult that = (WasmModuleValidationResult) obj;
        return valid == that.valid &&
               moduleSize == that.moduleSize &&
               Objects.equals(errors, that.errors) &&
               Objects.equals(warnings, that.warnings) &&
               Objects.equals(info, that.info);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(valid, moduleSize, errors, warnings, info);
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("WasmModuleValidationResult{\n");
        sb.append("  valid: ").append(valid).append('\n');
        sb.append("  moduleSize: ").append(moduleSize).append('\n');
        
        if (!errors.isEmpty()) {
            sb.append("  errors:\n");
            for (final String error : errors) {
                sb.append("    - ").append(error).append('\n');
            }
        }
        
        if (!warnings.isEmpty()) {
            sb.append("  warnings:\n");
            for (final String warning : warnings) {
                sb.append("    - ").append(warning).append('\n');
            }
        }
        
        if (!info.isEmpty()) {
            sb.append("  info:\n");
            for (final String infoItem : info) {
                sb.append("    - ").append(infoItem).append('\n');
            }
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Builder for creating validation results.
     */
    public static final class Builder {
        private boolean valid = false;
        private int moduleSize = -1;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> info = new ArrayList<>();
        
        /**
         * Sets whether the module is valid.
         *
         * @param valid true if the module is valid
         * @return this builder
         */
        public Builder valid(final boolean valid) {
            this.valid = valid;
            return this;
        }
        
        /**
         * Sets the module size in bytes.
         *
         * @param moduleSize the module size
         * @return this builder
         */
        public Builder moduleSize(final int moduleSize) {
            this.moduleSize = moduleSize;
            return this;
        }
        
        /**
         * Adds a validation error.
         *
         * @param error the error message
         * @return this builder
         */
        public Builder addError(final String error) {
            Objects.requireNonNull(error, "error cannot be null");
            this.errors.add(error);
            return this;
        }
        
        /**
         * Adds a validation warning.
         *
         * @param warning the warning message
         * @return this builder
         */
        public Builder addWarning(final String warning) {
            Objects.requireNonNull(warning, "warning cannot be null");
            this.warnings.add(warning);
            return this;
        }
        
        /**
         * Adds informational content.
         *
         * @param info the informational message
         * @return this builder
         */
        public Builder addInfo(final String info) {
            Objects.requireNonNull(info, "info cannot be null");
            this.info.add(info);
            return this;
        }
        
        /**
         * Adds multiple errors.
         *
         * @param errors the list of error messages
         * @return this builder
         */
        public Builder addErrors(final List<String> errors) {
            Objects.requireNonNull(errors, "errors cannot be null");
            this.errors.addAll(errors);
            return this;
        }
        
        /**
         * Adds multiple warnings.
         *
         * @param warnings the list of warning messages
         * @return this builder
         */
        public Builder addWarnings(final List<String> warnings) {
            Objects.requireNonNull(warnings, "warnings cannot be null");
            this.warnings.addAll(warnings);
            return this;
        }
        
        /**
         * Adds multiple informational items.
         *
         * @param info the list of informational messages
         * @return this builder
         */
        public Builder addInfo(final List<String> info) {
            Objects.requireNonNull(info, "info cannot be null");
            this.info.addAll(info);
            return this;
        }
        
        /**
         * Builds the validation result.
         *
         * @return the validation result
         */
        public WasmModuleValidationResult build() {
            return new WasmModuleValidationResult(this);
        }
    }
}