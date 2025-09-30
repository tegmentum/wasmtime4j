package ai.tegmentum.wasmtime4j.debug;

/**
 * Types of debugging events that can occur during WebAssembly execution.
 *
 * <p>These events represent various points of interest during execution
 * that can be used for debugging, profiling, and analysis.
 *
 * @since 1.0.0
 */
public enum DebugEventType {
  /** Function entry point reached */
  FUNCTION_ENTRY,

  /** Function exit point reached */
  FUNCTION_EXIT,

  /** Breakpoint hit */
  BREAKPOINT_HIT,

  /** Step operation completed */
  STEP_COMPLETED,

  /** Exception thrown */
  EXCEPTION_THROWN,

  /** Memory access occurred */
  MEMORY_ACCESS,

  /** Memory allocation occurred */
  MEMORY_ALLOCATION,

  /** Memory deallocation occurred */
  MEMORY_DEALLOCATION,

  /** Global variable accessed */
  GLOBAL_ACCESS,

  /** Table access occurred */
  TABLE_ACCESS,

  /** Host function called */
  HOST_FUNCTION_CALL,

  /** WASM function called */
  WASM_FUNCTION_CALL,

  /** Instruction executed */
  INSTRUCTION_EXECUTED,

  /** Module loaded */
  MODULE_LOADED,

  /** Module unloaded */
  MODULE_UNLOADED,

  /** Instance created */
  INSTANCE_CREATED,

  /** Instance destroyed */
  INSTANCE_DESTROYED,

  /** Trap occurred */
  TRAP_OCCURRED,

  /** Performance event */
  PERFORMANCE_EVENT,

  /** Custom debug event */
  CUSTOM_EVENT;

  /**
   * Checks if this event type represents a function-related event.
   *
   * @return true if function-related
   */
  public boolean isFunctionEvent() {
    return this == FUNCTION_ENTRY || this == FUNCTION_EXIT ||
           this == HOST_FUNCTION_CALL || this == WASM_FUNCTION_CALL;
  }

  /**
   * Checks if this event type represents a memory-related event.
   *
   * @return true if memory-related
   */
  public boolean isMemoryEvent() {
    return this == MEMORY_ACCESS || this == MEMORY_ALLOCATION || this == MEMORY_DEALLOCATION;
  }

  /**
   * Checks if this event type represents an error or exception event.
   *
   * @return true if error-related
   */
  public boolean isErrorEvent() {
    return this == EXCEPTION_THROWN || this == TRAP_OCCURRED;
  }

  /**
   * Checks if this event type represents a lifecycle event.
   *
   * @return true if lifecycle-related
   */
  public boolean isLifecycleEvent() {
    return this == MODULE_LOADED || this == MODULE_UNLOADED ||
           this == INSTANCE_CREATED || this == INSTANCE_DESTROYED;
  }

  /**
   * Gets a human-readable description of this event type.
   *
   * @return description string
   */
  public String getDescription() {
    return switch (this) {
      case FUNCTION_ENTRY -> "Function entry";
      case FUNCTION_EXIT -> "Function exit";
      case BREAKPOINT_HIT -> "Breakpoint hit";
      case STEP_COMPLETED -> "Step completed";
      case EXCEPTION_THROWN -> "Exception thrown";
      case MEMORY_ACCESS -> "Memory access";
      case MEMORY_ALLOCATION -> "Memory allocation";
      case MEMORY_DEALLOCATION -> "Memory deallocation";
      case GLOBAL_ACCESS -> "Global variable access";
      case TABLE_ACCESS -> "Table access";
      case HOST_FUNCTION_CALL -> "Host function call";
      case WASM_FUNCTION_CALL -> "WebAssembly function call";
      case INSTRUCTION_EXECUTED -> "Instruction executed";
      case MODULE_LOADED -> "Module loaded";
      case MODULE_UNLOADED -> "Module unloaded";
      case INSTANCE_CREATED -> "Instance created";
      case INSTANCE_DESTROYED -> "Instance destroyed";
      case TRAP_OCCURRED -> "Trap occurred";
      case PERFORMANCE_EVENT -> "Performance event";
      case CUSTOM_EVENT -> "Custom event";
    };
  }
}