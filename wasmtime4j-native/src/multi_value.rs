//! Multi-value function support for WebAssembly
//!
//! This module implements the WebAssembly multi-value proposal,
//! allowing functions to return multiple values directly.

use crate::error::{WasmtimeError, WasmtimeResult};
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use wasmtime::*;

/// Multi-value function signature
#[derive(Debug, Clone)]
pub struct MultiValueSignature {
    /// Function name
    pub name: String,
    /// Parameter types
    pub parameter_types: Vec<ValType>,
    /// Return types (may be multiple)
    pub return_types: Vec<ValType>,
}

impl MultiValueSignature {
    /// Creates a new multi-value function signature
    pub fn new(
        name: String,
        parameter_types: Vec<ValType>,
        return_types: Vec<ValType>,
    ) -> Self {
        Self {
            name,
            parameter_types,
            return_types,
        }
    }

    /// Checks if this function returns multiple values
    pub fn is_multi_value(&self) -> bool {
        self.return_types.len() > 1
    }

    /// Gets the number of return values
    pub fn return_count(&self) -> usize {
        self.return_types.len()
    }

    /// Converts to a Wasmtime FuncType
    pub fn to_func_type(&self, engine: &Engine) -> FuncType {
        FuncType::new(
            engine,
            self.parameter_types.clone(),
            self.return_types.clone(),
        )
    }
}

/// Multi-value function result
#[derive(Debug, Clone)]
pub struct MultiValueResult {
    /// Return values
    pub values: Vec<Val>,
}

impl MultiValueResult {
    /// Creates a new multi-value result
    pub fn new(values: Vec<Val>) -> Self {
        Self { values }
    }

    /// Gets all return values
    pub fn values(&self) -> &[Val] {
        &self.values
    }

    /// Gets a specific return value by index
    pub fn get_value(&self, index: usize) -> Option<&Val> {
        self.values.get(index)
    }

    /// Gets the first return value
    pub fn first_value(&self) -> Option<&Val> {
        self.values.first()
    }

    /// Gets the number of return values
    pub fn value_count(&self) -> usize {
        self.values.len()
    }

    /// Checks if multiple values are present
    pub fn has_multiple_values(&self) -> bool {
        self.values.len() > 1
    }
}

/// Multi-value function configuration
#[derive(Debug, Clone)]
pub struct MultiValueConfig {
    /// Validate return types
    pub validate_return_types: bool,
    /// Enable parameter validation
    pub enable_parameter_validation: bool,
    /// Maximum return values allowed
    pub max_return_values: usize,
    /// Allow empty returns
    pub allow_empty_returns: bool,
}

impl Default for MultiValueConfig {
    fn default() -> Self {
        Self {
            validate_return_types: true,
            enable_parameter_validation: true,
            max_return_values: 16,
            allow_empty_returns: false,
        }
    }
}

/// Multi-value host function trait
pub trait MultiValueHostFunction: Send + Sync {
    /// Invokes the host function with the given parameters
    fn invoke(&self, parameters: &[Val]) -> WasmtimeResult<MultiValueResult>;
}

/// Multi-value function handler
pub struct MultiValueFunction {
    /// Configuration
    config: MultiValueConfig,
    /// Engine reference
    engine: Engine,
    /// Registered host functions
    host_functions: Arc<Mutex<HashMap<String, Box<dyn MultiValueHostFunction>>>>,
}

impl MultiValueFunction {
    /// Creates a new multi-value function handler
    pub fn new(config: MultiValueConfig, engine: Engine) -> WasmtimeResult<Self> {
        Ok(Self {
            config,
            engine,
            host_functions: Arc::new(Mutex::new(HashMap::new())),
        })
    }

    /// Calls a multi-value WebAssembly function
    pub fn call_function(
        &self,
        func: &Func,
        store: &mut Store<()>,
        signature: &MultiValueSignature,
        parameters: &[Val],
    ) -> WasmtimeResult<MultiValueResult> {
        // Validate parameters if enabled
        if self.config.enable_parameter_validation {
            self.validate_parameters(signature, parameters)?;
        }

        // Prepare result vector
        let mut results = vec![Val::I32(0); signature.return_types.len()];

        // Call the function
        func.call(store, parameters, &mut results)
            .map_err(|e| WasmtimeError::Runtime(format!("Function call failed: {}", e)))?;

        // Validate return values if enabled
        if self.config.validate_return_types {
            self.validate_return_values(signature, &results)?;
        }

        Ok(MultiValueResult::new(results))
    }

    /// Creates a multi-value host function
    pub fn create_host_function<F>(
        &self,
        signature: &MultiValueSignature,
        implementation: F,
    ) -> WasmtimeResult<Func>
    where
        F: Fn(&[Val]) -> WasmtimeResult<MultiValueResult> + Send + Sync + 'static,
    {
        let func_type = signature.to_func_type(&self.engine);
        let config = self.config.clone();
        let sig_clone = signature.clone();

        let func = Func::new(
            &mut Store::default(),
            func_type,
            move |_caller, params, results| {
                // Validate parameters
                if config.enable_parameter_validation {
                    if let Err(e) = Self::validate_parameters_static(&sig_clone, params) {
                        return Err(Trap::new(format!("Parameter validation failed: {}", e)));
                    }
                }

                // Call the implementation
                let result = implementation(params)
                    .map_err(|e| Trap::new(format!("Host function failed: {}", e)))?;

                // Validate return values
                if config.validate_return_types {
                    if let Err(e) = Self::validate_return_values_static(&sig_clone, &result.values) {
                        return Err(Trap::new(format!("Return value validation failed: {}", e)));
                    }
                }

                // Copy results
                if results.len() != result.values.len() {
                    return Err(Trap::new("Result count mismatch"));
                }

                for (i, value) in result.values.iter().enumerate() {
                    results[i] = value.clone();
                }

                Ok(())
            },
        );

        Ok(func)
    }

    /// Registers a multi-value host function
    pub fn register_host_function(
        &self,
        name: String,
        function: Box<dyn MultiValueHostFunction>,
    ) -> WasmtimeResult<()> {
        let mut host_functions = self.host_functions.lock().map_err(|_| {
            WasmtimeError::Internal("Failed to acquire host functions lock".to_string())
        })?;

        host_functions.insert(name, function);
        Ok(())
    }

    /// Gets a registered host function
    pub fn get_host_function(&self, name: &str) -> WasmtimeResult<Option<Box<dyn MultiValueHostFunction>>> {
        let host_functions = self.host_functions.lock().map_err(|_| {
            WasmtimeError::Internal("Failed to acquire host functions lock".to_string())
        })?;

        // Note: This is a simplified implementation. In practice, you'd need
        // to handle the borrowing correctly or use a different approach.
        Ok(None)
    }

    /// Validates function parameters against the signature
    fn validate_parameters(
        &self,
        signature: &MultiValueSignature,
        parameters: &[Val],
    ) -> WasmtimeResult<()> {
        Self::validate_parameters_static(signature, parameters)
    }

    /// Static version of parameter validation
    fn validate_parameters_static(
        signature: &MultiValueSignature,
        parameters: &[Val],
    ) -> WasmtimeResult<()> {
        if parameters.len() != signature.parameter_types.len() {
            return Err(WasmtimeError::ValidationError(format!(
                "Parameter count mismatch. Expected: {}, Actual: {}",
                signature.parameter_types.len(),
                parameters.len()
            )));
        }

        for (i, (expected_type, actual_value)) in signature
            .parameter_types
            .iter()
            .zip(parameters.iter())
            .enumerate()
        {
            if !Self::value_matches_type(actual_value, expected_type) {
                return Err(WasmtimeError::ValidationError(format!(
                    "Parameter {} type mismatch. Expected: {:?}, Actual: {:?}",
                    i,
                    expected_type,
                    actual_value.ty()
                )));
            }
        }

        Ok(())
    }

    /// Validates return values against the signature
    fn validate_return_values(
        &self,
        signature: &MultiValueSignature,
        return_values: &[Val],
    ) -> WasmtimeResult<()> {
        Self::validate_return_values_static(signature, return_values)
    }

    /// Static version of return value validation
    fn validate_return_values_static(
        signature: &MultiValueSignature,
        return_values: &[Val],
    ) -> WasmtimeResult<()> {
        if return_values.len() > 16 {
            return Err(WasmtimeError::ValidationError(format!(
                "Too many return values: {} (max allowed: 16)",
                return_values.len()
            )));
        }

        if return_values.is_empty() && !signature.return_types.is_empty() {
            return Err(WasmtimeError::ValidationError(
                "Function returned no values but signature expects return values".to_string(),
            ));
        }

        if return_values.len() != signature.return_types.len() {
            return Err(WasmtimeError::ValidationError(format!(
                "Return value count mismatch. Expected: {}, Actual: {}",
                signature.return_types.len(),
                return_values.len()
            )));
        }

        for (i, (expected_type, actual_value)) in signature
            .return_types
            .iter()
            .zip(return_values.iter())
            .enumerate()
        {
            if !Self::value_matches_type(actual_value, expected_type) {
                return Err(WasmtimeError::ValidationError(format!(
                    "Return value {} type mismatch. Expected: {:?}, Actual: {:?}",
                    i,
                    expected_type,
                    actual_value.ty()
                )));
            }
        }

        Ok(())
    }

    /// Checks if a value matches the expected type
    fn value_matches_type(value: &Val, expected_type: &ValType) -> bool {
        match (value, expected_type) {
            (Val::I32(_), ValType::I32) => true,
            (Val::I64(_), ValType::I64) => true,
            (Val::F32(_), ValType::F32) => true,
            (Val::F64(_), ValType::F64) => true,
            (Val::V128(_), ValType::V128) => true,
            (Val::FuncRef(_), ValType::FuncRef) => true,
            (Val::ExternRef(_), ValType::ExternRef) => true,
            _ => false,
        }
    }

    /// Gets the configuration
    pub fn config(&self) -> &MultiValueConfig {
        &self.config
    }

    /// Gets the engine
    pub fn engine(&self) -> &Engine {
        &self.engine
    }
}

/// Concrete implementation of MultiValueHostFunction for closures
pub struct ClosureHostFunction<F>
where
    F: Fn(&[Val]) -> WasmtimeResult<MultiValueResult> + Send + Sync,
{
    closure: F,
}

impl<F> ClosureHostFunction<F>
where
    F: Fn(&[Val]) -> WasmtimeResult<MultiValueResult> + Send + Sync,
{
    /// Creates a new closure host function
    pub fn new(closure: F) -> Self {
        Self { closure }
    }
}

impl<F> MultiValueHostFunction for ClosureHostFunction<F>
where
    F: Fn(&[Val]) -> WasmtimeResult<MultiValueResult> + Send + Sync,
{
    fn invoke(&self, parameters: &[Val]) -> WasmtimeResult<MultiValueResult> {
        (self.closure)(parameters)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_multi_value_signature() {
        let signature = MultiValueSignature::new(
            "test_func".to_string(),
            vec![ValType::I32, ValType::F64],
            vec![ValType::I32, ValType::F64, ValType::I32],
        );

        assert_eq!(signature.name, "test_func");
        assert_eq!(signature.parameter_types.len(), 2);
        assert_eq!(signature.return_types.len(), 3);
        assert!(signature.is_multi_value());
        assert_eq!(signature.return_count(), 3);
    }

    #[test]
    fn test_multi_value_result() {
        let values = vec![Val::I32(42), Val::F64(3.14), Val::I32(100)];
        let result = MultiValueResult::new(values);

        assert_eq!(result.value_count(), 3);
        assert!(result.has_multiple_values());

        if let Some(Val::I32(first)) = result.first_value() {
            assert_eq!(*first, 42);
        } else {
            panic!("Expected I32 value");
        }

        if let Some(Val::F64(second)) = result.get_value(1) {
            assert_eq!(*second, 3.14);
        } else {
            panic!("Expected F64 value");
        }
    }

    #[test]
    fn test_multi_value_config() {
        let config = MultiValueConfig::default();

        assert!(config.validate_return_types);
        assert!(config.enable_parameter_validation);
        assert_eq!(config.max_return_values, 16);
        assert!(!config.allow_empty_returns);
    }

    #[test]
    fn test_parameter_validation() {
        let signature = MultiValueSignature::new(
            "test".to_string(),
            vec![ValType::I32, ValType::F64],
            vec![ValType::I32],
        );

        let valid_params = vec![Val::I32(42), Val::F64(3.14)];
        assert!(MultiValueFunction::validate_parameters_static(&signature, &valid_params).is_ok());

        let invalid_count = vec![Val::I32(42)];
        assert!(MultiValueFunction::validate_parameters_static(&signature, &invalid_count).is_err());

        let invalid_types = vec![Val::F32(1.0), Val::F64(3.14)];
        assert!(MultiValueFunction::validate_parameters_static(&signature, &invalid_types).is_err());
    }

    #[test]
    fn test_return_value_validation() {
        let signature = MultiValueSignature::new(
            "test".to_string(),
            vec![ValType::I32],
            vec![ValType::I32, ValType::F64],
        );

        let valid_returns = vec![Val::I32(42), Val::F64(3.14)];
        assert!(MultiValueFunction::validate_return_values_static(&signature, &valid_returns).is_ok());

        let invalid_count = vec![Val::I32(42)];
        assert!(MultiValueFunction::validate_return_values_static(&signature, &invalid_count).is_err());

        let invalid_types = vec![Val::F32(1.0), Val::F64(3.14)];
        assert!(MultiValueFunction::validate_return_values_static(&signature, &invalid_types).is_err());
    }

    #[test]
    fn test_closure_host_function() {
        let host_func = ClosureHostFunction::new(|params: &[Val]| {
            if params.len() != 2 {
                return Err(WasmtimeError::ValidationError(
                    "Expected 2 parameters".to_string(),
                ));
            }

            if let (Val::I32(a), Val::I32(b)) = (&params[0], &params[1]) {
                let sum = a + b;
                let product = a * b;
                Ok(MultiValueResult::new(vec![Val::I32(sum), Val::I32(product)]))
            } else {
                Err(WasmtimeError::ValidationError(
                    "Expected I32 parameters".to_string(),
                ))
            }
        });

        let params = vec![Val::I32(5), Val::I32(3)];
        let result = host_func.invoke(&params).unwrap();

        assert_eq!(result.value_count(), 2);
        if let (Some(Val::I32(sum)), Some(Val::I32(product))) = (result.get_value(0), result.get_value(1)) {
            assert_eq!(*sum, 8);
            assert_eq!(*product, 15);
        } else {
            panic!("Expected I32 return values");
        }
    }
}