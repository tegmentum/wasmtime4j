package ai.tegmentum.wasmtime4j.resource;

/**
 * Classification of different resource types for management and monitoring purposes.
 *
 * <p>ResourceType provides categorization of resources to enable type-specific management policies,
 * monitoring, and cleanup strategies.
 *
 * @since 1.0.0
 */
public enum ResourceType {

  /**
   * WebAssembly engine instances.
   *
   * <p>Engines are typically long-lived resources that manage compilation settings and shared state
   * across multiple modules.
   */
  ENGINE("Engine", "WebAssembly engine instances", true, 64L * 1024 * 1024), // 64MB

  /**
   * Compiled WebAssembly modules.
   *
   * <p>Modules contain compiled WebAssembly code and can be instantiated multiple times to create
   * instances.
   */
  MODULE("Module", "Compiled WebAssembly modules", true, 16L * 1024 * 1024), // 16MB

  /**
   * WebAssembly module instances.
   *
   * <p>Instances are runtime representations of modules with their own memory, globals, and
   * execution state.
   */
  INSTANCE("Instance", "WebAssembly module instances", false, 8L * 1024 * 1024), // 8MB

  /**
   * WebAssembly linear memory objects.
   *
   * <p>Memory objects represent the linear memory space accessible to WebAssembly code.
   */
  MEMORY("Memory", "WebAssembly linear memory objects", false, 256L * 1024 * 1024), // 256MB

  /**
   * WebAssembly global variables.
   *
   * <p>Globals are variables that can be shared between instances and the host environment.
   */
  GLOBAL("Global", "WebAssembly global variables", false, 1024L), // 1KB

  /**
   * WebAssembly table objects.
   *
   * <p>Tables contain references to functions or other objects that can be indirectly called or
   * accessed.
   */
  TABLE("Table", "WebAssembly table objects", false, 1L * 1024 * 1024), // 1MB

  /**
   * WebAssembly function references.
   *
   * <p>Functions are callable references to WebAssembly or host functions.
   */
  FUNCTION("Function", "WebAssembly function references", true, 4096L), // 4KB

  /**
   * WASI Preview 2 resources.
   *
   * <p>WASI resources provide system interface capabilities like filesystem access, networking, and
   * process management.
   */
  WASI_RESOURCE("WasiResource", "WASI Preview 2 resources", false, 4L * 1024 * 1024), // 4MB

  /**
   * Component model components.
   *
   * <p>Components are higher-level abstractions that can contain multiple modules and provide
   * component-level interfaces.
   */
  COMPONENT("Component", "Component model components", true, 32L * 1024 * 1024), // 32MB

  /**
   * Component model instances.
   *
   * <p>Component instances are runtime representations of components with their own isolated state
   * and interfaces.
   */
  COMPONENT_INSTANCE(
      "ComponentInstance", "Component model instances", false, 16L * 1024 * 1024), // 16MB

  /**
   * Linker objects for module linking.
   *
   * <p>Linkers manage the binding of imports and exports between modules and the host environment.
   */
  LINKER("Linker", "Module linker objects", true, 2L * 1024 * 1024), // 2MB

  /**
   * Store objects for resource management.
   *
   * <p>Stores manage the lifetime of WebAssembly objects and provide isolation between different
   * execution contexts.
   */
  STORE("Store", "WebAssembly store objects", false, 32L * 1024 * 1024), // 32MB

  /**
   * Native library handles.
   *
   * <p>Native handles represent underlying native resources that require explicit cleanup to
   * prevent resource leaks.
   */
  NATIVE_HANDLE("NativeHandle", "Native library handles", false, 8192L), // 8KB

  /**
   * Cached objects and compiled artifacts.
   *
   * <p>Cached resources are temporary objects stored for performance optimization and can be
   * evicted when memory pressure occurs.
   */
  CACHED_OBJECT("CachedObject", "Cached objects and artifacts", true, 4L * 1024 * 1024), // 4MB

  /**
   * Thread pool and executor resources.
   *
   * <p>Thread resources manage concurrent execution and should be carefully managed to prevent
   * thread leaks.
   */
  THREAD_RESOURCE(
      "ThreadResource", "Thread pool and executor resources", false, 512L * 1024), // 512KB

  /**
   * Generic managed resources.
   *
   * <p>Generic resources are catch-all category for custom resource types that don't fit into other
   * categories.
   */
  GENERIC("Generic", "Generic managed resources", false, 1L * 1024 * 1024); // 1MB

  private final String name;
  private final String description;
  private final boolean cacheable;
  private final long estimatedMemoryUsage;

  ResourceType(
      final String name,
      final String description,
      final boolean cacheable,
      final long estimatedMemoryUsage) {
    this.name = name;
    this.description = description;
    this.cacheable = cacheable;
    this.estimatedMemoryUsage = estimatedMemoryUsage;
  }

  /**
   * Gets the human-readable name of this resource type.
   *
   * @return the resource type name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the description of this resource type.
   *
   * @return the resource type description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Checks if resources of this type can be cached.
   *
   * @return true if resources of this type can be cached, false otherwise
   */
  public boolean isCacheable() {
    return cacheable;
  }

  /**
   * Gets the estimated memory usage for resources of this type.
   *
   * @return the estimated memory usage in bytes
   */
  public long getEstimatedMemoryUsage() {
    return estimatedMemoryUsage;
  }

  /**
   * Checks if resources of this type are considered heavy-weight.
   *
   * <p>Heavy-weight resources typically require more careful management and may have stricter
   * limits on concurrent instances.
   *
   * @return true if resources of this type are heavy-weight, false otherwise
   */
  public boolean isHeavyWeight() {
    return estimatedMemoryUsage > 8L * 1024 * 1024; // 8MB threshold
  }

  /**
   * Checks if resources of this type are thread-safe.
   *
   * <p>Thread-safe resources can be safely accessed from multiple threads without additional
   * synchronization.
   *
   * @return true if resources of this type are thread-safe, false otherwise
   */
  public boolean isThreadSafe() {
    // Most WebAssembly resources are not thread-safe except for engines and modules
    return this == ENGINE
        || this == MODULE
        || this == FUNCTION
        || this == COMPONENT
        || this == LINKER
        || this == CACHED_OBJECT;
  }

  /**
   * Gets the default cleanup priority for resources of this type.
   *
   * <p>Higher values indicate higher priority for cleanup when resource limits are exceeded.
   *
   * @return the cleanup priority (1-10, where 10 is highest priority)
   */
  public int getCleanupPriority() {
    switch (this) {
      case CACHED_OBJECT:
        return 10; // Highest priority - can be recreated
      case NATIVE_HANDLE:
        return 9; // High priority - prevent native leaks
      case THREAD_RESOURCE:
        return 8; // High priority - prevent thread leaks
      case INSTANCE:
      case COMPONENT_INSTANCE:
        return 7; // Medium-high priority - can be recreated from modules
      case MEMORY:
      case TABLE:
      case GLOBAL:
        return 6; // Medium priority - instance-specific
      case WASI_RESOURCE:
        return 5; // Medium priority - may have persistent state
      case STORE:
        return 4; // Medium-low priority - may contain multiple resources
      case FUNCTION:
        return 3; // Low priority - usually lightweight
      case MODULE:
      case COMPONENT:
        return 2; // Lower priority - expensive to recreate
      case LINKER:
        return 2; // Lower priority - configured state
      case ENGINE:
        return 1; // Lowest priority - very expensive to recreate
      case GENERIC:
      default:
        return 5; // Default medium priority
    }
  }
}
