package ai.tegmentum.wasmtime4j.debug;

import java.util.Objects;

/**
 * Represents a breakpoint in WebAssembly debugging.
 * Supports both byte-offset and source-location breakpoints with conditional expressions.
 */
public final class Breakpoint {
    private final int id;
    private final BreakpointType type;
    private final String sourceFile;
    private final int sourceLine;
    private final int sourceColumn;
    private final int byteOffset;
    private final String condition;
    private final boolean enabled;
    private final long hitCount;

    public Breakpoint(final int id, final BreakpointType type, final String sourceFile,
                     final int sourceLine, final int sourceColumn, final int byteOffset,
                     final String condition, final boolean enabled, final long hitCount) {
        this.id = id;
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        this.sourceFile = sourceFile;
        this.sourceLine = sourceLine;
        this.sourceColumn = sourceColumn;
        this.byteOffset = byteOffset;
        this.condition = condition;
        this.enabled = enabled;
        this.hitCount = hitCount;
    }

    public Breakpoint withEnabled(final boolean enabled) {
        return new Breakpoint(id, type, sourceFile, sourceLine, sourceColumn,
                            byteOffset, condition, enabled, hitCount);
    }

    public Breakpoint withHitCount(final long hitCount) {
        return new Breakpoint(id, type, sourceFile, sourceLine, sourceColumn,
                            byteOffset, condition, enabled, hitCount);
    }

    // Getters
    public int getId() { return id; }
    public BreakpointType getType() { return type; }
    public String getSourceFile() { return sourceFile; }
    public int getSourceLine() { return sourceLine; }
    public int getSourceColumn() { return sourceColumn; }
    public int getByteOffset() { return byteOffset; }
    public String getCondition() { return condition; }
    public boolean isEnabled() { return enabled; }
    public long getHitCount() { return hitCount; }
    public boolean isConditional() { return condition != null && !condition.trim().isEmpty(); }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Breakpoint)) return false;
        final Breakpoint that = (Breakpoint) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Breakpoint{");
        sb.append("id=").append(id);
        sb.append(", type=").append(type);
        if (type == BreakpointType.SOURCE_LOCATION) {
            sb.append(", location=").append(sourceFile).append(":").append(sourceLine);
            if (sourceColumn > 0) {
                sb.append(":").append(sourceColumn);
            }
        } else {
            sb.append(", byteOffset=").append(byteOffset);
        }
        sb.append(", enabled=").append(enabled);
        if (isConditional()) {
            sb.append(", condition='").append(condition).append("'");
        }
        sb.append(", hits=").append(hitCount);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Breakpoint type enumeration.
     */
    public enum BreakpointType {
        BYTE_OFFSET,
        SOURCE_LOCATION,
        FUNCTION_ENTRY
    }
}