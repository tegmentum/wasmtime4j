package ai.tegmentum.wasmtime4j.security;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;

/**
 * Advanced capability management with dynamic revocation, delegation, and inheritance.
 *
 * <p>This manager implements the latest WebAssembly capability-based security model with
 * support for:
 * <ul>
 *   <li>Dynamic capability revocation and updates
 *   <li>Capability inheritance and delegation
 *   <li>Time-based capability expiration
 *   <li>Capability auditing and tracking
 *   <li>Fine-grained access control
 * </ul>
 *
 * @since 1.0.0
 */
public final class CapabilityManager {

    private static final Logger LOGGER = Logger.getLogger(CapabilityManager.class.getName());

    private final Map<String, CapabilityGrant> activeCapabilities;
    private final Map<String, Set<String>> capabilityHierarchy;
    private final Map<String, Set<String>> revokedCapabilities;
    private final SecurityManager securityManager;

    /**
     * Creates a new capability manager.
     *
     * @param securityManager the security manager for audit logging
     */
    public CapabilityManager(final SecurityManager securityManager) {
        this.activeCapabilities = new ConcurrentHashMap<>();
        this.capabilityHierarchy = new ConcurrentHashMap<>();
        this.revokedCapabilities = new ConcurrentHashMap<>();
        this.securityManager = securityManager;
    }

    /**
     * Grants a capability to a principal.
     *
     * @param principalId the principal identifier
     * @param capability the capability to grant
     * @param grantorId the grantor's identifier
     * @param expiresAt optional expiration time
     * @param delegatable whether the capability can be delegated
     * @return the capability grant identifier
     */
    public String grantCapability(final String principalId, final Capability capability,
                                 final String grantorId, final Optional<Instant> expiresAt,
                                 final boolean delegatable) {
        final String grantId = generateGrantId();
        final CapabilityGrant grant = new CapabilityGrant(
            grantId, principalId, capability, grantorId, Instant.now(),
            expiresAt, delegatable, true
        );

        activeCapabilities.put(grantId, grant);

        // Track capability hierarchy
        capabilityHierarchy.computeIfAbsent(principalId, k -> new ConcurrentSkipListSet<>())
                          .add(grantId);

        // Log capability grant
        securityManager.logAuditEvent(AuditEvent.builder()
            .eventType("capability_granted")
            .principalId(grantorId)
            .resourceId(principalId)
            .action("grant_capability")
            .result("success")
            .details(Map.of(
                "grant_id", grantId,
                "capability_type", capability.getType(),
                "delegatable", String.valueOf(delegatable)
            ))
            .build());

        LOGGER.info(String.format("Capability %s granted to principal %s by %s",
                                 capability.getType(), principalId, grantorId));

        return grantId;
    }

    /**
     * Revokes a capability grant.
     *
     * @param grantId the capability grant identifier
     * @param revokerId the revoker's identifier
     * @return true if the capability was revoked, false if it didn't exist
     */
    public boolean revokeCapability(final String grantId, final String revokerId) {
        final CapabilityGrant grant = activeCapabilities.get(grantId);
        if (grant == null) {
            return false;
        }

        // Mark as revoked
        grant.setActive(false);
        activeCapabilities.remove(grantId);

        // Add to revoked list
        revokedCapabilities.computeIfAbsent(grant.getPrincipalId(), k -> new ConcurrentSkipListSet<>())
                          .add(grantId);

        // Remove from hierarchy
        final Set<String> principalCapabilities = capabilityHierarchy.get(grant.getPrincipalId());
        if (principalCapabilities != null) {
            principalCapabilities.remove(grantId);
        }

        // Revoke all delegated capabilities
        revokeDelegatedCapabilities(grantId, revokerId);

        // Log capability revocation
        securityManager.logAuditEvent(AuditEvent.builder()
            .eventType("capability_revoked")
            .principalId(revokerId)
            .resourceId(grant.getPrincipalId())
            .action("revoke_capability")
            .result("success")
            .details(Map.of(
                "grant_id", grantId,
                "capability_type", grant.getCapability().getType(),
                "original_grantor", grant.getGrantorId()
            ))
            .build());

        LOGGER.info(String.format("Capability %s revoked from principal %s by %s",
                                 grant.getCapability().getType(), grant.getPrincipalId(), revokerId));

        return true;
    }

    /**
     * Delegates a capability from one principal to another.
     *
     * @param grantId the original capability grant identifier
     * @param delegatorId the delegator's identifier
     * @param delegateeId the delegatee's identifier
     * @param restrictedCapability optional restricted version of the capability
     * @param expiresAt optional expiration time for the delegation
     * @return the new delegation grant identifier
     * @throws SecurityException if delegation is not allowed
     */
    public String delegateCapability(final String grantId, final String delegatorId,
                                   final String delegateeId,
                                   final Optional<Capability> restrictedCapability,
                                   final Optional<Instant> expiresAt) throws SecurityException {

        final CapabilityGrant originalGrant = activeCapabilities.get(grantId);
        if (originalGrant == null) {
            throw new SecurityException("Original capability grant not found: " + grantId);
        }

        if (!originalGrant.getPrincipalId().equals(delegatorId)) {
            throw new SecurityException("Only the capability holder can delegate it");
        }

        if (!originalGrant.isDelegatable()) {
            throw new SecurityException("Capability is not delegatable");
        }

        if (!originalGrant.isActive()) {
            throw new SecurityException("Cannot delegate inactive capability");
        }

        // Check if capability is expired
        if (originalGrant.getExpiresAt().isPresent() &&
            Instant.now().isAfter(originalGrant.getExpiresAt().get())) {
            throw new SecurityException("Cannot delegate expired capability");
        }

        // Use restricted capability if provided, otherwise use original
        final Capability delegatedCapability = restrictedCapability.orElse(originalGrant.getCapability());

        // Verify restricted capability is compatible with original
        if (!delegatedCapability.isCompatibleWith(originalGrant.getCapability())) {
            throw new SecurityException("Restricted capability is not compatible with original");
        }

        // Create delegation grant
        final String delegationId = generateGrantId();
        final CapabilityGrant delegationGrant = new CapabilityGrant(
            delegationId, delegateeId, delegatedCapability, delegatorId,
            Instant.now(), expiresAt, false, true // Delegations are not further delegatable
        );

        // Set delegation relationship
        delegationGrant.setParentGrantId(grantId);

        activeCapabilities.put(delegationId, delegationGrant);

        // Track in hierarchy
        capabilityHierarchy.computeIfAbsent(delegateeId, k -> new ConcurrentSkipListSet<>())
                          .add(delegationId);

        // Log delegation
        securityManager.logAuditEvent(AuditEvent.builder()
            .eventType("capability_delegated")
            .principalId(delegatorId)
            .resourceId(delegateeId)
            .action("delegate_capability")
            .result("success")
            .details(Map.of(
                "original_grant_id", grantId,
                "delegation_grant_id", delegationId,
                "capability_type", delegatedCapability.getType()
            ))
            .build());

        LOGGER.info(String.format("Capability %s delegated from %s to %s",
                                 delegatedCapability.getType(), delegatorId, delegateeId));

        return delegationId;
    }

    /**
     * Checks if a principal has a specific capability.
     *
     * @param principalId the principal identifier
     * @param capability the capability to check
     * @return true if the principal has the capability
     */
    public boolean hasCapability(final String principalId, final Capability capability) {
        final Set<String> principalCapabilities = capabilityHierarchy.get(principalId);
        if (principalCapabilities == null) {
            return false;
        }

        final Instant now = Instant.now();

        return principalCapabilities.stream()
            .map(activeCapabilities::get)
            .filter(grant -> grant != null && grant.isActive())
            .filter(grant -> grant.getExpiresAt().map(exp -> now.isBefore(exp)).orElse(true))
            .anyMatch(grant -> isCapabilityMatch(grant.getCapability(), capability));
    }

    /**
     * Gets all active capabilities for a principal.
     *
     * @param principalId the principal identifier
     * @return set of active capabilities
     */
    public Set<Capability> getCapabilities(final String principalId) {
        final Set<String> principalCapabilities = capabilityHierarchy.get(principalId);
        if (principalCapabilities == null) {
            return Set.of();
        }

        final Instant now = Instant.now();

        return principalCapabilities.stream()
            .map(activeCapabilities::get)
            .filter(grant -> grant != null && grant.isActive())
            .filter(grant -> grant.getExpiresAt().map(exp -> now.isBefore(exp)).orElse(true))
            .map(CapabilityGrant::getCapability)
            .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Updates a capability grant with new expiration or restrictions.
     *
     * @param grantId the capability grant identifier
     * @param updaterId the updater's identifier
     * @param newExpiresAt new expiration time
     * @param newCapability new restricted capability
     * @return true if the grant was updated
     */
    public boolean updateCapability(final String grantId, final String updaterId,
                                   final Optional<Instant> newExpiresAt,
                                   final Optional<Capability> newCapability) {

        final CapabilityGrant grant = activeCapabilities.get(grantId);
        if (grant == null || !grant.isActive()) {
            return false;
        }

        // Only the original grantor or admin can update
        if (!grant.getGrantorId().equals(updaterId) && !isAdmin(updaterId)) {
            return false;
        }

        boolean updated = false;

        if (newExpiresAt.isPresent()) {
            grant.setExpiresAt(newExpiresAt);
            updated = true;
        }

        if (newCapability.isPresent()) {
            final Capability newCap = newCapability.get();
            if (newCap.isCompatibleWith(grant.getCapability())) {
                grant.setCapability(newCap);
                updated = true;
            }
        }

        if (updated) {
            // Log capability update
            securityManager.logAuditEvent(AuditEvent.builder()
                .eventType("capability_updated")
                .principalId(updaterId)
                .resourceId(grant.getPrincipalId())
                .action("update_capability")
                .result("success")
                .details(Map.of("grant_id", grantId))
                .build());

            LOGGER.info(String.format("Capability grant %s updated by %s", grantId, updaterId));
        }

        return updated;
    }

    /**
     * Performs cleanup of expired capabilities.
     *
     * @return the number of capabilities cleaned up
     */
    public int cleanupExpiredCapabilities() {
        final Instant now = Instant.now();
        int cleanedUp = 0;

        for (final CapabilityGrant grant : activeCapabilities.values()) {
            if (grant.getExpiresAt().isPresent() && now.isAfter(grant.getExpiresAt().get())) {
                revokeCapability(grant.getGrantId(), "system");
                cleanedUp++;
            }
        }

        if (cleanedUp > 0) {
            LOGGER.info(String.format("Cleaned up %d expired capabilities", cleanedUp));
        }

        return cleanedUp;
    }

    // Private helper methods

    private void revokeDelegatedCapabilities(final String parentGrantId, final String revokerId) {
        activeCapabilities.values().stream()
            .filter(grant -> parentGrantId.equals(grant.getParentGrantId()))
            .forEach(delegation -> revokeCapability(delegation.getGrantId(), revokerId));
    }

    private boolean isCapabilityMatch(final Capability granted, final Capability required) {
        if (granted.equals(required)) {
            return true;
        }

        // Check if granted capability is more permissive than required
        if (granted.getType().equals(required.getType())) {
            return granted.isCompatibleWith(required);
        }

        return false;
    }

    private boolean isAdmin(final String principalId) {
        // Check if the principal has admin capabilities
        return hasCapability(principalId, Capability.systemCallAccess(Set.of("*")));
    }

    private String generateGrantId() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Capability grant representation with metadata.
     */
    public static final class CapabilityGrant {
        private final String grantId;
        private final String principalId;
        private Capability capability;
        private final String grantorId;
        private final Instant grantedAt;
        private Optional<Instant> expiresAt;
        private final boolean delegatable;
        private boolean active;
        private String parentGrantId;

        CapabilityGrant(final String grantId, final String principalId, final Capability capability,
                       final String grantorId, final Instant grantedAt,
                       final Optional<Instant> expiresAt, final boolean delegatable,
                       final boolean active) {
            this.grantId = grantId;
            this.principalId = principalId;
            this.capability = capability;
            this.grantorId = grantorId;
            this.grantedAt = grantedAt;
            this.expiresAt = expiresAt;
            this.delegatable = delegatable;
            this.active = active;
        }

        public String getGrantId() { return grantId; }
        public String getPrincipalId() { return principalId; }
        public Capability getCapability() { return capability; }
        public String getGrantorId() { return grantorId; }
        public Instant getGrantedAt() { return grantedAt; }
        public Optional<Instant> getExpiresAt() { return expiresAt; }
        public boolean isDelegatable() { return delegatable; }
        public boolean isActive() { return active; }
        public String getParentGrantId() { return parentGrantId; }

        void setCapability(final Capability capability) { this.capability = capability; }
        void setExpiresAt(final Optional<Instant> expiresAt) { this.expiresAt = expiresAt; }
        void setActive(final boolean active) { this.active = active; }
        void setParentGrantId(final String parentGrantId) { this.parentGrantId = parentGrantId; }
    }
}