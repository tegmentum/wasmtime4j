package ai.tegmentum.wasmtime4j.config;

/**
 * Strategy for module version validation during deserialization.
 *
 * <p>When deserializing precompiled modules, this controls how version compatibility is checked
 * between the module and the engine that compiled it.
 *
 * @since 1.0.0
 */
public enum ModuleVersionStrategy {
  /** Use the Wasmtime version for module compatibility checks (default). */
  WASMTIME_VERSION,
  /** Disable version checks entirely. */
  NONE,
  /** Use a custom version string for compatibility checks. */
  CUSTOM
}
