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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Input validation tests for JniComponentLinker.
 *
 * <p>These tests exercise real pre-native-call validation code paths: null checks, zero/negative
 * handle rejection, and resource type verification. All validation happens in Java before any
 * native call is made.
 *
 * <p>These tests use fake handles (which never reach native code) because the validation logic
 * under test runs entirely in Java. This is intentional — the goal is to verify that invalid inputs
 * are rejected before crossing the JNI boundary.
 */
@DisplayName("JniComponentLinker Validation Tests")
final class JniComponentLinkerValidationTest {

  private static final long VALID_HANDLE = 0x12345678L;

  private JniEngine testEngine;
  private JniComponentLinker<?> linker;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
    linker = new JniComponentLinker<>(VALID_HANDLE, testEngine);
  }

  @AfterEach
  void tearDown() {
    linker.markClosedForTesting();
    testEngine.markClosedForTesting();
  }

  @Nested
  @DisplayName("Constructor Handle Validation")
  class ConstructorHandleValidation {

    @Test
    @DisplayName("Zero handle should be rejected as null pointer")
    void zeroHandleShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> new JniComponentLinker<>(0L, testEngine));
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
              IllegalArgumentException.class, () -> new JniComponentLinker<>(-1L, testEngine));
      assertTrue(
          e.getMessage().contains("nativeHandle"), "Expected message to contain: nativeHandle");
      assertTrue(
          e.getMessage().contains("invalid native handle"),
          "Expected message to contain: invalid native handle");
      assertTrue(
          e.getMessage().contains("negative value"), "Expected message to contain: negative value");
    }
  }

  @Nested
  @DisplayName("getEngine")
  class GetEngine {

    @Test
    @DisplayName("Should return the engine passed to constructor")
    void shouldReturnCorrectEngine() {
      assertEquals(testEngine, linker.getEngine());
    }
  }

  @Nested
  @DisplayName("Resource Type")
  class ResourceType {

    @Test
    @DisplayName("getResourceType should return ComponentLinker")
    void shouldReturnComponentLinker() {
      assertEquals("ComponentLinker", linker.getResourceType());
    }
  }

  @Nested
  @DisplayName("isValid")
  class IsValid {

    @Test
    @DisplayName("Should return true for a valid linker")
    void shouldReturnTrueForValidLinker() {
      assertTrue(linker.isValid());
    }

    @Test
    @DisplayName("Should return false after linker is marked closed")
    void shouldReturnFalseAfterClose() {
      JniComponentLinker<?> localLinker = new JniComponentLinker<>(VALID_HANDLE, testEngine);
      localLinker.markClosedForTesting();
      assertFalse(localLinker.isValid());
    }
  }

  @Nested
  @DisplayName("defineFunction (4-param) Null Parameter Validation")
  class DefineFunctionFourParamNullValidation {

    @Test
    @DisplayName("Should reject null interfaceNamespace")
    void shouldRejectNullNamespace() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineFunction(null, "iface", "func", params -> params));
      assertTrue(
          e.getMessage().contains("Interface namespace cannot be null"),
          "Expected message about null namespace");
    }

    @Test
    @DisplayName("Should reject null interfaceName")
    void shouldRejectNullInterfaceName() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineFunction("ns", null, "func", params -> params));
      assertTrue(
          e.getMessage().contains("Interface name cannot be null"),
          "Expected message about null interface name");
    }

    @Test
    @DisplayName("Should reject null functionName")
    void shouldRejectNullFunctionName() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineFunction("ns", "iface", null, params -> params));
      assertTrue(
          e.getMessage().contains("Function name cannot be null"),
          "Expected message about null function name");
    }

    @Test
    @DisplayName("Should reject null implementation")
    void shouldRejectNullImplementation() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineFunction("ns", "iface", "func", null));
      assertTrue(
          e.getMessage().contains("Implementation cannot be null"),
          "Expected message about null implementation");
    }
  }

  @Nested
  @DisplayName("defineFunction (2-param) Null Parameter Validation")
  class DefineFunctionTwoParamNullValidation {

    @Test
    @DisplayName("Should reject null witPath")
    void shouldRejectNullWitPath() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> linker.defineFunction(null, params -> params));
      assertTrue(
          e.getMessage().contains("WIT path cannot be null"),
          "Expected message about null WIT path");
    }

    @Test
    @DisplayName("Should reject null implementation")
    void shouldRejectNullImplementation() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> linker.defineFunction("ns/iface#func", null));
      assertTrue(
          e.getMessage().contains("Implementation cannot be null"),
          "Expected message about null implementation");
    }
  }

  @Nested
  @DisplayName("defineFunctionAsync (4-param) Null Parameter Validation")
  class DefineFunctionAsyncFourParamNullValidation {

    @Test
    @DisplayName("Should reject null interfaceNamespace")
    void shouldRejectNullNamespace() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineFunctionAsync(null, "iface", "func", params -> params));
      assertTrue(
          e.getMessage().contains("Interface namespace cannot be null"),
          "Expected message about null namespace");
    }

    @Test
    @DisplayName("Should reject null interfaceName")
    void shouldRejectNullInterfaceName() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineFunctionAsync("ns", null, "func", params -> params));
      assertTrue(
          e.getMessage().contains("Interface name cannot be null"),
          "Expected message about null interface name");
    }

    @Test
    @DisplayName("Should reject null functionName")
    void shouldRejectNullFunctionName() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineFunctionAsync("ns", "iface", null, params -> params));
      assertTrue(
          e.getMessage().contains("Function name cannot be null"),
          "Expected message about null function name");
    }

    @Test
    @DisplayName("Should reject null implementation")
    void shouldRejectNullImplementation() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineFunctionAsync("ns", "iface", "func", null));
      assertTrue(
          e.getMessage().contains("Implementation cannot be null"),
          "Expected message about null implementation");
    }
  }

  @Nested
  @DisplayName("defineFunctionAsync (2-param) Null Parameter Validation")
  class DefineFunctionAsyncTwoParamNullValidation {

    @Test
    @DisplayName("Should reject null witPath")
    void shouldRejectNullWitPath() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineFunctionAsync(null, params -> params));
      assertTrue(
          e.getMessage().contains("WIT path cannot be null"),
          "Expected message about null WIT path");
    }

    @Test
    @DisplayName("Should reject null implementation")
    void shouldRejectNullImplementation() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineFunctionAsync("ns/iface#func", null));
      assertTrue(
          e.getMessage().contains("Implementation cannot be null"),
          "Expected message about null implementation");
    }
  }

  @Nested
  @DisplayName("defineInterface Null Parameter Validation")
  class DefineInterfaceNullValidation {

    @Test
    @DisplayName("Should reject null interfaceNamespace")
    void shouldRejectNullNamespace() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineInterface(null, "iface", java.util.Collections.emptyMap()));
      assertTrue(
          e.getMessage().contains("Interface namespace cannot be null"),
          "Expected message about null namespace");
    }

    @Test
    @DisplayName("Should reject null interfaceName")
    void shouldRejectNullInterfaceName() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineInterface("ns", null, java.util.Collections.emptyMap()));
      assertTrue(
          e.getMessage().contains("Interface name cannot be null"),
          "Expected message about null interface name");
    }

    @Test
    @DisplayName("Should reject null functions map")
    void shouldRejectNullFunctions() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> linker.defineInterface("ns", "iface", null));
      assertTrue(
          e.getMessage().contains("Functions cannot be null"),
          "Expected message about null functions");
    }
  }

  @Nested
  @DisplayName("defineResource Null Parameter Validation")
  class DefineResourceNullValidation {

    @Test
    @DisplayName("Should reject null interfaceNamespace")
    void shouldRejectNullNamespace() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineResource(null, "iface", "res", null));
      assertTrue(
          e.getMessage().contains("Interface namespace cannot be null"),
          "Expected message about null namespace");
    }

    @Test
    @DisplayName("Should reject null interfaceName")
    void shouldRejectNullInterfaceName() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> linker.defineResource("ns", null, "res", null));
      assertTrue(
          e.getMessage().contains("Interface name cannot be null"),
          "Expected message about null interface name");
    }

    @Test
    @DisplayName("Should reject null resourceName")
    void shouldRejectNullResourceName() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineResource("ns", "iface", null, null));
      assertTrue(
          e.getMessage().contains("Resource name cannot be null"),
          "Expected message about null resource name");
    }

    @Test
    @DisplayName("Should reject null resourceDefinition")
    void shouldRejectNullResourceDefinition() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineResource("ns", "iface", "res", null));
      assertTrue(
          e.getMessage().contains("Resource definition cannot be null"),
          "Expected message about null resource definition");
    }
  }

  @Nested
  @DisplayName("defineModule Null Parameter Validation")
  class DefineModuleNullValidation {

    @Test
    @DisplayName("Should reject null instancePath")
    void shouldRejectNullInstancePath() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> linker.defineModule(null, "name", null));
      assertTrue(
          e.getMessage().contains("Instance path cannot be null"),
          "Expected message about null instance path");
    }

    @Test
    @DisplayName("Should reject null name")
    void shouldRejectNullName() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> linker.defineModule("path", null, null));
      assertTrue(
          e.getMessage().contains("Name cannot be null"), "Expected message about null name");
    }

    @Test
    @DisplayName("Should reject empty name")
    void shouldRejectEmptyName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> linker.defineModule("path", "", null));
      assertTrue(
          e.getMessage().contains("Name cannot be null or empty"),
          "Expected message about empty name");
    }

    @Test
    @DisplayName("Should reject null module")
    void shouldRejectNullModule() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> linker.defineModule("path", "name", null));
      assertTrue(
          e.getMessage().contains("Module cannot be null"), "Expected message about null module");
    }
  }

  @Nested
  @DisplayName("linkInstance Null Parameter Validation")
  class LinkInstanceNullValidation {

    @Test
    @DisplayName("Should reject null instance")
    void shouldRejectNullInstance() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> linker.linkInstance(null));
      assertTrue(
          e.getMessage().contains("Instance cannot be null"),
          "Expected message about null instance");
    }
  }

  @Nested
  @DisplayName("linkComponent Null Parameter Validation")
  class LinkComponentNullValidation {

    @Test
    @DisplayName("Should reject null store")
    void shouldRejectNullStore() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> linker.linkComponent(null, null));
      assertTrue(
          e.getMessage().contains("Store cannot be null"), "Expected message about null store");
    }

    @Test
    @DisplayName("Should reject null component")
    void shouldRejectNullComponent() {
      JniStore store = new JniStore(VALID_HANDLE, testEngine);
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> linker.linkComponent(store, null));
      assertTrue(
          e.getMessage().contains("Component cannot be null"),
          "Expected message about null component");
      store.markClosedForTesting();
    }
  }

  @Nested
  @DisplayName("instantiate Null Parameter Validation")
  class InstantiateNullValidation {

    @Test
    @DisplayName("Should reject null store")
    void shouldRejectNullStore() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> linker.instantiate(null, null));
      assertTrue(
          e.getMessage().contains("Store cannot be null"), "Expected message about null store");
    }

    @Test
    @DisplayName("Should reject null component")
    void shouldRejectNullComponent() {
      JniStore store = new JniStore(VALID_HANDLE, testEngine);
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> linker.instantiate(store, null));
      assertTrue(
          e.getMessage().contains("Component cannot be null"),
          "Expected message about null component");
      store.markClosedForTesting();
    }
  }

  @Nested
  @DisplayName("instantiatePre Null Parameter Validation")
  class InstantiatePreNullValidation {

    @Test
    @DisplayName("Should reject null component")
    void shouldRejectNullComponent() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> linker.instantiatePre(null));
      assertTrue(
          e.getMessage().contains("Component cannot be null"),
          "Expected message about null component");
    }
  }

  @Nested
  @DisplayName("enableWasiPreview2 Config Null Validation")
  class EnableWasiPreview2ConfigNullValidation {

    @Test
    @DisplayName("Should reject null config")
    void shouldRejectNullConfig() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> linker.enableWasiPreview2(null));
      assertTrue(
          e.getMessage().contains("Config cannot be null"), "Expected message about null config");
    }
  }

  @Nested
  @DisplayName("enableWasiHttp Config Null Validation")
  class EnableWasiHttpConfigNullValidation {

    @Test
    @DisplayName("Should reject null config")
    void shouldRejectNullConfig() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> linker.enableWasiHttp(null));
      assertTrue(
          e.getMessage().contains("Config cannot be null"), "Expected message about null config");
    }
  }

  @Nested
  @DisplayName("setConfigVariables Null Validation")
  class SetConfigVariablesNullValidation {

    @Test
    @DisplayName("Should reject null variables")
    void shouldRejectNullVariables() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> linker.setConfigVariables(null));
      assertTrue(
          e.getMessage().contains("Variables cannot be null"),
          "Expected message about null variables");
    }
  }

  @Nested
  @DisplayName("hasInterface Null Parameter Validation")
  class HasInterfaceNullValidation {

    @Test
    @DisplayName("Should reject null interfaceNamespace")
    void shouldRejectNullNamespace() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> linker.hasInterface(null, "iface"));
      assertTrue(
          e.getMessage().contains("Interface namespace cannot be null"),
          "Expected message about null namespace");
    }

    @Test
    @DisplayName("Should reject null interfaceName")
    void shouldRejectNullInterfaceName() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> linker.hasInterface("ns", null));
      assertTrue(
          e.getMessage().contains("Interface name cannot be null"),
          "Expected message about null interface name");
    }
  }

  @Nested
  @DisplayName("hasFunction Null Parameter Validation")
  class HasFunctionNullValidation {

    @Test
    @DisplayName("Should reject null interfaceNamespace")
    void shouldRejectNullNamespace() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> linker.hasFunction(null, "iface", "func"));
      assertTrue(
          e.getMessage().contains("Interface namespace cannot be null"),
          "Expected message about null namespace");
    }

    @Test
    @DisplayName("Should reject null interfaceName")
    void shouldRejectNullInterfaceName() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> linker.hasFunction("ns", null, "func"));
      assertTrue(
          e.getMessage().contains("Interface name cannot be null"),
          "Expected message about null interface name");
    }

    @Test
    @DisplayName("Should reject null functionName")
    void shouldRejectNullFunctionName() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> linker.hasFunction("ns", "iface", null));
      assertTrue(
          e.getMessage().contains("Function name cannot be null"),
          "Expected message about null function name");
    }
  }

  @Nested
  @DisplayName("getDefinedFunctions Null Parameter Validation")
  class GetDefinedFunctionsNullValidation {

    @Test
    @DisplayName("Should reject null interfaceNamespace")
    void shouldRejectNullNamespace() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> linker.getDefinedFunctions(null, "iface"));
      assertTrue(
          e.getMessage().contains("Interface namespace cannot be null"),
          "Expected message about null namespace");
    }

    @Test
    @DisplayName("Should reject null interfaceName")
    void shouldRejectNullInterfaceName() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> linker.getDefinedFunctions("ns", null));
      assertTrue(
          e.getMessage().contains("Interface name cannot be null"),
          "Expected message about null interface name");
    }
  }

  @Nested
  @DisplayName("aliasInterface Null Parameter Validation")
  class AliasInterfaceNullValidation {

    @Test
    @DisplayName("Should reject null fromNamespace")
    void shouldRejectNullFromNamespace() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.aliasInterface(null, "fromIface", "toNs", "toIface"));
      assertTrue(
          e.getMessage().contains("From namespace cannot be null"),
          "Expected message about null from namespace");
    }

    @Test
    @DisplayName("Should reject null fromInterface")
    void shouldRejectNullFromInterface() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.aliasInterface("fromNs", null, "toNs", "toIface"));
      assertTrue(
          e.getMessage().contains("From interface cannot be null"),
          "Expected message about null from interface");
    }

    @Test
    @DisplayName("Should reject null toNamespace")
    void shouldRejectNullToNamespace() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.aliasInterface("fromNs", "fromIface", null, "toIface"));
      assertTrue(
          e.getMessage().contains("To namespace cannot be null"),
          "Expected message about null to namespace");
    }

    @Test
    @DisplayName("Should reject null toInterface")
    void shouldRejectNullToInterface() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.aliasInterface("fromNs", "fromIface", "toNs", null));
      assertTrue(
          e.getMessage().contains("To interface cannot be null"),
          "Expected message about null to interface");
    }
  }

  @Nested
  @DisplayName("defineUnknownImportsAsTraps Null Parameter Validation")
  class DefineUnknownImportsAsTrapsNullValidation {

    @Test
    @DisplayName("Should reject null component")
    void shouldRejectNullComponent() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> linker.defineUnknownImportsAsTraps(null));
      assertTrue(
          e.getMessage().contains("Component cannot be null"),
          "Expected message about null component");
    }
  }

  @Nested
  @DisplayName("substitutedComponentType Null Parameter Validation")
  class SubstitutedComponentTypeNullValidation {

    @Test
    @DisplayName("Should reject null component")
    void shouldRejectNullComponent() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> linker.substitutedComponentType(null));
      assertTrue(
          e.getMessage().contains("Component cannot be null"),
          "Expected message about null component");
    }
  }

  @Nested
  @DisplayName("setWasiMaxRandomSize Validation")
  class SetWasiMaxRandomSizeValidation {

    @Test
    @DisplayName("Should reject negative maxSize")
    void shouldRejectNegativeMaxSize() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> linker.setWasiMaxRandomSize(-1));
      assertTrue(
          e.getMessage().contains("maxSize cannot be negative"),
          "Expected message about negative maxSize");
    }
  }
}
