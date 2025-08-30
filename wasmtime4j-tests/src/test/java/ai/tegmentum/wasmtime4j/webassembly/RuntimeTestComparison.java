package ai.tegmentum.wasmtime4j.webassembly;

import java.time.Duration;
import java.util.Objects;

/**
 * Comparison between two runtime test executions.
 * Provides utilities for comparing results between JNI and Panama implementations.
 */
public final class RuntimeTestComparison {
    private final RuntimeTestExecution jniExecution;
    private final RuntimeTestExecution panamaExecution;
    
    /**
     * Creates a runtime test comparison.
     *
     * @param jniExecution the JNI execution result
     * @param panamaExecution the Panama execution result
     */
    public RuntimeTestComparison(final RuntimeTestExecution jniExecution,
                                final RuntimeTestExecution panamaExecution) {
        this.jniExecution = Objects.requireNonNull(jniExecution, "jniExecution cannot be null");
        this.panamaExecution = Objects.requireNonNull(panamaExecution, "panamaExecution cannot be null");
    }
    
    /**
     * Gets the JNI execution result.
     *
     * @return the JNI execution
     */
    public RuntimeTestExecution getJniExecution() {
        return jniExecution;
    }
    
    /**
     * Gets the Panama execution result.
     *
     * @return the Panama execution
     */
    public RuntimeTestExecution getPanamaExecution() {
        return panamaExecution;
    }
    
    /**
     * Checks if both executions were successful.
     *
     * @return true if both succeeded
     */
    public boolean bothSuccessful() {
        return jniExecution.isSuccessful() && panamaExecution.isSuccessful();
    }
    
    /**
     * Checks if the results are equal using Object.equals().
     *
     * @return true if results are equal
     */
    public boolean resultsEqual() {
        if (!bothSuccessful()) {
            return false;
        }
        
        return Objects.equals(jniExecution.getResult(), panamaExecution.getResult());
    }
    
    /**
     * Checks if the results are equal using String representation.
     *
     * @return true if string representations are equal
     */
    public boolean resultsEqualAsString() {
        if (!bothSuccessful()) {
            return false;
        }
        
        final String jniString = String.valueOf(jniExecution.getResult());
        final String panamaString = String.valueOf(panamaExecution.getResult());
        
        return Objects.equals(jniString, panamaString);
    }
    
    /**
     * Checks if both executions have the same success/failure status.
     *
     * @return true if both have same status
     */
    public boolean sameExecutionStatus() {
        return jniExecution.isSuccessful() == panamaExecution.isSuccessful();
    }
    
    /**
     * Gets the performance ratio (Panama time / JNI time).
     * Values > 1.0 indicate JNI is faster, values < 1.0 indicate Panama is faster.
     *
     * @return the performance ratio, or NaN if either execution failed
     */
    public double getPerformanceRatio() {
        if (!bothSuccessful()) {
            return Double.NaN;
        }
        
        final long jniMs = jniExecution.getDuration().toMillis();
        final long panamaMs = panamaExecution.getDuration().toMillis();
        
        if (jniMs == 0) {
            return Double.POSITIVE_INFINITY;
        }
        
        return (double) panamaMs / jniMs;
    }
    
    /**
     * Gets the absolute performance difference in milliseconds.
     *
     * @return the performance difference (Panama - JNI), or NaN if either execution failed
     */
    public long getPerformanceDifferenceMs() {
        if (!bothSuccessful()) {
            return Long.MAX_VALUE; // Indicate invalid
        }
        
        return panamaExecution.getDuration().toMillis() - jniExecution.getDuration().toMillis();
    }
    
    /**
     * Determines which runtime performed better.
     *
     * @return the faster runtime, or null if comparison is invalid
     */
    public RuntimeTestExecution getFasterExecution() {
        if (!bothSuccessful()) {
            return null;
        }
        
        return jniExecution.getDuration().compareTo(panamaExecution.getDuration()) <= 0 
            ? jniExecution 
            : panamaExecution;
    }
    
    /**
     * Checks if the performance difference is within an acceptable threshold.
     *
     * @param thresholdMs the threshold in milliseconds
     * @return true if the difference is within the threshold
     */
    public boolean performanceWithinThreshold(final long thresholdMs) {
        if (!bothSuccessful()) {
            return false;
        }
        
        return Math.abs(getPerformanceDifferenceMs()) <= thresholdMs;
    }
    
    /**
     * Checks if the performance ratio is within an acceptable range.
     *
     * @param maxRatio the maximum acceptable ratio (e.g., 2.0 means one can be up to 2x slower)
     * @return true if the ratio is within the range
     */
    public boolean performanceRatioWithinRange(final double maxRatio) {
        final double ratio = getPerformanceRatio();
        if (Double.isNaN(ratio) || Double.isInfinite(ratio)) {
            return false;
        }
        
        return ratio <= maxRatio && (1.0 / ratio) <= maxRatio;
    }
    
    /**
     * Creates a comparison summary.
     *
     * @return the comparison summary
     */
    public String getSummary() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Runtime Comparison:\n");
        
        sb.append("  JNI: ").append(jniExecution.getSummary()).append('\n');
        sb.append("  Panama: ").append(panamaExecution.getSummary()).append('\n');
        
        if (bothSuccessful()) {
            sb.append("  Results Equal: ").append(resultsEqual()).append('\n');
            sb.append("  Performance Ratio (Panama/JNI): ").append(String.format("%.2f", getPerformanceRatio())).append('\n');
            
            final long diffMs = getPerformanceDifferenceMs();
            if (diffMs > 0) {
                sb.append("  Panama is ").append(diffMs).append("ms slower");
            } else if (diffMs < 0) {
                sb.append("  Panama is ").append(-diffMs).append("ms faster");
            } else {
                sb.append("  Same performance");
            }
        } else {
            sb.append("  Status Consistency: ").append(sameExecutionStatus());
        }
        
        return sb.toString();
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        final RuntimeTestComparison that = (RuntimeTestComparison) obj;
        return Objects.equals(jniExecution, that.jniExecution) &&
               Objects.equals(panamaExecution, that.panamaExecution);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(jniExecution, panamaExecution);
    }
    
    @Override
    public String toString() {
        return "RuntimeTestComparison{" +
               "jniExecution=" + jniExecution +
               ", panamaExecution=" + panamaExecution +
               '}';
    }
}