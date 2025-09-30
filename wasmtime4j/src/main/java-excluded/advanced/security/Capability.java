package ai.tegmentum.wasmtime4j.security;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Represents a specific security capability or permission.
 *
 * <p>Capabilities define what operations a sandboxed module is allowed to perform. This follows the
 * principle of least privilege, where modules are granted only the minimum capabilities needed for
 * their intended function.
 *
 * @since 1.0.0
 */
public abstract class Capability {

  /**
   * Gets the capability type identifier.
   *
   * @return the capability type
   */
  public abstract String getType();

  /**
   * Gets the capability configuration parameters.
   *
   * @return the capability parameters
   */
  public abstract Map<String, Object> getParameters();

  /**
   * Checks if this capability is compatible with another capability.
   *
   * <p>Two capabilities are compatible if they can be granted together without security conflicts.
   *
   * @param other the other capability
   * @return true if compatible, false otherwise
   */
  public abstract boolean isCompatibleWith(final Capability other);

  /**
   * Creates a memory access capability.
   *
   * @param maxSize maximum memory size in bytes
   * @param writeAccess whether write access is allowed
   * @return a memory access capability
   */
  public static Capability memoryAccess(final long maxSize, final boolean writeAccess) {
    return new MemoryAccessCapability(maxSize, writeAccess);
  }

  /**
   * Creates a file system access capability with default permissions.
   *
   * @return a file system access capability
   */
  public static Capability fileSystemAccess() {
    return new FileSystemAccessCapability();
  }

  /**
   * Creates a file system access capability for specific paths.
   *
   * @param paths the allowed paths with their permissions
   * @return a file system access capability
   */
  public static Capability fileSystemAccess(final Map<Path, FilePermissions> paths) {
    return new FileSystemAccessCapability(paths);
  }

  /**
   * Creates a network access capability with default permissions.
   *
   * @return a network access capability
   */
  public static Capability networkAccess() {
    return new NetworkAccessCapability();
  }

  /**
   * Creates a network access capability with specific restrictions.
   *
   * @param protocols allowed protocols (tcp, udp, http, https)
   * @param hosts allowed destination hosts
   * @param portRanges allowed port ranges (start, end)
   * @return a network access capability
   */
  public static Capability networkAccess(
      final Set<String> protocols, final Set<String> hosts, final Set<PortRange> portRanges) {
    return new NetworkAccessCapability(protocols, hosts, portRanges);
  }

  /**
   * Creates a system call access capability.
   *
   * @param allowedCalls the allowed system calls
   * @return a system call access capability
   */
  public static Capability systemCallAccess(final Set<String> allowedCalls) {
    return new SystemCallAccessCapability(allowedCalls);
  }

  /**
   * Creates an inter-module communication capability.
   *
   * @param targetModules allowed target modules
   * @param protocols allowed communication protocols
   * @return an inter-module communication capability
   */
  public static Capability interModuleCommunication(
      final Set<String> targetModules, final Set<String> protocols) {
    return new InterModuleCommunicationCapability(targetModules, protocols);
  }

  /**
   * Creates a resource limits capability.
   *
   * @param cpuTimeMs CPU time limit in milliseconds
   * @param maxDuration maximum execution duration
   * @param maxInstructions maximum number of instructions
   * @return a resource limits capability
   */
  public static Capability resourceLimits(
      final Long cpuTimeMs, final Duration maxDuration, final Long maxInstructions) {
    return new ResourceLimitsCapability(cpuTimeMs, maxDuration, maxInstructions);
  }

  /**
   * Creates an environment variable access capability.
   *
   * @param allowedVars allowed environment variables
   * @param canSet whether new variables can be set
   * @return an environment access capability
   */
  public static Capability environmentAccess(final Set<String> allowedVars, final boolean canSet) {
    return new EnvironmentAccessCapability(allowedVars, canSet);
  }

  @Override
  public abstract boolean equals(final Object obj);

  @Override
  public abstract int hashCode();

  @Override
  public abstract String toString();

  // Inner classes for specific capability types

  private static final class MemoryAccessCapability extends Capability {
    private final long maxSize;
    private final boolean writeAccess;

    MemoryAccessCapability(final long maxSize, final boolean writeAccess) {
      this.maxSize = maxSize;
      this.writeAccess = writeAccess;
    }

    @Override
    public String getType() {
      return "memory_access";
    }

    @Override
    public Map<String, Object> getParameters() {
      return Map.of("max_size", maxSize, "write_access", writeAccess);
    }

    @Override
    public boolean isCompatibleWith(final Capability other) {
      if (other instanceof MemoryAccessCapability) {
        final MemoryAccessCapability mem = (MemoryAccessCapability) other;
        // Compatible if this is more restrictive
        return this.maxSize <= mem.maxSize && (!this.writeAccess || mem.writeAccess);
      }
      return true; // Compatible with non-memory capabilities
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof MemoryAccessCapability)) return false;
      final MemoryAccessCapability that = (MemoryAccessCapability) obj;
      return maxSize == that.maxSize && writeAccess == that.writeAccess;
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(maxSize, writeAccess);
    }

    @Override
    public String toString() {
      return String.format("MemoryAccess{maxSize=%d, writeAccess=%s}", maxSize, writeAccess);
    }
  }

  private static final class FileSystemAccessCapability extends Capability {
    private final Map<Path, FilePermissions> paths;

    FileSystemAccessCapability() {
      this.paths = Map.of();
    }

    FileSystemAccessCapability(final Map<Path, FilePermissions> paths) {
      this.paths = Map.copyOf(paths);
    }

    @Override
    public String getType() {
      return "filesystem_access";
    }

    @Override
    public Map<String, Object> getParameters() {
      return Map.of("paths", paths);
    }

    @Override
    public boolean isCompatibleWith(final Capability other) {
      // File system capabilities are generally compatible
      return true;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof FileSystemAccessCapability)) return false;
      final FileSystemAccessCapability that = (FileSystemAccessCapability) obj;
      return paths.equals(that.paths);
    }

    @Override
    public int hashCode() {
      return paths.hashCode();
    }

    @Override
    public String toString() {
      return String.format("FileSystemAccess{paths=%d}", paths.size());
    }
  }

  private static final class NetworkAccessCapability extends Capability {
    private final Set<String> protocols;
    private final Set<String> hosts;
    private final Set<PortRange> portRanges;

    NetworkAccessCapability() {
      this.protocols = Set.of();
      this.hosts = Set.of();
      this.portRanges = Set.of();
    }

    NetworkAccessCapability(
        final Set<String> protocols, final Set<String> hosts, final Set<PortRange> portRanges) {
      this.protocols = Set.copyOf(protocols);
      this.hosts = Set.copyOf(hosts);
      this.portRanges = Set.copyOf(portRanges);
    }

    @Override
    public String getType() {
      return "network_access";
    }

    @Override
    public Map<String, Object> getParameters() {
      return Map.of("protocols", protocols, "hosts", hosts, "port_ranges", portRanges);
    }

    @Override
    public boolean isCompatibleWith(final Capability other) {
      // Network capabilities are generally compatible
      return true;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof NetworkAccessCapability)) return false;
      final NetworkAccessCapability that = (NetworkAccessCapability) obj;
      return protocols.equals(that.protocols)
          && hosts.equals(that.hosts)
          && portRanges.equals(that.portRanges);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(protocols, hosts, portRanges);
    }

    @Override
    public String toString() {
      return String.format(
          "NetworkAccess{protocols=%s, hosts=%d, portRanges=%d}",
          protocols, hosts.size(), portRanges.size());
    }
  }

  private static final class SystemCallAccessCapability extends Capability {
    private final Set<String> allowedCalls;

    SystemCallAccessCapability(final Set<String> allowedCalls) {
      this.allowedCalls = Set.copyOf(allowedCalls);
    }

    @Override
    public String getType() {
      return "syscall_access";
    }

    @Override
    public Map<String, Object> getParameters() {
      return Map.of("allowed_calls", allowedCalls);
    }

    @Override
    public boolean isCompatibleWith(final Capability other) {
      return true;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof SystemCallAccessCapability)) return false;
      final SystemCallAccessCapability that = (SystemCallAccessCapability) obj;
      return allowedCalls.equals(that.allowedCalls);
    }

    @Override
    public int hashCode() {
      return allowedCalls.hashCode();
    }

    @Override
    public String toString() {
      return String.format("SystemCallAccess{calls=%d}", allowedCalls.size());
    }
  }

  private static final class InterModuleCommunicationCapability extends Capability {
    private final Set<String> targetModules;
    private final Set<String> protocols;

    InterModuleCommunicationCapability(
        final Set<String> targetModules, final Set<String> protocols) {
      this.targetModules = Set.copyOf(targetModules);
      this.protocols = Set.copyOf(protocols);
    }

    @Override
    public String getType() {
      return "inter_module_communication";
    }

    @Override
    public Map<String, Object> getParameters() {
      return Map.of("target_modules", targetModules, "protocols", protocols);
    }

    @Override
    public boolean isCompatibleWith(final Capability other) {
      return true;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof InterModuleCommunicationCapability)) return false;
      final InterModuleCommunicationCapability that = (InterModuleCommunicationCapability) obj;
      return targetModules.equals(that.targetModules) && protocols.equals(that.protocols);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(targetModules, protocols);
    }

    @Override
    public String toString() {
      return String.format(
          "InterModuleCommunication{targets=%d, protocols=%s}", targetModules.size(), protocols);
    }
  }

  private static final class ResourceLimitsCapability extends Capability {
    private final Long cpuTimeMs;
    private final Duration maxDuration;
    private final Long maxInstructions;

    ResourceLimitsCapability(
        final Long cpuTimeMs, final Duration maxDuration, final Long maxInstructions) {
      this.cpuTimeMs = cpuTimeMs;
      this.maxDuration = maxDuration;
      this.maxInstructions = maxInstructions;
    }

    @Override
    public String getType() {
      return "resource_limits";
    }

    @Override
    public Map<String, Object> getParameters() {
      return Map.of(
          "cpu_time_ms", cpuTimeMs,
          "max_duration", maxDuration,
          "max_instructions", maxInstructions);
    }

    @Override
    public boolean isCompatibleWith(final Capability other) {
      return true;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof ResourceLimitsCapability)) return false;
      final ResourceLimitsCapability that = (ResourceLimitsCapability) obj;
      return java.util.Objects.equals(cpuTimeMs, that.cpuTimeMs)
          && java.util.Objects.equals(maxDuration, that.maxDuration)
          && java.util.Objects.equals(maxInstructions, that.maxInstructions);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(cpuTimeMs, maxDuration, maxInstructions);
    }

    @Override
    public String toString() {
      return String.format(
          "ResourceLimits{cpuMs=%s, duration=%s, instructions=%s}",
          cpuTimeMs, maxDuration, maxInstructions);
    }
  }

  private static final class EnvironmentAccessCapability extends Capability {
    private final Set<String> allowedVars;
    private final boolean canSet;

    EnvironmentAccessCapability(final Set<String> allowedVars, final boolean canSet) {
      this.allowedVars = Set.copyOf(allowedVars);
      this.canSet = canSet;
    }

    @Override
    public String getType() {
      return "environment_access";
    }

    @Override
    public Map<String, Object> getParameters() {
      return Map.of("allowed_vars", allowedVars, "can_set", canSet);
    }

    @Override
    public boolean isCompatibleWith(final Capability other) {
      return true;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof EnvironmentAccessCapability)) return false;
      final EnvironmentAccessCapability that = (EnvironmentAccessCapability) obj;
      return allowedVars.equals(that.allowedVars) && canSet == that.canSet;
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(allowedVars, canSet);
    }

    @Override
    public String toString() {
      return String.format("EnvironmentAccess{vars=%d, canSet=%s}", allowedVars.size(), canSet);
    }
  }

  /** Represents a port range for network access. */
  public static final class PortRange {
    private final int start;
    private final int end;

    /**
     * Creates a new port range.
     *
     * @param start the start port (inclusive)
     * @param end the end port (inclusive)
     */
    public PortRange(final int start, final int end) {
      if (start < 1 || start > 65535 || end < 1 || end > 65535 || start > end) {
        throw new IllegalArgumentException("Invalid port range: " + start + "-" + end);
      }
      this.start = start;
      this.end = end;
    }

    /**
     * Creates a single port range.
     *
     * @param port the port number
     * @return a port range containing only the specified port
     */
    public static PortRange single(final int port) {
      return new PortRange(port, port);
    }

    /**
     * Gets the start port.
     *
     * @return the start port
     */
    public int getStart() {
      return start;
    }

    /**
     * Gets the end port.
     *
     * @return the end port
     */
    public int getEnd() {
      return end;
    }

    /**
     * Checks if a port is within this range.
     *
     * @param port the port to check
     * @return true if the port is in range, false otherwise
     */
    public boolean contains(final int port) {
      return port >= start && port <= end;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof PortRange)) return false;
      final PortRange portRange = (PortRange) obj;
      return start == portRange.start && end == portRange.end;
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(start, end);
    }

    @Override
    public String toString() {
      return start == end ? String.valueOf(start) : start + "-" + end;
    }
  }
}
