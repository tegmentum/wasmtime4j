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

import ai.tegmentum.wasmtime4j.ComponentStateTransitionConfig.ActionType;
import ai.tegmentum.wasmtime4j.ComponentStateTransitionConfig.AuditLevel;
import ai.tegmentum.wasmtime4j.ComponentStateTransitionConfig.ComponentState;
import ai.tegmentum.wasmtime4j.ComponentStateTransitionConfig.TransitionTrigger;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentStateTransitionConfig} interface.
 *
 * <p>ComponentStateTransitionConfig provides configuration for component state transitions.
 */
@DisplayName("ComponentStateTransitionConfig Tests")
class ComponentStateTransitionConfigTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentStateTransitionConfig.class.getModifiers()),
          "ComponentStateTransitionConfig should be public");
      assertTrue(
          ComponentStateTransitionConfig.class.isInterface(),
          "ComponentStateTransitionConfig should be an interface");
    }

    @Test
    @DisplayName("should have TransitionValidator nested interface")
    void shouldHaveTransitionValidatorNestedInterface() {
      final var nestedClasses = ComponentStateTransitionConfig.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("TransitionValidator")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "TransitionValidator should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have TransitionValidator nested interface");
    }

    @Test
    @DisplayName("should have TransitionInterceptor nested interface")
    void shouldHaveTransitionInterceptorNestedInterface() {
      final var nestedClasses = ComponentStateTransitionConfig.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("TransitionInterceptor")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "TransitionInterceptor should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have TransitionInterceptor nested interface");
    }

    @Test
    @DisplayName("should have TransitionGuard nested interface")
    void shouldHaveTransitionGuardNestedInterface() {
      final var nestedClasses = ComponentStateTransitionConfig.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("TransitionGuard")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "TransitionGuard should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have TransitionGuard nested interface");
    }

    @Test
    @DisplayName("should have TransitionAction nested interface")
    void shouldHaveTransitionActionNestedInterface() {
      final var nestedClasses = ComponentStateTransitionConfig.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("TransitionAction")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "TransitionAction should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have TransitionAction nested interface");
    }

    @Test
    @DisplayName("should have TransitionContext nested interface")
    void shouldHaveTransitionContextNestedInterface() {
      final var nestedClasses = ComponentStateTransitionConfig.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("TransitionContext")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "TransitionContext should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have TransitionContext nested interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getAllowedTransitions method")
    void shouldHaveGetAllowedTransitionsMethod() throws NoSuchMethodException {
      final Method method = ComponentStateTransitionConfig.class.getMethod("getAllowedTransitions");
      assertNotNull(method, "getAllowedTransitions method should exist");
      assertEquals(java.util.Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getValidators method")
    void shouldHaveGetValidatorsMethod() throws NoSuchMethodException {
      final Method method = ComponentStateTransitionConfig.class.getMethod("getValidators");
      assertNotNull(method, "getValidators method should exist");
      assertEquals(java.util.List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getInterceptors method")
    void shouldHaveGetInterceptorsMethod() throws NoSuchMethodException {
      final Method method = ComponentStateTransitionConfig.class.getMethod("getInterceptors");
      assertNotNull(method, "getInterceptors method should exist");
      assertEquals(java.util.List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getTimeoutConfig method")
    void shouldHaveGetTimeoutConfigMethod() throws NoSuchMethodException {
      final Method method = ComponentStateTransitionConfig.class.getMethod("getTimeoutConfig");
      assertNotNull(method, "getTimeoutConfig method should exist");
    }

    @Test
    @DisplayName("should have getRetryPolicy method")
    void shouldHaveGetRetryPolicyMethod() throws NoSuchMethodException {
      final Method method = ComponentStateTransitionConfig.class.getMethod("getRetryPolicy");
      assertNotNull(method, "getRetryPolicy method should exist");
    }

    @Test
    @DisplayName("should have getRollbackConfig method")
    void shouldHaveGetRollbackConfigMethod() throws NoSuchMethodException {
      final Method method = ComponentStateTransitionConfig.class.getMethod("getRollbackConfig");
      assertNotNull(method, "getRollbackConfig method should exist");
    }

    @Test
    @DisplayName("should have getAuditConfig method")
    void shouldHaveGetAuditConfigMethod() throws NoSuchMethodException {
      final Method method = ComponentStateTransitionConfig.class.getMethod("getAuditConfig");
      assertNotNull(method, "getAuditConfig method should exist");
    }

    @Test
    @DisplayName("should have isTransitionAllowed method")
    void shouldHaveIsTransitionAllowedMethod() throws NoSuchMethodException {
      final Method method =
          ComponentStateTransitionConfig.class.getMethod(
              "isTransitionAllowed", ComponentState.class, ComponentState.class);
      assertNotNull(method, "isTransitionAllowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getTransitionGuards method")
    void shouldHaveGetTransitionGuardsMethod() throws NoSuchMethodException {
      final Method method =
          ComponentStateTransitionConfig.class.getMethod(
              "getTransitionGuards", ComponentState.class, ComponentState.class);
      assertNotNull(method, "getTransitionGuards method should exist");
      assertEquals(java.util.List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getTransitionActions method")
    void shouldHaveGetTransitionActionsMethod() throws NoSuchMethodException {
      final Method method =
          ComponentStateTransitionConfig.class.getMethod(
              "getTransitionActions", ComponentState.class, ComponentState.class);
      assertNotNull(method, "getTransitionActions method should exist");
      assertEquals(java.util.List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("ComponentState Enum Tests")
  class ComponentStateEnumTests {

    @Test
    @DisplayName("should have all component states")
    void shouldHaveAllComponentStates() {
      final var states = ComponentState.values();
      assertEquals(13, states.length, "Should have 13 component states");
    }

    @Test
    @DisplayName("should have UNINITIALIZED state")
    void shouldHaveUninitializedState() {
      assertEquals(ComponentState.UNINITIALIZED, ComponentState.valueOf("UNINITIALIZED"));
    }

    @Test
    @DisplayName("should have INITIALIZING state")
    void shouldHaveInitializingState() {
      assertEquals(ComponentState.INITIALIZING, ComponentState.valueOf("INITIALIZING"));
    }

    @Test
    @DisplayName("should have READY state")
    void shouldHaveReadyState() {
      assertEquals(ComponentState.READY, ComponentState.valueOf("READY"));
    }

    @Test
    @DisplayName("should have STARTING state")
    void shouldHaveStartingState() {
      assertEquals(ComponentState.STARTING, ComponentState.valueOf("STARTING"));
    }

    @Test
    @DisplayName("should have RUNNING state")
    void shouldHaveRunningState() {
      assertEquals(ComponentState.RUNNING, ComponentState.valueOf("RUNNING"));
    }

    @Test
    @DisplayName("should have PAUSING state")
    void shouldHavePausingState() {
      assertEquals(ComponentState.PAUSING, ComponentState.valueOf("PAUSING"));
    }

    @Test
    @DisplayName("should have PAUSED state")
    void shouldHavePausedState() {
      assertEquals(ComponentState.PAUSED, ComponentState.valueOf("PAUSED"));
    }

    @Test
    @DisplayName("should have STOPPING state")
    void shouldHaveStoppingState() {
      assertEquals(ComponentState.STOPPING, ComponentState.valueOf("STOPPING"));
    }

    @Test
    @DisplayName("should have STOPPED state")
    void shouldHaveStoppedState() {
      assertEquals(ComponentState.STOPPED, ComponentState.valueOf("STOPPED"));
    }

    @Test
    @DisplayName("should have DESTROYING state")
    void shouldHaveDestroyingState() {
      assertEquals(ComponentState.DESTROYING, ComponentState.valueOf("DESTROYING"));
    }

    @Test
    @DisplayName("should have DESTROYED state")
    void shouldHaveDestroyedState() {
      assertEquals(ComponentState.DESTROYED, ComponentState.valueOf("DESTROYED"));
    }

    @Test
    @DisplayName("should have ERROR state")
    void shouldHaveErrorState() {
      assertEquals(ComponentState.ERROR, ComponentState.valueOf("ERROR"));
    }

    @Test
    @DisplayName("should have RECOVERING state")
    void shouldHaveRecoveringState() {
      assertEquals(ComponentState.RECOVERING, ComponentState.valueOf("RECOVERING"));
    }
  }

  @Nested
  @DisplayName("TransitionTrigger Enum Tests")
  class TransitionTriggerEnumTests {

    @Test
    @DisplayName("should have all transition triggers")
    void shouldHaveAllTransitionTriggers() {
      final var triggers = TransitionTrigger.values();
      assertEquals(6, triggers.length, "Should have 6 transition triggers");
    }

    @Test
    @DisplayName("should have USER trigger")
    void shouldHaveUserTrigger() {
      assertEquals(TransitionTrigger.USER, TransitionTrigger.valueOf("USER"));
    }

    @Test
    @DisplayName("should have SYSTEM trigger")
    void shouldHaveSystemTrigger() {
      assertEquals(TransitionTrigger.SYSTEM, TransitionTrigger.valueOf("SYSTEM"));
    }

    @Test
    @DisplayName("should have TIMER trigger")
    void shouldHaveTimerTrigger() {
      assertEquals(TransitionTrigger.TIMER, TransitionTrigger.valueOf("TIMER"));
    }

    @Test
    @DisplayName("should have EVENT trigger")
    void shouldHaveEventTrigger() {
      assertEquals(TransitionTrigger.EVENT, TransitionTrigger.valueOf("EVENT"));
    }

    @Test
    @DisplayName("should have ERROR trigger")
    void shouldHaveErrorTrigger() {
      assertEquals(TransitionTrigger.ERROR, TransitionTrigger.valueOf("ERROR"));
    }

    @Test
    @DisplayName("should have EXTERNAL trigger")
    void shouldHaveExternalTrigger() {
      assertEquals(TransitionTrigger.EXTERNAL, TransitionTrigger.valueOf("EXTERNAL"));
    }
  }

  @Nested
  @DisplayName("ActionType Enum Tests")
  class ActionTypeEnumTests {

    @Test
    @DisplayName("should have all action types")
    void shouldHaveAllActionTypes() {
      final var types = ActionType.values();
      assertEquals(5, types.length, "Should have 5 action types");
    }

    @Test
    @DisplayName("should have PRE_TRANSITION type")
    void shouldHavePreTransitionType() {
      assertEquals(ActionType.PRE_TRANSITION, ActionType.valueOf("PRE_TRANSITION"));
    }

    @Test
    @DisplayName("should have POST_TRANSITION type")
    void shouldHavePostTransitionType() {
      assertEquals(ActionType.POST_TRANSITION, ActionType.valueOf("POST_TRANSITION"));
    }

    @Test
    @DisplayName("should have CLEANUP type")
    void shouldHaveCleanupType() {
      assertEquals(ActionType.CLEANUP, ActionType.valueOf("CLEANUP"));
    }

    @Test
    @DisplayName("should have NOTIFICATION type")
    void shouldHaveNotificationType() {
      assertEquals(ActionType.NOTIFICATION, ActionType.valueOf("NOTIFICATION"));
    }

    @Test
    @DisplayName("should have VALIDATION type")
    void shouldHaveValidationType() {
      assertEquals(ActionType.VALIDATION, ActionType.valueOf("VALIDATION"));
    }
  }

  @Nested
  @DisplayName("AuditLevel Enum Tests")
  class AuditLevelEnumTests {

    @Test
    @DisplayName("should have all audit levels")
    void shouldHaveAllAuditLevels() {
      final var levels = AuditLevel.values();
      assertEquals(5, levels.length, "Should have 5 audit levels");
    }

    @Test
    @DisplayName("should have NONE level")
    void shouldHaveNoneLevel() {
      assertEquals(AuditLevel.NONE, AuditLevel.valueOf("NONE"));
    }

    @Test
    @DisplayName("should have ERROR level")
    void shouldHaveErrorLevel() {
      assertEquals(AuditLevel.ERROR, AuditLevel.valueOf("ERROR"));
    }

    @Test
    @DisplayName("should have IMPORTANT level")
    void shouldHaveImportantLevel() {
      assertEquals(AuditLevel.IMPORTANT, AuditLevel.valueOf("IMPORTANT"));
    }

    @Test
    @DisplayName("should have ALL level")
    void shouldHaveAllLevel() {
      assertEquals(AuditLevel.ALL, AuditLevel.valueOf("ALL"));
    }

    @Test
    @DisplayName("should have DEBUG level")
    void shouldHaveDebugLevel() {
      assertEquals(AuditLevel.DEBUG, AuditLevel.valueOf("DEBUG"));
    }
  }

  @Nested
  @DisplayName("Nested Interface Structure Tests")
  class NestedInterfaceStructureTests {

    @Test
    @DisplayName("should have all expected nested interfaces")
    void shouldHaveAllExpectedNestedInterfaces() {
      final var nestedClasses = ComponentStateTransitionConfig.class.getDeclaredClasses();
      final var classNames =
          java.util.Arrays.stream(nestedClasses)
              .map(Class::getSimpleName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(classNames.contains("TransitionValidator"), "Should have TransitionValidator");
      assertTrue(classNames.contains("TransitionInterceptor"), "Should have TransitionInterceptor");
      assertTrue(classNames.contains("TransitionGuard"), "Should have TransitionGuard");
      assertTrue(classNames.contains("TransitionAction"), "Should have TransitionAction");
      assertTrue(classNames.contains("TransitionContext"), "Should have TransitionContext");
      assertTrue(
          classNames.contains("TransitionTimeoutConfig"), "Should have TransitionTimeoutConfig");
      assertTrue(classNames.contains("TransitionRetryPolicy"), "Should have TransitionRetryPolicy");
      assertTrue(
          classNames.contains("TransitionRollbackConfig"), "Should have TransitionRollbackConfig");
      assertTrue(classNames.contains("TransitionAuditConfig"), "Should have TransitionAuditConfig");
      assertTrue(classNames.contains("ValidationResult"), "Should have ValidationResult");
      assertTrue(classNames.contains("ActionResult"), "Should have ActionResult");
      assertTrue(classNames.contains("RollbackStrategy"), "Should have RollbackStrategy");
      assertTrue(classNames.contains("RollbackContext"), "Should have RollbackContext");
      assertTrue(classNames.contains("RollbackResult"), "Should have RollbackResult");
      assertTrue(classNames.contains("FailureInfo"), "Should have FailureInfo");
      assertTrue(classNames.contains("AuditFilter"), "Should have AuditFilter");
    }

    @Test
    @DisplayName("should have all expected enums")
    void shouldHaveAllExpectedEnums() {
      final var nestedClasses = ComponentStateTransitionConfig.class.getDeclaredClasses();
      final var enumNames =
          java.util.Arrays.stream(nestedClasses)
              .filter(Class::isEnum)
              .map(Class::getSimpleName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(enumNames.contains("ComponentState"), "Should have ComponentState enum");
      assertTrue(enumNames.contains("TransitionTrigger"), "Should have TransitionTrigger enum");
      assertTrue(enumNames.contains("ActionType"), "Should have ActionType enum");
      assertTrue(enumNames.contains("AuditLevel"), "Should have AuditLevel enum");
    }
  }
}
