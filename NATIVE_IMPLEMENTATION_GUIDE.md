# Native Implementation Guide

This document describes the Rust/JNI native methods that need to be implemented to complete the wasmtime4j JNI backend.

## Overview

The Java JNI bridge has been fully implemented. The following Rust native methods need to be implemented in the `wasmtime4j-native` module to complete the functionality.

## Store Native Methods

### 1. `nativeCreateTable`

**Signature:**
```rust
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateTable(
    env: JNIEnv,
    _class: JClass,
    store_handle: jlong,
    element_type: jint,
    initial_size: jint,
    max_size: jint,
) -> jlong
```

**Implementation:**
- Extract Store from `store_handle`
- Create `TableType` with element type (funcref/externref), initial size, and max size (-1 = unlimited)
- Create `Table::new(&mut store, table_type, init_value)`
- Store table in handle registry
- Return table handle (or 0 on failure)

**Wasmtime API:**
```rust
use wasmtime::{Table, TableType, ValType, Limits};

let table_type = TableType::new(
    ValType::FuncRef, // or ValType::ExternRef
    Limits::new(initial_size, Some(max_size))
);
let table = Table::new(&mut store, table_type, Val::FuncRef(None))?;
```

### 2. `nativeCreateMemory`

**Signature:**
```rust
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateMemory(
    env: JNIEnv,
    _class: JClass,
    store_handle: jlong,
    initial_pages: jint,
    max_pages: jint,
) -> jlong
```

**Implementation:**
- Extract Store from `store_handle`
- Create `MemoryType` with initial pages and max pages (-1 = unlimited)
- Create `Memory::new(&mut store, memory_type)`
- Store memory in handle registry
- Return memory handle (or 0 on failure)

**Wasmtime API:**
```rust
use wasmtime::{Memory, MemoryType, Limits};

let memory_type = MemoryType::new(Limits::new(
    initial_pages,
    if max_pages == -1 { None } else { Some(max_pages) }
));
let memory = Memory::new(&mut store, memory_type)?;
```

## Linker Native Methods

### 3. `nativeDefineHostFunction`

**Signature:**
```rust
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeDefineHostFunction(
    env: JNIEnv,
    _object: JObject,
    linker_handle: jlong,
    module_name: JString,
    name: JString,
    param_types: jintArray,
    return_types: jintArray,
    callback_id: jlong,
) -> jboolean
```

**Implementation:**
- Extract Linker from `linker_handle`
- Convert JStrings to Rust strings
- Convert type arrays to `Vec<ValType>`
- Create `FuncType` from param/return types
- Create Wasmtime `Func` that invokes Java callback via JNI
- Call `linker.define(module_name, name, func)`
- Return JNI_TRUE on success

**Wasmtime API:**
```rust
use wasmtime::{Linker, Func, FuncType, ValType};

let func_type = FuncType::new(params, returns);
let func = Func::new(&mut store, func_type, move |mut caller, params, results| {
    // Call back to Java using callback_id
    // This requires JNI GlobalRef to HostFunction implementation
    // Invoke: implementation.execute(params) -> results
    Ok(())
});

linker.define(&module_name, &name, func)?;
```

**Critical: Java Callback Bridge**
- Store Java `HostFunction` GlobalRef keyed by `callback_id`
- In Rust closure, attach to JVM, get GlobalRef, invoke `execute()`
- Convert Wasmtime `Val[]` → Java `WasmValue[]`
- Convert Java `WasmValue[]` → Wasmtime `Val[]`
- Handle exceptions and propagate as traps

### 4. `nativeDefineMemory`

**Signature:**
```rust
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeDefineMemory(
    env: JNIEnv,
    _object: JObject,
    linker_handle: jlong,
    module_name: JString,
    name: JString,
    memory_handle: jlong,
) -> jboolean
```

**Implementation:**
- Extract Linker and Memory from handles
- Convert module_name and name to Rust strings
- Call `linker.define(module_name, name, memory)`
- Return JNI_TRUE on success

**Wasmtime API:**
```rust
linker.define(&module_name, &name, memory)?;
```

### 5. `nativeDefineTable`

**Signature:**
```rust
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeDefineTable(
    env: JNIEnv,
    _object: JObject,
    linker_handle: jlong,
    module_name: JString,
    name: JString,
    table_handle: jlong,
) -> jboolean
```

**Implementation:**
- Extract Linker and Table from handles
- Convert module_name and name to Rust strings
- Call `linker.define(module_name, name, table)`
- Return JNI_TRUE on success

**Wasmtime API:**
```rust
linker.define(&module_name, &name, table)?;
```

### 6. `nativeDefineGlobal`

**Signature:**
```rust
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeDefineGlobal(
    env: JNIEnv,
    _object: JObject,
    linker_handle: jlong,
    module_name: JString,
    name: JString,
    global_handle: jlong,
) -> jboolean
```

**Implementation:**
- Extract Linker and Global from handles
- Convert module_name and name to Rust strings
- Call `linker.define(module_name, name, global)`
- Return JNI_TRUE on success

**Wasmtime API:**
```rust
linker.define(&module_name, &name, global)?;
```

### 7. `nativeDefineInstance`

**Signature:**
```rust
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeDefineInstance(
    env: JNIEnv,
    _object: JObject,
    linker_handle: jlong,
    module_name: JString,
    instance_handle: jlong,
) -> jboolean
```

**Implementation:**
- Extract Linker and Instance from handles
- Convert module_name to Rust string
- Call `linker.instance(&mut store, module_name, instance)`
- Return JNI_TRUE on success

**Wasmtime API:**
```rust
linker.instance(&mut store, &module_name, instance)?;
```

### 8. `nativeInstantiate`

**Signature:**
```rust
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeInstantiate(
    env: JNIEnv,
    _object: JObject,
    linker_handle: jlong,
    store_handle: jlong,
    module_handle: jlong,
) -> jlong
```

**Implementation:**
- Extract Linker, Store, and Module from handles
- Call `linker.instantiate(&mut store, &module)`
- Store instance in handle registry
- Return instance handle (or 0 on failure)

**Wasmtime API:**
```rust
let instance = linker.instantiate(&mut store, &module)?;
```

### 9. `nativeInstantiateNamed`

**Signature:**
```rust
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeInstantiateNamed(
    env: JNIEnv,
    _object: JObject,
    linker_handle: jlong,
    store_handle: jlong,
    module_name: JString,
    module_handle: jlong,
) -> jlong
```

**Implementation:**
- Extract Linker, Store, and Module from handles
- Convert module_name to Rust string
- Call `linker.instantiate(&mut store, &module)`
- Call `linker.instance(&mut store, module_name, instance)` to register
- Store instance in handle registry
- Return instance handle (or 0 on failure)

**Wasmtime API:**
```rust
let instance = linker.instantiate(&mut store, &module)?;
linker.instance(&mut store, &module_name, instance)?;
```

## Implementation Checklist

### Store Methods
- [ ] `nativeCreateTable` - Create tables from Java
- [ ] `nativeCreateMemory` - Create memory from Java

### Linker Methods
- [ ] `nativeDefineHostFunction` - Register host functions (requires callback bridge)
- [ ] `nativeDefineMemory` - Register memory imports
- [ ] `nativeDefineTable` - Register table imports
- [ ] `nativeDefineGlobal` - Register global imports
- [ ] `nativeDefineInstance` - Register instance imports
- [ ] `nativeInstantiate` - Instantiate with imports
- [ ] `nativeInstantiateNamed` - Instantiate and register

### Critical Infrastructure
- [ ] **Host Function Callback Bridge** - Java↔Rust callback mechanism
  - Store Java GlobalRef for each host function
  - Attach to JVM thread in Rust closure
  - Convert Val[] ↔ WasmValue[]
  - Handle exceptions/traps

## Testing Strategy

Once implemented, run:

```bash
# Test host functions
mvn test -Dtest=HostFunctionTest -pl wasmtime4j-comparison-tests

# Test globals
mvn test -Dtest=GlobalsTest -pl wasmtime4j-comparison-tests

# Test tables
mvn test -Dtest=TablesTest -pl wasmtime4j-comparison-tests

# Test linker
mvn test -Dtest=LinkerTest -pl wasmtime4j-comparison-tests

# Test WASI
mvn test -Dtest=WasiTest -pl wasmtime4j-comparison-tests
```

## Key References

- **Wasmtime Rust API**: https://docs.rs/wasmtime/latest/wasmtime/
- **JNI Spec**: https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/jniTOC.html
- **Existing JNI implementations**: `/Users/zacharywhitley/git/wasmtime4j/wasmtime4j-native/src/`

## Notes

1. **Thread Safety**: All JNI calls must attach to JVM thread
2. **Error Handling**: Convert Wasmtime errors to Java WasmException
3. **Memory Management**: Use GlobalRef for persistent Java object references
4. **Handle Registry**: Maintain thread-safe handle→object mapping
5. **Defensive Programming**: Validate all handles before dereferencing
