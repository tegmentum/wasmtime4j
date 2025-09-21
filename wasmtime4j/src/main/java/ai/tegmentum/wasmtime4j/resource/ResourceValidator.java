package ai.tegmentum.wasmtime4j.resource;

/**
 * Function interface for validating resources in the pool.
 *
 * <p>ResourceValidator provides a way to check if a pooled resource is still
 * valid and safe to use. Invalid resources are discarded from the pool instead
 * of being returned for reuse.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ResourceValidator<Module> moduleValidator = module -> {
 *     try {
 *         // Check if module is still valid by accessing its exports
 *         module.getExports();
 *         return true;
 *     } catch (Exception e) {
 *         return false;
 *     }
 * };
 *
 * pool.registerFactory(ResourceType.MODULE, moduleFactory, moduleValidator);
 * }</pre>
 *
 * @param <T> the type of resource to validate
 * @since 1.0.0
 */
@FunctionalInterface
public interface ResourceValidator<T> {

    /**
     * Validates a resource to determine if it's still usable.
     *
     * <p>This method should perform appropriate checks to determine if the
     * resource is in a valid state. The validation should be lightweight
     * as it may be called frequently during pool operations.
     *
     * @param resource the resource to validate
     * @return true if the resource is valid, false otherwise
     */
    boolean isValid(final T resource);

    /**
     * Creates a validator that always returns true (no validation).
     *
     * @param <T> the resource type
     * @return a validator that accepts all resources
     */
    static <T> ResourceValidator<T> alwaysValid() {
        return resource -> true;
    }

    /**
     * Creates a validator that always returns false (reject all).
     *
     * @param <T> the resource type
     * @return a validator that rejects all resources
     */
    static <T> ResourceValidator<T> alwaysInvalid() {
        return resource -> false;
    }

    /**
     * Creates a validator that checks if the resource is not null.
     *
     * @param <T> the resource type
     * @return a validator that checks for null
     */
    static <T> ResourceValidator<T> notNull() {
        return resource -> resource != null;
    }

    /**
     * Combines this validator with another using logical AND.
     *
     * <p>The combined validator returns true only if both validators return true.
     *
     * @param other the other validator to combine with
     * @return a combined validator using AND logic
     * @throws IllegalArgumentException if other is null
     */
    default ResourceValidator<T> and(final ResourceValidator<T> other) {
        if (other == null) {
            throw new IllegalArgumentException("Other validator cannot be null");
        }
        return resource -> this.isValid(resource) && other.isValid(resource);
    }

    /**
     * Combines this validator with another using logical OR.
     *
     * <p>The combined validator returns true if either validator returns true.
     *
     * @param other the other validator to combine with
     * @return a combined validator using OR logic
     * @throws IllegalArgumentException if other is null
     */
    default ResourceValidator<T> or(final ResourceValidator<T> other) {
        if (other == null) {
            throw new IllegalArgumentException("Other validator cannot be null");
        }
        return resource -> this.isValid(resource) || other.isValid(resource);
    }

    /**
     * Creates a validator that negates this validator's result.
     *
     * @return a validator that returns the opposite result
     */
    default ResourceValidator<T> negate() {
        return resource -> !this.isValid(resource);
    }
}