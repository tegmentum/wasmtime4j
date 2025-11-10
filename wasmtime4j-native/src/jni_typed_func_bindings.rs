//! JNI bindings for TypedFunc
//!
//! Provides Java Native Interface bindings for zero-cost typed function calls.

use crate::error::WasmtimeResult;
use crate::typed_func::TypedFunc as CustomTypedFunc;
use jni::objects::{JClass, JObject, JValue};
use jni::sys::{jboolean, jdouble, jfloat, jint, jlong, jobject};
use jni::JNIEnv;
use std::sync::Arc;
use wasmtime::{Func, Store, WasmParams, WasmResults};

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
    let store = unsafe { &mut *(store_ptr as *mut Store<()>) };
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
    let handle = unsafe { &*(handle_ptr as *const TypedFuncHandle) };
    let store = unsafe { &mut *(store_ptr as *mut Store<()>) };

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
    let handle = unsafe { &*(handle_ptr as *const TypedFuncHandle) };
    let store = unsafe { &mut *(store_ptr as *mut Store<()>) };

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
    let handle = unsafe { &*(handle_ptr as *const TypedFuncHandle) };
    let store = unsafe { &mut *(store_ptr as *mut Store<()>) };

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
    let handle = unsafe { &*(handle_ptr as *const TypedFuncHandle) };
    let store = unsafe { &mut *(store_ptr as *mut Store<()>) };

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
    let handle = unsafe { &*(handle_ptr as *const TypedFuncHandle) };
    let store = unsafe { &mut *(store_ptr as *mut Store<()>) };

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
    let handle = unsafe { &*(handle_ptr as *const TypedFuncHandle) };
    let store = unsafe { &mut *(store_ptr as *mut Store<()>) };

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
    let handle = unsafe { &*(handle_ptr as *const TypedFuncHandle) };
    let store = unsafe { &mut *(store_ptr as *mut Store<()>) };

    if let Err(e) = call_typed_void_to_void(&handle.func, store) {
        let _ = env.throw_new("ai/tegmentum/wasmtime4j/WasmRuntimeException", format!("{}", e));
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

fn call_typed_i32_to_i32(func: &Func, mut store: &mut Store<()>, param: i32) -> WasmtimeResult<i32> {
    let typed = CustomTypedFunc::<i32, i32>::new(&mut store, func)?;
    typed.call(&mut store, param)
}

fn call_typed_i32i32_to_i32(func: &Func, mut store: &mut Store<()>, p1: i32, p2: i32) -> WasmtimeResult<i32> {
    let typed = CustomTypedFunc::<(i32, i32), i32>::new(&mut store, func)?;
    typed.call(&mut store, (p1, p2))
}

fn call_typed_i64_to_i64(func: &Func, mut store: &mut Store<()>, param: i64) -> WasmtimeResult<i64> {
    let typed = CustomTypedFunc::<i64, i64>::new(&mut store, func)?;
    typed.call(&mut store, param)
}

fn call_typed_i64i64_to_i64(func: &Func, mut store: &mut Store<()>, p1: i64, p2: i64) -> WasmtimeResult<i64> {
    let typed = CustomTypedFunc::<(i64, i64), i64>::new(&mut store, func)?;
    typed.call(&mut store, (p1, p2))
}

fn call_typed_f32_to_f32(func: &Func, mut store: &mut Store<()>, param: f32) -> WasmtimeResult<f32> {
    let typed = CustomTypedFunc::<f32, f32>::new(&mut store, func)?;
    typed.call(&mut store, param)
}

fn call_typed_f64_to_f64(func: &Func, mut store: &mut Store<()>, param: f64) -> WasmtimeResult<f64> {
    let typed = CustomTypedFunc::<f64, f64>::new(&mut store, func)?;
    typed.call(&mut store, param)
}

fn call_typed_void_to_void(func: &Func, mut store: &mut Store<()>) -> WasmtimeResult<()> {
    let typed = CustomTypedFunc::<(), ()>::new(&mut store, func)?;
    typed.call(&mut store, ())
}
