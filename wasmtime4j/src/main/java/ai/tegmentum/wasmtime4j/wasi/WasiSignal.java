package ai.tegmentum.wasmtime4j.wasi;

/**
 * Represents WASI process signals.
 *
 * <p>Signals are used for inter-process communication and process control within the WASI
 * environment. This enum provides standard POSIX-like signals adapted for WebAssembly.
 *
 * @since 1.0.0
 */
public enum WasiSignal {
  /** Hang up signal. */
  SIGHUP(1),

  /** Interrupt signal. */
  SIGINT(2),

  /** Quit signal. */
  SIGQUIT(3),

  /** Illegal instruction signal. */
  SIGILL(4),

  /** Trace/breakpoint trap signal. */
  SIGTRAP(5),

  /** Abort signal. */
  SIGABRT(6),

  /** Bus error signal. */
  SIGBUS(7),

  /** Floating point exception signal. */
  SIGFPE(8),

  /** Kill signal (cannot be caught or ignored). */
  SIGKILL(9),

  /** User-defined signal 1. */
  SIGUSR1(10),

  /** Segmentation violation signal. */
  SIGSEGV(11),

  /** User-defined signal 2. */
  SIGUSR2(12),

  /** Broken pipe signal. */
  SIGPIPE(13),

  /** Alarm clock signal. */
  SIGALRM(14),

  /** Termination signal. */
  SIGTERM(15),

  /** Stack fault signal. */
  SIGSTKFLT(16),

  /** Child status changed signal. */
  SIGCHLD(17),

  /** Continue signal. */
  SIGCONT(18),

  /** Stop signal (cannot be caught or ignored). */
  SIGSTOP(19),

  /** Terminal stop signal. */
  SIGTSTP(20),

  /** Background process attempting read. */
  SIGTTIN(21),

  /** Background process attempting write. */
  SIGTTOU(22),

  /** Urgent condition on socket. */
  SIGURG(23),

  /** CPU time limit exceeded. */
  SIGXCPU(24),

  /** File size limit exceeded. */
  SIGXFSZ(25),

  /** Virtual alarm clock. */
  SIGVTALRM(26),

  /** Profiling alarm clock. */
  SIGPROF(27),

  /** Window size change. */
  SIGWINCH(28),

  /** I/O now possible. */
  SIGIO(29),

  /** Power failure restart. */
  SIGPWR(30),

  /** Bad system call. */
  SIGSYS(31);

  private final int code;

  WasiSignal(final int code) {
    this.code = code;
  }

  /**
   * Gets the numeric signal code.
   *
   * @return the signal code
   */
  public int getCode() {
    return code;
  }

  /**
   * Creates a signal from its numeric code.
   *
   * @param code the signal code
   * @return the corresponding WasiSignal
   * @throws IllegalArgumentException if the code is not valid
   */
  public static WasiSignal fromCode(final int code) {
    for (final WasiSignal signal : values()) {
      if (signal.code == code) {
        return signal;
      }
    }
    throw new IllegalArgumentException("Unknown signal code: " + code);
  }
}