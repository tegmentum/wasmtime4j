/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link ServiceEndpoint} service endpoint data class. */
@DisplayName("ServiceEndpoint")
final class ServiceEndpointTest {

  @Nested
  @DisplayName("constructor and getters")
  final class ConstructorAndGetterTests {

    @Test
    @DisplayName("should store service ID correctly")
    void shouldStoreServiceId() {
      final ServiceEndpoint endpoint = new ServiceEndpoint("svc-001", "localhost", 8080);
      assertEquals("svc-001", endpoint.getServiceId(), "Service ID should be svc-001");
    }

    @Test
    @DisplayName("should store address correctly")
    void shouldStoreAddress() {
      final ServiceEndpoint endpoint = new ServiceEndpoint("svc-002", "192.168.1.100", 9090);
      assertEquals("192.168.1.100", endpoint.getAddress(), "Address should be 192.168.1.100");
    }

    @Test
    @DisplayName("should store port correctly")
    void shouldStorePort() {
      final ServiceEndpoint endpoint = new ServiceEndpoint("svc-003", "10.0.0.1", 443);
      assertEquals(443, endpoint.getPort(), "Port should be 443");
    }

    @Test
    @DisplayName("should accept port zero")
    void shouldAcceptPortZero() {
      final ServiceEndpoint endpoint = new ServiceEndpoint("svc-004", "host", 0);
      assertEquals(0, endpoint.getPort(), "Port should be 0");
    }
  }

  @Nested
  @DisplayName("null validation")
  final class NullValidationTests {

    @Test
    @DisplayName("should reject null service ID")
    void shouldRejectNullServiceId() {
      assertThrows(
          NullPointerException.class,
          () -> new ServiceEndpoint(null, "localhost", 80),
          "Expected NullPointerException for null service ID");
    }

    @Test
    @DisplayName("should reject null address")
    void shouldRejectNullAddress() {
      assertThrows(
          NullPointerException.class,
          () -> new ServiceEndpoint("svc-005", null, 80),
          "Expected NullPointerException for null address");
    }
  }

  @Nested
  @DisplayName("instance creation")
  final class InstanceCreationTests {

    @Test
    @DisplayName("should create non-null instance with valid params")
    void shouldCreateNonNullInstance() {
      final ServiceEndpoint endpoint = new ServiceEndpoint("test-svc", "api.example.com", 8443);
      assertNotNull(endpoint, "ServiceEndpoint instance should not be null");
    }

    @Test
    @DisplayName("should preserve all constructor arguments")
    void shouldPreserveAllArguments() {
      final String serviceId = "my-service";
      final String address = "10.20.30.40";
      final int port = 12345;
      final ServiceEndpoint endpoint = new ServiceEndpoint(serviceId, address, port);
      assertEquals(serviceId, endpoint.getServiceId(), "ServiceId should be preserved");
      assertEquals(address, endpoint.getAddress(), "Address should be preserved");
      assertEquals(port, endpoint.getPort(), "Port should be preserved");
    }
  }
}
