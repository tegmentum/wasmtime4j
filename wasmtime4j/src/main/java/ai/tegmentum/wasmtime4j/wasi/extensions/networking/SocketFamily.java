package ai.tegmentum.wasmtime4j.wasi.extensions.networking;

/**
 * Enumeration of socket address families for WASI networking operations.
 *
 * <p>Socket families define the address format and protocol family used for network communication.
 * Each family corresponds to different addressing schemes and network protocols.
 *
 * @since 1.0.0
 */
public enum SocketFamily {

  /**
   * IPv4 address family (AF_INET).
   *
   * <p>Uses 32-bit IPv4 addresses in dotted decimal notation (e.g., 192.168.1.1). This is the most
   * common address family for internet communications.
   */
  INET(2),

  /**
   * IPv6 address family (AF_INET6).
   *
   * <p>Uses 128-bit IPv6 addresses in hexadecimal notation (e.g., 2001:db8::1). Provides expanded
   * address space and improved protocol features compared to IPv4.
   */
  INET6(10),

  /**
   * Unix domain socket family (AF_UNIX).
   *
   * <p>Uses filesystem paths for local inter-process communication. Provides efficient
   * communication between processes on the same machine without network overhead.
   */
  UNIX(1);

  private final int value;

  SocketFamily(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this socket family.
   *
   * @return the numeric socket family value
   */
  public int getValue() {
    return value;
  }

  /**
   * Creates a SocketFamily from its numeric value.
   *
   * @param value the numeric socket family value
   * @return the corresponding SocketFamily
   * @throws IllegalArgumentException if the value is not recognized
   */
  public static SocketFamily fromValue(final int value) {
    for (final SocketFamily family : values()) {
      if (family.value == value) {
        return family;
      }
    }
    throw new IllegalArgumentException("Unknown socket family value: " + value);
  }
}
