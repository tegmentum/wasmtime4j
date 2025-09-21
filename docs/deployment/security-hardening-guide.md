# Security Hardening Guide for Production

This guide provides comprehensive security hardening recommendations for Wasmtime4j deployments in production environments, covering infrastructure security, application hardening, runtime security, and compliance requirements.

## Table of Contents
- [Infrastructure Security](#infrastructure-security)
- [Application Layer Hardening](#application-layer-hardening)
- [Runtime Security Configuration](#runtime-security-configuration)
- [Network Security](#network-security)
- [Access Control and Authentication](#access-control-and-authentication)
- [Monitoring and Incident Response](#monitoring-and-incident-response)
- [Compliance and Audit Requirements](#compliance-and-audit-requirements)
- [Security Testing and Validation](#security-testing-and-validation)

## Infrastructure Security

### Host Operating System Hardening

#### System Configuration

```bash
#!/bin/bash
# security-hardening.sh

set -euo pipefail

echo "=== Wasmtime4j Security Hardening ==="

# Disable unnecessary services
systemctl disable avahi-daemon
systemctl disable cups
systemctl disable bluetooth
systemctl disable apache2 || true
systemctl disable nginx || true  # Only if not used as reverse proxy

# Configure firewall
ufw --force reset
ufw default deny incoming
ufw default allow outgoing

# Allow SSH (adjust port as needed)
ufw allow 22/tcp

# Allow application ports
ufw allow 8080/tcp  # Application port
ufw allow 8081/tcp  # Metrics port (internal only)

# Allow from load balancer only
ufw allow from 10.0.0.0/8 to any port 8080

ufw --force enable

# Secure SSH configuration
cat > /etc/ssh/sshd_config.d/99-hardening.conf << 'EOF'
Protocol 2
PermitRootLogin no
PasswordAuthentication no
PubkeyAuthentication yes
AuthorizedKeysFile %h/.ssh/authorized_keys
X11Forwarding no
AllowTcpForwarding no
ClientAliveInterval 300
ClientAliveCountMax 2
MaxAuthTries 3
MaxSessions 2
LoginGraceTime 30
EOF

systemctl restart sshd

# Kernel hardening
cat >> /etc/sysctl.conf << 'EOF'
# Network security
net.ipv4.conf.all.send_redirects = 0
net.ipv4.conf.all.accept_redirects = 0
net.ipv4.conf.all.accept_source_route = 0
net.ipv4.conf.all.log_martians = 1
net.ipv4.conf.default.send_redirects = 0
net.ipv4.conf.default.accept_redirects = 0
net.ipv4.conf.default.accept_source_route = 0
net.ipv4.ip_forward = 0
net.ipv6.conf.all.accept_redirects = 0
net.ipv6.conf.default.accept_redirects = 0

# Memory protection
kernel.kptr_restrict = 2
kernel.dmesg_restrict = 1
kernel.yama.ptrace_scope = 1

# File system security
fs.suid_dumpable = 0
fs.protected_hardlinks = 1
fs.protected_symlinks = 1
EOF

sysctl -p

# Secure mount options
echo 'tmpfs /tmp tmpfs defaults,rw,nosuid,nodev,noexec,relatime,size=2G 0 0' >> /etc/fstab
echo 'tmpfs /var/tmp tmpfs defaults,rw,nosuid,nodev,noexec,relatime,size=1G 0 0' >> /etc/fstab

# File permissions
chmod 700 /root
chmod 644 /etc/passwd
chmod 644 /etc/group
chmod 600 /etc/shadow
chmod 600 /etc/gshadow

# Remove setuid/setgid bits from unnecessary files
find /usr -type f \( -perm -4000 -o -perm -2000 \) \
    ! -path '/usr/bin/sudo' \
    ! -path '/usr/bin/su' \
    ! -path '/usr/lib/openssh/ssh-keysign' \
    -exec chmod -s {} \;

echo "Host hardening completed"
```

#### AppArmor/SELinux Configuration

```bash
# AppArmor profile for Wasmtime4j
cat > /etc/apparmor.d/wasmtime4j << 'EOF'
#include <tunables/global>

/opt/wasmtime4j/app/wasmtime4j-app.jar {
  #include <abstractions/base>
  #include <abstractions/java>

  # Application files
  /opt/wasmtime4j/app/ r,
  /opt/wasmtime4j/app/** r,
  /opt/wasmtime4j/config/ r,
  /opt/wasmtime4j/config/** r,
  /opt/wasmtime4j/wasm-modules/ r,
  /opt/wasmtime4j/wasm-modules/** r,

  # Writable directories
  /opt/wasmtime4j/logs/ rw,
  /opt/wasmtime4j/logs/** rw,
  /opt/wasmtime4j/temp/ rw,
  /opt/wasmtime4j/temp/** rw,
  /opt/wasmtime4j/data/ rw,
  /opt/wasmtime4j/data/** rw,

  # System libraries
  /lib/x86_64-linux-gnu/ r,
  /lib/x86_64-linux-gnu/** rm,
  /usr/lib/jvm/** r,

  # Network access
  network inet stream,
  network inet dgram,

  # Deny dangerous capabilities
  deny capability sys_admin,
  deny capability sys_module,
  deny capability sys_rawio,
  deny capability dac_override,

  # Deny access to sensitive directories
  deny /etc/shadow r,
  deny /etc/passwd w,
  deny /home/** rw,
  deny /root/** rw,
  deny /proc/sys/** w,
}
EOF

# Load the profile
apparmor_parser -r /etc/apparmor.d/wasmtime4j
aa-enforce wasmtime4j
```

### Container Security Hardening

```dockerfile
# Secure Dockerfile for production
FROM openjdk:23-jdk-slim AS builder

# Security: Use specific base image digest
FROM openjdk@sha256:specific-digest AS runtime

# Security: Run as non-root user
RUN groupadd -r -g 1000 wasmapp && \
    useradd -r -u 1000 -g wasmapp -s /bin/false -d /app wasmapp

# Security: Remove package manager and unnecessary tools
RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates && \
    apt-get remove -y apt && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Security: Set secure working directory
WORKDIR /app

# Copy application with minimal permissions
COPY --from=builder --chown=wasmapp:wasmapp /app/target/wasmtime4j-*.jar app.jar

# Security: Remove shell access and create read-only filesystem
RUN rm -rf /bin/sh /bin/bash /usr/bin/sh /usr/bin/bash

# Security: Set up runtime user
USER 1000:1000

# Security: Health check with timeout
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD java -cp app.jar ai.tegmentum.wasmtime4j.health.HealthChecker || exit 1

# Security: Minimal exposed ports
EXPOSE 8080

# Security: Read-only root filesystem with writable temp
VOLUME ["/tmp"]

# Security: Set secure JVM options
ENV JAVA_OPTS="-Djava.security.manager \
               -Djava.security.policy=/app/security.policy \
               -Djava.awt.headless=true \
               -Dfile.encoding=UTF-8 \
               -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Kubernetes Security Context

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wasmtime4j-secure
spec:
  template:
    spec:
      securityContext:
        # Pod-level security
        runAsNonRoot: true
        runAsUser: 1000
        runAsGroup: 1000
        fsGroup: 1000
        seccompProfile:
          type: RuntimeDefault
        supplementalGroups: []

      containers:
      - name: wasmtime4j
        image: wasmtime4j:secure
        securityContext:
          # Container-level security
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
          runAsNonRoot: true
          runAsUser: 1000
          runAsGroup: 1000
          capabilities:
            drop:
            - ALL
          seccompProfile:
            type: RuntimeDefault

        resources:
          limits:
            memory: "2Gi"
            cpu: "2000m"
            ephemeral-storage: "1Gi"
          requests:
            memory: "1Gi"
            cpu: "500m"
            ephemeral-storage: "500Mi"

        volumeMounts:
        - name: tmp
          mountPath: /tmp
        - name: cache
          mountPath: /app/cache

      volumes:
      - name: tmp
        emptyDir:
          sizeLimit: 100Mi
      - name: cache
        emptyDir:
          sizeLimit: 200Mi

      # Network policy
      automountServiceAccountToken: false
```

## Application Layer Hardening

### Java Security Manager Configuration

```java
// security.policy - Java Security Manager policy
grant {
    // Basic permissions for application functionality
    permission java.lang.RuntimePermission "loadLibrary.*";
    permission java.lang.RuntimePermission "accessDeclaredMembers";
    permission java.lang.RuntimePermission "createClassLoader";

    // File system permissions (restricted)
    permission java.io.FilePermission "/opt/wasmtime4j/app/-", "read";
    permission java.io.FilePermission "/opt/wasmtime4j/config/-", "read";
    permission java.io.FilePermission "/opt/wasmtime4j/wasm-modules/-", "read";
    permission java.io.FilePermission "/opt/wasmtime4j/data/-", "read,write,delete";
    permission java.io.FilePermission "/opt/wasmtime4j/logs/-", "read,write,delete";
    permission java.io.FilePermission "/tmp/-", "read,write,delete";

    // Network permissions (restricted to application needs)
    permission java.net.SocketPermission "localhost:5432", "connect,resolve";  // Database
    permission java.net.SocketPermission "localhost:6379", "connect,resolve";  // Redis
    permission java.net.SocketPermission "*:80", "connect,resolve";           // HTTP
    permission java.net.SocketPermission "*:443", "connect,resolve";          // HTTPS

    // System properties (read-only)
    permission java.util.PropertyPermission "*", "read";

    // Reflection permissions (limited)
    permission java.lang.reflect.ReflectPermission "suppressAccessChecks";

    // JMX permissions for monitoring
    permission javax.management.MBeanServerPermission "createMBeanServer";
    permission javax.management.MBeanPermission "*", "registerMBean,unregisterMBean";
};

// Deny dangerous permissions
deny {
    permission java.security.AllPermission;
    permission java.lang.RuntimePermission "createSecurityManager";
    permission java.lang.RuntimePermission "setSecurityManager";
    permission java.lang.RuntimePermission "shutdownHooks";
    permission java.net.NetPermission "specifyStreamHandler";
    permission java.io.FilePermission "/etc/-", "read,write,delete";
    permission java.io.FilePermission "/home/-", "read,write,delete";
    permission java.io.FilePermission "/root/-", "read,write,delete";
};
```

### Secure Configuration Management

```java
@Configuration
@EnableConfigurationProperties
public class SecureConfiguration {

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // High cost factor
    }

    @Bean
    public SecretKeyService secretKeyService() {
        return new SecretKeyService();
    }

    @Component
    public static class SecretKeyService {

        @Value("${wasmtime4j.encryption.key:#{null}}")
        private String encryptionKey;

        private SecretKey secretKey;

        @PostConstruct
        public void initializeKey() {
            if (encryptionKey != null) {
                // Load from environment/vault
                byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
                secretKey = new SecretKeySpec(keyBytes, "AES");
            } else {
                // Generate secure random key
                KeyGenerator generator = KeyGenerator.getInstance("AES");
                generator.init(256);
                secretKey = generator.generateKey();

                // In production, store this securely
                log.warn("Generated new encryption key - store securely!");
            }
        }

        public String encrypt(String plaintext) throws Exception {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] iv = cipher.getIV();

            // Combine IV and encrypted data
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(result);
        }

        public String decrypt(String encryptedData) throws Exception {
            byte[] data = Base64.getDecoder().decode(encryptedData);

            // Extract IV and encrypted data
            byte[] iv = new byte[12];  // GCM IV length
            byte[] encrypted = new byte[data.length - 12];
            System.arraycopy(data, 0, iv, 0, 12);
            System.arraycopy(data, 12, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        }
    }
}
```

### Input Validation and Sanitization

```java
@Component
public class SecurityValidationService {

    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final int MAX_MODULE_SIZE = 10 * 1024 * 1024;  // 10MB
    private static final int MAX_INPUT_LENGTH = 1000;

    public void validateModuleUpload(String filename, byte[] content) {
        // Filename validation
        if (!SAFE_FILENAME.matcher(filename).matches()) {
            throw new SecurityException("Invalid filename: " + filename);
        }

        // Size validation
        if (content.length > MAX_MODULE_SIZE) {
            throw new SecurityException("Module too large: " + content.length);
        }

        // Content validation
        if (!isValidWasmModule(content)) {
            throw new SecurityException("Invalid WebAssembly module");
        }

        // Malware scanning
        if (containsMaliciousPatterns(content)) {
            throw new SecurityException("Malicious content detected");
        }
    }

    public String sanitizeUserInput(String input) {
        if (input == null) {
            return null;
        }

        // Length check
        if (input.length() > MAX_INPUT_LENGTH) {
            throw new SecurityException("Input too long");
        }

        // Remove control characters
        String sanitized = input.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

        // HTML encoding
        sanitized = StringEscapeUtils.escapeHtml4(sanitized);

        // SQL injection prevention
        sanitized = sanitized.replaceAll("['\"\\\\]", "");

        return sanitized;
    }

    private boolean isValidWasmModule(byte[] content) {
        // Check WebAssembly magic number
        if (content.length < 8) {
            return false;
        }

        // WebAssembly magic: 0x00 0x61 0x73 0x6D (little-endian "\0asm")
        return content[0] == 0x00 && content[1] == 0x61 &&
               content[2] == 0x73 && content[3] == 0x6D;
    }

    private boolean containsMaliciousPatterns(byte[] content) {
        // Convert to hex string for pattern matching
        String hex = DatatypeConverter.printHexBinary(content);

        // Check for suspicious patterns
        String[] maliciousPatterns = {
            "4D5A",        // PE header
            "7F454C46",    // ELF header
            "FEEDFACE",    // Mach-O header
            "CAFEBABE",    // Java class file
        };

        for (String pattern : maliciousPatterns) {
            if (hex.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    public void validateApiParameters(Map<String, Object> parameters) {
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Key validation
            if (!key.matches("^[a-zA-Z0-9_-]+$")) {
                throw new SecurityException("Invalid parameter name: " + key);
            }

            // Value validation
            if (value instanceof String) {
                sanitizeUserInput((String) value);
            } else if (value instanceof Number) {
                validateNumericInput((Number) value);
            }
        }
    }

    private void validateNumericInput(Number value) {
        if (value instanceof Integer) {
            int intValue = value.intValue();
            if (intValue < 0 || intValue > Integer.MAX_VALUE / 2) {
                throw new SecurityException("Invalid integer value: " + intValue);
            }
        } else if (value instanceof Long) {
            long longValue = value.longValue();
            if (longValue < 0 || longValue > Long.MAX_VALUE / 2) {
                throw new SecurityException("Invalid long value: " + longValue);
            }
        }
    }
}
```

## Runtime Security Configuration

### WebAssembly Runtime Hardening

```java
@Configuration
public class SecureRuntimeConfiguration {

    @Bean
    @Primary
    public WasmRuntime secureWasmRuntime() throws WasmException {
        return WasmRuntimeFactory.create(
            RuntimeConfig.builder()
                .runtimeType(RuntimeType.JNI)  // Predictable security properties
                .optimizationLevel(OptimizationLevel.NONE)  // Disable optimizations that might bypass security
                .memoryLimit(256L * 1024 * 1024)  // 256MB strict limit
                .executionTimeout(Duration.ofSeconds(10))  // Short timeout
                .enableDebug(false)
                .enableProfiling(false)
                .securityLevel(SecurityLevel.MAXIMUM)
                .build()
        );
    }

    @Bean
    public FaultTolerantEngine secureEngine(WasmRuntime runtime) throws WasmException {
        FaultToleranceConfig faultConfig = FaultToleranceConfig.builder()
            // Circuit breaker settings
            .failureThreshold(5)
            .circuitBreakerTimeout(Duration.ofMinutes(1))
            .successThreshold(3)

            // Retry settings (limited for security)
            .maxRetries(2)
            .retryDelay(Duration.ofSeconds(1), Duration.ofSeconds(5), 2.0)
            .retryableException(WasmTimeoutException.class)

            // Strict timeouts
            .operationTimeout(Duration.ofSeconds(10))
            .compilationTimeout(Duration.ofSeconds(30))
            .instantiationTimeout(Duration.ofSeconds(5))

            // Resource limits
            .memoryLimits(MemoryLimits.builder()
                .maxHeapMemory(128L * 1024 * 1024)      // 128MB
                .maxDirectMemory(64L * 1024 * 1024)     // 64MB
                .maxWasmMemory(64L * 1024 * 1024)       // 64MB
                .maxMemoryInstances(10)
                .build())

            .cpuLimits(CpuLimits.builder()
                .maxCpuUsage(0.5)  // 50% CPU limit
                .maxThreads(4)
                .maxCompilationTime(Duration.ofSeconds(30))
                .maxInstructions(1_000_000L)  // 1M instruction limit
                .build())

            .timeLimits(TimeLimits.builder()
                .maxExecutionTime(Duration.ofSeconds(10))
                .maxLifetime(Duration.ofMinutes(5))
                .maxIdleTime(Duration.ofSeconds(30))
                .build())

            .build();

        return FaultTolerantEngine.builder()
            .engine(runtime.createEngine())
            .faultToleranceConfig(faultConfig)
            .build();
    }

    @Bean
    public WasiContext secureWasiContext() {
        return WasiContextBuilder.create()
            // Minimal filesystem access
            .preopenDirectory("/app/data/safe", "/data", WasiDirectoryAccess.READ_ONLY)
            .preopenDirectory("/tmp/wasm-work", "/tmp", WasiDirectoryAccess.READ_WRITE)

            // No environment variable access
            // DO NOT use .inheritEnvironment()

            // No network access by default
            .networkAccess(WasiNetworkAccess.NONE)

            // Strict resource limits
            .resourceLimits(WasiResourceLimits.builder()
                .maxMemory(32L * 1024 * 1024)        // 32MB for WASI
                .maxOpenFiles(5)                      // Very limited file handles
                .maxNetworkConnections(0)             // No network
                .maxExecutionTime(Duration.ofSeconds(5))  // Short execution time
                .build())

            .build();
    }
}
```

### Host Function Security

```java
@Component
public class SecureHostFunctions {

    private final RateLimiter globalRateLimiter = RateLimiter.create(50.0);  // 50 calls/sec
    private final SecurityAuditLogger auditLogger;
    private final Set<String> allowedOperations = Set.of("log", "hash", "validate");

    public WasmFunction createSecureLogFunction() {
        return WasmFunction.hostFunction(
            "secure_log",
            FunctionType.of(
                new WasmValueType[]{WasmValueType.I32, WasmValueType.I32},
                new WasmValueType[]{}),
            (args) -> {
                // Rate limiting
                if (!globalRateLimiter.tryAcquire()) {
                    auditLogger.logSecurityEvent(
                        SecurityEventType.RATE_LIMIT_EXCEEDED,
                        "Host function rate limit exceeded");
                    throw new SecurityException("Rate limit exceeded");
                }

                int messagePtr = args[0].asI32();
                int messageLen = args[1].asI32();

                // Strict parameter validation
                if (messagePtr < 0 || messageLen < 0 || messageLen > 1024) {
                    auditLogger.logSecurityEvent(
                        SecurityEventType.INVALID_PARAMETERS,
                        "Invalid log parameters: ptr=" + messagePtr + ", len=" + messageLen);
                    throw new SecurityException("Invalid parameters");
                }

                // Get memory with bounds checking
                WasmMemory memory = getCurrentMemory();
                if (messagePtr + messageLen > memory.size() * 65536) {
                    auditLogger.logSecurityEvent(
                        SecurityEventType.MEMORY_VIOLATION,
                        "Log function memory access violation");
                    throw new SecurityException("Memory access violation");
                }

                try {
                    // Read and validate message
                    byte[] messageBytes = memory.read(messagePtr, messageLen);
                    String message = sanitizeLogMessage(new String(messageBytes, StandardCharsets.UTF_8));

                    // Audit logging
                    auditLogger.logWasmHostCall("secure_log", message);

                    // Safe logging (prevent log injection)
                    log.info("WASM: {}", message.replaceAll("[\r\n\t]", "_"));

                } catch (Exception e) {
                    auditLogger.logSecurityEvent(
                        SecurityEventType.HOST_FUNCTION_ERROR,
                        "Error in secure_log: " + e.getMessage());
                    throw new SecurityException("Log function error", e);
                }

                return new WasmValue[0];
            }
        );
    }

    public WasmFunction createSecureHashFunction() {
        return WasmFunction.hostFunction(
            "secure_hash",
            FunctionType.of(
                new WasmValueType[]{WasmValueType.I32, WasmValueType.I32, WasmValueType.I32},
                new WasmValueType[]{WasmValueType.I32}),
            (args) -> {
                // Rate limiting
                if (!globalRateLimiter.tryAcquire(5)) {  // Hash operations cost more
                    throw new SecurityException("Hash rate limit exceeded");
                }

                int inputPtr = args[0].asI32();
                int inputLen = args[1].asI32();
                int outputPtr = args[2].asI32();

                // Validate parameters
                if (inputPtr < 0 || inputLen < 0 || inputLen > 4096 || outputPtr < 0) {
                    throw new SecurityException("Invalid hash parameters");
                }

                try {
                    WasmMemory memory = getCurrentMemory();

                    // Read input data
                    byte[] inputData = memory.read(inputPtr, inputLen);

                    // Compute SHA-256 hash
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] hash = digest.digest(inputData);

                    // Write hash to output location (32 bytes for SHA-256)
                    if (outputPtr + 32 > memory.size() * 65536) {
                        throw new SecurityException("Hash output memory violation");
                    }

                    memory.write(outputPtr, hash);

                    auditLogger.logWasmHostCall("secure_hash", "input_len=" + inputLen);

                    return new WasmValue[]{WasmValue.i32(32)};  // Return hash length

                } catch (Exception e) {
                    auditLogger.logSecurityEvent(
                        SecurityEventType.HOST_FUNCTION_ERROR,
                        "Error in secure_hash: " + e.getMessage());
                    throw new SecurityException("Hash function error", e);
                }
            }
        );
    }

    private String sanitizeLogMessage(String message) {
        // Remove control characters and limit length
        String sanitized = message.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                                 .substring(0, Math.min(message.length(), 500));

        // Remove potentially dangerous content
        sanitized = sanitized.replaceAll("(?i)(password|token|secret|key)=\\S+", "$1=***");

        return sanitized;
    }
}
```

## Network Security

### TLS Configuration

```java
@Configuration
@EnableWebSecurity
public class NetworkSecurityConfiguration {

    @Bean
    public TomcatServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                // Security headers
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };

        // TLS configuration
        tomcat.addAdditionalTomcatConnectors(createHttpsConnector());
        return tomcat;
    }

    private Connector createHttpsConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();

        // Basic HTTPS configuration
        connector.setScheme("https");
        connector.setSecure(true);
        connector.setPort(8443);

        // SSL configuration
        protocol.setSSLEnabled(true);
        protocol.setKeystoreFile("/opt/wasmtime4j/certs/keystore.p12");
        protocol.setKeystorePass("changeit");
        protocol.setKeyAlias("wasmtime4j");

        // Security protocols
        protocol.setSslProtocol("TLSv1.2,TLSv1.3");
        protocol.setCiphers("TLS_AES_256_GCM_SHA384,TLS_CHACHA20_POLY1305_SHA256," +
                           "TLS_AES_128_GCM_SHA256,ECDHE-RSA-AES256-GCM-SHA384," +
                           "ECDHE-RSA-AES128-GCM-SHA256");

        // Security options
        protocol.setHonorCipherOrder(true);
        protocol.setDisableUploadTimeout(false);
        protocol.setMaxHttpHeaderSize(8192);

        return connector;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.requiresChannel(channel -> channel.anyRequest().requiresSecure())
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                    .preload(true))
                .and()
                .addHeaderWriter(new StaticHeadersWriter("X-Content-Type-Options", "nosniff"))
                .addHeaderWriter(new StaticHeadersWriter("X-XSS-Protection", "1; mode=block"))
                .addHeaderWriter(new StaticHeadersWriter("Referrer-Policy", "strict-origin-when-cross-origin"))
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                    "geolocation=(), microphone=(), camera=()"))
            );

        return http.build();
    }
}
```

### API Security

```java
@RestController
@RequestMapping("/api/secure")
@Validated
public class SecureApiController {

    private final SecurityValidationService validator;
    private final RateLimiter apiRateLimiter = RateLimiter.create(100.0);

    @PostMapping("/execute")
    @PreAuthorize("hasRole('WASM_EXECUTE')")
    public ResponseEntity<ExecutionResult> executeModule(
            @Valid @RequestBody ExecutionRequest request,
            HttpServletRequest httpRequest) {

        // Rate limiting
        if (!apiRateLimiter.tryAcquire()) {
            throw new TooManyRequestsException("API rate limit exceeded");
        }

        // IP-based rate limiting
        String clientIp = getClientIpAddress(httpRequest);
        if (!ipRateLimiter.tryAcquire(clientIp)) {
            auditLogger.logSecurityEvent(
                SecurityEventType.RATE_LIMIT_EXCEEDED,
                "IP rate limit exceeded: " + clientIp);
            throw new TooManyRequestsException("IP rate limit exceeded");
        }

        // Input validation
        validator.validateApiParameters(request.getParameters());

        // Module validation
        if (!isModuleAllowed(request.getModuleId())) {
            auditLogger.logSecurityEvent(
                SecurityEventType.UNAUTHORIZED_MODULE_ACCESS,
                "Unauthorized module access attempt: " + request.getModuleId());
            throw new ForbiddenException("Module not authorized");
        }

        try {
            // Execute with security context
            ExecutionResult result = wasmExecutor.execute(request);

            auditLogger.logWasmExecution(request.getModuleId(),
                                       request.getFunctionName(),
                                       result.getExecutionTime());

            return ResponseEntity.ok(result);

        } catch (WasmException e) {
            auditLogger.logSecurityEvent(
                SecurityEventType.EXECUTION_ERROR,
                "WebAssembly execution error: " + e.getMessage());
            throw new InternalServerErrorException("Execution failed");
        }
    }

    @PostMapping("/modules")
    @PreAuthorize("hasRole('MODULE_ADMIN')")
    public ResponseEntity<Void> uploadModule(
            @RequestParam("file") MultipartFile file,
            @RequestParam("moduleId") String moduleId,
            HttpServletRequest httpRequest) {

        // File validation
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024) {
            throw new BadRequestException("Invalid file size");
        }

        try {
            byte[] moduleBytes = file.getBytes();

            // Security validation
            validator.validateModuleUpload(file.getOriginalFilename(), moduleBytes);

            // Store module securely
            moduleManager.storeModule(moduleId, moduleBytes, getCurrentUser());

            auditLogger.logModuleUpload(moduleId, getCurrentUser(),
                                      getClientIpAddress(httpRequest));

            return ResponseEntity.ok().build();

        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to read file");
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
```

## Access Control and Authentication

### JWT Authentication

```java
@Component
public class JwtAuthenticationProvider {

    private final SecretKey signingKey;
    private final Duration tokenExpiry = Duration.ofHours(1);

    @PostConstruct
    public void initializeKey() {
        // Load from secure storage in production
        String keyString = environment.getProperty("jwt.signing.key");
        if (keyString != null) {
            signingKey = new SecretKeySpec(
                Base64.getDecoder().decode(keyString),
                "HmacSHA256");
        } else {
            // Generate secure random key
            signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
    }

    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now();

        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(tokenExpiry)))
            .claim("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()))
            .claim("permissions", getUserPermissions(userDetails))
            .signWith(signingKey)
            .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .setAllowedClockSkewSeconds(30)  // Allow 30s clock skew
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (JwtException e) {
            throw new AuthenticationException("Invalid JWT token", e);
        }
    }

    private Set<String> getUserPermissions(UserDetails userDetails) {
        // Map roles to specific permissions
        Set<String> permissions = new HashSet<>();

        for (GrantedAuthority authority : userDetails.getAuthorities()) {
            switch (authority.getAuthority()) {
                case "ROLE_ADMIN":
                    permissions.addAll(Arrays.asList(
                        "module:read", "module:write", "module:delete",
                        "execute:all", "config:write"));
                    break;
                case "ROLE_DEVELOPER":
                    permissions.addAll(Arrays.asList(
                        "module:read", "module:write", "execute:trusted"));
                    break;
                case "ROLE_USER":
                    permissions.addAll(Arrays.asList(
                        "module:read", "execute:sandboxed"));
                    break;
            }
        }

        return permissions;
    }
}
```

### Permission-Based Access Control

```java
@Component
public class WasmPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        if (targetDomainObject instanceof WasmModule) {
            return hasModulePermission(auth, (WasmModule) targetDomainObject, permission.toString());
        }

        if (targetDomainObject instanceof ExecutionRequest) {
            return hasExecutionPermission(auth, (ExecutionRequest) targetDomainObject, permission.toString());
        }

        return false;
    }

    private boolean hasModulePermission(Authentication auth, WasmModule module, String permission) {
        Set<String> userPermissions = getUserPermissions(auth);

        switch (permission) {
            case "read":
                return userPermissions.contains("module:read") ||
                       userPermissions.contains("module:write") ||
                       userPermissions.contains("module:delete");

            case "write":
                // Check if user can write to this module based on trust level
                if (module.getTrustLevel() == TrustLevel.TRUSTED) {
                    return userPermissions.contains("module:write") &&
                           userPermissions.contains("trusted:access");
                } else {
                    return userPermissions.contains("module:write");
                }

            case "delete":
                return userPermissions.contains("module:delete") &&
                       (isModuleOwner(auth, module) || hasAdminRole(auth));

            case "execute":
                return hasExecutePermission(auth, module);

            default:
                return false;
        }
    }

    private boolean hasExecutionPermission(Authentication auth, ExecutionRequest request, String permission) {
        Set<String> userPermissions = getUserPermissions(auth);
        WasmModule module = moduleRepository.findById(request.getModuleId());

        if (module == null) {
            return false;
        }

        // Check trust level permissions
        switch (module.getTrustLevel()) {
            case TRUSTED:
                return userPermissions.contains("execute:trusted") ||
                       userPermissions.contains("execute:all");

            case SANDBOXED:
                return userPermissions.contains("execute:sandboxed") ||
                       userPermissions.contains("execute:trusted") ||
                       userPermissions.contains("execute:all");

            case UNTRUSTED:
                return userPermissions.contains("execute:untrusted") ||
                       userPermissions.contains("execute:all");

            default:
                return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> getUserPermissions(Authentication auth) {
        if (auth.getPrincipal() instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwt = (JwtAuthenticationToken) auth.getPrincipal();
            return new HashSet<>((List<String>) jwt.getTokenAttributes().get("permissions"));
        }
        return Collections.emptySet();
    }
}
```

## Monitoring and Incident Response

### Security Event Monitoring

```java
@Component
public class SecurityMonitoringService {

    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;
    private final Counter securityEventCounter;
    private final Timer incidentResponseTime;

    public SecurityMonitoringService(ApplicationEventPublisher eventPublisher,
                                   MeterRegistry meterRegistry) {
        this.eventPublisher = eventPublisher;
        this.meterRegistry = meterRegistry;

        this.securityEventCounter = Counter.builder("security.events")
            .description("Security events by type and severity")
            .register(meterRegistry);

        this.incidentResponseTime = Timer.builder("security.incident.response.time")
            .description("Time to respond to security incidents")
            .register(meterRegistry);
    }

    @EventListener
    @Async
    public void handleSecurityEvent(SecurityEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Increment metrics
            securityEventCounter.increment(
                Tags.of(
                    "type", event.getType().name(),
                    "severity", event.getSeverity().name(),
                    "source", event.getSource()
                )
            );

            // Log structured event
            logSecurityEvent(event);

            // Trigger automated response
            triggerAutomatedResponse(event);

            // Alert if critical
            if (event.getSeverity() == SecuritySeverity.CRITICAL) {
                alertingService.sendCriticalAlert(event);
            }

            // Publish internal event for other handlers
            eventPublisher.publishEvent(new SecurityIncidentEvent(event));

        } finally {
            sample.stop(incidentResponseTime);
        }
    }

    private void logSecurityEvent(SecurityEvent event) {
        Map<String, Object> logData = Map.of(
            "event_type", event.getType(),
            "severity", event.getSeverity(),
            "timestamp", event.getTimestamp(),
            "source", event.getSource(),
            "description", event.getDescription(),
            "metadata", event.getMetadata(),
            "correlation_id", event.getCorrelationId()
        );

        // Structured JSON logging for SIEM integration
        securityLog.info("SECURITY_EVENT: {}",
                        objectMapper.writeValueAsString(logData));
    }

    private void triggerAutomatedResponse(SecurityEvent event) {
        switch (event.getType()) {
            case RATE_LIMIT_EXCEEDED:
                // Temporarily block IP
                ipBlockingService.blockTemporary(event.getSourceIp(), Duration.ofMinutes(5));
                break;

            case MALICIOUS_MODULE_DETECTED:
                // Quarantine module
                moduleQuarantineService.quarantine(event.getModuleId());
                break;

            case MEMORY_VIOLATION:
                // Terminate execution context
                executionContextManager.terminate(event.getExecutionId());
                break;

            case UNAUTHORIZED_ACCESS:
                // Invalidate user sessions
                sessionManager.invalidateUserSessions(event.getUserId());
                break;

            case SUSPICIOUS_PATTERN:
                // Increase monitoring for source
                monitoringService.increaseSurveillance(event.getSourceIp());
                break;
        }
    }
}
```

### Automated Incident Response

```java
@Component
public class IncidentResponseService {

    private final List<IncidentHandler> handlers;
    private final NotificationService notificationService;

    @EventListener
    public void handleSecurityIncident(SecurityIncidentEvent incident) {
        SecurityEvent event = incident.getSecurityEvent();

        // Create incident record
        IncidentRecord record = createIncidentRecord(event);

        // Execute response handlers
        for (IncidentHandler handler : handlers) {
            if (handler.canHandle(event)) {
                try {
                    IncidentResponse response = handler.handle(event);
                    record.addResponse(response);

                    if (response.isEscalationRequired()) {
                        escalateIncident(record);
                    }
                } catch (Exception e) {
                    log.error("Incident handler failed", e);
                    record.addFailure(handler.getClass().getSimpleName(), e);
                }
            }
        }

        // Store incident record
        incidentRepository.save(record);

        // Send notifications if required
        if (record.requiresNotification()) {
            notificationService.sendIncidentNotification(record);
        }
    }

    @Component
    public static class MaliciousModuleHandler implements IncidentHandler {

        @Override
        public boolean canHandle(SecurityEvent event) {
            return event.getType() == SecurityEventType.MALICIOUS_MODULE_DETECTED;
        }

        @Override
        public IncidentResponse handle(SecurityEvent event) {
            String moduleId = event.getModuleId();

            // Immediate containment
            moduleRepository.updateStatus(moduleId, ModuleStatus.QUARANTINED);

            // Remove from all running instances
            executionManager.terminateAllInstancesOfModule(moduleId);

            // Clear from cache
            moduleCache.evict(moduleId);

            // Analyze module for forensics
            ForensicsResult analysis = forensicsAnalyzer.analyzeModule(moduleId);

            return IncidentResponse.builder()
                .action("Module quarantined and removed from execution")
                .containmentAchieved(true)
                .forensicsData(analysis)
                .escalationRequired(analysis.indicatesAdvancedThreat())
                .build();
        }
    }

    @Component
    public static class ResourceExhaustionHandler implements IncidentHandler {

        @Override
        public boolean canHandle(SecurityEvent event) {
            return event.getType() == SecurityEventType.RESOURCE_EXHAUSTION;
        }

        @Override
        public IncidentResponse handle(SecurityEvent event) {
            // Scale down resource consumption
            resourceManager.enableEmergencyLimits();

            // Terminate low-priority executions
            executionManager.terminateNonCriticalExecutions();

            // Clear caches to free memory
            cacheManager.clearNonEssentialCaches();

            // Alert operations team
            alertingService.sendResourceAlert(event);

            return IncidentResponse.builder()
                .action("Emergency resource limits activated")
                .containmentAchieved(true)
                .escalationRequired(true)  // Always escalate resource issues
                .build();
        }
    }
}
```

## Compliance and Audit Requirements

### Audit Logging

```java
@Component
public class ComplianceAuditService {

    private final AuditEventRepository auditRepository;
    private final ObjectMapper objectMapper;

    @EventListener
    @Async("auditExecutor")
    public void recordAuditEvent(AuditableEvent event) {
        AuditRecord record = AuditRecord.builder()
            .eventType(event.getEventType())
            .timestamp(Instant.now())
            .userId(getCurrentUserId())
            .sessionId(getCurrentSessionId())
            .sourceIp(getCurrentSourceIp())
            .userAgent(getCurrentUserAgent())
            .resource(event.getResourceId())
            .action(event.getAction())
            .outcome(event.getOutcome())
            .details(serializeEventDetails(event))
            .riskLevel(calculateRiskLevel(event))
            .build();

        // Store in database
        auditRepository.save(record);

        // Send to external audit system if required
        if (isExternalAuditRequired(event)) {
            externalAuditService.sendAuditEvent(record);
        }
    }

    @Scheduled(fixedRate = 3600000)  // Every hour
    public void generateComplianceReport() {
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));

        List<AuditRecord> recentEvents = auditRepository.findByTimestampAfter(oneHourAgo);

        ComplianceReport report = ComplianceReport.builder()
            .periodStart(oneHourAgo)
            .periodEnd(Instant.now())
            .totalEvents(recentEvents.size())
            .criticalEvents(countCriticalEvents(recentEvents))
            .failedAuthentications(countFailedAuthentications(recentEvents))
            .privilegedOperations(countPrivilegedOperations(recentEvents))
            .moduleOperations(countModuleOperations(recentEvents))
            .dataAccess(countDataAccess(recentEvents))
            .build();

        // Store report
        complianceReportRepository.save(report);

        // Alert if anomalies detected
        if (report.hasAnomalies()) {
            alertingService.sendComplianceAlert(report);
        }
    }

    public void auditModuleAccess(String moduleId, String action, AccessOutcome outcome) {
        recordAuditEvent(ModuleAccessEvent.builder()
            .moduleId(moduleId)
            .action(action)
            .outcome(outcome)
            .build());
    }

    public void auditDataAccess(String dataType, String resource, DataAccessType accessType) {
        recordAuditEvent(DataAccessEvent.builder()
            .dataType(dataType)
            .resource(resource)
            .accessType(accessType)
            .build());
    }

    public void auditConfigurationChange(String setting, Object oldValue, Object newValue) {
        recordAuditEvent(ConfigurationChangeEvent.builder()
            .setting(setting)
            .oldValue(redactSensitiveValue(oldValue))
            .newValue(redactSensitiveValue(newValue))
            .build());
    }

    private Object redactSensitiveValue(Object value) {
        if (value == null) return null;

        String stringValue = value.toString();
        if (stringValue.toLowerCase().contains("password") ||
            stringValue.toLowerCase().contains("secret") ||
            stringValue.toLowerCase().contains("key")) {
            return "***REDACTED***";
        }

        return value;
    }
}
```

### Data Protection and Privacy

```java
@Component
public class DataProtectionService {

    private final EncryptionService encryptionService;
    private final DataClassificationService classificationService;

    public void handlePersonalData(byte[] wasmModuleData, DataProcessingContext context) {
        // Classify data sensitivity
        DataClassification classification = classificationService.classify(wasmModuleData);

        if (classification.containsPersonalData()) {
            // Apply GDPR/privacy requirements
            applyPrivacyControls(wasmModuleData, context, classification);
        }

        // Apply appropriate encryption
        if (classification.requiresEncryption()) {
            wasmModuleData = encryptionService.encrypt(wasmModuleData,
                                                     classification.getEncryptionLevel());
        }

        // Audit data processing
        auditService.auditDataProcessing(classification, context);
    }

    private void applyPrivacyControls(byte[] data, DataProcessingContext context,
                                    DataClassification classification) {
        // Check consent
        if (!consentService.hasValidConsent(context.getDataSubject(),
                                          context.getProcessingPurpose())) {
            throw new PrivacyViolationException("No valid consent for data processing");
        }

        // Apply data minimization
        if (classification.requiresMinimization()) {
            data = dataMinimizationService.minimize(data, context.getProcessingPurpose());
        }

        // Set retention policy
        retentionService.setRetentionPolicy(context.getDataId(),
                                          classification.getRetentionPeriod());

        // Record processing activity
        processingLogService.recordProcessingActivity(
            ProcessingActivity.builder()
                .dataSubject(context.getDataSubject())
                .processingPurpose(context.getProcessingPurpose())
                .dataCategories(classification.getDataCategories())
                .legalBasis(context.getLegalBasis())
                .timestamp(Instant.now())
                .build()
        );
    }

    @Scheduled(fixedRate = 86400000)  // Daily
    public void enforceRetentionPolicies() {
        List<RetentionPolicy> expiredPolicies = retentionService.findExpiredPolicies();

        for (RetentionPolicy policy : expiredPolicies) {
            try {
                // Delete or anonymize expired data
                if (policy.getAction() == RetentionAction.DELETE) {
                    dataService.secureDelete(policy.getDataId());
                } else if (policy.getAction() == RetentionAction.ANONYMIZE) {
                    dataService.anonymize(policy.getDataId());
                }

                // Audit retention enforcement
                auditService.auditRetentionEnforcement(policy);

            } catch (Exception e) {
                log.error("Failed to enforce retention policy: {}", policy.getId(), e);
                alertingService.sendRetentionAlert(policy, e);
            }
        }
    }
}
```

## Security Testing and Validation

### Automated Security Testing

```java
@Component
@Profile("security-test")
public class SecurityTestSuite {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WasmRuntime runtime;

    @Test
    public void testInputValidation() {
        // Test various malicious inputs
        String[] maliciousInputs = {
            "../../../etc/passwd",
            "<script>alert('xss')</script>",
            "'; DROP TABLE users; --",
            "\u0000\u0001\u0002",  // Null bytes
            "A".repeat(10000),      // Buffer overflow attempt
        };

        for (String input : maliciousInputs) {
            ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/secure/execute",
                createExecutionRequest(input),
                String.class
            );

            // Should reject malicious input
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Test
    public void testModuleValidation() throws IOException {
        // Test malicious WebAssembly modules
        byte[] maliciousModule = createMaliciousModule();

        assertThatThrownBy(() -> {
            runtime.compileModule(runtime.createEngine(), maliciousModule);
        }).isInstanceOf(SecurityException.class);
    }

    @Test
    public void testResourceLimits() {
        // Test resource exhaustion protection
        ExecutionRequest request = ExecutionRequest.builder()
            .moduleId("memory-bomb")
            .functionName("allocate_large")
            .parameters(Map.of("size", Integer.MAX_VALUE))
            .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/secure/execute",
            request,
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testAuthenticationBypass() {
        // Test various authentication bypass attempts
        HttpHeaders headers = new HttpHeaders();

        // Try without authentication
        ResponseEntity<String> response1 = restTemplate.exchange(
            "/api/secure/execute",
            HttpMethod.POST,
            new HttpEntity<>(createExecutionRequest("test"), headers),
            String.class
        );
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Try with invalid token
        headers.setBearerAuth("invalid.jwt.token");
        ResponseEntity<String> response2 = restTemplate.exchange(
            "/api/secure/execute",
            HttpMethod.POST,
            new HttpEntity<>(createExecutionRequest("test"), headers),
            String.class
        );
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void testRateLimiting() {
        String validToken = generateValidToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validToken);

        // Make many requests quickly
        int successCount = 0;
        int rateLimitedCount = 0;

        for (int i = 0; i < 200; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                "/api/secure/execute",
                HttpMethod.POST,
                new HttpEntity<>(createExecutionRequest("test"), headers),
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                successCount++;
            } else if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                rateLimitedCount++;
            }
        }

        // Should have rate limited some requests
        assertThat(rateLimitedCount).isGreaterThan(0);
        assertThat(successCount).isLessThan(200);
    }

    private byte[] createMaliciousModule() throws IOException {
        // Create a WebAssembly module that tries to do malicious things
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // WebAssembly magic number
        baos.write(new byte[]{0x00, 0x61, 0x73, 0x6D});
        // Version
        baos.write(new byte[]{0x01, 0x00, 0x00, 0x00});

        // Add sections that try to bypass security
        // (This would be a real malicious module in practice)

        return baos.toByteArray();
    }
}
```

### Penetration Testing Checklist

```yaml
# penetration-testing-checklist.yml
wasmtime4j_security_tests:
  infrastructure:
    - network_scanning
    - port_enumeration
    - service_fingerprinting
    - vulnerability_scanning

  authentication:
    - password_attacks
    - session_management
    - jwt_token_attacks
    - privilege_escalation

  input_validation:
    - injection_attacks
    - buffer_overflow
    - format_string_attacks
    - path_traversal

  webassembly_specific:
    - malicious_module_upload
    - memory_corruption
    - sandbox_escape
    - resource_exhaustion

  api_security:
    - authentication_bypass
    - authorization_flaws
    - rate_limiting
    - data_exposure

  configuration:
    - default_credentials
    - insecure_defaults
    - information_disclosure
    - security_headers
```

This comprehensive security hardening guide provides detailed recommendations for securing Wasmtime4j deployments across all layers - from infrastructure to application code. Following these practices will help ensure your WebAssembly runtime environment meets enterprise security requirements and industry best practices.