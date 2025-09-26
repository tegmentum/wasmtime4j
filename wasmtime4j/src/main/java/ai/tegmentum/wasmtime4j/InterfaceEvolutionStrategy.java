/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Strategy definition for interface evolution between WIT interface versions.
 *
 * <p>This class defines the overall strategy, rules, and constraints for evolving
 * interfaces over time while maintaining compatibility and system stability.
 *
 * @since 1.0.0
 */
public final class InterfaceEvolutionStrategy {

  private final String strategyName;
  private final EvolutionApproach approach;
  private final Set<EvolutionRule> rules;
  private final Map<String, Object> parameters;
  private final Duration timeWindow;
  private final int maxConcurrentEvolutions;
  private final Set<String> criticalInterfaces;
  private final boolean enableAutomaticRollback;
  private final double successThreshold;
  private final List<EvolutionPhase> phases;

  private InterfaceEvolutionStrategy(Builder builder) {
    this.strategyName = builder.strategyName;
    this.approach = builder.approach;
    this.rules = Set.copyOf(builder.rules);
    this.parameters = Map.copyOf(builder.parameters);
    this.timeWindow = builder.timeWindow;
    this.maxConcurrentEvolutions = builder.maxConcurrentEvolutions;
    this.criticalInterfaces = Set.copyOf(builder.criticalInterfaces);
    this.enableAutomaticRollback = builder.enableAutomaticRollback;
    this.successThreshold = builder.successThreshold;
    this.phases = List.copyOf(builder.phases);
  }

  /**
   * Gets the strategy name.
   *
   * @return strategy name
   */
  public String getStrategyName() {
    return strategyName;
  }

  /**
   * Gets the evolution approach.
   *
   * @return evolution approach
   */
  public EvolutionApproach getApproach() {
    return approach;
  }

  /**
   * Gets the evolution rules.
   *
   * @return evolution rules set
   */
  public Set<EvolutionRule> getRules() {
    return rules;
  }

  /**
   * Gets the strategy parameters.
   *
   * @return strategy parameters map
   */
  public Map<String, Object> getParameters() {
    return parameters;
  }

  /**
   * Gets the time window for evolution operations.
   *
   * @return time window
   */
  public Duration getTimeWindow() {
    return timeWindow;
  }

  /**
   * Gets the maximum number of concurrent evolutions.
   *
   * @return maximum concurrent evolutions
   */
  public int getMaxConcurrentEvolutions() {
    return maxConcurrentEvolutions;
  }

  /**
   * Gets the critical interfaces that require special handling.
   *
   * @return critical interfaces set
   */
  public Set<String> getCriticalInterfaces() {
    return criticalInterfaces;
  }

  /**
   * Gets whether automatic rollback is enabled.
   *
   * @return true if automatic rollback is enabled
   */
  public boolean isEnableAutomaticRollback() {
    return enableAutomaticRollback;
  }

  /**
   * Gets the success threshold for evolution operations.
   *
   * @return success threshold (0.0 to 1.0)
   */
  public double getSuccessThreshold() {
    return successThreshold;
  }

  /**
   * Gets the evolution phases.
   *
   * @return evolution phases list
   */
  public List<EvolutionPhase> getPhases() {
    return phases;
  }

  /**
   * Creates a new builder for InterfaceEvolutionStrategy.
   *
   * @param strategyName the strategy name
   * @return new builder instance
   */
  public static Builder builder(String strategyName) {
    return new Builder(strategyName);
  }

  /**
   * Creates a conservative evolution strategy.
   *
   * @return conservative strategy
   */
  public static InterfaceEvolutionStrategy conservative() {
    return builder("conservative")
        .approach(EvolutionApproach.INCREMENTAL)
        .enableAutomaticRollback(true)
        .successThreshold(0.95)
        .maxConcurrentEvolutions(1)
        .addRule(EvolutionRule.NO_BREAKING_CHANGES)
        .addRule(EvolutionRule.BACKWARD_COMPATIBILITY_REQUIRED)
        .build();
  }

  /**
   * Creates an aggressive evolution strategy.
   *
   * @return aggressive strategy
   */
  public static InterfaceEvolutionStrategy aggressive() {
    return builder("aggressive")
        .approach(EvolutionApproach.DIRECT)
        .enableAutomaticRollback(false)
        .successThreshold(0.8)
        .maxConcurrentEvolutions(5)
        .addRule(EvolutionRule.ALLOW_BREAKING_CHANGES)
        .build();
  }

  /**
   * Evolution approach enum.
   */
  public enum EvolutionApproach {
    /** Incremental evolution with small steps */
    INCREMENTAL,
    /** Direct evolution in single operation */
    DIRECT,
    /** Parallel evolution with multiple paths */
    PARALLEL,
    /** Adaptive evolution based on runtime conditions */
    ADAPTIVE
  }

  /**
   * Evolution rule enum.
   */
  public enum EvolutionRule {
    /** No breaking changes allowed */
    NO_BREAKING_CHANGES,
    /** Breaking changes allowed with explicit approval */
    ALLOW_BREAKING_CHANGES,
    /** Backward compatibility must be maintained */
    BACKWARD_COMPATIBILITY_REQUIRED,
    /** Forward compatibility preferred */
    FORWARD_COMPATIBILITY_PREFERRED,
    /** Require validation before evolution */
    REQUIRE_VALIDATION,
    /** Require testing before evolution */
    REQUIRE_TESTING,
    /** Require approval for critical interfaces */
    REQUIRE_CRITICAL_APPROVAL,
    /** Enable automatic adaptation generation */
    ENABLE_AUTO_ADAPTATION,
    /** Preserve function signatures */
    PRESERVE_FUNCTION_SIGNATURES,
    /** Preserve type definitions */
    PRESERVE_TYPE_DEFINITIONS
  }

  /**
   * Evolution phase definition.
   */
  public static final class EvolutionPhase {
    private final String phaseName;
    private final PhaseType type;
    private final Duration estimatedDuration;
    private final Set<String> prerequisites;
    private final Set<String> actions;
    private final Map<String, Object> phaseParameters;

    public EvolutionPhase(String phaseName, PhaseType type, Duration estimatedDuration,
                          Set<String> prerequisites, Set<String> actions, Map<String, Object> phaseParameters) {
      this.phaseName = phaseName;
      this.type = type;
      this.estimatedDuration = estimatedDuration;
      this.prerequisites = Set.copyOf(prerequisites);
      this.actions = Set.copyOf(actions);
      this.phaseParameters = Map.copyOf(phaseParameters);
    }

    public String getPhaseName() {
      return phaseName;
    }

    public PhaseType getType() {
      return type;
    }

    public Duration getEstimatedDuration() {
      return estimatedDuration;
    }

    public Set<String> getPrerequisites() {
      return prerequisites;
    }

    public Set<String> getActions() {
      return actions;
    }

    public Map<String, Object> getPhaseParameters() {
      return phaseParameters;
    }
  }

  /**
   * Phase type enum.
   */
  public enum PhaseType {
    /** Preparation phase */
    PREPARATION,
    /** Analysis phase */
    ANALYSIS,
    /** Planning phase */
    PLANNING,
    /** Execution phase */
    EXECUTION,
    /** Validation phase */
    VALIDATION,
    /** Deployment phase */
    DEPLOYMENT,
    /** Monitoring phase */
    MONITORING,
    /** Cleanup phase */
    CLEANUP
  }

  /**
   * Builder for InterfaceEvolutionStrategy.
   */
  public static final class Builder {
    private final String strategyName;
    private EvolutionApproach approach = EvolutionApproach.INCREMENTAL;
    private Set<EvolutionRule> rules = Set.of();
    private Map<String, Object> parameters = Map.of();
    private Duration timeWindow = Duration.ofHours(1);
    private int maxConcurrentEvolutions = 3;
    private Set<String> criticalInterfaces = Set.of();
    private boolean enableAutomaticRollback = true;
    private double successThreshold = 0.9;
    private List<EvolutionPhase> phases = List.of();

    private Builder(String strategyName) {
      if (strategyName == null || strategyName.trim().isEmpty()) {
        throw new IllegalArgumentException("Strategy name cannot be null or empty");
      }
      this.strategyName = strategyName.trim();
    }

    /**
     * Sets the evolution approach.
     *
     * @param approach the evolution approach
     * @return this builder
     */
    public Builder approach(EvolutionApproach approach) {
      this.approach = approach != null ? approach : EvolutionApproach.INCREMENTAL;
      return this;
    }

    /**
     * Sets the evolution rules.
     *
     * @param rules the evolution rules
     * @return this builder
     */
    public Builder rules(Set<EvolutionRule> rules) {
      this.rules = rules != null ? Set.copyOf(rules) : Set.of();
      return this;
    }

    /**
     * Adds an evolution rule.
     *
     * @param rule the rule to add
     * @return this builder
     */
    public Builder addRule(EvolutionRule rule) {
      if (rule != null) {
        this.rules = Set.of(this.rules.toArray(new EvolutionRule[0])) // Convert to mutable
            .stream()
            .collect(java.util.stream.Collectors.toSet());
        ((java.util.Set<EvolutionRule>) this.rules).add(rule);
        this.rules = Set.copyOf(this.rules);
      }
      return this;
    }

    /**
     * Sets the strategy parameters.
     *
     * @param parameters the strategy parameters
     * @return this builder
     */
    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
      return this;
    }

    /**
     * Sets the time window.
     *
     * @param timeWindow the time window
     * @return this builder
     */
    public Builder timeWindow(Duration timeWindow) {
      this.timeWindow = timeWindow != null ? timeWindow : Duration.ofHours(1);
      return this;
    }

    /**
     * Sets the maximum concurrent evolutions.
     *
     * @param maxConcurrentEvolutions the maximum concurrent evolutions
     * @return this builder
     */
    public Builder maxConcurrentEvolutions(int maxConcurrentEvolutions) {
      this.maxConcurrentEvolutions = Math.max(1, maxConcurrentEvolutions);
      return this;
    }

    /**
     * Sets the critical interfaces.
     *
     * @param criticalInterfaces the critical interfaces
     * @return this builder
     */
    public Builder criticalInterfaces(Set<String> criticalInterfaces) {
      this.criticalInterfaces = criticalInterfaces != null ? Set.copyOf(criticalInterfaces) : Set.of();
      return this;
    }

    /**
     * Sets whether to enable automatic rollback.
     *
     * @param enableAutomaticRollback true to enable automatic rollback
     * @return this builder
     */
    public Builder enableAutomaticRollback(boolean enableAutomaticRollback) {
      this.enableAutomaticRollback = enableAutomaticRollback;
      return this;
    }

    /**
     * Sets the success threshold.
     *
     * @param successThreshold the success threshold (0.0 to 1.0)
     * @return this builder
     */
    public Builder successThreshold(double successThreshold) {
      this.successThreshold = Math.max(0.0, Math.min(1.0, successThreshold));
      return this;
    }

    /**
     * Sets the evolution phases.
     *
     * @param phases the evolution phases
     * @return this builder
     */
    public Builder phases(List<EvolutionPhase> phases) {
      this.phases = phases != null ? List.copyOf(phases) : List.of();
      return this;
    }

    /**
     * Builds the InterfaceEvolutionStrategy.
     *
     * @return the configured InterfaceEvolutionStrategy
     */
    public InterfaceEvolutionStrategy build() {
      return new InterfaceEvolutionStrategy(this);
    }
  }
}