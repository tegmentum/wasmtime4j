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
  @DisplayName("linkerDefineMemoryFromExport wires caller's memory into a child linker")
  void linkerDefineMemoryFromExportRoundTrip() throws Exception {
    // Runtime witness for the r.3 override delivered by
    // F-Wasmtime4j-Panama-Consumer-Gated-Followups. Was blocked by the
    // caller_get_memory ValidatedMemory wrapper mismatch — now unblocked.
    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaLinker<Void> parentLinker = new PanamaLinker<>(engine);

    // Parent module: exports "memory" + imports "env.wire" which the
    // callback uses to define the parent's memory into a child linker
    // + instantiate a child module that consumes it.
    final String parentWat =
        "(module\n"
            + "  (import \"env\" \"wire\" (func $wire (result i32)))\n"
            + "  (memory (export \"memory\") 1)\n"
            + "  (func (export \"run\") (result i32) call $wire)\n"
            + ")";

    // Child module: imports env.hostmem (which will be the parent's
    // memory) + writes marker byte at offset 0. Instantiating this
    // child inside the callback proves linkerDefineMemoryFromExport
    // routed the parent's memory into the child linker.
    final String childWat =
        "(module\n"
            + "  (import \"env\" \"hostmem\" (memory 1))\n"
            + "  (func (export \"stamp\") (result i32)\n"
            + "    i32.const 0 i32.const 0xAA i32.store8\n"
            + "    i32.const 1)\n"
            + ")";

    // Pre-compile the child module outside the callback so we don't need
    // caller.compileModule + byte-materialisation inside; test focuses on
    // linkerDefineMemoryFromExport specifically.
    final ai.tegmentum.wasmtime4j.Module childModule = engine.compileWat(childWat);

    final AtomicReference<Throwable> failure = new AtomicReference<>();

    parentLinker.defineHostFunction(
        "env",
        "wire",
        FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
        new HostFunction.CallerAwareHostFunction<Void>(
            (final Caller<Void> caller, final WasmValue[] params) -> {
              try {
                // Build a child linker + define the parent's memory export
                // into it under name env.hostmem via the r.3 override.
                final PanamaLinker<Void> childLinker = new PanamaLinker<>(engine);
                caller.linkerDefineMemoryFromExport(
                    childLinker, "env", "hostmem", "memory");
                // Instantiate the pre-compiled child module + call stamp()
                // which writes to the shared memory. The write must be
                // observable post-callback via instance.getMemory("memory").
                final Instance childInst = childLinker.instantiate(store, childModule);
                final WasmValue[] stampResult = childInst.callFunction("stamp");
                assertEquals(1, stampResult[0].asInt());
                childInst.close();
                return new WasmValue[] {WasmValue.i32(1)};
              } catch (final Throwable t) {
                failure.set(t);
                return new WasmValue[] {WasmValue.i32(-1)};
              }
            }));

    final Instance instance = instantiate(engine, parentLinker, store, parentWat);
    final WasmValue[] results = instance.callFunction("run");

    if (failure.get() != null) {
      throw new AssertionError("Callback assertion failed", failure.get());
    }
    assertEquals(1, results[0].asInt());

    // Verify the child module's byte write is observable in the parent's
    // exported memory — proves the memories were the same underlying object.
    final Optional<WasmMemory> parentMem = instance.getMemory("memory");
    assertTrue(parentMem.isPresent());
    final byte[] read = new byte[1];
    parentMem.get().readBytes(0, read, 0, 1);
    assertEquals((byte) 0xAA, read[0], "child's memory write must be visible in parent's memory");

    instance.close();
    parentLinker.close();
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("linkerDefineTableFromExport wires caller's table into a child linker")
  void linkerDefineTableFromExportRoundTrip() throws Exception {
    // Runtime witness for the r.3 override — parallel to
    // linkerDefineMemoryFromExportRoundTrip but for tables.
    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaLinker<Void> parentLinker = new PanamaLinker<>(engine);

    final String parentWat =
        "(module\n"
            + "  (import \"env\" \"wire\" (func $wire (result i32)))\n"
            + "  (table (export \"t\") 4 funcref)\n"
            + "  (func (export \"run\") (result i32) call $wire)\n"
            + ")";

    // Child module: imports env.hostt table + queries its size. Proves
    // linkerDefineTableFromExport wired the parent's table into the child.
    final String childWat =
        "(module\n"
            + "  (import \"env\" \"hostt\" (table 4 funcref))\n"
            + "  (func (export \"tsize\") (result i32) table.size)\n"
            + ")";

    final ai.tegmentum.wasmtime4j.Module childModule = engine.compileWat(childWat);
    final AtomicReference<Throwable> failure = new AtomicReference<>();
    final AtomicLong observedChildTableSize = new AtomicLong(-1L);

    parentLinker.defineHostFunction(
        "env",
        "wire",
        FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
        new HostFunction.CallerAwareHostFunction<Void>(
            (final Caller<Void> caller, final WasmValue[] params) -> {
              try {
                final PanamaLinker<Void> childLinker = new PanamaLinker<>(engine);
                caller.linkerDefineTableFromExport(childLinker, "env", "hostt", "t");
                final Instance childInst = childLinker.instantiate(store, childModule);
                final WasmValue[] sizeResult = childInst.callFunction("tsize");
                observedChildTableSize.set(sizeResult[0].asInt());
                childInst.close();
                return new WasmValue[] {WasmValue.i32(1)};
              } catch (final Throwable t) {
                failure.set(t);
                return new WasmValue[] {WasmValue.i32(-1)};
              }
            }));

    final Instance instance = instantiate(engine, parentLinker, store, parentWat);
    final WasmValue[] results = instance.callFunction("run");

    if (failure.get() != null) {
      throw new AssertionError("Callback assertion failed", failure.get());
    }
    assertEquals(1, results[0].asInt());
    assertEquals(4L, observedChildTableSize.get(), "child sees parent's table.size == 4");

    instance.close();
    parentLinker.close();
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("caller.growTable(caller.getTable(), 1, null) grows table from within callback")
  void growTableFromWithinCallback() throws Exception {
    // Runtime witness for the caller-scoped table grow path.
    // Uses null init (registry-id 0) — non-null funcref via
    // FuncToRegistryId is covered by the sibling test.
    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaLinker<Void> linker = new PanamaLinker<>(engine);

    final String wat =
        "(module\n"
            + "  (import \"env\" \"grow\" (func $grow (result i32)))\n"
            + "  (table (export \"t\") 2 funcref)\n"
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
                final Optional<ai.tegmentum.wasmtime4j.WasmTable> tableOpt = caller.getTable("t");
                assertTrue(tableOpt.isPresent(), "caller.getTable('t') must succeed");
                final int prev = caller.growTable(tableOpt.get(), 2, null);
                observedPrev.set(prev);
                return new WasmValue[] {WasmValue.i32(prev)};
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
    assertEquals(2, results[0].asInt(), "growTable returns prev size == 2");
    assertEquals(2L, observedPrev.get());

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("caller.growTable with non-null PanamaHostFunction init exercises FuncToRegistryId")
  void growTableWithFuncrefInitExercisesFuncToRegistryId() throws Exception {
    // Runtime witness for the FuncToRegistryId path added in
    // F-Wasmtime4j-Panama-Consumer-Gated-Followups r.2, initially
    // documented as DEFERRED in cd872082 because PanamaHostFunction's
    // functionHandle stores an upcall-stub address (not a
    // *const crate::jni::function::FunctionHandle). Fixed by
    // F-Wasmtime4j-Panama-FuncToRegistryId-Wire-Alignment (2026-07-29):
    // PanamaCaller.resolveRefIdForMutation now routes PanamaHostFunction
    // through its already-cached `funcRefId` (registered at
    // registerInNativeRegistry), and PanamaCallerFunction through a new
    // Panama-specific FFI `callerFuncPtrToRegistryId` that accepts a
    // *const wasmtime::Func directly.
    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaLinker<Void> linker = new PanamaLinker<>(engine);

    final String wat =
        "(module\n"
            + "  (import \"env\" \"grow\" (func $grow (result i32)))\n"
            + "  (table (export \"t\") 1 funcref)\n"
            + "  (func (export \"run\") (result i32) call $grow)\n"
            + ")";

    // Build a PanamaHostFunction via createHostFunction; its funcRefId
    // is registered into the native REFERENCE_REGISTRY at construction.
    final ai.tegmentum.wasmtime4j.WasmFunction initFn =
        store.createHostFunction(
            "init_marker",
            FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
            (params) -> new WasmValue[] {WasmValue.i32(42)});

    final AtomicReference<Throwable> failure = new AtomicReference<>();
    final AtomicLong observedPrev = new AtomicLong(-1L);

    linker.defineHostFunction(
        "env",
        "grow",
        FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
        new HostFunction.CallerAwareHostFunction<Void>(
            (final Caller<Void> caller, final WasmValue[] params) -> {
              try {
                final Optional<ai.tegmentum.wasmtime4j.WasmTable> tableOpt = caller.getTable("t");
                assertTrue(tableOpt.isPresent());
                // Non-null init: exercises FuncToRegistryId path via
                // PanamaHostFunction.getFuncRefId — no wrapper FFI needed.
                final int prev = caller.growTable(tableOpt.get(), 2, initFn);
                observedPrev.set(prev);
                return new WasmValue[] {WasmValue.i32(prev)};
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
    assertEquals(1, results[0].asInt(), "growTable(funcref-init) returns prev size == 1");
    assertEquals(1L, observedPrev.get());

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
