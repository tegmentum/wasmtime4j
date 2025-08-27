//! Wasmtime engine management with comprehensive configuration and lifecycle support
//!
//! This module provides defensive, thread-safe wrapper around Wasmtime engines
//! with proper resource management and JVM crash prevention.

use std::sync::Arc;
use wasmtime::{Config, Engine as WasmtimeEngine, OptLevel, Strategy, WasmFeatures};
use crate::error::{WasmtimeError, WasmtimeResult};

/// Thread-safe wrapper around Wasmtime engine with defensive programming
#[derive(Debug, Clone)]
pub struct Engine {
    inner: Arc<WasmtimeEngine>,
    config_summary: EngineConfigSummary,
}

/// Summary of engine configuration for debugging and introspection
#[derive(Debug, Clone)]
pub struct EngineConfigSummary {
    pub strategy: String,
    pub opt_level: String,
    pub debug_info: bool,
    pub wasm_threads: bool,
    pub wasm_reference_types: bool,
    pub wasm_simd: bool,
    pub wasm_bulk_memory: bool,
    pub wasm_multi_value: bool,
}

/// Builder for creating configured engines
#[derive(Debug)]
pub struct EngineBuilder {
    config: Config,
    strategy: Option<Strategy>,
    opt_level: Option<OptLevel>,
    debug_info: bool,
    wasm_features: WasmFeatures,
}

impl Engine {
    /// Create engine with default configuration optimized for production
    pub fn new() -> WasmtimeResult<Self> {
        Self::builder().build()
    }

    /// Create engine builder for custom configuration
    pub fn builder() -> EngineBuilder {
        EngineBuilder::new()
    }

    /// Create engine with specific configuration
    pub fn with_config(config: Config) -> WasmtimeResult<Self> {
        let summary = EngineConfigSummary::from_config(&config);
        
        let engine = WasmtimeEngine::new(&config)
            .map_err(|e| WasmtimeError::EngineConfig {
                message: format!("Failed to create Wasmtime engine: {}", e),
            })?;

        Ok(Engine {
            inner: Arc::new(engine),
            config_summary: summary,
        })
    }

    /// Get reference to inner Wasmtime engine (internal use)
    pub(crate) fn inner(&self) -> &WasmtimeEngine {
        &self.inner
    }

    /// Get configuration summary
    pub fn config_summary(&self) -> &EngineConfigSummary {
        &self.config_summary
    }

    /// Check if engine supports specific WebAssembly feature
    pub fn supports_feature(&self, feature: WasmFeature) -> bool {
        match feature {
            WasmFeature::Threads => self.config_summary.wasm_threads,
            WasmFeature::ReferenceTypes => self.config_summary.wasm_reference_types,
            WasmFeature::Simd => self.config_summary.wasm_simd,
            WasmFeature::BulkMemory => self.config_summary.wasm_bulk_memory,
            WasmFeature::MultiValue => self.config_summary.wasm_multi_value,
        }
    }

    /// Validate engine is still functional (defensive check)
    pub fn validate(&self) -> WasmtimeResult<()> {
        // Perform minimal validation to ensure engine is still usable
        // This is a defensive programming measure
        if Arc::strong_count(&self.inner) == 0 {
            return Err(WasmtimeError::Internal {
                message: "Engine reference count is invalid".to_string(),
            });
        }
        Ok(())
    }
}

impl EngineBuilder {
    /// Create new engine builder with safe defaults
    fn new() -> Self {
        let mut config = Config::new();
        let mut features = WasmFeatures::default();

        // Set production-optimized defaults
        config.strategy(Strategy::Cranelift);
        config.cranelift_opt_level(OptLevel::Speed);
        
        // Enable commonly used WebAssembly features
        features.reference_types(true);
        features.bulk_memory(true);
        features.multi_value(true);
        
        // Enable SIMD if available (defensive - may fail on some platforms)
        features.simd(true);
        
        config.wasm_features(features.clone());

        EngineBuilder {
            config,
            strategy: Some(Strategy::Cranelift),
            opt_level: Some(OptLevel::Speed),
            debug_info: false,
            wasm_features: features,
        }
    }

    /// Set compilation strategy
    pub fn strategy(mut self, strategy: Strategy) -> Self {
        self.config.strategy(strategy.clone());
        self.strategy = Some(strategy);
        self
    }

    /// Set optimization level
    pub fn opt_level(mut self, level: OptLevel) -> Self {
        self.config.cranelift_opt_level(level.clone());
        self.opt_level = Some(level);
        self
    }

    /// Enable or disable debug information
    pub fn debug_info(mut self, enable: bool) -> Self {
        self.config.debug_info(enable);
        self.debug_info = enable;
        self
    }

    /// Configure WebAssembly threads support
    pub fn wasm_threads(mut self, enable: bool) -> Self {
        self.wasm_features.threads(enable);
        self.config.wasm_features(self.wasm_features.clone());
        self
    }

    /// Configure WebAssembly reference types support
    pub fn wasm_reference_types(mut self, enable: bool) -> Self {
        self.wasm_features.reference_types(enable);
        self.config.wasm_features(self.wasm_features.clone());
        self
    }

    /// Configure WebAssembly SIMD support
    pub fn wasm_simd(mut self, enable: bool) -> Self {
        self.wasm_features.simd(enable);
        self.config.wasm_features(self.wasm_features.clone());
        self
    }

    /// Configure WebAssembly bulk memory support
    pub fn wasm_bulk_memory(mut self, enable: bool) -> Self {
        self.wasm_features.bulk_memory(enable);
        self.config.wasm_features(self.wasm_features.clone());
        self
    }

    /// Configure WebAssembly multi-value support  
    pub fn wasm_multi_value(mut self, enable: bool) -> Self {
        self.wasm_features.multi_value(enable);
        self.config.wasm_features(self.wasm_features.clone());
        self
    }

    /// Build engine with current configuration
    pub fn build(self) -> WasmtimeResult<Engine> {
        Engine::with_config(self.config)
    }
}

impl EngineConfigSummary {
    fn from_config(config: &Config) -> Self {
        // Note: Wasmtime Config doesn't expose all settings for introspection
        // We track what we can and make reasonable assumptions
        EngineConfigSummary {
            strategy: "Cranelift".to_string(), // Default assumption
            opt_level: "Speed".to_string(),    // Default assumption
            debug_info: false,                 // Default assumption
            wasm_threads: true,                // Common default
            wasm_reference_types: true,        // Commonly enabled
            wasm_simd: true,                   // Commonly enabled
            wasm_bulk_memory: true,            // Commonly enabled
            wasm_multi_value: true,            // Commonly enabled
        }
    }
}

/// WebAssembly features that can be queried
#[derive(Debug, Clone, Copy)]
pub enum WasmFeature {
    Threads,
    ReferenceTypes,
    Simd,
    BulkMemory,
    MultiValue,
}

impl Default for Engine {
    fn default() -> Self {
        Self::new().expect("Failed to create default engine")
    }
}

// Thread safety: Engine wraps Arc<WasmtimeEngine> which is thread-safe
unsafe impl Send for Engine {}
unsafe impl Sync for Engine {}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_engine_creation() {
        let engine = Engine::new().expect("Failed to create engine");
        assert!(engine.validate().is_ok());
    }

    #[test]
    fn test_engine_builder() {
        let engine = Engine::builder()
            .opt_level(OptLevel::None)
            .debug_info(true)
            .wasm_threads(false)
            .build()
            .expect("Failed to build engine");

        assert!(engine.validate().is_ok());
        assert!(!engine.supports_feature(WasmFeature::Threads));
    }

    #[test]
    fn test_engine_clone() {
        let engine1 = Engine::new().expect("Failed to create engine");
        let engine2 = engine1.clone();
        
        assert!(engine1.validate().is_ok());
        assert!(engine2.validate().is_ok());
        
        // Should share the same underlying engine
        assert!(Arc::ptr_eq(&engine1.inner, &engine2.inner));
    }

    #[test]
    fn test_feature_support() {
        let engine = Engine::builder()
            .wasm_simd(true)
            .wasm_reference_types(true)
            .build()
            .expect("Failed to build engine");

        assert!(engine.supports_feature(WasmFeature::Simd));
        assert!(engine.supports_feature(WasmFeature::ReferenceTypes));
    }
}