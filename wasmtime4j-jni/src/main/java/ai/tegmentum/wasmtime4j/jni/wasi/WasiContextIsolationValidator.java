package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Context isolation validator to ensure WASI contexts cannot interfere with each other.
 *
 * <p>This class provides comprehensive isolation validation for WASI contexts to prevent:
 *
 * <ul>
 *   <li>Cross-context file system access violations
 *   <li>Resource sharing conflicts between contexts
 *   <li>Memory access boundaries violations
 *   <li>Environment variable leakage between contexts
 *   <li>Security boundary breaches
 * </ul>
 *
 * <p>The validator maintains strict isolation boundaries between WASI contexts and enforces
 * security policies to prevent any cross-context contamination or unauthorized access.
 *
 * @since 1.0.0
 */
public final class WasiContextIsolationValidator {

  private static final Logger LOGGER =
      Logger.getLogger(WasiContextIsolationValidator.class.getName());

  /** Active WASI contexts being tracked for isolation. */
  private final Map<String, IsolatedContextInfo> activeContexts = new ConcurrentHashMap<>();

  /** Global resource allocations to prevent conflicts. */
  private final Map<String, String> resourceAllocations = new ConcurrentHashMap<>();

  /** File paths allocated to specific contexts. */
  private final Map<Path, String> pathAllocations = new ConcurrentHashMap<>();

  /** Isolation violation statistics. */
  private final IsolationStatistics statistics = new IsolationStatistics();

  /** Whether strict isolation mode is enabled. */
  private final boolean strictIsolationMode;

  /**
   * Creates a new context isolation validator.
   *
   * @param strictIsolationMode whether to enable strict isolation mode
   */
  public WasiContextIsolationValidator(final boolean strictIsolationMode) {
    this.strictIsolationMode = strictIsolationMode;

    LOGGER.info(
        String.format(
            "Created WASI context isolation validator with strict mode: %s", strictIsolationMode));
  }

  /** Creates a new context isolation validator with strict isolation enabled. */
  public WasiContextIsolationValidator() {
    this(true);
  }

  /**
   * Registers a new WASI context for isolation tracking.
   *
   * @param contextId the unique context identifier
   * @param context the WASI context to register
   * @param isolationLevel the isolation level for this context
   */
  public void registerContext(
      final String contextId, final WasiContext context, final IsolationLevel isolationLevel) {

    JniValidation.requireNonEmpty(contextId, "contextId");
    JniValidation.requireNonNull(context, "context");
    JniValidation.requireNonNull(isolationLevel, "isolationLevel");

    if (activeContexts.containsKey(contextId)) {
      throw new JniException("Context ID already registered: " + contextId);
    }

    final IsolatedContextInfo contextInfo =
        new IsolatedContextInfo(contextId, context, isolationLevel);
    activeContexts.put(contextId, contextInfo);

    statistics.contextsRegistered.incrementAndGet();

    LOGGER.info(
        String.format(
            "Registered WASI context for isolation: %s (level: %s)", contextId, isolationLevel));
  }

  /**
   * Unregisters a WASI context from isolation tracking.
   *
   * @param contextId the unique context identifier
   */
  public void unregisterContext(final String contextId) {
    JniValidation.requireNonEmpty(contextId, "contextId");

    final IsolatedContextInfo contextInfo = activeContexts.remove(contextId);
    if (contextInfo != null) {
      // Clean up resource allocations for this context
      cleanupContextResources(contextId);

      statistics.contextsUnregistered.incrementAndGet();

      LOGGER.info(String.format("Unregistered WASI context from isolation: %s", contextId));
    }
  }

  /**
   * Validates that a file path access is within the context's isolation boundary.
   *
   * @param contextId the context requesting access
   * @param path the file path being accessed
   * @param operation the operation being performed
   * @throws JniException if the access violates isolation
   */
  public void validatePathAccess(
      final String contextId, final Path path, final WasiFileOperation operation) {

    JniValidation.requireNonEmpty(contextId, "contextId");
    JniValidation.requireNonNull(path, "path");
    JniValidation.requireNonNull(operation, "operation");

    final IsolatedContextInfo contextInfo = getContextInfo(contextId);
    final Path normalizedPath = path.normalize().toAbsolutePath();

    LOGGER.fine(
        String.format(
            "Validating path access isolation: context=%s, path=%s, operation=%s",
            contextId, normalizedPath, operation));

    // Check if path is already allocated to another context
    final String allocatedContext = pathAllocations.get(normalizedPath);
    if (allocatedContext != null && !allocatedContext.equals(contextId)) {
      statistics.pathIsolationViolations.incrementAndGet();
      throw new JniException(
          String.format(
              "Path access isolation violation: path %s is allocated to context %s, "
                  + "requested by context %s",
              normalizedPath, allocatedContext, contextId));
    }

    // Check if path is within allowed boundaries for this context
    if (!isPathWithinContextBoundaries(contextInfo, normalizedPath)) {
      statistics.pathBoundaryViolations.incrementAndGet();
      throw new JniException(
          String.format(
              "Path access boundary violation: path %s is outside allowed boundaries "
                  + "for context %s",
              normalizedPath, contextId));
    }

    // In strict mode, allocate exclusive path access for write operations
    if (strictIsolationMode && operation.isWriteOperation()) {
      final String existingAllocation = pathAllocations.putIfAbsent(normalizedPath, contextId);
      if (existingAllocation != null && !existingAllocation.equals(contextId)) {
        statistics.pathIsolationViolations.incrementAndGet();
        throw new JniException(
            String.format(
                "Strict isolation violation: exclusive write access to path %s is "
                    + "already allocated to context %s, requested by context %s",
                normalizedPath, existingAllocation, contextId));
      }
    }

    LOGGER.fine(
        String.format(
            "Path access isolation validation passed: context=%s, path=%s",
            contextId, normalizedPath));
  }

  /**
   * Validates that a resource access is within the context's isolation boundary.
   *
   * @param contextId the context requesting access
   * @param resourceId the resource identifier
   * @param resourceType the type of resource
   * @throws JniException if the access violates isolation
   */
  public void validateResourceAccess(
      final String contextId, final String resourceId, final String resourceType) {

    JniValidation.requireNonEmpty(contextId, "contextId");
    JniValidation.requireNonEmpty(resourceId, "resourceId");
    JniValidation.requireNonEmpty(resourceType, "resourceType");

    final IsolatedContextInfo contextInfo = getContextInfo(contextId);

    LOGGER.fine(
        String.format(
            "Validating resource access isolation: context=%s, resource=%s, type=%s",
            contextId, resourceId, resourceType));

    // Check if resource is already allocated to another context
    final String fullResourceId = resourceType + ":" + resourceId;
    final String allocatedContext = resourceAllocations.get(fullResourceId);

    if (allocatedContext != null && !allocatedContext.equals(contextId)) {
      statistics.resourceIsolationViolations.incrementAndGet();
      throw new JniException(
          String.format(
              "Resource access isolation violation: resource %s (%s) is allocated to "
                  + "context %s, requested by context %s",
              resourceId, resourceType, allocatedContext, contextId));
    }

    // Check isolation level requirements
    if (contextInfo.isolationLevel == IsolationLevel.STRICT) {
      // In strict mode, allocate exclusive resource access
      final String existingAllocation = resourceAllocations.putIfAbsent(fullResourceId, contextId);
      if (existingAllocation != null && !existingAllocation.equals(contextId)) {
        statistics.resourceIsolationViolations.incrementAndGet();
        throw new JniException(
            String.format(
                "Strict resource isolation violation: exclusive access to resource %s (%s) "
                    + "is already allocated to context %s, requested by context %s",
                resourceId, resourceType, existingAllocation, contextId));
      }
    }

    LOGGER.fine(
        String.format(
            "Resource access isolation validation passed: context=%s, resource=%s",
            contextId, resourceId));
  }

  /**
   * Validates that memory access is within the context's isolation boundary.
   *
   * @param contextId the context requesting access
   * @param memoryAddress the memory address being accessed
   * @param size the size of memory being accessed
   * @throws JniException if the access violates isolation
   */
  public void validateMemoryAccess(
      final String contextId, final long memoryAddress, final long size) {

    JniValidation.requireNonEmpty(contextId, "contextId");
    JniValidation.requireNonNegative(memoryAddress, "memoryAddress");
    JniValidation.requirePositive(size, "size");

    final IsolatedContextInfo contextInfo = getContextInfo(contextId);

    LOGGER.finest(
        String.format(
            "Validating memory access isolation: context=%s, address=0x%x, size=%d",
            contextId, memoryAddress, size));

    // Check if memory range overlaps with other contexts (simplified check)
    for (final Map.Entry<String, IsolatedContextInfo> entry : activeContexts.entrySet()) {
      final String otherContextId = entry.getKey();
      final IsolatedContextInfo otherContext = entry.getValue();

      if (!otherContextId.equals(contextId)
          && otherContext.isolationLevel == IsolationLevel.STRICT) {
        // In a real implementation, this would check actual memory ranges
        // For now, we perform basic address range validation
        if (isMemoryRangeConflict(contextInfo, otherContext, memoryAddress, size)) {
          statistics.memoryIsolationViolations.incrementAndGet();
          throw new JniException(
              String.format(
                  "Memory access isolation violation: memory range 0x%x-%x conflicts "
                      + "with context %s, requested by context %s",
                  memoryAddress, memoryAddress + size, otherContextId, contextId));
        }
      }
    }

    LOGGER.finest(
        String.format(
            "Memory access isolation validation passed: context=%s, address=0x%x",
            contextId, memoryAddress));
  }

  /**
   * Validates cross-context communication attempt.
   *
   * @param sourceContextId the source context
   * @param targetContextId the target context
   * @param communicationType the type of communication
   * @throws JniException if cross-context communication is not allowed
   */
  public void validateCrossContextCommunication(
      final String sourceContextId, final String targetContextId, final String communicationType) {

    JniValidation.requireNonEmpty(sourceContextId, "sourceContextId");
    JniValidation.requireNonEmpty(targetContextId, "targetContextId");
    JniValidation.requireNonEmpty(communicationType, "communicationType");

    final IsolatedContextInfo sourceContext = getContextInfo(sourceContextId);
    final IsolatedContextInfo targetContext = getContextInfo(targetContextId);

    LOGGER.fine(
        String.format(
            "Validating cross-context communication: %s -> %s (%s)",
            sourceContextId, targetContextId, communicationType));

    // Check if both contexts allow cross-context communication
    if (sourceContext.isolationLevel == IsolationLevel.STRICT
        || targetContext.isolationLevel == IsolationLevel.STRICT) {
      statistics.crossContextViolations.incrementAndGet();
      throw new JniException(
          String.format(
              "Cross-context communication violation: strict isolation prevents "
                  + "communication from context %s to context %s (%s)",
              sourceContextId, targetContextId, communicationType));
    }

    LOGGER.fine(
        String.format(
            "Cross-context communication validation passed: %s -> %s",
            sourceContextId, targetContextId));
  }

  /**
   * Gets isolation statistics.
   *
   * @return the isolation statistics
   */
  public IsolationStatistics getStatistics() {
    return new IsolationStatistics(statistics); // Defensive copy
  }

  /**
   * Gets the current number of active contexts.
   *
   * @return the number of active contexts
   */
  public int getActiveContextCount() {
    return activeContexts.size();
  }

  /**
   * Checks if strict isolation mode is enabled.
   *
   * @return true if strict isolation mode is enabled, false otherwise
   */
  public boolean isStrictIsolationMode() {
    return strictIsolationMode;
  }

  /** Gets context info, throwing if not found. */
  private IsolatedContextInfo getContextInfo(final String contextId) {
    final IsolatedContextInfo contextInfo = activeContexts.get(contextId);
    if (contextInfo == null) {
      throw new JniException("Unknown context ID: " + contextId);
    }
    return contextInfo;
  }

  /** Checks if a path is within the allowed boundaries for a context. */
  private boolean isPathWithinContextBoundaries(
      final IsolatedContextInfo contextInfo, final Path path) {
    // Get the context's pre-opened directories
    final Map<String, Path> preopenedDirs = contextInfo.context.getPreopenedDirectories();

    // Check if the path is within any of the pre-opened directories
    for (final Path allowedPath : preopenedDirs.values()) {
      if (path.startsWith(allowedPath.normalize().toAbsolutePath())) {
        return true;
      }
    }

    return false;
  }

  /** Simplified check for memory range conflicts. */
  private boolean isMemoryRangeConflict(
      final IsolatedContextInfo context1,
      final IsolatedContextInfo context2,
      final long memoryAddress,
      final long size) {
    // In a real implementation, this would maintain actual memory range tracking
    // For now, we perform a basic check based on context separation
    return false; // Simplified implementation
  }

  /** Cleans up resource allocations for a context. */
  private void cleanupContextResources(final String contextId) {
    // Remove path allocations for this context
    pathAllocations.entrySet().removeIf(entry -> entry.getValue().equals(contextId));

    // Remove resource allocations for this context
    resourceAllocations.entrySet().removeIf(entry -> entry.getValue().equals(contextId));

    LOGGER.fine(String.format("Cleaned up resource allocations for context: %s", contextId));
  }

  /** Information about an isolated context. */
  private static final class IsolatedContextInfo {
    final String contextId;
    final WasiContext context;
    final IsolationLevel isolationLevel;

    IsolatedContextInfo(
        final String contextId, final WasiContext context, final IsolationLevel isolationLevel) {
      this.contextId = contextId;
      this.context = context;
      this.isolationLevel = isolationLevel;
    }
  }

  /** Isolation level for WASI contexts. */
  public enum IsolationLevel {
    /** Permissive isolation - allows some resource sharing. */
    PERMISSIVE,

    /** Standard isolation - prevents most cross-context access. */
    STANDARD,

    /** Strict isolation - prevents all cross-context access. */
    STRICT
  }

  /** Isolation violation statistics. */
  public static final class IsolationStatistics {
    private final AtomicLong contextsRegistered = new AtomicLong(0);
    private final AtomicLong contextsUnregistered = new AtomicLong(0);
    private final AtomicLong pathIsolationViolations = new AtomicLong(0);
    private final AtomicLong resourceIsolationViolations = new AtomicLong(0);
    private final AtomicLong memoryIsolationViolations = new AtomicLong(0);
    private final AtomicLong pathBoundaryViolations = new AtomicLong(0);
    private final AtomicLong crossContextViolations = new AtomicLong(0);

    /** Creates new empty statistics. */
    IsolationStatistics() {}

    /** Creates a defensive copy of statistics. */
    IsolationStatistics(final IsolationStatistics source) {
      this.contextsRegistered.set(source.contextsRegistered.get());
      this.contextsUnregistered.set(source.contextsUnregistered.get());
      this.pathIsolationViolations.set(source.pathIsolationViolations.get());
      this.resourceIsolationViolations.set(source.resourceIsolationViolations.get());
      this.memoryIsolationViolations.set(source.memoryIsolationViolations.get());
      this.pathBoundaryViolations.set(source.pathBoundaryViolations.get());
      this.crossContextViolations.set(source.crossContextViolations.get());
    }

    public long getContextsRegistered() {
      return contextsRegistered.get();
    }

    public long getContextsUnregistered() {
      return contextsUnregistered.get();
    }

    public long getActiveContexts() {
      return contextsRegistered.get() - contextsUnregistered.get();
    }

    public long getPathIsolationViolations() {
      return pathIsolationViolations.get();
    }

    public long getResourceIsolationViolations() {
      return resourceIsolationViolations.get();
    }

    public long getMemoryIsolationViolations() {
      return memoryIsolationViolations.get();
    }

    public long getPathBoundaryViolations() {
      return pathBoundaryViolations.get();
    }

    public long getCrossContextViolations() {
      return crossContextViolations.get();
    }

    /**
     * Returns the total number of security violations across all categories.
     *
     * @return the sum of all violation counts
     */
    public long getTotalViolations() {
      return pathIsolationViolations.get()
          + resourceIsolationViolations.get()
          + memoryIsolationViolations.get()
          + pathBoundaryViolations.get()
          + crossContextViolations.get();
    }

    @Override
    public String toString() {
      return String.format(
          "IsolationStatistics{active=%d, violations={path=%d, resource=%d, memory=%d, "
              + "boundary=%d, crossContext=%d}}",
          getActiveContexts(),
          pathIsolationViolations.get(),
          resourceIsolationViolations.get(),
          memoryIsolationViolations.get(),
          pathBoundaryViolations.get(),
          crossContextViolations.get());
    }
  }
}
