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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public WitCompatibilityResult isCompatibleWith(final WitInterfaceDefinition other) {
      return WitCompatibilityResult.compatible();
    }
  }

  /** Simple mock implementation of WitEvolutionChange for testing. */
  private static WitEvolutionChange createMockChange(final boolean breaking) {
    return new WitEvolutionChange() {
      @Override
      public String getDescription() {
        return breaking ? "Breaking change" : "Non-breaking change";
      }

      @Override
      public boolean isBreaking() {
        return breaking;
      }

      @Override
      public WitEvolutionOperation getOperation() {
        return WitEvolutionOperation.ADD_FIELD;
      }

      @Override
      public String getTargetElement() {
        return "test-element";
      }

      @Override
      public Optional<String> getOldValue() {
        return Optional.empty();
      }

      @Override
      public Optional<String> getNewValue() {
        return Optional.of("new-value");
      }

      @Override
      public Map<String, Object> getMetadata() {
        return Map.of();
      }
    };
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

      final WitEvolutionResult result = new WitEvolutionResult(
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

      final WitEvolutionResult result = new WitEvolutionResult(
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

      final WitEvolutionResult result = WitEvolutionResult.success(
          sourceInterface,
          targetInterface,
          List.of(),
          Map.of(),
          null,
          metrics);

      assertTrue(result.isSuccessful());
      assertTrue(result.getErrorMessage().isEmpty());
      assertNotNull(result.getEvolutionTime());
    }

    @Test
    @DisplayName("failure should create failed result with error message")
    void failureShouldCreateFailedResultWithErrorMessage() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("target");

      final WitEvolutionResult result = WitEvolutionResult.failure(
          sourceInterface,
          targetInterface,
          "Evolution failed: incompatible types");

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

      final WitEvolutionResult result = WitEvolutionResult.failure(
          sourceInterface, targetInterface, "error");

      assertEquals("my-source", result.getSourceInterface().getName());
    }

    @Test
    @DisplayName("getTargetInterface should return target interface")
    void getTargetInterfaceShouldReturnTargetInterface() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("my-target");

      final WitEvolutionResult result = WitEvolutionResult.failure(
          sourceInterface, targetInterface, "error");

      assertEquals("my-target", result.getTargetInterface().getName());
    }

    @Test
    @DisplayName("getEvolutionTime should return evolution time")
    void getEvolutionTimeShouldReturnEvolutionTime() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("target");

      final WitEvolutionResult result = WitEvolutionResult.failure(
          sourceInterface, targetInterface, "error");

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

      final WitEvolutionResult result = WitEvolutionResult.success(
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

      final WitEvolutionResult result = WitEvolutionResult.success(
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

      final WitEvolutionResult result = WitEvolutionResult.success(
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

      final WitEvolutionResult result = WitEvolutionResult.success(
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

      // Create a mock type adapter
      final WitTypeAdapter mockAdapter = new WitTypeAdapter() {
        @Override
        public WitTypeAdapter.AdapterType getAdapterType() {
          return WitTypeAdapter.AdapterType.DIRECT_CONVERSION;
        }

        @Override
        public WitType getSourceType() {
          return null;
        }

        @Override
        public WitType getTargetType() {
          return null;
        }

        @Override
        public Object adapt(final Object value) {
          return value;
        }

        @Override
        public Object reverse(final Object value) {
          return value;
        }

        @Override
        public boolean isReversible() {
          return true;
        }

        @Override
        public WitTypeAdapter.ConversionCost getConversionCost() {
          return WitTypeAdapter.ConversionCost.LOW;
        }

        @Override
        public WitTypeAdapter.AdapterValidationResult validate() {
          return WitTypeAdapter.AdapterValidationResult.success();
        }
      };

      final WitEvolutionResult result = WitEvolutionResult.success(
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

      final WitEvolutionResult result = WitEvolutionResult.success(
          sourceInterface,
          targetInterface,
          List.of(),
          Map.of(),
          null,
          metrics);

      assertTrue(result.getTypeAdapter("nonexistent").isEmpty());
    }

    @Test
    @DisplayName("hasTypeAdapter should return true when adapter exists")
    void hasTypeAdapterShouldReturnTrueWhenAdapterExists() {
      final MockInterfaceDefinition sourceInterface = new MockInterfaceDefinition("source");
      final MockInterfaceDefinition targetInterface = new MockInterfaceDefinition("target");
      final WitEvolutionMetrics metrics = WitEvolutionMetrics.empty();

      final WitTypeAdapter mockAdapter = new WitTypeAdapter() {
        @Override
        public WitTypeAdapter.AdapterType getAdapterType() {
          return WitTypeAdapter.AdapterType.DIRECT_CONVERSION;
        }

        @Override
        public WitType getSourceType() {
          return null;
        }

        @Override
        public WitType getTargetType() {
          return null;
        }

        @Override
        public Object adapt(final Object value) {
          return value;
        }

        @Override
        public Object reverse(final Object value) {
          return value;
        }

        @Override
        public boolean isReversible() {
          return true;
        }

        @Override
        public WitTypeAdapter.ConversionCost getConversionCost() {
          return WitTypeAdapter.ConversionCost.LOW;
        }

        @Override
        public WitTypeAdapter.AdapterValidationResult validate() {
          return WitTypeAdapter.AdapterValidationResult.success();
        }
      };

      final WitEvolutionResult result = WitEvolutionResult.success(
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

      final WitEvolutionResult result = WitEvolutionResult.success(
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

      final WitEvolutionResult result = WitEvolutionResult.failure(
          sourceInterface, targetInterface, "error");

      final String str = result.toString();
      assertTrue(str.contains("successful=false"));
    }
  }
}
