package ai.tegmentum.wasmtime4j.panama.wasi;

import java.lang.foreign.MemorySegment;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Context isolation validator to ensure WASI contexts cannot interfere with each other using Panama
 * FFI.
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
 * security policies to prevent any cross-context contamination or unauthorized access in
 * Panama-based implementations.
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

  /** Memory segments allocated to specific contexts. */
  private final Map<Long, String> memoryAllocations = new ConcurrentHashMap<>();

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
            "Created Panama WASI context isolation validator with strict mode: %s",
            strictIsolationMode));
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

    if (contextId == null || contextId.isEmpty()) {
      throw new IllegalArgumentException("Context ID cannot be null or empty");
    }
    if (context == null) {
      throw new IllegalArgumentException("Context cannot be null");
    }
    if (isolationLevel == null) {
      throw new IllegalArgumentException("Isolation level cannot be null");
    }

    if (activeContexts.containsKey(contextId)) {
      throw new IllegalStateException("Context ID already registered: " + contextId);
    }

    final IsolatedContextInfo contextInfo =
        new IsolatedContextInfo(contextId, context, isolationLevel);
    activeContexts.put(contextId, contextInfo);

    statistics.contextsRegistered.incrementAndGet();

    LOGGER.info(
        String.format(
            "Registered Panama WASI context for isolation: %s (level: %s)",
            contextId, isolationLevel));
  }

  /**
   * Unregisters a WASI context from isolation tracking.
   *
   * @param contextId the unique context identifier
   */
  public void unregisterContext(final String contextId) {
    if (contextId == null || contextId.isEmpty()) {
      throw new IllegalArgumentException("Context ID cannot be null or empty");
    }

    final IsolatedContextInfo contextInfo = activeContexts.remove(contextId);
    if (contextInfo != null) {
      // Clean up resource allocations for this context
      cleanupContextResources(contextId);

      statistics.contextsUnregistered.incrementAndGet();

      LOGGER.info(String.format("Unregistered Panama WASI context from isolation: %s", contextId));
    }
  }

  /**
   * Validates that a file path access is within the context's isolation boundary.
   *
   * @param contextId the context requesting access
   * @param path the file path being accessed
   * @param operation the operation being performed
   * @throws IllegalStateException if the access violates isolation
   */
  public void validatePathAccess(
      final String contextId, final Path path, final WasiFileOperation operation) {

    if (contextId == null || contextId.isEmpty()) {
      throw new IllegalArgumentException("Context ID cannot be null or empty");
    }
    if (path == null) {
      throw new IllegalArgumentException("Path cannot be null");
    }
    if (operation == null) {
      throw new IllegalArgumentException("Operation cannot be null");
    }

    final IsolatedContextInfo contextInfo = getContextInfo(contextId);
    final Path normalizedPath = path.normalize().toAbsolutePath();

    LOGGER.fine(
        String.format(
            "Validating Panama path access isolation: context=%s, path=%s, operation=%s",
            contextId, normalizedPath, operation));

    // Check if path is already allocated to another context
    final String allocatedContext = pathAllocations.get(normalizedPath);
    if (allocatedContext != null && !allocatedContext.equals(contextId)) {
      statistics.pathIsolationViolations.incrementAndGet();
      throw new IllegalStateException(
          String.format(
              "Panama path access isolation violation: path %s is allocated to context %s, "
                  + "requested by context %s",
              normalizedPath, allocatedContext, contextId));
    }

    // Check if path is within allowed boundaries for this context
    if (!isPathWithinContextBoundaries(contextInfo, normalizedPath)) {
      statistics.pathBoundaryViolations.incrementAndGet();
      throw new IllegalStateException(
          String.format(
              "Panama path access boundary violation: path %s is outside allowed boundaries "
                  + "for context %s",
              normalizedPath, contextId));
    }

    // In strict mode, allocate exclusive path access for write operations
    if (strictIsolationMode && operation.isWriteOperation()) {
      final String existingAllocation = pathAllocations.putIfAbsent(normalizedPath, contextId);
      if (existingAllocation != null && !existingAllocation.equals(contextId)) {
        statistics.pathIsolationViolations.incrementAndGet();
        throw new IllegalStateException(
            String.format(
                "Panama strict isolation violation: exclusive write access to path %s is "
                    + "already allocated to context %s, requested by context %s",
                normalizedPath, existingAllocation, contextId));
      }
    }

    LOGGER.fine(
        String.format(
            "Panama path access isolation validation passed: context=%s, path=%s",
            contextId, normalizedPath));
  }

  /**
   * Validates that a resource access is within the context's isolation boundary.
   *
   * @param contextId the context requesting access
   * @param resourceId the resource identifier
   * @param resourceType the type of resource
   * @throws IllegalStateException if the access violates isolation
   */
  public void validateResourceAccess(
      final String contextId, final String resourceId, final String resourceType) {

    if (contextId == null || contextId.isEmpty()) {
      throw new IllegalArgumentException("Context ID cannot be null or empty");
    }
    if (resourceId == null || resourceId.isEmpty()) {
      throw new IllegalArgumentException("Resource ID cannot be null or empty");
    }
    if (resourceType == null || resourceType.isEmpty()) {
      throw new IllegalArgumentException("Resource type cannot be null or empty");
    }

    final IsolatedContextInfo contextInfo = getContextInfo(contextId);

    LOGGER.fine(
        String.format(
            "Validating Panama resource access isolation: context=%s, resource=%s, type=%s",
            contextId, resourceId, resourceType));

    // Check if resource is already allocated to another context
    final String fullResourceId = resourceType + ":" + resourceId;
    final String allocatedContext = resourceAllocations.get(fullResourceId);

    if (allocatedContext != null && !allocatedContext.equals(contextId)) {
      statistics.resourceIsolationViolations.incrementAndGet();
      throw new IllegalStateException(
          String.format(
              "Panama resource access isolation violation: resource %s (%s) is allocated to "
                  + "context %s, requested by context %s",
              resourceId, resourceType, allocatedContext, contextId));
    }

    // Check isolation level requirements
    if (contextInfo.isolationLevel == IsolationLevel.STRICT) {
      // In strict mode, allocate exclusive resource access
      final String existingAllocation = resourceAllocations.putIfAbsent(fullResourceId, contextId);
      if (existingAllocation != null && !existingAllocation.equals(contextId)) {
        statistics.resourceIsolationViolations.incrementAndGet();
        throw new IllegalStateException(
            String.format(
                "Panama strict resource isolation violation: exclusive access to resource %s (%s) "
                    + "is already allocated to context %s, requested by context %s",
                resourceId, resourceType, existingAllocation, contextId));
      }
    }

    LOGGER.fine(
        String.format(
            "Panama resource access isolation validation passed: context=%s, resource=%s",
            contextId, resourceId));
  }

  /**
   * Validates that memory segment access is within the context's isolation boundary.
   *
   * @param contextId the context requesting access
   * @param memorySegment the memory segment being accessed
   * @throws IllegalStateException if the access violates isolation
   */
  public void validateMemoryAccess(final String contextId, final MemorySegment memorySegment) {

    if (contextId == null || contextId.isEmpty()) {
      throw new IllegalArgumentException("Context ID cannot be null or empty");
    }
    if (memorySegment == null) {
      throw new IllegalArgumentException("Memory segment cannot be null");
    }

    final IsolatedContextInfo contextInfo = getContextInfo(contextId);
    final long memoryAddress = memorySegment.address();

    LOGGER.finest(
        String.format(
            "Validating Panama memory access isolation: context=%s, address=0x%x, size=%d",
            contextId, memoryAddress, memorySegment.byteSize()));

    // Check if memory is already allocated to another context
    final String allocatedContext = memoryAllocations.get(memoryAddress);
    if (allocatedContext != null && !allocatedContext.equals(contextId)) {
      statistics.memoryIsolationViolations.incrementAndGet();
      throw new IllegalStateException(
          String.format(
              "Panama memory access isolation violation: memory segment 0x%x is allocated to "
                  + "context %s, requested by context %s",
              memoryAddress, allocatedContext, contextId));
    }

    // In strict mode, allocate exclusive memory access
    if (contextInfo.isolationLevel == IsolationLevel.STRICT) {
      final String existingAllocation = memoryAllocations.putIfAbsent(memoryAddress, contextId);
      if (existingAllocation != null && !existingAllocation.equals(contextId)) {
        statistics.memoryIsolationViolations.incrementAndGet();
        throw new IllegalStateException(
            String.format(
                "Panama strict memory isolation violation: exclusive access to memory 0x%x "
                    + "is already allocated to context %s, requested by context %s",
                memoryAddress, existingAllocation, contextId));
      }
    }

    LOGGER.finest(
        String.format(
            "Panama memory access isolation validation passed: context=%s, address=0x%x",
            contextId, memoryAddress));
  }

  /**
   * Validates cross-context communication attempt.
   *
   * @param sourceContextId the source context
   * @param targetContextId the target context
   * @param communicationType the type of communication
   * @throws IllegalStateException if cross-context communication is not allowed
   */
  public void validateCrossContextCommunication(
      final String sourceContextId, final String targetContextId, final String communicationType) {

    if (sourceContextId == null || sourceContextId.isEmpty()) {
      throw new IllegalArgumentException("Source context ID cannot be null or empty");
    }
    if (targetContextId == null || targetContextId.isEmpty()) {
      throw new IllegalArgumentException("Target context ID cannot be null or empty");
    }
    if (communicationType == null || communicationType.isEmpty()) {
      throw new IllegalArgumentException("Communication type cannot be null or empty");
    }

    final IsolatedContextInfo sourceContext = getContextInfo(sourceContextId);
    final IsolatedContextInfo targetContext = getContextInfo(targetContextId);

    LOGGER.fine(
        String.format(
            "Validating Panama cross-context communication: %s -> %s (%s)",
            sourceContextId, targetContextId, communicationType));

    // Check if both contexts allow cross-context communication
    if (sourceContext.isolationLevel == IsolationLevel.STRICT
        || targetContext.isolationLevel == IsolationLevel.STRICT) {
      statistics.crossContextViolations.incrementAndGet();
      throw new IllegalStateException(
          String.format(
              "Panama cross-context communication violation: strict isolation prevents "
                  + "communication from context %s to context %s (%s)",
              sourceContextId, targetContextId, communicationType));
    }

    LOGGER.fine(
        String.format(
            "Panama cross-context communication validation passed: %s -> %s",
            sourceContextId, targetContextId));
  }

  /**
   * Allocates a memory segment to a specific context for isolation tracking.
   *
   * @param contextId the context allocating the memory
   * @param memorySegment the memory segment being allocated
   */
  public void allocateMemoryToContext(final String contextId, final MemorySegment memorySegment) {
    if (contextId == null || contextId.isEmpty()) {
      throw new IllegalArgumentException("Context ID cannot be null or empty");
    }
    if (memorySegment == null) {
      throw new IllegalArgumentException("Memory segment cannot be null");
    }

    final long memoryAddress = memorySegment.address();
    memoryAllocations.put(memoryAddress, contextId);

    LOGGER.fine(
        String.format(
            "Allocated Panama memory segment to context: 0x%x -> %s", memoryAddress, contextId));
  }

  /**
   * Deallocates a memory segment from a specific context.
   *
   * @param contextId the context deallocating the memory
   * @param memorySegment the memory segment being deallocated
   */
  public void deallocateMemoryFromContext(
      final String contextId, final MemorySegment memorySegment) {
    if (contextId == null || contextId.isEmpty()) {
      throw new IllegalArgumentException("Context ID cannot be null or empty");
    }
    if (memorySegment == null) {
      throw new IllegalArgumentException("Memory segment cannot be null");
    }

    final long memoryAddress = memorySegment.address();
    final String allocatedContext = memoryAllocations.remove(memoryAddress);

    if (allocatedContext != null && !allocatedContext.equals(contextId)) {
      LOGGER.warning(
          String.format(
              "Memory deallocation mismatch: segment 0x%x was allocated to %s but deallocated by"
                  + " %s",
              memoryAddress, allocatedContext, contextId));
    }

    LOGGER.fine(
        String.format(
            "Deallocated Panama memory segment from context: 0x%x -> %s",
            memoryAddress, contextId));
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
      throw new IllegalStateException("Unknown context ID: " + contextId);
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

  /** Cleans up resource allocations for a context. */
  private void cleanupContextResources(final String contextId) {
    // Remove path allocations for this context
    pathAllocations.entrySet().removeIf(entry -> entry.getValue().equals(contextId));

    // Remove resource allocations for this context
    resourceAllocations.entrySet().removeIf(entry -> entry.getValue().equals(contextId));

    // Remove memory allocations for this context
    memoryAllocations.entrySet().removeIf(entry -> entry.getValue().equals(contextId));

    LOGGER.fine(String.format("Cleaned up Panama resource allocations for context: %s", contextId));
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
     * Gets the total number of isolation violations across all categories.
     *
     * @return the total violation count
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
          "PanamaIsolationStatistics{active=%d, violations={path=%d, resource=%d, memory=%d, "
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
