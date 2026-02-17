package ai.tegmentum.wasmtime4j.pool;

/**
 * Shared base class for pooling allocator configuration implementations.
 *
 * <p>This class holds all configuration state and provides all getter, {@link #validate()}, and
 * {@link #toString()} implementations. Subclasses need only provide constructors that delegate to
 * {@code super(...)}.
 *
 * <p>Instances are immutable once created.
 *
 * @since 1.0.0
 */
@SuppressWarnings("checkstyle:ParameterNumber")
public abstract class AbstractPoolingAllocatorConfig implements PoolingAllocatorConfig {

  private final int instancePoolSize;
  private final long maxMemoryPerInstance;
  private final int stackSize;
  private final int maxStacks;
  private final int maxTablesPerInstance;
  private final int maxTables;
  private final boolean memoryDecommitEnabled;
  private final boolean poolWarmingEnabled;
  private final float poolWarmingPercentage;
  private final int totalCoreInstances;
  private final int totalComponentInstances;
  private final int maxCoreInstancesPerComponent;
  private final int totalGcHeaps;
  private final long maxMemorySize;
  private final int maxUnusedWarmSlots;
  private final int decommitBatchSize;
  private final long linearMemoryKeepResident;
  private final long tableKeepResident;
  private final long asyncStackKeepResident;
  private final int totalMemories;
  private final long maxCoreInstanceSize;
  private final long maxComponentInstanceSize;
  private final int maxMemoriesPerModule;
  private final int maxMemoriesPerComponent;
  private final int tableElements;
  private final boolean memoryProtectionKeysEnabled;
  private final int maxMemoryProtectionKeys;
  private final boolean pagemapScanEnabled;

  /** Creates a new configuration with default values. */
  protected AbstractPoolingAllocatorConfig() {
    this(
        DEFAULT_INSTANCE_POOL_SIZE,
        DEFAULT_MAX_MEMORY_PER_INSTANCE,
        DEFAULT_STACK_SIZE,
        DEFAULT_MAX_STACKS,
        DEFAULT_MAX_TABLES_PER_INSTANCE,
        DEFAULT_MAX_TABLES,
        true,
        true,
        DEFAULT_POOL_WARMING_PERCENTAGE,
        DEFAULT_TOTAL_CORE_INSTANCES,
        DEFAULT_TOTAL_COMPONENT_INSTANCES,
        DEFAULT_MAX_CORE_INSTANCES_PER_COMPONENT,
        DEFAULT_TOTAL_GC_HEAPS,
        DEFAULT_MAX_MEMORY_SIZE,
        DEFAULT_MAX_UNUSED_WARM_SLOTS,
        DEFAULT_DECOMMIT_BATCH_SIZE,
        DEFAULT_LINEAR_MEMORY_KEEP_RESIDENT,
        DEFAULT_TABLE_KEEP_RESIDENT,
        DEFAULT_ASYNC_STACK_KEEP_RESIDENT,
        DEFAULT_TOTAL_MEMORIES,
        DEFAULT_MAX_CORE_INSTANCE_SIZE,
        DEFAULT_MAX_COMPONENT_INSTANCE_SIZE,
        DEFAULT_MAX_MEMORIES_PER_MODULE,
        DEFAULT_MAX_MEMORIES_PER_COMPONENT,
        DEFAULT_TABLE_ELEMENTS,
        false,
        DEFAULT_MAX_MEMORY_PROTECTION_KEYS,
        false);
  }

  /**
   * Creates a new configuration with the original 14 parameters, using defaults for extended
   * fields.
   *
   * @param instancePoolSize the number of instances in the pool
   * @param maxMemoryPerInstance maximum memory per instance in bytes
   * @param stackSize stack size for WebAssembly execution in bytes
   * @param maxStacks maximum number of stacks in the pool
   * @param maxTablesPerInstance maximum tables per instance
   * @param maxTables maximum total tables in the pool
   * @param memoryDecommitEnabled whether memory decommit optimization is enabled
   * @param poolWarmingEnabled whether pool warming is enabled on startup
   * @param poolWarmingPercentage the pool warming percentage (0.0 to 1.0)
   * @param totalCoreInstances maximum concurrent core module instances
   * @param totalComponentInstances maximum concurrent component instances
   * @param maxCoreInstancesPerComponent max core instances per component
   * @param totalGcHeaps maximum concurrent GC heaps
   * @param maxMemorySize maximum memory size for any memory in the pool
   */
  protected AbstractPoolingAllocatorConfig(
      final int instancePoolSize,
      final long maxMemoryPerInstance,
      final int stackSize,
      final int maxStacks,
      final int maxTablesPerInstance,
      final int maxTables,
      final boolean memoryDecommitEnabled,
      final boolean poolWarmingEnabled,
      final float poolWarmingPercentage,
      final int totalCoreInstances,
      final int totalComponentInstances,
      final int maxCoreInstancesPerComponent,
      final int totalGcHeaps,
      final long maxMemorySize) {
    this(
        instancePoolSize,
        maxMemoryPerInstance,
        stackSize,
        maxStacks,
        maxTablesPerInstance,
        maxTables,
        memoryDecommitEnabled,
        poolWarmingEnabled,
        poolWarmingPercentage,
        totalCoreInstances,
        totalComponentInstances,
        maxCoreInstancesPerComponent,
        totalGcHeaps,
        maxMemorySize,
        DEFAULT_MAX_UNUSED_WARM_SLOTS,
        DEFAULT_DECOMMIT_BATCH_SIZE,
        DEFAULT_LINEAR_MEMORY_KEEP_RESIDENT,
        DEFAULT_TABLE_KEEP_RESIDENT,
        DEFAULT_ASYNC_STACK_KEEP_RESIDENT,
        DEFAULT_TOTAL_MEMORIES,
        DEFAULT_MAX_CORE_INSTANCE_SIZE,
        DEFAULT_MAX_COMPONENT_INSTANCE_SIZE,
        DEFAULT_MAX_MEMORIES_PER_MODULE,
        DEFAULT_MAX_MEMORIES_PER_COMPONENT,
        DEFAULT_TABLE_ELEMENTS,
        false,
        DEFAULT_MAX_MEMORY_PROTECTION_KEYS,
        false);
  }

  /**
   * Creates a new configuration with all 28 parameters.
   *
   * @param instancePoolSize the number of instances in the pool
   * @param maxMemoryPerInstance maximum memory per instance in bytes
   * @param stackSize stack size for WebAssembly execution in bytes
   * @param maxStacks maximum number of stacks in the pool
   * @param maxTablesPerInstance maximum tables per instance
   * @param maxTables maximum total tables in the pool
   * @param memoryDecommitEnabled whether memory decommit optimization is enabled
   * @param poolWarmingEnabled whether pool warming is enabled on startup
   * @param poolWarmingPercentage the pool warming percentage (0.0 to 1.0)
   * @param totalCoreInstances maximum concurrent core module instances
   * @param totalComponentInstances maximum concurrent component instances
   * @param maxCoreInstancesPerComponent max core instances per component
   * @param totalGcHeaps maximum concurrent GC heaps
   * @param maxMemorySize maximum memory size for any memory in the pool
   * @param maxUnusedWarmSlots maximum unused warm slots
   * @param decommitBatchSize decommit batch size
   * @param linearMemoryKeepResident linear memory keep resident size
   * @param tableKeepResident table keep resident size
   * @param asyncStackKeepResident async stack keep resident size
   * @param totalMemories total memories in the pool
   * @param maxCoreInstanceSize max core instance size
   * @param maxComponentInstanceSize max component instance size
   * @param maxMemoriesPerModule max memories per module
   * @param maxMemoriesPerComponent max memories per component
   * @param tableElements max table elements
   * @param memoryProtectionKeysEnabled whether MPK is enabled
   * @param maxMemoryProtectionKeys max memory protection keys
   * @param pagemapScanEnabled whether pagemap scan is enabled
   */
  protected AbstractPoolingAllocatorConfig(
      final int instancePoolSize,
      final long maxMemoryPerInstance,
      final int stackSize,
      final int maxStacks,
      final int maxTablesPerInstance,
      final int maxTables,
      final boolean memoryDecommitEnabled,
      final boolean poolWarmingEnabled,
      final float poolWarmingPercentage,
      final int totalCoreInstances,
      final int totalComponentInstances,
      final int maxCoreInstancesPerComponent,
      final int totalGcHeaps,
      final long maxMemorySize,
      final int maxUnusedWarmSlots,
      final int decommitBatchSize,
      final long linearMemoryKeepResident,
      final long tableKeepResident,
      final long asyncStackKeepResident,
      final int totalMemories,
      final long maxCoreInstanceSize,
      final long maxComponentInstanceSize,
      final int maxMemoriesPerModule,
      final int maxMemoriesPerComponent,
      final int tableElements,
      final boolean memoryProtectionKeysEnabled,
      final int maxMemoryProtectionKeys,
      final boolean pagemapScanEnabled) {
    this.instancePoolSize = instancePoolSize;
    this.maxMemoryPerInstance = maxMemoryPerInstance;
    this.stackSize = stackSize;
    this.maxStacks = maxStacks;
    this.maxTablesPerInstance = maxTablesPerInstance;
    this.maxTables = maxTables;
    this.memoryDecommitEnabled = memoryDecommitEnabled;
    this.poolWarmingEnabled = poolWarmingEnabled;
    this.poolWarmingPercentage = poolWarmingPercentage;
    this.totalCoreInstances = totalCoreInstances;
    this.totalComponentInstances = totalComponentInstances;
    this.maxCoreInstancesPerComponent = maxCoreInstancesPerComponent;
    this.totalGcHeaps = totalGcHeaps;
    this.maxMemorySize = maxMemorySize;
    this.maxUnusedWarmSlots = maxUnusedWarmSlots;
    this.decommitBatchSize = decommitBatchSize;
    this.linearMemoryKeepResident = linearMemoryKeepResident;
    this.tableKeepResident = tableKeepResident;
    this.asyncStackKeepResident = asyncStackKeepResident;
    this.totalMemories = totalMemories;
    this.maxCoreInstanceSize = maxCoreInstanceSize;
    this.maxComponentInstanceSize = maxComponentInstanceSize;
    this.maxMemoriesPerModule = maxMemoriesPerModule;
    this.maxMemoriesPerComponent = maxMemoriesPerComponent;
    this.tableElements = tableElements;
    this.memoryProtectionKeysEnabled = memoryProtectionKeysEnabled;
    this.maxMemoryProtectionKeys = maxMemoryProtectionKeys;
    this.pagemapScanEnabled = pagemapScanEnabled;
  }

  @Override
  public int getInstancePoolSize() {
    return instancePoolSize;
  }

  @Override
  public long getMaxMemoryPerInstance() {
    return maxMemoryPerInstance;
  }

  @Override
  public int getStackSize() {
    return stackSize;
  }

  @Override
  public int getMaxStacks() {
    return maxStacks;
  }

  @Override
  public int getMaxTablesPerInstance() {
    return maxTablesPerInstance;
  }

  @Override
  public int getMaxTables() {
    return maxTables;
  }

  @Override
  public boolean isMemoryDecommitEnabled() {
    return memoryDecommitEnabled;
  }

  @Override
  public boolean isPoolWarmingEnabled() {
    return poolWarmingEnabled;
  }

  @Override
  public float getPoolWarmingPercentage() {
    return poolWarmingPercentage;
  }

  @Override
  public int getTotalCoreInstances() {
    return totalCoreInstances;
  }

  @Override
  public int getTotalComponentInstances() {
    return totalComponentInstances;
  }

  @Override
  public int getMaxCoreInstancesPerComponent() {
    return maxCoreInstancesPerComponent;
  }

  @Override
  public int getTotalGcHeaps() {
    return totalGcHeaps;
  }

  @Override
  public long getMaxMemorySize() {
    return maxMemorySize;
  }

  @Override
  public int getMaxUnusedWarmSlots() {
    return maxUnusedWarmSlots;
  }

  @Override
  public int getDecommitBatchSize() {
    return decommitBatchSize;
  }

  @Override
  public long getLinearMemoryKeepResident() {
    return linearMemoryKeepResident;
  }

  @Override
  public long getTableKeepResident() {
    return tableKeepResident;
  }

  @Override
  public long getAsyncStackKeepResident() {
    return asyncStackKeepResident;
  }

  @Override
  public int getTotalMemories() {
    return totalMemories;
  }

  @Override
  public long getMaxCoreInstanceSize() {
    return maxCoreInstanceSize;
  }

  @Override
  public long getMaxComponentInstanceSize() {
    return maxComponentInstanceSize;
  }

  @Override
  public int getMaxMemoriesPerModule() {
    return maxMemoriesPerModule;
  }

  @Override
  public int getMaxMemoriesPerComponent() {
    return maxMemoriesPerComponent;
  }

  @Override
  public int getTableElements() {
    return tableElements;
  }

  @Override
  public boolean isMemoryProtectionKeysEnabled() {
    return memoryProtectionKeysEnabled;
  }

  @Override
  public int getMaxMemoryProtectionKeys() {
    return maxMemoryProtectionKeys;
  }

  @Override
  public boolean isPagemapScanEnabled() {
    return pagemapScanEnabled;
  }

  @Override
  public void validate() {
    if (instancePoolSize <= 0) {
      throw new IllegalArgumentException("instancePoolSize must be positive");
    }
    if (maxMemoryPerInstance <= 0) {
      throw new IllegalArgumentException("maxMemoryPerInstance must be positive");
    }
    if (stackSize <= 0) {
      throw new IllegalArgumentException("stackSize must be positive");
    }
    if (maxStacks <= 0) {
      throw new IllegalArgumentException("maxStacks must be positive");
    }
    if (maxTablesPerInstance < 0) {
      throw new IllegalArgumentException("maxTablesPerInstance cannot be negative");
    }
    if (maxTables <= 0) {
      throw new IllegalArgumentException("maxTables must be positive");
    }
    if (poolWarmingPercentage < 0.0f || poolWarmingPercentage > 1.0f) {
      throw new IllegalArgumentException("poolWarmingPercentage must be between 0.0 and 1.0");
    }
    if (totalCoreInstances <= 0) {
      throw new IllegalArgumentException("totalCoreInstances must be positive");
    }
    if (totalComponentInstances <= 0) {
      throw new IllegalArgumentException("totalComponentInstances must be positive");
    }
    if (maxCoreInstancesPerComponent <= 0) {
      throw new IllegalArgumentException("maxCoreInstancesPerComponent must be positive");
    }
    if (totalGcHeaps <= 0) {
      throw new IllegalArgumentException("totalGcHeaps must be positive");
    }
    if (maxMemorySize <= 0) {
      throw new IllegalArgumentException("maxMemorySize must be positive");
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{"
        + "instancePoolSize="
        + instancePoolSize
        + ", maxMemoryPerInstance="
        + maxMemoryPerInstance
        + ", stackSize="
        + stackSize
        + ", maxStacks="
        + maxStacks
        + ", maxTablesPerInstance="
        + maxTablesPerInstance
        + ", maxTables="
        + maxTables
        + ", memoryDecommitEnabled="
        + memoryDecommitEnabled
        + ", poolWarmingEnabled="
        + poolWarmingEnabled
        + ", poolWarmingPercentage="
        + poolWarmingPercentage
        + ", totalCoreInstances="
        + totalCoreInstances
        + ", totalComponentInstances="
        + totalComponentInstances
        + ", maxCoreInstancesPerComponent="
        + maxCoreInstancesPerComponent
        + ", totalGcHeaps="
        + totalGcHeaps
        + ", maxMemorySize="
        + maxMemorySize
        + ", maxUnusedWarmSlots="
        + maxUnusedWarmSlots
        + ", decommitBatchSize="
        + decommitBatchSize
        + ", linearMemoryKeepResident="
        + linearMemoryKeepResident
        + ", tableKeepResident="
        + tableKeepResident
        + ", asyncStackKeepResident="
        + asyncStackKeepResident
        + ", totalMemories="
        + totalMemories
        + ", maxCoreInstanceSize="
        + maxCoreInstanceSize
        + ", maxComponentInstanceSize="
        + maxComponentInstanceSize
        + ", maxMemoriesPerModule="
        + maxMemoriesPerModule
        + ", maxMemoriesPerComponent="
        + maxMemoriesPerComponent
        + ", tableElements="
        + tableElements
        + ", memoryProtectionKeysEnabled="
        + memoryProtectionKeysEnabled
        + ", maxMemoryProtectionKeys="
        + maxMemoryProtectionKeys
        + ", pagemapScanEnabled="
        + pagemapScanEnabled
        + '}';
  }
}
