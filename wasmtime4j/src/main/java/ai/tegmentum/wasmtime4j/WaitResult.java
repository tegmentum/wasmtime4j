package ai.tegmentum.wasmtime4j;

/**
 * Result of a WebAssembly atomic wait operation.
 *
 * <p>This enum represents the possible outcomes when a thread waits on a shared memory location
 * using {@link WasmMemory#atomicWait32} or {@link WasmMemory#atomicWait64}. It corresponds to the
 * Wasmtime {@code WaitResult} enum.
 *
 * @since 1.0.0
 */
public enum WaitResult {

  /** The thread was woken up by a notify operation. */
  OK(0),

  /** The expected value did not match the value at the memory location. */
  MISMATCH(1),

  /** The wait operation timed out. */
  TIMED_OUT(2);

  private final int nativeCode;

  WaitResult(final int nativeCode) {
    this.nativeCode = nativeCode;
  }

  /**
   * Returns the native integer code for this result.
   *
   * @return the native code (0=OK, 1=MISMATCH, 2=TIMED_OUT)
   */
  public int getNativeCode() {
    return nativeCode;
  }

  /**
   * Converts a native integer code to a {@code WaitResult}.
   *
   * @param code the native code (0=OK, 1=MISMATCH, 2=TIMED_OUT)
   * @return the corresponding WaitResult
   * @throws IllegalArgumentException if the code is not a valid WaitResult code
   */
  public static WaitResult fromNativeCode(final int code) {
    switch (code) {
      case 0:
        return OK;
      case 1:
        return MISMATCH;
      case 2:
        return TIMED_OUT;
      default:
        throw new IllegalArgumentException("Unknown WaitResult native code: " + code);
    }
  }
}
