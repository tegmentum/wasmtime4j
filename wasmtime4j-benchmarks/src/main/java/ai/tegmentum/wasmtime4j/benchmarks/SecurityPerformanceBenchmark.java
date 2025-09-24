package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.security.AdvancedSandbox;
import ai.tegmentum.wasmtime4j.security.Capability;
import ai.tegmentum.wasmtime4j.security.CapabilityManager;
import ai.tegmentum.wasmtime4j.security.CryptographicSecurityManager;
import ai.tegmentum.wasmtime4j.security.DynamicSecurityPolicyEngine;
import ai.tegmentum.wasmtime4j.security.ResourceLimits;
import ai.tegmentum.wasmtime4j.security.SecurityManager;
import ai.tegmentum.wasmtime4j.security.SecurityMonitoringSystem;
import ai.tegmentum.wasmtime4j.security.VulnerabilityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Comprehensive security performance benchmarks.
 *
 * <p>Measures performance of all security features including:
 * <ul>
 *   <li>Capability-based access control operations</li>
 *   <li>Sandbox creation and resource monitoring</li>
 *   <li>Cryptographic operations and signature verification</li>
 *   <li>Security policy evaluation and enforcement</li>
 *   <li>Security monitoring and event processing</li>
 *   <li>Vulnerability scanning and analysis</li>
 * </ul>
 *
 * <p>These benchmarks help identify performance bottlenecks and optimize
 * security operations for production environments.
 *
 * @since 1.0.0
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = {"-Xms2g", "-Xmx4g"})
@State(Scope.Benchmark)
public class SecurityPerformanceBenchmark {

    @Param({"JNI", "PANAMA"})
    private String runtime;

    @Param({"1", "10", "100"})
    private int operationCount;

    private SecurityManager securityManager;
    private CapabilityManager capabilityManager;
    private DynamicSecurityPolicyEngine policyEngine;
    private CryptographicSecurityManager cryptoManager;
    private SecurityMonitoringSystem monitoringSystem;
    private VulnerabilityManager vulnerabilityManager;

    // Test data
    private byte[] testModuleBytes;
    private Set<Capability> testCapabilities;
    private List<String> testPrincipalIds;

    @Setup(Level.Trial)
    public void setupBenchmark() {
        System.out.println("Setting up security performance benchmark for runtime: " + runtime);

        // Initialize security manager based on runtime
        this.securityManager = SecurityManager.builder()
            .requireSignatures(true)
            .auditLogging(true)
            .strictSandboxMode(true)
            .build();

        // Initialize security components
        this.capabilityManager = new CapabilityManager(securityManager);
        this.policyEngine = new DynamicSecurityPolicyEngine(securityManager);

        final CryptographicSecurityManager.CryptographicPolicy cryptoPolicy =
            CryptographicSecurityManager.CryptographicPolicy.builder()
                .name("Benchmark Crypto Policy")
                .requireSignatures(true)
                .build();
        this.cryptoManager = new CryptographicSecurityManager(securityManager, cryptoPolicy);

        final SecurityMonitoringSystem.MonitoringConfiguration monitoringConfig =
            new SecurityMonitoringSystem.MonitoringConfiguration("Benchmark Monitoring");
        this.monitoringSystem = new SecurityMonitoringSystem(securityManager, monitoringConfig);

        final VulnerabilityManager.VulnerabilityPolicy vulnPolicy =
            new VulnerabilityManager.VulnerabilityPolicy("Benchmark Vulnerability Policy");
        this.vulnerabilityManager = new VulnerabilityManager(securityManager, vulnPolicy);

        // Initialize test data
        setupTestData();

        System.out.println("Security benchmark setup completed for " + runtime +
                          " with " + operationCount + " operations");
    }

    @TearDown(Level.Trial)
    public void tearDownBenchmark() {
        System.out.println("Tearing down security benchmark for runtime: " + runtime);

        if (monitoringSystem != null) {
            monitoringSystem.close();
        }
        if (vulnerabilityManager != null) {
            vulnerabilityManager.close();
        }
        if (securityManager != null) {
            securityManager.close();
        }

        System.out.println("Security benchmark teardown completed");
    }

    @Benchmark
    public void benchmarkCapabilityGrant(final Blackhole blackhole) {
        for (int i = 0; i < operationCount; i++) {
            final String principalId = testPrincipalIds.get(i % testPrincipalIds.size());
            final Capability capability = Capability.memoryAccess(1024 * 1024, false);

            final String grantId = capabilityManager.grantCapability(
                principalId, capability, "benchmark-grantor", Optional.empty(), false);

            blackhole.consume(grantId);
        }
    }

    @Benchmark
    public void benchmarkCapabilityCheck(final Blackhole blackhole) {
        // Pre-grant capabilities
        for (int i = 0; i < operationCount; i++) {
            final String principalId = testPrincipalIds.get(i % testPrincipalIds.size());
            final Capability capability = Capability.fileSystemAccess();
            capabilityManager.grantCapability(
                principalId, capability, "benchmark-grantor", Optional.empty(), false);
        }

        // Benchmark capability checks
        for (int i = 0; i < operationCount; i++) {
            final String principalId = testPrincipalIds.get(i % testPrincipalIds.size());
            final Capability capability = Capability.fileSystemAccess();

            final boolean hasCapability = capabilityManager.hasCapability(principalId, capability);
            blackhole.consume(hasCapability);
        }
    }

    @Benchmark
    public void benchmarkCapabilityRevocation(final Blackhole blackhole) {
        // Pre-grant capabilities and collect grant IDs
        final String[] grantIds = new String[operationCount];
        for (int i = 0; i < operationCount; i++) {
            final String principalId = testPrincipalIds.get(i % testPrincipalIds.size());
            final Capability capability = Capability.networkAccess();
            grantIds[i] = capabilityManager.grantCapability(
                principalId, capability, "benchmark-grantor", Optional.empty(), false);
        }

        // Benchmark revocations
        for (int i = 0; i < operationCount; i++) {
            final boolean revoked = capabilityManager.revokeCapability(grantIds[i], "benchmark-revoker");
            blackhole.consume(revoked);
        }
    }

    @Benchmark
    public void benchmarkSandboxCreation(final Blackhole blackhole) {
        for (int i = 0; i < operationCount; i++) {
            final AdvancedSandbox.SandboxConfiguration config = AdvancedSandbox.SandboxConfiguration.builder()
                .securityLevel(3)
                .resourceLimits(ResourceLimits.builder()
                    .maxMemoryBytes(2048 * 1024)
                    .maxCpuTimeMs(1000L)
                    .build())
                .allowedSystemCalls(Set.of("read", "write"))
                .networkPolicy(AdvancedSandbox.NetworkPolicy.denyAll())
                .fileSystemPolicy(AdvancedSandbox.FileSystemPolicy.readOnly())
                .build();

            try (final AdvancedSandbox sandbox = new AdvancedSandbox(
                "benchmark-sandbox-" + i, config, securityManager)) {

                blackhole.consume(sandbox.getResourceUsage());
            }
        }
    }

    @Benchmark
    public void benchmarkSandboxExecution(final Blackhole blackhole) {
        final AdvancedSandbox.SandboxConfiguration config = AdvancedSandbox.SandboxConfiguration.builder()
            .securityLevel(2)
            .resourceLimits(ResourceLimits.builder()
                .maxMemoryBytes(4096 * 1024)
                .build())
            .build();

        try (final AdvancedSandbox sandbox = new AdvancedSandbox(
            "execution-benchmark-sandbox", config, securityManager)) {

            for (int i = 0; i < operationCount; i++) {
                final AdvancedSandbox.SandboxExecutable<String> task = (sb) -> {
                    // Simulate lightweight computation
                    return "Task " + i + " completed";
                };

                final String result = sandbox.execute(task, Duration.ofSeconds(1));
                blackhole.consume(result);
            }
        }
    }

    @Benchmark
    public void benchmarkCryptographicOperations(final Blackhole blackhole) {
        for (int i = 0; i < operationCount; i++) {
            final byte[] moduleBytes = ("test module content " + i).getBytes();
            final String keyId = "benchmark-key-" + (i % 10); // Reuse keys

            try {
                final CryptographicSecurityManager.EncryptedModule encrypted =
                    cryptoManager.encryptModule(moduleBytes, keyId);
                blackhole.consume(encrypted);

                // Also benchmark integrity validation
                final boolean valid = cryptoManager.validateModuleIntegrity(
                    moduleBytes, new byte[32], // Mock hash
                    CryptographicSecurityManager.HashAlgorithm.SHA256);
                blackhole.consume(valid);

            } catch (final Exception e) {
                // Expected for benchmark with mock keys
                blackhole.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkPolicyEvaluation(final Blackhole blackhole) throws Exception {
        // Setup test policy
        final DynamicSecurityPolicyEngine.EnhancedSecurityPolicy policy =
            DynamicSecurityPolicyEngine.EnhancedSecurityPolicy.builder()
                .id("benchmark-policy")
                .name("Benchmark Security Policy")
                .addRule(DynamicSecurityPolicyEngine.PolicyRule.builder()
                    .id("allow-read-rule")
                    .condition(new DynamicSecurityPolicyEngine.PolicyCondition(
                        "action", DynamicSecurityPolicyEngine.ConditionOperator.EQUALS, "read"))
                    .action(DynamicSecurityPolicyEngine.PolicyAction.PERMIT)
                    .priority(100)
                    .build())
                .build();

        policyEngine.addPolicy("benchmark-tenant", policy, "1.0");

        // Benchmark policy evaluations
        for (int i = 0; i < operationCount; i++) {
            final ai.tegmentum.wasmtime4j.security.AccessRequest request =
                ai.tegmentum.wasmtime4j.security.AccessRequest.builder()
                    .userIdentity(ai.tegmentum.wasmtime4j.security.UserIdentity.builder()
                        .id("benchmark-user-" + (i % 10))
                        .principalType(ai.tegmentum.wasmtime4j.security.UserIdentity.PrincipalType.USER)
                        .build())
                    .resourceId("benchmark-resource-" + i)
                    .action(i % 2 == 0 ? "read" : "write")
                    .build();

            final DynamicSecurityPolicyEngine.PolicyDecision decision =
                policyEngine.evaluate("benchmark-tenant", request);

            blackhole.consume(decision);
        }
    }

    @Benchmark
    public void benchmarkSecurityEventProcessing(final Blackhole blackhole) {
        for (int i = 0; i < operationCount; i++) {
            final SecurityMonitoringSystem.SecurityEvent event =
                new SecurityMonitoringSystem.SecurityEvent(
                    "benchmark-event-" + i,
                    "benchmark_operation",
                    Instant.now(),
                    "benchmark-module",
                    "Benchmark security event " + i,
                    Map.of(
                        "operation_id", String.valueOf(i),
                        "user_id", "benchmark-user-" + (i % 10),
                        "resource", "benchmark-resource"
                    ),
                    SecurityMonitoringSystem.SecurityEventSeverity.INFO
                );

            final SecurityMonitoringSystem.SecurityEventProcessingResult result =
                monitoringSystem.processSecurityEvent(event);

            blackhole.consume(result);
        }
    }

    @Benchmark
    public void benchmarkVulnerabilityScanning(final Blackhole blackhole) throws Exception {
        for (int i = 0; i < operationCount; i++) {
            final String moduleId = "benchmark-module-" + i;
            final byte[] moduleBytes = ("benchmark module content " + i).getBytes();
            final VulnerabilityManager.ModuleMetadata metadata =
                new VulnerabilityManager.ModuleMetadata(List.of(
                    new VulnerabilityManager.Dependency("test-lib", "1.0." + (i % 10)),
                    new VulnerabilityManager.Dependency("benchmark-dep", "2.0.0")
                ));

            final VulnerabilityManager.SecurityScanResult scanResult =
                vulnerabilityManager.scanModule(moduleId, moduleBytes, metadata);

            blackhole.consume(scanResult);
        }
    }

    @Benchmark
    public void benchmarkSecurityManagerOperations(final Blackhole blackhole) throws Exception {
        for (int i = 0; i < operationCount; i++) {
            // Create security context
            final ai.tegmentum.wasmtime4j.security.SecurityContext context =
                securityManager.createSecurityContext("benchmark-context-" + i, 3);
            blackhole.consume(context);

            // Create sandbox
            final String sandboxId = securityManager.createSandbox("benchmark-module-" + i, context);
            blackhole.consume(sandboxId);

            // Check capability
            final Capability testCapability = Capability.memoryAccess(1024 * 512, false);
            final boolean hasCapability = securityManager.hasCapability(sandboxId, testCapability);
            blackhole.consume(hasCapability);

            // Remove sandbox
            securityManager.removeSandbox(sandboxId);
        }
    }

    @Benchmark
    public void benchmarkConcurrentSecurityOperations(final Blackhole blackhole) throws InterruptedException {
        final java.util.concurrent.CountDownLatch latch =
            new java.util.concurrent.CountDownLatch(operationCount);

        final java.util.concurrent.ExecutorService executor =
            java.util.concurrent.Executors.newFixedThreadPool(
                Math.min(operationCount, Runtime.getRuntime().availableProcessors()));

        for (int i = 0; i < operationCount; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    // Mix of different security operations
                    final String principalId = "concurrent-principal-" + taskId;
                    final Capability capability = Capability.systemCallAccess(Set.of("read", "write"));

                    // Grant capability
                    final String grantId = capabilityManager.grantCapability(
                        principalId, capability, "concurrent-grantor", Optional.empty(), false);

                    // Check capability
                    final boolean hasCapability = capabilityManager.hasCapability(principalId, capability);

                    // Process security event
                    final SecurityMonitoringSystem.SecurityEvent event =
                        new SecurityMonitoringSystem.SecurityEvent(
                            "concurrent-event-" + taskId,
                            "concurrent_operation",
                            Instant.now(),
                            "concurrent-module",
                            "Concurrent operation " + taskId,
                            Map.of("task_id", String.valueOf(taskId)),
                            SecurityMonitoringSystem.SecurityEventSeverity.INFO
                        );

                    final SecurityMonitoringSystem.SecurityEventProcessingResult result =
                        monitoringSystem.processSecurityEvent(event);

                    blackhole.consume(grantId);
                    blackhole.consume(hasCapability);
                    blackhole.consume(result);

                } catch (final Exception e) {
                    System.err.println("Concurrent operation failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }

    /**
     * Comprehensive security benchmark combining multiple operations.
     */
    @Benchmark
    public void benchmarkComprehensiveSecurityWorkflow(final Blackhole blackhole) throws Exception {
        for (int i = 0; i < operationCount; i++) {
            final String workflowId = "workflow-" + i;

            // 1. Create security context
            final ai.tegmentum.wasmtime4j.security.SecurityContext context =
                securityManager.createSecurityContext(workflowId + "-context", 4);

            // 2. Grant multiple capabilities
            final String memoryGrantId = capabilityManager.grantCapability(
                workflowId + "-principal",
                Capability.memoryAccess(2048 * 1024, true),
                workflowId + "-grantor",
                Optional.empty(), false);

            final String fileGrantId = capabilityManager.grantCapability(
                workflowId + "-principal",
                Capability.fileSystemAccess(),
                workflowId + "-grantor",
                Optional.empty(), true);

            // 3. Create sandbox
            final String sandboxId = securityManager.createSandbox(workflowId + "-module", context);

            // 4. Perform vulnerability scan
            final VulnerabilityManager.SecurityScanResult scanResult =
                vulnerabilityManager.scanModule(
                    workflowId + "-module",
                    ("module content for " + workflowId).getBytes(),
                    new VulnerabilityManager.ModuleMetadata(List.of(
                        new VulnerabilityManager.Dependency("workflow-lib", "1.0.0")
                    )));

            // 5. Process security events
            final SecurityMonitoringSystem.SecurityEvent event =
                new SecurityMonitoringSystem.SecurityEvent(
                    workflowId + "-event",
                    "workflow_completed",
                    Instant.now(),
                    workflowId + "-module",
                    "Comprehensive workflow completed",
                    Map.of("workflow_id", workflowId),
                    SecurityMonitoringSystem.SecurityEventSeverity.INFO
                );

            final SecurityMonitoringSystem.SecurityEventProcessingResult eventResult =
                monitoringSystem.processSecurityEvent(event);

            // 6. Cleanup
            capabilityManager.revokeCapability(memoryGrantId, workflowId + "-revoker");
            capabilityManager.revokeCapability(fileGrantId, workflowId + "-revoker");
            securityManager.removeSandbox(sandboxId);

            // Consume results
            blackhole.consume(context);
            blackhole.consume(memoryGrantId);
            blackhole.consume(fileGrantId);
            blackhole.consume(sandboxId);
            blackhole.consume(scanResult);
            blackhole.consume(eventResult);
        }
    }

    private void setupTestData() {
        // Initialize test module bytes
        this.testModuleBytes = "benchmark test module content".getBytes();

        // Initialize test capabilities
        this.testCapabilities = Set.of(
            Capability.memoryAccess(1024 * 1024, true),
            Capability.fileSystemAccess(),
            Capability.networkAccess(),
            Capability.systemCallAccess(Set.of("read", "write", "mmap")),
            Capability.environmentAccess(Set.of("PATH", "HOME"), false)
        );

        // Initialize test principal IDs
        this.testPrincipalIds = java.util.stream.IntStream.range(0, Math.max(100, operationCount))
            .mapToObj(i -> "benchmark-principal-" + i)
            .collect(java.util.stream.Collectors.toList());

        System.out.println("Test data initialized: " +
                          testCapabilities.size() + " capabilities, " +
                          testPrincipalIds.size() + " principals");
    }
}