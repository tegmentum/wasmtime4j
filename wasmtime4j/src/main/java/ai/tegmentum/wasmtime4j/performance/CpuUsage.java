package ai.tegmentum.wasmtime4j.performance;

import java.util.Objects;

/**
 * Detailed CPU usage information for system and process-level monitoring.
 *
 * <p>This class provides comprehensive CPU statistics including user time, system time,
 * total utilization, context switches, and system calls.
 *
 * @since 1.0.0
 */
public final class CpuUsage {
  private final double userCpuTime;
  private final double systemCpuTime;
  private final double totalCpuTime;
  private final double cpuUtilization;
  private final long contextSwitches;
  private final long systemCalls;

  /**
   * Creates a CPU usage record.
   *
   * @param userCpuTime CPU time spent in user mode (0.0 to 1.0)
   * @param systemCpuTime CPU time spent in system mode (0.0 to 1.0)
   * @param totalCpuTime total CPU time used (0.0 to 1.0)
   * @param cpuUtilization overall CPU utilization (0.0 to 1.0)
   * @param contextSwitches number of context switches
   * @param systemCalls number of system calls
   */
  public CpuUsage(
      final double userCpuTime,
      final double systemCpuTime,
      final double totalCpuTime,
      final double cpuUtilization,
      final long contextSwitches,
      final long systemCalls) {
    this.userCpuTime = Math.max(0.0, Math.min(1.0, userCpuTime));
    this.systemCpuTime = Math.max(0.0, Math.min(1.0, systemCpuTime));
    this.totalCpuTime = Math.max(0.0, Math.min(1.0, totalCpuTime));
    this.cpuUtilization = Math.max(0.0, Math.min(1.0, cpuUtilization));
    this.contextSwitches = Math.max(0, contextSwitches);
    this.systemCalls = Math.max(0, systemCalls);
  }

  /**
   * Gets the CPU time spent in user mode.
   *
   * @return user CPU time (0.0 to 1.0)
   */
  public double getUserCpuTime() {
    return userCpuTime;
  }

  /**
   * Gets the CPU time spent in system mode.
   *
   * @return system CPU time (0.0 to 1.0)
   */
  public double getSystemCpuTime() {
    return systemCpuTime;
  }

  /**
   * Gets the total CPU time used.
   *
   * @return total CPU time (0.0 to 1.0)
   */
  public double getTotalCpuTime() {
    return totalCpuTime;
  }

  /**
   * Gets the overall CPU utilization.
   *
   * @return CPU utilization (0.0 to 1.0)
   */
  public double getCpuUtilization() {
    return cpuUtilization;
  }

  /**
   * Gets the number of context switches.
   *
   * @return context switch count
   */
  public long getContextSwitches() {
    return contextSwitches;
  }

  /**
   * Gets the number of system calls.
   *
   * @return system call count
   */
  public long getSystemCalls() {
    return systemCalls;
  }

  /**
   * Checks if CPU usage is high.
   *
   * <p>Returns true if CPU utilization exceeds 80%.
   *
   * @return true if CPU usage is high
   */
  public boolean isHighCpuUsage() {
    return cpuUtilization > 0.8;
  }

  /**
   * Gets the CPU efficiency (user time / total time).
   *
   * <p>Higher values indicate more time spent in user code vs system calls.
   *
   * @return CPU efficiency (0.0 to 1.0)
   */
  public double getEfficiency() {
    return totalCpuTime > 0 ? userCpuTime / totalCpuTime : 0.0;
  }

  /**
   * Checks if the process is system call heavy.
   *
   * <p>Returns true if system CPU time is more than 30% of total CPU time.
   *
   * @return true if system call heavy
   */
  public boolean isSystemCallHeavy() {
    return totalCpuTime > 0 && (systemCpuTime / totalCpuTime) > 0.3;
  }

  /**
   * Gets the context switch rate per unit of CPU time.
   *
   * @return context switches per CPU time unit
   */
  public double getContextSwitchRate() {
    return totalCpuTime > 0 ? contextSwitches / totalCpuTime : 0.0;
  }

  /**
   * Gets the system call rate per unit of CPU time.
   *
   * @return system calls per CPU time unit
   */
  public double getSystemCallRate() {
    return totalCpuTime > 0 ? systemCalls / totalCpuTime : 0.0;
  }

  /**
   * Checks if there is excessive context switching.
   *
   * <p>Returns true if context switch rate is abnormally high.
   *
   * @return true if excessive context switching
   */
  public boolean hasExcessiveContextSwitching() {
    return getContextSwitchRate() > 10000; // More than 10K switches per CPU time unit
  }

  /**
   * Gets the CPU performance score (0.0 to 1.0).
   *
   * <p>Higher scores indicate better CPU performance (high efficiency, low overhead).
   *
   * @return CPU performance score
   */
  public double getPerformanceScore() {
    double score = 1.0;

    // Penalize high CPU utilization
    if (cpuUtilization > 0.9) {
      score -= 0.3;
    } else if (cpuUtilization > 0.8) {
      score -= 0.1;
    }

    // Penalize low efficiency (too much system time)
    final double efficiency = getEfficiency();
    if (efficiency < 0.5) {
      score -= 0.3;
    } else if (efficiency < 0.7) {
      score -= 0.1;
    }

    // Penalize excessive context switching
    if (hasExcessiveContextSwitching()) {
      score -= 0.2;
    }

    return Math.max(0.0, score);
  }

  /**
   * Formats CPU percentage for display.
   *
   * @param ratio the CPU ratio (0.0 to 1.0)
   * @return formatted percentage string
   */
  public static String formatPercentage(final double ratio) {
    return String.format("%.1f%%", ratio * 100);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final CpuUsage cpuUsage = (CpuUsage) obj;
    return Double.compare(cpuUsage.userCpuTime, userCpuTime) == 0 &&
        Double.compare(cpuUsage.systemCpuTime, systemCpuTime) == 0 &&
        Double.compare(cpuUsage.totalCpuTime, totalCpuTime) == 0 &&
        Double.compare(cpuUsage.cpuUtilization, cpuUtilization) == 0 &&
        contextSwitches == cpuUsage.contextSwitches &&
        systemCalls == cpuUsage.systemCalls;
  }

  @Override
  public int hashCode() {
    return Objects.hash(userCpuTime, systemCpuTime, totalCpuTime, cpuUtilization, contextSwitches, systemCalls);
  }

  @Override
  public String toString() {
    return String.format(
        "CpuUsage{utilization=%s, user=%s, system=%s, " +
        "contextSwitches=%d, systemCalls=%d, efficiency=%.1f%%}",
        formatPercentage(cpuUtilization), formatPercentage(userCpuTime), formatPercentage(systemCpuTime),
        contextSwitches, systemCalls, getEfficiency() * 100);
  }
}