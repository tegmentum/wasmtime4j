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
package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.Caller;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Positive-path runtime tests for Panama Caller&lt;T&gt; mutation methods delivered by
 * F-Wasmtime4j-Panama-Consumer-Gated-Followups r.2-r.4 (2026-07-28). This
 * suite is the r.4 slice of the follow-up wire-fix charter
 * F-Wasmtime4j-Panama-Callback-Caller-Wire (2026-07-28), which extended the
 * Panama callback ABI to carry a real {@code wasmtime::Caller<'_,
 * StoreData>*} instead of a store-address fallback.
 *
 * <p>Each test wires a wasm module that imports a host function which,
 * inside a live callback frame, exercises one of the caller-scoped mutation
 * APIs and asserts an observable side-effect after the callback returns.
 * Any assertion failure inside the callback is captured to an
 * {@link AtomicReference} and re-thrown post-return so JUnit reports the
 * failure.
 */
@DisplayName("Panama Caller<T> mutation — positive-path runtime")
final class PanamaCallerMutationRuntimeTest {

  private static Instance instantiate(
      final PanamaEngine engine,
      final PanamaLinker<Void> linker,
      final PanamaStore store,
      final String wat)
      throws WasmException {
    final PanamaModule module = (PanamaModule) engine.compileWat(wat);
    return linker.instantiate(store, module);
  }

  @Test
  @DisplayName("getMemory('memory') from callback returns valid PanamaMemory (validated-wrapper fix)")
  void getMemoryFromCallbackReturnsValidatedMemory() throws Exception {
    // F-Wasmtime4j-Panama-Memory-From-Caller-Wrapper-Fix (2026-07-28):
    // wasmtime4j_panama_caller_get_memory previously boxed a raw
    // wasmtime::Memory, but downstream panamaMemorySize* / Grow FFIs
    // deref-cast the ptr as *const ValidatedMemory (see memory/core.rs).
    // Fix wraps the returned Memory in Memory::from_wasmtime_memory +
    // create_validated_memory before boxing. This test proves the fix
    // by calling .size() and .grow() on the returned PanamaMemory
    // (both route through panamaMemorySizePages / panamaMemoryGrow).
    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaLinker<Void> linker = new PanamaLinker<>(engine);

    final String wat =
        "(module\n"
            + "  (import \"env\" \"probe\" (func $probe (result i32)))\n"
            + "  (memory (export \"memory\") 1)\n"
            + "  (func (export \"run\") (result i32) call $probe)\n"
            + ")";

    final AtomicReference<Throwable> failure = new AtomicReference<>();
    final AtomicReference<WasmMemory> capturedMemory = new AtomicReference<>();

    linker.defineHostFunction(
        "env",
        "probe",
        FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
        new HostFunction.CallerAwareHostFunction<Void>(
            (final Caller<Void> caller, final WasmValue[] params) -> {
              try {
                // Under fix: caller.getMemory returns a valid PanamaMemory
                // wrapping the caller's memory as a ValidatedMemory ptr.
                // Note: calling .size() / .grow() from within the callback
                // would hit wasmtime's store re-entrancy check; for callback-
                // scoped mutation use caller.growMemory(...). This test
                // instead captures the returned PanamaMemory and verifies
                // .size() / .grow() work AFTER callback return, which
                // exercises the ValidatedMemory wrapper end-to-end.
                final Optional<WasmMemory> memOpt = caller.getMemory("memory");
                assertTrue(memOpt.isPresent(), "caller.getMemory('memory') must succeed post-fix");
                capturedMemory.set(memOpt.get());
                return new WasmValue[] {WasmValue.i32(1)};
              } catch (final Throwable t) {
                failure.set(t);
                return new WasmValue[] {WasmValue.i32(-1)};
              }
            }));

    final Instance instance = instantiate(engine, linker, store, wat);
    final WasmValue[] results = instance.callFunction("run");

    if (failure.get() != null) {
      throw new AssertionError("Callback assertion failed", failure.get());
    }
    assertEquals(1, results[0].asInt());

    // Post-callback: exercise size + grow on the captured PanamaMemory.
    // This routes through panamaMemorySizePages / panamaMemoryGrow which
    // deref the ptr as *const ValidatedMemory — the fix's core validation.
    final WasmMemory captured = capturedMemory.get();
    assertNotNull(captured, "captured memory must survive callback exit");
    assertEquals(1, captured.size(), "initial memory size == 1 page");
    final int prev = captured.grow(1);
    assertEquals(1, prev, "grow(1) returns previous size == 1");
    assertEquals(2, captured.size(), "post-grow memory size == 2 pages");

    // Independent verification via instance.getMemory: same underlying memory.
    final Optional<WasmMemory> instanceMem = instance.getMemory("memory");
    assertTrue(instanceMem.isPresent());
    assertEquals(2L, instanceMem.get().size(), "instance sees the same grown memory");

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("caller.growMemory(caller.getMemory('memory'), 1) grows memory from within callback")
  void growMemoryFromWithinCallback() throws Exception {
    // Follow-up to F-Wasmtime4j-Panama-Memory-From-Caller-Wrapper-Fix:
    // exercises the caller-aware mutation path (caller.growMemory) from
    // WITHIN a callback frame, using the freshly-obtained PanamaMemory
    // from caller.getMemory. Proves:
    //   1. caller.getMemory returns a PanamaMemory whose nativeMemory
    //      ptr is a *const ValidatedMemory (r.4 fix).
    //   2. caller.growMemory extracts the inner wasmtime::Memory from
    //      that ValidatedMemory ptr (r.4 follow-up alignment of
    //      wasmtime4j_panama_caller_grow_memory) and uses
    //      caller.as_context_mut() — no store re-entrancy issue.
    //   3. Grow effect is observable post-callback via instance.getMemory.
    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaLinker<Void> linker = new PanamaLinker<>(engine);

    final String wat =
        "(module\n"
            + "  (import \"env\" \"grow\" (func $grow (result i32)))\n"
            + "  (memory (export \"memory\") 1)\n"
            + "  (func (export \"run\") (result i32) call $grow)\n"
            + ")";

    final AtomicReference<Throwable> failure = new AtomicReference<>();
    final AtomicLong observedPrev = new AtomicLong(-1L);

    linker.defineHostFunction(
        "env",
        "grow",
        FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
        new HostFunction.CallerAwareHostFunction<Void>(
            (final Caller<Void> caller, final WasmValue[] params) -> {
              try {
                final Optional<WasmMemory> memOpt = caller.getMemory("memory");
                assertTrue(memOpt.isPresent(), "caller.getMemory('memory') must succeed");
                // Grow via the caller-aware path — no store re-entrancy.
                final long prev = caller.growMemory(memOpt.get(), 1L);
                observedPrev.set(prev);
                return new WasmValue[] {WasmValue.i32((int) prev)};
              } catch (final Throwable t) {
                failure.set(t);
                return new WasmValue[] {WasmValue.i32(-1)};
              }
            }));

    final Instance instance = instantiate(engine, linker, store, wat);
    final WasmValue[] results = instance.callFunction("run");

    if (failure.get() != null) {
      throw new AssertionError("Callback assertion failed", failure.get());
    }
    assertEquals(1, results[0].asInt(), "callback returns prev pages = 1");
    assertEquals(1L, observedPrev.get(), "growMemory prev pages == 1");

    // Post-callback verification: memory is observably 2 pages.
    final Optional<WasmMemory> instanceMem = instance.getMemory("memory");
    assertTrue(instanceMem.isPresent());
    assertEquals(2L, instanceMem.get().size(), "post-callback memory size == 2 pages");

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("hasExport('memory') from callback returns true (caller-wire smoke)")
  void hasExportFromCallbackReturnsTrue() throws Exception {
    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaLinker<Void> linker = new PanamaLinker<>(engine);

    final String wat =
        "(module\n"
            + "  (import \"env\" \"probe\" (func $probe (result i32)))\n"
            + "  (memory (export \"memory\") 1)\n"
            + "  (func (export \"run\") (result i32) call $probe)\n"
            + ")";

    final AtomicReference<Throwable> failure = new AtomicReference<>();
    final AtomicLong observedHasExport = new AtomicLong(-1L);

    linker.defineHostFunction(
        "env",
        "probe",
        FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
        new HostFunction.CallerAwareHostFunction<Void>(
            (final Caller<Void> caller, final WasmValue[] params) -> {
              try {
                assertNotNull(caller, "Caller must be delivered to CallerAware callback");
                // hasExport routes: Java PanamaCaller -> Rust FFI
                // wasmtime4j_panama_caller_has_export -> wasmtime
                // Caller::get_export. Passing the ACTUAL wasmtime::Caller
                // ptr (not the pre-r.3 store-address fallback) is what
                // makes this return true for a real export.
                final boolean has = caller.hasExport("memory");
                observedHasExport.set(has ? 1L : 0L);
                assertTrue(has, "hasExport('memory') must be true from a real caller frame");
                return new WasmValue[] {WasmValue.i32(1)};
              } catch (final Throwable t) {
                failure.set(t);
                return new WasmValue[] {WasmValue.i32(-1)};
              }
            }));

    final Instance instance = instantiate(engine, linker, store, wat);
    final WasmValue[] results = instance.callFunction("run");

    if (failure.get() != null) {
      throw new AssertionError("Callback assertion failed", failure.get());
    }
    assertEquals(1, results[0].asInt(), "callback should report hasExport succeeded");
    assertEquals(
        1L, observedHasExport.get(), "hasExport('memory') must be true from callback frame");

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("writeMemory + readMemory from callback observe each other's effects")
  void writeMemoryFromCallbackVisibleAfterReturn() throws Exception {
    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaLinker<Void> linker = new PanamaLinker<>(engine);

    final String wat =
        "(module\n"
            + "  (import \"env\" \"stamp\" (func $stamp (result i32)))\n"
            + "  (memory (export \"memory\") 1)\n"
            + "  (func (export \"run\") (result i32) call $stamp)\n"
            + ")";

    final AtomicReference<Throwable> failure = new AtomicReference<>();
    final byte[] payload = {0x48, 0x69, 0x21}; // "Hi!"

    linker.defineHostFunction(
        "env",
        "stamp",
        FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
        new HostFunction.CallerAwareHostFunction<Void>(
            (final Caller<Void> caller, final WasmValue[] params) -> {
              try {
                caller.writeMemory("memory", 0x100L, payload);
                final byte[] roundTrip = caller.readMemory("memory", 0x100L, payload.length);
                assertEquals(
                    new String(payload),
                    new String(roundTrip),
                    "readMemory must observe writeMemory");
                return new WasmValue[] {WasmValue.i32(0)};
              } catch (final Throwable t) {
                failure.set(t);
                return new WasmValue[] {WasmValue.i32(-1)};
              }
            }));

    final Instance instance = instantiate(engine, linker, store, wat);
    final WasmValue[] results = instance.callFunction("run");

    if (failure.get() != null) {
      throw new AssertionError("Callback assertion failed", failure.get());
    }
    assertEquals(0, results[0].asInt());

    final byte[] readBack = new byte[payload.length];
    instance.getMemory("memory").get().readBytes((int) 0x100L, readBack, 0, payload.length);
    for (int i = 0; i < payload.length; i++) {
      assertEquals(
          payload[i], readBack[i], "byte " + i + " must survive post-callback (proves visibility)");
    }

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("compileModule from callback returns a real, instantiable Module")
  void compileModuleFromCallback() throws Exception {
    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaLinker<Void> linker = new PanamaLinker<>(engine);

    // Minimal wasm module bytes: (module (func (export "answer") (result i32) i32.const 42))
    // Precomputed to avoid needing WAT compilation from within the callback.
    final byte[] childWasm =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, // magic
          0x01, 0x00, 0x00, 0x00, // version
          0x01, 0x05, 0x01, 0x60, 0x00, 0x01, 0x7f, // type: () -> i32
          0x03, 0x02, 0x01, 0x00, // function
          0x07, 0x0a, 0x01, 0x06, 0x61, 0x6e, 0x73, 0x77, 0x65, 0x72, 0x00, 0x00, // export "answer"
          0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b, // code: i32.const 42; end
        };

    final String parentWat =
        "(module\n"
            + "  (import \"env\" \"kickoff\" (func $kickoff (result i32)))\n"
            + "  (func (export \"run\") (result i32) call $kickoff)\n"
            + ")";

    final AtomicReference<Throwable> failure = new AtomicReference<>();
    final AtomicReference<ai.tegmentum.wasmtime4j.Module> capturedModule = new AtomicReference<>();

    linker.defineHostFunction(
        "env",
        "kickoff",
        FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
        new HostFunction.CallerAwareHostFunction<Void>(
            (final Caller<Void> caller, final WasmValue[] params) -> {
              try {
                final ai.tegmentum.wasmtime4j.Module compiled = caller.compileModule(childWasm);
                assertNotNull(compiled, "caller.compileModule must return a Module");
                assertTrue(
                    compiled instanceof PanamaModule,
                    "caller.compileModule must return a PanamaModule (parity with tier)");
                capturedModule.set(compiled);
                return new WasmValue[] {WasmValue.i32(1)};
              } catch (final Throwable t) {
                failure.set(t);
                return new WasmValue[] {WasmValue.i32(-1)};
              }
            }));

    final Instance instance = instantiate(engine, linker, store, parentWat);
    final WasmValue[] results = instance.callFunction("run");

    if (failure.get() != null) {
      throw new AssertionError("Callback assertion failed", failure.get());
    }
    assertEquals(1, results[0].asInt());
    assertNotNull(capturedModule.get(), "captured module must survive callback exit");

    // Instantiate the callback-compiled module and verify it runs.
    final Instance childInstance = linker.instantiate(store, capturedModule.get());
    final WasmValue[] answer = childInstance.callFunction("answer");
    assertEquals(42, answer[0].asInt(), "child module's 'answer' should return 42");

    childInstance.close();
    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("caller.data() from callback returns the store's user data")
  void callerDataFromCallback() throws Exception {
    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    store.setData("panama-store-data-marker");
    final PanamaLinker<Void> linker = new PanamaLinker<>(engine);

    final String wat =
        "(module\n"
            + "  (import \"env\" \"read_data\" (func $read (result i32)))\n"
            + "  (func (export \"run\") (result i32) call $read)\n"
            + ")";

    final AtomicReference<Throwable> failure = new AtomicReference<>();
    final AtomicReference<Object> seenData = new AtomicReference<>();

    linker.defineHostFunction(
        "env",
        "read_data",
        FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
        new HostFunction.CallerAwareHostFunction<String>(
            (final Caller<String> caller, final WasmValue[] params) -> {
              try {
                final String data = caller.data();
                seenData.set(data);
                assertEquals(
                    "panama-store-data-marker",
                    data,
                    "caller.data() must return the store's user data across the callback wire");
                return new WasmValue[] {WasmValue.i32(data.length())};
              } catch (final Throwable t) {
                failure.set(t);
                return new WasmValue[] {WasmValue.i32(-1)};
              }
            }));

    final Instance instance = instantiate(engine, linker, store, wat);
    final WasmValue[] results = instance.callFunction("run");

    if (failure.get() != null) {
      throw new AssertionError("Callback assertion failed", failure.get());
    }
    assertEquals(
        "panama-store-data-marker".length(),
        results[0].asInt(),
        "callback must return the length of the store's user data");
    assertEquals("panama-store-data-marker", seenData.get());

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("Non-caller-aware host functions still receive no caller (backward-compat)")
  void nonCallerAwareRunsUnchanged() throws Exception {
    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaLinker<Void> linker = new PanamaLinker<>(engine);

    final String wat =
        "(module\n"
            + "  (import \"env\" \"add_one\" (func $a (param i32) (result i32)))\n"
            + "  (func (export \"run\") (param i32) (result i32) local.get 0 call $a)\n"
            + ")";

    linker.defineHostFunction(
        "env",
        "add_one",
        FunctionType.of(
            new WasmValueType[] {WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32}),
        (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt() + 1)});

    final Instance instance = instantiate(engine, linker, store, wat);
    final WasmValue[] results = instance.callFunction("run", WasmValue.i32(41));
    assertEquals(42, results[0].asInt(), "plain HostFunction path must still work end-to-end");

    // Verify caller-context is cleared post-run (no stale ThreadLocal state).
    assertNull(
        PanamaHostFunctionCallerContextProbe.currentCaller(),
        "CALLER_CONTEXT must be cleared after non-caller-aware callback");

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  /**
   * Probe class that exposes PanamaHostFunction's CALLER_CONTEXT for assertion. Placed here
   * (package-private accessor) since PanamaHostFunction's getCurrentCaller() throws when null.
   */
  private static final class PanamaHostFunctionCallerContextProbe {
    static Caller<?> currentCaller() {
      try {
        return PanamaHostFunction.getCurrentCaller();
      } catch (final UnsupportedOperationException ignored) {
        return null;
      }
    }
  }
}
