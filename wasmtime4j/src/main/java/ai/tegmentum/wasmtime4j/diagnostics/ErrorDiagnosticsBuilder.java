package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builder for constructing ErrorDiagnostics instances.
 *
 * <p>This builder provides a fluent API for creating comprehensive diagnostic information
 * for WebAssembly errors with detailed analysis and suggestions.
 *
 * @since 1.0.0
 */
public final class ErrorDiagnosticsBuilder {

    private ErrorDiagnostics.Level level = ErrorDiagnostics.Level.BASIC;
    private String errorCode;
    private String description;
    private String rootCauseAnalysis;
    private final List<ValidationIssue> validationIssues = new ArrayList<>();
    private PerformanceImpact performanceImpact;
    private final List<SuggestedFix> suggestedFixes = new ArrayList<>();
    private final List<String> documentationLinks = new ArrayList<>();
    private final List<KnownIssue> similarIssues = new ArrayList<>();
    private DebugInfo debugInfo;
    private RuntimeMetrics runtimeMetrics;
    private final Map<String, Object> properties = new HashMap<>();
    private long collectionTimestamp = System.currentTimeMillis();
    private boolean containsSensitiveInfo = false;

    /**
     * Sets the diagnostic level.
     *
     * @param level the diagnostic level
     * @return this builder
     */
    public ErrorDiagnosticsBuilder level(final ErrorDiagnostics.Level level) {
        this.level = level;
        return this;
    }

    /**
     * Sets the error code.
     *
     * @param errorCode the error code
     * @return this builder
     */
    public ErrorDiagnosticsBuilder errorCode(final String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    /**
     * Sets the error description.
     *
     * @param description the error description
     * @return this builder
     */
    public ErrorDiagnosticsBuilder description(final String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets the root cause analysis.
     *
     * @param rootCauseAnalysis the root cause analysis
     * @return this builder
     */
    public ErrorDiagnosticsBuilder rootCauseAnalysis(final String rootCauseAnalysis) {
        this.rootCauseAnalysis = rootCauseAnalysis;
        return this;
    }

    /**
     * Adds a validation issue.
     *
     * @param validationIssue the validation issue
     * @return this builder
     */
    public ErrorDiagnosticsBuilder addValidationIssue(final ValidationIssue validationIssue) {
        this.validationIssues.add(validationIssue);
        return this;
    }

    /**
     * Sets the validation issues.
     *
     * @param validationIssues the validation issues
     * @return this builder
     */
    public ErrorDiagnosticsBuilder validationIssues(final List<ValidationIssue> validationIssues) {
        this.validationIssues.clear();
        this.validationIssues.addAll(validationIssues);
        return this;
    }

    /**
     * Sets the performance impact.
     *
     * @param performanceImpact the performance impact
     * @return this builder
     */
    public ErrorDiagnosticsBuilder performanceImpact(final PerformanceImpact performanceImpact) {
        this.performanceImpact = performanceImpact;
        return this;
    }

    /**
     * Adds a suggested fix.
     *
     * @param suggestedFix the suggested fix
     * @return this builder
     */
    public ErrorDiagnosticsBuilder addSuggestedFix(final SuggestedFix suggestedFix) {
        this.suggestedFixes.add(suggestedFix);
        return this;
    }

    /**
     * Sets the suggested fixes.
     *
     * @param suggestedFixes the suggested fixes
     * @return this builder
     */
    public ErrorDiagnosticsBuilder suggestedFixes(final List<SuggestedFix> suggestedFixes) {
        this.suggestedFixes.clear();
        this.suggestedFixes.addAll(suggestedFixes);
        return this;
    }

    /**
     * Adds a documentation link.
     *
     * @param documentationLink the documentation link
     * @return this builder
     */
    public ErrorDiagnosticsBuilder addDocumentationLink(final String documentationLink) {
        this.documentationLinks.add(documentationLink);
        return this;
    }

    /**
     * Sets the documentation links.
     *
     * @param documentationLinks the documentation links
     * @return this builder
     */
    public ErrorDiagnosticsBuilder documentationLinks(final List<String> documentationLinks) {
        this.documentationLinks.clear();
        this.documentationLinks.addAll(documentationLinks);
        return this;
    }

    /**
     * Adds a similar issue.
     *
     * @param similarIssue the similar issue
     * @return this builder
     */
    public ErrorDiagnosticsBuilder addSimilarIssue(final KnownIssue similarIssue) {
        this.similarIssues.add(similarIssue);
        return this;
    }

    /**
     * Sets the similar issues.
     *
     * @param similarIssues the similar issues
     * @return this builder
     */
    public ErrorDiagnosticsBuilder similarIssues(final List<KnownIssue> similarIssues) {
        this.similarIssues.clear();
        this.similarIssues.addAll(similarIssues);
        return this;
    }

    /**
     * Sets the debug info.
     *
     * @param debugInfo the debug info
     * @return this builder
     */
    public ErrorDiagnosticsBuilder debugInfo(final DebugInfo debugInfo) {
        this.debugInfo = debugInfo;
        return this;
    }

    /**
     * Sets the runtime metrics.
     *
     * @param runtimeMetrics the runtime metrics
     * @return this builder
     */
    public ErrorDiagnosticsBuilder runtimeMetrics(final RuntimeMetrics runtimeMetrics) {
        this.runtimeMetrics = runtimeMetrics;
        return this;
    }

    /**
     * Adds a property.
     *
     * @param key the property key
     * @param value the property value
     * @return this builder
     */
    public ErrorDiagnosticsBuilder addProperty(final String key, final Object value) {
        this.properties.put(key, value);
        return this;
    }

    /**
     * Sets the properties.
     *
     * @param properties the properties
     * @return this builder
     */
    public ErrorDiagnosticsBuilder properties(final Map<String, Object> properties) {
        this.properties.clear();
        this.properties.putAll(properties);
        return this;
    }

    /**
     * Sets the collection timestamp.
     *
     * @param collectionTimestamp the collection timestamp
     * @return this builder
     */
    public ErrorDiagnosticsBuilder collectionTimestamp(final long collectionTimestamp) {
        this.collectionTimestamp = collectionTimestamp;
        return this;
    }

    /**
     * Sets whether the diagnostics contain sensitive information.
     *
     * @param containsSensitiveInfo true if contains sensitive information
     * @return this builder
     */
    public ErrorDiagnosticsBuilder containsSensitiveInfo(final boolean containsSensitiveInfo) {
        this.containsSensitiveInfo = containsSensitiveInfo;
        return this;
    }

    /**
     * Builds the ErrorDiagnostics instance.
     *
     * @return the constructed ErrorDiagnostics
     * @throws IllegalStateException if required fields are missing
     */
    public ErrorDiagnostics build() {
        if (errorCode == null) {
            throw new IllegalStateException("Error code is required");
        }
        if (description == null) {
            throw new IllegalStateException("Description is required");
        }

        return new ErrorDiagnosticsImpl(
            level,
            errorCode,
            description,
            rootCauseAnalysis,
            new ArrayList<>(validationIssues),
            performanceImpact,
            new ArrayList<>(suggestedFixes),
            new ArrayList<>(documentationLinks),
            new ArrayList<>(similarIssues),
            debugInfo,
            runtimeMetrics,
            new HashMap<>(properties),
            collectionTimestamp,
            containsSensitiveInfo
        );
    }

    /**
     * Internal implementation of ErrorDiagnostics.
     */
    private static final class ErrorDiagnosticsImpl implements ErrorDiagnostics {
        private final Level level;
        private final String errorCode;
        private final String description;
        private final String rootCauseAnalysis;
        private final List<ValidationIssue> validationIssues;
        private final PerformanceImpact performanceImpact;
        private final List<SuggestedFix> suggestedFixes;
        private final List<String> documentationLinks;
        private final List<KnownIssue> similarIssues;
        private final DebugInfo debugInfo;
        private final RuntimeMetrics runtimeMetrics;
        private final Map<String, Object> properties;
        private final long collectionTimestamp;
        private final boolean containsSensitiveInfo;

        private ErrorDiagnosticsImpl(final Level level, final String errorCode, final String description,
                                    final String rootCauseAnalysis, final List<ValidationIssue> validationIssues,
                                    final PerformanceImpact performanceImpact, final List<SuggestedFix> suggestedFixes,
                                    final List<String> documentationLinks, final List<KnownIssue> similarIssues,
                                    final DebugInfo debugInfo, final RuntimeMetrics runtimeMetrics,
                                    final Map<String, Object> properties, final long collectionTimestamp,
                                    final boolean containsSensitiveInfo) {
            this.level = level;
            this.errorCode = errorCode;
            this.description = description;
            this.rootCauseAnalysis = rootCauseAnalysis;
            this.validationIssues = validationIssues;
            this.performanceImpact = performanceImpact;
            this.suggestedFixes = suggestedFixes;
            this.documentationLinks = documentationLinks;
            this.similarIssues = similarIssues;
            this.debugInfo = debugInfo;
            this.runtimeMetrics = runtimeMetrics;
            this.properties = properties;
            this.collectionTimestamp = collectionTimestamp;
            this.containsSensitiveInfo = containsSensitiveInfo;
        }

        @Override
        public Level getLevel() {
            return level;
        }

        @Override
        public String getErrorCode() {
            return errorCode;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public Optional<String> getRootCauseAnalysis() {
            return Optional.ofNullable(rootCauseAnalysis);
        }

        @Override
        public List<ValidationIssue> getValidationIssues() {
            return new ArrayList<>(validationIssues);
        }

        @Override
        public Optional<PerformanceImpact> getPerformanceImpact() {
            return Optional.ofNullable(performanceImpact);
        }

        @Override
        public List<SuggestedFix> getSuggestedFixes() {
            return new ArrayList<>(suggestedFixes);
        }

        @Override
        public List<String> getDocumentationLinks() {
            return new ArrayList<>(documentationLinks);
        }

        @Override
        public List<KnownIssue> getSimilarIssues() {
            return new ArrayList<>(similarIssues);
        }

        @Override
        public Optional<DebugInfo> getDebugInfo() {
            return Optional.ofNullable(debugInfo);
        }

        @Override
        public Optional<RuntimeMetrics> getRuntimeMetrics() {
            return Optional.ofNullable(runtimeMetrics);
        }

        @Override
        public Map<String, Object> getProperties() {
            return new HashMap<>(properties);
        }

        @Override
        public long getCollectionTimestamp() {
            return collectionTimestamp;
        }

        @Override
        public boolean containsSensitiveInfo() {
            return containsSensitiveInfo;
        }

        @Override
        public ErrorDiagnostics sanitized() {
            if (!containsSensitiveInfo) {
                return this;
            }

            // Create a sanitized version with sensitive information removed
            final Map<String, Object> sanitizedProperties = new HashMap<>(properties);
            sanitizedProperties.entrySet().removeIf(entry ->
                entry.getKey().toLowerCase().contains("password") ||
                entry.getKey().toLowerCase().contains("secret") ||
                entry.getKey().toLowerCase().contains("token") ||
                entry.getKey().toLowerCase().contains("key")
            );

            return new ErrorDiagnosticsImpl(
                level,
                errorCode,
                description,
                rootCauseAnalysis,
                validationIssues,
                performanceImpact,
                suggestedFixes,
                documentationLinks,
                similarIssues,
                debugInfo,
                runtimeMetrics,
                sanitizedProperties,
                collectionTimestamp,
                false // No longer contains sensitive info after sanitization
            );
        }
    }
}