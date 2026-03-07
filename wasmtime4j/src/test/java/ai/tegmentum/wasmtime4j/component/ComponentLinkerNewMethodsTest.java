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
package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiPreview2Config;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfig;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the new query and configuration methods added to {@link ComponentLinker}.
 *
 * <p>Tests verify the interface contracts for: {@code isWasiP2Enabled()}, {@code
 * isWasiHttpEnabled()}, {@code getHostFunctionCount()}, {@code getInterfaceCount()}, {@code
 * setAsyncSupport(boolean)}, and {@code setWasiMaxRandomSize(long)}.
 *
 * <p>Uses a stateful stub implementation to verify that methods can be called and values tracked.
 */
@DisplayName("ComponentLinker New Query/Config Methods Tests")
class ComponentLinkerNewMethodsTest {

  @Nested
  @DisplayName("isWasiP2Enabled()")
  class IsWasiP2EnabledTests {

    @Test
    @DisplayName("should return false when WASI P2 not enabled")
    void shouldReturnFalseByDefault() {
      final StatefulStubLinker linker = new StatefulStubLinker();
      assertFalse(linker.isWasiP2Enabled(), "Should be false before enableWasiPreview2()");
    }

    @Test
    @DisplayName("should return true after enableWasiPreview2()")
    void shouldReturnTrueAfterEnable() {
      final StatefulStubLinker linker = new StatefulStubLinker();
      linker.enableWasiPreview2();
      assertTrue(linker.isWasiP2Enabled(), "Should be true after enableWasiPreview2()");
    }

    @Test
    @DisplayName("should return true after enableWasiPreview2(config)")
    void shouldReturnTrueAfterEnableWithConfig() {
      final StatefulStubLinker linker = new StatefulStubLinker();
      linker.enableWasiPreview2(WasiPreview2Config.builder().build());
      assertTrue(linker.isWasiP2Enabled(), "Should be true after enableWasiPreview2(config)");
    }
  }

  @Nested
  @DisplayName("isWasiHttpEnabled()")
  class IsWasiHttpEnabledTests {

    @Test
    @DisplayName("should return false when WASI HTTP not enabled")
    void shouldReturnFalseByDefault() {
      final StatefulStubLinker linker = new StatefulStubLinker();
      assertFalse(linker.isWasiHttpEnabled(), "Should be false before enableWasiHttp()");
    }

    @Test
    @DisplayName("should return true after enableWasiHttp()")
    void shouldReturnTrueAfterEnable() {
      final StatefulStubLinker linker = new StatefulStubLinker();
      linker.enableWasiHttp();
      assertTrue(linker.isWasiHttpEnabled(), "Should be true after enableWasiHttp()");
    }
  }

  @Nested
  @DisplayName("getHostFunctionCount()")
  class GetHostFunctionCountTests {

    @Test
    @DisplayName("should return 0 when no host functions defined")
    void shouldReturnZeroByDefault() {
      final StatefulStubLinker linker = new StatefulStubLinker();
      assertEquals(0, linker.getHostFunctionCount(), "Should be 0 before any definitions");
    }

    @Test
    @DisplayName("should increment after defining host function")
    void shouldIncrementAfterDefine() throws WasmException {
      final StatefulStubLinker linker = new StatefulStubLinker();
      linker.defineFunction("ns", "iface", "func1", params -> Collections.emptyList());
      assertEquals(1, linker.getHostFunctionCount(), "Should be 1 after one definition");

      linker.defineFunction("ns", "iface", "func2", params -> Collections.emptyList());
      assertEquals(2, linker.getHostFunctionCount(), "Should be 2 after two definitions");
    }
  }

  @Nested
  @DisplayName("getInterfaceCount()")
  class GetInterfaceCountTests {

    @Test
    @DisplayName("should return 0 when no interfaces defined")
    void shouldReturnZeroByDefault() {
      final StatefulStubLinker linker = new StatefulStubLinker();
      assertEquals(0, linker.getInterfaceCount(), "Should be 0 before any definitions");
    }

    @Test
    @DisplayName("should count distinct interfaces")
    void shouldCountDistinctInterfaces() throws WasmException {
      final StatefulStubLinker linker = new StatefulStubLinker();
      linker.defineFunction("ns1", "iface1", "func1", params -> Collections.emptyList());
      assertEquals(1, linker.getInterfaceCount(), "Should be 1 after one interface");

      linker.defineFunction("ns1", "iface1", "func2", params -> Collections.emptyList());
      assertEquals(
          1, linker.getInterfaceCount(), "Should still be 1 - same interface, different function");

      linker.defineFunction("ns2", "iface2", "func1", params -> Collections.emptyList());
      assertEquals(2, linker.getInterfaceCount(), "Should be 2 after second interface");
    }
  }

  @Nested
  @DisplayName("setAsyncSupport()")
  class SetAsyncSupportTests {

    @Test
    @DisplayName("should accept true without throwing")
    void shouldAcceptTrue() throws WasmException {
      final StatefulStubLinker linker = new StatefulStubLinker();
      linker.setAsyncSupport(true);
      assertTrue(linker.asyncEnabled, "Async should be enabled");
    }

    @Test
    @DisplayName("should accept false without throwing")
    void shouldAcceptFalse() throws WasmException {
      final StatefulStubLinker linker = new StatefulStubLinker();
      linker.setAsyncSupport(true);
      linker.setAsyncSupport(false);
      assertFalse(linker.asyncEnabled, "Async should be disabled");
    }
  }

  @Nested
  @DisplayName("setWasiMaxRandomSize()")
  class SetWasiMaxRandomSizeTests {

    @Test
    @DisplayName("should accept zero value")
    void shouldAcceptZero() throws WasmException {
      final StatefulStubLinker linker = new StatefulStubLinker();
      linker.setWasiMaxRandomSize(0);
      assertEquals(0L, linker.maxRandomSize, "Max random size should be 0");
    }

    @Test
    @DisplayName("should accept positive value")
    void shouldAcceptPositiveValue() throws WasmException {
      final StatefulStubLinker linker = new StatefulStubLinker();
      linker.setWasiMaxRandomSize(4096);
      assertEquals(4096L, linker.maxRandomSize, "Max random size should be 4096");
    }

    @Test
    @DisplayName("should accept max long value")
    void shouldAcceptMaxLong() throws WasmException {
      final StatefulStubLinker linker = new StatefulStubLinker();
      linker.setWasiMaxRandomSize(Long.MAX_VALUE);
      assertEquals(
          Long.MAX_VALUE, linker.maxRandomSize, "Max random size should be Long.MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("getDefinedInterfaces()")
  class GetDefinedInterfacesTests {

    @Test
    @DisplayName("should return empty set initially")
    void shouldReturnEmptyInitially() {
      final StatefulStubLinker linker = new StatefulStubLinker();
      assertTrue(linker.getDefinedInterfaces().isEmpty(), "Should be empty initially");
    }

    @Test
    @DisplayName("should track defined interfaces after defineFunction")
    void shouldTrackInterfaces() throws WasmException {
      final StatefulStubLinker linker = new StatefulStubLinker();
      linker.defineFunction("ns", "iface", "func", params -> Collections.emptyList());

      final Set<String> interfaces = linker.getDefinedInterfaces();
      assertEquals(1, interfaces.size(), "Should have 1 interface");
      assertTrue(interfaces.contains("ns:iface"), "Should contain 'ns:iface'");
    }
  }

  @Nested
  @DisplayName("getDefinedFunctions()")
  class GetDefinedFunctionsTests {

    @Test
    @DisplayName("should throw on null namespace")
    void shouldThrowOnNullNamespace() {
      final StatefulStubLinker linker = new StatefulStubLinker();
      assertThrows(
          IllegalArgumentException.class,
          () -> linker.getDefinedFunctions(null, "iface"),
          "Should throw on null namespace");
    }

    @Test
    @DisplayName("should throw on null interface name")
    void shouldThrowOnNullInterfaceName() {
      final StatefulStubLinker linker = new StatefulStubLinker();
      assertThrows(
          IllegalArgumentException.class,
          () -> linker.getDefinedFunctions("ns", null),
          "Should throw on null interface name");
    }

    @Test
    @DisplayName("should return empty set for undefined interface")
    void shouldReturnEmptyForUndefined() {
      final StatefulStubLinker linker = new StatefulStubLinker();
      final Set<String> functions = linker.getDefinedFunctions("ns", "iface");
      assertTrue(functions.isEmpty(), "Should be empty for undefined interface");
    }

    @Test
    @DisplayName("should return functions for defined interface")
    void shouldReturnFunctionsForDefined() throws WasmException {
      final StatefulStubLinker linker = new StatefulStubLinker();
      linker.defineFunction("ns", "iface", "func1", params -> Collections.emptyList());
      linker.defineFunction("ns", "iface", "func2", params -> Collections.emptyList());

      final Set<String> functions = linker.getDefinedFunctions("ns", "iface");
      assertEquals(2, functions.size(), "Should have 2 functions");
      assertTrue(functions.contains("func1"), "Should contain func1");
      assertTrue(functions.contains("func2"), "Should contain func2");
    }
  }

  /**
   * Stateful stub that tracks state for testing the new interface methods. Unlike the minimal
   * StubComponentLinker, this one actually records calls so we can verify behavior.
   */
  @SuppressWarnings("unchecked")
  private static class StatefulStubLinker implements ComponentLinker<Object> {

    boolean wasiP2Enabled;
    boolean wasiHttpEnabled;
    boolean asyncEnabled;
    long maxRandomSize = -1;
    int hostFunctionCount;
    final java.util.concurrent.ConcurrentHashMap<String, Set<String>> definedInterfaces =
        new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void defineFunction(
        final String interfaceNamespace,
        final String interfaceName,
        final String functionName,
        final ComponentHostFunction implementation)
        throws WasmException {
      final String key = interfaceNamespace + ":" + interfaceName;
      definedInterfaces
          .computeIfAbsent(key, k -> java.util.concurrent.ConcurrentHashMap.newKeySet())
          .add(functionName);
      hostFunctionCount++;
    }

    @Override
    public void defineFunction(final String witPath, final ComponentHostFunction implementation)
        throws WasmException {}

    @Override
    public void defineFunctionAsync(
        final String interfaceNamespace,
        final String interfaceName,
        final String functionName,
        final ComponentHostFunction implementation)
        throws WasmException {}

    @Override
    public void defineFunctionAsync(
        final String witPath, final ComponentHostFunction implementation) throws WasmException {}

    @Override
    public void defineInterface(
        final String interfaceNamespace,
        final String interfaceName,
        final Map<String, ComponentHostFunction> functions)
        throws WasmException {}

    @Override
    public void defineResource(
        final String interfaceNamespace,
        final String interfaceName,
        final String resourceName,
        final ComponentResourceDefinition<?> resourceDefinition)
        throws WasmException {}

    @Override
    public void defineModule(
        final String instancePath, final String name, final ai.tegmentum.wasmtime4j.Module module)
        throws WasmException {}

    @Override
    public void linkInstance(final ComponentInstance instance) {}

    @Override
    public ComponentInstance linkComponent(final Store store, final Component component) {
      return null;
    }

    @Override
    public ComponentInstance instantiate(final Store store, final Component component) {
      return null;
    }

    @Override
    public ComponentInstancePre instantiatePre(final Component component) {
      return null;
    }

    @Override
    public void enableWasiPreview2() {
      wasiP2Enabled = true;
    }

    @Override
    public void enableWasiPreview2(final WasiPreview2Config config) {
      wasiP2Enabled = true;
    }

    @Override
    public void enableWasiHttp() {
      wasiHttpEnabled = true;
    }

    @Override
    public void enableWasiHttp(final WasiHttpConfig config) {
      wasiHttpEnabled = true;
    }

    @Override
    public void enableWasiConfig() {}

    @Override
    public void setConfigVariables(final Map<String, String> variables) {}

    @Override
    public boolean isWasiP2Enabled() {
      return wasiP2Enabled;
    }

    @Override
    public boolean isWasiHttpEnabled() {
      return wasiHttpEnabled;
    }

    @Override
    public int getHostFunctionCount() {
      return hostFunctionCount;
    }

    @Override
    public int getInterfaceCount() {
      return definedInterfaces.size();
    }

    @Override
    public void setAsyncSupport(final boolean enabled) {
      asyncEnabled = enabled;
    }

    @Override
    public void setWasiMaxRandomSize(final long maxSize) {
      if (maxSize < 0) {
        throw new IllegalArgumentException("maxSize cannot be negative");
      }
      maxRandomSize = maxSize;
    }

    @Override
    public Engine getEngine() {
      return null;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public boolean hasInterface(final String interfaceNamespace, final String interfaceName) {
      return definedInterfaces.containsKey(interfaceNamespace + ":" + interfaceName);
    }

    @Override
    public boolean hasFunction(
        final String interfaceNamespace, final String interfaceName, final String functionName) {
      final Set<String> funcs = definedInterfaces.get(interfaceNamespace + ":" + interfaceName);
      return funcs != null && funcs.contains(functionName);
    }

    @Override
    public Set<String> getDefinedInterfaces() {
      return Collections.unmodifiableSet(new java.util.HashSet<>(definedInterfaces.keySet()));
    }

    @Override
    public Set<String> getDefinedFunctions(
        final String interfaceNamespace, final String interfaceName) {
      if (interfaceNamespace == null) {
        throw new IllegalArgumentException("Interface namespace cannot be null");
      }
      if (interfaceName == null) {
        throw new IllegalArgumentException("Interface name cannot be null");
      }
      final String key = interfaceNamespace + ":" + interfaceName;
      final Set<String> funcs = definedInterfaces.get(key);
      return funcs != null
          ? Collections.unmodifiableSet(new java.util.HashSet<>(funcs))
          : Collections.emptySet();
    }

    @Override
    public void aliasInterface(
        final String fromNamespace,
        final String fromInterface,
        final String toNamespace,
        final String toInterface) {}

    @Override
    public void allowShadowing(final boolean allow) {}

    @Override
    public void defineUnknownImportsAsTraps(final Component component) {}

    @Override
    public void close() {}
  }
}
