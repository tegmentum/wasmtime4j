package ai.tegmentum.wasmtime4j;

/**
 * WebAssembly Component Model features that can be enabled or disabled in a component engine
 * configuration.
 *
 * <p>These features extend the core Component Model specification with advanced capabilities for
 * enterprise and production use cases.
 *
 * @since 1.0.0
 */
public enum ComponentFeature {
  /** Core Component Model with WIT interface support. */
  COMPONENT_MODEL,

  /** Advanced component orchestration with dependency resolution. */
  ORCHESTRATION,

  /** Component hot-swapping and live updates. */
  HOT_SWAPPING,

  /** Distributed component support with networking. */
  DISTRIBUTED_COMPONENTS,

  /** Component clustering and coordination. */
  CLUSTERING,

  /** Enterprise audit logging and compliance. */
  AUDIT_LOGGING,

  /** Component security policies and enforcement. */
  SECURITY_POLICIES,

  /** Component monitoring and metrics collection. */
  MONITORING,

  /** Component diagnostics and debugging tools. */
  DIAGNOSTICS,

  /** WIT interface compatibility checking and migration. */
  WIT_COMPATIBILITY,

  /** Component resource optimization and management. */
  RESOURCE_OPTIMIZATION,

  /** Component performance auto-scaling. */
  AUTO_SCALING,

  /** Component backup and restore capabilities. */
  BACKUP_RESTORE,

  /** Component load balancing and distribution. */
  LOAD_BALANCING,

  /** Component configuration management and deployment. */
  CONFIG_MANAGEMENT
}
