package ai.tegmentum.wasmtime4j.debug;

import java.util.Objects;
import java.util.Optional;

/**
 * Type information extracted from DWARF debugging data.
 *
 * <p>This class represents type information from DWARF debugging sections,
 * including base types, composite types, and type modifiers. It provides
 * the type system information needed for variable inspection and debugging.
 *
 * @since 1.0.0
 */
public final class DwarfType {

    private final String name;
    private final int tag;
    private final Long size;
    private final Integer encoding;
    private final Long baseType;

    /**
     * Creates a new DWARF type.
     *
     * @param name the type name (optional)
     * @param tag the DWARF tag (DW_TAG_*)
     * @param size the type size in bytes (optional)
     * @param encoding the encoding for base types (optional)
     * @param baseType the base type reference (optional)
     */
    public DwarfType(
            final String name,
            final int tag,
            final Long size,
            final Integer encoding,
            final Long baseType) {
        if (tag < 0) {
            throw new IllegalArgumentException("DWARF tag cannot be negative: " + tag);
        }
        if (size != null && size < 0) {
            throw new IllegalArgumentException("Type size cannot be negative: " + size);
        }
        if (encoding != null && encoding < 0) {
            throw new IllegalArgumentException("Type encoding cannot be negative: " + encoding);
        }

        this.name = name;
        this.tag = tag;
        this.size = size;
        this.encoding = encoding;
        this.baseType = baseType;
    }

    /**
     * Creates a simple DWARF type with name and tag.
     *
     * @param name the type name
     * @param tag the DWARF tag
     */
    public DwarfType(final String name, final int tag) {
        this(name, tag, null, null, null);
    }

    /**
     * Gets the type name if available.
     *
     * @return the type name
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Gets the DWARF tag.
     *
     * @return the DWARF tag (DW_TAG_*)
     */
    public int getTag() {
        return tag;
    }

    /**
     * Gets the type size if available.
     *
     * @return the type size in bytes
     */
    public Optional<Long> getSize() {
        return Optional.ofNullable(size);
    }

    /**
     * Gets the encoding for base types if available.
     *
     * @return the encoding (DW_ATE_*)
     */
    public Optional<Integer> getEncoding() {
        return Optional.ofNullable(encoding);
    }

    /**
     * Gets the base type reference if available.
     *
     * @return the base type reference
     */
    public Optional<Long> getBaseType() {
        return Optional.ofNullable(baseType);
    }

    /**
     * Gets the display name for this type.
     *
     * @return the type name or a generated name based on tag
     */
    public String getDisplayName() {
        if (name != null) {
            return name;
        }
        return getTagName(tag);
    }

    /**
     * Checks if this is a base type.
     *
     * @return true if this is a base type (DW_TAG_base_type)
     */
    public boolean isBaseType() {
        return tag == 0x24; // DW_TAG_base_type
    }

    /**
     * Checks if this is a pointer type.
     *
     * @return true if this is a pointer type (DW_TAG_pointer_type)
     */
    public boolean isPointerType() {
        return tag == 0x0f; // DW_TAG_pointer_type
    }

    /**
     * Checks if this is an array type.
     *
     * @return true if this is an array type (DW_TAG_array_type)
     */
    public boolean isArrayType() {
        return tag == 0x01; // DW_TAG_array_type
    }

    /**
     * Checks if this is a structure type.
     *
     * @return true if this is a structure type (DW_TAG_structure_type)
     */
    public boolean isStructureType() {
        return tag == 0x13; // DW_TAG_structure_type
    }

    /**
     * Checks if this is a union type.
     *
     * @return true if this is a union type (DW_TAG_union_type)
     */
    public boolean isUnionType() {
        return tag == 0x17; // DW_TAG_union_type
    }

    /**
     * Checks if this is an enumeration type.
     *
     * @return true if this is an enumeration type (DW_TAG_enumeration_type)
     */
    public boolean isEnumerationType() {
        return tag == 0x04; // DW_TAG_enumeration_type
    }

    /**
     * Checks if this is a function type.
     *
     * @return true if this is a function type (DW_TAG_subroutine_type)
     */
    public boolean isFunctionType() {
        return tag == 0x15; // DW_TAG_subroutine_type
    }

    /**
     * Checks if this is a typedef.
     *
     * @return true if this is a typedef (DW_TAG_typedef)
     */
    public boolean isTypedef() {
        return tag == 0x16; // DW_TAG_typedef
    }

    /**
     * Gets the encoding name for base types.
     *
     * @return the encoding name if this is a base type
     */
    public Optional<String> getEncodingName() {
        if (encoding == null) {
            return Optional.empty();
        }

        switch (encoding) {
            case 0x01: return Optional.of("address");
            case 0x02: return Optional.of("boolean");
            case 0x03: return Optional.of("complex_float");
            case 0x04: return Optional.of("float");
            case 0x05: return Optional.of("signed");
            case 0x06: return Optional.of("signed_char");
            case 0x07: return Optional.of("unsigned");
            case 0x08: return Optional.of("unsigned_char");
            case 0x09: return Optional.of("imaginary_float");
            case 0x0a: return Optional.of("packed_decimal");
            case 0x0b: return Optional.of("numeric_string");
            case 0x0c: return Optional.of("edited");
            case 0x0d: return Optional.of("signed_fixed");
            case 0x0e: return Optional.of("unsigned_fixed");
            case 0x0f: return Optional.of("decimal_float");
            case 0x10: return Optional.of("UTF");
            default: return Optional.of("unknown_" + encoding);
        }
    }

    /**
     * Creates a formatted display string.
     *
     * @param includeDetails whether to include size and encoding details
     * @return formatted display string
     */
    public String toDisplayString(final boolean includeDetails) {
        final StringBuilder sb = new StringBuilder();
        sb.append(getDisplayName());

        if (includeDetails) {
            if (size != null) {
                sb.append(" (").append(size).append(" bytes)");
            }

            if (encoding != null && isBaseType()) {
                final Optional<String> encodingName = getEncodingName();
                if (encodingName.isPresent()) {
                    sb.append(" [").append(encodingName.get()).append("]");
                }
            }

            if (baseType != null) {
                sb.append(" -> 0x").append(Long.toHexString(baseType));
            }
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
        sb.append(getDisplayName());
        sb.append("\n  DWARF tag: ").append(getTagName(tag)).append(" (0x").append(Integer.toHexString(tag)).append(")");

        if (size != null) {
            sb.append("\n  Size: ").append(size).append(" bytes");
        }

        if (encoding != null) {
            final Optional<String> encodingName = getEncodingName();
            sb.append("\n  Encoding: ").append(encodingName.orElse("unknown"));
            sb.append(" (0x").append(Integer.toHexString(encoding)).append(")");
        }

        if (baseType != null) {
            sb.append("\n  Base type: 0x").append(Long.toHexString(baseType));
        }

        return sb.toString();
    }

    /**
     * Gets the name for a DWARF tag.
     *
     * @param tag the DWARF tag
     * @return the tag name
     */
    private static String getTagName(final int tag) {
        switch (tag) {
            case 0x01: return "array_type";
            case 0x02: return "class_type";
            case 0x03: return "entry_point";
            case 0x04: return "enumeration_type";
            case 0x05: return "formal_parameter";
            case 0x08: return "imported_declaration";
            case 0x0a: return "label";
            case 0x0b: return "lexical_block";
            case 0x0d: return "member";
            case 0x0f: return "pointer_type";
            case 0x10: return "reference_type";
            case 0x11: return "compile_unit";
            case 0x12: return "string_type";
            case 0x13: return "structure_type";
            case 0x15: return "subroutine_type";
            case 0x16: return "typedef";
            case 0x17: return "union_type";
            case 0x18: return "unspecified_parameters";
            case 0x19: return "variant";
            case 0x1a: return "common_block";
            case 0x1b: return "common_inclusion";
            case 0x1c: return "inheritance";
            case 0x1d: return "inlined_subroutine";
            case 0x1e: return "module";
            case 0x1f: return "ptr_to_member_type";
            case 0x20: return "set_type";
            case 0x21: return "subrange_type";
            case 0x22: return "with_stmt";
            case 0x23: return "access_declaration";
            case 0x24: return "base_type";
            case 0x25: return "catch_block";
            case 0x26: return "const_type";
            case 0x27: return "constant";
            case 0x28: return "enumerator";
            case 0x29: return "file_type";
            case 0x2a: return "friend";
            case 0x2b: return "namelist";
            case 0x2c: return "namelist_item";
            case 0x2d: return "packed_type";
            case 0x2e: return "subprogram";
            case 0x2f: return "template_type_parameter";
            case 0x30: return "template_value_parameter";
            case 0x31: return "thrown_type";
            case 0x32: return "try_block";
            case 0x33: return "variant_part";
            case 0x34: return "variable";
            case 0x35: return "volatile_type";
            default: return "unknown_tag_" + Integer.toHexString(tag);
        }
    }

    /**
     * Creates a base type.
     *
     * @param name the type name
     * @param size the type size
     * @param encoding the type encoding
     * @return a base type
     */
    public static DwarfType baseType(final String name, final long size, final int encoding) {
        return new DwarfType(name, 0x24, size, encoding, null);
    }

    /**
     * Creates a pointer type.
     *
     * @param baseType the base type reference
     * @param size the pointer size (typically 4 or 8 bytes)
     * @return a pointer type
     */
    public static DwarfType pointerType(final long baseType, final long size) {
        return new DwarfType(null, 0x0f, size, null, baseType);
    }

    /**
     * Creates a structure type.
     *
     * @param name the structure name
     * @param size the structure size
     * @return a structure type
     */
    public static DwarfType structureType(final String name, final long size) {
        return new DwarfType(name, 0x13, size, null, null);
    }

    /**
     * Creates a typedef.
     *
     * @param name the typedef name
     * @param baseType the base type reference
     * @return a typedef
     */
    public static DwarfType typedef(final String name, final long baseType) {
        return new DwarfType(name, 0x16, null, null, baseType);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final DwarfType dwarfType = (DwarfType) obj;
        return tag == dwarfType.tag
                && Objects.equals(name, dwarfType.name)
                && Objects.equals(size, dwarfType.size)
                && Objects.equals(encoding, dwarfType.encoding)
                && Objects.equals(baseType, dwarfType.baseType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, tag, size, encoding, baseType);
    }

    @Override
    public String toString() {
        return toDisplayString(true);
    }
}