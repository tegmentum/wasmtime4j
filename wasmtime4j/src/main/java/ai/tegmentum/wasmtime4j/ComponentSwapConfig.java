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
import java.util.Map;
import java.util.Set;

/**
 * Configuration for component hot-swapping operations.
 *
 * <p>This configuration controls how component hot-swapping is performed, including
 * safety checks, rollback strategies, and state preservation options.
 *
 * @since 1.0.0
 */
public final class ComponentSwapConfig {

  private final SwapStrategy strategy;
  private final Duration maxSwapTime;
  private final boolean preserveState;
  private final RollbackStrategy rollbackStrategy;
  private final Set<StatePreservationScope> stateScopes;
  private final Map<String, Object> swapParameters;
  private final boolean enableGracefulShutdown;
  private final Duration gracefulShutdownTimeout;
  private final CompatibilityCheckLevel compatibilityCheck;
  private final boolean allowInterfaceMigration;

  private ComponentSwapConfig(Builder builder) {
    this.strategy = builder.strategy;
    this.maxSwapTime = builder.maxSwapTime;
    this.preserveState = builder.preserveState;
    this.rollbackStrategy = builder.rollbackStrategy;
    this.stateScopes = Set.copyOf(builder.stateScopes);
    this.swapParameters = Map.copyOf(builder.swapParameters);
    this.enableGracefulShutdown = builder.enableGracefulShutdown;
    this.gracefulShutdownTimeout = builder.gracefulShutdownTimeout;
    this.compatibilityCheck = builder.compatibilityCheck;
    this.allowInterfaceMigration = builder.allowInterfaceMigration;
  }

  /**
   * Gets the swap strategy.
   *
   * @return the swap strategy
   */
  public SwapStrategy getStrategy() {
    return strategy;
  }

  /**
   * Gets the maximum swap time allowed.
   *
   * @return the maximum swap time
   */
  public Duration getMaxSwapTime() {
    return maxSwapTime;
  }

  /**
   * Checks if state should be preserved during swap.
   *
   * @return true if state should be preserved
   */
  public boolean isPreserveState() {
    return preserveState;
  }

  /**
   * Gets the rollback strategy.
   *
   * @return the rollback strategy
   */
  public RollbackStrategy getRollbackStrategy() {
    return rollbackStrategy;
  }

  /**
   * Gets the state preservation scopes.
   *
   * @return set of state preservation scopes
   */
  public Set<StatePreservationScope> getStateScopes() {
    return stateScopes;
  }

  /**
   * Gets the swap parameters.
   *
   * @return map of swap parameters
   */
  public Map<String, Object> getSwapParameters() {
    return swapParameters;
  }

  /**
   * Checks if graceful shutdown is enabled.
   *
   * @return true if graceful shutdown is enabled
   */
  public boolean isGracefulShutdownEnabled() {
    return enableGracefulShutdown;
  }

  /**
   * Gets the graceful shutdown timeout.
   *
   * @return the graceful shutdown timeout
   */
  public Duration getGracefulShutdownTimeout() {
    return gracefulShutdownTimeout;
  }

  /**
   * Gets the compatibility check level.
   *
   * @return the compatibility check level
   */
  public CompatibilityCheckLevel getCompatibilityCheck() {
    return compatibilityCheck;
  }

  /**
   * Checks if interface migration is allowed.
   *
   * @return true if interface migration is allowed
   */
  public boolean isInterfaceMigrationAllowed() {
    return allowInterfaceMigration;
  }

  /**
   * Creates a new builder for ComponentSwapConfig.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a safe configuration for production hot-swapping.
   *
   * @return safe production configuration
   */
  public static ComponentSwapConfig safeProductionConfig() {
    return builder()
        .strategy(SwapStrategy.BLUE_GREEN)
        .maxSwapTime(Duration.ofSeconds(30))
        .preserveState(true)
        .rollbackStrategy(RollbackStrategy.AUTOMATIC)
        .enableGracefulShutdown(true)
        .gracefulShutdownTimeout(Duration.ofSeconds(15))
        .compatibilityCheck(CompatibilityCheckLevel.STRICT)
        .build();
  }

  /**
   * Creates a fast configuration for development hot-swapping.
   *
   * @return fast development configuration
   */
  public static ComponentSwapConfig fastDevelopmentConfig() {
    return builder()
        .strategy(SwapStrategy.DIRECT_REPLACEMENT)
        .maxSwapTime(Duration.ofSeconds(5))
        .preserveState(false)
        .rollbackStrategy(RollbackStrategy.MANUAL)
        .compatibilityCheck(CompatibilityCheckLevel.BASIC)
        .allowInterfaceMigration(true)
        .build();
  }

  /**
   * Builder for ComponentSwapConfig.
   */
  public static final class Builder {
    private SwapStrategy strategy = SwapStrategy.ROLLING_UPDATE;
    private Duration maxSwapTime = Duration.ofMinutes(1);
    private boolean preserveState = true;
    private RollbackStrategy rollbackStrategy = RollbackStrategy.AUTOMATIC;
    private Set<StatePreservationScope> stateScopes = Set.of(StatePreservationScope.COMPONENT_STATE);
    private Map<String, Object> swapParameters = Map.of();
    private boolean enableGracefulShutdown = true;
    private Duration gracefulShutdownTimeout = Duration.ofSeconds(30);
    private CompatibilityCheckLevel compatibilityCheck = CompatibilityCheckLevel.MODERATE;
    private boolean allowInterfaceMigration = false;

    private Builder() {}

    public Builder strategy(SwapStrategy strategy) {
      this.strategy = strategy;
      return this;
    }

    public Builder maxSwapTime(Duration maxSwapTime) {
      this.maxSwapTime = maxSwapTime;
      return this;
    }

    public Builder preserveState(boolean preserveState) {
      this.preserveState = preserveState;
      return this;
    }

    public Builder rollbackStrategy(RollbackStrategy rollbackStrategy) {
      this.rollbackStrategy = rollbackStrategy;
      return this;
    }

    public Builder stateScopes(Set<StatePreservationScope> stateScopes) {
      this.stateScopes = stateScopes;
      return this;
    }

    public Builder swapParameters(Map<String, Object> swapParameters) {
      this.swapParameters = swapParameters;
      return this;
    }

    public Builder enableGracefulShutdown(boolean enableGracefulShutdown) {
      this.enableGracefulShutdown = enableGracefulShutdown;
      return this;
    }

    public Builder gracefulShutdownTimeout(Duration gracefulShutdownTimeout) {
      this.gracefulShutdownTimeout = gracefulShutdownTimeout;
      return this;
    }

    public Builder compatibilityCheck(CompatibilityCheckLevel compatibilityCheck) {
      this.compatibilityCheck = compatibilityCheck;
      return this;
    }

    public Builder allowInterfaceMigration(boolean allowInterfaceMigration) {
      this.allowInterfaceMigration = allowInterfaceMigration;
      return this;
    }

    public ComponentSwapConfig build() {
      return new ComponentSwapConfig(this);
    }
  }

  /**
   * Hot-swap strategies.
   */
  public enum SwapStrategy {
    /** Direct replacement without intermediate states */
    DIRECT_REPLACEMENT,
    /** Blue-green deployment with complete switchover */
    BLUE_GREEN,
    /** Rolling update with gradual replacement */
    ROLLING_UPDATE,
    /** Canary deployment with gradual traffic shifting */
    CANARY,
    /** A/B testing with controlled rollout */
    AB_TESTING
  }

  /**
   * Rollback strategies.
   */
  public enum RollbackStrategy {
    /** No automatic rollback */
    NONE,
    /** Manual rollback only */
    MANUAL,
    /** Automatic rollback on failure */
    AUTOMATIC,
    /** Automatic rollback with health checks */
    AUTOMATIC_WITH_HEALTH_CHECKS
  }

  /**
   * State preservation scopes.
   */
  public enum StatePreservationScope {
    /** Preserve component instance state */
    COMPONENT_STATE,
    /** Preserve shared resource state */
    SHARED_RESOURCES,
    /** Preserve connection state */
    CONNECTIONS,
    /** Preserve cached data */
    CACHE,
    /** Preserve configuration */
    CONFIGURATION,
    /** Preserve all state */
    ALL
  }

  /**
   * Compatibility check levels.
   */
  public enum CompatibilityCheckLevel {
    /** No compatibility checking */
    NONE,
    /** Basic interface compatibility */
    BASIC,
    /** Moderate compatibility with version checks */
    MODERATE,
    /** Strict compatibility validation */
    STRICT,
    /** Complete compatibility including behavior verification */
    COMPLETE
  }
}