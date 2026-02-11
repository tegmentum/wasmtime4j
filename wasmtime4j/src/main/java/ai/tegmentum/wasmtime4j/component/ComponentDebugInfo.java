package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.component.ExecutionState;

/**
 * Component debug information interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ComponentDebugInfo {

  /**
   * Gets the component ID.
   *
   * @return component ID
   */
  String getComponentId();

  /**
   * Gets the component name.
   *
   * @return component name
   */
  String getComponentName();

  /**
   * Gets debug symbols.
   *
   * @return debug symbols
   */
  DebugSymbols getSymbols();

  /**
   * Gets source maps.
   *
   * @return source maps
   */
  java.util.List<SourceMap> getSourceMaps();

  /**
   * Gets current execution state.
   *
   * @return execution state
   */
  ExecutionState getExecutionState();

  /**
   * Gets variable information.
   *
   * @return variable information
   */
  java.util.List<VariableInfo> getVariables();

  /**
   * Gets function information.
   *
   * @return function information
   */
  java.util.List<FunctionInfo> getFunctions();

  /**
   * Gets memory layout information.
   *
   * @return memory layout
   */
  MemoryLayout getMemoryLayout();

  /**
   * Gets stack trace.
   *
   * @return stack trace
   */
  java.util.List<StackFrame> getStackTrace();

  /**
   * Gets breakpoint information.
   *
   * @return breakpoints
   */
  java.util.List<Breakpoint> getBreakpoints();

  /** Debug symbols interface. */
  interface DebugSymbols {
    /**
     * Gets symbol table.
     *
     * @return symbol table
     */
    java.util.Map<String, Symbol> getSymbolTable();

    /**
     * Gets symbol by address.
     *
     * @param address memory address
     * @return symbol at address, or null
     */
    Symbol getSymbolAt(long address);

    /**
     * Gets symbols by name.
     *
     * @param name symbol name
     * @return list of symbols
     */
    java.util.List<Symbol> getSymbolsByName(String name);
  }

  /** Symbol interface. */
  interface Symbol {
    /**
     * Gets symbol name.
     *
     * @return symbol name
     */
    String getName();

    /**
     * Gets symbol address.
     *
     * @return symbol address
     */
    long getAddress();

    /**
     * Gets symbol size.
     *
     * @return symbol size
     */
    long getSize();

    /**
     * Gets symbol type.
     *
     * @return symbol type
     */
    SymbolType getType();
  }

  /** Source map interface. */
  interface SourceMap {
    /**
     * Gets source file name.
     *
     * @return source file
     */
    String getSourceFile();

    /**
     * Gets line mappings.
     *
     * @return line mappings
     */
    java.util.List<LineMapping> getLineMappings();

    /**
     * Gets column mappings.
     *
     * @return column mappings
     */
    java.util.List<ColumnMapping> getColumnMappings();
  }

  /** Line mapping interface. */
  interface LineMapping {
    /**
     * Gets source line number.
     *
     * @return source line
     */
    int getSourceLine();

    /**
     * Gets WebAssembly instruction offset.
     *
     * @return instruction offset
     */
    int getInstructionOffset();
  }

  /** Column mapping interface. */
  interface ColumnMapping {
    /**
     * Gets source column number.
     *
     * @return source column
     */
    int getSourceColumn();

    /**
     * Gets WebAssembly instruction offset.
     *
     * @return instruction offset
     */
    int getInstructionOffset();
  }

  /** Variable information interface. */
  interface VariableInfo {
    /**
     * Gets variable name.
     *
     * @return variable name
     */
    String getName();

    /**
     * Gets variable type.
     *
     * @return variable type
     */
    String getType();

    /**
     * Gets variable value.
     *
     * @return variable value
     */
    Object getValue();

    /**
     * Gets variable scope.
     *
     * @return variable scope
     */
    VariableScope getScope();

    /**
     * Gets variable location.
     *
     * @return variable location
     */
    VariableLocation getLocation();
  }

  /** Function information interface. */
  interface FunctionInfo {
    /**
     * Gets function name.
     *
     * @return function name
     */
    String getName();

    /**
     * Gets function start address.
     *
     * @return start address
     */
    long getStartAddress();

    /**
     * Gets function end address.
     *
     * @return end address
     */
    long getEndAddress();

    /**
     * Gets function parameters.
     *
     * @return parameter list
     */
    java.util.List<ParameterInfo> getParameters();

    /**
     * Gets local variables.
     *
     * @return local variables
     */
    java.util.List<VariableInfo> getLocalVariables();
  }

  /** Parameter information interface. */
  interface ParameterInfo {
    /**
     * Gets parameter name.
     *
     * @return parameter name
     */
    String getName();

    /**
     * Gets parameter type.
     *
     * @return parameter type
     */
    String getType();

    /**
     * Gets parameter index.
     *
     * @return parameter index
     */
    int getIndex();
  }

  /** Memory layout interface. */
  interface MemoryLayout {
    /**
     * Gets heap information.
     *
     * @return heap info
     */
    HeapInfo getHeapInfo();

    /**
     * Gets stack information.
     *
     * @return stack info
     */
    StackInfo getStackInfo();

    /**
     * Gets memory segments.
     *
     * @return memory segments
     */
    java.util.List<MemorySegment> getSegments();
  }

  /** Heap information interface. */
  interface HeapInfo {
    /**
     * Gets heap start address.
     *
     * @return start address
     */
    long getStartAddress();

    /**
     * Gets heap size.
     *
     * @return heap size
     */
    long getSize();

    /**
     * Gets used heap size.
     *
     * @return used size
     */
    long getUsedSize();
  }

  /** Stack information interface. */
  interface StackInfo {
    /**
     * Gets stack start address.
     *
     * @return start address
     */
    long getStartAddress();

    /**
     * Gets stack size.
     *
     * @return stack size
     */
    long getSize();

    /**
     * Gets current stack pointer.
     *
     * @return stack pointer
     */
    long getStackPointer();
  }

  /** Memory segment interface. */
  interface MemorySegment {
    /**
     * Gets segment name.
     *
     * @return segment name
     */
    String getName();

    /**
     * Gets segment start address.
     *
     * @return start address
     */
    long getStartAddress();

    /**
     * Gets segment size.
     *
     * @return segment size
     */
    long getSize();

    /**
     * Gets segment type.
     *
     * @return segment type
     */
    SegmentType getType();
  }

  /** Stack frame interface. */
  interface StackFrame {
    /**
     * Gets function name.
     *
     * @return function name
     */
    String getFunctionName();

    /**
     * Gets instruction pointer.
     *
     * @return instruction pointer
     */
    long getInstructionPointer();

    /**
     * Gets frame pointer.
     *
     * @return frame pointer
     */
    long getFramePointer();

    /**
     * Gets frame depth.
     *
     * @return frame depth
     */
    int getDepth();
  }

  /** Breakpoint interface. */
  interface Breakpoint {
    /**
     * Gets breakpoint ID.
     *
     * @return breakpoint ID
     */
    String getId();

    /**
     * Gets breakpoint address.
     *
     * @return breakpoint address
     */
    long getAddress();

    /**
     * Gets breakpoint condition.
     *
     * @return condition, or null if unconditional
     */
    String getCondition();

    /**
     * Checks if breakpoint is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();
  }

  /** Symbol type enumeration. */
  enum SymbolType {
    /** Function symbol. */
    FUNCTION,
    /** Variable symbol. */
    VARIABLE,
    /** Type symbol. */
    TYPE,
    /** Label symbol. */
    LABEL,
    /** Section symbol. */
    SECTION
  }

  /** Variable scope enumeration. */
  enum VariableScope {
    /** Global scope. */
    GLOBAL,
    /** Function scope. */
    FUNCTION,
    /** Block scope. */
    BLOCK,
    /** Parameter scope. */
    PARAMETER
  }

  /** Variable location enumeration. */
  enum VariableLocation {
    /** In register. */
    REGISTER,
    /** In memory. */
    MEMORY,
    /** In stack. */
    STACK,
    /** Constant value. */
    CONSTANT
  }

  /** Segment type enumeration. */
  enum SegmentType {
    /** Code segment. */
    CODE,
    /** Data segment. */
    DATA,
    /** Heap segment. */
    HEAP,
    /** Stack segment. */
    STACK
  }
}
