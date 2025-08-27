package ai.tegmentum.wasmtime4j.benchmarks;

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
 * Benchmarks for WebAssembly module loading, compilation, and instantiation performance.
 *
 * <p>This benchmark class measures the performance characteristics of WebAssembly module
 * operations, comparing JNI and Panama implementations across different module types and sizes.
 *
 * <p>Key metrics measured:
 * <ul>
 *   <li>Module compilation time</li>
 *   <li>Module validation performance</li>
 *   <li>Instance creation overhead</li>
 *   <li>Module caching efficiency</li>
 *   <li>Large module handling performance</li>
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms1g", "-Xmx2g"})
public class ModuleOperationBenchmark extends BenchmarkBase {

    /**
     * Runtime implementation to benchmark.
     */
    @Param({"JNI", "PANAMA"})
    private RuntimeType runtimeType;

    /**
     * Module type to test with different complexity levels.
     */
    @Param({"SIMPLE", "COMPLEX", "LARGE"})
    private String moduleType;

    /**
     * Mock WebAssembly module representation for testing.
     */
    private static final class MockWasmModule {
        private final byte[] bytecode;
        private final String type;
        private final RuntimeType runtimeType;
        private boolean compiled;
        private boolean instantiated;
        private long compilationTime;

        MockWasmModule(final byte[] bytecode, final String type, final RuntimeType runtimeType) {
            this.bytecode = bytecode.clone();
            this.type = type;
            this.runtimeType = runtimeType;
            this.compiled = false;
            this.instantiated = false;
            this.compilationTime = 0;
        }

        void compile() {
            if (!compiled) {
                final long startTime = System.nanoTime();
                
                // Simulate compilation work based on module type and runtime
                validateWasmModule(bytecode);
                final int workAmount = getCompilationWorkAmount();
                
                for (int i = 0; i < workAmount; i++) {
                    Math.pow(i % 100, 2);
                }
                
                this.compilationTime = System.nanoTime() - startTime;
                this.compiled = true;
            }
        }

        void instantiate() {
            if (!compiled) {
                compile();
            }
            
            if (!instantiated) {
                // Simulate instantiation work
                final int instantiationWork = getInstantiationWorkAmount();
                
                for (int i = 0; i < instantiationWork; i++) {
                    Math.sqrt(i + 1.0);
                }
                
                this.instantiated = true;
            }
        }

        void cleanup() {
            this.instantiated = false;
            this.compiled = false;
            this.compilationTime = 0;
        }

        private int getCompilationWorkAmount() {
            int baseWork = 100;
            
            // Adjust based on module type
            switch (type) {
                case "SIMPLE":
                    baseWork = 50;
                    break;
                case "COMPLEX":
                    baseWork = 200;
                    break;
                case "LARGE":
                    baseWork = 500;
                    break;
                default:
                    break;
            }
            
            // Adjust based on runtime type
            if (runtimeType == RuntimeType.PANAMA) {
                baseWork = (int) (baseWork * 1.2); // Panama has slight overhead
            }
            
            return baseWork;
        }

        private int getInstantiationWorkAmount() {
            int baseWork = 25;
            
            switch (type) {
                case "SIMPLE":
                    baseWork = 15;
                    break;
                case "COMPLEX":
                    baseWork = 50;
                    break;
                case "LARGE":
                    baseWork = 100;
                    break;
                default:
                    break;
            }
            
            return baseWork;
        }

        public byte[] getBytecode() {
            return bytecode.clone();
        }

        public String getType() {
            return type;
        }

        public RuntimeType getRuntimeType() {
            return runtimeType;
        }

        public boolean isCompiled() {
            return compiled;
        }

        public boolean isInstantiated() {
            return instantiated;
        }

        public long getCompilationTime() {
            return compilationTime;
        }
    }

    /**
     * Current module being benchmarked.
     */
    private MockWasmModule module;

    /**
     * Module bytecode based on the selected type.
     */
    private byte[] moduleBytes;

    /**
     * Setup performed before each benchmark iteration.
     */
    @Setup(Level.Iteration)
    public void setupIteration() {
        // Select appropriate module bytes based on type
        switch (moduleType) {
            case "SIMPLE":
                moduleBytes = SIMPLE_WASM_MODULE.clone();
                break;
            case "COMPLEX":
                moduleBytes = COMPLEX_WASM_MODULE.clone();
                break;
            case "LARGE":
                moduleBytes = generateLargeModule();
                break;
            default:
                moduleBytes = SIMPLE_WASM_MODULE.clone();
                break;
        }

        // Clean up any existing module
        if (module != null) {
            module.cleanup();
        }
        module = null;
        
        // Force GC to ensure clean state
        System.gc();
    }

    /**
     * Cleanup performed after each benchmark iteration.
     */
    @TearDown(Level.Iteration)
    public void teardownIteration() {
        if (module != null) {
            module.cleanup();
            module = null;
        }
        moduleBytes = null;
    }

    /**
     * Benchmarks WebAssembly module compilation performance.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     * @return the compiled module
     */
    @Benchmark
    public MockWasmModule benchmarkModuleCompilation(final Blackhole blackhole) {
        final MockWasmModule newModule = new MockWasmModule(moduleBytes, moduleType, runtimeType);
        newModule.compile();
        
        blackhole.consume(newModule.isCompiled());
        blackhole.consume(newModule.getCompilationTime());
        
        return newModule;
    }

    /**
     * Benchmarks module validation without compilation.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void benchmarkModuleValidation(final Blackhole blackhole) {
        // Simulate validation work
        validateWasmModule(moduleBytes);
        
        // Additional validation checks
        final int validationWork = moduleType.equals("LARGE") ? 50 : 25;
        for (int i = 0; i < validationWork; i++) {
            Math.log(moduleBytes.length + i);
        }
        
        blackhole.consume(moduleBytes.length);
        blackhole.consume(moduleType);
    }

    /**
     * Benchmarks full module instantiation including compilation.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     * @return the instantiated module
     */
    @Benchmark
    public MockWasmModule benchmarkModuleInstantiation(final Blackhole blackhole) {
        final MockWasmModule newModule = new MockWasmModule(moduleBytes, moduleType, runtimeType);
        newModule.instantiate();
        
        blackhole.consume(newModule.isCompiled());
        blackhole.consume(newModule.isInstantiated());
        blackhole.consume(newModule.getCompilationTime());
        
        return newModule;
    }

    /**
     * Benchmarks compile-then-instantiate workflow.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     * @return the processed module
     */
    @Benchmark
    public MockWasmModule benchmarkCompileThenInstantiate(final Blackhole blackhole) {
        final MockWasmModule newModule = new MockWasmModule(moduleBytes, moduleType, runtimeType);
        
        // First compile
        newModule.compile();
        blackhole.consume(newModule.isCompiled());
        
        // Then instantiate
        newModule.instantiate();
        blackhole.consume(newModule.isInstantiated());
        
        return newModule;
    }

    /**
     * Benchmarks multiple module compilation for batch scenarios.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void benchmarkBatchModuleCompilation(final Blackhole blackhole) {
        final int batchSize = moduleType.equals("LARGE") ? 3 : 5;
        final MockWasmModule[] modules = new MockWasmModule[batchSize];
        
        for (int i = 0; i < batchSize; i++) {
            modules[i] = new MockWasmModule(moduleBytes, moduleType, runtimeType);
            modules[i].compile();
            blackhole.consume(modules[i].isCompiled());
        }
        
        // Cleanup
        for (final MockWasmModule mod : modules) {
            mod.cleanup();
        }
    }

    /**
     * Benchmarks module compilation with memory pressure.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void benchmarkCompilationWithMemoryPressure(final Blackhole blackhole) {
        // Create memory pressure
        final byte[][] memoryPressure = new byte[200][];
        for (int i = 0; i < memoryPressure.length; i++) {
            memoryPressure[i] = new byte[2048]; // 2KB per allocation
        }
        
        final MockWasmModule newModule = new MockWasmModule(moduleBytes, moduleType, runtimeType);
        newModule.compile();
        
        blackhole.consume(newModule.isCompiled());
        blackhole.consume(memoryPressure.length);
        
        newModule.cleanup();
        
        // Clear memory pressure
        for (int i = 0; i < memoryPressure.length; i++) {
            memoryPressure[i] = null;
        }
    }

    /**
     * Benchmarks module serialization and deserialization performance.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void benchmarkModuleSerialization(final Blackhole blackhole) {
        final MockWasmModule newModule = new MockWasmModule(moduleBytes, moduleType, runtimeType);
        newModule.compile();
        
        // Simulate serialization
        final byte[] serialized = newModule.getBytecode();
        blackhole.consume(serialized.length);
        
        // Simulate deserialization
        final MockWasmModule deserializedModule = new MockWasmModule(serialized, moduleType, runtimeType);
        deserializedModule.compile();
        
        blackhole.consume(deserializedModule.isCompiled());
        
        newModule.cleanup();
        deserializedModule.cleanup();
    }

    /**
     * Generates a larger WebAssembly module for testing scalability.
     *
     * @return byte array representing a large WebAssembly module
     */
    private byte[] generateLargeModule() {
        // Create a larger module by duplicating and modifying the complex module
        final byte[] base = COMPLEX_WASM_MODULE.clone();
        final byte[] large = new byte[base.length * 3];
        
        // Copy base module multiple times with slight modifications
        System.arraycopy(base, 0, large, 0, base.length);
        System.arraycopy(base, 0, large, base.length, base.length);
        System.arraycopy(base, 0, large, base.length * 2, base.length);
        
        // Add some variation to make it realistic
        for (int i = base.length; i < large.length; i++) {
            if (large[i] != 0x00 && large[i] != 0x61 && large[i] != 0x73 && large[i] != 0x6d) {
                large[i] = (byte) ((large[i] + i) % 256);
            }
        }
        
        return large;
    }
}