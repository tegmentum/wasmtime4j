package ai.tegmentum.wasmtime4j;

import java.util.Objects;

/**
 * Represents a service endpoint with network address information.
 */
public final class ServiceEndpoint {
  private final String serviceId;
  private final String address;
  private final int port;

  /**
   * Creates a new service endpoint.
   *
   * @param serviceId the unique identifier for the service
   * @param address the network address
   * @param port the network port
   */
  public ServiceEndpoint(final String serviceId, final String address, final int port) {
    this.serviceId = Objects.requireNonNull(serviceId);
    this.address = Objects.requireNonNull(address);
    this.port = port;
  }

  /**
   * Gets the service identifier.
   *
   * @return the service identifier
   */
  public String getServiceId() {
    return serviceId;
  }

  /**
   * Gets the network address.
   *
   * @return the network address
   */
  public String getAddress() {
    return address;
  }

  /**
   * Gets the network port.
   *
   * @return the network port
   */
  public int getPort() {
    return port;
  }
}
