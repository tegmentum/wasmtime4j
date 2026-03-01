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
import java.util.Objects;

/**
 * Represents a WebAssembly breakpoint at a specific instruction within a module.
 *
 * <p>Breakpoints are set using {@link BreakpointEditor} and cause the debug handler to be invoked
 * with {@link DebugEvent#BREAKPOINT} when execution reaches the specified program counter.
 *
 * @since 1.1.0
 */
public final class Breakpoint {

  private final Module module;
  private final int pc;

  /**
   * Creates a new Breakpoint.
   *
   * @param module the module containing the breakpoint
   * @param pc the program counter (instruction offset) within the module
   * @throws NullPointerException if module is null
   * @throws IllegalArgumentException if pc is negative
   */
  public Breakpoint(final Module module, final int pc) {
    Objects.requireNonNull(module, "module cannot be null");
    if (pc < 0) {
      throw new IllegalArgumentException("pc cannot be negative: " + pc);
    }
    this.module = module;
    this.pc = pc;
  }

  /**
   * Gets the module containing this breakpoint.
   *
   * @return the module
   */
  public Module getModule() {
    return module;
  }

  /**
   * Gets the program counter (instruction offset) of this breakpoint.
   *
   * @return the program counter
   */
  public int getPc() {
    return pc;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Breakpoint that = (Breakpoint) obj;
    return pc == that.pc && Objects.equals(module, that.module);
  }

  @Override
  public int hashCode() {
    return Objects.hash(module, pc);
  }

  @Override
  public String toString() {
    return "Breakpoint{module=" + module + ", pc=" + pc + "}";
  }
}
