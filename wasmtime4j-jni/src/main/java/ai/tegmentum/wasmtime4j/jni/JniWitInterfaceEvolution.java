package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.AdaptationConfig;
import ai.tegmentum.wasmtime4j.CompatibilityRequirements;
import ai.tegmentum.wasmtime4j.EvolutionValidationResult;
import ai.tegmentum.wasmtime4j.InterfaceEvolutionStrategy;
import ai.tegmentum.wasmtime4j.MigrationConfig;
import ai.tegmentum.wasmtime4j.WitEvolutionMetrics;
import ai.tegmentum.wasmtime4j.WitInterfaceDefinition;
import ai.tegmentum.wasmtime4j.WitInterfaceEvolution;
import ai.tegmentum.wasmtime4j.WitInterfaceVersion;
import ai.tegmentum.wasmtime4j.WitTypeAdapter;
import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of WIT interface evolution support.
 *
 * <p>This class provides JNI-based implementation for WebAssembly Interface Type evolution,
 * including backward compatibility checking, type adaptation, and interface migration.
 *
 * <p>Thread-safe implementation with proper resource management and error handling.
 *
 * @since 1.0.0
 */
public final class JniWitInterfaceEvolution implements WitInterfaceEvolution {

  private static final Logger LOGGER = Logger.getLogger(JniWitInterfaceEvolution.class.getName());

  /** Native handle for the evolution manager. */
  private volatile long evolutionManagerHandle;

  /** Registered interface versions. */
  private final Map<String, List<WitInterfaceVersion>> versionRegistry = new ConcurrentHashMap<>();

  /** Type adapter registry. */
  private final Map<String, WitTypeAdapter> adapterRegistry = new ConcurrentHashMap<>();

  /** Evolution metrics. */
  private final WitEvolutionMetrics.Builder metricsBuilder = WitEvolutionMetrics.builder();

  /** Lock for thread-safe operations. */
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  /** Native library loader. */
  private static final JniLibraryLoader LIBRARY_LOADER = JniLibraryLoader.getInstance();

  static {
    // Ensure native library is loaded
    LIBRARY_LOADER.loadLibrary();
  }

  /**
   * Creates a new JNI WIT interface evolution instance.
   *
   * @throws WasmRuntimeException if native initialization fails
   */
  public JniWitInterfaceEvolution() {
    try {
      this.evolutionManagerHandle = nativeCreateEvolutionManager();
      if (evolutionManagerHandle == 0) {
        throw new WasmRuntimeException("Failed to create native evolution manager");
      }
      LOGGER.fine(
          "Created JNI WIT interface evolution manager with handle: " + evolutionManagerHandle);
    } catch (final Exception e) {
      throw new WasmRuntimeException("Failed to initialize JNI WIT interface evolution", e);
    }
  }

  @Override
  public InterfaceEvolutionAnalysis analyzeEvolution(
      final WitInterfaceVersion fromVersion, final WitInterfaceVersion toVersion) {
    Objects.requireNonNull(fromVersion, "fromVersion must not be null");
    Objects.requireNonNull(toVersion, "toVersion must not be null");

    lock.readLock().lock();
    try {
      checkNativeHandle();

      final long analysisHandle =
          nativeAnalyzeEvolution(
              evolutionManagerHandle,
              fromVersion.getVersion(),
              toVersion.getVersion(),
              serializeInterface(fromVersion.getInterface()),
              serializeInterface(toVersion.getInterface()));

      if (analysisHandle == 0) {
        throw new WasmRuntimeException("Failed to analyze interface evolution");
      }

      return new JniInterfaceEvolutionAnalysis(analysisHandle);

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to analyze interface evolution", e);
      throw new WasmRuntimeException("Interface evolution analysis failed", e);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public BackwardCompatibilityResult checkBackwardCompatibility(
      final WitInterfaceVersion olderVersion, final WitInterfaceVersion newerVersion) {
    Objects.requireNonNull(olderVersion, "olderVersion must not be null");
    Objects.requireNonNull(newerVersion, "newerVersion must not be null");

    lock.readLock().lock();
    try {
      checkNativeHandle();

      final long compatibilityHandle =
          nativeCheckBackwardCompatibility(
              evolutionManagerHandle,
              olderVersion.getVersion(),
              newerVersion.getVersion(),
              serializeInterface(olderVersion.getInterface()),
              serializeInterface(newerVersion.getInterface()));

      if (compatibilityHandle == 0) {
        throw new WasmRuntimeException("Failed to check backward compatibility");
      }

      return new JniBackwardCompatibilityResult(compatibilityHandle);

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to check backward compatibility", e);
      throw new WasmRuntimeException("Backward compatibility check failed", e);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public ForwardCompatibilityResult checkForwardCompatibility(
      final WitInterfaceVersion newerVersion, final WitInterfaceVersion olderVersion) {
    Objects.requireNonNull(newerVersion, "newerVersion must not be null");
    Objects.requireNonNull(olderVersion, "olderVersion must not be null");

    lock.readLock().lock();
    try {
      checkNativeHandle();

      final long compatibilityHandle =
          nativeCheckForwardCompatibility(
              evolutionManagerHandle,
              newerVersion.getVersion(),
              olderVersion.getVersion(),
              serializeInterface(newerVersion.getInterface()),
              serializeInterface(olderVersion.getInterface()));

      if (compatibilityHandle == 0) {
        throw new WasmRuntimeException("Failed to check forward compatibility");
      }

      return new JniForwardCompatibilityResult(compatibilityHandle);

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to check forward compatibility", e);
      throw new WasmRuntimeException("Forward compatibility check failed", e);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public InterfaceAdapter createAdapter(
      final WitInterfaceVersion sourceVersion,
      final WitInterfaceVersion targetVersion,
      final AdaptationConfig adaptationConfig) {
    Objects.requireNonNull(sourceVersion, "sourceVersion must not be null");
    Objects.requireNonNull(targetVersion, "targetVersion must not be null");
    Objects.requireNonNull(adaptationConfig, "adaptationConfig must not be null");

    lock.writeLock().lock();
    try {
      checkNativeHandle();

      final long adapterHandle =
          nativeCreateAdapter(
              evolutionManagerHandle,
              sourceVersion.getVersion(),
              targetVersion.getVersion(),
              serializeInterface(sourceVersion.getInterface()),
              serializeInterface(targetVersion.getInterface()),
              serializeAdaptationConfig(adaptationConfig));

      if (adapterHandle == 0) {
        throw new WasmRuntimeException("Failed to create interface adapter");
      }

      return new JniInterfaceAdapter(adapterHandle, sourceVersion, targetVersion);

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to create interface adapter", e);
      throw new WasmRuntimeException("Interface adapter creation failed", e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public EvolutionValidationResult validateEvolutionStrategy(
      final InterfaceEvolutionStrategy strategy) {
    Objects.requireNonNull(strategy, "strategy must not be null");

    lock.readLock().lock();
    try {
      checkNativeHandle();

      final long validationHandle =
          nativeValidateEvolutionStrategy(
              evolutionManagerHandle, serializeEvolutionStrategy(strategy));

      if (validationHandle == 0) {
        throw new WasmRuntimeException("Failed to validate evolution strategy");
      }

      return new JniEvolutionValidationResult(validationHandle);

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to validate evolution strategy", e);
      throw new WasmRuntimeException("Evolution strategy validation failed", e);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public InterfaceMigrationPlan createMigrationPlan(
      final WitInterfaceDefinition currentInterface,
      final WitInterfaceDefinition targetInterface,
      final MigrationConfig migrationConfig) {
    Objects.requireNonNull(currentInterface, "currentInterface must not be null");
    Objects.requireNonNull(targetInterface, "targetInterface must not be null");
    Objects.requireNonNull(migrationConfig, "migrationConfig must not be null");

    lock.writeLock().lock();
    try {
      checkNativeHandle();

      final long planHandle =
          nativeCreateMigrationPlan(
              evolutionManagerHandle,
              serializeInterface(currentInterface),
              serializeInterface(targetInterface),
              serializeMigrationConfig(migrationConfig));

      if (planHandle == 0) {
        throw new WasmRuntimeException("Failed to create migration plan");
      }

      return new JniInterfaceMigrationPlan(planHandle);

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to create migration plan", e);
      throw new WasmRuntimeException("Migration plan creation failed", e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public MigrationExecutionResult executeMigration(final InterfaceMigrationPlan migrationPlan) {
    Objects.requireNonNull(migrationPlan, "migrationPlan must not be null");

    lock.writeLock().lock();
    try {
      checkNativeHandle();

      if (!(migrationPlan instanceof JniInterfaceMigrationPlan)) {
        throw new WasmRuntimeException("Migration plan must be a JNI implementation");
      }

      final JniInterfaceMigrationPlan jniPlan = (JniInterfaceMigrationPlan) migrationPlan;
      final long executionHandle =
          nativeExecuteMigration(evolutionManagerHandle, jniPlan.getHandle());

      if (executionHandle == 0) {
        throw new WasmRuntimeException("Failed to execute migration");
      }

      return new JniMigrationExecutionResult(executionHandle);

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to execute migration", e);
      throw new WasmRuntimeException("Migration execution failed", e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public InterfaceEvolutionHistory getEvolutionHistory(final String interfaceName) {
    Objects.requireNonNull(interfaceName, "interfaceName must not be null");
    if (interfaceName.trim().isEmpty()) {
      throw new IllegalArgumentException("interfaceName must not be empty");
    }

    lock.readLock().lock();
    try {
      checkNativeHandle();

      final long historyHandle = nativeGetEvolutionHistory(evolutionManagerHandle, interfaceName);

      if (historyHandle == 0) {
        throw new WasmRuntimeException(
            "Failed to get evolution history for interface: " + interfaceName);
      }

      return new JniInterfaceEvolutionHistory(historyHandle);

    } catch (final Exception e) {
      LOGGER.log(
          Level.SEVERE, "Failed to get evolution history for interface: " + interfaceName, e);
      throw new WasmRuntimeException("Evolution history retrieval failed", e);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void registerInterfaceVersion(final WitInterfaceVersion interfaceVersion) {
    Objects.requireNonNull(interfaceVersion, "interfaceVersion must not be null");

    lock.writeLock().lock();
    try {
      checkNativeHandle();

      final boolean success =
          nativeRegisterInterfaceVersion(
              evolutionManagerHandle,
              interfaceVersion.getInterfaceName(),
              interfaceVersion.getVersion(),
              serializeInterface(interfaceVersion.getInterface()));

      if (!success) {
        throw new WasmRuntimeException(
            "Failed to register interface version: " + interfaceVersion.getVersion());
      }

      // Update local registry
      versionRegistry
          .computeIfAbsent(interfaceVersion.getInterfaceName(), k -> new java.util.ArrayList<>())
          .add(interfaceVersion);

      LOGGER.fine(
          "Registered interface version: "
              + interfaceVersion.getInterfaceName()
              + "@"
              + interfaceVersion.getVersion());

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to register interface version", e);
      throw new WasmRuntimeException("Interface version registration failed", e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void deprecateInterfaceVersion(
      final WitInterfaceVersion interfaceVersion, final DeprecationInfo deprecationInfo) {
    Objects.requireNonNull(interfaceVersion, "interfaceVersion must not be null");
    Objects.requireNonNull(deprecationInfo, "deprecationInfo must not be null");

    lock.writeLock().lock();
    try {
      checkNativeHandle();

      final boolean success =
          nativeDeprecateInterfaceVersion(
              evolutionManagerHandle,
              interfaceVersion.getInterfaceName(),
              interfaceVersion.getVersion(),
              serializeDeprecationInfo(deprecationInfo));

      if (!success) {
        throw new WasmRuntimeException(
            "Failed to deprecate interface version: " + interfaceVersion.getVersion());
      }

      LOGGER.info(
          "Deprecated interface version: "
              + interfaceVersion.getInterfaceName()
              + "@"
              + interfaceVersion.getVersion());

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to deprecate interface version", e);
      throw new WasmRuntimeException("Interface version deprecation failed", e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public List<WitInterfaceVersion> getInterfaceVersions(final String interfaceName) {
    Objects.requireNonNull(interfaceName, "interfaceName must not be null");
    if (interfaceName.trim().isEmpty()) {
      throw new IllegalArgumentException("interfaceName must not be empty");
    }

    lock.readLock().lock();
    try {
      checkNativeHandle();

      final String[] versions = nativeGetInterfaceVersions(evolutionManagerHandle, interfaceName);

      if (versions == null) {
        return Collections.emptyList(); // No versions found
      }

      // Convert native array to interface version list
      return convertToInterfaceVersions(interfaceName, versions);

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to get interface versions for: " + interfaceName, e);
      throw new WasmRuntimeException("Interface versions retrieval failed", e);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public java.util.Optional<WitInterfaceVersion> findCompatibleVersion(
      final String interfaceName, final CompatibilityRequirements requirements) {
    Objects.requireNonNull(interfaceName, "interfaceName must not be null");
    Objects.requireNonNull(requirements, "requirements must not be null");

    lock.readLock().lock();
    try {
      checkNativeHandle();

      final String compatibleVersion =
          nativeFindCompatibleVersion(
              evolutionManagerHandle,
              interfaceName,
              serializeCompatibilityRequirements(requirements));

      if (compatibleVersion == null) {
        return java.util.Optional.empty();
      }

      // Create interface version from found version
      return createInterfaceVersion(interfaceName, compatibleVersion);

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to find compatible version for: " + interfaceName, e);
      throw new WasmRuntimeException("Compatible version search failed", e);
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Closes this evolution manager and releases native resources. */
  public void close() {
    lock.writeLock().lock();
    try {
      if (evolutionManagerHandle != 0) {
        nativeDestroyEvolutionManager(evolutionManagerHandle);
        evolutionManagerHandle = 0;
        LOGGER.fine("Closed JNI WIT interface evolution manager");
      }
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Error closing JNI WIT interface evolution manager", e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // Private helper methods

  private void checkNativeHandle() {
    if (evolutionManagerHandle == 0) {
      throw new WasmRuntimeException("Evolution manager has been closed");
    }
  }

  private String serializeInterface(final WitInterfaceDefinition interfaceDefinition) {
    // Serialize interface definition to JSON or binary format
    // This is a simplified implementation - real implementation would use proper serialization
    return String.format(
        "{\"name\":\"%s\",\"version\":\"%s\",\"package\":\"%s\",\"functions\":[%s],\"types\":[%s]}",
        interfaceDefinition.getName(),
        interfaceDefinition.getVersion(),
        interfaceDefinition.getPackageName(),
        String.join(
            ",",
            interfaceDefinition.getFunctionNames().stream()
                .map(name -> "\"" + name + "\"")
                .toList()),
        String.join(
            ",",
            interfaceDefinition.getTypeNames().stream().map(name -> "\"" + name + "\"").toList()));
  }

  private String serializeAdaptationConfig(final AdaptationConfig config) {
    // Serialize adaptation configuration
    return "{}"; // Simplified implementation
  }

  private String serializeEvolutionStrategy(final InterfaceEvolutionStrategy strategy) {
    // Serialize evolution strategy
    return "{}"; // Simplified implementation
  }

  private String serializeMigrationConfig(final MigrationConfig config) {
    // Serialize migration configuration
    return "{}"; // Simplified implementation
  }

  private String serializeDeprecationInfo(final DeprecationInfo info) {
    // Serialize deprecation information
    return String.format(
        "{\"deprecationDate\":\"%s\",\"reason\":\"%s\"}",
        info.getDeprecationDate().toString(), info.getReason());
  }

  private String serializeCompatibilityRequirements(final CompatibilityRequirements requirements) {
    // Serialize compatibility requirements
    return "{}"; // Simplified implementation
  }

  private List<WitInterfaceVersion> convertToInterfaceVersions(
      final String interfaceName, final String[] versions) {
    // Convert version strings to interface version objects
    return java.util.Arrays.stream(versions)
        .map(version -> createInterfaceVersion(interfaceName, version))
        .filter(java.util.Optional::isPresent)
        .map(java.util.Optional::get)
        .toList();
  }

  private java.util.Optional<WitInterfaceVersion> createInterfaceVersion(
      final String interfaceName, final String version) {
    // Create interface version from name and version string
    // This is a simplified implementation
    return java.util.Optional.empty(); // Would create actual version object
  }

  // Native method declarations
  private static native long nativeCreateEvolutionManager();

  private static native void nativeDestroyEvolutionManager(long handle);

  private static native long nativeAnalyzeEvolution(
      long managerHandle,
      String fromVersion,
      String toVersion,
      String fromInterface,
      String toInterface);

  private static native long nativeCheckBackwardCompatibility(
      long managerHandle,
      String olderVersion,
      String newerVersion,
      String olderInterface,
      String newerInterface);

  private static native long nativeCheckForwardCompatibility(
      long managerHandle,
      String newerVersion,
      String olderVersion,
      String newerInterface,
      String olderInterface);

  private static native long nativeCreateAdapter(
      long managerHandle,
      String sourceVersion,
      String targetVersion,
      String sourceInterface,
      String targetInterface,
      String adaptationConfig);

  private static native long nativeValidateEvolutionStrategy(
      long managerHandle, String evolutionStrategy);

  private static native long nativeCreateMigrationPlan(
      long managerHandle, String currentInterface, String targetInterface, String migrationConfig);

  private static native long nativeExecuteMigration(long managerHandle, long migrationPlanHandle);

  private static native long nativeGetEvolutionHistory(long managerHandle, String interfaceName);

  private static native boolean nativeRegisterInterfaceVersion(
      long managerHandle, String interfaceName, String version, String interfaceDefinition);

  private static native boolean nativeDeprecateInterfaceVersion(
      long managerHandle, String interfaceName, String version, String deprecationInfo);

  private static native String[] nativeGetInterfaceVersions(
      long managerHandle, String interfaceName);

  private static native String nativeFindCompatibleVersion(
      long managerHandle, String interfaceName, String requirements);

  // Inner classes for JNI implementations would be implemented here
  // These are simplified stubs for the implementation

  private static class JniInterfaceEvolutionAnalysis implements InterfaceEvolutionAnalysis {
    private final long handle;

    JniInterfaceEvolutionAnalysis(final long handle) {
      this.handle = handle;
    }

    // Implementation of InterfaceEvolutionAnalysis methods would go here
    @Override
    public WitInterfaceVersion getSourceVersion() {
      return null;
    }

    @Override
    public WitInterfaceVersion getTargetVersion() {
      return null;
    }

    @Override
    public EvolutionType getEvolutionType() {
      return EvolutionType.MAJOR;
    }

    @Override
    public List<BreakingChange> getBreakingChanges() {
      return Collections.emptyList();
    }

    @Override
    public List<NonBreakingChange> getNonBreakingChanges() {
      return Collections.emptyList();
    }

    @Override
    public List<RequiredAdaptation> getRequiredAdaptations() {
      return Collections.emptyList();
    }

    @Override
    public MigrationComplexity getMigrationComplexity() {
      return MigrationComplexity.SIMPLE;
    }

    @Override
    public MigrationEffort getEstimatedEffort() {
      return MigrationEffort.LOW;
    }
  }

  private static class JniBackwardCompatibilityResult implements BackwardCompatibilityResult {
    private final long handle;

    JniBackwardCompatibilityResult(final long handle) {
      this.handle = handle;
    }

    @Override
    public boolean isBackwardCompatible() {
      return true;
    }

    @Override
    public List<CompatibilityIssue> getIssues() {
      return Collections.emptyList();
    }

    @Override
    public CompatibilityLevel getCompatibilityLevel() {
      return CompatibilityLevel.FULL;
    }

    @Override
    public List<String> getSuggestions() {
      return Collections.emptyList();
    }
  }

  private static class JniForwardCompatibilityResult implements ForwardCompatibilityResult {
    private final long handle;

    JniForwardCompatibilityResult(final long handle) {
      this.handle = handle;
    }

    @Override
    public boolean isForwardCompatible() {
      return true;
    }

    @Override
    public List<CompatibilityIssue> getIssues() {
      return Collections.emptyList();
    }

    @Override
    public CompatibilityLevel getCompatibilityLevel() {
      return CompatibilityLevel.FULL;
    }

    @Override
    public RiskAssessment getRiskAssessment() {
      return null;
    }
  }

  private static class JniInterfaceAdapter implements InterfaceAdapter {
    private final long handle;
    private final WitInterfaceVersion sourceVersion;
    private final WitInterfaceVersion targetVersion;

    JniInterfaceAdapter(
        final long handle,
        final WitInterfaceVersion sourceVersion,
        final WitInterfaceVersion targetVersion) {
      this.handle = handle;
      this.sourceVersion = sourceVersion;
      this.targetVersion = targetVersion;
    }

    @Override
    public WitInterfaceVersion getSourceVersion() {
      return sourceVersion;
    }

    @Override
    public WitInterfaceVersion getTargetVersion() {
      return targetVersion;
    }

    @Override
    public ai.tegmentum.wasmtime4j.WasmValue[] adaptCall(
        final String functionName, final ai.tegmentum.wasmtime4j.WasmValue[] sourceArgs) {
      return sourceArgs; // Simplified implementation
    }

    @Override
    public ai.tegmentum.wasmtime4j.WasmValue adaptReturn(
        final String functionName, final ai.tegmentum.wasmtime4j.WasmValue targetResult) {
      return targetResult; // Simplified implementation
    }

    @Override
    public AdaptationStatistics getStatistics() {
      return null;
    }
  }

  private static class JniEvolutionValidationResult implements EvolutionValidationResult {
    private final long handle;

    JniEvolutionValidationResult(final long handle) {
      this.handle = handle;
    }

    // Stub implementation
  }

  private static class JniInterfaceMigrationPlan implements InterfaceMigrationPlan {
    private final long handle;

    JniInterfaceMigrationPlan(final long handle) {
      this.handle = handle;
    }

    long getHandle() {
      return handle;
    }

    @Override
    public String getId() {
      return "migration-" + handle;
    }

    @Override
    public WitInterfaceDefinition getSourceInterface() {
      return null;
    }

    @Override
    public WitInterfaceDefinition getTargetInterface() {
      return null;
    }

    @Override
    public List<MigrationStep> getSteps() {
      return Collections.emptyList();
    }

    @Override
    public java.time.Duration getEstimatedDuration() {
      return java.time.Duration.ZERO;
    }

    @Override
    public List<MigrationRisk> getRisks() {
      return Collections.emptyList();
    }

    @Override
    public List<MigrationStep> getRollbackSteps() {
      return Collections.emptyList();
    }
  }

  private static class JniMigrationExecutionResult implements MigrationExecutionResult {
    private final long handle;

    JniMigrationExecutionResult(final long handle) {
      this.handle = handle;
    }

    @Override
    public InterfaceMigrationPlan getPlan() {
      return null;
    }

    @Override
    public boolean isSuccessful() {
      return true;
    }

    @Override
    public java.time.Instant getStartTime() {
      return java.time.Instant.now();
    }

    @Override
    public java.time.Instant getEndTime() {
      return java.time.Instant.now();
    }

    @Override
    public java.time.Duration getActualDuration() {
      return java.time.Duration.ZERO;
    }

    @Override
    public List<MigrationStep> getCompletedSteps() {
      return Collections.emptyList();
    }

    @Override
    public List<MigrationStep> getFailedSteps() {
      return Collections.emptyList();
    }

    @Override
    public java.util.Optional<Exception> getError() {
      return java.util.Optional.empty();
    }

    @Override
    public Map<String, Object> getMetrics() {
      return Collections.emptyMap();
    }
  }

  private static class JniInterfaceEvolutionHistory implements InterfaceEvolutionHistory {
    private final long handle;

    JniInterfaceEvolutionHistory(final long handle) {
      this.handle = handle;
    }

    @Override
    public String getInterfaceName() {
      return "";
    }

    @Override
    public List<WitInterfaceVersion> getVersionHistory() {
      return Collections.emptyList();
    }

    @Override
    public List<VersionChange> getMajorChanges() {
      return Collections.emptyList();
    }

    @Override
    public List<VersionChange> getMinorChanges() {
      return Collections.emptyList();
    }

    @Override
    public List<VersionChange> getPatchChanges() {
      return Collections.emptyList();
    }

    @Override
    public List<DeprecationEvent> getDeprecationHistory() {
      return Collections.emptyList();
    }

    @Override
    public List<CompletedMigration> getMigrationHistory() {
      return Collections.emptyList();
    }
  }
}
