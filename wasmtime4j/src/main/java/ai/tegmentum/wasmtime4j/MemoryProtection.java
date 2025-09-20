package ai.tegmentum.wasmtime4j;

/**
 * Memory protection and security features for WebAssembly linear memory.
 *
 * <p>This interface provides comprehensive memory protection capabilities including access control,
 * permission management, and security policy enforcement. All protection features are implemented
 * with defensive programming principles to prevent unauthorized access and maintain system
 * security.
 *
 * @since 1.0.0
 */
public interface MemoryProtection {

  /**
   * Sets a memory region as read-only.
   *
   * <p>Once set as read-only, any attempt to write to the specified region will result in a
   * security exception. This protection cannot be bypassed and provides strong guarantees for
   * memory immutability.
   *
   * @param memory the target memory instance
   * @param offset the starting offset of the region
   * @param length the length of the region in bytes
   * @throws IllegalArgumentException if memory is null or parameters are invalid
   * @throws IndexOutOfBoundsException if the region extends beyond memory bounds
   * @throws SecurityException if the current context lacks permission to modify protection
   * @throws RuntimeException if the protection cannot be set
   */
  void setReadOnly(final WasmMemory memory, final int offset, final int length);

  /**
   * Sets a memory region as executable.
   *
   * <p>This marks the specified region as containing executable code. Depending on the security
   * policy, this may enable or restrict certain operations on the region.
   *
   * @param memory the target memory instance
   * @param offset the starting offset of the region
   * @param length the length of the region in bytes
   * @throws IllegalArgumentException if memory is null or parameters are invalid
   * @throws IndexOutOfBoundsException if the region extends beyond memory bounds
   * @throws SecurityException if the current context lacks permission to modify protection
   * @throws RuntimeException if the protection cannot be set
   */
  void setExecutable(final WasmMemory memory, final int offset, final int length);

  /**
   * Removes read-only protection from a memory region.
   *
   * <p>This restores write access to a previously read-only region. Requires appropriate security
   * permissions to prevent unauthorized modifications.
   *
   * @param memory the target memory instance
   * @param offset the starting offset of the region
   * @param length the length of the region in bytes
   * @throws IllegalArgumentException if memory is null or parameters are invalid
   * @throws IndexOutOfBoundsException if the region extends beyond memory bounds
   * @throws SecurityException if the current context lacks permission to modify protection
   * @throws RuntimeException if the protection cannot be removed
   */
  void removeReadOnly(final WasmMemory memory, final int offset, final int length);

  /**
   * Removes executable protection from a memory region.
   *
   * <p>This removes the executable flag from the specified region, potentially changing how the
   * region can be used based on security policies.
   *
   * @param memory the target memory instance
   * @param offset the starting offset of the region
   * @param length the length of the region in bytes
   * @throws IllegalArgumentException if memory is null or parameters are invalid
   * @throws IndexOutOfBoundsException if the region extends beyond memory bounds
   * @throws SecurityException if the current context lacks permission to modify protection
   * @throws RuntimeException if the protection cannot be removed
   */
  void removeExecutable(final WasmMemory memory, final int offset, final int length);

  /**
   * Checks if a memory location is readable.
   *
   * <p>This verifies read permissions for a specific memory location without actually performing a
   * read operation.
   *
   * @param memory the target memory instance
   * @param offset the memory offset to check
   * @return true if the location is readable, false otherwise
   * @throws IllegalArgumentException if memory is null or offset is invalid
   * @throws RuntimeException if permission checking fails
   */
  boolean isReadable(final WasmMemory memory, final int offset);

  /**
   * Checks if a memory location is writable.
   *
   * <p>This verifies write permissions for a specific memory location without actually performing a
   * write operation.
   *
   * @param memory the target memory instance
   * @param offset the memory offset to check
   * @return true if the location is writable, false otherwise
   * @throws IllegalArgumentException if memory is null or offset is invalid
   * @throws RuntimeException if permission checking fails
   */
  boolean isWritable(final WasmMemory memory, final int offset);

  /**
   * Checks if a memory location is executable.
   *
   * <p>This verifies executable permissions for a specific memory location.
   *
   * @param memory the target memory instance
   * @param offset the memory offset to check
   * @return true if the location is executable, false otherwise
   * @throws IllegalArgumentException if memory is null or offset is invalid
   * @throws RuntimeException if permission checking fails
   */
  boolean isExecutable(final WasmMemory memory, final int offset);

  /**
   * Gets the protection flags for a specific memory region.
   *
   * <p>This returns a bitmask representing the current protection state of the specified memory
   * region.
   *
   * @param memory the target memory instance
   * @param offset the memory offset to check
   * @param length the length of the region to check
   * @return protection flags as a bitmask (READ=1, WRITE=2, EXECUTE=4)
   * @throws IllegalArgumentException if memory is null or parameters are invalid
   * @throws IndexOutOfBoundsException if the region extends beyond memory bounds
   * @throws RuntimeException if protection information cannot be retrieved
   */
  int getProtectionFlags(final WasmMemory memory, final int offset, final int length);

  /**
   * Sets comprehensive protection flags for a memory region.
   *
   * <p>This allows setting multiple protection attributes in a single operation using a bitmask for
   * efficiency.
   *
   * @param memory the target memory instance
   * @param offset the starting offset of the region
   * @param length the length of the region in bytes
   * @param flags protection flags as a bitmask (READ=1, WRITE=2, EXECUTE=4)
   * @throws IllegalArgumentException if memory is null, parameters are invalid, or flags are
   *     invalid
   * @throws IndexOutOfBoundsException if the region extends beyond memory bounds
   * @throws SecurityException if the current context lacks permission to modify protection
   * @throws RuntimeException if the protection cannot be set
   */
  void setProtectionFlags(
      final WasmMemory memory, final int offset, final int length, final int flags);

  /**
   * Creates a protected memory view with restricted access.
   *
   * <p>This creates a view of the memory that enforces specific access restrictions, providing an
   * additional layer of security for sensitive operations.
   *
   * @param memory the source memory instance
   * @param offset the starting offset of the view
   * @param length the length of the view in bytes
   * @param allowRead whether read access is permitted
   * @param allowWrite whether write access is permitted
   * @return a protected memory view
   * @throws IllegalArgumentException if memory is null or parameters are invalid
   * @throws IndexOutOfBoundsException if the region extends beyond memory bounds
   * @throws SecurityException if the current context lacks permission to create protected views
   * @throws RuntimeException if the protected view cannot be created
   */
  WasmMemory createProtectedView(
      final WasmMemory memory,
      final int offset,
      final int length,
      final boolean allowRead,
      final boolean allowWrite);

  /**
   * Validates that a memory operation would be permitted under current protections.
   *
   * <p>This performs comprehensive validation of a proposed memory operation against all active
   * protection policies without actually performing the operation.
   *
   * @param memory the target memory instance
   * @param operation the type of operation ("read", "write", "execute")
   * @param offset the memory offset for the operation
   * @param length the length of data involved in the operation
   * @return true if the operation would be permitted, false otherwise
   * @throws IllegalArgumentException if any parameter is null or invalid
   * @throws RuntimeException if validation cannot be performed
   */
  boolean validateOperation(
      final WasmMemory memory, final String operation, final int offset, final int length);

  /**
   * Enables audit logging for memory protection events.
   *
   * <p>When enabled, all protection changes and access violations will be logged for security
   * monitoring and compliance purposes.
   *
   * @throws SecurityException if the current context lacks permission to enable auditing
   * @throws RuntimeException if auditing cannot be enabled
   */
  void enableAuditLogging();

  /**
   * Disables audit logging for memory protection events.
   *
   * @throws SecurityException if the current context lacks permission to disable auditing
   * @throws RuntimeException if auditing cannot be disabled
   */
  void disableAuditLogging();

  /**
   * Checks if audit logging is currently enabled.
   *
   * @return true if audit logging is enabled, false otherwise
   */
  boolean isAuditLoggingEnabled();

  /** Protection flag constants for use with protection methods. */
  final class ProtectionFlags {
    /** Read access permission flag. */
    public static final int READ = 1;

    /** Write access permission flag. */
    public static final int WRITE = 2;

    /** Execute access permission flag. */
    public static final int EXECUTE = 4;

    /** Read-write access permission flags. */
    public static final int READ_WRITE = READ | WRITE;

    /** Full access permission flags. */
    public static final int FULL_ACCESS = READ | WRITE | EXECUTE;

    /** No access permission flags. */
    public static final int NO_ACCESS = 0;

    private ProtectionFlags() {
      // Utility class - prevent instantiation
    }
  }
}
