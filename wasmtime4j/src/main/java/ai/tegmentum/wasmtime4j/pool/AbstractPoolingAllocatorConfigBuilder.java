package ai.tegmentum.wasmtime4j.pool;

/**
 * Shared base class for pooling allocator configuration builder implementations.
 *
 * <p>This class holds all builder state and provides all setter implementations with eager
 * validation. Subclasses need only implement {@link #build()} to construct the runtime-specific
 * {@link PoolingAllocatorConfig} instance.
 *
 * @since 1.0.0
 */
public abstract class AbstractPoolingAllocatorConfigBuilder
    implements PoolingAllocatorConfigBuilder {

  /** Instance pool size. */
  protected int instancePoolSize = PoolingAllocatorConfig.DEFAULT_INSTANCE_POOL_SIZE;

  /** Maximum memory per instance. */
  protected long maxMemoryPerInstance = PoolingAllocatorConfig.DEFAULT_MAX_MEMORY_PER_INSTANCE;

  /** Stack size. */
  protected int stackSize = PoolingAllocatorConfig.DEFAULT_STACK_SIZE;

  /** Maximum stacks. */
  protected int maxStacks = PoolingAllocatorConfig.DEFAULT_MAX_STACKS;

  /** Maximum tables per instance. */
  protected int maxTablesPerInstance = PoolingAllocatorConfig.DEFAULT_MAX_TABLES_PER_INSTANCE;

  /** Maximum tables. */
  protected int maxTables = PoolingAllocatorConfig.DEFAULT_MAX_TABLES;

  /** Memory decommit enabled. */
  protected boolean memoryDecommitEnabled = true;

  /** Pool warming enabled. */
  protected boolean poolWarmingEnabled = true;

  /** Pool warming percentage. */
  protected float poolWarmingPercentage = PoolingAllocatorConfig.DEFAULT_POOL_WARMING_PERCENTAGE;

  /** Total core instances. */
  protected int totalCoreInstances = PoolingAllocatorConfig.DEFAULT_TOTAL_CORE_INSTANCES;

  /** Total component instances. */
  protected int totalComponentInstances = PoolingAllocatorConfig.DEFAULT_TOTAL_COMPONENT_INSTANCES;

  /** Maximum core instances per component. */
  protected int maxCoreInstancesPerComponent =
      PoolingAllocatorConfig.DEFAULT_MAX_CORE_INSTANCES_PER_COMPONENT;

  /** Total GC heaps. */
  protected int totalGcHeaps = PoolingAllocatorConfig.DEFAULT_TOTAL_GC_HEAPS;

  /** Maximum memory size. */
  protected long maxMemorySize = PoolingAllocatorConfig.DEFAULT_MAX_MEMORY_SIZE;

  /** Maximum unused warm slots. */
  protected int maxUnusedWarmSlots = PoolingAllocatorConfig.DEFAULT_MAX_UNUSED_WARM_SLOTS;

  /** Decommit batch size. */
  protected int decommitBatchSize = PoolingAllocatorConfig.DEFAULT_DECOMMIT_BATCH_SIZE;

  /** Linear memory keep resident. */
  protected long linearMemoryKeepResident =
      PoolingAllocatorConfig.DEFAULT_LINEAR_MEMORY_KEEP_RESIDENT;

  /** Table keep resident. */
  protected long tableKeepResident = PoolingAllocatorConfig.DEFAULT_TABLE_KEEP_RESIDENT;

  /** Async stack keep resident. */
  protected long asyncStackKeepResident = PoolingAllocatorConfig.DEFAULT_ASYNC_STACK_KEEP_RESIDENT;

  /** Total memories. */
  protected int totalMemories = PoolingAllocatorConfig.DEFAULT_TOTAL_MEMORIES;

  /** Maximum core instance size. */
  protected long maxCoreInstanceSize = PoolingAllocatorConfig.DEFAULT_MAX_CORE_INSTANCE_SIZE;

  /** Maximum component instance size. */
  protected long maxComponentInstanceSize =
      PoolingAllocatorConfig.DEFAULT_MAX_COMPONENT_INSTANCE_SIZE;

  /** Maximum memories per module. */
  protected int maxMemoriesPerModule = PoolingAllocatorConfig.DEFAULT_MAX_MEMORIES_PER_MODULE;

  /** Maximum memories per component. */
  protected int maxMemoriesPerComponent = PoolingAllocatorConfig.DEFAULT_MAX_MEMORIES_PER_COMPONENT;

  /** Table elements. */
  protected int tableElements = PoolingAllocatorConfig.DEFAULT_TABLE_ELEMENTS;

  /** Memory protection keys enabled. */
  protected ai.tegmentum.wasmtime4j.config.Enabled memoryProtectionKeysEnabled =
      ai.tegmentum.wasmtime4j.config.Enabled.NO;

  /** Maximum memory protection keys. */
  protected int maxMemoryProtectionKeys = PoolingAllocatorConfig.DEFAULT_MAX_MEMORY_PROTECTION_KEYS;

  /** Pagemap scan enabled. */
  protected ai.tegmentum.wasmtime4j.config.Enabled pagemapScanEnabled =
      ai.tegmentum.wasmtime4j.config.Enabled.NO;

  @Override
  public PoolingAllocatorConfigBuilder instancePoolSize(final int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("instancePoolSize must be positive");
    }
    this.instancePoolSize = size;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxMemoryPerInstance(final long bytes) {
    if (bytes <= 0) {
      throw new IllegalArgumentException("maxMemoryPerInstance must be positive");
    }
    this.maxMemoryPerInstance = bytes;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder stackSize(final int bytes) {
    if (bytes <= 0) {
      throw new IllegalArgumentException("stackSize must be positive");
    }
    this.stackSize = bytes;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxStacks(final int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("maxStacks must be positive");
    }
    this.maxStacks = count;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxTablesPerInstance(final int count) {
    if (count < 0) {
      throw new IllegalArgumentException("maxTablesPerInstance cannot be negative");
    }
    this.maxTablesPerInstance = count;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxTables(final int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("maxTables must be positive");
    }
    this.maxTables = count;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder memoryDecommitEnabled(final boolean enabled) {
    this.memoryDecommitEnabled = enabled;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder poolWarmingEnabled(final boolean enabled) {
    this.poolWarmingEnabled = enabled;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder poolWarmingPercentage(final float percentage) {
    if (percentage < 0.0f || percentage > 1.0f) {
      throw new IllegalArgumentException("poolWarmingPercentage must be between 0.0 and 1.0");
    }
    this.poolWarmingPercentage = percentage;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder totalCoreInstances(final int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("totalCoreInstances must be positive");
    }
    this.totalCoreInstances = count;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder totalComponentInstances(final int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("totalComponentInstances must be positive");
    }
    this.totalComponentInstances = count;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxCoreInstancesPerComponent(final int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("maxCoreInstancesPerComponent must be positive");
    }
    this.maxCoreInstancesPerComponent = count;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder totalGcHeaps(final int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("totalGcHeaps must be positive");
    }
    this.totalGcHeaps = count;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxMemorySize(final long bytes) {
    if (bytes <= 0) {
      throw new IllegalArgumentException("maxMemorySize must be positive");
    }
    this.maxMemorySize = bytes;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxUnusedWarmSlots(final int count) {
    if (count < 0) {
      throw new IllegalArgumentException("maxUnusedWarmSlots cannot be negative");
    }
    this.maxUnusedWarmSlots = count;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder decommitBatchSize(final int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("decommitBatchSize must be positive");
    }
    this.decommitBatchSize = size;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder linearMemoryKeepResident(final long bytes) {
    if (bytes < 0) {
      throw new IllegalArgumentException("linearMemoryKeepResident cannot be negative");
    }
    this.linearMemoryKeepResident = bytes;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder tableKeepResident(final long bytes) {
    if (bytes < 0) {
      throw new IllegalArgumentException("tableKeepResident cannot be negative");
    }
    this.tableKeepResident = bytes;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder asyncStackKeepResident(final long bytes) {
    if (bytes < 0) {
      throw new IllegalArgumentException("asyncStackKeepResident cannot be negative");
    }
    this.asyncStackKeepResident = bytes;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder totalMemories(final int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("totalMemories must be positive");
    }
    this.totalMemories = count;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxCoreInstanceSize(final long bytes) {
    if (bytes <= 0) {
      throw new IllegalArgumentException("maxCoreInstanceSize must be positive");
    }
    this.maxCoreInstanceSize = bytes;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxComponentInstanceSize(final long bytes) {
    if (bytes <= 0) {
      throw new IllegalArgumentException("maxComponentInstanceSize must be positive");
    }
    this.maxComponentInstanceSize = bytes;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxMemoriesPerModule(final int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("maxMemoriesPerModule must be positive");
    }
    this.maxMemoriesPerModule = count;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxMemoriesPerComponent(final int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("maxMemoriesPerComponent must be positive");
    }
    this.maxMemoriesPerComponent = count;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder tableElements(final int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("tableElements must be positive");
    }
    this.tableElements = count;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder memoryProtectionKeysEnabled(final boolean enabled) {
    this.memoryProtectionKeysEnabled = ai.tegmentum.wasmtime4j.config.Enabled.fromBoolean(enabled);
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder memoryProtectionKeysEnabled(
      final ai.tegmentum.wasmtime4j.config.Enabled enabled) {
    if (enabled == null) {
      throw new IllegalArgumentException("enabled cannot be null");
    }
    this.memoryProtectionKeysEnabled = enabled;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxMemoryProtectionKeys(final int count) {
    if (count < 0) {
      throw new IllegalArgumentException("maxMemoryProtectionKeys cannot be negative");
    }
    this.maxMemoryProtectionKeys = count;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder pagemapScanEnabled(final boolean enabled) {
    this.pagemapScanEnabled = ai.tegmentum.wasmtime4j.config.Enabled.fromBoolean(enabled);
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder pagemapScanEnabled(
      final ai.tegmentum.wasmtime4j.config.Enabled enabled) {
    if (enabled == null) {
      throw new IllegalArgumentException("enabled cannot be null");
    }
    this.pagemapScanEnabled = enabled;
    return this;
  }
}
