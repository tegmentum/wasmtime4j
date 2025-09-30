package ai.tegmentum.wasmtime4j.experimental;

/**
 * String encoding formats for string imports.
 *
 * @since 1.0.0
 */
public enum StringEncodingFormat {
    /** UTF-8 encoding. */
    UTF8("utf8", "UTF-8 encoding"),

    /** UTF-16 encoding. */
    UTF16("utf16", "UTF-16 encoding"),

    /** Latin-1 encoding. */
    LATIN1("latin1", "Latin-1 encoding"),

    /** Custom encoding format. */
    CUSTOM("custom", "Custom encoding format");

    private final String key;
    private final String description;

    StringEncodingFormat(final String key, final String description) {
        this.key = key;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return key + " (" + description + ")";
    }
}