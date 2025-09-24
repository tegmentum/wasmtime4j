package ai.tegmentum.wasmtime4j;

/**
 * Represents the possible states of a WebAssembly thread.
 *
 * <p>WebAssembly threads progress through different states during their lifecycle, similar to Java
 * threads but with WebAssembly-specific semantics.
 *
 * @since 1.0.0
 */
public enum WasmThreadState {

  /** The thread has been created but has not yet started execution. */
  NEW,

  /** The thread is currently executing WebAssembly code. */
  RUNNING,

  /** The thread is waiting for a condition to be met (e.g., atomic wait operation). */
  WAITING,

  /** The thread is waiting for a condition with a timeout. */
  TIMED_WAITING,

  /** The thread is blocked waiting for a resource or synchronization primitive. */
  BLOCKED,

  /** The thread has been suspended by the runtime. */
  SUSPENDED,

  /** The thread has completed execution normally. */
  TERMINATED,

  /** The thread has been terminated due to an error or exception. */
  ERROR,

  /** The thread has been forcibly terminated. */
  KILLED
}
