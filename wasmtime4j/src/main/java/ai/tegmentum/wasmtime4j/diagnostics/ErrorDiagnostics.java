package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Comprehensive diagnostic information for WebAssembly errors.
 *
 * <p>This interface provides detailed diagnostic data to help developers
 * understand, debug, and resolve WebAssembly execution issues.
 *
 * @since 1.0.0
 */
public interface ErrorDiagnostics {

    /**
     * Diagnostic levels for error information.
     */
    enum Level {
        /** Basic error information */
        BASIC,
        /** Detailed diagnostic information */
        DETAILED,
        /** Comprehensive debugging information */
        COMPREHENSIVE,
        /** Expert-level technical details */
        EXPERT
    }

    /**
     * Gets the diagnostic level.
     *
     * @return the diagnostic level
     */
    Level getLevel();

    /**
     * Gets the primary error code.
     *
     * @return the error code
     */
    String getErrorCode();

    /**
     * Gets the detailed error description.
     *
     * @return the error description
     */
    String getDescription();

    /**
     * Gets the root cause analysis.
     *
     * @return the root cause analysis, or empty if not available
     */
    Optional<String> getRootCauseAnalysis();

    /**
     * Gets the validation issues if applicable.
     *
     * @return list of validation issues
     */
    List<ValidationIssue> getValidationIssues();

    /**
     * Gets the performance impact assessment.
     *
     * @return the performance impact, or empty if not applicable
     */
    Optional<PerformanceImpact> getPerformanceImpact();

    /**
     * Gets suggested fixes for the error.
     *
     * @return list of suggested fixes
     */
    List<SuggestedFix> getSuggestedFixes();

    /**
     * Gets related documentation links.
     *
     * @return list of documentation URLs
     */
    List<String> getDocumentationLinks();

    /**
     * Gets similar known issues.
     *
     * @return list of similar issues
     */
    List<KnownIssue> getSimilarIssues();

    /**
     * Gets the debugging information.
     *
     * @return the debug information, or empty if not available
     */
    Optional<DebugInfo> getDebugInfo();

    /**
     * Gets the runtime metrics at the time of error.
     *
     * @return the runtime metrics, or empty if not available
     */
    Optional<RuntimeMetrics> getRuntimeMetrics();

    /**
     * Gets additional diagnostic properties.
     *
     * @return map of diagnostic properties
     */
    Map<String, Object> getProperties();

    /**
     * Gets the diagnostic collection timestamp.
     *
     * @return the collection timestamp
     */
    long getCollectionTimestamp();

    /**
     * Checks if this diagnostic contains sensitive information.
     *
     * @return true if contains sensitive information
     */
    boolean containsSensitiveInfo();

    /**
     * Gets the sanitized version of diagnostics with sensitive information removed.
     *
     * @return sanitized diagnostics
     */
    ErrorDiagnostics sanitized();

    /**
     * Creates a builder for constructing ErrorDiagnostics instances.
     *
     * @return a new diagnostics builder
     */
    static ErrorDiagnosticsBuilder builder() {
        return new ErrorDiagnosticsBuilder();
    }
}