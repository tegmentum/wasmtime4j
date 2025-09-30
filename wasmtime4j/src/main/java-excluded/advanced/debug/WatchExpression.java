package ai.tegmentum.wasmtime4j.debug;

import java.util.Objects;

/**
 * Represents a watch expression for monitoring variable values during debugging.
 */
public final class WatchExpression {
    private final String name;
    private final String expression;
    private final boolean enabled;

    public WatchExpression(final String name, final String expression, final boolean enabled) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.expression = Objects.requireNonNull(expression, "Expression cannot be null");
        this.enabled = enabled;
    }

    public WatchExpression withEnabled(final boolean enabled) {
        return new WatchExpression(name, expression, enabled);
    }

    // Getters
    public String getName() { return name; }
    public String getExpression() { return expression; }
    public boolean isEnabled() { return enabled; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof WatchExpression)) return false;
        final WatchExpression that = (WatchExpression) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return String.format("WatchExpression{name='%s', expression='%s', enabled=%s}",
                           name, expression, enabled);
    }
}