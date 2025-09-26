package ai.tegmentum.wasmtime4j.debug;

import java.util.Objects;
import java.util.Optional;

/**
 * Variable information extracted from DWARF debugging data.
 *
 * <p>This class represents low-level variable information from DWARF debugging
 * sections, including location expressions and type references. It provides
 * the raw debugging information that can be used to resolve variable values
 * at runtime.
 *
 * @since 1.0.0
 */
public final class DwarfVariable {

    private final String name;
    private final String type;
    private final Long typeReference;
    private final byte[] locationExpression;

    /**
     * Creates a new DWARF variable.
     *
     * @param name the variable name
     * @param type the variable type (optional, may be resolved from type reference)
     * @param typeReference the DWARF type reference (optional)
     * @param locationExpression the DWARF location expression (optional)
     */
    public DwarfVariable(
            final String name,
            final String type,
            final Long typeReference,
            final byte[] locationExpression) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Variable name cannot be null or empty");
        }

        this.name = name;
        this.type = type;
        this.typeReference = typeReference;
        this.locationExpression = locationExpression != null ? locationExpression.clone() : null;
    }

    /**
     * Creates a simple DWARF variable with just name and type.
     *
     * @param name the variable name
     * @param type the variable type
     */
    public DwarfVariable(final String name, final String type) {
        this(name, type, null, null);
    }

    /**
     * Gets the variable name.
     *
     * @return the variable name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the variable type if available.
     *
     * @return the variable type, or "unknown" if not available
     */
    public String getType() {
        return type != null ? type : "unknown";
    }

    /**
     * Gets the DWARF type reference if available.
     *
     * @return the type reference
     */
    public Optional<Long> getTypeReference() {
        return Optional.ofNullable(typeReference);
    }

    /**
     * Gets the DWARF location expression if available.
     *
     * @return the location expression bytes
     */
    public Optional<byte[]> getLocationExpression() {
        return Optional.ofNullable(locationExpression).map(byte[]::clone);
    }

    /**
     * Checks if this variable has type information.
     *
     * @return true if type information is available
     */
    public boolean hasType() {
        return type != null;
    }

    /**
     * Checks if this variable has a type reference.
     *
     * @return true if type reference is available
     */
    public boolean hasTypeReference() {
        return typeReference != null;
    }

    /**
     * Checks if this variable has location information.
     *
     * @return true if location expression is available
     */
    public boolean hasLocation() {
        return locationExpression != null;
    }

    /**
     * Gets the size of the location expression in bytes.
     *
     * @return the location expression size, or 0 if not available
     */
    public int getLocationSize() {
        return locationExpression != null ? locationExpression.length : 0;
    }

    /**
     * Creates a copy with updated type information.
     *
     * @param newType the new type
     * @return a new variable with updated type
     */
    public DwarfVariable withType(final String newType) {
        return new DwarfVariable(name, newType, typeReference, locationExpression);
    }

    /**
     * Creates a copy with updated type reference.
     *
     * @param newTypeReference the new type reference
     * @return a new variable with updated type reference
     */
    public DwarfVariable withTypeReference(final long newTypeReference) {
        return new DwarfVariable(name, type, newTypeReference, locationExpression);
    }

    /**
     * Creates a copy with updated location expression.
     *
     * @param newLocationExpression the new location expression
     * @return a new variable with updated location expression
     */
    public DwarfVariable withLocationExpression(final byte[] newLocationExpression) {
        return new DwarfVariable(name, type, typeReference, newLocationExpression);
    }

    /**
     * Creates a formatted display string.
     *
     * @param includeLocation whether to include location information
     * @return formatted display string
     */
    public String toDisplayString(final boolean includeLocation) {
        final StringBuilder sb = new StringBuilder();
        sb.append(name).append(": ").append(getType());

        if (includeLocation && hasLocation()) {
            sb.append(" (location: ").append(locationExpression.length).append(" bytes)");
        }

        if (typeReference != null) {
            sb.append(" [type_ref: 0x").append(Long.toHexString(typeReference)).append("]");
        }

        return sb.toString();
    }

    /**
     * Creates a detailed string representation.
     *
     * @return detailed string representation
     */
    public String toDetailedString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(name).append(": ").append(getType());

        if (typeReference != null) {
            sb.append("\n  Type reference: 0x").append(Long.toHexString(typeReference));
        }

        if (hasLocation()) {
            sb.append("\n  Location expression: ").append(locationExpression.length).append(" bytes");
            sb.append("\n  Location bytes: ");
            for (int i = 0; i < Math.min(locationExpression.length, 16); i++) {
                if (i > 0) sb.append(" ");
                sb.append(String.format("%02x", locationExpression[i] & 0xFF));
            }
            if (locationExpression.length > 16) {
                sb.append("...");
            }
        }

        return sb.toString();
    }

    /**
     * Formats the location expression as a hex string.
     *
     * @return hex string representation of location expression, or empty if not available
     */
    public String getLocationHexString() {
        if (locationExpression == null) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        for (final byte b : locationExpression) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    /**
     * Checks if this variable represents a parameter.
     *
     * <p>This is a heuristic based on common DWARF conventions.
     * In practice, the context of where this variable appears
     * (in a parameter list vs local variable list) determines this.
     *
     * @return true if this looks like a parameter
     */
    public boolean isParameter() {
        // This would need more sophisticated DWARF analysis in practice
        // For now, we rely on the context where the variable is used
        return false;
    }

    /**
     * Creates a parameter variable.
     *
     * @param name the parameter name
     * @param type the parameter type
     * @param locationExpression the location expression
     * @return a parameter variable
     */
    public static DwarfVariable parameter(final String name, final String type, final byte[] locationExpression) {
        return new DwarfVariable(name, type, null, locationExpression);
    }

    /**
     * Creates a local variable.
     *
     * @param name the variable name
     * @param type the variable type
     * @param locationExpression the location expression
     * @return a local variable
     */
    public static DwarfVariable local(final String name, final String type, final byte[] locationExpression) {
        return new DwarfVariable(name, type, null, locationExpression);
    }

    /**
     * Creates a variable with type reference.
     *
     * @param name the variable name
     * @param typeReference the type reference
     * @param locationExpression the location expression
     * @return a variable with type reference
     */
    public static DwarfVariable withTypeReference(
            final String name, final long typeReference, final byte[] locationExpression) {
        return new DwarfVariable(name, null, typeReference, locationExpression);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final DwarfVariable that = (DwarfVariable) obj;
        return Objects.equals(name, that.name)
                && Objects.equals(type, that.type)
                && Objects.equals(typeReference, that.typeReference)
                && java.util.Arrays.equals(locationExpression, that.locationExpression);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name, type, typeReference);
        result = 31 * result + java.util.Arrays.hashCode(locationExpression);
        return result;
    }

    @Override
    public String toString() {
        return toDisplayString(false);
    }
}