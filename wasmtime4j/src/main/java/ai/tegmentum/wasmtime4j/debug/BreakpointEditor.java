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
package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.Module;

/**
 * Editor for batch-modifying breakpoints on a store.
 *
 * <p>BreakpointEditor allows adding and removing breakpoints, as well as toggling single-step mode.
 * Changes are applied atomically when the editor is committed via {@link #apply()}.
 *
 * <p>Obtain an editor via {@link ai.tegmentum.wasmtime4j.Store#editBreakpoints()}.
 *
 * @since 1.1.0
 */
public interface BreakpointEditor {

  /**
   * Adds a breakpoint at the specified module and program counter.
   *
   * @param module the module to set the breakpoint in
   * @param pc the program counter (instruction offset) to break at
   * @return this editor for method chaining
   * @throws NullPointerException if module is null
   * @throws IllegalArgumentException if pc is negative
   */
  BreakpointEditor addBreakpoint(Module module, int pc);

  /**
   * Removes a breakpoint at the specified module and program counter.
   *
   * @param module the module containing the breakpoint
   * @param pc the program counter of the breakpoint to remove
   * @return this editor for method chaining
   * @throws NullPointerException if module is null
   * @throws IllegalArgumentException if pc is negative
   */
  BreakpointEditor removeBreakpoint(Module module, int pc);

  /**
   * Enables or disables single-step mode.
   *
   * <p>When single-step mode is enabled, the debug handler will be invoked before each WebAssembly
   * instruction is executed.
   *
   * @param enabled true to enable single-step mode
   * @return this editor for method chaining
   */
  BreakpointEditor singleStep(boolean enabled);

  /**
   * Applies all pending breakpoint changes.
   *
   * <p>This method must be called to commit the changes made through this editor.
   */
  void apply();
}
