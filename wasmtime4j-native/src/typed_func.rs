//! Typed function support for zero-cost WebAssembly function calls
//!
//! This module provides `TypedFunc<Params, Results>` which allows calling
//! WebAssembly functions with statically known signatures without runtime
//! type checking overhead.

use crate::error::{WasmtimeError, WasmtimeResult};
use std::marker::PhantomData;
use wasmtime::*;

/// A statically typed WebAssembly function
///
/// `TypedFunc` represents a WebAssembly function with a known signature at compile time.
/// This allows for zero-cost abstraction when calling functions repeatedly, as type
/// checking is performed once during creation rather than on every call.
///
/// # Type Parameters
///
/// - `Params`: The parameter types (must implement `WasmParams`)
/// - `Results`: The return types (must implement `WasmResults`)
///
/// # Performance
///
/// `TypedFunc` provides significant performance improvements over dynamic `Func` calls:
/// - No runtime type checking on each call
/// - Direct memory layout matching
/// - Optimized parameter/result marshalling
///
/// # Example
///
/// ```ignore
/// // For a function: (i32, i32) -> i64
/// let typed_func = TypedFunc::<(i32, i32), i64>::new(store, func)?;
/// let result = typed_func.call(store, (10, 20))?;
/// ```
pub struct TypedFunc<Params, Results> {
    /// The underlying Wasmtime typed function
    inner: wasmtime::TypedFunc<Params, Results>,
    /// Parameter types phantom
    _params: PhantomData<Params>,
    /// Result types phantom
    _results: PhantomData<Results>,
}

impl<Params, Results> TypedFunc<Params, Results>
where
    Params: WasmParams,
    Results: WasmResults,
{
    /// Create a new typed function from a dynamic function
    ///
    /// # Arguments
    ///
    /// * `store` - The store context
    /// * `func` - The function to wrap with type information
    ///
    /// # Errors
    ///
    /// Returns an error if:
    /// - The function signature doesn't match `Params` and `Results`
    /// - Type conversion fails
    ///
    /// # Example
    ///
    /// ```ignore
    /// let func = instance.get_func(&mut store, "add")?;
    /// let typed = TypedFunc::<(i32, i32), i32>::new(&mut store, &func)?;
    /// ```
    pub fn new(store: impl AsContextMut, func: &Func) -> WasmtimeResult<Self> {
        let typed = func
            .typed::<Params, Results>(store)
            .map_err(|e| WasmtimeError::Type {
                message: format!("Failed to create typed function: {}", e),
            })?;

        Ok(TypedFunc {
            inner: typed,
            _params: PhantomData,
            _results: PhantomData,
        })
    }

    /// Call the typed function with the given parameters
    ///
    /// This is the high-performance call path that avoids runtime type checking.
    ///
    /// # Arguments
    ///
    /// * `store` - The store context
    /// * `params` - The parameters matching the function signature
    ///
    /// # Returns
    ///
    /// The function results matching the return type
    ///
    /// # Errors
    ///
    /// Returns an error if:
    /// - The function traps during execution
    /// - Stack overflow occurs
    /// - Out of fuel/epoch deadline exceeded
    ///
    /// # Example
    ///
    /// ```ignore
    /// let result = typed_func.call(&mut store, (5, 10))?;
    /// assert_eq!(result, 15);
    /// ```
    pub fn call(&self, mut store: impl AsContextMut, params: Params) -> WasmtimeResult<Results> {
        self.inner
            .call(store.as_context_mut(), params)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Typed function call failed: {}", e),
                backtrace: None,
            })
    }

    /// Get the underlying untyped function
    ///
    /// This is useful when you need to pass the function to APIs that expect `Func`.
    ///
    /// # Returns
    ///
    /// A clone of the underlying `Func`
    pub fn func(&self) -> Func {
        self.inner.func().clone()
    }
}

impl<Params, Results> Clone for TypedFunc<Params, Results> {
    fn clone(&self) -> Self {
        TypedFunc {
            inner: self.inner.clone(),
            _params: PhantomData,
            _results: PhantomData,
        }
    }
}

/// Registry for managing typed functions
///
/// This provides a centralized way to create and manage typed functions
/// with different signatures.
pub struct TypedFuncRegistry {
    /// Cache of typed functions by signature hash
    /// Format: "param_types -> result_types"
    signature_cache: std::sync::RwLock<std::collections::HashMap<String, u64>>,
}

impl TypedFuncRegistry {
    /// Create a new typed function registry
    pub fn new() -> Self {
        TypedFuncRegistry {
            signature_cache: std::sync::RwLock::new(std::collections::HashMap::new()),
        }
    }

    /// Generate a signature key for caching
    fn signature_key(param_types: &[ValType], result_types: &[ValType]) -> String {
        let params = param_types
            .iter()
            .map(|t| format!("{:?}", t))
            .collect::<Vec<_>>()
            .join(",");
        let results = result_types
            .iter()
            .map(|t| format!("{:?}", t))
            .collect::<Vec<_>>()
            .join(",");
        format!("({}) -> ({})", params, results)
    }

    /// Register a function signature
    ///
    /// This is used for tracking and validation purposes.
    pub fn register_signature(
        &self,
        param_types: &[ValType],
        result_types: &[ValType],
    ) -> WasmtimeResult<u64> {
        let key = Self::signature_key(param_types, result_types);
        let mut cache = self.signature_cache.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire signature cache lock".to_string(),
            }
        })?;

        let id = cache.len() as u64;
        cache.entry(key).or_insert(id);
        Ok(id)
    }

    /// Get the number of registered signatures
    pub fn signature_count(&self) -> WasmtimeResult<usize> {
        let cache = self.signature_cache.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire signature cache lock".to_string(),
            }
        })?;
        Ok(cache.len())
    }
}

impl Default for TypedFuncRegistry {
    fn default() -> Self {
        Self::new()
    }
}

/// Common typed function signatures
///
/// Pre-defined type aliases for frequently used function signatures.
pub mod signatures {
    use super::*;

    /// Function with no parameters or returns: () -> ()
    pub type VoidFunc = TypedFunc<(), ()>;

    /// Function returning i32: () -> i32
    pub type I32Func = TypedFunc<(), i32>;

    /// Function returning i64: () -> i64
    pub type I64Func = TypedFunc<(), i64>;

    /// Function taking i32 and returning i32: (i32) -> i32
    pub type UnaryI32Func = TypedFunc<i32, i32>;

    /// Function taking two i32s and returning i32: (i32, i32) -> i32
    pub type BinaryI32Func = TypedFunc<(i32, i32), i32>;

    /// Function taking i64 and returning i64: (i64) -> i64
    pub type UnaryI64Func = TypedFunc<i64, i64>;

    /// Function taking two i64s and returning i64: (i64, i64) -> i64
    pub type BinaryI64Func = TypedFunc<(i64, i64), i64>;

    /// Function taking f32 and returning f32: (f32) -> f32
    pub type UnaryF32Func = TypedFunc<f32, f32>;

    /// Function taking two f32s and returning f32: (f32, f32) -> f32
    pub type BinaryF32Func = TypedFunc<(f32, f32), f32>;

    /// Function taking f64 and returning f64: (f64) -> f64
    pub type UnaryF64Func = TypedFunc<f64, f64>;

    /// Function taking two f64s and returning f64: (f64, f64) -> f64
    pub type BinaryF64Func = TypedFunc<(f64, f64), f64>;

    /// Function taking i32 and i64, returning i64: (i32, i64) -> i64
    pub type MixedFunc = TypedFunc<(i32, i64), i64>;
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_typed_func_registry() {
        let registry = TypedFuncRegistry::new();

        // Register some signatures
        let id1 = registry
            .register_signature(&[ValType::I32, ValType::I32], &[ValType::I32])
            .unwrap();
        let id2 = registry
            .register_signature(&[ValType::I64], &[ValType::I64])
            .unwrap();

        assert_eq!(registry.signature_count().unwrap(), 2);

        // Registering same signature again should return same ID
        let id3 = registry
            .register_signature(&[ValType::I32, ValType::I32], &[ValType::I32])
            .unwrap();
        assert_eq!(id1, id3);
        assert_eq!(registry.signature_count().unwrap(), 2);
    }

    #[test]
    fn test_signature_key_generation() {
        let key1 =
            TypedFuncRegistry::signature_key(&[ValType::I32, ValType::I32], &[ValType::I32]);
        assert_eq!(key1, "(I32,I32) -> (I32)");

        let key2 = TypedFuncRegistry::signature_key(&[], &[]);
        assert_eq!(key2, "() -> ()");

        let key3 = TypedFuncRegistry::signature_key(
            &[ValType::I32, ValType::I64, ValType::F32],
            &[ValType::I64, ValType::F64],
        );
        assert_eq!(key3, "(I32,I64,F32) -> (I64,F64)");
    }
}
