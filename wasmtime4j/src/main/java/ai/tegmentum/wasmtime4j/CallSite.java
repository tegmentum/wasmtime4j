package ai.tegmentum.wasmtime4j;

/**
 * Represents a function call site for optimization analysis.
 *
 * <p>Call sites provide information about function invocations including their position in the
 * code, calling context, and optimization potential. This is particularly important for tail call
 * optimization analysis.
 *
 * @since 1.1.0
 */
public interface CallSite {

  /**
   * Gets the instruction offset of this call site.
   *
   * @return the byte offset within the function
   */
  long getInstructionOffset();

  /**
   * Gets the source location information, if available.
   *
   * @return the source location, or null if not available
   */
  SourceLocation getSourceLocation();

  /**
   * Checks if this call is in tail position.
   *
   * <p>A call is in tail position if its result is immediately returned without further
   * computation.
   *
   * @return true if the call is in tail position
   */
  boolean isInTailPosition();

  /**
   * Gets the target function being called, if statically known.
   *
   * @return the target function, or null if unknown (indirect call)
   */
  FunctionContext getTargetFunction();

  /**
   * Gets the calling function context.
   *
   * @return the calling function
   */
  FunctionContext getCallingFunction();

  /**
   * Gets the call type (direct, indirect, tail call, etc.).
   *
   * @return the call type
   */
  CallType getCallType();

  /**
   * Gets call frequency information from profiling.
   *
   * @return call frequency data, or null if not available
   */
  CallFrequency getFrequency();

  /**
   * Checks if this call site is a hot spot (frequently executed).
   *
   * @return true if this is a hot call site
   */
  default boolean isHotSpot() {
    final CallFrequency freq = getFrequency();
    return freq != null && freq.getCallCount() > 1000;
  }

  /**
   * Gets optimization information for this call site.
   *
   * @return optimization information
   */
  CallSiteOptimization getOptimization();

  /** Call type enumeration. */
  enum CallType {
    /** Direct function call. */
    DIRECT,
    /** Indirect call through function table. */
    INDIRECT,
    /** Tail call optimization. */
    TAIL_CALL,
    /** Indirect tail call. */
    TAIL_CALL_INDIRECT,
    /** Return call. */
    RETURN_CALL,
    /** Indirect return call. */
    RETURN_CALL_INDIRECT
  }

  /** Source location information. */
  interface SourceLocation {
    String getFileName();

    int getLineNumber();

    int getColumnNumber();

    String getFunctionName();
  }

  /** Call frequency and profiling information. */
  interface CallFrequency {
    /** Number of times this call site has been executed. */
    long getCallCount();

    /** Total execution time spent in calls from this site. */
    long getTotalExecutionTime();

    /** Average execution time per call. */
    double getAverageExecutionTime();

    /** Percentage of total program execution time. */
    double getExecutionTimePercentage();

    /** Whether this call site shows recursive behavior. */
    boolean isRecursive();

    /** Average recursion depth if recursive. */
    double getAverageRecursionDepth();
  }

  /** Call site optimization information. */
  interface CallSiteOptimization {
    /** Whether this call has been inlined. */
    boolean isInlined();

    /** Whether tail call optimization has been applied. */
    boolean hasTailCallOptimization();

    /** Whether this call is a candidate for inlining. */
    boolean isInliningCandidate();

    /** Whether this call is a candidate for tail call optimization. */
    boolean isTailCallCandidate();

    /** Estimated cost of the call (for optimization decisions). */
    int getCallCost();

    /** Estimated benefit of optimization. */
    double getOptimizationBenefit();

    /** Reason why optimization was or wasn't applied. */
    String getOptimizationReason();
  }

  /**
   * Creates a call site.
   *
   * @param instructionOffset the instruction offset
   * @param callingFunction the calling function
   * @param callType the call type
   * @param isInTailPosition whether the call is in tail position
   * @return a new call site
   */
  static CallSite create(
      final long instructionOffset,
      final FunctionContext callingFunction,
      final CallType callType,
      final boolean isInTailPosition) {
    return create(instructionOffset, callingFunction, null, callType, isInTailPosition, null);
  }

  /**
   * Creates a call site with target function information.
   *
   * @param instructionOffset the instruction offset
   * @param callingFunction the calling function
   * @param targetFunction the target function (null for indirect calls)
   * @param callType the call type
   * @param isInTailPosition whether the call is in tail position
   * @param sourceLocation the source location (optional)
   * @return a new call site
   */
  static CallSite create(
      final long instructionOffset,
      final FunctionContext callingFunction,
      final FunctionContext targetFunction,
      final CallType callType,
      final boolean isInTailPosition,
      final SourceLocation sourceLocation) {

    return new CallSite() {
      private final CallFrequencyImpl frequency = new CallFrequencyImpl();
      private final CallSiteOptimizationImpl optimization = new CallSiteOptimizationImpl();

      @Override
      public long getInstructionOffset() {
        return instructionOffset;
      }

      @Override
      public SourceLocation getSourceLocation() {
        return sourceLocation;
      }

      @Override
      public boolean isInTailPosition() {
        return isInTailPosition;
      }

      @Override
      public FunctionContext getTargetFunction() {
        return targetFunction;
      }

      @Override
      public FunctionContext getCallingFunction() {
        return callingFunction;
      }

      @Override
      public CallType getCallType() {
        return callType;
      }

      @Override
      public CallFrequency getFrequency() {
        return frequency;
      }

      @Override
      public CallSiteOptimization getOptimization() {
        return optimization;
      }

      @Override
      public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CallSite{");
        sb.append("offset=0x").append(Long.toHexString(instructionOffset));
        sb.append(", type=").append(callType);
        sb.append(", tailPosition=").append(isInTailPosition);
        if (targetFunction != null) {
          sb.append(", target=").append(targetFunction.getFunctionName());
        }
        sb.append(", caller=").append(callingFunction.getFunctionName());
        sb.append('}');
        return sb.toString();
      }
    };
  }

  /** Default implementation of CallFrequency. */
  class CallFrequencyImpl implements CallFrequency {
    private long callCount = 0;
    private long totalExecutionTime = 0;
    private boolean recursive = false;

    @Override
    public long getCallCount() {
      return callCount;
    }

    @Override
    public long getTotalExecutionTime() {
      return totalExecutionTime;
    }

    @Override
    public double getAverageExecutionTime() {
      return callCount > 0 ? (double) totalExecutionTime / callCount : 0.0;
    }

    @Override
    public double getExecutionTimePercentage() {
      return 0.0;
    } // Would be calculated

    @Override
    public boolean isRecursive() {
      return recursive;
    }

    @Override
    public double getAverageRecursionDepth() {
      return recursive ? 3.0 : 0.0;
    }

    void recordCall(final long executionTime) {
      callCount++;
      totalExecutionTime += executionTime;
    }

    void setRecursive(final boolean recursive) {
      this.recursive = recursive;
    }
  }

  /** Default implementation of CallSiteOptimization. */
  class CallSiteOptimizationImpl implements CallSiteOptimization {
    @Override
    public boolean isInlined() {
      return false;
    }

    @Override
    public boolean hasTailCallOptimization() {
      return false;
    }

    @Override
    public boolean isInliningCandidate() {
      return true;
    }

    @Override
    public boolean isTailCallCandidate() {
      return true;
    }

    @Override
    public int getCallCost() {
      return 10;
    }

    @Override
    public double getOptimizationBenefit() {
      return 1.1;
    }

    @Override
    public String getOptimizationReason() {
      return "Candidate for optimization";
    }
  }
}
