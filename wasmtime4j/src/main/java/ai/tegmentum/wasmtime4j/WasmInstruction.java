package ai.tegmentum.wasmtime4j;

/**
 * Represents a WebAssembly instruction.
 *
 * <p>This interface provides access to instruction metadata including opcode, operands, and
 * execution characteristics. It is used by optimization systems and analysis tools.
 *
 * @since 1.1.0
 */
public interface WasmInstruction {

  /**
   * Gets the instruction opcode.
   *
   * @return the instruction opcode
   */
  int getOpcode();

  /**
   * Gets the instruction mnemonic (human-readable name).
   *
   * @return the instruction mnemonic
   */
  String getMnemonic();

  /**
   * Gets the instruction operands.
   *
   * @return array of operands, or empty array if none
   */
  Object[] getOperands();

  /**
   * Gets the instruction category.
   *
   * @return the instruction category
   */
  InstructionCategory getCategory();

  /**
   * Checks if this instruction can trap (cause a runtime exception).
   *
   * @return true if the instruction can trap
   */
  boolean canTrap();

  /**
   * Gets the stack effect of this instruction.
   *
   * @return the stack effect (positive = pushes, negative = pops)
   */
  StackEffect getStackEffect();

  /** Instruction categories for classification. */
  enum InstructionCategory {
    /** Numeric operations (add, sub, mul, etc.). */
    NUMERIC,
    /** Control flow (br, br_if, call, etc.). */
    CONTROL,
    /** Memory operations (load, store). */
    MEMORY,
    /** Variable operations (local.get, global.set, etc.). */
    VARIABLE,
    /** Reference operations (ref.null, ref.func, etc.). */
    REFERENCE,
    /** Table operations (table.get, table.set, etc.). */
    TABLE,
    /** SIMD operations. */
    SIMD,
    /** Atomic operations. */
    ATOMIC,
    /** Bulk memory operations. */
    BULK_MEMORY
  }

  /** Stack effect information for an instruction. */
  interface StackEffect {
    /** Number of values popped from the stack. */
    int getPops();

    /** Number of values pushed to the stack. */
    int getPushes();

    /** Net effect on stack depth (pushes - pops). */
    default int getNetEffect() {
      return getPushes() - getPops();
    }

    /** Types of values popped from the stack. */
    WasmValueType[] getPopTypes();

    /** Types of values pushed to the stack. */
    WasmValueType[] getPushTypes();

    /** Whether the stack effect is polymorphic (depends on context). */
    boolean isPolymorphic();
  }

  /**
   * Creates a WebAssembly instruction.
   *
   * @param opcode the instruction opcode
   * @param mnemonic the instruction mnemonic
   * @param category the instruction category
   * @param operands the instruction operands
   * @return a WebAssembly instruction
   */
  static WasmInstruction create(
      final int opcode,
      final String mnemonic,
      final InstructionCategory category,
      final Object... operands) {
    return new WasmInstruction() {
      @Override
      public int getOpcode() {
        return opcode;
      }

      @Override
      public String getMnemonic() {
        return mnemonic;
      }

      @Override
      public Object[] getOperands() {
        return operands.clone();
      }

      @Override
      public InstructionCategory getCategory() {
        return category;
      }

      @Override
      public boolean canTrap() {
        // Default implementation - specific instructions override this
        switch (category) {
          case MEMORY:
            return true; // Memory operations can trap on out-of-bounds
          case NUMERIC:
            return mnemonic.contains("div") || mnemonic.contains("rem"); // Division can trap
          default:
            return false;
        }
      }

      @Override
      public StackEffect getStackEffect() {
        return createStackEffect(opcode, mnemonic);
      }

      @Override
      public String toString() {
        final StringBuilder sb = new StringBuilder(mnemonic);
        if (operands.length > 0) {
          sb.append(" ");
          for (int i = 0; i < operands.length; i++) {
            if (i > 0) {
              sb.append(" ");
            }
            sb.append(operands[i]);
          }
        }
        return sb.toString();
      }
    };
  }

  /** Creates stack effect information for common instructions. */
  static StackEffect createStackEffect(final int opcode, final String mnemonic) {
    // Simplified stack effect calculation
    return new StackEffect() {
      @Override
      public int getPops() {
        // Basic heuristic based on instruction type
        switch (opcode) {
          case 0x0C: // br
            return 0; // Unconditional branch
          case 0x0D: // br_if
            return 1; // Pops condition
          case 0x10: // call
            return 0; // Function-specific
          case 0x11: // call_indirect
            return 1; // Pops function index
          default:
            if (mnemonic.startsWith("i32.")
                || mnemonic.startsWith("i64.")
                || mnemonic.startsWith("f32.")
                || mnemonic.startsWith("f64.")) {
              if (mnemonic.contains("const")) {
                return 0;
              }
              if (mnemonic.contains("load")) {
                return 1;
              }
              if (mnemonic.contains("store")) {
                return 2;
              }
              return 2; // Most binary operations
            }
            return 0;
        }
      }

      @Override
      public int getPushes() {
        // Basic heuristic
        switch (opcode) {
          case 0x0C: // br
          case 0x0D: // br_if
            return 0;
          default:
            if (mnemonic.contains("const") || mnemonic.contains("load")) {
              return 1;
            }
            if (mnemonic.contains("store")) {
              return 0;
            }
            if (mnemonic.startsWith("i32.")
                || mnemonic.startsWith("i64.")
                || mnemonic.startsWith("f32.")
                || mnemonic.startsWith("f64.")) {
              return 1; // Most operations produce a result
            }
            return 0;
        }
      }

      @Override
      public WasmValueType[] getPopTypes() {
        // Simplified - real implementation would be more precise
        final int pops = getPops();
        final WasmValueType[] types = new WasmValueType[pops];
        for (int i = 0; i < pops; i++) {
          types[i] = inferTypeFromMnemonic(mnemonic);
        }
        return types;
      }

      @Override
      public WasmValueType[] getPushTypes() {
        final int pushes = getPushes();
        final WasmValueType[] types = new WasmValueType[pushes];
        for (int i = 0; i < pushes; i++) {
          types[i] = inferTypeFromMnemonic(mnemonic);
        }
        return types;
      }

      @Override
      public boolean isPolymorphic() {
        return opcode == 0x00 || opcode == 0x01; // unreachable, nop
      }
    };
  }

  /** Infers value type from instruction mnemonic. */
  private static WasmValueType inferTypeFromMnemonic(final String mnemonic) {
    if (mnemonic.startsWith("i32.")) {
      return WasmValueType.I32;
    }
    if (mnemonic.startsWith("i64.")) {
      return WasmValueType.I64;
    }
    if (mnemonic.startsWith("f32.")) {
      return WasmValueType.F32;
    }
    if (mnemonic.startsWith("f64.")) {
      return WasmValueType.F64;
    }
    return WasmValueType.I32; // Default
  }
}
