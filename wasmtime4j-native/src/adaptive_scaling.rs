//! Adaptive thread pool scaling with workload-based sizing
//!
//! This module implements intelligent thread pool scaling that adapts to workload
//! patterns and system conditions, featuring:
//! - Machine learning-based workload prediction
//! - Dynamic scaling with hysteresis and smoothing
//! - Resource utilization optimization
//! - Predictive scaling based on historical patterns
//! - Auto-tuning of scaling parameters

use std::sync::Arc;
use std::sync::atomic::{AtomicBool, AtomicU64, AtomicUsize, Ordering};
use std::collections::{HashMap, VecDeque};
use std::thread::JoinHandle;
use std::time::{Duration, SystemTime, UNIX_EPOCH};
use parking_lot::RwLock as ParkingRwLock;
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::work_stealing::WorkStealingScheduler;
use crate::thread_affinity::ThreadAffinityManager;

/// Adaptive thread pool scaling manager
pub struct AdaptiveScalingManager {
    /// Scaling configuration
    config: ScalingConfig,
    /// Workload predictor
    predictor: Arc<WorkloadPredictor>,
    /// Resource monitor
    resource_monitor: Arc<ResourceMonitor>,
    /// Scaling decision engine
    decision_engine: Arc<ScalingDecisionEngine>,
    /// Scaling executor
    scaling_executor: Arc<ScalingExecutor>,
    /// Performance metrics collector
    metrics_collector: Arc<PerformanceMetricsCollector>,
    /// Scaling history
    scaling_history: Arc<ParkingRwLock<VecDeque<ScalingDecision>>>,
    /// Current pool size
    current_pool_size: Arc<AtomicUsize>,
    /// Target pool size
    target_pool_size: Arc<AtomicUsize>,
    /// Scaling in progress flag
    scaling_in_progress: Arc<AtomicBool>,
    /// Management thread handle
    manager_thread: Option<JoinHandle<WasmtimeResult<()>>>,
    /// Shutdown flag
    shutdown: Arc<AtomicBool>,
    /// Total scaling operations counter
    total_scaling_operations: Arc<AtomicU64>,
    /// Successful scaling operations counter
    successful_operations: Arc<AtomicU64>,
    /// Failed scaling operations counter
    failed_operations: Arc<AtomicU64>,
}

/// Scaling configuration parameters
#[derive(Debug, Clone)]
pub struct ScalingConfig {
    /// Minimum thread pool size
    pub min_pool_size: usize,
    /// Maximum thread pool size
    pub max_pool_size: usize,
    /// Initial thread pool size
    pub initial_pool_size: usize,
    /// Scaling evaluation interval
    pub evaluation_interval: Duration,
    /// Scale-up threshold (load factor)
    pub scale_up_threshold: f64,
    /// Scale-down threshold (load factor)
    pub scale_down_threshold: f64,
    /// Scaling velocity (threads per scaling operation)
    pub scaling_velocity: usize,
    /// Hysteresis factor to prevent oscillation
    pub hysteresis_factor: f64,
    /// Cooldown period between scaling operations
    pub scaling_cooldown: Duration,
    /// Enable predictive scaling
    pub predictive_scaling_enabled: bool,
    /// Prediction horizon
    pub prediction_horizon: Duration,
    /// Enable auto-tuning of parameters
    pub auto_tuning_enabled: bool,
    /// Learning rate for auto-tuning
    pub learning_rate: f64,
    /// Resource utilization weight in scaling decisions
    pub resource_weight: f64,
    /// Workload prediction weight in scaling decisions
    pub prediction_weight: f64,
}

/// Workload pattern predictor using machine learning
pub struct WorkloadPredictor {
    /// Historical workload data
    workload_history: Arc<ParkingRwLock<VecDeque<WorkloadSample>>>,
    /// Prediction model
    model: Arc<ParkingRwLock<PredictionModel>>,
    /// Model training parameters
    training_config: TrainingConfig,
    /// Prediction cache
    prediction_cache: Arc<ParkingRwLock<HashMap<u64, PredictionResult>>>,
    /// Model performance metrics
    model_metrics: Arc<ParkingRwLock<ModelMetrics>>,
    /// Feature extractors
    feature_extractors: Vec<Box<dyn FeatureExtractor + Send + Sync>>,
}

/// Workload sample for training and prediction
#[derive(Debug, Clone)]
pub struct WorkloadSample {
    /// Sample timestamp
    pub timestamp: SystemTime,
    /// Task arrival rate (tasks per second)
    pub arrival_rate: f64,
    /// Task completion rate (tasks per second)
    pub completion_rate: f64,
    /// Average task execution time
    pub avg_execution_time: Duration,
    /// Queue length
    pub queue_length: usize,
    /// Thread utilization
    pub thread_utilization: f64,
    /// CPU utilization
    pub cpu_utilization: f64,
    /// Memory utilization
    pub memory_utilization: f64,
    /// System load average
    pub load_average: f64,
    /// Time-based features (hour of day, day of week)
    pub time_features: TimeFeatures,
}

/// Time-based features for temporal patterns
#[derive(Debug, Clone)]
pub struct TimeFeatures {
    /// Hour of day (0-23)
    pub hour_of_day: u8,
    /// Day of week (0-6, Sunday = 0)
    pub day_of_week: u8,
    /// Day of month (1-31)
    pub day_of_month: u8,
    /// Week of year (1-53)
    pub week_of_year: u8,
    /// Month of year (1-12)
    pub month_of_year: u8,
    /// Is weekend
    pub is_weekend: bool,
    /// Is business hours (9 AM - 5 PM weekdays)
    pub is_business_hours: bool,
}

/// Machine learning prediction model
#[derive(Debug, Clone)]
pub struct PredictionModel {
    /// Model type
    model_type: ModelType,
    /// Model parameters
    parameters: ModelParameters,
    /// Feature weights
    feature_weights: Vec<f64>,
    /// Model accuracy metrics
    accuracy: f64,
    /// Model training count
    training_iterations: u64,
    /// Last training timestamp
    last_training: SystemTime,
}

/// Types of prediction models
#[derive(Debug, Clone, Copy)]
pub enum ModelType {
    /// Linear regression model
    LinearRegression,
    /// Moving average model
    MovingAverage,
    /// Exponential smoothing model
    ExponentialSmoothing,
    /// Neural network model
    NeuralNetwork,
    /// Ensemble model combining multiple approaches
    Ensemble,
}

/// Model parameters for different model types
#[derive(Debug, Clone)]
pub enum ModelParameters {
    /// Linear regression parameters
    LinearRegression {
        intercept: f64,
        coefficients: Vec<f64>,
    },
    /// Moving average parameters
    MovingAverage {
        window_size: usize,
        weights: Vec<f64>,
    },
    /// Exponential smoothing parameters
    ExponentialSmoothing {
        alpha: f64,
        beta: f64,
        gamma: f64,
    },
    /// Neural network parameters
    NeuralNetwork {
        layers: Vec<LayerParameters>,
        activation_function: ActivationFunction,
    },
    /// Ensemble parameters
    Ensemble {
        models: Vec<PredictionModel>,
        weights: Vec<f64>,
    },
}

/// Neural network layer parameters
#[derive(Debug, Clone)]
pub struct LayerParameters {
    /// Layer weights matrix
    pub weights: Vec<Vec<f64>>,
    /// Layer biases
    pub biases: Vec<f64>,
    /// Layer size
    pub size: usize,
}

/// Neural network activation functions
#[derive(Debug, Clone, Copy)]
pub enum ActivationFunction {
    /// Linear activation
    Linear,
    /// ReLU activation
    ReLU,
    /// Sigmoid activation
    Sigmoid,
    /// Tanh activation
    Tanh,
}

/// Model training configuration
#[derive(Debug, Clone)]
pub struct TrainingConfig {
    /// Training data window size
    pub training_window_size: usize,
    /// Minimum samples required for training
    pub min_training_samples: usize,
    /// Training frequency
    pub training_frequency: Duration,
    /// Learning rate
    pub learning_rate: f64,
    /// Regularization factor
    pub regularization_factor: f64,
    /// Early stopping threshold
    pub early_stopping_threshold: f64,
    /// Maximum training iterations
    pub max_training_iterations: usize,
}

/// Prediction result
#[derive(Debug, Clone)]
pub struct PredictionResult {
    /// Predicted workload metrics
    pub predicted_workload: WorkloadSample,
    /// Confidence score (0.0 - 1.0)
    pub confidence: f64,
    /// Prediction horizon
    pub horizon: Duration,
    /// Prediction timestamp
    pub predicted_at: SystemTime,
    /// Contributing features
    pub feature_contributions: HashMap<String, f64>,
}

/// Model performance metrics
#[derive(Debug, Clone, Default)]
pub struct ModelMetrics {
    /// Mean absolute error
    pub mean_absolute_error: f64,
    /// Root mean square error
    pub root_mean_square_error: f64,
    /// Mean absolute percentage error
    pub mean_absolute_percentage_error: f64,
    /// Model accuracy (0.0 - 1.0)
    pub accuracy: f64,
    /// R-squared coefficient
    pub r_squared: f64,
    /// Prediction hit rate
    pub prediction_hit_rate: f64,
    /// Total predictions made
    pub total_predictions: u64,
    /// Correct predictions
    pub correct_predictions: u64,
}

/// Feature extractor trait for workload analysis
pub trait FeatureExtractor: Send + Sync {
    /// Extract features from workload sample
    fn extract_features(&self, sample: &WorkloadSample) -> Vec<f64>;

    /// Get feature names
    fn get_feature_names(&self) -> Vec<String>;

    /// Get feature importance scores
    fn get_feature_importance(&self) -> Vec<f64>;
}

/// Basic statistical feature extractor
pub struct StatisticalFeatureExtractor {
    /// Window size for statistical calculations
    window_size: usize,
}

/// Time-based feature extractor
pub struct TimeBasedFeatureExtractor {
    /// Include cyclical encodings
    include_cyclical: bool,
}

/// Trend analysis feature extractor
pub struct TrendAnalysisFeatureExtractor {
    /// Trend analysis window
    analysis_window: usize,
}

/// System resource monitor
pub struct ResourceMonitor {
    /// Monitoring configuration
    config: ResourceMonitoringConfig,
    /// CPU utilization tracker
    cpu_monitor: CpuMonitor,
    /// Memory utilization tracker
    memory_monitor: MemoryMonitor,
    /// Network utilization tracker
    network_monitor: NetworkMonitor,
    /// Disk I/O utilization tracker
    disk_monitor: DiskMonitor,
    /// System metrics cache
    metrics_cache: Arc<ParkingRwLock<SystemMetrics>>,
    /// Monitoring thread handle
    monitor_thread: Option<JoinHandle<WasmtimeResult<()>>>,
    /// Shutdown flag
    shutdown: Arc<AtomicBool>,
}

/// Resource monitoring configuration
#[derive(Debug, Clone)]
pub struct ResourceMonitoringConfig {
    /// Monitoring interval
    pub monitoring_interval: Duration,
    /// Enable CPU monitoring
    pub cpu_monitoring_enabled: bool,
    /// Enable memory monitoring
    pub memory_monitoring_enabled: bool,
    /// Enable network monitoring
    pub network_monitoring_enabled: bool,
    /// Enable disk I/O monitoring
    pub disk_monitoring_enabled: bool,
    /// History retention period
    pub history_retention: Duration,
    /// Alerting thresholds
    pub alerting_thresholds: AlertingThresholds,
}

/// System alerting thresholds
#[derive(Debug, Clone)]
pub struct AlertingThresholds {
    /// CPU utilization threshold
    pub cpu_threshold: f64,
    /// Memory utilization threshold
    pub memory_threshold: f64,
    /// Network utilization threshold
    pub network_threshold: f64,
    /// Disk utilization threshold
    pub disk_threshold: f64,
}

/// CPU utilization monitor
pub struct CpuMonitor {
    /// Per-core utilization
    core_utilization: Vec<AtomicU64>, // Fixed-point percentage * 100
    /// Overall utilization
    overall_utilization: AtomicU64,
    /// Load averages
    load_averages: [AtomicU64; 3], // 1min, 5min, 15min * 100
}

/// Memory utilization monitor
pub struct MemoryMonitor {
    /// Total memory in bytes
    total_memory: AtomicU64,
    /// Used memory in bytes
    used_memory: AtomicU64,
    /// Available memory in bytes
    available_memory: AtomicU64,
    /// Memory pressure indicator
    memory_pressure: AtomicU64, // 0-100
}

/// Network utilization monitor
pub struct NetworkMonitor {
    /// Bytes received
    bytes_received: AtomicU64,
    /// Bytes transmitted
    bytes_transmitted: AtomicU64,
    /// Packets received
    packets_received: AtomicU64,
    /// Packets transmitted
    packets_transmitted: AtomicU64,
    /// Network utilization percentage
    utilization: AtomicU64,
}

/// Disk I/O utilization monitor
pub struct DiskMonitor {
    /// Bytes read
    bytes_read: AtomicU64,
    /// Bytes written
    bytes_written: AtomicU64,
    /// Read operations
    read_operations: AtomicU64,
    /// Write operations
    write_operations: AtomicU64,
    /// Disk utilization percentage
    utilization: AtomicU64,
}

/// System metrics snapshot
#[derive(Debug, Clone)]
pub struct SystemMetrics {
    /// CPU metrics
    pub cpu: CpuMetrics,
    /// Memory metrics
    pub memory: MemoryMetrics,
    /// Network metrics
    pub network: NetworkMetrics,
    /// Disk metrics
    pub disk: DiskMetrics,
    /// Timestamp
    pub timestamp: SystemTime,
}

/// CPU metrics
#[derive(Debug, Clone)]
pub struct CpuMetrics {
    /// Per-core utilization percentages
    pub core_utilization: Vec<f64>,
    /// Overall CPU utilization
    pub overall_utilization: f64,
    /// Load averages [1min, 5min, 15min]
    pub load_averages: [f64; 3],
    /// CPU frequency information
    pub frequencies: Vec<u32>,
}

/// Memory metrics
#[derive(Debug, Clone)]
pub struct MemoryMetrics {
    /// Total memory in bytes
    pub total: u64,
    /// Used memory in bytes
    pub used: u64,
    /// Available memory in bytes
    pub available: u64,
    /// Memory utilization percentage
    pub utilization: f64,
    /// Memory pressure level
    pub pressure: f64,
}

/// Workload metrics for adaptive scaling
#[derive(Debug, Clone)]
pub struct WorkloadMetrics {
    /// CPU utilization percentage (0.0 - 1.0)
    pub cpu_utilization: f64,
    /// Memory utilization percentage (0.0 - 1.0)
    pub memory_utilization: f64,
    /// Current queue depth
    pub queue_depth: u64,
    /// Throughput in operations per second
    pub throughput: f64,
    /// Average response time in milliseconds
    pub response_time_ms: f64,
    /// Error rate percentage (0.0 - 1.0)
    pub error_rate: f64,
}

/// Network metrics
#[derive(Debug, Clone)]
pub struct NetworkMetrics {
    /// Network throughput in bytes per second
    pub throughput: u64,
    /// Network utilization percentage
    pub utilization: f64,
    /// Packet loss rate
    pub packet_loss: f64,
    /// Network latency
    pub latency: Duration,
}

/// Disk I/O metrics
#[derive(Debug, Clone)]
pub struct DiskMetrics {
    /// Disk throughput in bytes per second
    pub throughput: u64,
    /// Disk utilization percentage
    pub utilization: f64,
    /// Average response time
    pub response_time: Duration,
    /// Queue depth
    pub queue_depth: u32,
}

/// Scaling decision engine
pub struct ScalingDecisionEngine {
    /// Decision algorithms
    algorithms: Vec<Box<dyn ScalingAlgorithm + Send + Sync>>,
    /// Algorithm weights
    algorithm_weights: Vec<f64>,
    /// Decision history
    decision_history: Arc<ParkingRwLock<VecDeque<DecisionRecord>>>,
    /// Decision metrics
    metrics: Arc<ParkingRwLock<DecisionMetrics>>,
}

/// Scaling algorithm trait
pub trait ScalingAlgorithm: Send + Sync {
    /// Make scaling decision based on current state
    fn make_decision(
        &self,
        current_metrics: &SystemMetrics,
        workload_prediction: &PredictionResult,
        current_pool_size: usize,
        config: &ScalingConfig,
    ) -> ScalingDecision;

    /// Get algorithm name
    fn get_name(&self) -> &str;

    /// Get algorithm confidence in decision
    fn get_confidence(&self) -> f64;
}

/// Threshold-based scaling algorithm
pub struct ThresholdBasedAlgorithm {
    /// Algorithm configuration
    config: ThresholdAlgorithmConfig,
}

/// Threshold algorithm configuration
#[derive(Debug, Clone)]
pub struct ThresholdAlgorithmConfig {
    /// CPU utilization thresholds
    pub cpu_thresholds: (f64, f64), // (scale_up, scale_down)
    /// Memory utilization thresholds
    pub memory_thresholds: (f64, f64),
    /// Queue length thresholds
    pub queue_thresholds: (usize, usize),
    /// Response time thresholds
    pub response_time_thresholds: (Duration, Duration),
}

/// Predictive scaling algorithm
pub struct PredictiveScalingAlgorithm {
    /// Prediction horizon
    horizon: Duration,
    /// Confidence threshold
    confidence_threshold: f64,
}

/// Machine learning-based scaling algorithm
pub struct MLScalingAlgorithm {
    /// ML model for scaling decisions
    model: Arc<ParkingRwLock<ScalingMLModel>>,
    /// Feature processors
    feature_processors: Vec<Box<dyn FeatureProcessor + Send + Sync>>,
}

/// Feature processor trait
pub trait FeatureProcessor: Send + Sync {
    /// Process features for ML model
    fn process_features(&self, metrics: &SystemMetrics) -> Vec<f64>;
}

/// Scaling ML model
pub struct ScalingMLModel {
    /// Model weights
    weights: Vec<f64>,
    /// Model bias
    bias: f64,
    /// Model accuracy
    accuracy: f64,
}

/// Scaling decision
#[derive(Debug, Clone)]
pub struct ScalingDecision {
    /// Recommended action
    pub action: ScalingAction,
    /// Target pool size
    pub target_size: usize,
    /// Confidence in decision (0.0 - 1.0)
    pub confidence: f64,
    /// Decision rationale
    pub rationale: String,
    /// Contributing factors
    pub factors: HashMap<String, f64>,
    /// Decision timestamp
    pub timestamp: SystemTime,
    /// Algorithm that made the decision
    pub algorithm: String,
}

/// Scaling actions
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum ScalingAction {
    /// No scaling needed
    NoAction,
    /// Scale up thread pool
    ScaleUp,
    /// Scale down thread pool
    ScaleDown,
    /// Emergency scale up
    EmergencyScaleUp,
    /// Gradual scale down
    GradualScaleDown,
}

/// Decision record for history tracking
#[derive(Debug, Clone)]
pub struct DecisionRecord {
    /// Decision made
    pub decision: ScalingDecision,
    /// System state at decision time
    pub system_state: SystemMetrics,
    /// Actual outcome
    pub outcome: Option<ScalingOutcome>,
    /// Decision effectiveness score
    pub effectiveness: Option<f64>,
}

/// Scaling decision outcome
#[derive(Debug, Clone)]
pub struct ScalingOutcome {
    /// Actual pool size change
    pub actual_size_change: i32,
    /// Performance improvement
    pub performance_improvement: f64,
    /// Resource utilization change
    pub resource_utilization_change: f64,
    /// Time to achieve target state
    pub time_to_target: Duration,
}

/// Decision engine metrics
#[derive(Debug, Clone, Default)]
pub struct DecisionMetrics {
    /// Total decisions made
    pub total_decisions: u64,
    /// Correct decisions
    pub correct_decisions: u64,
    /// False positives
    pub false_positives: u64,
    /// False negatives
    pub false_negatives: u64,
    /// Decision accuracy
    pub decision_accuracy: f64,
    /// Average decision time
    pub avg_decision_time: Duration,
}

/// Scaling executor
pub struct ScalingExecutor {
    /// Executor configuration
    config: ExecutorConfig,
    /// Thread pool reference
    thread_pool: Arc<WorkStealingScheduler>,
    /// Affinity manager reference
    affinity_manager: Arc<ThreadAffinityManager>,
    /// Active scaling operations
    active_operations: Arc<ParkingRwLock<HashMap<u64, ScalingOperation>>>,
    /// Execution metrics
    metrics: Arc<ParkingRwLock<ExecutorMetrics>>,
    /// Operation ID counter
    next_operation_id: AtomicU64,
}

/// Scaling executor configuration
#[derive(Debug, Clone)]
pub struct ExecutorConfig {
    /// Maximum concurrent scaling operations
    pub max_concurrent_operations: usize,
    /// Scaling operation timeout
    pub operation_timeout: Duration,
    /// Thread creation timeout
    pub thread_creation_timeout: Duration,
    /// Thread destruction timeout
    pub thread_destruction_timeout: Duration,
    /// Enable graceful scaling
    pub graceful_scaling: bool,
    /// Scaling batch size
    pub batch_size: usize,
}

/// Scaling operation
#[derive(Debug, Clone)]
pub struct ScalingOperation {
    /// Operation ID
    pub id: u64,
    /// Operation type
    pub operation_type: ScalingAction,
    /// Target size
    pub target_size: usize,
    /// Current progress
    pub progress: f64,
    /// Operation start time
    pub started_at: SystemTime,
    /// Estimated completion time
    pub estimated_completion: SystemTime,
    /// Operation status
    pub status: OperationStatus,
}

/// Scaling operation status
#[derive(Debug, Clone, Copy)]
pub enum OperationStatus {
    /// Operation pending
    Pending,
    /// Operation in progress
    InProgress,
    /// Operation completed successfully
    Completed,
    /// Operation failed
    Failed,
    /// Operation cancelled
    Cancelled,
    /// Operation timed out
    TimedOut,
}

/// Scaling executor metrics
#[derive(Debug, Clone, Default)]
pub struct ExecutorMetrics {
    /// Total operations executed
    pub total_operations: u64,
    /// Successful operations
    pub successful_operations: u64,
    /// Failed operations
    pub failed_operations: u64,
    /// Average operation duration
    pub avg_operation_duration: Duration,
    /// Operations per minute
    pub operations_per_minute: f64,
}

/// Performance metrics collector
pub struct PerformanceMetricsCollector {
    /// Collection configuration
    config: CollectionConfig,
    /// Metrics history
    metrics_history: Arc<ParkingRwLock<VecDeque<PerformanceSnapshot>>>,
    /// Real-time metrics
    current_metrics: Arc<ParkingRwLock<PerformanceSnapshot>>,
    /// Collection thread handle
    collector_thread: Option<JoinHandle<WasmtimeResult<()>>>,
    /// Shutdown flag
    shutdown: Arc<AtomicBool>,
}

/// Metrics collection configuration
#[derive(Debug, Clone)]
pub struct CollectionConfig {
    /// Collection interval
    pub collection_interval: Duration,
    /// Metrics retention period
    pub retention_period: Duration,
    /// Enable detailed profiling
    pub detailed_profiling: bool,
    /// Aggregation window size
    pub aggregation_window: Duration,
}

/// Performance snapshot
#[derive(Debug, Clone)]
pub struct PerformanceSnapshot {
    /// Snapshot timestamp
    pub timestamp: SystemTime,
    /// Task throughput (tasks per second)
    pub throughput: f64,
    /// Average response time
    pub avg_response_time: Duration,
    /// 95th percentile response time
    pub p95_response_time: Duration,
    /// 99th percentile response time
    pub p99_response_time: Duration,
    /// Error rate
    pub error_rate: f64,
    /// Resource utilization
    pub resource_utilization: ResourceUtilization,
    /// Thread pool metrics
    pub thread_pool_metrics: ThreadPoolMetrics,
}

/// Resource utilization metrics
#[derive(Debug, Clone)]
pub struct ResourceUtilization {
    /// CPU utilization
    pub cpu: f64,
    /// Memory utilization
    pub memory: f64,
    /// Network utilization
    pub network: f64,
    /// Disk utilization
    pub disk: f64,
}

/// Thread pool specific metrics
#[derive(Debug, Clone)]
pub struct ThreadPoolMetrics {
    /// Active threads
    pub active_threads: usize,
    /// Idle threads
    pub idle_threads: usize,
    /// Queue length
    pub queue_length: usize,
    /// Thread creation rate
    pub thread_creation_rate: f64,
    /// Thread destruction rate
    pub thread_destruction_rate: f64,
    /// Load balancing efficiency
    pub load_balance_efficiency: f64,
}

// Implementation of default configurations
impl Default for ScalingConfig {
    fn default() -> Self {
        let cpu_count = num_cpus::get();
        Self {
            min_pool_size: (cpu_count / 4).max(1),
            max_pool_size: cpu_count * 4,
            initial_pool_size: cpu_count,
            evaluation_interval: Duration::from_millis(1000),
            scale_up_threshold: 0.8,
            scale_down_threshold: 0.2,
            scaling_velocity: (cpu_count / 4).max(1),
            hysteresis_factor: 0.1,
            scaling_cooldown: Duration::from_secs(30),
            predictive_scaling_enabled: true,
            prediction_horizon: Duration::from_secs(5 * 60),
            auto_tuning_enabled: true,
            learning_rate: 0.01,
            resource_weight: 0.6,
            prediction_weight: 0.4,
        }
    }
}

impl Default for TrainingConfig {
    fn default() -> Self {
        Self {
            training_window_size: 1000,
            min_training_samples: 50,
            training_frequency: Duration::from_secs(10 * 60),
            learning_rate: 0.01,
            regularization_factor: 0.001,
            early_stopping_threshold: 0.001,
            max_training_iterations: 100,
        }
    }
}

impl Default for ResourceMonitoringConfig {
    fn default() -> Self {
        Self {
            monitoring_interval: Duration::from_millis(500),
            cpu_monitoring_enabled: true,
            memory_monitoring_enabled: true,
            network_monitoring_enabled: false,
            disk_monitoring_enabled: false,
            history_retention: Duration::from_secs(1 * 60 * 60),
            alerting_thresholds: AlertingThresholds {
                cpu_threshold: 0.9,
                memory_threshold: 0.9,
                network_threshold: 0.8,
                disk_threshold: 0.8,
            },
        }
    }
}

impl Default for ExecutorConfig {
    fn default() -> Self {
        Self {
            max_concurrent_operations: 4,
            operation_timeout: Duration::from_secs(60),
            thread_creation_timeout: Duration::from_secs(10),
            thread_destruction_timeout: Duration::from_secs(5),
            graceful_scaling: true,
            batch_size: 4,
        }
    }
}

impl Default for CollectionConfig {
    fn default() -> Self {
        Self {
            collection_interval: Duration::from_millis(100),
            retention_period: Duration::from_secs(2 * 60 * 60),
            detailed_profiling: false,
            aggregation_window: Duration::from_secs(10),
        }
    }
}

// Stub implementations for complex components
impl AdaptiveScalingManager {
    /// Create a new adaptive scaling manager
    pub fn new(
        config: ScalingConfig,
        thread_pool: Arc<WorkStealingScheduler>,
        affinity_manager: Arc<ThreadAffinityManager>,
    ) -> WasmtimeResult<Self> {
        let predictor = Arc::new(WorkloadPredictor::new(TrainingConfig::default())?);
        let resource_monitor = Arc::new(ResourceMonitor::new(ResourceMonitoringConfig::default())?);
        let decision_engine = Arc::new(ScalingDecisionEngine::new()?);
        let scaling_executor = Arc::new(ScalingExecutor::new(
            ExecutorConfig::default(),
            thread_pool,
            affinity_manager,
        )?);
        let metrics_collector = Arc::new(PerformanceMetricsCollector::new(CollectionConfig::default())?);
        let initial_pool_size = config.initial_pool_size;

        Ok(Self {
            config,
            predictor,
            resource_monitor,
            decision_engine,
            scaling_executor,
            metrics_collector,
            scaling_history: Arc::new(ParkingRwLock::new(VecDeque::new())),
            current_pool_size: Arc::new(AtomicUsize::new(initial_pool_size)),
            target_pool_size: Arc::new(AtomicUsize::new(initial_pool_size)),
            scaling_in_progress: Arc::new(AtomicBool::new(false)),
            manager_thread: None,
            shutdown: Arc::new(AtomicBool::new(false)),
            total_scaling_operations: Arc::new(AtomicU64::new(0)),
            successful_operations: Arc::new(AtomicU64::new(0)),
            failed_operations: Arc::new(AtomicU64::new(0)),
        })
    }

    /// Start the adaptive scaling manager
    pub fn start(&mut self) -> WasmtimeResult<()> {
        // Start component subsystems
        self.resource_monitor.start()?;
        self.metrics_collector.start()?;

        // Start main management loop
        // Implementation would start the management thread here

        log::info!("Adaptive scaling manager started");
        Ok(())
    }

    /// Stop the adaptive scaling manager
    pub fn stop(&mut self) -> WasmtimeResult<()> {
        self.shutdown.store(true, Ordering::Release);

        // Stop subsystems
        self.resource_monitor.stop()?;
        self.metrics_collector.stop()?;

        // Wait for management thread
        if let Some(handle) = self.manager_thread.take() {
            handle.join().map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to join management thread".to_string(),
            })??;
        }

        log::info!("Adaptive scaling manager stopped");
        Ok(())
    }

    /// Get current scaling statistics
    pub fn get_statistics(&self) -> ScalingStatistics {
        ScalingStatistics {
            current_pool_size: self.current_pool_size.load(Ordering::Relaxed),
            target_pool_size: self.target_pool_size.load(Ordering::Relaxed),
            scaling_in_progress: self.scaling_in_progress.load(Ordering::Relaxed),
            total_scaling_operations: self.total_scaling_operations.load(Ordering::Relaxed),
            successful_operations: self.successful_operations.load(Ordering::Relaxed),
            failed_operations: self.failed_operations.load(Ordering::Relaxed),
        }
    }

    /// Record a scaling operation attempt
    pub fn record_scaling_attempt(&self, success: bool) {
        self.total_scaling_operations.fetch_add(1, Ordering::Relaxed);
        if success {
            self.successful_operations.fetch_add(1, Ordering::Relaxed);
        } else {
            self.failed_operations.fetch_add(1, Ordering::Relaxed);
        }
    }
}

/// Scaling statistics
#[derive(Debug, Clone)]
pub struct ScalingStatistics {
    /// Current thread pool size
    pub current_pool_size: usize,
    /// Target thread pool size
    pub target_pool_size: usize,
    /// Scaling operation in progress
    pub scaling_in_progress: bool,
    /// Total scaling operations performed
    pub total_scaling_operations: u64,
    /// Successful operations
    pub successful_operations: u64,
    /// Failed operations
    pub failed_operations: u64,
}

// Stub implementations for major components
impl WorkloadPredictor {
    pub fn new(_config: TrainingConfig) -> WasmtimeResult<Self> {
        Ok(Self {
            workload_history: Arc::new(ParkingRwLock::new(VecDeque::new())),
            model: Arc::new(ParkingRwLock::new(PredictionModel::new(ModelType::MovingAverage)?)),
            training_config: TrainingConfig::default(),
            prediction_cache: Arc::new(ParkingRwLock::new(HashMap::new())),
            model_metrics: Arc::new(ParkingRwLock::new(ModelMetrics::default())),
            feature_extractors: vec![
                Box::new(StatisticalFeatureExtractor::new(10)),
                Box::new(TimeBasedFeatureExtractor::new(true)),
                Box::new(TrendAnalysisFeatureExtractor::new(20)),
            ],
        })
    }
}

impl PredictionModel {
    pub fn new(model_type: ModelType) -> WasmtimeResult<Self> {
        let parameters = match model_type {
            ModelType::MovingAverage => ModelParameters::MovingAverage {
                window_size: 10,
                weights: vec![1.0; 10],
            },
            _ => ModelParameters::MovingAverage {
                window_size: 10,
                weights: vec![1.0; 10],
            },
        };

        Ok(Self {
            model_type,
            parameters,
            feature_weights: Vec::new(),
            accuracy: 0.0,
            training_iterations: 0,
            last_training: UNIX_EPOCH,
        })
    }
}

impl ResourceMonitor {
    pub fn new(_config: ResourceMonitoringConfig) -> WasmtimeResult<Self> {
        let cpu_count = num_cpus::get();
        Ok(Self {
            config: ResourceMonitoringConfig::default(),
            cpu_monitor: CpuMonitor::new(cpu_count),
            memory_monitor: MemoryMonitor::new(),
            network_monitor: NetworkMonitor::new(),
            disk_monitor: DiskMonitor::new(),
            metrics_cache: Arc::new(ParkingRwLock::new(SystemMetrics::default())),
            monitor_thread: None,
            shutdown: Arc::new(AtomicBool::new(false)),
        })
    }

    pub fn start(&self) -> WasmtimeResult<()> {
        // Implementation would start monitoring thread
        Ok(())
    }

    pub fn stop(&self) -> WasmtimeResult<()> {
        self.shutdown.store(true, Ordering::Release);
        Ok(())
    }
}

impl CpuMonitor {
    pub fn new(cpu_count: usize) -> Self {
        let mut core_utilization = Vec::with_capacity(cpu_count);
        for _ in 0..cpu_count {
            core_utilization.push(AtomicU64::new(0));
        }

        Self {
            core_utilization,
            overall_utilization: AtomicU64::new(0),
            load_averages: [AtomicU64::new(0), AtomicU64::new(0), AtomicU64::new(0)],
        }
    }
}

impl MemoryMonitor {
    pub fn new() -> Self {
        Self {
            total_memory: AtomicU64::new(0),
            used_memory: AtomicU64::new(0),
            available_memory: AtomicU64::new(0),
            memory_pressure: AtomicU64::new(0),
        }
    }
}

impl NetworkMonitor {
    pub fn new() -> Self {
        Self {
            bytes_received: AtomicU64::new(0),
            bytes_transmitted: AtomicU64::new(0),
            packets_received: AtomicU64::new(0),
            packets_transmitted: AtomicU64::new(0),
            utilization: AtomicU64::new(0),
        }
    }
}

impl DiskMonitor {
    pub fn new() -> Self {
        Self {
            bytes_read: AtomicU64::new(0),
            bytes_written: AtomicU64::new(0),
            read_operations: AtomicU64::new(0),
            write_operations: AtomicU64::new(0),
            utilization: AtomicU64::new(0),
        }
    }
}

impl Default for SystemMetrics {
    fn default() -> Self {
        Self {
            cpu: CpuMetrics {
                core_utilization: Vec::new(),
                overall_utilization: 0.0,
                load_averages: [0.0, 0.0, 0.0],
                frequencies: Vec::new(),
            },
            memory: MemoryMetrics {
                total: 0,
                used: 0,
                available: 0,
                utilization: 0.0,
                pressure: 0.0,
            },
            network: NetworkMetrics {
                throughput: 0,
                utilization: 0.0,
                packet_loss: 0.0,
                latency: Duration::from_millis(0),
            },
            disk: DiskMetrics {
                throughput: 0,
                utilization: 0.0,
                response_time: Duration::from_millis(0),
                queue_depth: 0,
            },
            timestamp: SystemTime::now(),
        }
    }
}

impl ScalingDecisionEngine {
    pub fn new() -> WasmtimeResult<Self> {
        let algorithms: Vec<Box<dyn ScalingAlgorithm + Send + Sync>> = vec![
            Box::new(ThresholdBasedAlgorithm::new()),
            Box::new(PredictiveScalingAlgorithm::new(Duration::from_secs(5 * 60), 0.8)),
        ];

        Ok(Self {
            algorithms,
            algorithm_weights: vec![0.6, 0.4],
            decision_history: Arc::new(ParkingRwLock::new(VecDeque::new())),
            metrics: Arc::new(ParkingRwLock::new(DecisionMetrics::default())),
        })
    }
}

impl ScalingExecutor {
    pub fn new(
        config: ExecutorConfig,
        _thread_pool: Arc<WorkStealingScheduler>,
        _affinity_manager: Arc<ThreadAffinityManager>,
    ) -> WasmtimeResult<Self> {
        Ok(Self {
            config,
            thread_pool: _thread_pool,
            affinity_manager: _affinity_manager,
            active_operations: Arc::new(ParkingRwLock::new(HashMap::new())),
            metrics: Arc::new(ParkingRwLock::new(ExecutorMetrics::default())),
            next_operation_id: AtomicU64::new(1),
        })
    }
}

impl PerformanceMetricsCollector {
    pub fn new(_config: CollectionConfig) -> WasmtimeResult<Self> {
        Ok(Self {
            config: _config,
            metrics_history: Arc::new(ParkingRwLock::new(VecDeque::new())),
            current_metrics: Arc::new(ParkingRwLock::new(PerformanceSnapshot::default())),
            collector_thread: None,
            shutdown: Arc::new(AtomicBool::new(false)),
        })
    }

    pub fn start(&self) -> WasmtimeResult<()> {
        // Implementation would start collection thread
        Ok(())
    }

    pub fn stop(&self) -> WasmtimeResult<()> {
        self.shutdown.store(true, Ordering::Release);
        Ok(())
    }
}

impl Default for PerformanceSnapshot {
    fn default() -> Self {
        Self {
            timestamp: SystemTime::now(),
            throughput: 0.0,
            avg_response_time: Duration::from_millis(0),
            p95_response_time: Duration::from_millis(0),
            p99_response_time: Duration::from_millis(0),
            error_rate: 0.0,
            resource_utilization: ResourceUtilization {
                cpu: 0.0,
                memory: 0.0,
                network: 0.0,
                disk: 0.0,
            },
            thread_pool_metrics: ThreadPoolMetrics {
                active_threads: 0,
                idle_threads: 0,
                queue_length: 0,
                thread_creation_rate: 0.0,
                thread_destruction_rate: 0.0,
                load_balance_efficiency: 0.0,
            },
        }
    }
}

// Feature extractor implementations
impl StatisticalFeatureExtractor {
    pub fn new(window_size: usize) -> Self {
        Self { window_size }
    }
}

impl FeatureExtractor for StatisticalFeatureExtractor {
    fn extract_features(&self, sample: &WorkloadSample) -> Vec<f64> {
        vec![
            sample.arrival_rate,
            sample.completion_rate,
            sample.avg_execution_time.as_secs_f64(),
            sample.queue_length as f64,
            sample.thread_utilization,
            sample.cpu_utilization,
            sample.memory_utilization,
            sample.load_average,
        ]
    }

    fn get_feature_names(&self) -> Vec<String> {
        vec![
            "arrival_rate".to_string(),
            "completion_rate".to_string(),
            "avg_execution_time".to_string(),
            "queue_length".to_string(),
            "thread_utilization".to_string(),
            "cpu_utilization".to_string(),
            "memory_utilization".to_string(),
            "load_average".to_string(),
        ]
    }

    fn get_feature_importance(&self) -> Vec<f64> {
        vec![0.2, 0.25, 0.15, 0.1, 0.1, 0.1, 0.05, 0.05]
    }
}

impl TimeBasedFeatureExtractor {
    pub fn new(include_cyclical: bool) -> Self {
        Self { include_cyclical }
    }
}

impl FeatureExtractor for TimeBasedFeatureExtractor {
    fn extract_features(&self, sample: &WorkloadSample) -> Vec<f64> {
        let features = &sample.time_features;
        if self.include_cyclical {
            vec![
                features.hour_of_day as f64 / 24.0,
                (features.hour_of_day as f64 * 2.0 * std::f64::consts::PI / 24.0).sin(),
                (features.hour_of_day as f64 * 2.0 * std::f64::consts::PI / 24.0).cos(),
                features.day_of_week as f64 / 7.0,
                (features.day_of_week as f64 * 2.0 * std::f64::consts::PI / 7.0).sin(),
                (features.day_of_week as f64 * 2.0 * std::f64::consts::PI / 7.0).cos(),
                if features.is_weekend { 1.0 } else { 0.0 },
                if features.is_business_hours { 1.0 } else { 0.0 },
            ]
        } else {
            vec![
                features.hour_of_day as f64,
                features.day_of_week as f64,
                if features.is_weekend { 1.0 } else { 0.0 },
                if features.is_business_hours { 1.0 } else { 0.0 },
            ]
        }
    }

    fn get_feature_names(&self) -> Vec<String> {
        if self.include_cyclical {
            vec![
                "hour_normalized".to_string(),
                "hour_sin".to_string(),
                "hour_cos".to_string(),
                "day_normalized".to_string(),
                "day_sin".to_string(),
                "day_cos".to_string(),
                "is_weekend".to_string(),
                "is_business_hours".to_string(),
            ]
        } else {
            vec![
                "hour_of_day".to_string(),
                "day_of_week".to_string(),
                "is_weekend".to_string(),
                "is_business_hours".to_string(),
            ]
        }
    }

    fn get_feature_importance(&self) -> Vec<f64> {
        if self.include_cyclical {
            vec![0.1, 0.15, 0.15, 0.1, 0.15, 0.15, 0.1, 0.1]
        } else {
            vec![0.3, 0.3, 0.2, 0.2]
        }
    }
}

impl TrendAnalysisFeatureExtractor {
    pub fn new(analysis_window: usize) -> Self {
        Self { analysis_window }
    }
}

impl FeatureExtractor for TrendAnalysisFeatureExtractor {
    fn extract_features(&self, _sample: &WorkloadSample) -> Vec<f64> {
        // Simplified trend analysis
        vec![0.0, 0.0, 0.0] // trend, volatility, momentum
    }

    fn get_feature_names(&self) -> Vec<String> {
        vec![
            "trend".to_string(),
            "volatility".to_string(),
            "momentum".to_string(),
        ]
    }

    fn get_feature_importance(&self) -> Vec<f64> {
        vec![0.4, 0.3, 0.3]
    }
}

// Scaling algorithm implementations
impl ThresholdBasedAlgorithm {
    pub fn new() -> Self {
        Self {
            config: ThresholdAlgorithmConfig {
                cpu_thresholds: (0.8, 0.2),
                memory_thresholds: (0.8, 0.2),
                queue_thresholds: (50, 5),
                response_time_thresholds: (Duration::from_millis(100), Duration::from_millis(10)),
            },
        }
    }
}

impl ScalingAlgorithm for ThresholdBasedAlgorithm {
    fn make_decision(
        &self,
        current_metrics: &SystemMetrics,
        _workload_prediction: &PredictionResult,
        current_pool_size: usize,
        config: &ScalingConfig,
    ) -> ScalingDecision {
        let cpu_util = current_metrics.cpu.overall_utilization;
        let memory_util = current_metrics.memory.utilization;

        let action = if cpu_util > self.config.cpu_thresholds.0 || memory_util > self.config.memory_thresholds.0 {
            ScalingAction::ScaleUp
        } else if cpu_util < self.config.cpu_thresholds.1 && memory_util < self.config.memory_thresholds.1 {
            ScalingAction::ScaleDown
        } else {
            ScalingAction::NoAction
        };

        let target_size = match action {
            ScalingAction::ScaleUp => (current_pool_size + config.scaling_velocity).min(config.max_pool_size),
            ScalingAction::ScaleDown => (current_pool_size.saturating_sub(config.scaling_velocity)).max(config.min_pool_size),
            _ => current_pool_size,
        };

        ScalingDecision {
            action,
            target_size,
            confidence: 0.8,
            rationale: format!("Threshold-based decision: CPU={:.2}, Memory={:.2}", cpu_util, memory_util),
            factors: [
                ("cpu_utilization".to_string(), cpu_util),
                ("memory_utilization".to_string(), memory_util),
            ].into_iter().collect(),
            timestamp: SystemTime::now(),
            algorithm: "ThresholdBased".to_string(),
        }
    }

    fn get_name(&self) -> &str {
        "ThresholdBased"
    }

    fn get_confidence(&self) -> f64 {
        0.8
    }
}

impl PredictiveScalingAlgorithm {
    pub fn new(horizon: Duration, confidence_threshold: f64) -> Self {
        Self {
            horizon,
            confidence_threshold,
        }
    }
}

impl ScalingAlgorithm for PredictiveScalingAlgorithm {
    fn make_decision(
        &self,
        _current_metrics: &SystemMetrics,
        workload_prediction: &PredictionResult,
        current_pool_size: usize,
        config: &ScalingConfig,
    ) -> ScalingDecision {
        if workload_prediction.confidence < self.confidence_threshold {
            return ScalingDecision {
                action: ScalingAction::NoAction,
                target_size: current_pool_size,
                confidence: workload_prediction.confidence,
                rationale: "Insufficient prediction confidence".to_string(),
                factors: HashMap::new(),
                timestamp: SystemTime::now(),
                algorithm: "Predictive".to_string(),
            };
        }

        let predicted_load = workload_prediction.predicted_workload.cpu_utilization;
        let action = if predicted_load > config.scale_up_threshold {
            ScalingAction::ScaleUp
        } else if predicted_load < config.scale_down_threshold {
            ScalingAction::ScaleDown
        } else {
            ScalingAction::NoAction
        };

        let target_size = match action {
            ScalingAction::ScaleUp => (current_pool_size + config.scaling_velocity).min(config.max_pool_size),
            ScalingAction::ScaleDown => (current_pool_size.saturating_sub(config.scaling_velocity)).max(config.min_pool_size),
            _ => current_pool_size,
        };

        ScalingDecision {
            action,
            target_size,
            confidence: workload_prediction.confidence,
            rationale: format!("Predictive scaling based on forecast: {:.2}", predicted_load),
            factors: [("predicted_cpu_utilization".to_string(), predicted_load)].into_iter().collect(),
            timestamp: SystemTime::now(),
            algorithm: "Predictive".to_string(),
        }
    }

    fn get_name(&self) -> &str {
        "Predictive"
    }

    fn get_confidence(&self) -> f64 {
        0.9
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::Arc;

    #[test]
    fn test_scaling_config_defaults() {
        let config = ScalingConfig::default();
        assert!(config.min_pool_size > 0);
        assert!(config.max_pool_size >= config.min_pool_size);
        assert!(config.scale_up_threshold > config.scale_down_threshold);
    }

    #[test]
    fn test_workload_predictor_creation() {
        let predictor = WorkloadPredictor::new(TrainingConfig::default())
            .expect("Failed to create workload predictor");

        assert_eq!(predictor.feature_extractors.len(), 3);
    }

    #[test]
    fn test_threshold_algorithm_decision() {
        let algorithm = ThresholdBasedAlgorithm::new();
        let mut metrics = SystemMetrics::default();
        metrics.cpu.overall_utilization = 0.9; // High CPU
        metrics.memory.utilization = 0.5; // Normal memory

        let prediction = PredictionResult {
            predicted_workload: WorkloadSample {
                timestamp: SystemTime::now(),
                arrival_rate: 100.0,
                completion_rate: 90.0,
                avg_execution_time: Duration::from_millis(50),
                queue_length: 10,
                thread_utilization: 0.8,
                cpu_utilization: 0.9,
                memory_utilization: 0.5,
                load_average: 2.0,
                time_features: TimeFeatures {
                    hour_of_day: 14,
                    day_of_week: 2,
                    day_of_month: 15,
                    week_of_year: 20,
                    month_of_year: 5,
                    is_weekend: false,
                    is_business_hours: true,
                },
            },
            confidence: 0.8,
            horizon: Duration::from_secs(5 * 60),
            predicted_at: SystemTime::now(),
            feature_contributions: HashMap::new(),
        };

        let config = ScalingConfig::default();
        let decision = algorithm.make_decision(&metrics, &prediction, 4, &config);

        assert_eq!(decision.action, ScalingAction::ScaleUp);
        assert!(decision.confidence > 0.0);
    }

    #[test]
    fn test_feature_extractor_statistical() {
        let extractor = StatisticalFeatureExtractor::new(10);

        let sample = WorkloadSample {
            timestamp: SystemTime::now(),
            arrival_rate: 100.0,
            completion_rate: 95.0,
            avg_execution_time: Duration::from_millis(50),
            queue_length: 20,
            thread_utilization: 0.8,
            cpu_utilization: 0.7,
            memory_utilization: 0.6,
            load_average: 1.5,
            time_features: TimeFeatures {
                hour_of_day: 12,
                day_of_week: 2,
                day_of_month: 15,
                week_of_year: 20,
                month_of_year: 5,
                is_weekend: false,
                is_business_hours: true,
            },
        };

        let features = extractor.extract_features(&sample);
        let feature_names = extractor.get_feature_names();
        let importance = extractor.get_feature_importance();

        assert_eq!(features.len(), 8);
        assert_eq!(feature_names.len(), 8);
        assert_eq!(importance.len(), 8);

        // Check that features match expected values
        assert_eq!(features[0], 100.0); // arrival_rate
        assert_eq!(features[1], 95.0);  // completion_rate
        assert_eq!(features[3], 20.0);  // queue_length
    }

    #[test]
    fn test_time_based_feature_extractor() {
        let extractor = TimeBasedFeatureExtractor::new(true);

        let sample = WorkloadSample {
            timestamp: SystemTime::now(),
            arrival_rate: 0.0,
            completion_rate: 0.0,
            avg_execution_time: Duration::from_millis(0),
            queue_length: 0,
            thread_utilization: 0.0,
            cpu_utilization: 0.0,
            memory_utilization: 0.0,
            load_average: 0.0,
            time_features: TimeFeatures {
                hour_of_day: 12,
                day_of_week: 1, // Monday
                day_of_month: 15,
                week_of_year: 20,
                month_of_year: 5,
                is_weekend: false,
                is_business_hours: true,
            },
        };

        let features = extractor.extract_features(&sample);
        assert_eq!(features.len(), 8); // Cyclical encoding

        // Check normalized hour
        assert_eq!(features[0], 0.5); // 12/24 = 0.5

        // Check weekend and business hours flags
        assert_eq!(features[6], 0.0); // not weekend
        assert_eq!(features[7], 1.0); // is business hours
    }

    #[test]
    fn test_system_metrics_defaults() {
        let metrics = SystemMetrics::default();
        assert_eq!(metrics.cpu.overall_utilization, 0.0);
        assert_eq!(metrics.memory.utilization, 0.0);
        assert!(metrics.timestamp <= SystemTime::now());
    }
}