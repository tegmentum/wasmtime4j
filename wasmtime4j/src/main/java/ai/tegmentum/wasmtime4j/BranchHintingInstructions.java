package ai.tegmentum.wasmtime4j;

/**
 * WebAssembly branch hinting instructions for performance optimization.
 *
 * <p>The branch hinting proposal adds annotations to conditional branches to help
 * the runtime engine optimize performance by providing hints about which branch
 * is more likely to be taken. This enables better:
 *
 * <ul>
 *   <li>Branch prediction optimization
 *   <li>Code layout and instruction scheduling
 *   <li>Profile-guided optimization integration
 *   <li>Speculative execution improvements
 * </ul>
 *
 * <p>Branch hints are performance optimizations only and do not affect the
 * semantic behavior of WebAssembly programs. Runtimes may ignore hints
 * if they don't provide benefit.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Hint that the if branch is likely to be taken
 * BranchHint.LIKELY_TAKEN.apply(instance, "conditional_branch");
 *
 * // Hint that a loop is expected to iterate many times
 * BranchHint.LOOP_HOT.apply(instance, "main_loop");
 * }</pre>
 *
 * @since 1.1.0
 */
public enum BranchHintingInstructions {

    /**
     * Hint that a conditional branch is likely to be taken.
     * Applied to br_if instructions to indicate the condition is usually true.
     */
    LIKELY_TAKEN(0x01, "hint.likely", BranchProbability.HIGH, BranchType.CONDITIONAL),

    /**
     * Hint that a conditional branch is unlikely to be taken.
     * Applied to br_if instructions to indicate the condition is usually false.
     */
    UNLIKELY_TAKEN(0x02, "hint.unlikely", BranchProbability.LOW, BranchType.CONDITIONAL),

    /**
     * Hint that a loop is expected to execute many iterations (hot loop).
     * Applied to loop blocks to indicate high iteration count.
     */
    LOOP_HOT(0x03, "hint.loop_hot", BranchProbability.HIGH, BranchType.LOOP),

    /**
     * Hint that a loop is expected to execute few iterations (cold loop).
     * Applied to loop blocks to indicate low iteration count.
     */
    LOOP_COLD(0x04, "hint.loop_cold", BranchProbability.LOW, BranchType.LOOP),

    /**
     * Hint that a branch target is a hot path (frequently executed).
     * Applied to unconditional branches to optimize target code placement.
     */
    HOT_PATH(0x05, "hint.hot_path", BranchProbability.HIGH, BranchType.UNCONDITIONAL),

    /**
     * Hint that a branch target is a cold path (rarely executed).
     * Applied to unconditional branches for error handling or rare cases.
     */
    COLD_PATH(0x06, "hint.cold_path", BranchProbability.LOW, BranchType.UNCONDITIONAL),

    /**
     * Hint for switch/br_table optimization indicating the most likely case.
     * Applied to br_table instructions to optimize dispatch.
     */
    SWITCH_HOT_CASE(0x07, "hint.switch_hot", BranchProbability.HIGH, BranchType.SWITCH),

    /**
     * Hint for function call frequency indicating a hot call site.
     * Applied to call instructions for inlining and optimization decisions.
     */
    CALL_HOT(0x08, "hint.call_hot", BranchProbability.HIGH, BranchType.CALL),

    /**
     * Hint for function call frequency indicating a cold call site.
     * Applied to call instructions for rare or error path calls.
     */
    CALL_COLD(0x09, "hint.call_cold", BranchProbability.LOW, BranchType.CALL);

    private final int opcode;
    private final String mnemonic;
    private final BranchProbability probability;
    private final BranchType branchType;

    BranchHintingInstructions(final int opcode, final String mnemonic,
                             final BranchProbability probability, final BranchType branchType) {
        this.opcode = opcode;
        this.mnemonic = mnemonic;
        this.probability = probability;
        this.branchType = branchType;
    }

    /**
     * Gets the instruction opcode.
     *
     * @return the opcode value
     */
    public int getOpcode() {
        return opcode;
    }

    /**
     * Gets the instruction mnemonic.
     *
     * @return the mnemonic string
     */
    public String getMnemonic() {
        return mnemonic;
    }

    /**
     * Gets the branch probability hint.
     *
     * @return the probability hint
     */
    public BranchProbability getProbability() {
        return probability;
    }

    /**
     * Gets the type of branch this hint applies to.
     *
     * @return the branch type
     */
    public BranchType getBranchType() {
        return branchType;
    }

    /**
     * Applies this branch hint to a WebAssembly instance at a specific location.
     *
     * <p>This method is used by compilers and optimization tools to attach
     * branch hints to specific instructions or code locations.
     *
     * @param context the execution context
     * @param instructionOffset the byte offset of the target instruction
     * @param additionalData optional additional hint data
     * @return true if the hint was successfully applied
     */
    public boolean applyHint(final WasmExecutionContext context,
                           final long instructionOffset,
                           final Object... additionalData) {
        if (context == null) {
            return false;
        }

        try {
            return context.applyBranchHint(this, instructionOffset, additionalData);
        } catch (final Exception e) {
            // Hints are optimization only - failures should not affect execution
            return false;
        }
    }

    /**
     * Creates a branch hint annotation for use in compilation.
     *
     * @param targetLabel the target label or instruction identifier
     * @param confidence the confidence level of the hint (0.0 to 1.0)
     * @return a branch hint annotation
     */
    public BranchHintAnnotation createAnnotation(final String targetLabel, final double confidence) {
        return new BranchHintAnnotation(this, targetLabel, confidence);
    }

    /**
     * Checks if this hint is compatible with the given branch instruction.
     *
     * @param instruction the WebAssembly instruction to check
     * @return true if the hint can be applied to the instruction
     */
    public boolean isCompatibleWith(final WasmInstruction instruction) {
        switch (branchType) {
            case CONDITIONAL:
                return instruction.getOpcode() == 0x0D; // br_if
            case UNCONDITIONAL:
                return instruction.getOpcode() == 0x0C; // br
            case LOOP:
                return instruction.getOpcode() == 0x03; // loop
            case SWITCH:
                return instruction.getOpcode() == 0x0E; // br_table
            case CALL:
                return instruction.getOpcode() == 0x10 // call
                    || instruction.getOpcode() == 0x11; // call_indirect
            default:
                return false;
        }
    }

    /**
     * Branch probability enumeration.
     */
    public enum BranchProbability {
        /** Very low probability (~5% or less). */
        VERY_LOW(0.05),
        /** Low probability (~25% or less). */
        LOW(0.25),
        /** Medium probability (~50%). */
        MEDIUM(0.5),
        /** High probability (~75% or more). */
        HIGH(0.75),
        /** Very high probability (~95% or more). */
        VERY_HIGH(0.95);

        private final double value;

        BranchProbability(final double value) {
            this.value = value;
        }

        public double getValue() {
            return value;
        }
    }

    /**
     * Branch type enumeration.
     */
    public enum BranchType {
        /** Conditional branch (br_if). */
        CONDITIONAL,
        /** Unconditional branch (br). */
        UNCONDITIONAL,
        /** Loop construct. */
        LOOP,
        /** Switch/br_table construct. */
        SWITCH,
        /** Function call. */
        CALL
    }

    /**
     * Branch hint annotation for compilation and optimization.
     */
    public static class BranchHintAnnotation {
        private final BranchHintingInstructions hint;
        private final String targetLabel;
        private final double confidence;
        private final long timestamp;

        public BranchHintAnnotation(final BranchHintingInstructions hint,
                                  final String targetLabel,
                                  final double confidence) {
            this.hint = hint;
            this.targetLabel = targetLabel;
            this.confidence = Math.max(0.0, Math.min(1.0, confidence));
            this.timestamp = System.currentTimeMillis();
        }

        public BranchHintingInstructions getHint() {
            return hint;
        }

        public String getTargetLabel() {
            return targetLabel;
        }

        public double getConfidence() {
            return confidence;
        }

        public long getTimestamp() {
            return timestamp;
        }

        /**
         * Gets the effective probability considering confidence.
         *
         * @return adjusted probability based on confidence level
         */
        public double getEffectiveProbability() {
            final double baseProbability = hint.getProbability().getValue();
            // Adjust probability based on confidence
            if (confidence < 0.5) {
                // Low confidence - move towards neutral (0.5)
                return baseProbability + (0.5 - baseProbability) * (1.0 - confidence * 2.0);
            } else {
                // High confidence - use full probability
                return baseProbability;
            }
        }

        @Override
        public String toString() {
            return String.format("%s[%s, confidence=%.2f, prob=%.2f]",
                hint.getMnemonic(), targetLabel, confidence, getEffectiveProbability());
        }
    }

    /**
     * Finds a branch hinting instruction by opcode.
     *
     * @param opcode the instruction opcode
     * @return the matching instruction, or null if not found
     */
    public static BranchHintingInstructions fromOpcode(final int opcode) {
        for (final BranchHintingInstructions hint : values()) {
            if (hint.opcode == opcode) {
                return hint;
            }
        }
        return null;
    }

    /**
     * Finds a branch hinting instruction by mnemonic.
     *
     * @param mnemonic the instruction mnemonic
     * @return the matching instruction, or null if not found
     */
    public static BranchHintingInstructions fromMnemonic(final String mnemonic) {
        for (final BranchHintingInstructions hint : values()) {
            if (hint.mnemonic.equals(mnemonic)) {
                return hint;
            }
        }
        return null;
    }
}