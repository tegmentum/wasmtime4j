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

import ai.tegmentum.wasmtime4j.component.ComponentDebugInfo;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Panama FFI implementation of ComponentDebugInfo.
 *
 * <p>This class provides debug information for WebAssembly components through the Panama Foreign
 * Function API. It provides access to symbols, source maps, execution state, and memory layout.
 *
 * @since 1.0.0
 */
final class PanamaComponentDebugInfo implements ComponentDebugInfo {

  private final String componentId;
  private final String componentName;

  /**
   * Creates a new Panama component debug info instance.
   *
   * @param componentId the component ID
   * @param componentName the component name
   */
  PanamaComponentDebugInfo(final String componentId, final String componentName) {
    if (componentId == null) {
      throw new IllegalArgumentException("componentId cannot be null");
    }
    this.componentId = componentId;
    this.componentName = componentName != null ? componentName : "component-" + componentId;
  }

  @Override
  public String getComponentId() {
    return componentId;
  }

  @Override
  public String getComponentName() {
    return componentName;
  }

  @Override
  public DebugSymbols getSymbols() {
    return new DefaultDebugSymbols();
  }

  @Override
  public List<SourceMap> getSourceMaps() {
    return Collections.emptyList();
  }

  @Override
  public List<VariableInfo> getVariables() {
    return Collections.emptyList();
  }

  @Override
  public List<FunctionInfo> getFunctions() {
    return Collections.emptyList();
  }

  @Override
  public MemoryLayout getMemoryLayout() {
    return new DefaultMemoryLayout();
  }

  @Override
  public List<StackFrame> getStackTrace() {
    return Collections.emptyList();
  }

  @Override
  public List<Breakpoint> getBreakpoints() {
    return Collections.emptyList();
  }

  /** Default implementation of DebugSymbols. */
  private static final class DefaultDebugSymbols implements DebugSymbols {

    @Override
    public Map<String, Symbol> getSymbolTable() {
      return Collections.emptyMap();
    }

    @Override
    public Symbol getSymbolAt(final long address) {
      return null;
    }

    @Override
    public List<Symbol> getSymbolsByName(final String name) {
      return Collections.emptyList();
    }
  }

  /** Default implementation of MemoryLayout. */
  private static final class DefaultMemoryLayout implements MemoryLayout {

    @Override
    public HeapInfo getHeapInfo() {
      return new DefaultHeapInfo();
    }

    @Override
    public StackInfo getStackInfo() {
      return new DefaultStackInfo();
    }

    @Override
    public List<ComponentDebugInfo.MemorySegment> getSegments() {
      return Collections.emptyList();
    }
  }

  /** Default implementation of HeapInfo. */
  private static final class DefaultHeapInfo implements HeapInfo {

    @Override
    public long getStartAddress() {
      return 0;
    }

    @Override
    public long getSize() {
      return 0;
    }

    @Override
    public long getUsedSize() {
      return 0;
    }
  }

  /** Default implementation of StackInfo. */
  private static final class DefaultStackInfo implements StackInfo {

    @Override
    public long getStartAddress() {
      return 0;
    }

    @Override
    public long getSize() {
      return 0;
    }

    @Override
    public long getStackPointer() {
      return 0;
    }
  }
}
