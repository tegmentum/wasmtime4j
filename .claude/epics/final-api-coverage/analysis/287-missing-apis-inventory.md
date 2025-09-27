# Missing APIs Detailed Inventory

**Issue**: #287 - API Gap Analysis and Prioritization
**Date**: 2025-09-27
**For**: Implementation tasks #288-296

## P0 Critical APIs (Must Implement)

### Linker APIs
```rust
// Core Linker functionality
pub struct Linker<T> { .. }
impl<T> Linker<T> {
    pub fn new(engine: &Engine) -> Linker<T>
    pub fn define(&mut self, module: &str, name: &str, item: Extern) -> Result<()>
    pub fn define_func(&mut self, module: &str, name: &str, func: Func) -> Result<()>
    pub fn define_wasi(&mut self) -> Result<()>
    pub fn instantiate(&self, store: &mut Store<T>, module: &Module) -> Result<Instance>
    pub fn get_default(&self, store: &mut Store<T>, name: &str) -> Result<Func>
}

// LinkerInstance for scoped imports
pub struct LinkerInstance<'a, T> { .. }
impl<'a, T> LinkerInstance<'a, T> {
    pub fn define(&mut self, name: &str, item: Extern) -> Result<()>
    pub fn define_func(&mut self, name: &str, func: Func) -> Result<()>
}
```

**Java API Design:**
```java
public interface Linker<T> {
    void define(String module, String name, Extern item) throws WasmtimeException;
    void defineFunc(String module, String name, Function func) throws WasmtimeException;
    void defineWasi() throws WasmtimeException;
    WasmInstance instantiate(Store<T> store, Module module) throws WasmtimeException;
    Function getDefault(Store<T> store, String name) throws WasmtimeException;
}
```

### Module Serialization APIs
```rust
impl Module {
    pub fn serialize(&self) -> Result<Vec<u8>>
    pub fn deserialize(engine: &Engine, bytes: &[u8]) -> Result<Module>
    pub fn deserialize_file(engine: &Engine, path: impl AsRef<Path>) -> Result<Module>
}
```

**Java API Design:**
```java
public interface Module {
    byte[] serialize() throws WasmtimeException;
    static Module deserialize(Engine engine, byte[] bytes) throws WasmtimeException;
    static Module deserializeFile(Engine engine, Path path) throws WasmtimeException;
}
```

### WASI APIs
```rust
// Core WASI implementation
pub struct WasiCtx { .. }
impl WasiCtx {
    pub fn new() -> WasiCtx
    pub fn set_argv(&mut self, argv: &[String]) -> &mut Self
    pub fn set_env(&mut self, key: &str, value: &str) -> &mut Self
    pub fn inherit_stdio(&mut self) -> &mut Self
    pub fn preopened_dir(&mut self, path: impl AsRef<Path>, guest_path: &str) -> Result<&mut Self>
}

// WASI integration
pub fn add_to_linker(linker: &mut Linker<WasiCtx>, get_cx: impl Fn(&mut WasiCtx) -> &mut WasiCtx + Send + Sync + Copy + 'static) -> Result<()>
```

**Java API Design:**
```java
public interface WasiContext {
    WasiContext setArgv(String[] argv);
    WasiContext setEnv(String key, String value);
    WasiContext inheritStdio();
    WasiContext preopenedDir(Path path, String guestPath) throws WasmtimeException;
}

public class WasiLinker {
    public static void addToLinker(Linker<WasiContext> linker) throws WasmtimeException;
}
```

### Caller Context APIs
```rust
// Host function caller context
pub struct Caller<'a, T> { .. }
impl<'a, T> Caller<'a, T> {
    pub fn data(&self) -> &T
    pub fn data_mut(&mut self) -> &mut T
    pub fn get_export(&self, name: &str) -> Option<Extern>
    pub fn get_func(&self, name: &str) -> Option<Func>
}

// Host functions with caller
type HostFunc<T> = dyn Fn(Caller<'_, T>, &[Val], &mut [Val]) -> Result<()> + Send + Sync;
```

**Java API Design:**
```java
public interface Caller<T> {
    T data();
    Optional<Extern> getExport(String name);
    Optional<Function> getFunc(String name);
}

@FunctionalInterface
public interface HostFunction<T> {
    void call(Caller<T> caller, Val[] params, Val[] results) throws WasmtimeException;
}
```

## P1 Strategic APIs (Should Implement)

### Component Model APIs
```rust
pub struct Component { .. }
pub struct ComponentInstance { .. }
pub struct ComponentLinker<T> { .. }

impl Component {
    pub fn new(engine: &Engine, bytes: impl AsRef<[u8]>) -> Result<Component>
    pub fn validate(engine: &Engine, bytes: impl AsRef<[u8]>) -> Result<()>
}

impl ComponentLinker<T> {
    pub fn new(engine: &Engine) -> ComponentLinker<T>
    pub fn instantiate(&self, store: &mut Store<T>, component: &Component) -> Result<ComponentInstance>
}
```

### Async APIs
```rust
pub struct AsyncEngine { .. }
pub struct AsyncStore<T> { .. }

impl AsyncEngine {
    pub fn new(config: &Config) -> Result<AsyncEngine>
}

impl<T> AsyncStore<T> {
    pub fn new(engine: &AsyncEngine, data: T) -> AsyncStore<T>
    pub async fn call_async(&mut self, func: &Func, params: &[Val], results: &mut [Val]) -> Result<()>
}
```

### Advanced Memory APIs
```rust
pub struct SharedMemory { .. }
impl SharedMemory {
    pub fn new(engine: &Engine, ty: MemoryType) -> Result<SharedMemory>
    pub fn atomic_load8(&self, offset: usize) -> Result<u8>
    pub fn atomic_store8(&self, offset: usize, value: u8) -> Result<()>
    pub fn atomic_rmw_add(&self, offset: usize, value: u64) -> Result<u64>
}

impl Memory {
    pub fn copy(&self, dst: usize, src: usize, len: usize) -> Result<()>
    pub fn fill(&self, dst: usize, val: u8, len: usize) -> Result<()>
    pub fn atomic_notify(&self, offset: usize, count: u32) -> Result<u32>
    pub fn atomic_wait32(&self, offset: usize, expected: u32, timeout: Option<Duration>) -> Result<WaitResult>
}
```

## P2 Enhancement APIs (Nice to Have)

### Advanced Engine Configuration
```rust
impl Config {
    pub fn profiler(&mut self, profiler: ProfilingStrategy) -> &mut Self
    pub fn epoch_interruption(&mut self, enable: bool) -> &mut Self
    pub fn memory_init_cow(&mut self, enable: bool) -> &mut Self
    pub fn memory_guaranteed_dense_image_size(&mut self, size: u64) -> &mut Self
}
```

### Advanced Store APIs
```rust
impl<T> Store<T> {
    pub fn set_fuel(&mut self, fuel: u64) -> Result<()>
    pub fn fuel_consumed(&self) -> Option<u64>
    pub fn set_epoch_deadline(&mut self, ticks: u64)
    pub fn interrupt_handle(&self) -> Result<InterruptHandle>
}
```

### Module Validation and Introspection
```rust
impl Module {
    pub fn validate_with_features(engine: &Engine, bytes: &[u8], features: &WasmFeatures) -> Result<()>
    pub fn imports(&self) -> impl Iterator<Item = ImportType>
    pub fn exports(&self) -> impl Iterator<Item = ExportType>
    pub fn get_type(&self, index: u32) -> Option<&dyn WasmTy>
}
```

### Advanced Function APIs
```rust
impl Func {
    pub fn call_unchecked(&self, store: &mut Store<T>, args_and_results: &mut [Val]) -> Result<()>
    pub fn typed_unchecked<Params, Results>(&self, store: &Store<T>) -> Result<TypedFunc<Params, Results>>
    pub fn get_host_func(&self) -> Option<*const dyn Fn()>
}
```

## Implementation Guidance by Module

### wasmtime4j-native (Rust)
**Priority**: Implement P0 APIs first, focus on:
1. Linker struct and methods
2. Module serialization functions
3. WASI context and integration
4. Caller context parameter passing

**Key Files to Create**:
- `src/linker.rs` - Core linker implementation
- `src/serialization.rs` - Module serialize/deserialize
- `src/wasi.rs` - WASI implementation
- `src/caller.rs` - Caller context management

### wasmtime4j-jni (Java + JNI)
**Priority**: Create Java interfaces first, then JNI bindings
1. `Linker.java` interface and implementation
2. Module serialization methods
3. `WasiContext.java` and related classes
4. `Caller.java` interface and host function support

**Key Files to Create**:
- `Linker.java`, `LinkerInstance.java`
- `WasiContext.java`, `WasiLinker.java`
- `Caller.java`, `HostFunction.java` interface
- Native method declarations and JNI implementations

### wasmtime4j-panama (Java + Panama)
**Priority**: Same interfaces as JNI, different native binding
1. Reuse Java interfaces from JNI implementation
2. Implement Panama FFI bindings for native functions
3. Memory layout and foreign function signatures
4. Type marshalling for complex structures

## Testing Requirements

### P0 API Testing
- [ ] Linker import/export resolution with host functions
- [ ] Module serialization round-trip validation
- [ ] WASI filesystem and stdio operations
- [ ] Caller context memory access

### P1 API Testing
- [ ] Component instantiation and calling
- [ ] Async function execution
- [ ] SharedMemory multi-threaded access
- [ ] Advanced memory operations

### Integration Testing
- [ ] Cross-implementation consistency (JNI vs Panama)
- [ ] Performance regression validation
- [ ] WASI compliance with standard test suites
- [ ] Real-world usage scenarios

## Success Criteria

**API Completeness**: 100% coverage of Wasmtime 36.0.2 stable APIs
**Performance**: <10% overhead vs direct Rust calls
**Compatibility**: All existing functionality preserved
**Documentation**: Complete Javadoc for all new APIs
**Testing**: >95% code coverage, comprehensive integration tests