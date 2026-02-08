//! JNI bindings for Java 8-22 compatibility
//!
//! This module provides JNI-compatible functions for use by the wasmtime4j-jni module.
//! All functions follow JNI naming conventions and handle Java/native type conversions.

#![allow(unused_variables)]

#[cfg(feature = "jni-bindings")]
use jni::JNIEnv;
#[cfg(feature = "jni-bindings")]
use jni::objects::JClass;
#[cfg(feature = "jni-bindings")]
use jni::sys::jstring;

// Public modules
#[cfg(feature = "jni-bindings")]
pub mod instance;
#[cfg(feature = "jni-bindings")]
pub mod engine;
#[cfg(feature = "jni-bindings")]
pub mod function;
#[cfg(feature = "jni-bindings")]
pub mod native_method_bindings;
#[cfg(feature = "jni-bindings")]
pub mod store;
#[cfg(feature = "jni-bindings")]
pub mod linker;
#[cfg(feature = "jni-bindings")]
pub mod module;
#[cfg(feature = "jni-bindings")]
pub mod component;
#[cfg(feature = "jni-bindings")]
pub mod hostfunc;
#[cfg(feature = "jni-bindings")]
pub mod functionref;
#[cfg(feature = "jni-bindings")]
pub mod global;
#[cfg(feature = "jni-bindings")]
pub mod table;
#[cfg(feature = "jni-bindings")]
pub mod memory;
#[cfg(feature = "jni-bindings")]
pub mod runtime;
#[cfg(feature = "jni-bindings")]
pub mod wasi;
#[cfg(feature = "jni-bindings")]
pub mod caller;
#[cfg(feature = "jni-bindings")]
pub mod simd;
#[cfg(feature = "jni-bindings")]
pub mod serializer;
#[cfg(feature = "jni-bindings")]
pub mod debugger;
#[cfg(feature = "jni-bindings")]
pub mod component_linker;
#[cfg(feature = "jni-bindings")]
pub mod resource_limiter;

// Private modules
#[cfg(feature = "jni-bindings")]
mod private;

// Re-export commonly used types for other JNI modules
#[cfg(feature = "jni-bindings")]
pub use function::FunctionHandle;
#[cfg(feature = "jni-bindings")]
pub use linker::{wasm_values_to_java_array, JniHostFunctionCallback};
#[cfg(feature = "jni-bindings")]
pub use module::{VecByteArrayConverter, JStringConverter};
#[cfg(feature = "jni-bindings")]
pub use hostfunc::unmarshal_function_type;

/// Get library version for debugging
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeGetLibraryVersion(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let version = "wasmtime4j-native-DEBUG-2025-10-06-18:45-WITH-SIGNALS-DISABLED";
    match env.new_string(version) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut()
    }
}

/// JNI binding for NativeMethodBindings.nativeGetWasmtimeVersion (TOP LEVEL)
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeGetWasmtimeVersion(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    // Return Wasmtime version from constant
    match env.new_string(crate::WASMTIME_VERSION) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut()
    }
}

// Empty fallback modules for when feature is disabled
#[cfg(not(feature = "jni-bindings"))]
pub mod engine {}
#[cfg(not(feature = "jni-bindings"))]
pub mod module {}
#[cfg(not(feature = "jni-bindings"))]
pub mod instance {}
