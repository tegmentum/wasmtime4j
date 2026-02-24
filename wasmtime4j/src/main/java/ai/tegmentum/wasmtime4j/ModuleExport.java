package ai.tegmentum.wasmtime4j;

/**
 * An opaque handle representing a pre-resolved export from a WebAssembly module.
 *
 * <p>ModuleExport provides O(1) export lookup by caching the internal index that Wasmtime uses to
 * locate exports. Instead of performing a string-based hash lookup on every access, callers can
 * resolve the export name once via {@link Module#getModuleExport(String)} and then use this handle
 * for repeated fast lookups via {@link Instance#getExport(Store, ModuleExport)}.
 *
 * <p>A ModuleExport is tied to the {@link Module} that created it. Using a ModuleExport with an
 * Instance created from a different Module produces undefined behavior.
 *
 * <p>ModuleExport instances are lightweight and safe to cache for the lifetime of their parent
 * Module.
 *
 * @since 1.0.0
 */
public interface ModuleExport {

  /**
   * Returns the name of the export this handle refers to.
   *
   * @return the export name
   */
  String name();

  /**
   * Returns the native handle for this module export.
   *
   * <p>This is an internal method used by runtime implementations and should not be called by
   * application code.
   *
   * @return the native handle value
   */
  long nativeHandle();
}
