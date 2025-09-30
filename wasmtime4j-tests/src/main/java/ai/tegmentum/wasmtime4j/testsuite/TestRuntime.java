package ai.tegmentum.wasmtime4j.testsuite;

/** Enumeration of supported WebAssembly runtime implementations for testing. */
public enum TestRuntime {
  /** JNI-based runtime implementation using native Wasmtime bindings. */
  JNI("jni", "JNI Runtime", "Native JNI-based Wasmtime runtime"),

  /** Panama Foreign Function API-based runtime implementation. */
  PANAMA("panama", "Panama Runtime", "Panama FFI-based Wasmtime runtime");

  private final String id;
  private final String displayName;
  private final String description;

  TestRuntime(final String id, final String displayName, final String description) {
    this.id = id;
    this.displayName = displayName;
    this.description = description;
  }

  public String getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Gets TestRuntime by ID.
   *
   * @param id runtime ID
   * @return TestRuntime or null if not found
   */
  public static TestRuntime fromId(final String id) {
    if (id == null) {
      return null;
    }
    for (final TestRuntime runtime : values()) {
      if (runtime.id.equals(id)) {
        return runtime;
      }
    }
    return null;
  }
}
