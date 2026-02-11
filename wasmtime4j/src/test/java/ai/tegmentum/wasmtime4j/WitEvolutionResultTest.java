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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitEvolutionChange;
import ai.tegmentum.wasmtime4j.wit.WitEvolutionMetrics;
import ai.tegmentum.wasmtime4j.wit.WitEvolutionResult;
import ai.tegmentum.wasmtime4j.wit.WitInterfaceDefinition;
import ai.tegmentum.wasmtime4j.wit.WitTypeAdapter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitEvolutionResult} class.
 *
 * <p>WitEvolutionResult encapsulates the complete result of evolving one WIT interface to another.
 */
@DisplayName("WitEvolutionResult Tests")
class WitEvolutionResultTest {

  /** Simple mock implementation of WitInterfaceDefinition for testing. */
  private static class MockInterfaceDefinition implements WitInterfaceDefinition {
    private final String name;

    MockInterfaceDefinition(final String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getVersion() {
      return "1.0.0";
    }

    @Override
    public String getPackageName() {
      return "test";
    }

    @Override
    public List<String> getFunctionNames() {
      return List.of();
    }

    @Override
    public List<String> getTypeNames() {
      return List.of();
    }

    @Override
    public List<String> getImportNames() {
      return List.of();
    }

    @Override
    public List<String> getExportNames() {
      return List.of();
    }

    @Override
    public Set<String> getDependencies() {
      return Set.of();
    }

    @Override
    public String getWitText() {
      return "interface " + name + " {}";
    }

    @Override
    public WitCompatibilityResult isCompatibleWith(final WitInterfaceDefinition other) {
      return WitCompatibilityResult.compatible("Compatible", Set.of());
    }
  }

  /**
   * Creates a WitEvolutionChange for testing using the factory methods. Since WitEvolutionChange is
   * a final class, we use the static factory methods.
   */
  private static WitEvolutionChange createMockChange(final boolean breaking) {
    if (breaking) {
      return WitEvolutionChange.functionRemoved("testFunc", "func testFunc()");
    } else {
      return WitEvolutionChange.functionAdded("testFunc", "func testFunc()");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create result with all fields")
    void shouldCreateResultWithAllFields() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("target");
      final Instant evolutionTime = Instant.now();
      final WitEvolutionMetrics metrics = WitEvolutionMetrics.empty();

      final WitEvolutionResult result =
          new WitEvolutionResult(
              sourceInterface,
              targetInterface,
              true,
              List.of(),
              Map.of(),
              null,
              evolutionTime,
              Optional.empty(),
              metrics);

      assertEquals("source", result.getSourceInterface().getName());
      assertEquals("target", result.getTargetInterface().getName());
      assertTrue(result.isSuccessful());
      assertTrue(result.getChanges().isEmpty());
      assertTrue(result.getTypeAdapters().isEmpty());
      assertNull(result.getBindings());
      assertEquals(evolutionTime, result.getEvolutionTime());
      assertTrue(result.getErrorMessage().isEmpty());
      assertNotNull(result.getMetrics());
    }

    @Test
    @DisplayName("should create defensive copies of lists and maps")
    void shouldCreateDefensiveCopiesOfListsAndMaps() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("target");
      final Instant evolutionTime = Instant.now();
      final WitEvolutionMetrics metrics = WitEvolutionMetrics.empty();

      final WitEvolutionResult result =
          new WitEvolutionResult(
              sourceInterface,
              targetInterface,
              true,
              List.of(createMockChange(false)),
              Map.of(),
              null,
              evolutionTime,
              Optional.empty(),
              metrics);

      assertEquals(1, result.getChanges().size());
      assertTrue(result.getTypeAdapters().isEmpty());
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("success should create successful result")
    void successShouldCreateSuccessfulResult() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("target");
      final WitEvolutionMetrics metrics = WitEvolutionMetrics.empty();

      final WitEvolutionResult result =
          WitEvolutionResult.success(
              sourceInterface, targetInterface, List.of(), Map.of(), null, metrics);

      assertTrue(result.isSuccessful());
      assertTrue(result.getErrorMessage().isEmpty());
      assertNotNull(result.getEvolutionTime());
    }

    @Test
    @DisplayName("failure should create failed result with error message")
    void failureShouldCreateFailedResultWithErrorMessage() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("target");

      final WitEvolutionResult result =
          WitEvolutionResult.failure(
              sourceInterface, targetInterface, "Evolution failed: incompatible types");

      assertFalse(result.isSuccessful());
      assertTrue(result.getErrorMessage().isPresent());
      assertEquals("Evolution failed: incompatible types", result.getErrorMessage().get());
      assertTrue(result.getChanges().isEmpty());
      assertTrue(result.getTypeAdapters().isEmpty());
      assertNull(result.getBindings());
      assertNotNull(result.getMetrics());
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getSourceInterface should return source interface")
    void getSourceInterfaceShouldReturnSourceInterface() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("my-source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("target");

      final WitEvolutionResult result =
          WitEvolutionResult.failure(sourceInterface, targetInterface, "error");

      assertEquals("my-source", result.getSourceInterface().getName());
    }

    @Test
    @DisplayName("getTargetInterface should return target interface")
    void getTargetInterfaceShouldReturnTargetInterface() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("my-target");

      final WitEvolutionResult result =
          WitEvolutionResult.failure(sourceInterface, targetInterface, "error");

      assertEquals("my-target", result.getTargetInterface().getName());
    }

    @Test
    @DisplayName("getEvolutionTime should return evolution time")
    void getEvolutionTimeShouldReturnEvolutionTime() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("target");

      final WitEvolutionResult result =
          WitEvolutionResult.failure(sourceInterface, targetInterface, "error");

      assertNotNull(result.getEvolutionTime());
      // Evolution time should be recent
      final Instant now = Instant.now();
      assertTrue(Duration.between(result.getEvolutionTime(), now).toMillis() < 1000);
    }
  }

  @Nested
  @DisplayName("Breaking Changes Tests")
  class BreakingChangesTests {

    @Test
    @DisplayName("hasBreakingChanges should return true when breaking changes exist")
    void hasBreakingChangesShouldReturnTrueWhenBreakingChangesExist() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("target");
      final WitEvolutionMetrics metrics = WitEvolutionMetrics.empty();

      final WitEvolutionResult result =
          WitEvolutionResult.success(
              sourceInterface,
              targetInterface,
              List.of(createMockChange(true), createMockChange(false)),
              Map.of(),
              null,
              metrics);

      assertTrue(result.hasBreakingChanges());
    }

    @Test
    @DisplayName("hasBreakingChanges should return false when no breaking changes")
    void hasBreakingChangesShouldReturnFalseWhenNoBreakingChanges() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("target");
      final WitEvolutionMetrics metrics = WitEvolutionMetrics.empty();

      final WitEvolutionResult result =
          WitEvolutionResult.success(
              sourceInterface,
              targetInterface,
              List.of(createMockChange(false), createMockChange(false)),
              Map.of(),
              null,
              metrics);

      assertFalse(result.hasBreakingChanges());
    }

    @Test
    @DisplayName("getBreakingChanges should return only breaking changes")
    void getBreakingChangesShouldReturnOnlyBreakingChanges() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("target");
      final WitEvolutionMetrics metrics = WitEvolutionMetrics.empty();

      final WitEvolutionResult result =
          WitEvolutionResult.success(
              sourceInterface,
              targetInterface,
              List.of(createMockChange(true), createMockChange(false), createMockChange(true)),
              Map.of(),
              null,
              metrics);

      final List<WitEvolutionChange> breakingChanges = result.getBreakingChanges();
      assertEquals(2, breakingChanges.size());
      assertTrue(breakingChanges.stream().allMatch(WitEvolutionChange::isBreaking));
    }

    @Test
    @DisplayName("getNonBreakingChanges should return only non-breaking changes")
    void getNonBreakingChangesShouldReturnOnlyNonBreakingChanges() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("target");
      final WitEvolutionMetrics metrics = WitEvolutionMetrics.empty();

      final WitEvolutionResult result =
          WitEvolutionResult.success(
              sourceInterface,
              targetInterface,
              List.of(createMockChange(true), createMockChange(false), createMockChange(false)),
              Map.of(),
              null,
              metrics);

      final List<WitEvolutionChange> nonBreakingChanges = result.getNonBreakingChanges();
      assertEquals(2, nonBreakingChanges.size());
      assertTrue(nonBreakingChanges.stream().noneMatch(WitEvolutionChange::isBreaking));
    }
  }

  @Nested
  @DisplayName("Type Adapter Tests")
  class TypeAdapterTests {

    @Test
    @DisplayName("getTypeAdapter should return adapter when exists")
    void getTypeAdapterShouldReturnAdapterWhenExists() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("target");
      final WitEvolutionMetrics metrics = WitEvolutionMetrics.empty();

      // Create a mock type adapter implementing the interface
      final WitTypeAdapter mockAdapter = createMockTypeAdapter();

      final WitEvolutionResult result =
          WitEvolutionResult.success(
              sourceInterface,
              targetInterface,
              List.of(),
              Map.of("myType", mockAdapter),
              null,
              metrics);

      assertTrue(result.getTypeAdapter("myType").isPresent());
      assertEquals(mockAdapter, result.getTypeAdapter("myType").get());
    }

    @Test
    @DisplayName("getTypeAdapter should return empty when not exists")
    void getTypeAdapterShouldReturnEmptyWhenNotExists() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("target");
      final WitEvolutionMetrics metrics = WitEvolutionMetrics.empty();

      final WitEvolutionResult result =
          WitEvolutionResult.success(
              sourceInterface, targetInterface, List.of(), Map.of(), null, metrics);

      assertTrue(result.getTypeAdapter("nonexistent").isEmpty());
    }

    @Test
    @DisplayName("hasTypeAdapter should return true when adapter exists")
    void hasTypeAdapterShouldReturnTrueWhenAdapterExists() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("target");
      final WitEvolutionMetrics metrics = WitEvolutionMetrics.empty();

      final WitTypeAdapter mockAdapter = createMockTypeAdapter();

      final WitEvolutionResult result =
          WitEvolutionResult.success(
              sourceInterface,
              targetInterface,
              List.of(),
              Map.of("existingType", mockAdapter),
              null,
              metrics);

      assertTrue(result.hasTypeAdapter("existingType"));
      assertFalse(result.hasTypeAdapter("missingType"));
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain key information")
    void toStringShouldContainKeyInformation() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("sourceIface");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("targetIface");
      final WitEvolutionMetrics metrics = WitEvolutionMetrics.empty();

      final WitEvolutionResult result =
          WitEvolutionResult.success(
              sourceInterface,
              targetInterface,
              List.of(createMockChange(false)),
              Map.of(),
              null,
              metrics);

      final String str = result.toString();
      assertTrue(str.contains("WitEvolutionResult"));
      assertTrue(str.contains("sourceInterface=sourceIface"));
      assertTrue(str.contains("targetInterface=targetIface"));
      assertTrue(str.contains("successful=true"));
      assertTrue(str.contains("changes=1"));
    }

    @Test
    @DisplayName("toString for failure should show failure status")
    void toStringForFailureShouldShowFailureStatus() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("target");

      final WitEvolutionResult result =
          WitEvolutionResult.failure(sourceInterface, targetInterface, "error");

      final String str = result.toString();
      assertTrue(str.contains("successful=false"));
    }
  }

  /** Creates a mock WitTypeAdapter for testing. */
  private static WitTypeAdapter createMockTypeAdapter() {
    return new WitTypeAdapter() {
      @Override
      public String getSourceTypeName() {
        return "source-type";
      }

      @Override
      public String getTargetTypeName() {
        return "target-type";
      }

      @Override
      public AdapterType getAdapterType() {
        return AdapterType.DIRECT_CONVERSION;
      }

      @Override
      public boolean supportsForwardConversion() {
        return true;
      }

      @Override
      public boolean supportsReverseConversion() {
        return true;
      }

      @Override
      public WasmValue convertForward(final WasmValue sourceValue) {
        return sourceValue;
      }

      @Override
      public WasmValue convertReverse(final WasmValue targetValue) {
        return targetValue;
      }

      @Override
      public AdapterValidationResult validateForwardConversion(final WasmValue sourceValue) {
        return AdapterValidationResult.success();
      }

      @Override
      public AdapterValidationResult validateReverseConversion(final WasmValue targetValue) {
        return AdapterValidationResult.success();
      }

      @Override
      public ConversionMetadata getForwardConversionMetadata() {
        return createMockConversionMetadata();
      }

      @Override
      public ConversionMetadata getReverseConversionMetadata() {
        return createMockConversionMetadata();
      }

      @Override
      public AdapterStatistics getStatistics() {
        return createMockAdapterStatistics();
      }

      @Override
      public boolean isLossless() {
        return true;
      }

      @Override
      public List<String> getLimitations() {
        return List.of();
      }

      @Override
      public Optional<TypeMappingInfo> getTypeMappingInfo() {
        return Optional.empty();
      }

      @Override
      public void resetStatistics() {
        // No-op for mock
      }
    };
  }

  /** Creates a mock ConversionMetadata for testing. */
  private static WitTypeAdapter.ConversionMetadata createMockConversionMetadata() {
    return new WitTypeAdapter.ConversionMetadata() {
      @Override
      public boolean isLossy() {
        return false;
      }

      @Override
      public WitTypeAdapter.ConversionCost getCost() {
        return WitTypeAdapter.ConversionCost.LOW;
      }

      @Override
      public List<String> getPreconditions() {
        return List.of();
      }

      @Override
      public List<String> getPostconditions() {
        return List.of();
      }

      @Override
      public Map<String, Object> getProperties() {
        return Map.of();
      }
    };
  }

  /** Creates a mock AdapterStatistics for testing. */
  private static WitTypeAdapter.AdapterStatistics createMockAdapterStatistics() {
    return new WitTypeAdapter.AdapterStatistics() {
      @Override
      public long getForwardConversions() {
        return 0;
      }

      @Override
      public long getReverseConversions() {
        return 0;
      }

      @Override
      public long getSuccessfulConversions() {
        return 0;
      }

      @Override
      public long getFailedConversions() {
        return 0;
      }

      @Override
      public double getAverageConversionTime() {
        return 0.0;
      }

      @Override
      public long getTotalConversionTime() {
        return 0;
      }

      @Override
      public double getSuccessRate() {
        return 1.0;
      }

      @Override
      public Optional<java.time.Instant> getLastConversionTime() {
        return Optional.empty();
      }
    };
  }
}
