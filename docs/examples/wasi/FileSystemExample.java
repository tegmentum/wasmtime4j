package ai.tegmentum.wasmtime4j.examples.wasi;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.exception.*;
import ai.tegmentum.wasmtime4j.jni.wasi.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;

/**
 * Comprehensive WASI file system integration example.
 * 
 * This example demonstrates:
 * - Secure file system access with sandboxing
 * - Directory preopen configuration
 * - File operations from WebAssembly modules
 * - Permission management and security controls
 * - Error handling for file system operations
 */
public class FileSystemExample {
    
    private final WasmRuntime runtime;
    private final Engine engine;
    
    public FileSystemExample() throws WasmException {
        this.runtime = WasmRuntimeFactory.create();
        this.engine = runtime.createEngine();
    }
    
    public static void main(String[] args) throws Exception {
        FileSystemExample example = new FileSystemExample();
        
        try {
            // Setup sandbox environment
            example.setupSandboxEnvironment();
            
            // Demonstrate various WASI file operations
            example.demonstrateBasicFileOperations();
            example.demonstrateDirectoryOperations();
            example.demonstrateSecureFileAccess();
            example.demonstrateAdvancedFileOperations();
            
        } finally {
            example.cleanup();
        }
    }
    
    /**
     * Setup a secure sandbox environment for file operations.
     */
    private void setupSandboxEnvironment() throws IOException {
        System.out.println("=== Setting up WASI File System Sandbox ===");
        
        // Create sandbox directories
        Path sandboxRoot = Files.createTempDirectory("wasi-sandbox");
        Path dataDir = sandboxRoot.resolve("data");
        Path tempDir = sandboxRoot.resolve("temp");
        Path readOnlyDir = sandboxRoot.resolve("readonly");
        
        Files.createDirectories(dataDir);
        Files.createDirectories(tempDir);
        Files.createDirectories(readOnlyDir);
        
        // Create sample files
        Files.write(dataDir.resolve("input.txt"), "Hello, WASI File System!".getBytes());
        Files.write(readOnlyDir.resolve("config.json"), 
            "{\"version\": \"1.0\", \"readonly\": true}".getBytes());
        
        System.out.printf("Sandbox created at: %s%n", sandboxRoot);
        System.out.printf("Data directory: %s%n", dataDir);
        System.out.printf("Temp directory: %s%n", tempDir);
        System.out.printf("Read-only directory: %s%n", readOnlyDir);
    }
    
    /**
     * Demonstrate basic file operations through WASI.
     */
    private void demonstrateBasicFileOperations() throws Exception {
        System.out.println("\n=== Basic File Operations ===");
        
        // Create WASI context with controlled file access
        WasiContext wasiContext = createBasicWasiContext();
        
        // Load WASI-enabled WebAssembly module
        byte[] wasmBytes = loadFileOperationModule();
        Module module = runtime.compileModule(engine, wasmBytes);
        
        // Create imports with WASI
        ImportMap imports = new ImportMap();
        wasiContext.addToImports(imports);
        
        // Instantiate module
        Instance instance = runtime.instantiate(module, imports);
        
        // Demonstrate file reading
        executeFileRead(instance, "/data/input.txt");
        
        // Demonstrate file writing
        executeFileWrite(instance, "/temp/output.txt", "Hello from WebAssembly!");
        
        // Demonstrate file metadata access
        executeFileMetadata(instance, "/data/input.txt");
    }
    
    /**
     * Demonstrate directory operations.
     */
    private void demonstrateDirectoryOperations() throws Exception {
        System.out.println("\n=== Directory Operations ===");
        
        WasiContext wasiContext = createDirectoryWasiContext();
        
        byte[] wasmBytes = loadDirectoryOperationModule();
        Module module = runtime.compileModule(engine, wasmBytes);
        
        ImportMap imports = new ImportMap();
        wasiContext.addToImports(imports);
        
        Instance instance = runtime.instantiate(module, imports);
        
        // List directory contents
        executeDirectoryList(instance, "/data");
        
        // Create directory
        executeDirectoryCreate(instance, "/temp/newdir");
        
        // Directory metadata
        executeDirectoryMetadata(instance, "/temp");
    }
    
    /**
     * Demonstrate secure file access with permission controls.
     */
    private void demonstrateSecureFileAccess() throws Exception {
        System.out.println("\n=== Secure File Access ===");
        
        // Create restrictive WASI context
        WasiContext secureContext = WasiContextBuilder.create()
            .preopenDirectory(getDataDirectory().toString(), "/data", WasiDirectoryAccess.READ_ONLY)
            .preopenDirectory(getTempDirectory().toString(), "/temp", WasiDirectoryAccess.READ_WRITE)
            // Explicitly deny access to sensitive directories
            .denyDirectory("/etc")
            .denyDirectory("/home")
            .denyDirectory("/root")
            // Set resource limits
            .resourceLimits(WasiResourceLimits.builder()
                .maxOpenFiles(5)
                .maxFileSize(1024 * 1024) // 1MB limit
                .build())
            .build();
        
        byte[] wasmBytes = loadSecureFileModule();
        Module module = runtime.compileModule(engine, wasmBytes);
        
        ImportMap imports = new ImportMap();
        secureContext.addToImports(imports);
        
        Instance instance = runtime.instantiate(module, imports);
        
        // Test allowed operations
        try {
            executeSecureFileRead(instance, "/data/input.txt");
            System.out.println("✓ Read from allowed directory succeeded");
        } catch (Exception e) {
            System.out.println("✗ Read from allowed directory failed: " + e.getMessage());
        }
        
        // Test denied operations
        try {
            executeSecureFileWrite(instance, "/data/forbidden.txt", "Should fail");
            System.out.println("✗ Write to read-only directory should have failed!");
        } catch (Exception e) {
            System.out.println("✓ Write to read-only directory correctly denied: " + e.getMessage());
        }
        
        // Test resource limits
        try {
            executeSecureFileWrite(instance, "/temp/large.txt", "x".repeat(2 * 1024 * 1024)); // 2MB
            System.out.println("✗ Large file write should have failed!");
        } catch (Exception e) {
            System.out.println("✓ Large file write correctly denied: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrate advanced file operations like seeking, truncation, etc.
     */
    private void demonstrateAdvancedFileOperations() throws Exception {
        System.out.println("\n=== Advanced File Operations ===");
        
        WasiContext wasiContext = createAdvancedWasiContext();
        
        byte[] wasmBytes = loadAdvancedFileModule();
        Module module = runtime.compileModule(engine, wasmBytes);
        
        ImportMap imports = new ImportMap();
        wasiContext.addToImports(imports);
        
        Instance instance = runtime.instantiate(module, imports);
        
        // Demonstrate file seeking
        executeFileSeek(instance, "/temp/seekable.txt");
        
        // Demonstrate file truncation
        executeFileTruncate(instance, "/temp/truncate.txt");
        
        // Demonstrate file locking
        executeFileLock(instance, "/temp/locked.txt");
        
        // Demonstrate async file operations
        executeAsyncFileOperations(instance);
    }
    
    // WASI Context Creation Methods
    
    private WasiContext createBasicWasiContext() throws IOException {
        return WasiContextBuilder.create()
            .inheritStdio()
            .preopenDirectory(getDataDirectory().toString(), "/data")
            .preopenDirectory(getTempDirectory().toString(), "/temp")
            .args("file-example", "--mode", "basic")
            .env("EXAMPLE_MODE", "basic")
            .build();
    }
    
    private WasiContext createDirectoryWasiContext() throws IOException {
        return WasiContextBuilder.create()
            .inheritStdio()
            .preopenDirectory(getDataDirectory().toString(), "/data")
            .preopenDirectory(getTempDirectory().toString(), "/temp")
            .preopenDirectory(getReadOnlyDirectory().toString(), "/readonly", WasiDirectoryAccess.READ_ONLY)
            .args("dir-example")
            .build();
    }
    
    private WasiContext createAdvancedWasiContext() throws IOException {
        return WasiContextBuilder.create()
            .inheritStdio()
            .preopenDirectory(getTempDirectory().toString(), "/temp", WasiDirectoryAccess.READ_WRITE)
            .resourceLimits(WasiResourceLimits.builder()
                .maxOpenFiles(20)
                .maxFileSize(10 * 1024 * 1024) // 10MB
                .maxExecutionTime(Duration.ofSeconds(30))
                .build())
            .build();
    }
    
    // File Operation Execution Methods
    
    private void executeFileRead(Instance instance, String path) throws WasmException {
        System.out.printf("Reading file: %s%n", path);
        
        WasmFunction readFunc = instance.getFunction("read_file");
        WasmMemory memory = instance.getMemory("memory");
        
        // Write path to memory
        int pathOffset = 1024;
        byte[] pathBytes = path.getBytes();
        memory.write(pathOffset, pathBytes);
        
        // Call read function
        WasmValue[] args = {
            WasmValue.i32(pathOffset),
            WasmValue.i32(pathBytes.length),
            WasmValue.i32(pathOffset + 1024) // Buffer for result
        };
        
        WasmValue[] results = readFunc.call(args);
        int bytesRead = results[0].asI32();
        
        if (bytesRead > 0) {
            byte[] content = memory.read(pathOffset + 1024, bytesRead);
            System.out.printf("  Content (%d bytes): %s%n", bytesRead, new String(content));
        } else {
            System.out.println("  File read failed or empty");
        }
    }
    
    private void executeFileWrite(Instance instance, String path, String content) throws WasmException {
        System.out.printf("Writing to file: %s%n", path);
        
        WasmFunction writeFunc = instance.getFunction("write_file");
        WasmMemory memory = instance.getMemory("memory");
        
        // Write path and content to memory
        int pathOffset = 1024;
        int contentOffset = 2048;
        
        byte[] pathBytes = path.getBytes();
        byte[] contentBytes = content.getBytes();
        
        memory.write(pathOffset, pathBytes);
        memory.write(contentOffset, contentBytes);
        
        // Call write function
        WasmValue[] args = {
            WasmValue.i32(pathOffset),
            WasmValue.i32(pathBytes.length),
            WasmValue.i32(contentOffset),
            WasmValue.i32(contentBytes.length)
        };
        
        WasmValue[] results = writeFunc.call(args);
        int bytesWritten = results[0].asI32();
        
        System.out.printf("  Wrote %d bytes%n", bytesWritten);
    }
    
    private void executeFileMetadata(Instance instance, String path) throws WasmException {
        System.out.printf("Getting metadata for: %s%n", path);
        
        WasmFunction metadataFunc = instance.getFunction("file_metadata");
        WasmMemory memory = instance.getMemory("memory");
        
        // Write path to memory
        int pathOffset = 1024;
        byte[] pathBytes = path.getBytes();
        memory.write(pathOffset, pathBytes);
        
        // Call metadata function
        WasmValue[] args = {
            WasmValue.i32(pathOffset),
            WasmValue.i32(pathBytes.length),
            WasmValue.i32(3072) // Metadata buffer
        };
        
        WasmValue[] results = metadataFunc.call(args);
        int success = results[0].asI32();
        
        if (success != 0) {
            // Read metadata structure (simplified)
            byte[] metadataBytes = memory.read(3072, 64);
            System.out.printf("  Metadata available (%d bytes)%n", metadataBytes.length);
        } else {
            System.out.println("  Failed to get metadata");
        }
    }
    
    private void executeDirectoryList(Instance instance, String path) throws WasmException {
        System.out.printf("Listing directory: %s%n", path);
        
        WasmFunction listFunc = instance.getFunction("list_directory");
        WasmMemory memory = instance.getMemory("memory");
        
        // Implementation similar to file operations...
        // For brevity, showing simplified version
        
        WasmValue[] results = listFunc.call(WasmValue.i32(1024), WasmValue.i32(path.length()));
        int entryCount = results[0].asI32();
        
        System.out.printf("  Found %d entries%n", entryCount);
    }
    
    private void executeDirectoryCreate(Instance instance, String path) throws WasmException {
        System.out.printf("Creating directory: %s%n", path);
        
        WasmFunction createFunc = instance.getFunction("create_directory");
        WasmValue[] results = createFunc.call(WasmValue.i32(1024), WasmValue.i32(path.length()));
        int success = results[0].asI32();
        
        System.out.printf("  Directory creation %s%n", success != 0 ? "succeeded" : "failed");
    }
    
    private void executeDirectoryMetadata(Instance instance, String path) throws WasmException {
        System.out.printf("Getting directory metadata: %s%n", path);
        // Similar implementation to file metadata...
    }
    
    private void executeSecureFileRead(Instance instance, String path) throws WasmException {
        executeFileRead(instance, path);
    }
    
    private void executeSecureFileWrite(Instance instance, String path, String content) throws WasmException {
        executeFileWrite(instance, path, content);
    }
    
    private void executeFileSeek(Instance instance, String path) throws WasmException {
        System.out.printf("Testing file seek operations on: %s%n", path);
        
        // First, create a file with known content
        executeFileWrite(instance, path, "0123456789ABCDEF");
        
        WasmFunction seekFunc = instance.getFunction("seek_and_read");
        WasmValue[] results = seekFunc.call(
            WasmValue.i32(1024), // path
            WasmValue.i32(path.length()),
            WasmValue.i32(5),    // seek position
            WasmValue.i32(3)     // bytes to read
        );
        
        int bytesRead = results[0].asI32();
        System.out.printf("  Seek and read returned %d bytes%n", bytesRead);
    }
    
    private void executeFileTruncate(Instance instance, String path) throws WasmException {
        System.out.printf("Testing file truncation: %s%n", path);
        
        // Create file with content
        executeFileWrite(instance, path, "This file will be truncated");
        
        WasmFunction truncateFunc = instance.getFunction("truncate_file");
        WasmValue[] results = truncateFunc.call(
            WasmValue.i32(1024), // path
            WasmValue.i32(path.length()),
            WasmValue.i32(10)    // new size
        );
        
        int success = results[0].asI32();
        System.out.printf("  Truncation %s%n", success != 0 ? "succeeded" : "failed");
    }
    
    private void executeFileLock(Instance instance, String path) throws WasmException {
        System.out.printf("Testing file locking: %s%n", path);
        
        WasmFunction lockFunc = instance.getFunction("lock_file");
        WasmValue[] results = lockFunc.call(
            WasmValue.i32(1024), // path
            WasmValue.i32(path.length()),
            WasmValue.i32(1)     // exclusive lock
        );
        
        int success = results[0].asI32();
        System.out.printf("  File locking %s%n", success != 0 ? "succeeded" : "failed");
    }
    
    private void executeAsyncFileOperations(Instance instance) throws WasmException {
        System.out.println("Testing async file operations");
        
        WasmFunction asyncFunc = instance.getFunction("async_file_op");
        WasmValue[] results = asyncFunc.call(WasmValue.i32(1024));
        
        int operationsCompleted = results[0].asI32();
        System.out.printf("  Completed %d async operations%n", operationsCompleted);
    }
    
    // Module Loading Methods (these would load actual WASM modules)
    
    private byte[] loadFileOperationModule() throws IOException {
        // In a real implementation, this would load a WASM module
        // For this example, we'll return a minimal module structure
        return createMockWasmModule("file_operations");
    }
    
    private byte[] loadDirectoryOperationModule() throws IOException {
        return createMockWasmModule("directory_operations");
    }
    
    private byte[] loadSecureFileModule() throws IOException {
        return createMockWasmModule("secure_file_operations");
    }
    
    private byte[] loadAdvancedFileModule() throws IOException {
        return createMockWasmModule("advanced_file_operations");
    }
    
    private byte[] createMockWasmModule(String moduleName) {
        // This is a simplified mock - in reality, you would compile from Rust/C/Go
        // Here's a minimal WASM module structure
        return new byte[] {
            0x00, 0x61, 0x73, 0x6d, // magic
            0x01, 0x00, 0x00, 0x00, // version
            // Minimal sections would follow...
        };
    }
    
    // Helper methods
    
    private Path getDataDirectory() throws IOException {
        return Files.createTempDirectory("wasi-data");
    }
    
    private Path getTempDirectory() throws IOException {
        return Files.createTempDirectory("wasi-temp");
    }
    
    private Path getReadOnlyDirectory() throws IOException {
        return Files.createTempDirectory("wasi-readonly");
    }
    
    private void cleanup() {
        runtime.close();
        System.out.println("\nCleaned up resources");
    }
}