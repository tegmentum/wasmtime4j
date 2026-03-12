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
package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Input validation tests for JniComponentImpl.
 *
 * <p>These tests exercise real pre-native-call validation code paths: null checks and zero/negative
 * handle rejection. All validation happens in Java before any native call is made.
 *
 * <p>These tests use fake handles (which never reach native code) because the validation logic
 * under test runs entirely in Java. This is intentional — the goal is to verify that invalid inputs
 * are rejected before crossing the JNI boundary.
 *
 * <p>Note: Tests requiring a valid JniComponentImpl instance (getEngine, isValid, method parameter
 * validation) are not included here because the outer JniComponentEngine class requires native
 * library initialization in its constructor. Those validations are covered by integration tests.
 */
@DisplayName("JniComponentImpl Validation Tests")
final class JniComponentImplTest {

  private static final long VALID_HANDLE = 0x12345678L;

  private JniComponent.JniComponentHandle componentHandle;

  @BeforeEach
  void setUp() {
    componentHandle = new JniComponent.JniComponentHandle(VALID_HANDLE);
  }

  @AfterEach
  void tearDown() {
    componentHandle.markClosedForTesting();
  }

  @Nested
  @DisplayName("Constructor Validation")
  class ConstructorValidation {

    @Test
    @DisplayName("Should reject null nativeComponent")
    void shouldRejectNullNativeComponent() {
      // engine parameter is irrelevant here — validation rejects null nativeComponent first
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> new JniComponentImpl(null, null));
      assertTrue(
          e.getMessage().contains("nativeComponent"),
          "Expected message to contain: nativeComponent");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }

    @Test
    @DisplayName("Should reject null engine when nativeComponent is valid")
    void shouldRejectNullEngine() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> new JniComponentImpl(componentHandle, null));
      assertTrue(e.getMessage().contains("engine"), "Expected message to contain: engine");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }
  }

  @Nested
  @DisplayName("JniComponentHandle Validation")
  class ComponentHandleValidation {

    @Test
    @DisplayName("Zero handle should be rejected as null pointer")
    void zeroHandleShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> new JniComponent.JniComponentHandle(0L));
      assertTrue(
          e.getMessage().contains("nativeHandle"), "Expected message to contain: nativeHandle");
      assertTrue(
          e.getMessage().contains("invalid native handle"),
          "Expected message to contain: invalid native handle");
      assertTrue(
          e.getMessage().contains("null pointer"), "Expected message to contain: null pointer");
    }

    @Test
    @DisplayName("Negative handle should be rejected as invalid")
    void negativeHandleShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> new JniComponent.JniComponentHandle(-1L));
      assertTrue(
          e.getMessage().contains("nativeHandle"), "Expected message to contain: nativeHandle");
      assertTrue(
          e.getMessage().contains("invalid native handle"),
          "Expected message to contain: invalid native handle");
      assertTrue(
          e.getMessage().contains("negative value"), "Expected message to contain: negative value");
    }

    @Test
    @DisplayName("Valid handle should be accepted")
    void validHandleShouldBeAccepted() {
      JniComponent.JniComponentHandle handle = new JniComponent.JniComponentHandle(VALID_HANDLE);
      assertTrue(handle.isValid(), "Handle with valid value should report as valid");
      handle.markClosedForTesting();
    }
  }

  @Nested
  @DisplayName("JniComponentInstanceHandle Validation")
  class ComponentInstanceHandleValidation {

    @Test
    @DisplayName("Zero handle should be rejected as null pointer")
    void zeroHandleShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> new JniComponent.JniComponentInstanceHandle(0L));
      assertTrue(
          e.getMessage().contains("nativeHandle"), "Expected message to contain: nativeHandle");
      assertTrue(
          e.getMessage().contains("invalid native handle"),
          "Expected message to contain: invalid native handle");
      assertTrue(
          e.getMessage().contains("null pointer"), "Expected message to contain: null pointer");
    }

    @Test
    @DisplayName("Negative handle should be rejected as invalid")
    void negativeHandleShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> new JniComponent.JniComponentInstanceHandle(-1L));
      assertTrue(
          e.getMessage().contains("nativeHandle"), "Expected message to contain: nativeHandle");
      assertTrue(
          e.getMessage().contains("invalid native handle"),
          "Expected message to contain: invalid native handle");
      assertTrue(
          e.getMessage().contains("negative value"), "Expected message to contain: negative value");
    }

    @Test
    @DisplayName("Valid handle should be accepted")
    void validHandleShouldBeAccepted() {
      JniComponent.JniComponentInstanceHandle handle =
          new JniComponent.JniComponentInstanceHandle(VALID_HANDLE);
      assertTrue(handle.isValid(), "Handle with valid value should report as valid");
      handle.markClosedForTesting();
    }
  }

  @Nested
  @DisplayName("JniComponentEngine Inner Class Validation")
  class ComponentEngineHandleValidation {

    @Test
    @DisplayName("Zero handle should be rejected as null pointer")
    void zeroHandleShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> new JniComponent.JniComponentEngine(0L));
      assertTrue(
          e.getMessage().contains("nativeHandle"), "Expected message to contain: nativeHandle");
      assertTrue(
          e.getMessage().contains("invalid native handle"),
          "Expected message to contain: invalid native handle");
      assertTrue(
          e.getMessage().contains("null pointer"), "Expected message to contain: null pointer");
    }

    @Test
    @DisplayName("Negative handle should be rejected as invalid")
    void negativeHandleShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> new JniComponent.JniComponentEngine(-1L));
      assertTrue(
          e.getMessage().contains("nativeHandle"), "Expected message to contain: nativeHandle");
      assertTrue(
          e.getMessage().contains("invalid native handle"),
          "Expected message to contain: invalid native handle");
      assertTrue(
          e.getMessage().contains("negative value"), "Expected message to contain: negative value");
    }

    @Test
    @DisplayName("Valid handle should be accepted")
    void validHandleShouldBeAccepted() {
      JniComponent.JniComponentEngine engine = new JniComponent.JniComponentEngine(VALID_HANDLE);
      assertTrue(engine.isValid(), "Engine with valid handle should report as valid");
      engine.markClosedForTesting();
    }
  }
}
