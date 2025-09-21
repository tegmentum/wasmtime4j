package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.Optional;

/**
 * Location information for validation issues in WebAssembly modules.
 *
 * <p>This interface provides precise location information for where validation issues were detected
 * within WebAssembly bytecode or module structure.
 *
 * @since 1.0.0
 */
public interface ValidationLocation {

  /**
   * Gets the module section where the issue was found.
   *
   * @return the module section, or empty if not applicable
   */
  Optional<String> getModuleSection();

  /**
   * Gets the function index where the issue was found.
   *
   * @return the function index, or empty if not in a function
   */
  Optional<Integer> getFunctionIndex();

  /**
   * Gets the instruction offset within the function.
   *
   * @return the instruction offset, or empty if not applicable
   */
  Optional<Long> getInstructionOffset();

  /**
   * Gets the bytecode offset within the module.
   *
   * @return the bytecode offset, or empty if not available
   */
  Optional<Long> getBytecodeOffset();

  /**
   * Gets the type index if the issue is related to a type.
   *
   * @return the type index, or empty if not applicable
   */
  Optional<Integer> getTypeIndex();

  /**
   * Gets additional location context.
   *
   * @return the location context
   */
  String getContext();

  /**
   * Creates a ValidationLocation with basic information.
   *
   * @param context the location context
   * @return the validation location
   */
  static ValidationLocation of(final String context) {
    return new ValidationLocationImpl(context, null, null, null, null);
  }

  /**
   * Creates a ValidationLocation with function and offset information.
   *
   * @param functionIndex the function index
   * @param instructionOffset the instruction offset
   * @param context the location context
   * @return the validation location
   */
  static ValidationLocation of(
      final int functionIndex, final long instructionOffset, final String context) {
    return new ValidationLocationImpl(context, functionIndex, instructionOffset, null, null);
  }

  /** Simple implementation of ValidationLocation. */
  final class ValidationLocationImpl implements ValidationLocation {
    private final String context;
    private final Integer functionIndex;
    private final Long instructionOffset;
    private final Long bytecodeOffset;
    private final Integer typeIndex;

    public ValidationLocationImpl(
        final String context,
        final Integer functionIndex,
        final Long instructionOffset,
        final Long bytecodeOffset,
        final Integer typeIndex) {
      this.context = context;
      this.functionIndex = functionIndex;
      this.instructionOffset = instructionOffset;
      this.bytecodeOffset = bytecodeOffset;
      this.typeIndex = typeIndex;
    }

    @Override
    public Optional<String> getModuleSection() {
      return Optional.empty();
    }

    @Override
    public Optional<Integer> getFunctionIndex() {
      return Optional.ofNullable(functionIndex);
    }

    @Override
    public Optional<Long> getInstructionOffset() {
      return Optional.ofNullable(instructionOffset);
    }

    @Override
    public Optional<Long> getBytecodeOffset() {
      return Optional.ofNullable(bytecodeOffset);
    }

    @Override
    public Optional<Integer> getTypeIndex() {
      return Optional.ofNullable(typeIndex);
    }

    @Override
    public String getContext() {
      return context;
    }
  }
}
