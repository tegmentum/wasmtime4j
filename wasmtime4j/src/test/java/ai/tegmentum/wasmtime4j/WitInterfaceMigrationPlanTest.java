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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitInterfaceMigrationPlan} interface.
 *
 * <p>WitInterfaceMigrationPlan provides migration planning capabilities for WIT interface
 * evolution.
 */
@DisplayName("WitInterfaceMigrationPlan Tests")
class WitInterfaceMigrationPlanTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("WitInterfaceMigrationPlan should be an interface")
    void witInterfaceMigrationPlanShouldBeAnInterface() {
      assertTrue(WitInterfaceMigrationPlan.class.isInterface());
    }

    @Test
    @DisplayName("MigrationStep should be a nested interface")
    void migrationStepShouldBeNestedInterface() {
      assertTrue(WitInterfaceMigrationPlan.MigrationStep.class.isInterface());
    }

    @Test
    @DisplayName("MigrationRisk should be a nested interface")
    void migrationRiskShouldBeNestedInterface() {
      assertTrue(WitInterfaceMigrationPlan.MigrationRisk.class.isInterface());
    }

    @Test
    @DisplayName("RollbackPlan should be a nested interface")
    void rollbackPlanShouldBeNestedInterface() {
      assertTrue(WitInterfaceMigrationPlan.RollbackPlan.class.isInterface());
    }

    @Test
    @DisplayName("RollbackStep should be a nested interface")
    void rollbackStepShouldBeNestedInterface() {
      assertTrue(WitInterfaceMigrationPlan.RollbackStep.class.isInterface());
    }

    @Test
    @DisplayName("Prerequisite should be a nested interface")
    void prerequisiteShouldBeNestedInterface() {
      assertTrue(WitInterfaceMigrationPlan.Prerequisite.class.isInterface());
    }

    @Test
    @DisplayName("MigrationContext should be a nested interface")
    void migrationContextShouldBeNestedInterface() {
      assertTrue(WitInterfaceMigrationPlan.MigrationContext.class.isInterface());
    }

    @Test
    @DisplayName("StepContext should be a nested interface")
    void stepContextShouldBeNestedInterface() {
      assertTrue(WitInterfaceMigrationPlan.StepContext.class.isInterface());
    }

    @Test
    @DisplayName("RollbackContext should be a nested interface")
    void rollbackContextShouldBeNestedInterface() {
      assertTrue(WitInterfaceMigrationPlan.RollbackContext.class.isInterface());
    }

    @Test
    @DisplayName("MigrationResult should be a nested interface")
    void migrationResultShouldBeNestedInterface() {
      assertTrue(WitInterfaceMigrationPlan.MigrationResult.class.isInterface());
    }

    @Test
    @DisplayName("StepResult should be a nested interface")
    void stepResultShouldBeNestedInterface() {
      assertTrue(WitInterfaceMigrationPlan.StepResult.class.isInterface());
    }

    @Test
    @DisplayName("RollbackResult should be a nested interface")
    void rollbackResultShouldBeNestedInterface() {
      assertTrue(WitInterfaceMigrationPlan.RollbackResult.class.isInterface());
    }

    @Test
    @DisplayName("ValidationResult should be a nested interface")
    void validationResultShouldBeNestedInterface() {
      assertTrue(WitInterfaceMigrationPlan.ValidationResult.class.isInterface());
    }

    @Test
    @DisplayName("MigrationListener should be a nested interface")
    void migrationListenerShouldBeNestedInterface() {
      assertTrue(WitInterfaceMigrationPlan.MigrationListener.class.isInterface());
    }
  }

  @Nested
  @DisplayName("MigrationStrategy Enum Tests")
  class MigrationStrategyEnumTests {

    @Test
    @DisplayName("MigrationStrategy should be an enum")
    void migrationStrategyShouldBeEnum() {
      assertTrue(WitInterfaceMigrationPlan.MigrationStrategy.class.isEnum());
    }

    @Test
    @DisplayName("MigrationStrategy should have 5 values")
    void migrationStrategyShouldHave5Values() {
      assertEquals(5, WitInterfaceMigrationPlan.MigrationStrategy.values().length);
    }

    @Test
    @DisplayName("MigrationStrategy should have all expected values")
    void migrationStrategyShouldHaveAllExpectedValues() {
      assertNotNull(WitInterfaceMigrationPlan.MigrationStrategy.BIG_BANG);
      assertNotNull(WitInterfaceMigrationPlan.MigrationStrategy.PHASED);
      assertNotNull(WitInterfaceMigrationPlan.MigrationStrategy.BLUE_GREEN);
      assertNotNull(WitInterfaceMigrationPlan.MigrationStrategy.ROLLING);
      assertNotNull(WitInterfaceMigrationPlan.MigrationStrategy.CANARY);
    }

    @Test
    @DisplayName("MigrationStrategy valueOf should work")
    void migrationStrategyValueOfShouldWork() {
      assertEquals(
          WitInterfaceMigrationPlan.MigrationStrategy.BIG_BANG,
          WitInterfaceMigrationPlan.MigrationStrategy.valueOf("BIG_BANG"));
      assertEquals(
          WitInterfaceMigrationPlan.MigrationStrategy.PHASED,
          WitInterfaceMigrationPlan.MigrationStrategy.valueOf("PHASED"));
      assertEquals(
          WitInterfaceMigrationPlan.MigrationStrategy.BLUE_GREEN,
          WitInterfaceMigrationPlan.MigrationStrategy.valueOf("BLUE_GREEN"));
    }

    @Test
    @DisplayName("MigrationStrategy ordinals should be consistent")
    void migrationStrategyOrdinalsShouldBeConsistent() {
      assertEquals(0, WitInterfaceMigrationPlan.MigrationStrategy.BIG_BANG.ordinal());
      assertEquals(1, WitInterfaceMigrationPlan.MigrationStrategy.PHASED.ordinal());
      assertEquals(2, WitInterfaceMigrationPlan.MigrationStrategy.BLUE_GREEN.ordinal());
      assertEquals(3, WitInterfaceMigrationPlan.MigrationStrategy.ROLLING.ordinal());
      assertEquals(4, WitInterfaceMigrationPlan.MigrationStrategy.CANARY.ordinal());
    }
  }

  @Nested
  @DisplayName("StepType Enum Tests")
  class StepTypeEnumTests {

    @Test
    @DisplayName("StepType should be an enum")
    void stepTypeShouldBeEnum() {
      assertTrue(WitInterfaceMigrationPlan.StepType.class.isEnum());
    }

    @Test
    @DisplayName("StepType should have 9 values")
    void stepTypeShouldHave9Values() {
      assertEquals(9, WitInterfaceMigrationPlan.StepType.values().length);
    }

    @Test
    @DisplayName("StepType should have all expected values")
    void stepTypeShouldHaveAllExpectedValues() {
      assertNotNull(WitInterfaceMigrationPlan.StepType.VALIDATION);
      assertNotNull(WitInterfaceMigrationPlan.StepType.BACKUP);
      assertNotNull(WitInterfaceMigrationPlan.StepType.TRANSFORMATION);
      assertNotNull(WitInterfaceMigrationPlan.StepType.DATA_MIGRATION);
      assertNotNull(WitInterfaceMigrationPlan.StepType.CONFIGURATION);
      assertNotNull(WitInterfaceMigrationPlan.StepType.TESTING);
      assertNotNull(WitInterfaceMigrationPlan.StepType.DEPLOYMENT);
      assertNotNull(WitInterfaceMigrationPlan.StepType.VERIFICATION);
      assertNotNull(WitInterfaceMigrationPlan.StepType.CLEANUP);
    }

    @Test
    @DisplayName("StepType valueOf should work")
    void stepTypeValueOfShouldWork() {
      assertEquals(
          WitInterfaceMigrationPlan.StepType.VALIDATION,
          WitInterfaceMigrationPlan.StepType.valueOf("VALIDATION"));
      assertEquals(
          WitInterfaceMigrationPlan.StepType.BACKUP,
          WitInterfaceMigrationPlan.StepType.valueOf("BACKUP"));
      assertEquals(
          WitInterfaceMigrationPlan.StepType.TRANSFORMATION,
          WitInterfaceMigrationPlan.StepType.valueOf("TRANSFORMATION"));
    }

    @Test
    @DisplayName("StepType ordinals should be in order")
    void stepTypeOrdinalsShouldBeInOrder() {
      assertEquals(0, WitInterfaceMigrationPlan.StepType.VALIDATION.ordinal());
      assertEquals(1, WitInterfaceMigrationPlan.StepType.BACKUP.ordinal());
      assertEquals(2, WitInterfaceMigrationPlan.StepType.TRANSFORMATION.ordinal());
      assertEquals(3, WitInterfaceMigrationPlan.StepType.DATA_MIGRATION.ordinal());
      assertEquals(4, WitInterfaceMigrationPlan.StepType.CONFIGURATION.ordinal());
      assertEquals(5, WitInterfaceMigrationPlan.StepType.TESTING.ordinal());
      assertEquals(6, WitInterfaceMigrationPlan.StepType.DEPLOYMENT.ordinal());
      assertEquals(7, WitInterfaceMigrationPlan.StepType.VERIFICATION.ordinal());
      assertEquals(8, WitInterfaceMigrationPlan.StepType.CLEANUP.ordinal());
    }
  }

  @Nested
  @DisplayName("RiskImpact Enum Tests")
  class RiskImpactEnumTests {

    @Test
    @DisplayName("RiskImpact should be an enum")
    void riskImpactShouldBeEnum() {
      assertTrue(WitInterfaceMigrationPlan.RiskImpact.class.isEnum());
    }

    @Test
    @DisplayName("RiskImpact should have 4 values")
    void riskImpactShouldHave4Values() {
      assertEquals(4, WitInterfaceMigrationPlan.RiskImpact.values().length);
    }

    @Test
    @DisplayName("RiskImpact should have all expected values")
    void riskImpactShouldHaveAllExpectedValues() {
      assertNotNull(WitInterfaceMigrationPlan.RiskImpact.LOW);
      assertNotNull(WitInterfaceMigrationPlan.RiskImpact.MEDIUM);
      assertNotNull(WitInterfaceMigrationPlan.RiskImpact.HIGH);
      assertNotNull(WitInterfaceMigrationPlan.RiskImpact.CRITICAL);
    }

    @Test
    @DisplayName("RiskImpact ordinals should be in order")
    void riskImpactOrdinalsShouldBeInOrder() {
      assertEquals(0, WitInterfaceMigrationPlan.RiskImpact.LOW.ordinal());
      assertEquals(1, WitInterfaceMigrationPlan.RiskImpact.MEDIUM.ordinal());
      assertEquals(2, WitInterfaceMigrationPlan.RiskImpact.HIGH.ordinal());
      assertEquals(3, WitInterfaceMigrationPlan.RiskImpact.CRITICAL.ordinal());
    }
  }

  @Nested
  @DisplayName("PrerequisiteType Enum Tests")
  class PrerequisiteTypeEnumTests {

    @Test
    @DisplayName("PrerequisiteType should be an enum")
    void prerequisiteTypeShouldBeEnum() {
      assertTrue(WitInterfaceMigrationPlan.PrerequisiteType.class.isEnum());
    }

    @Test
    @DisplayName("PrerequisiteType should have 5 values")
    void prerequisiteTypeShouldHave5Values() {
      assertEquals(5, WitInterfaceMigrationPlan.PrerequisiteType.values().length);
    }

    @Test
    @DisplayName("PrerequisiteType should have all expected values")
    void prerequisiteTypeShouldHaveAllExpectedValues() {
      assertNotNull(WitInterfaceMigrationPlan.PrerequisiteType.SYSTEM);
      assertNotNull(WitInterfaceMigrationPlan.PrerequisiteType.DEPENDENCY);
      assertNotNull(WitInterfaceMigrationPlan.PrerequisiteType.CONFIGURATION);
      assertNotNull(WitInterfaceMigrationPlan.PrerequisiteType.PERMISSION);
      assertNotNull(WitInterfaceMigrationPlan.PrerequisiteType.RESOURCE);
    }

    @Test
    @DisplayName("PrerequisiteType ordinals should be in order")
    void prerequisiteTypeOrdinalsShouldBeInOrder() {
      assertEquals(0, WitInterfaceMigrationPlan.PrerequisiteType.SYSTEM.ordinal());
      assertEquals(1, WitInterfaceMigrationPlan.PrerequisiteType.DEPENDENCY.ordinal());
      assertEquals(2, WitInterfaceMigrationPlan.PrerequisiteType.CONFIGURATION.ordinal());
      assertEquals(3, WitInterfaceMigrationPlan.PrerequisiteType.PERMISSION.ordinal());
      assertEquals(4, WitInterfaceMigrationPlan.PrerequisiteType.RESOURCE.ordinal());
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("MigrationStrategy ordinals should be consistent")
    void migrationStrategyOrdinalsShouldBeConsistent() {
      final var values = WitInterfaceMigrationPlan.MigrationStrategy.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal());
      }
    }

    @Test
    @DisplayName("StepType ordinals should be consistent")
    void stepTypeOrdinalsShouldBeConsistent() {
      final var values = WitInterfaceMigrationPlan.StepType.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal());
      }
    }

    @Test
    @DisplayName("RiskImpact ordinals should be consistent")
    void riskImpactOrdinalsShouldBeConsistent() {
      final var values = WitInterfaceMigrationPlan.RiskImpact.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal());
      }
    }

    @Test
    @DisplayName("PrerequisiteType ordinals should be consistent")
    void prerequisiteTypeOrdinalsShouldBeConsistent() {
      final var values = WitInterfaceMigrationPlan.PrerequisiteType.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal());
      }
    }
  }
}
