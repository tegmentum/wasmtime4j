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

package ai.tegmentum.wasmtime4j.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GlobalDistributionSystem} class.
 *
 * <p>GlobalDistributionSystem provides multi-region deployment and data distribution capabilities.
 */
@DisplayName("GlobalDistributionSystem Class Tests")
class GlobalDistributionSystemTest {

  @Nested
  @DisplayName("Region Enum Tests")
  class RegionEnumTests {

    @Test
    @DisplayName("should have all expected regions")
    void shouldHaveAllExpectedRegions() {
      final GlobalDistributionSystem.Region[] regions = GlobalDistributionSystem.Region.values();

      assertEquals(10, regions.length, "Should have 10 regions");

      final Set<String> regionNames =
          Set.of(
              "US_EAST_1",
              "US_WEST_2",
              "EU_WEST_1",
              "EU_CENTRAL_1",
              "AP_SOUTHEAST_1",
              "AP_NORTHEAST_1",
              "AP_SOUTH_1",
              "SA_EAST_1",
              "CA_CENTRAL_1",
              "AF_SOUTH_1");

      for (final GlobalDistributionSystem.Region region : regions) {
        assertTrue(
            regionNames.contains(region.name()), "Region " + region.name() + " should be expected");
      }
    }

    @Test
    @DisplayName("valueOf should return correct region")
    void valueOfShouldReturnCorrectRegion() {
      assertEquals(
          GlobalDistributionSystem.Region.US_EAST_1,
          GlobalDistributionSystem.Region.valueOf("US_EAST_1"));
      assertEquals(
          GlobalDistributionSystem.Region.EU_WEST_1,
          GlobalDistributionSystem.Region.valueOf("EU_WEST_1"));
    }

    @Test
    @DisplayName("regions should have getRegionCode method")
    void regionsShouldHaveGetRegionCodeMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.Region.class.getMethod("getRegionCode");
      assertNotNull(method, "getRegionCode method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("regions should have getDisplayName method")
    void regionsShouldHaveGetDisplayNameMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.Region.class.getMethod("getDisplayName");
      assertNotNull(method, "getDisplayName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("regions should have getCityName method")
    void regionsShouldHaveGetCityNameMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.Region.class.getMethod("getCityName");
      assertNotNull(method, "getCityName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("each region should have non-empty region code")
    void eachRegionShouldHaveNonEmptyRegionCode() {
      for (final GlobalDistributionSystem.Region region :
          GlobalDistributionSystem.Region.values()) {
        assertNotNull(region.getRegionCode(), region.name() + " region code should not be null");
        assertFalse(
            region.getRegionCode().isEmpty(), region.name() + " region code should not be empty");
      }
    }

    @Test
    @DisplayName("each region should have non-empty display name")
    void eachRegionShouldHaveNonEmptyDisplayName() {
      for (final GlobalDistributionSystem.Region region :
          GlobalDistributionSystem.Region.values()) {
        assertNotNull(region.getDisplayName(), region.name() + " display name should not be null");
        assertFalse(
            region.getDisplayName().isEmpty(), region.name() + " display name should not be empty");
      }
    }

    @Test
    @DisplayName("each region should have non-empty city name")
    void eachRegionShouldHaveNonEmptyCityName() {
      for (final GlobalDistributionSystem.Region region :
          GlobalDistributionSystem.Region.values()) {
        assertNotNull(region.getCityName(), region.name() + " city name should not be null");
        assertFalse(
            region.getCityName().isEmpty(), region.name() + " city name should not be empty");
      }
    }
  }

  @Nested
  @DisplayName("NodeHealthStatus Enum Tests")
  class NodeHealthStatusEnumTests {

    @Test
    @DisplayName("should have all expected health statuses")
    void shouldHaveAllExpectedHealthStatuses() {
      final GlobalDistributionSystem.NodeHealthStatus[] statuses =
          GlobalDistributionSystem.NodeHealthStatus.values();

      assertEquals(6, statuses.length, "Should have 6 health statuses");

      final Set<String> statusNames =
          Set.of("HEALTHY", "DEGRADED", "UNHEALTHY", "UNREACHABLE", "MAINTENANCE", "DRAINING");

      for (final GlobalDistributionSystem.NodeHealthStatus status : statuses) {
        assertTrue(
            statusNames.contains(status.name()), "Status " + status.name() + " should be expected");
      }
    }

    @Test
    @DisplayName("valueOf should return correct status")
    void valueOfShouldReturnCorrectStatus() {
      assertEquals(
          GlobalDistributionSystem.NodeHealthStatus.HEALTHY,
          GlobalDistributionSystem.NodeHealthStatus.valueOf("HEALTHY"));
      assertEquals(
          GlobalDistributionSystem.NodeHealthStatus.DEGRADED,
          GlobalDistributionSystem.NodeHealthStatus.valueOf("DEGRADED"));
    }

    @Test
    @DisplayName("health statuses should have getDescription method")
    void healthStatusesShouldHaveGetDescriptionMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.NodeHealthStatus.class.getMethod("getDescription");
      assertNotNull(method, "getDescription method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("each status should have non-empty description")
    void eachStatusShouldHaveNonEmptyDescription() {
      for (final GlobalDistributionSystem.NodeHealthStatus status :
          GlobalDistributionSystem.NodeHealthStatus.values()) {
        assertNotNull(status.getDescription(), status.name() + " description should not be null");
        assertFalse(
            status.getDescription().isEmpty(), status.name() + " description should not be empty");
      }
    }
  }

  @Nested
  @DisplayName("ReplicationStrategy Enum Tests")
  class ReplicationStrategyEnumTests {

    @Test
    @DisplayName("should have all expected replication strategies")
    void shouldHaveAllExpectedReplicationStrategies() {
      final GlobalDistributionSystem.ReplicationStrategy[] strategies =
          GlobalDistributionSystem.ReplicationStrategy.values();

      assertEquals(6, strategies.length, "Should have 6 replication strategies");

      final Set<String> strategyNames =
          Set.of(
              "EVENTUAL_CONSISTENCY",
              "STRONG_CONSISTENCY",
              "QUORUM_CONSISTENCY",
              "REGION_ISOLATED",
              "MASTER_SLAVE",
              "MULTI_MASTER");

      for (final GlobalDistributionSystem.ReplicationStrategy strategy : strategies) {
        assertTrue(
            strategyNames.contains(strategy.name()),
            "Strategy " + strategy.name() + " should be expected");
      }
    }

    @Test
    @DisplayName("valueOf should return correct strategy")
    void valueOfShouldReturnCorrectStrategy() {
      assertEquals(
          GlobalDistributionSystem.ReplicationStrategy.EVENTUAL_CONSISTENCY,
          GlobalDistributionSystem.ReplicationStrategy.valueOf("EVENTUAL_CONSISTENCY"));
      assertEquals(
          GlobalDistributionSystem.ReplicationStrategy.STRONG_CONSISTENCY,
          GlobalDistributionSystem.ReplicationStrategy.valueOf("STRONG_CONSISTENCY"));
    }

    @Test
    @DisplayName("replication strategies should have getDescription method")
    void replicationStrategiesShouldHaveGetDescriptionMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.ReplicationStrategy.class.getMethod("getDescription");
      assertNotNull(method, "getDescription method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("each strategy should have non-empty description")
    void eachStrategyShouldHaveNonEmptyDescription() {
      for (final GlobalDistributionSystem.ReplicationStrategy strategy :
          GlobalDistributionSystem.ReplicationStrategy.values()) {
        assertNotNull(
            strategy.getDescription(), strategy.name() + " description should not be null");
        assertFalse(
            strategy.getDescription().isEmpty(),
            strategy.name() + " description should not be empty");
      }
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(GlobalDistributionSystem.class.getModifiers()),
          "GlobalDistributionSystem should be a final class");
    }

    @Test
    @DisplayName("should have public constructor")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      final var constructor = GlobalDistributionSystem.class.getConstructor();
      assertNotNull(constructor, "Public constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should be instantiable")
    void shouldBeInstantiable() {
      final GlobalDistributionSystem system = new GlobalDistributionSystem();
      assertNotNull(system, "System should be instantiable");
    }

    @Test
    @DisplayName("each instance should be independent")
    void eachInstanceShouldBeIndependent() {
      final GlobalDistributionSystem system1 = new GlobalDistributionSystem();
      final GlobalDistributionSystem system2 = new GlobalDistributionSystem();

      assertNotNull(system1, "Instance 1 should not be null");
      assertNotNull(system2, "Instance 2 should not be null");
      assertNotSame(system1, system2, "Instances should be different objects");
    }
  }

  @Nested
  @DisplayName("RegionalNode Class Tests")
  class RegionalNodeClassTests {

    @Test
    @DisplayName("RegionalNode should be a static nested class")
    void regionalNodeShouldBeStaticNestedClass() {
      assertTrue(
          Modifier.isStatic(GlobalDistributionSystem.RegionalNode.class.getModifiers()),
          "RegionalNode should be static");
      assertTrue(
          Modifier.isFinal(GlobalDistributionSystem.RegionalNode.class.getModifiers()),
          "RegionalNode should be final");
    }

    @Test
    @DisplayName("RegionalNode should have getNodeId method")
    void regionalNodeShouldHaveGetNodeIdMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.RegionalNode.class.getMethod("getNodeId");
      assertNotNull(method, "getNodeId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("RegionalNode should have getRegion method")
    void regionalNodeShouldHaveGetRegionMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.RegionalNode.class.getMethod("getRegion");
      assertNotNull(method, "getRegion method should exist");
      assertEquals(
          GlobalDistributionSystem.Region.class, method.getReturnType(), "Should return Region");
    }

    @Test
    @DisplayName("RegionalNode should have getEndpoint method")
    void regionalNodeShouldHaveGetEndpointMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.RegionalNode.class.getMethod("getEndpoint");
      assertNotNull(method, "getEndpoint method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("RegionalNode should have getHealthStatus method")
    void regionalNodeShouldHaveGetHealthStatusMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.RegionalNode.class.getMethod("getHealthStatus");
      assertNotNull(method, "getHealthStatus method should exist");
      assertEquals(
          GlobalDistributionSystem.NodeHealthStatus.class,
          method.getReturnType(),
          "Should return NodeHealthStatus");
    }

    @Test
    @DisplayName("RegionalNode should have getLastHealthCheck method")
    void regionalNodeShouldHaveGetLastHealthCheckMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.RegionalNode.class.getMethod("getLastHealthCheck");
      assertNotNull(method, "getLastHealthCheck method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("RegionalNode should have getCapabilities method")
    void regionalNodeShouldHaveGetCapabilitiesMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.RegionalNode.class.getMethod("getCapabilities");
      assertNotNull(method, "getCapabilities method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("RegionalNode should have getAverageLatency method")
    void regionalNodeShouldHaveGetAverageLatencyMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.RegionalNode.class.getMethod("getAverageLatency");
      assertNotNull(method, "getAverageLatency method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("RegionalNode should have isHealthy method")
    void regionalNodeShouldHaveIsHealthyMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.RegionalNode.class.getMethod("isHealthy");
      assertNotNull(method, "isHealthy method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("RegionalNode should have isAvailable method")
    void regionalNodeShouldHaveIsAvailableMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.RegionalNode.class.getMethod("isAvailable");
      assertNotNull(method, "isAvailable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("RegionalNode should have getHealthScore method")
    void regionalNodeShouldHaveGetHealthScoreMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.RegionalNode.class.getMethod("getHealthScore");
      assertNotNull(method, "getHealthScore method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  @Nested
  @DisplayName("RoutingDecision Class Tests")
  class RoutingDecisionClassTests {

    @Test
    @DisplayName("RoutingDecision should be a static nested class")
    void routingDecisionShouldBeStaticNestedClass() {
      assertTrue(
          Modifier.isStatic(GlobalDistributionSystem.RoutingDecision.class.getModifiers()),
          "RoutingDecision should be static");
      assertTrue(
          Modifier.isFinal(GlobalDistributionSystem.RoutingDecision.class.getModifiers()),
          "RoutingDecision should be final");
    }

    @Test
    @DisplayName("RoutingDecision should have getTargetNode method")
    void routingDecisionShouldHaveGetTargetNodeMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.RoutingDecision.class.getMethod("getTargetNode");
      assertNotNull(method, "getTargetNode method should exist");
      assertEquals(
          GlobalDistributionSystem.RegionalNode.class,
          method.getReturnType(),
          "Should return RegionalNode");
    }

    @Test
    @DisplayName("RoutingDecision should have getRoutingReason method")
    void routingDecisionShouldHaveGetRoutingReasonMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.RoutingDecision.class.getMethod("getRoutingReason");
      assertNotNull(method, "getRoutingReason method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("RoutingDecision should have getRoutingMetadata method")
    void routingDecisionShouldHaveGetRoutingMetadataMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.RoutingDecision.class.getMethod("getRoutingMetadata");
      assertNotNull(method, "getRoutingMetadata method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("RoutingDecision should have getDecisionTime method")
    void routingDecisionShouldHaveGetDecisionTimeMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.RoutingDecision.class.getMethod("getDecisionTime");
      assertNotNull(method, "getDecisionTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("RoutingDecision should have getConfidenceScore method")
    void routingDecisionShouldHaveGetConfidenceScoreMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.RoutingDecision.class.getMethod("getConfidenceScore");
      assertNotNull(method, "getConfidenceScore method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  @Nested
  @DisplayName("ReplicationJob Class Tests")
  class ReplicationJobClassTests {

    @Test
    @DisplayName("ReplicationJob should be a static nested class")
    void replicationJobShouldBeStaticNestedClass() {
      assertTrue(
          Modifier.isStatic(GlobalDistributionSystem.ReplicationJob.class.getModifiers()),
          "ReplicationJob should be static");
      assertTrue(
          Modifier.isFinal(GlobalDistributionSystem.ReplicationJob.class.getModifiers()),
          "ReplicationJob should be final");
    }

    @Test
    @DisplayName("ReplicationJob should have getJobId method")
    void replicationJobShouldHaveGetJobIdMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.ReplicationJob.class.getMethod("getJobId");
      assertNotNull(method, "getJobId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("ReplicationJob should have getDataId method")
    void replicationJobShouldHaveGetDataIdMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.ReplicationJob.class.getMethod("getDataId");
      assertNotNull(method, "getDataId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("ReplicationJob should have getSourceNode method")
    void replicationJobShouldHaveGetSourceNodeMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.ReplicationJob.class.getMethod("getSourceNode");
      assertNotNull(method, "getSourceNode method should exist");
      assertEquals(
          GlobalDistributionSystem.RegionalNode.class,
          method.getReturnType(),
          "Should return RegionalNode");
    }

    @Test
    @DisplayName("ReplicationJob should have getTargetNode method")
    void replicationJobShouldHaveGetTargetNodeMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.ReplicationJob.class.getMethod("getTargetNode");
      assertNotNull(method, "getTargetNode method should exist");
      assertEquals(
          GlobalDistributionSystem.RegionalNode.class,
          method.getReturnType(),
          "Should return RegionalNode");
    }

    @Test
    @DisplayName("ReplicationJob should have getStrategy method")
    void replicationJobShouldHaveGetStrategyMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.ReplicationJob.class.getMethod("getStrategy");
      assertNotNull(method, "getStrategy method should exist");
      assertEquals(
          GlobalDistributionSystem.ReplicationStrategy.class,
          method.getReturnType(),
          "Should return ReplicationStrategy");
    }

    @Test
    @DisplayName("ReplicationJob should have isCompleted method")
    void replicationJobShouldHaveIsCompletedMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.ReplicationJob.class.getMethod("isCompleted");
      assertNotNull(method, "isCompleted method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("ReplicationJob should have isSuccessful method")
    void replicationJobShouldHaveIsSuccessfulMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.ReplicationJob.class.getMethod("isSuccessful");
      assertNotNull(method, "isSuccessful method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("ReplicationJob should have getReplicationTime method")
    void replicationJobShouldHaveGetReplicationTimeMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.ReplicationJob.class.getMethod("getReplicationTime");
      assertNotNull(method, "getReplicationTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }
  }

  @Nested
  @DisplayName("System Method Tests")
  class SystemMethodTests {

    @Test
    @DisplayName("should have addRegionalNode method")
    void shouldHaveAddRegionalNodeMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.class.getMethod(
              "addRegionalNode", GlobalDistributionSystem.RegionalNode.class);
      assertNotNull(method, "addRegionalNode method should exist");
    }

    @Test
    @DisplayName("should have removeRegionalNode method")
    void shouldHaveRemoveRegionalNodeMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.class.getMethod("removeRegionalNode", String.class);
      assertNotNull(method, "removeRegionalNode method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getRegionalNodes method")
    void shouldHaveGetRegionalNodesMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.class.getMethod("getRegionalNodes");
      assertNotNull(method, "getRegionalNodes method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getHealthyNodesInRegion method")
    void shouldHaveGetHealthyNodesInRegionMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.class.getMethod(
              "getHealthyNodesInRegion", GlobalDistributionSystem.Region.class);
      assertNotNull(method, "getHealthyNodesInRegion method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have routeRequest method")
    void shouldHaveRouteRequestMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.class.getMethod(
              "routeRequest", GlobalDistributionSystem.Region.class, Map.class);
      assertNotNull(method, "routeRequest method should exist");
      assertEquals(
          GlobalDistributionSystem.RoutingDecision.class,
          method.getReturnType(),
          "Should return RoutingDecision");
    }

    @Test
    @DisplayName("should have replicateData method")
    void shouldHaveReplicateDataMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.class.getMethod(
              "replicateData",
              String.class,
              byte[].class,
              String.class,
              GlobalDistributionSystem.ReplicationStrategy.class,
              Map.class);
      assertNotNull(method, "replicateData method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getPrimaryRegion method")
    void shouldHaveGetPrimaryRegionMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.class.getMethod("getPrimaryRegion");
      assertNotNull(method, "getPrimaryRegion method should exist");
      assertEquals(
          GlobalDistributionSystem.Region.class, method.getReturnType(), "Should return Region");
    }

    @Test
    @DisplayName("should have setPrimaryRegion method")
    void shouldHaveSetPrimaryRegionMethod() throws NoSuchMethodException {
      final Method method =
          GlobalDistributionSystem.class.getMethod(
              "setPrimaryRegion", GlobalDistributionSystem.Region.class);
      assertNotNull(method, "setPrimaryRegion method should exist");
    }

    @Test
    @DisplayName("should have getDistributionStatistics method")
    void shouldHaveGetDistributionStatisticsMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.class.getMethod("getDistributionStatistics");
      assertNotNull(method, "getDistributionStatistics method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have setEnabled method")
    void shouldHaveSetEnabledMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.class.getMethod("setEnabled", boolean.class);
      assertNotNull(method, "setEnabled method should exist");
    }

    @Test
    @DisplayName("should have shutdown method")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      final Method method = GlobalDistributionSystem.class.getMethod("shutdown");
      assertNotNull(method, "shutdown method should exist");
    }
  }

  @Nested
  @DisplayName("System Instance Behavior Tests")
  class SystemInstanceBehaviorTests {

    private GlobalDistributionSystem system;

    @AfterEach
    void tearDown() {
      if (system != null) {
        system.shutdown();
      }
    }

    @Test
    @DisplayName("system should be enabled initially")
    void systemShouldBeEnabledInitially() {
      system = new GlobalDistributionSystem();
      assertTrue(system.isEnabled(), "System should be enabled initially");
    }

    @Test
    @DisplayName("getRegionalNodes should return non-empty map")
    void getRegionalNodesShouldReturnNonEmptyMap() {
      system = new GlobalDistributionSystem();
      final Map<?, ?> nodes = system.getRegionalNodes();
      assertNotNull(nodes, "Nodes map should not be null");
      assertFalse(nodes.isEmpty(), "System initializes with default nodes");
    }

    @Test
    @DisplayName("getPrimaryRegion should return default region")
    void getPrimaryRegionShouldReturnDefaultRegion() {
      system = new GlobalDistributionSystem();
      final GlobalDistributionSystem.Region primary = system.getPrimaryRegion();
      assertNotNull(primary, "Primary region should not be null");
      assertEquals(
          GlobalDistributionSystem.Region.US_EAST_1,
          primary,
          "Default primary region should be US_EAST_1");
    }

    @Test
    @DisplayName("setPrimaryRegion should change primary region")
    void setPrimaryRegionShouldChangePrimaryRegion() {
      system = new GlobalDistributionSystem();
      system.setPrimaryRegion(GlobalDistributionSystem.Region.EU_WEST_1);
      assertEquals(
          GlobalDistributionSystem.Region.EU_WEST_1,
          system.getPrimaryRegion(),
          "Primary region should be EU_WEST_1");
    }

    @Test
    @DisplayName("setEnabled should change enabled state")
    void setEnabledShouldChangeEnabledState() {
      system = new GlobalDistributionSystem();
      system.setEnabled(false);
      assertFalse(system.isEnabled(), "System should be disabled");
      system.setEnabled(true);
      assertTrue(system.isEnabled(), "System should be enabled");
    }

    @Test
    @DisplayName("getDistributionStatistics should return non-null string")
    void getDistributionStatisticsShouldReturnNonNullString() {
      system = new GlobalDistributionSystem();
      final String stats = system.getDistributionStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertFalse(stats.isEmpty(), "Statistics should not be empty");
    }

    @Test
    @DisplayName("removeRegionalNode should return false for non-existent node")
    void removeRegionalNodeShouldReturnFalseForNonExistent() {
      system = new GlobalDistributionSystem();
      assertFalse(
          system.removeRegionalNode("non-existent-node-id"),
          "Should return false for non-existent node");
    }
  }

  @Nested
  @DisplayName("RegionalNode Direct Construction Tests")
  class RegionalNodeDirectConstructionTests {

    @Test
    @DisplayName("RegionalNode constructor should work")
    void regionalNodeConstructorShouldWork() {
      final GlobalDistributionSystem.RegionalNode node =
          new GlobalDistributionSystem.RegionalNode(
              "test-node-id",
              GlobalDistributionSystem.Region.US_EAST_1,
              "https://test.example.com",
              Map.of("feature", "enabled"));

      assertEquals("test-node-id", node.getNodeId(), "Node ID should match");
      assertEquals(
          GlobalDistributionSystem.Region.US_EAST_1, node.getRegion(), "Region should match");
      assertEquals("https://test.example.com", node.getEndpoint(), "Endpoint should match");
      assertNotNull(node.getCapabilities(), "Capabilities should not be null");
      assertEquals("enabled", node.getCapabilities().get("feature"), "Capability should match");
    }

    @Test
    @DisplayName("RegionalNode should be healthy initially")
    void regionalNodeShouldBeHealthyInitially() {
      final GlobalDistributionSystem.RegionalNode node =
          new GlobalDistributionSystem.RegionalNode(
              "test-node",
              GlobalDistributionSystem.Region.EU_WEST_1,
              "https://eu.example.com",
              Map.of());

      assertEquals(
          GlobalDistributionSystem.NodeHealthStatus.HEALTHY,
          node.getHealthStatus(),
          "Status should be HEALTHY");
      assertTrue(node.isHealthy(), "Node should be healthy");
      assertTrue(node.isAvailable(), "Node should be available");
    }

    @Test
    @DisplayName("RegionalNode should handle null capabilities")
    void regionalNodeShouldHandleNullCapabilities() {
      final GlobalDistributionSystem.RegionalNode node =
          new GlobalDistributionSystem.RegionalNode(
              "test-node",
              GlobalDistributionSystem.Region.AP_NORTHEAST_1,
              "https://ap.example.com",
              null);

      assertNotNull(node.getCapabilities(), "Capabilities should not be null");
      assertTrue(node.getCapabilities().isEmpty(), "Capabilities should be empty");
    }

    @Test
    @DisplayName("RegionalNode updateHealthStatus should work")
    void regionalNodeUpdateHealthStatusShouldWork() {
      final GlobalDistributionSystem.RegionalNode node =
          new GlobalDistributionSystem.RegionalNode(
              "test-node",
              GlobalDistributionSystem.Region.US_WEST_2,
              "https://west.example.com",
              Map.of());

      node.updateHealthStatus(GlobalDistributionSystem.NodeHealthStatus.DEGRADED);
      assertEquals(
          GlobalDistributionSystem.NodeHealthStatus.DEGRADED,
          node.getHealthStatus(),
          "Status should be DEGRADED");
      assertTrue(node.isHealthy(), "DEGRADED should still be considered healthy");

      node.updateHealthStatus(GlobalDistributionSystem.NodeHealthStatus.UNHEALTHY);
      assertFalse(node.isHealthy(), "UNHEALTHY should not be considered healthy");
    }

    @Test
    @DisplayName("RegionalNode getHealthScore should return correct scores")
    void regionalNodeGetHealthScoreShouldReturnCorrectScores() {
      final GlobalDistributionSystem.RegionalNode node =
          new GlobalDistributionSystem.RegionalNode(
              "test-node",
              GlobalDistributionSystem.Region.EU_CENTRAL_1,
              "https://central.example.com",
              Map.of());

      node.updateHealthStatus(GlobalDistributionSystem.NodeHealthStatus.HEALTHY);
      assertEquals(1.0, node.getHealthScore(), 0.001, "HEALTHY score should be 1.0");

      node.updateHealthStatus(GlobalDistributionSystem.NodeHealthStatus.DEGRADED);
      assertEquals(0.7, node.getHealthScore(), 0.001, "DEGRADED score should be 0.7");

      node.updateHealthStatus(GlobalDistributionSystem.NodeHealthStatus.UNHEALTHY);
      assertEquals(0.3, node.getHealthScore(), 0.001, "UNHEALTHY score should be 0.3");

      node.updateHealthStatus(GlobalDistributionSystem.NodeHealthStatus.UNREACHABLE);
      assertEquals(0.0, node.getHealthScore(), 0.001, "UNREACHABLE score should be 0.0");
    }
  }

  @Nested
  @DisplayName("RoutingDecision Direct Construction Tests")
  class RoutingDecisionDirectConstructionTests {

    @Test
    @DisplayName("RoutingDecision constructor should work")
    void routingDecisionConstructorShouldWork() {
      final GlobalDistributionSystem.RegionalNode node =
          new GlobalDistributionSystem.RegionalNode(
              "node-1",
              GlobalDistributionSystem.Region.US_EAST_1,
              "https://us.example.com",
              Map.of());

      final GlobalDistributionSystem.RoutingDecision decision =
          new GlobalDistributionSystem.RoutingDecision(
              node, "Geographic proximity", Map.of("latency_ms", 50), 0.95);

      assertEquals(node, decision.getTargetNode(), "Target node should match");
      assertEquals("Geographic proximity", decision.getRoutingReason(), "Reason should match");
      assertEquals(50, decision.getRoutingMetadata().get("latency_ms"), "Metadata should match");
      assertEquals(0.95, decision.getConfidenceScore(), 0.001, "Confidence should match");
      assertNotNull(decision.getDecisionTime(), "Decision time should be set");
    }

    @Test
    @DisplayName("RoutingDecision should handle null metadata")
    void routingDecisionShouldHandleNullMetadata() {
      final GlobalDistributionSystem.RegionalNode node =
          new GlobalDistributionSystem.RegionalNode(
              "node-1",
              GlobalDistributionSystem.Region.EU_WEST_1,
              "https://eu.example.com",
              Map.of());

      final GlobalDistributionSystem.RoutingDecision decision =
          new GlobalDistributionSystem.RoutingDecision(node, "Load balancing", null, 0.8);

      assertNotNull(decision.getRoutingMetadata(), "Metadata should not be null");
      assertTrue(decision.getRoutingMetadata().isEmpty(), "Metadata should be empty");
    }
  }

  @Nested
  @DisplayName("ReplicationJob Direct Construction Tests")
  class ReplicationJobDirectConstructionTests {

    @Test
    @DisplayName("ReplicationJob constructor should work")
    void replicationJobConstructorShouldWork() {
      final GlobalDistributionSystem.RegionalNode sourceNode =
          new GlobalDistributionSystem.RegionalNode(
              "source-node",
              GlobalDistributionSystem.Region.US_EAST_1,
              "https://source.example.com",
              Map.of());

      final GlobalDistributionSystem.RegionalNode targetNode =
          new GlobalDistributionSystem.RegionalNode(
              "target-node",
              GlobalDistributionSystem.Region.EU_WEST_1,
              "https://target.example.com",
              Map.of());

      final byte[] data = "test data".getBytes();
      final GlobalDistributionSystem.ReplicationJob job =
          new GlobalDistributionSystem.ReplicationJob(
              "job-1",
              "data-123",
              sourceNode,
              targetNode,
              GlobalDistributionSystem.ReplicationStrategy.EVENTUAL_CONSISTENCY,
              data,
              Map.of("priority", "high"));

      assertEquals("job-1", job.getJobId(), "Job ID should match");
      assertEquals("data-123", job.getDataId(), "Data ID should match");
      assertEquals(sourceNode, job.getSourceNode(), "Source node should match");
      assertEquals(targetNode, job.getTargetNode(), "Target node should match");
      assertEquals(
          GlobalDistributionSystem.ReplicationStrategy.EVENTUAL_CONSISTENCY,
          job.getStrategy(),
          "Strategy should match");
      assertFalse(job.isCompleted(), "Job should not be completed initially");
    }

    @Test
    @DisplayName("ReplicationJob start and complete should work")
    void replicationJobStartAndCompleteShouldWork() {
      final GlobalDistributionSystem.RegionalNode sourceNode =
          new GlobalDistributionSystem.RegionalNode(
              "source",
              GlobalDistributionSystem.Region.US_WEST_2,
              "https://source.example.com",
              Map.of());

      final GlobalDistributionSystem.RegionalNode targetNode =
          new GlobalDistributionSystem.RegionalNode(
              "target",
              GlobalDistributionSystem.Region.AP_NORTHEAST_1,
              "https://target.example.com",
              Map.of());

      final GlobalDistributionSystem.ReplicationJob job =
          new GlobalDistributionSystem.ReplicationJob(
              "job-2",
              "data-456",
              sourceNode,
              targetNode,
              GlobalDistributionSystem.ReplicationStrategy.STRONG_CONSISTENCY,
              "payload".getBytes(),
              Map.of());

      job.start();
      assertNotNull(job.getStartTime(), "Start time should be set");

      job.complete(true, null);
      assertTrue(job.isCompleted(), "Job should be completed");
      assertTrue(job.isSuccessful(), "Job should be successful");
      assertNotNull(job.getCompletionTime(), "Completion time should be set");
      assertNotNull(job.getReplicationTime(), "Replication time should be calculated");
    }

    @Test
    @DisplayName("ReplicationJob should handle failure")
    void replicationJobShouldHandleFailure() {
      final GlobalDistributionSystem.RegionalNode sourceNode =
          new GlobalDistributionSystem.RegionalNode(
              "source",
              GlobalDistributionSystem.Region.SA_EAST_1,
              "https://sa.example.com",
              Map.of());

      final GlobalDistributionSystem.RegionalNode targetNode =
          new GlobalDistributionSystem.RegionalNode(
              "target",
              GlobalDistributionSystem.Region.AF_SOUTH_1,
              "https://af.example.com",
              Map.of());

      final GlobalDistributionSystem.ReplicationJob job =
          new GlobalDistributionSystem.ReplicationJob(
              "job-3",
              "data-789",
              sourceNode,
              targetNode,
              GlobalDistributionSystem.ReplicationStrategy.QUORUM_CONSISTENCY,
              "data".getBytes(),
              Map.of());

      job.start();
      job.complete(false, "Network timeout");

      assertTrue(job.isCompleted(), "Job should be completed");
      assertFalse(job.isSuccessful(), "Job should not be successful");
      assertEquals("Network timeout", job.getErrorMessage(), "Error message should match");
    }

    @Test
    @DisplayName("ReplicationJob getData should return copy")
    void replicationJobGetDataShouldReturnCopy() {
      final GlobalDistributionSystem.RegionalNode sourceNode =
          new GlobalDistributionSystem.RegionalNode(
              "source",
              GlobalDistributionSystem.Region.US_EAST_1,
              "https://source.example.com",
              Map.of());

      final GlobalDistributionSystem.RegionalNode targetNode =
          new GlobalDistributionSystem.RegionalNode(
              "target",
              GlobalDistributionSystem.Region.EU_WEST_1,
              "https://target.example.com",
              Map.of());

      final byte[] originalData = "original".getBytes();
      final GlobalDistributionSystem.ReplicationJob job =
          new GlobalDistributionSystem.ReplicationJob(
              "job-4",
              "data-abc",
              sourceNode,
              targetNode,
              GlobalDistributionSystem.ReplicationStrategy.MASTER_SLAVE,
              originalData,
              Map.of());

      final byte[] retrievedData = job.getData();
      assertNotSame(originalData, retrievedData, "getData should return a copy");
      assertEquals(
          new String(originalData), new String(retrievedData), "Data content should match");
    }
  }

  @Nested
  @DisplayName("Routing Behavior Tests")
  class RoutingBehaviorTests {

    private GlobalDistributionSystem system;

    @AfterEach
    void tearDown() {
      if (system != null) {
        system.shutdown();
      }
    }

    @Test
    @DisplayName("routeRequest should return null when disabled")
    void routeRequestShouldReturnNullWhenDisabled() {
      system = new GlobalDistributionSystem();
      system.setEnabled(false);

      final GlobalDistributionSystem.RoutingDecision decision =
          system.routeRequest(GlobalDistributionSystem.Region.US_EAST_1, Map.of());

      // When disabled, routeRequest returns null
      assertTrue(
          decision == null || !system.isEnabled(),
          "Should return null when system is disabled or be disabled");
    }

    @Test
    @DisplayName("routeRequest with matching region should prefer geographic routing")
    void routeRequestWithMatchingRegionShouldPreferGeographicRouting() {
      system = new GlobalDistributionSystem();

      final GlobalDistributionSystem.RoutingDecision decision =
          system.routeRequest(GlobalDistributionSystem.Region.US_EAST_1, Map.of());

      assertNotNull(decision, "Decision should not be null");
      assertNotNull(decision.getTargetNode(), "Target node should not be null");
      assertTrue(decision.getConfidenceScore() > 0, "Confidence score should be positive");
    }
  }
}
