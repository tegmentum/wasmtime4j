package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WitEvolutionMetrics;
import ai.tegmentum.wasmtime4j.WitInterfaceDefinition;
import ai.tegmentum.wasmtime4j.WitInterfaceEvolution;
import ai.tegmentum.wasmtime4j.WitTypeAdapter;
import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama implementation of WIT interface evolution support.
 *
 * <p>This class provides Panama Foreign Function Interface implementation for WebAssembly Interface
 * Type evolution, including backward compatibility checking, type adaptation, and interface
 * migration using Java 23+ Panama FFI.
 *
 * <p>Thread-safe implementation with proper resource management and native memory handling.
 *
 * @since 1.0.0
 */
public final class PanamaWitInterfaceEvolution implements WitInterfaceEvolution {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaWitInterfaceEvolution.class.getName());

  /** Arena for native memory management. */
  private final Arena arena;

  /** Native handle for the evolution manager. */
  private volatile MemorySegment evolutionManagerHandle;

  /** Registered interface versions. */
  private final Map<String, List<WitInterfaceVersion>> versionRegistry = new ConcurrentHashMap<>();

  /** Type adapter registry. */
  private final Map<String, WitTypeAdapter> adapterRegistry = new ConcurrentHashMap<>();

  /** Evolution metrics. */
  private final WitEvolutionMetrics.Builder metricsBuilder = WitEvolutionMetrics.builder();

  /** Lock for thread-safe operations. */
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  /** Native library symbol lookup. */
  private static final SymbolLookup NATIVE_LOOKUP;

  /** Native method handles. */
  private static final MethodHandle CREATE_EVOLUTION_MANAGER;

  private static final MethodHandle DESTROY_EVOLUTION_MANAGER;
  private static final MethodHandle ANALYZE_EVOLUTION;
  private static final MethodHandle CHECK_BACKWARD_COMPATIBILITY;
  private static final MethodHandle CHECK_FORWARD_COMPATIBILITY;
  private static final MethodHandle CREATE_ADAPTER;
  private static final MethodHandle VALIDATE_EVOLUTION_STRATEGY;
  private static final MethodHandle CREATE_MIGRATION_PLAN;
  private static final MethodHandle EXECUTE_MIGRATION;
  private static final MethodHandle GET_EVOLUTION_HISTORY;
  private static final MethodHandle REGISTER_INTERFACE_VERSION;
  private static final MethodHandle DEPRECATE_INTERFACE_VERSION;
  private static final MethodHandle GET_INTERFACE_VERSIONS;
  private static final MethodHandle FIND_COMPATIBLE_VERSION;

  static {
    try {
      // Load native library and get symbol lookup
      NATIVE_LOOKUP = PanamaNativeLibrary.getSymbolLookup();

      // Initialize method handles
      final Linker linker = Linker.nativeLinker();

      CREATE_EVOLUTION_MANAGER =
          linker.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_create_evolution_manager").orElseThrow(),
              FunctionDescriptor.of(ValueLayout.ADDRESS));

      DESTROY_EVOLUTION_MANAGER =
          linker.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_destroy_evolution_manager").orElseThrow(),
              FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

      ANALYZE_EVOLUTION =
          linker.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_analyze_evolution").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      CHECK_BACKWARD_COMPATIBILITY =
          linker.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_check_backward_compatibility").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      CHECK_FORWARD_COMPATIBILITY =
          linker.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_check_forward_compatibility").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      CREATE_ADAPTER =
          linker.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_create_adapter").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      VALIDATE_EVOLUTION_STRATEGY =
          linker.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_validate_evolution_strategy").orElseThrow(),
              FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      CREATE_MIGRATION_PLAN =
          linker.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_create_migration_plan").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      EXECUTE_MIGRATION =
          linker.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_execute_migration").orElseThrow(),
              FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      GET_EVOLUTION_HISTORY =
          linker.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_get_evolution_history").orElseThrow(),
              FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      REGISTER_INTERFACE_VERSION =
          linker.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_register_interface_version").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_BOOLEAN,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      DEPRECATE_INTERFACE_VERSION =
          linker.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_deprecate_interface_version").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_BOOLEAN,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      GET_INTERFACE_VERSIONS =
          linker.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_get_interface_versions").orElseThrow(),
              FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      FIND_COMPATIBLE_VERSION =
          linker.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_find_compatible_version").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      LOGGER.info("Initialized Panama WIT interface evolution method handles");

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to initialize Panama WIT interface evolution", e);
      throw new ExceptionInInitializerError(
          "Failed to initialize Panama WIT interface evolution: " + e.getMessage());
    }
  }

  /**
   * Creates a new Panama WIT interface evolution instance.
   *
   * @throws WasmRuntimeException if native initialization fails
   */
  public PanamaWitInterfaceEvolution() {
    this.arena = Arena.ofShared();

    try {
      this.evolutionManagerHandle = (MemorySegment) CREATE_EVOLUTION_MANAGER.invokeExact();
      if (evolutionManagerHandle.address() == 0L) {
        throw new WasmRuntimeException("Failed to create native evolution manager");
      }
      LOGGER.fine("Created Panama WIT interface evolution manager: " + evolutionManagerHandle);
    } catch (final Throwable e) {
      arena.close();
      throw new WasmRuntimeException("Failed to initialize Panama WIT interface evolution", e);
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

      final MemorySegment fromVersionStr = createNativeString(fromVersion.getVersion());
      final MemorySegment toVersionStr = createNativeString(toVersion.getVersion());
      final MemorySegment fromInterfaceStr =
          createNativeString(serializeInterface(fromVersion.getInterface()));
      final MemorySegment toInterfaceStr =
          createNativeString(serializeInterface(toVersion.getInterface()));

      final MemorySegment analysisHandle =
          (MemorySegment)
              ANALYZE_EVOLUTION.invokeExact(
                  evolutionManagerHandle,
                  fromVersionStr,
                  toVersionStr,
                  fromInterfaceStr,
                  toInterfaceStr);

      if (analysisHandle.address() == 0L) {
        throw new WasmRuntimeException("Failed to analyze interface evolution");
      }

      return new PanamaInterfaceEvolutionAnalysis(arena, analysisHandle);

    } catch (final Throwable e) {
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

      final MemorySegment olderVersionStr = createNativeString(olderVersion.getVersion());
      final MemorySegment newerVersionStr = createNativeString(newerVersion.getVersion());
      final MemorySegment olderInterfaceStr =
          createNativeString(serializeInterface(olderVersion.getInterface()));
      final MemorySegment newerInterfaceStr =
          createNativeString(serializeInterface(newerVersion.getInterface()));

      final MemorySegment compatibilityHandle =
          (MemorySegment)
              CHECK_BACKWARD_COMPATIBILITY.invokeExact(
                  evolutionManagerHandle,
                  olderVersionStr,
                  newerVersionStr,
                  olderInterfaceStr,
                  newerInterfaceStr);

      if (compatibilityHandle.address() == 0L) {
        throw new WasmRuntimeException("Failed to check backward compatibility");
      }

      return new PanamaBackwardCompatibilityResult(arena, compatibilityHandle);

    } catch (final Throwable e) {
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

      final MemorySegment newerVersionStr = createNativeString(newerVersion.getVersion());
      final MemorySegment olderVersionStr = createNativeString(olderVersion.getVersion());
      final MemorySegment newerInterfaceStr =
          createNativeString(serializeInterface(newerVersion.getInterface()));
      final MemorySegment olderInterfaceStr =
          createNativeString(serializeInterface(olderVersion.getInterface()));

      final MemorySegment compatibilityHandle =
          (MemorySegment)
              CHECK_FORWARD_COMPATIBILITY.invokeExact(
                  evolutionManagerHandle,
                  newerVersionStr,
                  olderVersionStr,
                  newerInterfaceStr,
                  olderInterfaceStr);

      if (compatibilityHandle.address() == 0L) {
        throw new WasmRuntimeException("Failed to check forward compatibility");
      }

      return new PanamaForwardCompatibilityResult(arena, compatibilityHandle);

    } catch (final Throwable e) {
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

      final MemorySegment sourceVersionStr = createNativeString(sourceVersion.getVersion());
      final MemorySegment targetVersionStr = createNativeString(targetVersion.getVersion());
      final MemorySegment sourceInterfaceStr =
          createNativeString(serializeInterface(sourceVersion.getInterface()));
      final MemorySegment targetInterfaceStr =
          createNativeString(serializeInterface(targetVersion.getInterface()));
      final MemorySegment adaptationConfigStr =
          createNativeString(serializeAdaptationConfig(adaptationConfig));

      final MemorySegment adapterHandle =
          (MemorySegment)
              CREATE_ADAPTER.invokeExact(
                  evolutionManagerHandle,
                  sourceVersionStr,
                  targetVersionStr,
                  sourceInterfaceStr,
                  targetInterfaceStr,
                  adaptationConfigStr);

      if (adapterHandle.address() == 0L) {
        throw new WasmRuntimeException("Failed to create interface adapter");
      }

      return new PanamaInterfaceAdapter(arena, adapterHandle, sourceVersion, targetVersion);

    } catch (final Throwable e) {
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

      final MemorySegment strategyStr = createNativeString(serializeEvolutionStrategy(strategy));

      final MemorySegment validationHandle =
          (MemorySegment)
              VALIDATE_EVOLUTION_STRATEGY.invokeExact(evolutionManagerHandle, strategyStr);

      if (validationHandle.address() == 0L) {
        throw new WasmRuntimeException("Failed to validate evolution strategy");
      }

      return new PanamaEvolutionValidationResult(arena, validationHandle);

    } catch (final Throwable e) {
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

      final MemorySegment currentInterfaceStr =
          createNativeString(serializeInterface(currentInterface));
      final MemorySegment targetInterfaceStr =
          createNativeString(serializeInterface(targetInterface));
      final MemorySegment migrationConfigStr =
          createNativeString(serializeMigrationConfig(migrationConfig));

      final MemorySegment planHandle =
          (MemorySegment)
              CREATE_MIGRATION_PLAN.invokeExact(
                  evolutionManagerHandle,
                  currentInterfaceStr,
                  targetInterfaceStr,
                  migrationConfigStr);

      if (planHandle.address() == 0L) {
        throw new WasmRuntimeException("Failed to create migration plan");
      }

      return new PanamaInterfaceMigrationPlan(arena, planHandle);

    } catch (final Throwable e) {
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

      if (!(migrationPlan instanceof PanamaInterfaceMigrationPlan)) {
        throw new WasmRuntimeException("Migration plan must be a Panama implementation");
      }

      final PanamaInterfaceMigrationPlan panamaPlan = (PanamaInterfaceMigrationPlan) migrationPlan;
      final MemorySegment executionHandle =
          (MemorySegment)
              EXECUTE_MIGRATION.invokeExact(evolutionManagerHandle, panamaPlan.getHandle());

      if (executionHandle.address() == 0L) {
        throw new WasmRuntimeException("Failed to execute migration");
      }

      return new PanamaMigrationExecutionResult(arena, executionHandle);

    } catch (final Throwable e) {
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

      final MemorySegment interfaceNameStr = createNativeString(interfaceName);

      final MemorySegment historyHandle =
          (MemorySegment)
              GET_EVOLUTION_HISTORY.invokeExact(evolutionManagerHandle, interfaceNameStr);

      if (historyHandle.address() == 0L) {
        throw new WasmRuntimeException(
            "Failed to get evolution history for interface: " + interfaceName);
      }

      return new PanamaInterfaceEvolutionHistory(arena, historyHandle);

    } catch (final Throwable e) {
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

      final MemorySegment interfaceNameStr =
          createNativeString(interfaceVersion.getInterfaceName());
      final MemorySegment versionStr = createNativeString(interfaceVersion.getVersion());
      final MemorySegment interfaceDefStr =
          createNativeString(serializeInterface(interfaceVersion.getInterface()));

      final boolean success =
          (boolean)
              REGISTER_INTERFACE_VERSION.invokeExact(
                  evolutionManagerHandle, interfaceNameStr, versionStr, interfaceDefStr);

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

    } catch (final Throwable e) {
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

      final MemorySegment interfaceNameStr =
          createNativeString(interfaceVersion.getInterfaceName());
      final MemorySegment versionStr = createNativeString(interfaceVersion.getVersion());
      final MemorySegment deprecationInfoStr =
          createNativeString(serializeDeprecationInfo(deprecationInfo));

      final boolean success =
          (boolean)
              DEPRECATE_INTERFACE_VERSION.invokeExact(
                  evolutionManagerHandle, interfaceNameStr, versionStr, deprecationInfoStr);

      if (!success) {
        throw new WasmRuntimeException(
            "Failed to deprecate interface version: " + interfaceVersion.getVersion());
      }

      LOGGER.info(
          "Deprecated interface version: "
              + interfaceVersion.getInterfaceName()
              + "@"
              + interfaceVersion.getVersion());

    } catch (final Throwable e) {
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

      final MemorySegment interfaceNameStr = createNativeString(interfaceName);

      final MemorySegment versionsHandle =
          (MemorySegment)
              GET_INTERFACE_VERSIONS.invokeExact(evolutionManagerHandle, interfaceNameStr);

      if (versionsHandle.address() == 0L) {
        return List.of(); // No versions found
      }

      // Convert native array to interface version list
      return convertToInterfaceVersions(interfaceName, versionsHandle);

    } catch (final Throwable e) {
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

      final MemorySegment interfaceNameStr = createNativeString(interfaceName);
      final MemorySegment requirementsStr =
          createNativeString(serializeCompatibilityRequirements(requirements));

      final MemorySegment compatibleVersionHandle =
          (MemorySegment)
              FIND_COMPATIBLE_VERSION.invokeExact(
                  evolutionManagerHandle, interfaceNameStr, requirementsStr);

      if (compatibleVersionHandle.address() == 0L) {
        return java.util.Optional.empty();
      }

      // Create interface version from found version
      final String versionStr = readNativeString(compatibleVersionHandle);
      return createInterfaceVersion(interfaceName, versionStr);

    } catch (final Throwable e) {
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
      if (evolutionManagerHandle != null && evolutionManagerHandle.address() != 0L) {
        DESTROY_EVOLUTION_MANAGER.invokeExact(evolutionManagerHandle);
        evolutionManagerHandle = MemorySegment.NULL;
        LOGGER.fine("Closed Panama WIT interface evolution manager");
      }
    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "Error closing Panama WIT interface evolution manager", e);
    } finally {
      try {
        arena.close();
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Error closing Panama arena", e);
      }
      lock.writeLock().unlock();
    }
  }

  // Private helper methods

  private void checkNativeHandle() {
    if (evolutionManagerHandle == null || evolutionManagerHandle.address() == 0L) {
      throw new WasmRuntimeException("Evolution manager has been closed");
    }
  }

  private MemorySegment createNativeString(final String str) {
    final byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
    final MemorySegment segment = arena.allocate(bytes.length + 1);
    segment.copyFrom(MemorySegment.ofArray(bytes));
    segment.set(ValueLayout.JAVA_BYTE, bytes.length, (byte) 0); // Null terminator
    return segment;
  }

  private String readNativeString(final MemorySegment segment) {
    return segment.reinterpret(Long.MAX_VALUE).getString(0, StandardCharsets.UTF_8);
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
      final String interfaceName, final MemorySegment versionsHandle) {
    // Convert native array to interface version objects
    // This is a simplified implementation
    return List.of(); // Would parse actual version array
  }

  private java.util.Optional<WitInterfaceVersion> createInterfaceVersion(
      final String interfaceName, final String version) {
    // Create interface version from name and version string
    // This is a simplified implementation
    return java.util.Optional.empty(); // Would create actual version object
  }

  // Inner classes for Panama implementations would be implemented here
  // These are simplified stubs for the implementation

  private static class PanamaInterfaceEvolutionAnalysis implements InterfaceEvolutionAnalysis {
    private final Arena arena;
    private final MemorySegment handle;

    PanamaInterfaceEvolutionAnalysis(final Arena arena, final MemorySegment handle) {
      this.arena = arena;
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
      return List.of();
    }

    @Override
    public List<NonBreakingChange> getNonBreakingChanges() {
      return List.of();
    }

    @Override
    public List<RequiredAdaptation> getRequiredAdaptations() {
      return List.of();
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

  private static class PanamaBackwardCompatibilityResult implements BackwardCompatibilityResult {
    private final Arena arena;
    private final MemorySegment handle;

    PanamaBackwardCompatibilityResult(final Arena arena, final MemorySegment handle) {
      this.arena = arena;
      this.handle = handle;
    }

    @Override
    public boolean isBackwardCompatible() {
      return true;
    }

    @Override
    public List<CompatibilityIssue> getIssues() {
      return List.of();
    }

    @Override
    public CompatibilityLevel getCompatibilityLevel() {
      return CompatibilityLevel.FULL;
    }

    @Override
    public List<String> getSuggestions() {
      return List.of();
    }
  }

  private static class PanamaForwardCompatibilityResult implements ForwardCompatibilityResult {
    private final Arena arena;
    private final MemorySegment handle;

    PanamaForwardCompatibilityResult(final Arena arena, final MemorySegment handle) {
      this.arena = arena;
      this.handle = handle;
    }

    @Override
    public boolean isForwardCompatible() {
      return true;
    }

    @Override
    public List<CompatibilityIssue> getIssues() {
      return List.of();
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

  private static class PanamaInterfaceAdapter implements InterfaceAdapter {
    private final Arena arena;
    private final MemorySegment handle;
    private final WitInterfaceVersion sourceVersion;
    private final WitInterfaceVersion targetVersion;

    PanamaInterfaceAdapter(
        final Arena arena,
        final MemorySegment handle,
        final WitInterfaceVersion sourceVersion,
        final WitInterfaceVersion targetVersion) {
      this.arena = arena;
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

  private static class PanamaEvolutionValidationResult implements EvolutionValidationResult {
    private final Arena arena;
    private final MemorySegment handle;

    PanamaEvolutionValidationResult(final Arena arena, final MemorySegment handle) {
      this.arena = arena;
      this.handle = handle;
    }

    // Stub implementation
  }

  private static class PanamaInterfaceMigrationPlan implements InterfaceMigrationPlan {
    private final Arena arena;
    private final MemorySegment handle;

    PanamaInterfaceMigrationPlan(final Arena arena, final MemorySegment handle) {
      this.arena = arena;
      this.handle = handle;
    }

    MemorySegment getHandle() {
      return handle;
    }

    @Override
    public String getId() {
      return "migration-" + handle.address();
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
      return List.of();
    }

    @Override
    public java.time.Duration getEstimatedDuration() {
      return java.time.Duration.ZERO;
    }

    @Override
    public List<MigrationRisk> getRisks() {
      return List.of();
    }

    @Override
    public List<MigrationStep> getRollbackSteps() {
      return List.of();
    }
  }

  private static class PanamaMigrationExecutionResult implements MigrationExecutionResult {
    private final Arena arena;
    private final MemorySegment handle;

    PanamaMigrationExecutionResult(final Arena arena, final MemorySegment handle) {
      this.arena = arena;
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
      return List.of();
    }

    @Override
    public List<MigrationStep> getFailedSteps() {
      return List.of();
    }

    @Override
    public java.util.Optional<Exception> getError() {
      return java.util.Optional.empty();
    }

    @Override
    public Map<String, Object> getMetrics() {
      return Map.of();
    }
  }

  private static class PanamaInterfaceEvolutionHistory implements InterfaceEvolutionHistory {
    private final Arena arena;
    private final MemorySegment handle;

    PanamaInterfaceEvolutionHistory(final Arena arena, final MemorySegment handle) {
      this.arena = arena;
      this.handle = handle;
    }

    @Override
    public String getInterfaceName() {
      return "";
    }

    @Override
    public List<WitInterfaceVersion> getVersionHistory() {
      return List.of();
    }

    @Override
    public List<VersionChange> getMajorChanges() {
      return List.of();
    }

    @Override
    public List<VersionChange> getMinorChanges() {
      return List.of();
    }

    @Override
    public List<VersionChange> getPatchChanges() {
      return List.of();
    }

    @Override
    public List<DeprecationEvent> getDeprecationHistory() {
      return List.of();
    }

    @Override
    public List<CompletedMigration> getMigrationHistory() {
      return List.of();
    }
  }
}
