package ai.tegmentum.wasmtime4j.memory;

import ai.tegmentum.wasmtime4j.WasmMemory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for WebAssembly memory64 proposal instructions.
 *
 * <p>This class provides a centralized mechanism for executing 64-bit memory instructions against
 * WebAssembly memory instances. It handles instruction validation, execution, and error reporting
 * for the memory64 proposal.
 *
 * <p>The handler supports:
 *
 * <ul>
 *   <li>All memory64 load and store instructions with proper alignment checking
 *   <li>Memory control operations (size, grow, fill, copy, init)
 *   <li>Instruction caching for performance optimization
 *   <li>Comprehensive error handling and validation
 *   <li>Execution statistics and monitoring
 * </ul>
 *
 * @since 1.1.0
 */
public final class Memory64InstructionHandler {

  private static final Logger LOGGER = Logger.getLogger(Memory64InstructionHandler.class.getName());

  private final Map<Integer, Memory64Instruction> opcodeCache;
  private final Map<String, Memory64Instruction> mnemonicCache;
  private final ExecutionStatistics statistics;

  /** Creates a new Memory64InstructionHandler with default configuration. */
  public Memory64InstructionHandler() {
    this.opcodeCache = new ConcurrentHashMap<>();
    this.mnemonicCache = new ConcurrentHashMap<>();
    this.statistics = new ExecutionStatistics();
    initializeCaches();
  }

  /**
   * Executes a memory64 instruction by opcode.
   *
   * @param opcode the instruction opcode
   * @param memory the memory instance to operate on
   * @param offset the memory offset (64-bit)
   * @param value the value for store operations (ignored for loads)
   * @return the result value for load operations, or 0 for store/control operations
   * @throws UnsupportedOperationException if the instruction or memory doesn't support 64-bit
   * @throws IndexOutOfBoundsException if the operation is out of bounds
   * @throws IllegalArgumentException if parameters are invalid
   */
  public long executeByOpcode(
      final int opcode, final WasmMemory memory, final long offset, final long value) {
    final Memory64Instruction instruction = getInstructionByOpcode(opcode);
    return executeInstruction(instruction, memory, offset, value);
  }

  /**
   * Executes a memory64 instruction by mnemonic.
   *
   * @param mnemonic the instruction mnemonic
   * @param memory the memory instance to operate on
   * @param offset the memory offset (64-bit)
   * @param value the value for store operations (ignored for loads)
   * @return the result value for load operations, or 0 for store/control operations
   * @throws UnsupportedOperationException if the instruction or memory doesn't support 64-bit
   * @throws IndexOutOfBoundsException if the operation is out of bounds
   * @throws IllegalArgumentException if parameters are invalid
   */
  public long executeByMnemonic(
      final String mnemonic, final WasmMemory memory, final long offset, final long value) {
    final Memory64Instruction instruction = getInstructionByMnemonic(mnemonic);
    return executeInstruction(instruction, memory, offset, value);
  }

  /**
   * Executes a memory64 instruction.
   *
   * @param instruction the instruction to execute
   * @param memory the memory instance to operate on
   * @param offset the memory offset (64-bit)
   * @param value the value for store operations (ignored for loads)
   * @return the result value for load operations, or 0 for store/control operations
   * @throws UnsupportedOperationException if the instruction or memory doesn't support 64-bit
   * @throws IndexOutOfBoundsException if the operation is out of bounds
   * @throws IllegalArgumentException if parameters are invalid
   */
  public long executeInstruction(
      final Memory64Instruction instruction,
      final WasmMemory memory,
      final long offset,
      final long value) {
    if (instruction == null) {
      throw new IllegalArgumentException("Instruction cannot be null");
    }
    if (memory == null) {
      throw new IllegalArgumentException("Memory cannot be null");
    }

    statistics.recordExecution(instruction);

    try {
      final long startTime = System.nanoTime();
      final long result = instruction.execute(memory, offset, value);
      final long endTime = System.nanoTime();

      statistics.recordExecutionTime(instruction, endTime - startTime);

      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.fine(
            String.format(
                "Executed %s at offset 0x%X, value=0x%X, result=0x%X",
                instruction.getMnemonic(), offset, value, result));
      }

      return result;
    } catch (final Exception e) {
      statistics.recordError(instruction);
      LOGGER.log(
          Level.WARNING,
          String.format(
              "Error executing %s at offset 0x%X: %s",
              instruction.getMnemonic(), offset, e.getMessage()),
          e);
      throw e;
    }
  }

  /**
   * Validates that a memory supports 64-bit addressing.
   *
   * @param memory the memory to validate
   * @throws UnsupportedOperationException if the memory doesn't support 64-bit addressing
   */
  public void validateMemory64Support(final WasmMemory memory) {
    if (memory == null) {
      throw new IllegalArgumentException("Memory cannot be null");
    }
    if (!memory.supports64BitAddressing()) {
      throw new UnsupportedOperationException("Memory does not support 64-bit addressing");
    }
  }

  /**
   * Validates instruction parameters before execution.
   *
   * @param instruction the instruction to validate
   * @param memory the memory instance
   * @param offset the memory offset
   * @throws IllegalArgumentException if any parameter is invalid
   * @throws IndexOutOfBoundsException if offset is out of bounds
   */
  public void validateInstructionParameters(
      final Memory64Instruction instruction, final WasmMemory memory, final long offset) {
    if (instruction == null) {
      throw new IllegalArgumentException("Instruction cannot be null");
    }
    if (memory == null) {
      throw new IllegalArgumentException("Memory cannot be null");
    }

    // Validate alignment
    instruction.validateAlignment(offset);

    // Validate bounds
    instruction.validateBounds(offset, memory.getSizeInBytes64());
  }

  /**
   * Gets execution statistics for monitoring and performance analysis.
   *
   * @return the execution statistics
   */
  public ExecutionStatistics getStatistics() {
    return statistics;
  }

  /** Resets execution statistics. */
  public void resetStatistics() {
    statistics.reset();
  }

  /**
   * Checks if an instruction is supported by this handler.
   *
   * @param opcode the instruction opcode
   * @return true if the instruction is supported
   */
  public boolean isInstructionSupported(final int opcode) {
    return opcodeCache.containsKey(opcode);
  }

  /**
   * Checks if an instruction is supported by this handler.
   *
   * @param mnemonic the instruction mnemonic
   * @return true if the instruction is supported
   */
  public boolean isInstructionSupported(final String mnemonic) {
    return mnemonicCache.containsKey(mnemonic);
  }

  private Memory64Instruction getInstructionByOpcode(final int opcode) {
    final Memory64Instruction instruction = opcodeCache.get(opcode);
    if (instruction == null) {
      throw new IllegalArgumentException(
          "Unknown memory64 instruction opcode: 0x" + Integer.toHexString(opcode));
    }
    return instruction;
  }

  private Memory64Instruction getInstructionByMnemonic(final String mnemonic) {
    final Memory64Instruction instruction = mnemonicCache.get(mnemonic);
    if (instruction == null) {
      throw new IllegalArgumentException("Unknown memory64 instruction mnemonic: " + mnemonic);
    }
    return instruction;
  }

  private void initializeCaches() {
    for (final Memory64Instruction instruction : Memory64Instruction.values()) {
      opcodeCache.put(instruction.getOpcode(), instruction);
      mnemonicCache.put(instruction.getMnemonic(), instruction);
    }
    LOGGER.info(
        "Initialized memory64 instruction handler with " + opcodeCache.size() + " instructions");
  }

  /** Statistics for memory64 instruction execution. */
  public static final class ExecutionStatistics {

    private final Map<Memory64Instruction, Long> executionCounts;
    private final Map<Memory64Instruction, Long> totalExecutionTimes;
    private final Map<Memory64Instruction, Long> errorCounts;
    private long totalExecutions;
    private long totalErrors;

    ExecutionStatistics() {
      this.executionCounts = new ConcurrentHashMap<>();
      this.totalExecutionTimes = new ConcurrentHashMap<>();
      this.errorCounts = new ConcurrentHashMap<>();
      this.totalExecutions = 0;
      this.totalErrors = 0;
    }

    void recordExecution(final Memory64Instruction instruction) {
      executionCounts.merge(instruction, 1L, Long::sum);
      totalExecutions++;
    }

    void recordExecutionTime(final Memory64Instruction instruction, final long nanos) {
      totalExecutionTimes.merge(instruction, nanos, Long::sum);
    }

    void recordError(final Memory64Instruction instruction) {
      errorCounts.merge(instruction, 1L, Long::sum);
      totalErrors++;
    }

    /**
     * Gets the total number of executions for all instructions.
     *
     * @return the total execution count
     */
    public long getTotalExecutions() {
      return totalExecutions;
    }

    /**
     * Gets the total number of errors for all instructions.
     *
     * @return the total error count
     */
    public long getTotalErrors() {
      return totalErrors;
    }

    /**
     * Gets the execution count for a specific instruction.
     *
     * @param instruction the instruction
     * @return the execution count
     */
    public long getExecutionCount(final Memory64Instruction instruction) {
      return executionCounts.getOrDefault(instruction, 0L);
    }

    /**
     * Gets the total execution time for a specific instruction.
     *
     * @param instruction the instruction
     * @return the total execution time in nanoseconds
     */
    public long getTotalExecutionTime(final Memory64Instruction instruction) {
      return totalExecutionTimes.getOrDefault(instruction, 0L);
    }

    /**
     * Gets the average execution time for a specific instruction.
     *
     * @param instruction the instruction
     * @return the average execution time in nanoseconds
     */
    public double getAverageExecutionTime(final Memory64Instruction instruction) {
      final long count = getExecutionCount(instruction);
      if (count == 0) {
        return 0.0;
      }
      return (double) getTotalExecutionTime(instruction) / count;
    }

    /**
     * Gets the error count for a specific instruction.
     *
     * @param instruction the instruction
     * @return the error count
     */
    public long getErrorCount(final Memory64Instruction instruction) {
      return errorCounts.getOrDefault(instruction, 0L);
    }

    /**
     * Gets the error rate for a specific instruction.
     *
     * @param instruction the instruction
     * @return the error rate as a percentage (0.0 to 100.0)
     */
    public double getErrorRate(final Memory64Instruction instruction) {
      final long count = getExecutionCount(instruction);
      if (count == 0) {
        return 0.0;
      }
      return (double) getErrorCount(instruction) * 100.0 / count;
    }

    /**
     * Gets the overall error rate for all instructions.
     *
     * @return the overall error rate as a percentage (0.0 to 100.0)
     */
    public double getOverallErrorRate() {
      if (totalExecutions == 0) {
        return 0.0;
      }
      return (double) totalErrors * 100.0 / totalExecutions;
    }

    /** Resets all statistics. */
    public void reset() {
      executionCounts.clear();
      totalExecutionTimes.clear();
      errorCounts.clear();
      totalExecutions = 0;
      totalErrors = 0;
    }

    @Override
    public String toString() {
      return String.format(
          "ExecutionStatistics{totalExecutions=%d, totalErrors=%d, errorRate=%.2f%%}",
          totalExecutions, totalErrors, getOverallErrorRate());
    }
  }
}
