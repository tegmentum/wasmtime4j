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
/// ```text
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
    /// ```text
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
    /// ```text
    /// let result = typed_func.call(&mut store, (5, 10))?;
    /// assert_eq!(result, 15);
    /// ```
    pub fn call(&self, mut store: impl AsContextMut, params: Params) -> WasmtimeResult<Results> {
        self.inner
            .call(store.as_context_mut(), params)
            .map_err(|e| WasmtimeError::from_wasmtime_error(e))
    }

    /// Call the typed function asynchronously with the given parameters
    ///
    /// This is the async call path for engines created with `async_support(true)`.
    /// It allows WebAssembly functions to be executed without blocking the calling thread.
    ///
    /// # Arguments
    ///
    /// * `store` - The store context
    /// * `params` - The parameters matching the function signature
    ///
    /// # Returns
    ///
    /// The function results matching the return type, wrapped in a future
    ///
    /// # Errors
    ///
    /// Returns an error if:
    /// - The engine was not created with async support enabled
    /// - The function traps during execution
    /// - Stack overflow occurs
    /// - Out of fuel/epoch deadline exceeded
    ///
    /// # Example
    ///
    /// ```text
    /// let result = typed_func.call_async(&mut store, (5, 10)).await?;
    /// assert_eq!(result, 15);
    /// ```
    pub async fn call_async(
        &self,
        mut store: impl AsContextMut<Data = crate::store::StoreData>,
        params: Params,
    ) -> WasmtimeResult<Results>
    where
        Params: Send + Sync,
        Results: Send + Sync,
    {
        self.inner
            .call_async(store.as_context_mut(), params)
            .await
            .map_err(|e| WasmtimeError::from_wasmtime_error(e))
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
