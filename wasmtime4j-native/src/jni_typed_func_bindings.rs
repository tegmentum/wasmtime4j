//! JNI bindings for TypedFunc
//!
//! Provides Java Native Interface bindings for zero-cost typed function calls.

use crate::error::WasmtimeResult;
use crate::store::Store as WasmStore;
use crate::typed_func::TypedFunc as CustomTypedFunc;
use jni::objects::{JClass, JObject, JValue};
use jni::sys::{jboolean, jdouble, jfloat, jint, jlong, jobject};
use jni::JNIEnv;
use std::os::raw::c_void;
use std::sync::Arc;
use wasmtime::{Func, WasmParams, WasmResults};

/// Macro to validate and dereference pointers with error handling
///
/// This prevents JVM crashes by validating pointers before use.
/// Returns early with Java exception if validation fails.
macro_rules! validate_and_deref {
    ($env:expr, $ptr:expr, $type:ty, $name:expr) => {{
        if $ptr == 0 {
            let _ = $env.throw_new(
                "java/lang/IllegalStateException",
                format!("{} pointer is null", $name)
            );
            return Default::default();
        }
        unsafe { &*($ptr as *const $type) }
    }};
    (mut $env:expr, $ptr:expr, $type:ty, $name:expr) => {{
        if $ptr == 0 {
            let _ = $env.throw_new(
                "java/lang/IllegalStateException",
                format!("{} pointer is null", $name)
            );
            return Default::default();
        }
        unsafe { &*($ptr as *const $type) }  // Use const reference, not mut, for Store
    }};
}

/// Create a typed function from a regular function
///
/// Signature types are encoded as strings:
/// - "i" = i32
/// - "I" = i64
/// - "f" = f32
/// - "F" = f64
/// - "v" = void/none
///
/// Format: "params->results", e.g., "ii->i" for (i32, i32) -> i32
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCreate(
    mut env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
    func_ptr: jlong,
    signature: JObject,
) -> jlong {
    let store = unsafe { &mut *(store_ptr as *mut WasmStore) };
    let func = unsafe { &*(func_ptr as *const Func) };

    // Get signature string
    let sig_str: String = match env.get_string(&signature.into()) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", format!("Invalid signature: {}", e));
            return 0;
        }
    };

    // Parse signature and create appropriate typed function
    // For now, we'll create a generic wrapper that stores the signature
    // The actual typed call will be handled based on the signature at call time

    // Create a TypedFuncHandle that stores both the function and signature
    let handle = Box::new(TypedFuncHandle {
        func: func.clone(),
        signature: sig_str,
    });

    Box::into_raw(handle) as jlong
}

/// Call a typed function with i32 parameters and i32 result
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallI32ToI32(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param: jint,
) -> jint {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return 0;
    }

    match call_typed_i32_to_i32(&handle.func, store, param) {
        Ok(result) => result,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
            0
        }
    }
}

/// Call a typed function with (i32, i32) parameters and i32 result
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallI32I32ToI32(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param1: jint,
    param2: jint,
) -> jint {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return 0;
    }

    match call_typed_i32i32_to_i32(&handle.func, store, param1, param2) {
        Ok(result) => result,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
            0
        }
    }
}

/// Call a typed function with i64 parameters and i64 result
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallI64ToI64(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param: jlong,
) -> jlong {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return 0;
    }

    match call_typed_i64_to_i64(&handle.func, store, param) {
        Ok(result) => result,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
            0
        }
    }
}

/// Call a typed function with (i64, i64) parameters and i64 result
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallI64I64ToI64(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param1: jlong,
    param2: jlong,
) -> jlong {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return 0;
    }

    match call_typed_i64i64_to_i64(&handle.func, store, param1, param2) {
        Ok(result) => result,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
            0
        }
    }
}

/// Call a typed function with f32 parameters and f32 result
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallF32ToF32(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param: jfloat,
) -> jfloat {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return 0.0;
    }

    match call_typed_f32_to_f32(&handle.func, store, param) {
        Ok(result) => result,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
            0.0
        }
    }
}

/// Call a typed function with f64 parameters and f64 result
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallF64ToF64(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param: jdouble,
) -> jdouble {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return 0.0;
    }

    match call_typed_f64_to_f64(&handle.func, store, param) {
        Ok(result) => result,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
            0.0
        }
    }
}

/// Call a typed function with no parameters and no result
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallVoidToVoid(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
) {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return;
    }

    if let Err(e) = call_typed_void_to_void(&handle.func, store) {
        let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
    }
}

/// Call a typed function with (i32) -> void
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallI32ToVoid(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param: jint,
) {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return;
    }

    if let Err(e) = call_typed_i32_to_void(&handle.func, store, param) {
        let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
    }
}

/// Call a typed function with (i32, i32) -> void
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallI32I32ToVoid(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param1: jint,
    param2: jint,
) {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return;
    }

    if let Err(e) = call_typed_i32i32_to_void(&handle.func, store, param1, param2) {
        let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
    }
}

/// Call a typed function with (i64) -> void
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallI64ToVoid(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param: jlong,
) {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return;
    }

    if let Err(e) = call_typed_i64_to_void(&handle.func, store, param) {
        let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
    }
}

/// Call a typed function with (i64, i64) -> void
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallI64I64ToVoid(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param1: jlong,
    param2: jlong,
) {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return;
    }

    if let Err(e) = call_typed_i64i64_to_void(&handle.func, store, param1, param2) {
        let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
    }
}

/// Call a typed function with (f32, f32) -> f32
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallF32F32ToF32(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param1: jfloat,
    param2: jfloat,
) -> jfloat {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return 0.0;
    }

    match call_typed_f32f32_to_f32(&handle.func, store, param1, param2) {
        Ok(result) => result,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
            0.0
        }
    }
}

/// Call a typed function with (f64, f64) -> f64
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallF64F64ToF64(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param1: jdouble,
    param2: jdouble,
) -> jdouble {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return 0.0;
    }

    match call_typed_f64f64_to_f64(&handle.func, store, param1, param2) {
        Ok(result) => result,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
            0.0
        }
    }
}

/// Call a typed function with (i32, i32, i32) -> i32
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallI32I32I32ToI32(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param1: jint,
    param2: jint,
    param3: jint,
) -> jint {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return 0;
    }

    match call_typed_i32i32i32_to_i32(&handle.func, store, param1, param2, param3) {
        Ok(result) => result,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
            0
        }
    }
}

/// Call a typed function with (i64, i64, i64) -> i64
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallI64I64I64ToI64(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param1: jlong,
    param2: jlong,
    param3: jlong,
) -> jlong {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return 0;
    }

    match call_typed_i64i64i64_to_i64(&handle.func, store, param1, param2, param3) {
        Ok(result) => result,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
            0
        }
    }
}

/// Call a typed function with (f32, f32, f32) -> f32
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallF32F32F32ToF32(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param1: jfloat,
    param2: jfloat,
    param3: jfloat,
) -> jfloat {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return 0.0;
    }

    match call_typed_f32f32f32_to_f32(&handle.func, store, param1, param2, param3) {
        Ok(result) => result,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
            0.0
        }
    }
}

/// Call a typed function with (f64, f64, f64) -> f64
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallF64F64F64ToF64(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param1: jdouble,
    param2: jdouble,
    param3: jdouble,
) -> jdouble {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return 0.0;
    }

    match call_typed_f64f64f64_to_f64(&handle.func, store, param1, param2, param3) {
        Ok(result) => result,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
            0.0
        }
    }
}

/// Call a typed function with (i32, i32) -> i64
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallI32I32ToI64(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param1: jint,
    param2: jint,
) -> jlong {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return 0;
    }

    match call_typed_i32i32_to_i64(&handle.func, store, param1, param2) {
        Ok(result) => result,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
            0
        }
    }
}

/// Call a typed function with (i64) -> i32
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallI64ToI32(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param: jlong,
) -> jint {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return 0;
    }

    match call_typed_i64_to_i32(&handle.func, store, param) {
        Ok(result) => result,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
            0
        }
    }
}

/// Call a typed function with (i32, f32) -> f32
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallI32F32ToF32(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param1: jint,
    param2: jfloat,
) -> jfloat {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return 0.0;
    }

    match call_typed_i32f32_to_f32(&handle.func, store, param1, param2) {
        Ok(result) => result,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
            0.0
        }
    }
}

/// Call a typed function with (f32, i32) -> f32
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeCallF32I32ToF32(
    mut env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
    store_ptr: jlong,
    param1: jfloat,
    param2: jint,
) -> jfloat {
    // Validate pointers before dereferencing
    let handle = validate_and_deref!(env, handle_ptr, TypedFuncHandle, "TypedFunc handle");
    let store = validate_and_deref!(mut env, store_ptr, WasmStore, "Store");

    // Validate Store is not closed/corrupted
    if let Err(e) = store.validate() {
        let _ = env.throw_new(
            "java/lang/IllegalStateException",
            format!("Store is invalid or closed: {}", e)
        );
        return 0.0;
    }

    match call_typed_f32i32_to_f32(&handle.func, store, param1, param2) {
        Ok(result) => result,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
            0.0
        }
    }
}

/// Destroy a typed function handle
#[no_mangle]
pub extern "C" fn Java_ai_tegmentum_wasmtime4j_jni_JniTypedFunc_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    handle_ptr: jlong,
) {
    if handle_ptr != 0 {
        unsafe {
            drop(Box::from_raw(handle_ptr as *mut TypedFuncHandle));
        }
    }
}

/// Handle for typed functions
struct TypedFuncHandle {
    func: Func,
    signature: String,
}

// Helper functions for common typed call patterns

fn call_typed_i32_to_i32(func: &Func, store: &WasmStore, param: i32) -> WasmtimeResult<i32> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<i32, i32>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, param)
}

fn call_typed_i32i32_to_i32(func: &Func, store: &WasmStore, p1: i32, p2: i32) -> WasmtimeResult<i32> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<(i32, i32), i32>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, (p1, p2))
}

fn call_typed_i64_to_i64(func: &Func, store: &WasmStore, param: i64) -> WasmtimeResult<i64> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<i64, i64>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, param)
}

fn call_typed_i64i64_to_i64(func: &Func, store: &WasmStore, p1: i64, p2: i64) -> WasmtimeResult<i64> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<(i64, i64), i64>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, (p1, p2))
}

fn call_typed_f32_to_f32(func: &Func, store: &WasmStore, param: f32) -> WasmtimeResult<f32> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<f32, f32>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, param)
}

fn call_typed_f64_to_f64(func: &Func, store: &WasmStore, param: f64) -> WasmtimeResult<f64> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<f64, f64>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, param)
}

fn call_typed_void_to_void(func: &Func, store: &WasmStore) -> WasmtimeResult<()> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<(), ()>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, ())
}

fn call_typed_i32_to_void(func: &Func, store: &WasmStore, param: i32) -> WasmtimeResult<()> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<i32, ()>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, param)
}

fn call_typed_i32i32_to_void(func: &Func, store: &WasmStore, p1: i32, p2: i32) -> WasmtimeResult<()> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<(i32, i32), ()>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, (p1, p2))
}

fn call_typed_i64_to_void(func: &Func, store: &WasmStore, param: i64) -> WasmtimeResult<()> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<i64, ()>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, param)
}

fn call_typed_i64i64_to_void(func: &Func, store: &WasmStore, p1: i64, p2: i64) -> WasmtimeResult<()> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<(i64, i64), ()>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, (p1, p2))
}

fn call_typed_f32f32_to_f32(func: &Func, store: &WasmStore, p1: f32, p2: f32) -> WasmtimeResult<f32> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<(f32, f32), f32>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, (p1, p2))
}

fn call_typed_f64f64_to_f64(func: &Func, store: &WasmStore, p1: f64, p2: f64) -> WasmtimeResult<f64> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<(f64, f64), f64>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, (p1, p2))
}

fn call_typed_i32i32i32_to_i32(func: &Func, store: &WasmStore, p1: i32, p2: i32, p3: i32) -> WasmtimeResult<i32> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<(i32, i32, i32), i32>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, (p1, p2, p3))
}

fn call_typed_i64i64i64_to_i64(func: &Func, store: &WasmStore, p1: i64, p2: i64, p3: i64) -> WasmtimeResult<i64> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<(i64, i64, i64), i64>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, (p1, p2, p3))
}

fn call_typed_f32f32f32_to_f32(func: &Func, store: &WasmStore, p1: f32, p2: f32, p3: f32) -> WasmtimeResult<f32> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<(f32, f32, f32), f32>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, (p1, p2, p3))
}

fn call_typed_f64f64f64_to_f64(func: &Func, store: &WasmStore, p1: f64, p2: f64, p3: f64) -> WasmtimeResult<f64> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<(f64, f64, f64), f64>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, (p1, p2, p3))
}

fn call_typed_i32i32_to_i64(func: &Func, store: &WasmStore, p1: i32, p2: i32) -> WasmtimeResult<i64> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<(i32, i32), i64>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, (p1, p2))
}

fn call_typed_i64_to_i32(func: &Func, store: &WasmStore, param: i64) -> WasmtimeResult<i32> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<i64, i32>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, param)
}

fn call_typed_i32f32_to_f32(func: &Func, store: &WasmStore, p1: i32, p2: f32) -> WasmtimeResult<f32> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<(i32, f32), f32>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, (p1, p2))
}

fn call_typed_f32i32_to_f32(func: &Func, store: &WasmStore, p1: f32, p2: i32) -> WasmtimeResult<f32> {
    let mut store_guard = store.lock_store();
    let typed = CustomTypedFunc::<(f32, i32), f32>::new(&mut *store_guard, func)?;
    typed.call(&mut *store_guard, (p1, p2))
}
