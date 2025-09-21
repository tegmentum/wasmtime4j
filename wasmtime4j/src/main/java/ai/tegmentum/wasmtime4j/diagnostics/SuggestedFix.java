package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.List;
import java.util.Optional;

/**
 * Represents a suggested fix for a WebAssembly issue.
 *
 * <p>This interface provides actionable suggestions for resolving errors,
 * validation issues, and other problems in WebAssembly code.
 *
 * @since 1.0.0
 */
public interface SuggestedFix {

    /**
     * Types of suggested fixes.
     */
    enum FixType {
        /** Code modification suggestion */
        CODE_CHANGE,
        /** Configuration change suggestion */
        CONFIG_CHANGE,
        /** Environment setup suggestion */
        ENVIRONMENT_SETUP,
        /** Dependency update suggestion */
        DEPENDENCY_UPDATE,
        /** Feature enablement suggestion */
        FEATURE_ENABLE,
        /** Resource allocation suggestion */
        RESOURCE_ALLOCATION,
        /** Documentation reference */
        DOCUMENTATION,
        /** Manual intervention required */
        MANUAL_ACTION
    }

    /**
     * Priority levels for suggested fixes.
     */
    enum Priority {
        /** Low priority suggestion */
        LOW,
        /** Medium priority suggestion */
        MEDIUM,
        /** High priority suggestion */
        HIGH,
        /** Critical fix that should be applied immediately */
        CRITICAL
    }

    /**
     * Gets the unique fix identifier.
     *
     * @return the fix ID
     */
    String getFixId();

    /**
     * Gets the fix title.
     *
     * @return the fix title
     */
    String getTitle();

    /**
     * Gets the detailed fix description.
     *
     * @return the fix description
     */
    String getDescription();

    /**
     * Gets the fix type.
     *
     * @return the fix type
     */
    FixType getType();

    /**
     * Gets the fix priority.
     *
     * @return the fix priority
     */
    Priority getPriority();

    /**
     * Gets the step-by-step instructions for applying the fix.
     *
     * @return list of fix steps
     */
    List<String> getSteps();

    /**
     * Gets the code changes required for this fix.
     *
     * @return the code changes, or empty if not applicable
     */
    Optional<CodeChange> getCodeChange();

    /**
     * Gets the configuration changes required for this fix.
     *
     * @return the configuration changes, or empty if not applicable
     */
    Optional<ConfigurationChange> getConfigurationChange();

    /**
     * Gets the estimated effort to apply this fix.
     *
     * @return the effort estimate, or empty if not estimated
     */
    Optional<EffortEstimate> getEffortEstimate();

    /**
     * Gets the potential side effects of applying this fix.
     *
     * @return list of potential side effects
     */
    List<String> getPotentialSideEffects();

    /**
     * Gets related documentation links.
     *
     * @return list of documentation URLs
     */
    List<String> getDocumentationLinks();

    /**
     * Gets example code demonstrating the fix.
     *
     * @return the example code, or empty if not available
     */
    Optional<String> getExampleCode();

    /**
     * Checks if this fix can be applied automatically.
     *
     * @return true if the fix can be applied automatically
     */
    boolean isAutoApplicable();

    /**
     * Checks if this fix requires manual intervention.
     *
     * @return true if manual intervention is required
     */
    boolean requiresManualIntervention();

    /**
     * Checks if this fix is experimental or untested.
     *
     * @return true if the fix is experimental
     */
    boolean isExperimental();

    /**
     * Gets the compatibility information for this fix.
     *
     * @return the compatibility info, or empty if not available
     */
    Optional<CompatibilityInfo> getCompatibilityInfo();

    /**
     * Creates a builder for constructing SuggestedFix instances.
     *
     * @return a new suggested fix builder
     */
    static SuggestedFixBuilder builder() {
        return new SuggestedFixBuilder();
    }

    /**
     * Creates a simple SuggestedFix with basic information.
     *
     * @param fixId the fix ID
     * @param title the fix title
     * @param description the fix description
     * @param type the fix type
     * @param priority the fix priority
     * @return the suggested fix
     */
    static SuggestedFix of(final String fixId, final String title, final String description,
                          final FixType type, final Priority priority) {
        return builder()
            .fixId(fixId)
            .title(title)
            .description(description)
            .type(type)
            .priority(priority)
            .build();
    }
}