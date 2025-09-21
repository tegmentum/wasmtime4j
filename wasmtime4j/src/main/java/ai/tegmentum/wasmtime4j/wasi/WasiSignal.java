package ai.tegmentum.wasmtime4j.wasi;

/**
 * Enumeration of WASI signals that can be sent to processes.
 *
 * <p>These signals correspond to common Unix signals but are abstracted for the WASI environment.
 * Not all signals may be supported on all platforms or WASI implementations.
 *
 * @since 1.0.0
 */
public enum WasiSignal {

  /** No signal (used for testing if a process exists). */
  NONE(0),

  /** Hangup detected on controlling terminal or death of controlling process. */
  HUP(1),

  /** Interrupt from keyboard (Ctrl+C). */
  INT(2),

  /** Quit from keyboard (Ctrl+\). */
  QUIT(3),

  /** Illegal instruction. */
  ILL(4),

  /** Trace/breakpoint trap. */
  TRAP(5),

  /** Abort signal from abort() function. */
  ABRT(6),

  /** Bus error (bad memory access). */
  BUS(7),

  /** Floating point exception. */
  FPE(8),

  /** Kill signal (cannot be caught or ignored). */
  KILL(9),

  /** User-defined signal 1. */
  USR1(10),

  /** Invalid memory reference. */
  SEGV(11),

  /** User-defined signal 2. */
  USR2(12),

  /** Broken pipe: write to pipe with no readers. */
  PIPE(13),

  /** Timer signal from alarm(). */
  ALRM(14),

  /** Termination signal. */
  TERM(15),

  /** Stack fault on coprocessor. */
  STKFLT(16),

  /** Child stopped or terminated. */
  CHLD(17),

  /** Continue if stopped. */
  CONT(18),

  /** Stop process. */
  STOP(19),

  /** Stop typed at terminal. */
  TSTP(20),

  /** Terminal input for background process. */
  TTIN(21),

  /** Terminal output for background process. */
  TTOU(22),

  /** Urgent condition on socket. */
  URG(23),

  /** CPU time limit exceeded. */
  XCPU(24),

  /** File size limit exceeded. */
  XFSZ(25),

  /** Virtual alarm clock. */
  VTALRM(26),

  /** Profiling timer expired. */
  PROF(27),

  /** Window resize signal. */
  WINCH(28),

  /** I/O now possible. */
  IO(29),

  /** Power failure. */
  PWR(30),

  /** Bad system call. */
  SYS(31);

  private final int value;

  WasiSignal(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this signal.
   *
   * @return the signal number
   */
  public int getValue() {
    return value;
  }

  /**
   * Converts a numeric value to the corresponding WasiSignal.
   *
   * @param value the numeric value
   * @return the corresponding WasiSignal
   * @throws IllegalArgumentException if the value doesn't correspond to a known signal
   */
  public static WasiSignal fromValue(final int value) {
    for (final WasiSignal signal : values()) {
      if (signal.value == value) {
        return signal;
      }
    }
    throw new IllegalArgumentException("Unknown WASI signal value: " + value);
  }

  /**
   * Checks if this signal is a termination signal.
   *
   * <p>Termination signals typically cause the process to exit when received.
   *
   * @return true if this is a termination signal, false otherwise
   */
  public boolean isTerminationSignal() {
    return this == TERM || this == KILL || this == INT || this == QUIT || this == ABRT;
  }

  /**
   * Checks if this signal is a stop signal.
   *
   * <p>Stop signals cause the process to suspend execution.
   *
   * @return true if this is a stop signal, false otherwise
   */
  public boolean isStopSignal() {
    return this == STOP || this == TSTP;
  }

  /**
   * Checks if this signal can be caught or ignored by the process.
   *
   * <p>Some signals (like KILL and STOP) cannot be caught, blocked, or ignored.
   *
   * @return true if the signal can be caught, false otherwise
   */
  public boolean isCatchable() {
    return this != KILL && this != STOP;
  }

  @Override
  public String toString() {
    return name() + "(" + value + ")";
  }
}
