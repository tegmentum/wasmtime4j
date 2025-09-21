package ai.tegmentum.wasmtime4j.security;

/**
 * Represents a memory access operation for security validation.
 *
 * @since 1.0.0
 */
public final class MemoryAccess {

  private final MemoryOperation operation;
  private final long offset;
  private final long length;
  private final String sourceModule;
  private final String sourceFunction;

  public MemoryAccess(
      final MemoryOperation operation,
      final long offset,
      final long length,
      final String sourceModule,
      final String sourceFunction) {
    this.operation = operation;
    this.offset = offset;
    this.length = length;
    this.sourceModule = sourceModule;
    this.sourceFunction = sourceFunction;
  }

  public MemoryOperation getOperation() {
    return operation;
  }

  public long getOffset() {
    return offset;
  }

  public long getLength() {
    return length;
  }

  public String getSourceModule() {
    return sourceModule;
  }

  public String getSourceFunction() {
    return sourceFunction;
  }

  public enum MemoryOperation {
    READ,
    WRITE,
    GROW,
    BOUNDS_CHECK
  }
}
