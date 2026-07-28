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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.InstancePre;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.Caller;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for the 9 caller-scoped mutation methods overridden on
 * {@link PanamaCaller} in
 * F-Wasmtime4j-Panama-Caller-Scoped-Mutation-Java-Bindings r.3 slice 2.
 *
 * <p>These are reflection-level tests that prove each mutation method is
 * a real override on {@link PanamaCaller} (not inherited from the
 * {@link Caller}-interface {@code default} UOE fallback). Positive-path
 * runtime testing needs a live callback frame + Panama-consumer harness
 * and is deferred per charter §Out-of-scope.
 */
final class PanamaCallerMutationContractTest {

  private static Method callerDefault(final String name, final Class<?>... params)
      throws NoSuchMethodException {
    return Caller.class.getMethod(name, params);
  }

  private static Method panamaOverride(final String name, final Class<?>... params)
      throws NoSuchMethodException {
    return PanamaCaller.class.getDeclaredMethod(name, params);
  }

  private static void assertRealOverride(final String name, final Class<?>... params)
      throws NoSuchMethodException {
    final Method iface = callerDefault(name, params);
    final Method impl = panamaOverride(name, params);
    // Same signature declared on PanamaCaller — proves override presence.
    assertNotSame(
        iface, impl, "PanamaCaller must declare its own " + name + "; inherited default present");
    // Sanity: not abstract, not default (i.e. real body).
    assertFalse(
        Modifier.isAbstract(impl.getModifiers()),
        name + " must not be abstract on PanamaCaller");
    assertFalse(impl.isDefault(), name + " must not be default on PanamaCaller");
    assertEquals(
        iface.getReturnType(),
        impl.getReturnType(),
        name + " return type must match Caller interface");
  }

  @Test
  void grow_table_is_real_override() throws NoSuchMethodException {
    assertRealOverride("growTable", WasmTable.class, int.class, Object.class);
  }

  @Test
  void set_table_element_is_real_override() throws NoSuchMethodException {
    assertRealOverride("setTableElement", WasmTable.class, int.class, Object.class);
  }

  @Test
  void grow_memory_is_real_override() throws NoSuchMethodException {
    assertRealOverride("growMemory", WasmMemory.class, long.class);
  }

  @Test
  void read_memory_is_real_override() throws NoSuchMethodException {
    assertRealOverride("readMemory", String.class, long.class, int.class);
  }

  @Test
  void write_memory_is_real_override() throws NoSuchMethodException {
    assertRealOverride("writeMemory", String.class, long.class, byte[].class);
  }

  @Test
  void instantiate_is_real_override() throws NoSuchMethodException {
    assertRealOverride("instantiate", InstancePre.class);
  }

  @Test
  void linker_define_memory_is_real_override() throws NoSuchMethodException {
    assertRealOverride(
        "linkerDefineMemory", Linker.class, String.class, String.class, WasmMemory.class);
  }

  @Test
  void linker_define_table_is_real_override() throws NoSuchMethodException {
    assertRealOverride(
        "linkerDefineTable", Linker.class, String.class, String.class, WasmTable.class);
  }

  @Test
  void linker_define_global_is_real_override() throws NoSuchMethodException {
    assertRealOverride(
        "linkerDefineGlobal", Linker.class, String.class, String.class, WasmGlobal.class);
  }

  // ---------------------------------------------------------------------------
  // F-Wasmtime4j-Panama-Consumer-Gated-Followups r.3 + r.4 (2026-07-28) — 3
  // additional overrides that closed the last inherited-UOE gaps: FromExport
  // pair + compileModule.
  // ---------------------------------------------------------------------------

  @Test
  void linker_define_memory_from_export_is_real_override() throws NoSuchMethodException {
    assertRealOverride(
        "linkerDefineMemoryFromExport",
        Linker.class,
        String.class,
        String.class,
        String.class);
  }

  @Test
  void linker_define_table_from_export_is_real_override() throws NoSuchMethodException {
    assertRealOverride(
        "linkerDefineTableFromExport",
        Linker.class,
        String.class,
        String.class,
        String.class);
  }

  @Test
  void compile_module_is_real_override() throws NoSuchMethodException {
    assertRealOverride("compileModule", byte[].class);
  }

  @Test
  void compile_module_returns_module() throws NoSuchMethodException {
    // Panama's compileModule must return ai.tegmentum.wasmtime4j.Module (the
    // public api-level type), parity with JniCaller.compileModule shape.
    final Method impl = panamaOverride("compileModule", byte[].class);
    assertEquals(ai.tegmentum.wasmtime4j.Module.class, impl.getReturnType());
  }

  @Test
  void followup_overrides_throw_wasm_exception() throws NoSuchMethodException {
    // Each of the 3 new overrides declares throws WasmException at the
    // interface level. Sanity-check the impl signature keeps it — same
    // pattern as all_overrides_throw_wasm_exception for the r.3 slice.
    for (final Method m : PanamaCaller.class.getDeclaredMethods()) {
      final String name = m.getName();
      if (name.equals("linkerDefineMemoryFromExport")
          || name.equals("linkerDefineTableFromExport")
          || name.equals("compileModule")) {
        boolean declaresWasmException = false;
        for (final Class<?> t : m.getExceptionTypes()) {
          if (WasmException.class.isAssignableFrom(t)) {
            declaresWasmException = true;
            break;
          }
        }
        if (!declaresWasmException) {
          throw new AssertionError(
              name + " on PanamaCaller must declare throws WasmException");
        }
      }
    }
  }

  @Test
  void all_overrides_throw_wasm_exception() throws NoSuchMethodException {
    // Each of the 9 overrides declares throws WasmException at the interface
    // level. Sanity-check the impl signature keeps it (matters for JDK reflect
    // consumers checking checked-exception contracts).
    for (final Method m : PanamaCaller.class.getDeclaredMethods()) {
      final String name = m.getName();
      if (name.equals("growTable")
          || name.equals("setTableElement")
          || name.equals("growMemory")
          || name.equals("readMemory")
          || name.equals("writeMemory")
          || name.equals("instantiate")
          || name.equals("linkerDefineMemory")
          || name.equals("linkerDefineTable")
          || name.equals("linkerDefineGlobal")) {
        boolean declaresWasmException = false;
        for (final Class<?> t : m.getExceptionTypes()) {
          if (WasmException.class.isAssignableFrom(t)) {
            declaresWasmException = true;
            break;
          }
        }
        assertNotNull(
            m,
            "guard: reflection returned non-null Method for " + name);
        if (!declaresWasmException) {
          throw new AssertionError(
              name + " on PanamaCaller must declare throws WasmException");
        }
      }
    }
  }

  @Test
  void instantiate_returns_instance() throws NoSuchMethodException {
    // Sanity: instantiate override must return ai.tegmentum.wasmtime4j.Instance
    // (parity with JniCaller's shape).
    final Method impl = panamaOverride("instantiate", InstancePre.class);
    assertEquals(Instance.class, impl.getReturnType());
  }

  @Test
  void resolve_ref_id_helper_is_private_instance_method() throws NoSuchMethodException {
    // F-Wasmtime4j-Panama-Consumer-Gated-Followups r.2 (2026-07-28) —
    // resolveRefIdForMutation was flipped from static to instance so it can
    // reach `bindings` + `callerPtr` for non-null WasmFunction dispatch. It
    // remains private and off the public surface; consumers see behavior via
    // growTable / setTableElement, not the helper directly.
    final Method helper =
        PanamaCaller.class.getDeclaredMethod("resolveRefIdForMutation", Object.class, String.class);
    assertNotNull(helper);
    assertEquals(long.class, helper.getReturnType());
    if (!Modifier.isPrivate(helper.getModifiers())) {
      throw new AssertionError("resolveRefIdForMutation must be private");
    }
    if (Modifier.isStatic(helper.getModifiers())) {
      throw new AssertionError(
          "resolveRefIdForMutation must NOT be static after r.2 — it needs "
              + "`bindings` and `callerPtr` to register non-null funcref");
    }
  }

  /**
   * A previously-passed api-level {@link Module} handed to PanamaCaller
   * indirectly (e.g. an api-level type not wrapping a Panama concrete type)
   * would fail extraction. This test reserves that behavior via reflection
   * on the presence of the extract* helpers.
   */
  @Test
  void tier_extraction_helpers_are_defined() throws NoSuchMethodException {
    assertNotNull(
        PanamaCaller.class.getDeclaredMethod("extractPanamaTable", WasmTable.class));
    assertNotNull(
        PanamaCaller.class.getDeclaredMethod("extractPanamaMemory", WasmMemory.class));
    assertNotNull(
        PanamaCaller.class.getDeclaredMethod("extractPanamaGlobal", WasmGlobal.class));
    assertNotNull(
        PanamaCaller.class.getDeclaredMethod("extractPanamaLinker", Linker.class));
  }
}
