/**
 * Enterprise security features for WebAssembly module execution.
 *
 * <p>This package provides comprehensive security functionality including:
 *
 * <h2>Module Signing and Verification</h2>
 *
 * <ul>
 *   <li>{@link ai.tegmentum.wasmtime4j.security.ModuleSignature} - Cryptographic signatures
 *   <li>{@link ai.tegmentum.wasmtime4j.security.SignatureAlgorithm} - Supported algorithms
 *   <li>Trust store management for certificate validation
 * </ul>
 *
 * <h2>Access Control and Authorization</h2>
 *
 * <ul>
 *   <li>Role-Based Access Control (RBAC)
 *   <li>Attribute-Based Access Control (ABAC)
 *   <li>Session management with token-based authentication
 *   <li>Enterprise identity provider integration
 * </ul>
 *
 * <h2>Secure Sandboxing</h2>
 *
 * <ul>
 *   <li>{@link ai.tegmentum.wasmtime4j.security.SecurityContext} - Execution contexts
 *   <li>{@link ai.tegmentum.wasmtime4j.security.Capability} - Fine-grained permissions
 *   <li>Resource limits and monitoring
 *   <li>Capability-based security model
 * </ul>
 *
 * <h2>Audit and Compliance</h2>
 *
 * <ul>
 *   <li>Comprehensive audit logging
 *   <li>Compliance reporting for SOX, GDPR, HIPAA
 *   <li>Tamper-evident logging with cryptographic protection
 *   <li>Security event correlation and alerting
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Create a security manager
 * SecurityManager securityManager = SecurityManager.builder()
 *     .requireSignatures(true)
 *     .auditLogging(true)
 *     .strictSandboxMode(true)
 *     .build();
 *
 * // Create a security context
 * SecurityContext context = SecurityContext.builder("module-1", 5)
 *     .grantCapability(Capability.memoryAccess(1024 * 1024, false))
 *     .grantCapability(Capability.fileSystemAccess())
 *     .expiresIn(Duration.ofHours(1))
 *     .build();
 *
 * // Create a secure sandbox
 * String sandboxId = securityManager.createSandbox("module-1", context);
 *
 * // Check capabilities
 * boolean hasMemory = securityManager.hasCapability(
 *     sandboxId, Capability.memoryAccess(512 * 1024, false));
 * }</pre>
 *
 * <h2>Security Principles</h2>
 *
 * <p>The security system follows these key principles:
 *
 * <ul>
 *   <li><strong>Least Privilege</strong> - Grant minimal necessary permissions
 *   <li><strong>Defense in Depth</strong> - Multiple layers of security controls
 *   <li><strong>Fail Secure</strong> - Default to deny when in doubt
 *   <li><strong>Audit Everything</strong> - Log all security-relevant events
 *   <li><strong>Zero Trust</strong> - Verify all operations regardless of source
 * </ul>
 *
 * @since 1.0.0
 */
package ai.tegmentum.wasmtime4j.security;
