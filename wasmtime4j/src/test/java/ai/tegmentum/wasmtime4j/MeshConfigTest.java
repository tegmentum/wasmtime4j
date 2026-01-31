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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link MeshConfig} mesh networking configuration. */
@DisplayName("MeshConfig")
final class MeshConfigTest {

  @Nested
  @DisplayName("default configuration")
  final class DefaultConfigTests {

    @Test
    @DisplayName("should create default config with non-null sub-configs")
    void shouldCreateDefaultConfigWithNonNullSubConfigs() {
      final MeshConfig config = MeshConfig.defaultConfig();
      assertNotNull(config.getServiceDiscovery(), "Service discovery should not be null");
      assertNotNull(config.getLoadBalancing(), "Load balancing should not be null");
      assertNotNull(config.getSecurity(), "Security should not be null");
      assertNotNull(config.getStreaming(), "Streaming should not be null");
      assertNotNull(config.getCdn(), "CDN should not be null");
      assertNotNull(config.getAnalytics(), "Analytics should not be null");
      assertNotNull(config.getFederation(), "Federation should not be null");
      assertNotNull(config.getMonitoring(), "Monitoring should not be null");
    }
  }

  @Nested
  @DisplayName("ServiceDiscoveryConfig defaults")
  final class ServiceDiscoveryDefaultTests {

    @Test
    @DisplayName("should have default discovery protocols")
    void shouldHaveDefaultDiscoveryProtocols() {
      final MeshConfig.ServiceDiscoveryConfig sd =
          MeshConfig.defaultConfig().getServiceDiscovery();
      assertFalse(
          sd.getDiscoveryProtocols().isEmpty(),
          "Default discovery protocols should not be empty");
      assertTrue(
          sd.getDiscoveryProtocols().contains("dns"),
          "Default should include dns protocol");
    }

    @Test
    @DisplayName("should have default health check interval")
    void shouldHaveDefaultHealthCheckInterval() {
      final MeshConfig.ServiceDiscoveryConfig sd =
          MeshConfig.defaultConfig().getServiceDiscovery();
      assertEquals(
          Duration.ofSeconds(30),
          sd.getHealthCheckInterval(),
          "Default health check interval should be 30 seconds");
    }

    @Test
    @DisplayName("should have auto registration enabled by default")
    void shouldHaveAutoRegistrationEnabled() {
      final MeshConfig.ServiceDiscoveryConfig sd =
          MeshConfig.defaultConfig().getServiceDiscovery();
      assertTrue(sd.isAutoRegistration(), "Auto registration should be enabled by default");
    }

    @Test
    @DisplayName("should have default service timeout")
    void shouldHaveDefaultServiceTimeout() {
      final MeshConfig.ServiceDiscoveryConfig sd =
          MeshConfig.defaultConfig().getServiceDiscovery();
      assertEquals(
          Duration.ofMinutes(5),
          sd.getServiceTimeout(),
          "Default service timeout should be 5 minutes");
    }

    @Test
    @DisplayName("should have empty metadata by default")
    void shouldHaveEmptyMetadata() {
      final MeshConfig.ServiceDiscoveryConfig sd =
          MeshConfig.defaultConfig().getServiceDiscovery();
      assertTrue(sd.getMetadata().isEmpty(), "Default metadata should be empty");
    }
  }

  @Nested
  @DisplayName("LoadBalancingConfig defaults")
  final class LoadBalancingDefaultTests {

    @Test
    @DisplayName("should have round_robin as default strategy")
    void shouldHaveRoundRobinDefault() {
      final MeshConfig.LoadBalancingConfig lb = MeshConfig.defaultConfig().getLoadBalancing();
      assertEquals(
          "round_robin", lb.getDefaultStrategy(), "Default strategy should be round_robin");
    }

    @Test
    @DisplayName("should have multiple available strategies")
    void shouldHaveMultipleStrategies() {
      final MeshConfig.LoadBalancingConfig lb = MeshConfig.defaultConfig().getLoadBalancing();
      assertTrue(
          lb.getAvailableStrategies().size() > 1,
          "Should have multiple available strategies");
    }

    @Test
    @DisplayName("should have health check enabled by default")
    void shouldHaveHealthCheckEnabled() {
      final MeshConfig.LoadBalancingConfig lb = MeshConfig.defaultConfig().getLoadBalancing();
      assertTrue(lb.isHealthCheckEnabled(), "Health check should be enabled by default");
    }

    @Test
    @DisplayName("should have circuit breaker failure threshold")
    void shouldHaveCircuitBreakerThreshold() {
      final MeshConfig.LoadBalancingConfig lb = MeshConfig.defaultConfig().getLoadBalancing();
      assertEquals(
          5,
          lb.getCircuitBreakerFailureThreshold(),
          "Circuit breaker failure threshold should be 5");
    }
  }

  @Nested
  @DisplayName("SecurityConfig defaults")
  final class SecurityDefaultTests {

    @Test
    @DisplayName("should have encryption enabled by default")
    void shouldHaveEncryptionEnabled() {
      final MeshConfig.SecurityConfig sec = MeshConfig.defaultConfig().getSecurity();
      assertTrue(sec.isEncryptionEnabled(), "Encryption should be enabled by default");
    }

    @Test
    @DisplayName("should use AES_256_GCM encryption algorithm")
    void shouldUseAes256Gcm() {
      final MeshConfig.SecurityConfig sec = MeshConfig.defaultConfig().getSecurity();
      assertEquals(
          "AES_256_GCM",
          sec.getEncryptionAlgorithm(),
          "Encryption algorithm should be AES_256_GCM");
    }

    @Test
    @DisplayName("should have access control, audit logging, threat detection enabled")
    void shouldHaveSecurityFeaturesEnabled() {
      final MeshConfig.SecurityConfig sec = MeshConfig.defaultConfig().getSecurity();
      assertTrue(sec.isAccessControlEnabled(), "Access control should be enabled");
      assertTrue(sec.isAuditLoggingEnabled(), "Audit logging should be enabled");
      assertTrue(sec.isThreatDetectionEnabled(), "Threat detection should be enabled");
    }
  }

  @Nested
  @DisplayName("CdnConfig defaults")
  final class CdnDefaultTests {

    @Test
    @DisplayName("should have CDN disabled by default")
    void shouldHaveCdnDisabled() {
      final MeshConfig.CdnConfig cdn = MeshConfig.defaultConfig().getCdn();
      assertFalse(cdn.isEnabled(), "CDN should be disabled by default");
    }

    @Test
    @DisplayName("should have nearest_edge routing strategy")
    void shouldHaveNearestEdgeRouting() {
      final MeshConfig.CdnConfig cdn = MeshConfig.defaultConfig().getCdn();
      assertEquals(
          "nearest_edge", cdn.getRoutingStrategy(), "Default routing should be nearest_edge");
    }

    @Test
    @DisplayName("should have 1 hour cache TTL")
    void shouldHaveOneHourCacheTtl() {
      final MeshConfig.CdnConfig cdn = MeshConfig.defaultConfig().getCdn();
      assertEquals(
          Duration.ofHours(1), cdn.getCacheTtl(), "Default cache TTL should be 1 hour");
    }
  }

  @Nested
  @DisplayName("FederationConfig defaults")
  final class FederationDefaultTests {

    @Test
    @DisplayName("should have federation disabled by default")
    void shouldHaveFederationDisabled() {
      final MeshConfig.FederationConfig fed = MeshConfig.defaultConfig().getFederation();
      assertFalse(fed.isEnabled(), "Federation should be disabled by default");
    }

    @Test
    @DisplayName("should have eventual consistency by default")
    void shouldHaveEventualConsistency() {
      final MeshConfig.FederationConfig fed = MeshConfig.defaultConfig().getFederation();
      assertEquals(
          "eventual",
          fed.getConsistencyLevel(),
          "Default consistency level should be eventual");
    }
  }

  @Nested
  @DisplayName("MonitoringConfig defaults")
  final class MonitoringDefaultTests {

    @Test
    @DisplayName("should have monitoring enabled by default")
    void shouldHaveMonitoringEnabled() {
      final MeshConfig.MonitoringConfig mon = MeshConfig.defaultConfig().getMonitoring();
      assertTrue(mon.isEnabled(), "Monitoring should be enabled by default");
    }

    @Test
    @DisplayName("should have prometheus metrics format")
    void shouldHavePrometheusFormat() {
      final MeshConfig.MonitoringConfig mon = MeshConfig.defaultConfig().getMonitoring();
      assertEquals(
          "prometheus", mon.getMetricsFormat(), "Default metrics format should be prometheus");
    }
  }

  @Nested
  @DisplayName("equals and hashCode")
  final class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal for two default configs")
    void shouldBeEqualForTwoDefaults() {
      final MeshConfig config1 = MeshConfig.defaultConfig();
      final MeshConfig config2 = MeshConfig.defaultConfig();
      assertEquals(config1, config2, "Two default configs should be equal");
      assertEquals(
          config1.hashCode(), config2.hashCode(), "Hash codes of equal configs should match");
    }

    @Test
    @DisplayName("should be reflexively equal")
    void shouldBeReflexivelyEqual() {
      final MeshConfig config = MeshConfig.defaultConfig();
      assertEquals(config, config, "Config should be equal to itself");
    }
  }

  @Nested
  @DisplayName("toString")
  final class ToStringTests {

    @Test
    @DisplayName("should produce non-null string")
    void shouldProduceNonNullString() {
      final MeshConfig config = MeshConfig.defaultConfig();
      assertNotNull(config.toString(), "toString should not return null");
      assertTrue(
          config.toString().contains("MeshConfig"),
          "toString should contain class name");
    }
  }
}
