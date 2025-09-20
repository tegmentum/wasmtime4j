package ai.tegmentum.wasmtime4j.wasi.impl;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.exception.WasiResourceException;
import ai.tegmentum.wasmtime4j.wasi.WasiInstance;
import ai.tegmentum.wasmtime4j.wasi.WasiResource;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceHandle;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceMetadata;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceState;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceStats;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Generic implementation of WasiResource for basic resource types.
 *
 * <p>This implementation provides a foundation for WASI Preview 2 resources with common
 * functionality including lifecycle management, metadata tracking, and basic operations.
 * It can be extended for specific resource types or used directly for simple resources.
 *
 * @since 1.0.0
 */
public class WasiGenericResourceImpl implements WasiResource {

  private static final Logger LOGGER = Logger.getLogger(WasiGenericResourceImpl.class.getName());

  private final long id;
  private final String name;
  private final String type;
  private final WasiResourceConfig config;
  private final Instant createdAt;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicLong accessCount = new AtomicLong(0);

  private volatile WasiInstance owner;
  private volatile Instant lastAccessedAt;
  private volatile boolean owned = true;

  /**
   * Creates a new generic WASI resource.
   *
   * @param id the unique resource identifier
   * @param name the resource name
   * @param type the resource type name
   * @param config the resource configuration
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  public WasiGenericResourceImpl(final long id, final String name, final String type,
      final WasiResourceConfig config) {
    if (id <= 0) {
      throw new IllegalArgumentException("Resource ID must be positive");
    }
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Resource name cannot be null or empty");
    }
    if (type == null || type.trim().isEmpty()) {
      throw new IllegalArgumentException("Resource type cannot be null or empty");
    }

    this.id = id;
    this.name = name.trim();
    this.type = type.trim();
    this.config = Objects.requireNonNull(config, "config");
    this.createdAt = Instant.now();

    LOGGER.fine("Created generic WASI resource: " + name + " (type: " + type + ")");
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public WasiInstance getOwner() {
    return owner;
  }

  @Override
  public boolean isOwned() {
    return owned && !closed.get();
  }

  @Override
  public boolean isValid() {
    return !closed.get();
  }

  @Override
  public Instant getCreatedAt() {
    return createdAt;
  }

  @Override
  public Optional<Instant> getLastAccessedAt() {
    return Optional.ofNullable(lastAccessedAt);
  }

  @Override
  public WasiResourceMetadata getMetadata() throws WasmException {
    ensureValid();
    recordAccess();

    return new WasiResourceMetadata() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public String getType() {
        return type;
      }

      @Override
      public long getId() {
        return id;
      }

      @Override
      public Instant getCreatedAt() {
        return createdAt;
      }

      @Override
      public Optional<Instant> getLastAccessedAt() {
        return WasiGenericResourceImpl.this.getLastAccessedAt();
      }

      @Override
      public Map<String, Object> getProperties() {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("name", name);
        properties.put("type", type);
        properties.put("owned", owned);
        properties.put("access_count", accessCount.get());
        properties.put("config", config.toString());
        return Collections.unmodifiableMap(properties);
      }

      @Override
      public long getSize() {
        // Generic implementation returns estimated size
        return 64 + name.length() + type.length(); // Basic overhead estimate
      }
    };
  }

  @Override
  public WasiResourceState getState() throws WasmException {
    ensureValid();
    recordAccess();

    return new WasiResourceState() {
      @Override
      public String getName() {
        return closed.get() ? "CLOSED" : "ACTIVE";
      }

      @Override
      public boolean isActive() {
        return !closed.get();
      }

      @Override
      public Map<String, Object> getStateData() {
        final Map<String, Object> state = new HashMap<>();
        state.put("closed", closed.get());
        state.put("owned", owned);
        state.put("valid", isValid());
        return Collections.unmodifiableMap(state);
      }
    };
  }

  @Override
  public WasiResourceStats getStats() {
    return new WasiResourceStats() {
      @Override
      public long getAccessCount() {
        return accessCount.get();
      }

      @Override
      public Instant getCreatedAt() {
        return createdAt;
      }

      @Override
      public Optional<Instant> getLastAccessedAt() {
        return WasiGenericResourceImpl.this.getLastAccessedAt();
      }

      @Override
      public long getOperationCount() {
        return accessCount.get(); // Generic implementation uses access count
      }

      @Override
      public long getErrorCount() {
        return 0; // Generic implementation doesn't track errors separately
      }

      @Override
      public Map<String, Object> getCustomStats() {
        final Map<String, Object> stats = new HashMap<>();
        stats.put("resource_type", type);
        stats.put("resource_name", name);
        return Collections.unmodifiableMap(stats);
      }
    };
  }

  @Override
  public Object invoke(final String operation, final Object... parameters) throws WasmException {
    Objects.requireNonNull(operation, "operation");
    ensureValid();
    recordAccess();

    // Generic implementation supports basic operations
    switch (operation.toLowerCase()) {
      case "get_info":
        return getResourceInfo();
      case "get_config":
        return config;
      case "get_access_count":
        return accessCount.get();
      case "is_owned":
        return owned;
      default:
        throw new WasiResourceException("Unsupported operation: " + operation);
    }
  }

  @Override
  public List<String> getAvailableOperations() {
    if (!isValid()) {
      return Collections.emptyList();
    }

    return Arrays.asList(
        "get_info",
        "get_config",
        "get_access_count",
        "is_owned"
    );
  }

  @Override
  public WasiResourceHandle createHandle() throws WasmException {
    ensureValid();
    recordAccess();

    return new WasiResourceHandle() {
      @Override
      public long getResourceId() {
        return id;
      }

      @Override
      public String getResourceType() {
        return type;
      }

      @Override
      public boolean isValid() {
        return WasiGenericResourceImpl.this.isValid();
      }

      @Override
      public WasiResource resolve() throws WasmException {
        return WasiGenericResourceImpl.this;
      }
    };
  }

  @Override
  public void transferOwnership(final WasiInstance targetInstance) throws WasmException {
    Objects.requireNonNull(targetInstance, "targetInstance");
    ensureValid();

    if (!owned) {
      throw new IllegalStateException("Cannot transfer ownership of borrowed resource");
    }

    recordAccess();

    this.owner = targetInstance;
    this.owned = false; // Resource is now owned by target instance

    LOGGER.fine("Transferred ownership of resource " + name + " to " + targetInstance);
  }

  @Override
  public void close() {
    if (!owned) {
      throw new IllegalStateException("Cannot close borrowed resource");
    }

    if (closed.compareAndSet(false, true)) {
      try {
        performCleanup();
        LOGGER.fine("Closed WASI resource: " + name);
      } catch (final Exception e) {
        LOGGER.warning("Error during resource cleanup: " + e.getMessage());
      }
    }
  }

  /**
   * Sets the owner of this resource.
   *
   * @param owner the owner instance
   */
  public void setOwner(final WasiInstance owner) {
    this.owner = owner;
  }

  /**
   * Sets the ownership status of this resource.
   *
   * @param owned true if the resource is owned, false if borrowed
   */
  public void setOwned(final boolean owned) {
    this.owned = owned;
  }

  /**
   * Gets the resource name.
   *
   * @return the resource name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the resource configuration.
   *
   * @return the resource configuration
   */
  public WasiResourceConfig getConfig() {
    return config;
  }

  /**
   * Ensures the resource is still valid.
   *
   * @throws WasiResourceException if the resource is invalid
   */
  protected void ensureValid() throws WasiResourceException {
    if (!isValid()) {
      throw new WasiResourceException("Resource is no longer valid: " + name);
    }
  }

  /**
   * Records an access to this resource for statistics.
   */
  protected void recordAccess() {
    accessCount.incrementAndGet();
    lastAccessedAt = Instant.now();
  }

  /**
   * Performs resource-specific cleanup.
   * Subclasses can override this method to provide custom cleanup logic.
   */
  protected void performCleanup() {
    // Default implementation does nothing
    // Subclasses should override to provide specific cleanup
  }

  /**
   * Gets basic resource information.
   *
   * @return a map containing resource information
   */
  private Map<String, Object> getResourceInfo() {
    final Map<String, Object> info = new HashMap<>();
    info.put("id", id);
    info.put("name", name);
    info.put("type", type);
    info.put("owned", owned);
    info.put("created_at", createdAt);
    info.put("access_count", accessCount.get());
    info.put("last_accessed_at", lastAccessedAt);
    return Collections.unmodifiableMap(info);
  }

  @Override
  public String toString() {
    return "WasiResource{" +
        "id=" + id +
        ", name='" + name + '\'' +
        ", type='" + type + '\'' +
        ", owned=" + owned +
        ", valid=" + isValid() +
        '}';
  }
}