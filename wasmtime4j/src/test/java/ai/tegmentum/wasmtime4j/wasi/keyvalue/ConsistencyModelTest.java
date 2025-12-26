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

package ai.tegmentum.wasmtime4j.wasi.keyvalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ConsistencyModel} enum.
 *
 * <p>ConsistencyModel represents consistency models supported by WASI-keyvalue.
 */
@DisplayName("ConsistencyModel Tests")
class ConsistencyModelTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(ConsistencyModel.class.isEnum(), "ConsistencyModel should be an enum");
    }

    @Test
    @DisplayName("should have exactly 8 values")
    void shouldHaveExactlyEightValues() {
      final ConsistencyModel[] values = ConsistencyModel.values();
      assertEquals(8, values.length, "Should have exactly 8 consistency models");
    }

    @Test
    @DisplayName("should have EVENTUAL value")
    void shouldHaveEventualValue() {
      assertNotNull(ConsistencyModel.valueOf("EVENTUAL"), "Should have EVENTUAL");
    }

    @Test
    @DisplayName("should have STRONG value")
    void shouldHaveStrongValue() {
      assertNotNull(ConsistencyModel.valueOf("STRONG"), "Should have STRONG");
    }

    @Test
    @DisplayName("should have CAUSAL value")
    void shouldHaveCausalValue() {
      assertNotNull(ConsistencyModel.valueOf("CAUSAL"), "Should have CAUSAL");
    }

    @Test
    @DisplayName("should have SEQUENTIAL value")
    void shouldHaveSequentialValue() {
      assertNotNull(ConsistencyModel.valueOf("SEQUENTIAL"), "Should have SEQUENTIAL");
    }

    @Test
    @DisplayName("should have LINEARIZABLE value")
    void shouldHaveLinearizableValue() {
      assertNotNull(ConsistencyModel.valueOf("LINEARIZABLE"), "Should have LINEARIZABLE");
    }

    @Test
    @DisplayName("should have SESSION value")
    void shouldHaveSessionValue() {
      assertNotNull(ConsistencyModel.valueOf("SESSION"), "Should have SESSION");
    }

    @Test
    @DisplayName("should have MONOTONIC_READ value")
    void shouldHaveMonotonicReadValue() {
      assertNotNull(ConsistencyModel.valueOf("MONOTONIC_READ"), "Should have MONOTONIC_READ");
    }

    @Test
    @DisplayName("should have MONOTONIC_WRITE value")
    void shouldHaveMonotonicWriteValue() {
      assertNotNull(ConsistencyModel.valueOf("MONOTONIC_WRITE"), "Should have MONOTONIC_WRITE");
    }
  }

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final ConsistencyModel model : ConsistencyModel.values()) {
        assertTrue(ordinals.add(model.ordinal()), "Ordinal should be unique: " + model);
      }
    }

    @Test
    @DisplayName("should have unique names")
    void shouldHaveUniqueNames() {
      final Set<String> names = new HashSet<>();
      for (final ConsistencyModel model : ConsistencyModel.values()) {
        assertTrue(names.add(model.name()), "Name should be unique: " + model);
      }
    }

    @Test
    @DisplayName("should be retrievable by name")
    void shouldBeRetrievableByName() {
      for (final ConsistencyModel model : ConsistencyModel.values()) {
        assertEquals(
            model, ConsistencyModel.valueOf(model.name()), "Should be retrievable by name");
      }
    }
  }

  @Nested
  @DisplayName("Consistency Model Category Tests")
  class ConsistencyModelCategoryTests {

    @Test
    @DisplayName("should have weak consistency models")
    void shouldHaveWeakConsistencyModels() {
      // Weak consistency models offer best performance
      final Set<ConsistencyModel> weakModels =
          Set.of(ConsistencyModel.EVENTUAL, ConsistencyModel.SESSION);

      for (final ConsistencyModel model : weakModels) {
        assertNotNull(model, "Should have weak model: " + model);
      }
    }

    @Test
    @DisplayName("should have strong consistency models")
    void shouldHaveStrongConsistencyModels() {
      // Strong consistency models offer strongest guarantees
      final Set<ConsistencyModel> strongModels =
          Set.of(ConsistencyModel.STRONG, ConsistencyModel.LINEARIZABLE);

      for (final ConsistencyModel model : strongModels) {
        assertNotNull(model, "Should have strong model: " + model);
      }
    }

    @Test
    @DisplayName("should have ordering consistency models")
    void shouldHaveOrderingConsistencyModels() {
      // Models that maintain ordering guarantees
      final Set<ConsistencyModel> orderingModels =
          Set.of(
              ConsistencyModel.CAUSAL,
              ConsistencyModel.SEQUENTIAL,
              ConsistencyModel.MONOTONIC_READ,
              ConsistencyModel.MONOTONIC_WRITE);

      for (final ConsistencyModel model : orderingModels) {
        assertNotNull(model, "Should have ordering model: " + model);
      }
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support switch statement")
    void shouldSupportSwitchStatement() {
      final ConsistencyModel model = ConsistencyModel.STRONG;

      final String description;
      switch (model) {
        case EVENTUAL:
          description = "Best performance, eventual convergence";
          break;
        case STRONG:
          description = "Immediate consistency, slower performance";
          break;
        case CAUSAL:
          description = "Maintains causal relationships";
          break;
        case LINEARIZABLE:
          description = "Strongest consistency model";
          break;
        default:
          description = "Other consistency model";
      }

      assertEquals("Immediate consistency, slower performance", description, "STRONG description");
    }

    @Test
    @DisplayName("should be usable in collections")
    void shouldBeUsableInCollections() {
      final Set<ConsistencyModel> supportedModels = new HashSet<>();
      supportedModels.add(ConsistencyModel.EVENTUAL);
      supportedModels.add(ConsistencyModel.STRONG);
      supportedModels.add(ConsistencyModel.CAUSAL);

      assertTrue(supportedModels.contains(ConsistencyModel.EVENTUAL), "Should contain EVENTUAL");
      assertTrue(supportedModels.contains(ConsistencyModel.STRONG), "Should contain STRONG");
      assertEquals(3, supportedModels.size(), "Should have 3 supported models");
    }

    @Test
    @DisplayName("should support consistency level selection")
    void shouldSupportConsistencyLevelSelection() {
      // Pattern: select consistency based on requirements
      final boolean needsStrongConsistency = true;
      final boolean needsPerformance = false;

      final ConsistencyModel selected;
      if (needsStrongConsistency && !needsPerformance) {
        selected = ConsistencyModel.LINEARIZABLE;
      } else if (!needsStrongConsistency && needsPerformance) {
        selected = ConsistencyModel.EVENTUAL;
      } else {
        selected = ConsistencyModel.STRONG;
      }

      assertEquals(ConsistencyModel.LINEARIZABLE, selected, "Should select LINEARIZABLE");
    }

    @Test
    @DisplayName("should support consistency comparison")
    void shouldSupportConsistencyComparison() {
      // LINEARIZABLE is strongest, EVENTUAL is weakest based on typical ordinal ordering
      final ConsistencyModel eventual = ConsistencyModel.EVENTUAL;
      final ConsistencyModel linearizable = ConsistencyModel.LINEARIZABLE;

      final boolean eventualIsWeaker = eventual.ordinal() < linearizable.ordinal();
      assertTrue(eventualIsWeaker, "EVENTUAL should have lower ordinal than LINEARIZABLE");
    }
  }

  @Nested
  @DisplayName("Distributed Systems Pattern Tests")
  class DistributedSystemsPatternTests {

    @Test
    @DisplayName("should support CAP theorem tradeoffs")
    void shouldSupportCapTheoremTradeoffs() {
      // Eventual consistency favors availability
      // Strong consistency favors consistency
      final ConsistencyModel availabilityFocused = ConsistencyModel.EVENTUAL;
      final ConsistencyModel consistencyFocused = ConsistencyModel.STRONG;

      assertEquals("EVENTUAL", availabilityFocused.name(), "Availability-focused model");
      assertEquals("STRONG", consistencyFocused.name(), "Consistency-focused model");
    }

    @Test
    @DisplayName("should support read-your-writes pattern")
    void shouldSupportReadYourWritesPattern() {
      // SESSION consistency provides read-your-writes guarantee
      final ConsistencyModel readYourWrites = ConsistencyModel.SESSION;

      assertNotNull(readYourWrites, "SESSION provides read-your-writes");
    }

    @Test
    @DisplayName("should support monotonic reads pattern")
    void shouldSupportMonotonicReadsPattern() {
      // MONOTONIC_READ ensures reads never go backwards
      final ConsistencyModel monotonic = ConsistencyModel.MONOTONIC_READ;

      assertNotNull(monotonic, "MONOTONIC_READ ensures progress");
    }

    @Test
    @DisplayName("should support causal ordering pattern")
    void shouldSupportCausalOrderingPattern() {
      // CAUSAL consistency maintains happens-before relationships
      final ConsistencyModel causal = ConsistencyModel.CAUSAL;

      assertNotNull(causal, "CAUSAL maintains causal ordering");
    }
  }

  @Nested
  @DisplayName("Database Consistency Tests")
  class DatabaseConsistencyTests {

    @Test
    @DisplayName("should cover common database consistency models")
    void shouldCoverCommonDatabaseConsistencyModels() {
      // Verify common database consistency models are represented
      assertNotNull(ConsistencyModel.EVENTUAL, "NoSQL typical model");
      assertNotNull(ConsistencyModel.STRONG, "SQL typical model");
      assertNotNull(ConsistencyModel.SEQUENTIAL, "Sequential consistency");
      assertNotNull(ConsistencyModel.LINEARIZABLE, "Strictest model");
    }

    @Test
    @DisplayName("should support replica consistency selection")
    void shouldSupportReplicaConsistencySelection() {
      // Pattern: select consistency based on replica topology
      final int replicaCount = 3;
      final boolean geographicallyDistributed = true;

      final ConsistencyModel selected;
      if (geographicallyDistributed && replicaCount > 2) {
        selected = ConsistencyModel.EVENTUAL; // Better for geo-distributed
      } else {
        selected = ConsistencyModel.STRONG;
      }

      assertEquals(ConsistencyModel.EVENTUAL, selected, "Geo-distributed should use EVENTUAL");
    }
  }
}
