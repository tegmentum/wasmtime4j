//! JNI bindings for SIMD operations

use jni::JNIEnv;
use jni::objects::{JByteArray, JClass, JObject};
use jni::sys::{jboolean, jbyte, jbyteArray, jfloat, jint, jlong, jstring};

use crate::error::{jni_utils, WasmtimeError};
use crate::simd;

/// Check if SIMD is supported
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeIsSimdSupported(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
) -> jboolean {
    // For now, always return true as SIMD is supported in Wasmtime
    1
}

/// Check if Component Model is supported
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSupportsComponentModel(
    _env: JNIEnv,
    _class: JClass,
    _runtime_handle: jlong,
) -> jboolean {
    // Component Model is enabled in Wasmtime build via "component-model" feature
    #[cfg(feature = "component-model")]
    {
        1
    }
    #[cfg(not(feature = "component-model"))]
    {
        0
    }
}

/// Get SIMD capabilities
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeGetSimdCapabilities(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
) -> jstring {
    match env.new_string("v128 SIMD with platform optimizations (SSE, AVX, NEON)") {
        Ok(version_str) => version_str.into_raw(),
        Err(e) => {
            log::error!("Failed to create SIMD capabilities string: {}", e);
            std::ptr::null_mut()
        }
    }
}

/// SIMD vector addition
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdAdd(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
) -> jbyteArray {
    // Convert byte arrays outside the closure
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(), // Error getting array length
    };
    let b_len = match env.get_array_length(&b) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(), // Error getting array length
    };

    let mut a_bytes = vec![0i8; a_len as usize];
    let mut b_bytes = vec![0i8; b_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut(); // Error reading array
    }
    if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
        return std::ptr::null_mut(); // Error reading array
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
    let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vectors must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };
        let b_v128 = simd::V128 { data: b_bytes_u8.try_into().expect("length validated above") };

        // Create SIMD operations instance with default config
        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.add(&a_v128, &b_v128)?;
        Ok(result.data.to_vec())
    });

    // Create result array outside closure
    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut() // Error setting array region
            }
        },
        Err(_) => std::ptr::null_mut(), // Error creating array
    }
}

/// SIMD vector subtraction
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdSubtract(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
) -> jbyteArray {
    // Convert byte arrays outside the closure
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };
    let b_len = match env.get_array_length(&b) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];
    let mut b_bytes = vec![0i8; b_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }
    if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
    let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vectors must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };
        let b_v128 = simd::V128 { data: b_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.subtract(&a_v128, &b_v128)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD vector multiplication
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdMultiply(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
) -> jbyteArray {
    jni_utils::jni_try_object(&mut env, |env| {
        let a_bytes = env.convert_byte_array(a)?;
        let b_bytes = env.convert_byte_array(b)?;

        if a_bytes.len() != 16 || b_bytes.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vectors must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes.try_into().expect("length validated above") };
        let b_v128 = simd::V128 { data: b_bytes.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.multiply(&a_v128, &b_v128)?;
        let data = result.data.to_vec();

        let byte_array = env.new_byte_array(data.len() as i32)
            .map_err(|e| WasmtimeError::Memory { message: format!("Failed to create byte array: {}", e) })?;
        env.set_byte_array_region(&byte_array, 0, &data.iter().map(|&b| b as i8).collect::<Vec<i8>>())
            .map_err(|e| WasmtimeError::Memory { message: format!("Failed to set byte array region: {}", e) })?;

        Ok(unsafe { JObject::from_raw(byte_array.into_raw()) })
    })
}

/// SIMD FMA operation (a * b + c)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdFma(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
    c: JByteArray,
) -> jbyteArray {
    jni_utils::jni_try_object(&mut env, |env| {
        let a_bytes = env.convert_byte_array(a)?;
        let b_bytes = env.convert_byte_array(b)?;
        let c_bytes = env.convert_byte_array(c)?;

        if a_bytes.len() != 16 || b_bytes.len() != 16 || c_bytes.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vectors must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes.try_into().expect("length validated above") };
        let b_v128 = simd::V128 { data: b_bytes.try_into().expect("length validated above") };
        let c_v128 = simd::V128 { data: c_bytes.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.fma(&a_v128, &b_v128, &c_v128)?;
        let data = result.data.to_vec();

        let byte_array = env.new_byte_array(data.len() as i32)
            .map_err(|e| WasmtimeError::Memory { message: format!("Failed to create byte array: {}", e) })?;
        env.set_byte_array_region(&byte_array, 0, &data.iter().map(|&b| b as i8).collect::<Vec<i8>>())
            .map_err(|e| WasmtimeError::Memory { message: format!("Failed to set byte array region: {}", e) })?;

        Ok(unsafe { JObject::from_raw(byte_array.into_raw()) })
    })
}

/// SIMD horizontal sum
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdHorizontalSum(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
) -> jfloat {
    match env.convert_byte_array(a) {
        Ok(a_bytes) => {
            if a_bytes.len() == 16 {
                let a_v128 = simd::V128 { data: a_bytes.try_into().expect("length validated above") };

                let simd_config = simd::SIMDConfig::default();
                match simd::SIMDOperations::new(simd_config) {
                    Ok(simd_ops) => {
                        match simd_ops.reduce_sum_i32(&a_v128) {
                            Ok(result) => result as f32,
                            Err(_) => 0.0,
                        }
                    }
                    Err(_) => 0.0,
                }
            } else {
                0.0
            }
        }
        Err(_) => 0.0,
    }
}

/// SIMD vector division
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdDivide(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };
    let b_len = match env.get_array_length(&b) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];
    let mut b_bytes = vec![0i8; b_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }
    if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
    let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vectors must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };
        let b_v128 = simd::V128 { data: b_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.divide(&a_v128, &b_v128)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD saturated addition
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdAddSaturated(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };
    let b_len = match env.get_array_length(&b) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];
    let mut b_bytes = vec![0i8; b_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }
    if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
    let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vectors must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };
        let b_v128 = simd::V128 { data: b_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.add_saturated(&a_v128, &b_v128)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD bitwise AND
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdAnd(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };
    let b_len = match env.get_array_length(&b) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];
    let mut b_bytes = vec![0i8; b_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }
    if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
    let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vectors must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };
        let b_v128 = simd::V128 { data: b_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.and(&a_v128, &b_v128)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD bitwise OR
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdOr(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };
    let b_len = match env.get_array_length(&b) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];
    let mut b_bytes = vec![0i8; b_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }
    if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
    let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vectors must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };
        let b_v128 = simd::V128 { data: b_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.or(&a_v128, &b_v128)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD bitwise XOR
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdXor(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };
    let b_len = match env.get_array_length(&b) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];
    let mut b_bytes = vec![0i8; b_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }
    if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
    let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vectors must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };
        let b_v128 = simd::V128 { data: b_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.xor(&a_v128, &b_v128)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD bitwise NOT
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdNot(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vector must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.not(&a_v128)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD equals comparison
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdEquals(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };
    let b_len = match env.get_array_length(&b) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];
    let mut b_bytes = vec![0i8; b_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }
    if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
    let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vectors must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };
        let b_v128 = simd::V128 { data: b_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.equals(&a_v128, &b_v128)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD less than comparison
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdLessThan(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };
    let b_len = match env.get_array_length(&b) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];
    let mut b_bytes = vec![0i8; b_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }
    if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
    let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vectors must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };
        let b_v128 = simd::V128 { data: b_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.less_than(&a_v128, &b_v128)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD greater than comparison
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdGreaterThan(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };
    let b_len = match env.get_array_length(&b) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];
    let mut b_bytes = vec![0i8; b_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }
    if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
    let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vectors must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };
        let b_v128 = simd::V128 { data: b_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.greater_than(&a_v128, &b_v128)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD fused multiply-subtract
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdFms(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
    c: JByteArray,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };
    let b_len = match env.get_array_length(&b) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };
    let c_len = match env.get_array_length(&c) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];
    let mut b_bytes = vec![0i8; b_len as usize];
    let mut c_bytes = vec![0i8; c_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }
    if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
        return std::ptr::null_mut();
    }
    if env.get_byte_array_region(c, 0, &mut c_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
    let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();
    let c_bytes_u8: Vec<u8> = c_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 || c_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vectors must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };
        let b_v128 = simd::V128 { data: b_bytes_u8.try_into().expect("length validated above") };
        let c_v128 = simd::V128 { data: c_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.fms(&a_v128, &b_v128, &c_v128)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD reciprocal
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdReciprocal(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vector must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.reciprocal(&a_v128)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD square root
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdSqrt(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vector must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.sqrt(&a_v128)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD reciprocal square root
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdRsqrt(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vector must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.rsqrt(&a_v128)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD vector shuffle
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdShuffle(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
    indices: JByteArray,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };
    let b_len = match env.get_array_length(&b) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };
    let indices_len = match env.get_array_length(&indices) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];
    let mut b_bytes = vec![0i8; b_len as usize];
    let mut indices_bytes = vec![0i8; indices_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }
    if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
        return std::ptr::null_mut();
    }
    if env.get_byte_array_region(indices, 0, &mut indices_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
    let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();
    let indices_u8: Vec<u8> = indices_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 || indices_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vectors and indices must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };
        let b_v128 = simd::V128 { data: b_bytes_u8.try_into().expect("length validated above") };
        let indices_arr: [u8; 16] = indices_u8.try_into().expect("length validated above");

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.shuffle(&a_v128, &b_v128, &indices_arr)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD relaxed add
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdRelaxedAdd(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };
    let b_len = match env.get_array_length(&b) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];
    let mut b_bytes = vec![0i8; b_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }
    if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
    let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vectors must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };
        let b_v128 = simd::V128 { data: b_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.relaxed_add(&a_v128, &b_v128)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD convert I32 to F32
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdConvertI32ToF32(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vector must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.convert_i32_to_f32(&a_v128)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD convert F32 to I32
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdConvertF32ToI32(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vector must be 16 bytes".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.convert_f32_to_i32(&a_v128)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD extract I32 lane
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdExtractLaneI32(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
    lane: jint,
) -> jint {
    match env.convert_byte_array(a) {
        Ok(a_bytes) => {
            if a_bytes.len() == 16 && lane >= 0 && lane < 4 {
                let a_v128 = simd::V128 { data: a_bytes.try_into().expect("length validated above") };

                let simd_config = simd::SIMDConfig::default();
                match simd::SIMDOperations::new(simd_config) {
                    Ok(simd_ops) => {
                        match simd_ops.extract_lane_i32(&a_v128, lane as u8) {
                            Ok(result) => result,
                            Err(_) => 0,
                        }
                    }
                    Err(_) => 0,
                }
            } else {
                0
            }
        }
        Err(_) => 0,
    }
}

/// SIMD replace I32 lane
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdReplaceLaneI32(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
    lane: jint,
    value: jint,
) -> jbyteArray {
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut a_bytes = vec![0i8; a_len as usize];

    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }

    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();

    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        if a_bytes_u8.len() != 16 || lane < 0 || lane >= 4 {
            return Err(crate::error::WasmtimeError::InvalidOperation {
                message: "SIMD vector must be 16 bytes and lane must be 0-3".to_string(),
            });
        }

        let a_v128 = simd::V128 { data: a_bytes_u8.try_into().expect("length validated above") };

        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.replace_lane_i32(&a_v128, lane as u8, value)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD splat I32
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdSplatI32(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    value: jint,
) -> jbyteArray {
    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.splat_i32(value)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD splat F32
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdSplatF32(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    value: jfloat,
) -> jbyteArray {
    let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
        let simd_config = simd::SIMDConfig::default();
        let simd_ops = simd::SIMDOperations::new(simd_config)?;

        let result = simd_ops.splat_f32(value)?;
        Ok(result.data.to_vec())
    });

    match env.new_byte_array(16) {
        Ok(byte_array) => {
            let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
            if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                byte_array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD horizontal min
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdHorizontalMin(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
) -> jfloat {
    match env.convert_byte_array(a) {
        Ok(a_bytes) => {
            if a_bytes.len() == 16 {
                let a_v128 = simd::V128 { data: a_bytes.try_into().expect("length validated above") };

                let simd_config = simd::SIMDConfig::default();
                match simd::SIMDOperations::new(simd_config) {
                    Ok(simd_ops) => {
                        match simd_ops.reduce_min_i32(&a_v128) {
                            Ok(result) => result as f32,
                            Err(_) => 0.0,
                        }
                    }
                    Err(_) => 0.0,
                }
            } else {
                0.0
            }
        }
        Err(_) => 0.0,
    }
}

/// SIMD horizontal max
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdHorizontalMax(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
) -> jfloat {
    match env.convert_byte_array(a) {
        Ok(a_bytes) => {
            if a_bytes.len() == 16 {
                let a_v128 = simd::V128 { data: a_bytes.try_into().expect("length validated above") };

                let simd_config = simd::SIMDConfig::default();
                match simd::SIMDOperations::new(simd_config) {
                    Ok(simd_ops) => {
                        match simd_ops.reduce_max_i32(&a_v128) {
                            Ok(result) => result as f32,
                            Err(_) => 0.0,
                        }
                    }
                    Err(_) => 0.0,
                }
            } else {
                0.0
            }
        }
        Err(_) => 0.0,
    }
}

/// SIMD load from memory
///
/// NOTE: Requires Java API change to add store_handle parameter.
/// Memory read operations need store context access.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdLoad(
    mut env: JNIEnv,
    _class: JClass,
    _runtime_handle: jlong,
    _memory_handle: jlong,
    _offset: jint,
) -> jbyteArray {
    jni_utils::throw_jni_exception(&mut env, &WasmtimeError::UnsupportedFeature {
        message: "SIMD load requires store context; Java API update needed".to_string(),
    });
    std::ptr::null_mut()
}

/// SIMD load aligned from memory
///
/// NOTE: Requires Java API change to add store_handle parameter.
/// Memory read operations need store context access.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdLoadAligned(
    mut env: JNIEnv,
    _class: JClass,
    _runtime_handle: jlong,
    _memory_handle: jlong,
    _offset: jint,
    _alignment: jint,
) -> jbyteArray {
    jni_utils::throw_jni_exception(&mut env, &WasmtimeError::UnsupportedFeature {
        message: "SIMD aligned load requires store context; Java API update needed".to_string(),
    });
    std::ptr::null_mut()
}

/// SIMD store to memory
///
/// NOTE: Requires Java API change to add store_handle parameter.
/// Memory write operations need store context access.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdStore(
    mut env: JNIEnv,
    _class: JClass,
    _runtime_handle: jlong,
    _memory_handle: jlong,
    _offset: jint,
    _vector: JByteArray,
) -> jboolean {
    jni_utils::throw_jni_exception(&mut env, &WasmtimeError::UnsupportedFeature {
        message: "SIMD store requires store context; Java API update needed".to_string(),
    });
    0
}

/// SIMD store aligned to memory
///
/// NOTE: Requires Java API change to add store_handle parameter.
/// Memory write operations need store context access.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdStoreAligned(
    mut env: JNIEnv,
    _class: JClass,
    _runtime_handle: jlong,
    _memory_handle: jlong,
    _offset: jint,
    _vector: JByteArray,
    _alignment: jint,
) -> jboolean {
    jni_utils::throw_jni_exception(&mut env, &WasmtimeError::UnsupportedFeature {
        message: "SIMD aligned store requires store context; Java API update needed".to_string(),
    });
    0
}

/// SIMD popcount - count set bits in each byte of the vector
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdPopcount(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    a: JByteArray,
) -> jbyteArray {
    // Get array length
    let a_len = match env.get_array_length(&a) {
        Ok(len) => len,
        Err(_) => return std::ptr::null_mut(),
    };

    // Read input bytes
    let mut a_bytes = vec![0i8; a_len as usize];
    if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
        return std::ptr::null_mut();
    }

    // Convert to u8
    let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();

    // Ensure we have exactly 16 bytes (128-bit SIMD vector)
    if a_bytes_u8.len() != 16 {
        return std::ptr::null_mut();
    }

    // Compute popcount for each byte
    let mut result = vec![0i8; 16];
    for i in 0..16 {
        result[i] = a_bytes_u8[i].count_ones() as i8;
    }

    // Create result array
    match env.new_byte_array(16) {
        Ok(result_array) => {
            if env.set_byte_array_region(&result_array, 0, &result).is_err() {
                return std::ptr::null_mut();
            }
            result_array.into_raw()
        }
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD variable shift left - shifts each lane of 'a' left by the corresponding lane value in 'b'
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdShlVariable(
    mut env: JNIEnv,
    _class: JClass,
    _runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
) -> jbyteArray {
    // Get input bytes
    let a_bytes = match jni_utils::get_byte_array_bytes(&env, &a) {
        Ok(bytes) => bytes,
        Err(_) => return std::ptr::null_mut(),
    };
    let b_bytes = match jni_utils::get_byte_array_bytes(&env, &b) {
        Ok(bytes) => bytes,
        Err(_) => return std::ptr::null_mut(),
    };

    // Ensure both arrays are 16 bytes (128-bit SIMD vector)
    if a_bytes.len() != 16 || b_bytes.len() != 16 {
        return std::ptr::null_mut();
    }

    // Perform variable shift left - each byte shifted by corresponding shift amount
    let mut result = [0u8; 16];
    for i in 0..16 {
        let shift_amount = (b_bytes[i] & 0x07) as u32; // Mask to 3 bits (0-7) for byte shift
        result[i] = a_bytes[i].wrapping_shl(shift_amount);
    }

    // Create result array
    match env.new_byte_array(16) {
        Ok(result_array) => {
            let result_signed: Vec<i8> = result.iter().map(|&x| x as i8).collect();
            if env.set_byte_array_region(&result_array, 0, &result_signed).is_err() {
                return std::ptr::null_mut();
            }
            result_array.into_raw()
        }
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD variable shift right - shifts each lane of 'a' right by the corresponding lane value in 'b'
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdShrVariable(
    mut env: JNIEnv,
    _class: JClass,
    _runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
) -> jbyteArray {
    // Get input bytes
    let a_bytes = match jni_utils::get_byte_array_bytes(&env, &a) {
        Ok(bytes) => bytes,
        Err(_) => return std::ptr::null_mut(),
    };
    let b_bytes = match jni_utils::get_byte_array_bytes(&env, &b) {
        Ok(bytes) => bytes,
        Err(_) => return std::ptr::null_mut(),
    };

    // Ensure both arrays are 16 bytes (128-bit SIMD vector)
    if a_bytes.len() != 16 || b_bytes.len() != 16 {
        return std::ptr::null_mut();
    }

    // Perform variable shift right - each byte shifted by corresponding shift amount
    let mut result = [0u8; 16];
    for i in 0..16 {
        let shift_amount = (b_bytes[i] & 0x07) as u32; // Mask to 3 bits (0-7) for byte shift
        result[i] = a_bytes[i].wrapping_shr(shift_amount);
    }

    // Create result array
    match env.new_byte_array(16) {
        Ok(result_array) => {
            let result_signed: Vec<i8> = result.iter().map(|&x| x as i8).collect();
            if env.set_byte_array_region(&result_array, 0, &result_signed).is_err() {
                return std::ptr::null_mut();
            }
            result_array.into_raw()
        }
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD select - for each bit, selects from 'a' if mask bit is 1, from 'b' if mask bit is 0
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdSelect(
    mut env: JNIEnv,
    _class: JClass,
    _runtime_handle: jlong,
    mask: JByteArray,
    a: JByteArray,
    b: JByteArray,
) -> jbyteArray {
    // Get input bytes
    let mask_bytes = match jni_utils::get_byte_array_bytes(&env, &mask) {
        Ok(bytes) => bytes,
        Err(_) => return std::ptr::null_mut(),
    };
    let a_bytes = match jni_utils::get_byte_array_bytes(&env, &a) {
        Ok(bytes) => bytes,
        Err(_) => return std::ptr::null_mut(),
    };
    let b_bytes = match jni_utils::get_byte_array_bytes(&env, &b) {
        Ok(bytes) => bytes,
        Err(_) => return std::ptr::null_mut(),
    };

    // Ensure all arrays are 16 bytes (128-bit SIMD vector)
    if mask_bytes.len() != 16 || a_bytes.len() != 16 || b_bytes.len() != 16 {
        return std::ptr::null_mut();
    }

    // Perform bitwise select: result = (a & mask) | (b & ~mask)
    let mut result = [0u8; 16];
    for i in 0..16 {
        result[i] = (a_bytes[i] & mask_bytes[i]) | (b_bytes[i] & !mask_bytes[i]);
    }

    // Create result array
    match env.new_byte_array(16) {
        Ok(result_array) => {
            let result_signed: Vec<i8> = result.iter().map(|&x| x as i8).collect();
            if env.set_byte_array_region(&result_array, 0, &result_signed).is_err() {
                return std::ptr::null_mut();
            }
            result_array.into_raw()
        }
        Err(_) => std::ptr::null_mut(),
    }
}

/// SIMD blend - selects bytes from 'a' or 'b' based on mask bits (per-byte selection)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdBlend(
    mut env: JNIEnv,
    _class: JClass,
    _runtime_handle: jlong,
    a: JByteArray,
    b: JByteArray,
    mask: jint,
) -> jbyteArray {
    // Get input bytes
    let a_bytes = match jni_utils::get_byte_array_bytes(&env, &a) {
        Ok(bytes) => bytes,
        Err(_) => return std::ptr::null_mut(),
    };
    let b_bytes = match jni_utils::get_byte_array_bytes(&env, &b) {
        Ok(bytes) => bytes,
        Err(_) => return std::ptr::null_mut(),
    };

    // Ensure both arrays are 16 bytes (128-bit SIMD vector)
    if a_bytes.len() != 16 || b_bytes.len() != 16 {
        return std::ptr::null_mut();
    }

    // Perform blend: for each byte position, select from 'a' if mask bit is 0, from 'b' if 1
    // The mask is a 16-bit value (lower 16 bits of jint), one bit per byte
    let mask_u16 = mask as u16;
    let mut result = [0u8; 16];
    for i in 0..16 {
        if (mask_u16 >> i) & 1 == 1 {
            result[i] = b_bytes[i];
        } else {
            result[i] = a_bytes[i];
        }
    }

    // Create result array
    match env.new_byte_array(16) {
        Ok(result_array) => {
            let result_signed: Vec<i8> = result.iter().map(|&x| x as i8).collect();
            if env.set_byte_array_region(&result_array, 0, &result_signed).is_err() {
                return std::ptr::null_mut();
            }
            result_array.into_raw()
        }
        Err(_) => std::ptr::null_mut(),
    }
}

/// Initialize table from element segment (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeTableInit(
    mut env: JNIEnv,
    _class: JClass,
    table_ptr: jlong,
    store_ptr: jlong,
    instance_ptr: jlong,
    dst: jint,
    src: jint,
    len: jint,
    segment_index: jint,
) -> jint {
    jni_utils::jni_try_code(&mut env, || {
        use std::os::raw::c_void;

        // Validate parameters
        if table_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Table handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if instance_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Instance handle cannot be null".to_string(),
            });
        }
        if dst < 0 || src < 0 || len < 0 || segment_index < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Invalid parameters: dst={}, src={}, len={}, segment_index={}",
                    dst, src, len, segment_index
                ),
            });
        }

        // Get objects from handles
        let table = unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };
        let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr as *const c_void)? };

        // Call table.init_from_segment
        table.init_from_segment(
            store,
            instance,
            dst as u32,
            src as u32,
            len as u32,
            segment_index as u32,
        )?;

        Ok(())
    })
}

/// Drop an element segment (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeElemDrop(
    mut env: JNIEnv,
    _class: JClass,
    instance_ptr: jlong,
    segment_index: jint,
) -> jint {
    jni_utils::jni_try_code(&mut env, || {
        use std::os::raw::c_void;

        // Validate parameters
        if instance_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Instance handle cannot be null".to_string(),
            });
        }
        if segment_index < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid segment index: {}", segment_index),
            });
        }

        // Get instance from handle
        let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr as *const c_void)? };

        // Drop the element segment
        let segment_manager = instance.get_element_segment_manager();
        segment_manager.drop_segment(segment_index as u32)?;

        Ok(())
    })
}

/// Initialize memory from data segment (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeMemoryInit(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    instance_ptr: jlong,
    dest_offset: jint,
    data_segment_index: jint,
    src_offset: jint,
    len: jint,
) -> jint {
    jni_utils::jni_try_code(&mut env, || {
        use std::os::raw::c_void;

        // Validate parameters
        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if instance_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Instance handle cannot be null".to_string(),
            });
        }
        if dest_offset < 0 || data_segment_index < 0 || src_offset < 0 || len < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Invalid parameters: dest_offset={}, data_segment_index={}, src_offset={}, len={}",
                    dest_offset, data_segment_index, src_offset, len
                ),
            });
        }

        // Get objects from handles
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };
        let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr as *const c_void)? };

        // Call memory_init
        crate::memory::core::memory_init(
            memory,
            store,
            instance,
            dest_offset as u32,
            data_segment_index as u32,
            src_offset as u32,
            len as u32,
        )?;

        Ok(())
    })
}

/// Drop a data segment (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeDataDrop(
    mut env: JNIEnv,
    _class: JClass,
    instance_ptr: jlong,
    data_segment_index: jint,
) -> jint {
    jni_utils::jni_try_code(&mut env, || {
        use std::os::raw::c_void;

        // Validate parameters
        if instance_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Instance handle cannot be null".to_string(),
            });
        }
        if data_segment_index < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid data segment index: {}", data_segment_index),
            });
        }

        // Get instance from handle
        let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr as *const c_void)? };

        // Drop the data segment
        crate::memory::core::data_drop(instance, data_segment_index as u32)?;

        Ok(())
    })
}

/// Copy memory within the same memory instance (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeMemoryCopy(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    dest_offset: jint,
    src_offset: jint,
    len: jint,
) -> jint {
    jni_utils::jni_try_code(&mut env, || {
        use std::os::raw::c_void;

        // Validate parameters
        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if dest_offset < 0 || src_offset < 0 || len < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Invalid parameters: dest_offset={}, src_offset={}, len={}",
                    dest_offset, src_offset, len
                ),
            });
        }

        // Get objects from handles
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

        // Call memory_copy
        crate::memory::core::memory_copy(
            memory,
            store,
            dest_offset as usize,
            src_offset as usize,
            len as usize,
        )?;

        Ok(())
    })
}

/// Fill memory with a byte value (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeMemoryFill(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jbyte,
    len: jint,
) -> jint {
    jni_utils::jni_try_code(&mut env, || {
        use std::os::raw::c_void;

        // Validate parameters
        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if offset < 0 || len < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid parameters: offset={}, len={}", offset, len),
            });
        }

        // Get objects from handles
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

        // Call memory_fill
        crate::memory::core::memory_fill(
            memory,
            store,
            offset as usize,
            value as u8,
            len as usize,
        )?;

        Ok(())
    })
}

// ==================== Atomic Memory Operations ====================

/// Check if memory is shared (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeIsShared(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
) -> jboolean {
    jni_utils::jni_try_default(&mut env, 0, || {
        use std::os::raw::c_void;

        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }

        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr as *mut c_void)? };

        let is_shared = crate::memory::core::memory_is_shared(memory, store)?;
        Ok(if is_shared { 1 } else { 0 })
    })
}

/// Atomic compare-and-swap on 32-bit value (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicCompareAndSwapInt(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    expected: jint,
    new_value: jint,
) -> jint {
    jni_utils::jni_try_default(&mut env, 0, || {
        use std::os::raw::c_void;

        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if offset < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid offset: {}", offset),
            });
        }

        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

        let result = crate::memory::core::atomic_compare_and_swap_i32(
            memory,
            store,
            offset as usize,
            expected,
            new_value,
        )?;

        Ok(result)
    })
}

/// Atomic compare-and-swap on 64-bit value (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicCompareAndSwapLong(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    expected: jlong,
    new_value: jlong,
) -> jlong {
    jni_utils::jni_try_default(&mut env, 0, || {
        use std::os::raw::c_void;

        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if offset < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid offset: {}", offset),
            });
        }

        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

        let result = crate::memory::core::atomic_compare_and_swap_i64(
            memory,
            store,
            offset as usize,
            expected,
            new_value,
        )?;

        Ok(result)
    })
}

/// Atomic load of 32-bit value (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicLoadInt(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
) -> jint {
    jni_utils::jni_try_default(&mut env, 0, || {
        use std::os::raw::c_void;

        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if offset < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid offset: {}", offset),
            });
        }

        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr as *mut c_void)? };

        let result = crate::memory::core::atomic_load_i32(memory, store, offset as usize)?;

        Ok(result)
    })
}

/// Atomic load of 64-bit value (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicLoadLong(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
) -> jlong {
    jni_utils::jni_try_default(&mut env, 0, || {
        use std::os::raw::c_void;

        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if offset < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid offset: {}", offset),
            });
        }

        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr as *mut c_void)? };

        let result = crate::memory::core::atomic_load_i64(memory, store, offset as usize)?;

        Ok(result)
    })
}

/// Atomic store of 32-bit value (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicStoreInt(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jint,
) {
    jni_utils::jni_try_code(&mut env, || {
        use std::os::raw::c_void;

        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if offset < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid offset: {}", offset),
            });
        }

        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

        crate::memory::core::atomic_store_i32(memory, store, offset as usize, value)?;

        Ok(())
    });
}

/// Atomic store of 64-bit value (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicStoreLong(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jlong,
) {
    jni_utils::jni_try_code(&mut env, || {
        use std::os::raw::c_void;

        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if offset < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid offset: {}", offset),
            });
        }

        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

        crate::memory::core::atomic_store_i64(memory, store, offset as usize, value)?;

        Ok(())
    });
}

/// Atomic add on 32-bit value (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicAddInt(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jint,
) -> jint {
    jni_utils::jni_try_default(&mut env, 0, || {
        use std::os::raw::c_void;

        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if offset < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid offset: {}", offset),
            });
        }

        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

        let result = crate::memory::core::atomic_add_i32(memory, store, offset as usize, value)?;

        Ok(result)
    })
}

/// Atomic add on 64-bit value (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicAddLong(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jlong,
) -> jlong {
    jni_utils::jni_try_default(&mut env, 0, || {
        use std::os::raw::c_void;

        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if offset < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid offset: {}", offset),
            });
        }

        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

        let result = crate::memory::core::atomic_add_i64(memory, store, offset as usize, value)?;

        Ok(result)
    })
}

/// Atomic bitwise AND on 32-bit value (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicAndInt(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jint,
) -> jint {
    jni_utils::jni_try_default(&mut env, 0, || {
        use std::os::raw::c_void;

        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if offset < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid offset: {}", offset),
            });
        }

        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

        let result = crate::memory::core::atomic_and_i32(memory, store, offset as usize, value)?;

        Ok(result)
    })
}

/// Atomic bitwise OR on 32-bit value (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicOrInt(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jint,
) -> jint {
    jni_utils::jni_try_default(&mut env, 0, || {
        use std::os::raw::c_void;

        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if offset < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid offset: {}", offset),
            });
        }

        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

        let result = crate::memory::core::atomic_or_i32(memory, store, offset as usize, value)?;

        Ok(result)
    })
}

/// Atomic bitwise XOR on 32-bit value (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicXorInt(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jint,
) -> jint {
    jni_utils::jni_try_default(&mut env, 0, || {
        use std::os::raw::c_void;

        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if offset < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid offset: {}", offset),
            });
        }

        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

        let result = crate::memory::core::atomic_xor_i32(memory, store, offset as usize, value)?;

        Ok(result)
    })
}

/// Atomic memory fence (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicFence(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
) {
    jni_utils::jni_try_code(&mut env, || {
        use std::os::raw::c_void;

        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }

        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr as *mut c_void)? };

        crate::memory::core::atomic_fence(memory, store)?;

        Ok(())
    });
}

/// Atomic notify (wake threads waiting on a memory location) (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicNotify(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    count: jint,
) -> jint {
    jni_utils::jni_try_default(&mut env, 0, || {
        use std::os::raw::c_void;

        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if offset < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid offset: {}", offset),
            });
        }
        if count < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid count: {}", count),
            });
        }

        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr as *mut c_void)? };

        let result = crate::memory::core::atomic_notify(memory, store, offset as usize, count)?;

        Ok(result)
    })
}

/// Atomic wait on 32-bit value (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicWait32(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    expected: jint,
    timeout_nanos: jlong,
) -> jint {
    jni_utils::jni_try_default(&mut env, 0, || {
        use std::os::raw::c_void;

        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if offset < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid offset: {}", offset),
            });
        }
        if timeout_nanos < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid timeout: {}", timeout_nanos),
            });
        }

        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr as *mut c_void)? };

        let result = crate::memory::core::atomic_wait32(
            memory,
            store,
            offset as usize,
            expected,
            timeout_nanos,
        )?;

        Ok(result)
    })
}

/// Atomic wait on 64-bit value (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicWait64(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    expected: jlong,
    timeout_nanos: jlong,
) -> jint {
    jni_utils::jni_try_default(&mut env, 0, || {
        use std::os::raw::c_void;

        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if offset < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid offset: {}", offset),
            });
        }
        if timeout_nanos < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid timeout: {}", timeout_nanos),
            });
        }

        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr as *mut c_void)? };

        let result = crate::memory::core::atomic_wait64(
            memory,
            store,
            offset as usize,
            expected,
            timeout_nanos,
        )?;

        Ok(result)
    })
}

/// Check if memory supports 64-bit addressing (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeSupports64BitAddressing(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
) -> jboolean {
    jni_utils::jni_try_default(&env, 0, || {
        if memory_ptr == 0 {
            log::error!("JNI Memory.nativeSupports64BitAddressing: null memory handle");
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }

        // Get memory reference
        let memory = unsafe {
            crate::memory::core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)?
        };

        // Check if it's a 64-bit memory from the cached memory type
        let is_64 = memory.memory_type.is_64();

        log::debug!("Memory 0x{:x} is {}",  memory_ptr, if is_64 { "64-bit" } else { "32-bit" });

        Ok(if is_64 { 1 } else { 0 })
    }) as jboolean
}

/// Copy elements within a table (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeTableCopy(
    mut env: JNIEnv,
    _class: JClass,
    table_ptr: jlong,
    store_ptr: jlong,
    dst: jint,
    src: jint,
    len: jint,
) -> jint {
    jni_utils::jni_try_code(&mut env, || {
        use std::os::raw::c_void;

        // Validate parameters
        if table_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Table handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if dst < 0 || src < 0 || len < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid parameters: dst={}, src={}, len={}", dst, src, len),
            });
        }

        // Get objects from handles
        let table = unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr as *const c_void)? };

        // Call table.copy_within
        table.copy_within(store, dst as u32, src as u32, len as u32)?;

        Ok(())
    })
}

/// Copy elements from another table (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeTableCopyFrom(
    mut env: JNIEnv,
    _class: JClass,
    dst_table_ptr: jlong,
    store_ptr: jlong,
    dst: jint,
    src_table_ptr: jlong,
    src: jint,
    len: jint,
    ) -> jint {
    jni_utils::jni_try_code(&mut env, || {
        use std::os::raw::c_void;

        // Validate parameters
        if dst_table_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Destination table handle cannot be null".to_string(),
            });
        }
        if src_table_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Source table handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if dst < 0 || src < 0 || len < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Invalid parameters: dst={}, src={}, len={}",
                    dst, src, len
                ),
            });
        }

        // Get objects from handles
        let dst_table = unsafe { crate::table::core::get_table_ref(dst_table_ptr as *const c_void)? };
        let src_table = unsafe { crate::table::core::get_table_ref(src_table_ptr as *const c_void)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr as *const c_void)? };

        // Call table.copy_from
        dst_table.copy_from(store, dst as u32, src_table, src as u32, len as u32)?;

        Ok(())
    })
}

/// Grow a table by delta elements (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeTableGrow(
    mut env: JNIEnv,
    _class: JClass,
    table_ptr: jlong,
    store_ptr: jlong,
    delta: jint,
    init_value: jlong,
) -> jlong {
    jni_utils::jni_try_default(&env, -1, || {
        use std::os::raw::c_void;

        // Validate parameters
        if table_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Table handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if delta < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid delta: {}", delta),
            });
        }

        // Get objects from handles
        let table = unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr as *const c_void)? };

        // Create TableElement from init_value (assuming it's a funcref or null)
        let elem = if init_value == 0 {
            crate::table::TableElement::FuncRef(None)
        } else {
            // For non-null values, would need to handle properly
            crate::table::TableElement::FuncRef(None)
        };

        // Call table.grow
        let old_size = table.grow(store, delta as u32, elem)?;

        Ok(old_size as jlong)
    })
}

/// Fill a table with an element value (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeTableFill(
    mut env: JNIEnv,
    _class: JClass,
    table_ptr: jlong,
    store_ptr: jlong,
    dst: jint,
    value: jlong,
    len: jint,
) -> jint {
    jni_utils::jni_try_code(&mut env, || {
        use std::os::raw::c_void;

        // Validate parameters
        if table_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Table handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if dst < 0 || len < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid parameters: dst={}, len={}", dst, len),
            });
        }

        // Get objects from handles
        let table = unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr as *const c_void)? };

        // Create TableElement from value (assuming it's a funcref or null)
        let elem = if value == 0 {
            crate::table::TableElement::FuncRef(None)
        } else {
            // For non-null values, would need to handle properly
            crate::table::TableElement::FuncRef(None)
        };

        // Call table.fill
        table.fill(store, dst as u32, elem, len as u32)?;

        Ok(())
    })
}
