package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Configuration for individual template components. */
final class ComponentConfiguration {
  private final boolean conditional;
  private final String condition;
  private final int order;
  private final Set<String> requiredData;
  private final Map<String, Object> defaultData;

  private ComponentConfiguration(final Builder builder) {
    this.conditional = builder.conditional;
    this.condition = builder.condition;
    this.order = builder.order;
    this.requiredData = Set.copyOf(builder.requiredData);
    this.defaultData = Map.copyOf(builder.defaultData);
  }

  public boolean isConditional() {
    return conditional;
  }

  public String getCondition() {
    return condition;
  }

  public int getOrder() {
    return order;
  }

  public Set<String> getRequiredData() {
    return requiredData;
  }

  public Map<String, Object> getDefaultData() {
    return defaultData;
  }

  /** Creates default component configuration. */
  public static ComponentConfiguration defaultConfig() {
    return new Builder().conditional(false).order(0).build();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ComponentConfiguration that = (ComponentConfiguration) obj;
    return conditional == that.conditional
        && order == that.order
        && Objects.equals(condition, that.condition)
        && Objects.equals(requiredData, that.requiredData)
        && Objects.equals(defaultData, that.defaultData);
  }

  @Override
  public int hashCode() {
    return Objects.hash(conditional, condition, order, requiredData, defaultData);
  }

  @Override
  public String toString() {
    return "ComponentConfiguration{" + "conditional=" + conditional + ", order=" + order + '}';
  }

  /** Builder for ComponentConfiguration. */
  public static final class Builder {
    private boolean conditional = false;
    private String condition = "";
    private int order = 0;
    private Set<String> requiredData = Collections.emptySet();
    private Map<String, Object> defaultData = Collections.emptyMap();

    public Builder conditional(final boolean conditional) {
      this.conditional = conditional;
      return this;
    }

    public Builder condition(final String condition) {
      this.condition = condition;
      return this;
    }

    public Builder order(final int order) {
      this.order = order;
      return this;
    }

    public Builder requiredData(final Set<String> requiredData) {
      this.requiredData = Objects.requireNonNull(requiredData, "requiredData cannot be null");
      return this;
    }

    public Builder defaultData(final Map<String, Object> defaultData) {
      this.defaultData = Objects.requireNonNull(defaultData, "defaultData cannot be null");
      return this;
    }

    public ComponentConfiguration build() {
      return new ComponentConfiguration(this);
    }
  }
}
