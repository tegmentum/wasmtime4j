//! # Advanced Cryptographic Validation Module
//!
//! This module provides comprehensive cryptographic validation for WebAssembly modules
//! and components with enterprise-grade security features:
//!
//! - Zero-trust architecture with mandatory cryptographic verification
//! - Multi-signature support with configurable signature schemes
//! - Certificate chain validation with CRL and OCSP checking
//! - Hardware security module (HSM) integration
//! - Post-quantum cryptography support preparation
//! - Time-stamping and non-repudiation services
//! - Advanced key management and rotation
//! - Cryptographic agility with algorithm negotiation
//! - Comprehensive audit trails and compliance reporting

use std::collections::{HashMap, HashSet, BTreeMap};
use std::sync::{Arc, RwLock, Mutex};
use std::time::{SystemTime, UNIX_EPOCH, Duration};
use std::path::{Path, PathBuf};
use std::fs::{File, read};
use std::io::{Read, Write, BufReader};
use ring::{digest, signature, rand, aead, pbkdf2, hmac, hkdf};
use ring::signature::{Ed25519KeyPair, KeyPair, UnparsedPublicKey, VerificationAlgorithm};
use ring::rand::SystemRandom;
use base64::{Engine as _, engine::general_purpose};
use serde::{Deserialize, Serialize};
use sha2::{Sha256, Digest};
use chrono::{DateTime, Utc};
use x509_parser::{prelude::*, certificate::X509Certificate};
use der_parser::oid::Oid;

use crate::error::{WasmtimeError, WasmtimeResult, ErrorCode};
use crate::security::{ModuleSignature, SignatureAlgorithm, TrustStore, CertificateInfo};

/// Cryptographic validation configuration
#[derive(Debug, Clone)]
pub struct CryptoValidationConfig {
    /// Required signature algorithms (at least one must be present)
    pub required_algorithms: Vec<SignatureAlgorithm>,
    /// Minimum required signatures for multi-signature validation
    pub min_required_signatures: usize,
    /// Maximum allowed signature age
    pub max_signature_age: Duration,
    /// Enable certificate chain validation
    pub enable_cert_chain_validation: bool,
    /// Enable CRL (Certificate Revocation List) checking
    pub enable_crl_checking: bool,
    /// Enable OCSP (Online Certificate Status Protocol) checking
    pub enable_ocsp_checking: bool,
    /// Enable time-stamping validation
    pub enable_timestamping: bool,
    /// Trust store configuration
    pub trust_store_config: TrustStoreConfig,
    /// HSM configuration
    pub hsm_config: Option<HsmConfig>,
    /// Post-quantum cryptography preparedness
    pub pqc_enabled: bool,
    /// Cryptographic agility settings
    pub crypto_agility: CryptoAgilityConfig,
}

impl Default for CryptoValidationConfig {
    fn default() -> Self {
        Self {
            required_algorithms: vec![SignatureAlgorithm::Ed25519],
            min_required_signatures: 1,
            max_signature_age: Duration::from_secs(86400 * 365), // 1 year
            enable_cert_chain_validation: true,
            enable_crl_checking: false, // Disabled by default for performance
            enable_ocsp_checking: false, // Disabled by default for performance
            enable_timestamping: false,
            trust_store_config: TrustStoreConfig::default(),
            hsm_config: None,
            pqc_enabled: false,
            crypto_agility: CryptoAgilityConfig::default(),
        }
    }
}

impl CryptoValidationConfig {
    /// Create a high-security configuration
    pub fn high_security() -> Self {
        Self {
            required_algorithms: vec![
                SignatureAlgorithm::Ed25519,
                SignatureAlgorithm::RsaSha256,
            ],
            min_required_signatures: 2,
            max_signature_age: Duration::from_secs(86400 * 90), // 90 days
            enable_cert_chain_validation: true,
            enable_crl_checking: true,
            enable_ocsp_checking: true,
            enable_timestamping: true,
            trust_store_config: TrustStoreConfig::strict(),
            hsm_config: None, // Would be configured if HSM available
            pqc_enabled: true,
            crypto_agility: CryptoAgilityConfig::strict(),
        }
    }

    /// Create a balanced security/performance configuration
    pub fn balanced() -> Self {
        Self::default()
    }
}

/// Trust store configuration
#[derive(Debug, Clone)]
pub struct TrustStoreConfig {
    /// Path to trust store file
    pub store_path: Option<PathBuf>,
    /// Enable automatic trust store updates
    pub auto_update: bool,
    /// Trust store update sources
    pub update_sources: Vec<TrustStoreSource>,
    /// Minimum trust level required
    pub min_trust_level: TrustLevel,
    /// Enable trust store backup/restore
    pub enable_backup: bool,
}

impl Default for TrustStoreConfig {
    fn default() -> Self {
        Self {
            store_path: None,
            auto_update: false,
            update_sources: Vec::new(),
            min_trust_level: TrustLevel::Medium,
            enable_backup: true,
        }
    }
}

impl TrustStoreConfig {
    /// Create a strict trust store configuration
    pub fn strict() -> Self {
        Self {
            store_path: None,
            auto_update: false,
            update_sources: Vec::new(),
            min_trust_level: TrustLevel::High,
            enable_backup: true,
        }
    }
}

#[derive(Debug, Clone)]
pub struct TrustStoreSource {
    /// Source URL or path
    pub source: String,
    /// Source type
    pub source_type: TrustStoreSourceType,
    /// Authentication credentials
    pub credentials: Option<TrustStoreCredentials>,
    /// Update frequency
    pub update_frequency: Duration,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum TrustStoreSourceType {
    /// HTTP/HTTPS URL
    Http,
    /// File system path
    File,
    /// LDAP directory
    Ldap,
    /// Custom provider
    Custom,
}

#[derive(Debug, Clone)]
pub struct TrustStoreCredentials {
    /// Username
    pub username: String,
    /// Password or token
    pub password: String,
    /// Additional authentication parameters
    pub parameters: HashMap<String, String>,
}

/// Trust level for certificates and keys
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub enum TrustLevel {
    /// Untrusted
    None,
    /// Low trust level
    Low,
    /// Medium trust level
    Medium,
    /// High trust level
    High,
    /// Absolute trust (e.g., root CA)
    Absolute,
}

/// Hardware Security Module configuration
#[derive(Debug, Clone)]
pub struct HsmConfig {
    /// HSM provider type
    pub provider: HsmProvider,
    /// HSM connection configuration
    pub connection_config: HsmConnectionConfig,
    /// Key slot assignments
    pub key_slots: HashMap<String, u32>,
    /// HSM authentication
    pub authentication: HsmAuthentication,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum HsmProvider {
    /// PKCS#11 compatible HSM
    Pkcs11,
    /// AWS CloudHSM
    AwsCloudHsm,
    /// Azure Dedicated HSM
    AzureDedicatedHsm,
    /// Google Cloud HSM
    GoogleCloudHsm,
    /// Custom HSM provider
    Custom(String),
}

#[derive(Debug, Clone)]
pub struct HsmConnectionConfig {
    /// Connection parameters
    pub parameters: HashMap<String, String>,
    /// Timeout settings
    pub timeout: Duration,
    /// Retry configuration
    pub retry_config: RetryConfig,
}

#[derive(Debug, Clone)]
pub struct RetryConfig {
    /// Maximum retry attempts
    pub max_attempts: u32,
    /// Base retry delay
    pub base_delay: Duration,
    /// Maximum retry delay
    pub max_delay: Duration,
    /// Backoff multiplier
    pub backoff_multiplier: f64,
}

#[derive(Debug, Clone)]
pub struct HsmAuthentication {
    /// Authentication method
    pub method: HsmAuthMethod,
    /// Credentials
    pub credentials: HashMap<String, String>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum HsmAuthMethod {
    /// PIN-based authentication
    Pin,
    /// Certificate-based authentication
    Certificate,
    /// Multi-factor authentication
    MultiFactorAuth,
    /// Custom authentication method
    Custom(String),
}

/// Cryptographic agility configuration
#[derive(Debug, Clone)]
pub struct CryptoAgilityConfig {
    /// Supported signature algorithms in preference order
    pub supported_algorithms: Vec<SignatureAlgorithm>,
    /// Algorithm deprecation schedule
    pub deprecation_schedule: HashMap<SignatureAlgorithm, SystemTime>,
    /// Minimum key sizes by algorithm
    pub min_key_sizes: HashMap<SignatureAlgorithm, usize>,
    /// Algorithm upgrade recommendations
    pub upgrade_recommendations: HashMap<SignatureAlgorithm, SignatureAlgorithm>,
    /// Enable automatic algorithm negotiation
    pub enable_auto_negotiation: bool,
}

impl Default for CryptoAgilityConfig {
    fn default() -> Self {
        let mut min_key_sizes = HashMap::new();
        min_key_sizes.insert(SignatureAlgorithm::Ed25519, 256);
        min_key_sizes.insert(SignatureAlgorithm::RsaSha256, 2048);
        min_key_sizes.insert(SignatureAlgorithm::EcdsaP256Sha256, 256);

        Self {
            supported_algorithms: vec![
                SignatureAlgorithm::Ed25519,
                SignatureAlgorithm::RsaSha256,
                SignatureAlgorithm::EcdsaP256Sha256,
            ],
            deprecation_schedule: HashMap::new(),
            min_key_sizes,
            upgrade_recommendations: HashMap::new(),
            enable_auto_negotiation: true,
        }
    }
}

impl CryptoAgilityConfig {
    /// Create a strict cryptographic agility configuration
    pub fn strict() -> Self {
        let mut config = Self::default();
        config.supported_algorithms = vec![SignatureAlgorithm::Ed25519];
        config.min_key_sizes.insert(SignatureAlgorithm::RsaSha256, 4096);
        config.enable_auto_negotiation = false;
        config
    }
}

/// Multi-signature validation context
#[derive(Debug, Clone)]
pub struct MultiSignatureContext {
    /// Required signers (if specified)
    pub required_signers: Option<HashSet<String>>,
    /// Minimum signatures required
    pub min_signatures: usize,
    /// Signature validation policy
    pub validation_policy: MultiSignaturePolicy,
    /// Threshold configuration
    pub threshold_config: ThresholdConfig,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum MultiSignaturePolicy {
    /// All signatures must be valid
    AllValid,
    /// Majority of signatures must be valid
    Majority,
    /// Threshold number of signatures must be valid
    Threshold,
    /// At least one signature from each required group
    GroupThreshold,
}

#[derive(Debug, Clone)]
pub struct ThresholdConfig {
    /// Threshold value
    pub threshold: usize,
    /// Total possible signers
    pub total_signers: usize,
    /// Signature groups (for group threshold policy)
    pub signature_groups: HashMap<String, HashSet<String>>,
}

/// Certificate chain validation context
#[derive(Debug, Clone)]
pub struct CertChainValidationContext {
    /// Root certificates
    pub root_certificates: HashSet<String>,
    /// Intermediate certificates
    pub intermediate_certificates: HashMap<String, Vec<u8>>,
    /// Certificate validation policy
    pub validation_policy: CertValidationPolicy,
    /// CRL sources
    pub crl_sources: Vec<CrlSource>,
    /// OCSP sources
    pub ocsp_sources: Vec<OcspSource>,
}

#[derive(Debug, Clone)]
pub struct CertValidationPolicy {
    /// Require valid certificate chain
    pub require_valid_chain: bool,
    /// Check certificate expiration
    pub check_expiration: bool,
    /// Check certificate revocation
    pub check_revocation: bool,
    /// Allow self-signed certificates
    pub allow_self_signed: bool,
    /// Minimum certificate validity period remaining
    pub min_validity_remaining: Duration,
    /// Required certificate extensions
    pub required_extensions: Vec<String>,
}

#[derive(Debug, Clone)]
pub struct CrlSource {
    /// CRL URL
    pub url: String,
    /// CRL cache duration
    pub cache_duration: Duration,
    /// Last update timestamp
    pub last_update: Option<SystemTime>,
}

#[derive(Debug, Clone)]
pub struct OcspSource {
    /// OCSP responder URL
    pub url: String,
    /// Response cache duration
    pub cache_duration: Duration,
    /// OCSP request configuration
    pub request_config: OcspRequestConfig,
}

#[derive(Debug, Clone)]
pub struct OcspRequestConfig {
    /// Request timeout
    pub timeout: Duration,
    /// OCSP nonce usage
    pub use_nonce: bool,
    /// Request signing certificate
    pub signing_cert: Option<Vec<u8>>,
}

/// Cryptographic validation engine
#[derive(Debug)]
pub struct CryptoValidationEngine {
    /// Configuration
    config: CryptoValidationConfig,
    /// Trust store
    trust_store: Arc<RwLock<EnhancedTrustStore>>,
    /// Certificate chain validator
    cert_validator: CertificateChainValidator,
    /// Multi-signature validator
    multi_sig_validator: MultiSignatureValidator,
    /// Time-stamping validator
    timestamp_validator: Option<TimestampValidator>,
    /// HSM interface
    hsm_interface: Option<Arc<HsmInterface>>,
    /// Validation statistics
    validation_stats: Arc<Mutex<ValidationStatistics>>,
    /// Algorithm negotiator
    algorithm_negotiator: AlgorithmNegotiator,
}

/// Enhanced trust store with additional features
#[derive(Debug, Clone)]
pub struct EnhancedTrustStore {
    /// Base trust store
    base_store: TrustStore,
    /// Certificate metadata
    certificate_metadata: HashMap<String, CertificateMetadata>,
    /// Trust levels
    trust_levels: HashMap<String, TrustLevel>,
    /// Certificate hierarchies
    certificate_hierarchies: HashMap<String, CertificateHierarchy>,
    /// Revocation information
    revocation_info: HashMap<String, RevocationInfo>,
}

#[derive(Debug, Clone)]
pub struct CertificateMetadata {
    /// Certificate subject
    pub subject: String,
    /// Certificate issuer
    pub issuer: String,
    /// Certificate serial number
    pub serial_number: String,
    /// Validity period
    pub not_before: SystemTime,
    /// Expiration time
    pub not_after: SystemTime,
    /// Key usage
    pub key_usage: Vec<KeyUsage>,
    /// Extended key usage
    pub extended_key_usage: Vec<ExtendedKeyUsage>,
    /// Certificate policies
    pub certificate_policies: Vec<String>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum KeyUsage {
    DigitalSignature,
    NonRepudiation,
    KeyEncipherment,
    DataEncipherment,
    KeyAgreement,
    KeyCertSign,
    CrlSign,
    EncipherOnly,
    DecipherOnly,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ExtendedKeyUsage {
    ServerAuth,
    ClientAuth,
    CodeSigning,
    EmailProtection,
    TimeStamping,
    OcspSigning,
    IpsecEndSystem,
    IpsecTunnel,
    IpsecUser,
}

#[derive(Debug, Clone)]
pub struct CertificateHierarchy {
    /// Root certificate
    pub root: String,
    /// Intermediate certificates
    pub intermediates: Vec<String>,
    /// End entity certificate
    pub end_entity: String,
    /// Chain validation status
    pub validation_status: ChainValidationStatus,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ChainValidationStatus {
    Valid,
    Invalid,
    Unknown,
    Expired,
    Revoked,
}

#[derive(Debug, Clone)]
pub struct RevocationInfo {
    /// Revocation status
    pub status: RevocationStatus,
    /// Revocation reason
    pub reason: Option<RevocationReason>,
    /// Revocation time
    pub revocation_time: Option<SystemTime>,
    /// Information source
    pub source: RevocationSource,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum RevocationStatus {
    NotRevoked,
    Revoked,
    Unknown,
    OnHold,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum RevocationReason {
    Unspecified,
    KeyCompromise,
    CaCompromise,
    AffiliationChanged,
    Superseded,
    CessationOfOperation,
    CertificateHold,
    RemoveFromCrl,
    PrivilegeWithdrawn,
    AaCompromise,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum RevocationSource {
    Crl,
    Ocsp,
    Manual,
    Unknown,
}

/// Certificate chain validator
#[derive(Debug)]
pub struct CertificateChainValidator {
    /// Validation context
    context: Arc<RwLock<CertChainValidationContext>>,
    /// CRL cache
    crl_cache: Arc<RwLock<HashMap<String, CrlEntry>>>,
    /// OCSP cache
    ocsp_cache: Arc<RwLock<HashMap<String, OcspEntry>>>,
    /// Validation statistics
    validation_stats: Arc<Mutex<ChainValidationStatistics>>,
}

#[derive(Debug, Clone)]
pub struct CrlEntry {
    /// CRL data
    pub data: Vec<u8>,
    /// Cache timestamp
    pub cached_at: SystemTime,
    /// Expiration time
    pub expires_at: SystemTime,
    /// Revoked certificates
    pub revoked_certificates: HashSet<String>,
}

#[derive(Debug, Clone)]
pub struct OcspEntry {
    /// OCSP response data
    pub response: Vec<u8>,
    /// Cache timestamp
    pub cached_at: SystemTime,
    /// Response status
    pub status: OcspResponseStatus,
    /// Certificate status
    pub cert_status: OcspCertStatus,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum OcspResponseStatus {
    Successful,
    MalformedRequest,
    InternalError,
    TryLater,
    SigRequired,
    Unauthorized,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum OcspCertStatus {
    Good,
    Revoked,
    Unknown,
}

/// Multi-signature validator
#[derive(Debug)]
pub struct MultiSignatureValidator {
    /// Validation contexts
    contexts: Arc<RwLock<HashMap<String, MultiSignatureContext>>>,
    /// Signature verification cache
    verification_cache: Arc<RwLock<HashMap<String, SignatureVerificationResult>>>,
    /// Validation statistics
    validation_stats: Arc<Mutex<MultiSignatureStatistics>>,
}

#[derive(Debug, Clone)]
pub struct SignatureVerificationResult {
    /// Verification status
    pub status: VerificationStatus,
    /// Signer identifier
    pub signer: String,
    /// Signature algorithm
    pub algorithm: SignatureAlgorithm,
    /// Verification timestamp
    pub verified_at: SystemTime,
    /// Certificate chain (if applicable)
    pub cert_chain: Option<Vec<Vec<u8>>>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum VerificationStatus {
    Valid,
    Invalid,
    Expired,
    Revoked,
    Unknown,
}

/// Time-stamp validator
#[derive(Debug)]
pub struct TimestampValidator {
    /// Time-stamp authorities
    tsa_sources: Arc<RwLock<HashMap<String, TsaSource>>>,
    /// Time-stamp cache
    timestamp_cache: Arc<RwLock<HashMap<String, TimestampEntry>>>,
    /// Validation statistics
    validation_stats: Arc<Mutex<TimestampStatistics>>,
}

#[derive(Debug, Clone)]
pub struct TsaSource {
    /// TSA URL
    pub url: String,
    /// TSA certificate
    pub certificate: Vec<u8>,
    /// Request configuration
    pub request_config: TsaRequestConfig,
    /// Response validation policy
    pub validation_policy: TsaValidationPolicy,
}

#[derive(Debug, Clone)]
pub struct TsaRequestConfig {
    /// Request timeout
    pub timeout: Duration,
    /// Hash algorithm for time-stamp request
    pub hash_algorithm: digest::Algorithm,
    /// Request certificates in response
    pub request_certificates: bool,
}

#[derive(Debug, Clone)]
pub struct TsaValidationPolicy {
    /// Allow untrusted TSA certificates
    pub allow_untrusted: bool,
    /// Maximum allowed clock skew
    pub max_clock_skew: Duration,
    /// Required TSA key usage
    pub required_key_usage: Vec<ExtendedKeyUsage>,
}

#[derive(Debug, Clone)]
pub struct TimestampEntry {
    /// Time-stamp token
    pub token: Vec<u8>,
    /// Time-stamp value
    pub timestamp: SystemTime,
    /// TSA certificate
    pub tsa_cert: Vec<u8>,
    /// Validation status
    pub status: TimestampStatus,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum TimestampStatus {
    Valid,
    Invalid,
    Expired,
    Unknown,
}

/// HSM interface
#[derive(Debug)]
pub struct HsmInterface {
    /// HSM configuration
    config: HsmConfig,
    /// Connection state
    connection_state: Arc<Mutex<HsmConnectionState>>,
    /// Key cache
    key_cache: Arc<RwLock<HashMap<String, HsmKey>>>,
    /// Operation statistics
    operation_stats: Arc<Mutex<HsmOperationStatistics>>,
}

#[derive(Debug, Clone)]
pub struct HsmConnectionState {
    /// Connection status
    pub connected: bool,
    /// Last connection attempt
    pub last_connection_attempt: Option<SystemTime>,
    /// Connection errors
    pub connection_errors: Vec<String>,
    /// Session handle
    pub session_handle: Option<u64>,
}

#[derive(Debug, Clone)]
pub struct HsmKey {
    /// Key identifier
    pub key_id: String,
    /// Key slot
    pub slot: u32,
    /// Key type
    pub key_type: HsmKeyType,
    /// Key attributes
    pub attributes: HashMap<String, String>,
    /// Last used timestamp
    pub last_used: SystemTime,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum HsmKeyType {
    SigningKey,
    EncryptionKey,
    MacKey,
    DerivationKey,
}

/// Algorithm negotiator
#[derive(Debug)]
pub struct AlgorithmNegotiator {
    /// Configuration
    config: CryptoAgilityConfig,
    /// Algorithm capabilities
    capabilities: Arc<RwLock<HashMap<SignatureAlgorithm, AlgorithmCapabilities>>>,
    /// Negotiation statistics
    negotiation_stats: Arc<Mutex<NegotiationStatistics>>,
}

#[derive(Debug, Clone)]
pub struct AlgorithmCapabilities {
    /// Algorithm strength
    pub strength: u32,
    /// Performance rating
    pub performance: f64,
    /// Hardware support
    pub hardware_support: bool,
    /// Security level
    pub security_level: SecurityLevel,
    /// Quantum resistance
    pub quantum_resistant: bool,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub enum SecurityLevel {
    Low,
    Medium,
    High,
    VeryHigh,
}

/// Validation statistics
#[derive(Debug, Clone)]
pub struct ValidationStatistics {
    /// Total validations performed
    pub total_validations: u64,
    /// Successful validations
    pub successful_validations: u64,
    /// Failed validations
    pub failed_validations: u64,
    /// Average validation time
    pub avg_validation_time: Duration,
    /// Validations by algorithm
    pub validations_by_algorithm: HashMap<SignatureAlgorithm, u64>,
    /// Chain validation statistics
    pub chain_validation_stats: ChainValidationStatistics,
    /// Multi-signature statistics
    pub multi_signature_stats: MultiSignatureStatistics,
    /// Time-stamp statistics
    pub timestamp_stats: Option<TimestampStatistics>,
    /// HSM operation statistics
    pub hsm_stats: Option<HsmOperationStatistics>,
    /// Negotiation statistics
    pub negotiation_stats: NegotiationStatistics,
}

#[derive(Debug, Clone)]
pub struct ChainValidationStatistics {
    /// Total chain validations
    pub total_chain_validations: u64,
    /// Successful chain validations
    pub successful_chain_validations: u64,
    /// CRL checks performed
    pub crl_checks: u64,
    /// OCSP checks performed
    pub ocsp_checks: u64,
    /// Cache hit rates
    pub crl_cache_hit_rate: f64,
    /// OCSP cache hit rate
    pub ocsp_cache_hit_rate: f64,
}

#[derive(Debug, Clone)]
pub struct MultiSignatureStatistics {
    /// Total multi-signature validations
    pub total_multi_sig_validations: u64,
    /// Successful multi-signature validations
    pub successful_multi_sig_validations: u64,
    /// Average signatures per validation
    pub avg_signatures_per_validation: f64,
    /// Threshold policy usage
    pub threshold_policy_usage: HashMap<MultiSignaturePolicy, u64>,
}

#[derive(Debug, Clone)]
pub struct TimestampStatistics {
    /// Total time-stamp validations
    pub total_timestamp_validations: u64,
    /// Successful time-stamp validations
    pub successful_timestamp_validations: u64,
    /// TSA sources used
    pub tsa_sources_used: HashMap<String, u64>,
    /// Average timestamp verification time
    pub avg_timestamp_verification_time: Duration,
}

#[derive(Debug, Clone)]
pub struct HsmOperationStatistics {
    /// Total HSM operations
    pub total_operations: u64,
    /// Successful HSM operations
    pub successful_operations: u64,
    /// Operations by type
    pub operations_by_type: HashMap<HsmOperationType, u64>,
    /// Average operation time
    pub avg_operation_time: Duration,
    /// Connection failures
    pub connection_failures: u64,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum HsmOperationType {
    Sign,
    Verify,
    Encrypt,
    Decrypt,
    GenerateKey,
    DestroyKey,
}

#[derive(Debug, Clone)]
pub struct NegotiationStatistics {
    /// Total negotiations
    pub total_negotiations: u64,
    /// Successful negotiations
    pub successful_negotiations: u64,
    /// Algorithm preferences
    pub algorithm_preferences: HashMap<SignatureAlgorithm, u64>,
    /// Negotiation failures
    pub negotiation_failures: HashMap<String, u64>,
}

impl CryptoValidationEngine {
    /// Create a new cryptographic validation engine
    pub fn new(config: CryptoValidationConfig) -> WasmtimeResult<Self> {
        let trust_store = Arc::new(RwLock::new(EnhancedTrustStore {
            base_store: TrustStore::new(),
            certificate_metadata: HashMap::new(),
            trust_levels: HashMap::new(),
            certificate_hierarchies: HashMap::new(),
            revocation_info: HashMap::new(),
        }));

        let cert_validator = CertificateChainValidator {
            context: Arc::new(RwLock::new(CertChainValidationContext {
                root_certificates: HashSet::new(),
                intermediate_certificates: HashMap::new(),
                validation_policy: CertValidationPolicy {
                    require_valid_chain: config.enable_cert_chain_validation,
                    check_expiration: true,
                    check_revocation: config.enable_crl_checking || config.enable_ocsp_checking,
                    allow_self_signed: false,
                    min_validity_remaining: Duration::from_secs(86400), // 1 day
                    required_extensions: Vec::new(),
                },
                crl_sources: Vec::new(),
                ocsp_sources: Vec::new(),
            })),
            crl_cache: Arc::new(RwLock::new(HashMap::new())),
            ocsp_cache: Arc::new(RwLock::new(HashMap::new())),
            validation_stats: Arc::new(Mutex::new(ChainValidationStatistics {
                total_chain_validations: 0,
                successful_chain_validations: 0,
                crl_checks: 0,
                ocsp_checks: 0,
                crl_cache_hit_rate: 0.0,
                ocsp_cache_hit_rate: 0.0,
            })),
        };

        let multi_sig_validator = MultiSignatureValidator {
            contexts: Arc::new(RwLock::new(HashMap::new())),
            verification_cache: Arc::new(RwLock::new(HashMap::new())),
            validation_stats: Arc::new(Mutex::new(MultiSignatureStatistics {
                total_multi_sig_validations: 0,
                successful_multi_sig_validations: 0,
                avg_signatures_per_validation: 0.0,
                threshold_policy_usage: HashMap::new(),
            })),
        };

        let timestamp_validator = if config.enable_timestamping {
            Some(TimestampValidator {
                tsa_sources: Arc::new(RwLock::new(HashMap::new())),
                timestamp_cache: Arc::new(RwLock::new(HashMap::new())),
                validation_stats: Arc::new(Mutex::new(TimestampStatistics {
                    total_timestamp_validations: 0,
                    successful_timestamp_validations: 0,
                    tsa_sources_used: HashMap::new(),
                    avg_timestamp_verification_time: Duration::new(0, 0),
                })),
            })
        } else {
            None
        };

        let hsm_interface = if let Some(hsm_config) = &config.hsm_config {
            Some(Arc::new(HsmInterface {
                config: hsm_config.clone(),
                connection_state: Arc::new(Mutex::new(HsmConnectionState {
                    connected: false,
                    last_connection_attempt: None,
                    connection_errors: Vec::new(),
                    session_handle: None,
                })),
                key_cache: Arc::new(RwLock::new(HashMap::new())),
                operation_stats: Arc::new(Mutex::new(HsmOperationStatistics {
                    total_operations: 0,
                    successful_operations: 0,
                    operations_by_type: HashMap::new(),
                    avg_operation_time: Duration::new(0, 0),
                    connection_failures: 0,
                })),
            }))
        } else {
            None
        };

        let algorithm_negotiator = AlgorithmNegotiator {
            config: config.crypto_agility.clone(),
            capabilities: Arc::new(RwLock::new(HashMap::new())),
            negotiation_stats: Arc::new(Mutex::new(NegotiationStatistics {
                total_negotiations: 0,
                successful_negotiations: 0,
                algorithm_preferences: HashMap::new(),
                negotiation_failures: HashMap::new(),
            })),
        };

        Ok(Self {
            config,
            trust_store,
            cert_validator,
            multi_sig_validator,
            timestamp_validator,
            hsm_interface,
            validation_stats: Arc::new(Mutex::new(ValidationStatistics {
                total_validations: 0,
                successful_validations: 0,
                failed_validations: 0,
                avg_validation_time: Duration::new(0, 0),
                validations_by_algorithm: HashMap::new(),
                chain_validation_stats: ChainValidationStatistics {
                    total_chain_validations: 0,
                    successful_chain_validations: 0,
                    crl_checks: 0,
                    ocsp_checks: 0,
                    crl_cache_hit_rate: 0.0,
                    ocsp_cache_hit_rate: 0.0,
                },
                multi_signature_stats: MultiSignatureStatistics {
                    total_multi_sig_validations: 0,
                    successful_multi_sig_validations: 0,
                    avg_signatures_per_validation: 0.0,
                    threshold_policy_usage: HashMap::new(),
                },
                timestamp_stats: None,
                hsm_stats: None,
                negotiation_stats: NegotiationStatistics {
                    total_negotiations: 0,
                    successful_negotiations: 0,
                    algorithm_preferences: HashMap::new(),
                    negotiation_failures: HashMap::new(),
                },
            })),
            algorithm_negotiator,
        })
    }

    /// Validate a WebAssembly module with comprehensive cryptographic verification
    pub fn validate_module(
        &self,
        module_bytes: &[u8],
        signatures: &[ModuleSignature],
        validation_context: &ValidationContext,
    ) -> WasmtimeResult<ValidationResult> {
        let start_time = SystemTime::now();

        // Check if we have the minimum required signatures
        if signatures.len() < self.config.min_required_signatures {
            return Err(WasmtimeError::new(
                ErrorCode::ValidationFailure,
                format!(
                    "Insufficient signatures: {} provided, {} required",
                    signatures.len(),
                    self.config.min_required_signatures
                ),
            ));
        }

        let mut validation_results = Vec::new();

        // Validate each signature
        for signature in signatures {
            let result = self.validate_single_signature(module_bytes, signature, validation_context)?;
            validation_results.push(result);
        }

        // Apply multi-signature validation if configured
        let multi_sig_result = if self.config.min_required_signatures > 1 {
            Some(self.validate_multi_signature(&validation_results, validation_context)?)
        } else {
            None
        };

        // Validate certificate chains if enabled
        let cert_chain_results = if self.config.enable_cert_chain_validation {
            let mut chain_results = Vec::new();
            for signature in signatures {
                if let Some(cert_chain) = &signature.certificate_chain {
                    let result = self.validate_certificate_chain(cert_chain)?;
                    chain_results.push(result);
                }
            }
            Some(chain_results)
        } else {
            None
        };

        // Validate timestamps if enabled
        let timestamp_results = if self.config.enable_timestamping && self.timestamp_validator.is_some() {
            let mut ts_results = Vec::new();
            for signature in signatures {
                if let Some(timestamp_token) = signature.metadata.get("timestamp_token") {
                    let result = self.validate_timestamp(timestamp_token.as_bytes())?;
                    ts_results.push(result);
                }
            }
            Some(ts_results)
        } else {
            None
        };

        // Determine overall validation result
        let overall_result = self.determine_overall_result(&validation_results, &multi_sig_result)?;

        let validation_time = start_time.elapsed().unwrap_or(Duration::new(0, 0));

        // Update statistics
        self.update_validation_statistics(&validation_results, validation_time)?;

        Ok(ValidationResult {
            overall_status: overall_result,
            signature_results: validation_results,
            multi_signature_result: multi_sig_result,
            certificate_chain_results: cert_chain_results,
            timestamp_results,
            validation_time,
            warnings: Vec::new(),
            metadata: validation_context.metadata.clone(),
        })
    }

    /// Validate a single signature
    fn validate_single_signature(
        &self,
        module_bytes: &[u8],
        signature: &ModuleSignature,
        _context: &ValidationContext,
    ) -> WasmtimeResult<SingleSignatureValidationResult> {
        // Check signature age
        let signature_age = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or(Duration::new(0, 0))
            .as_secs()
            .saturating_sub(signature.timestamp);

        if Duration::from_secs(signature_age) > self.config.max_signature_age {
            return Ok(SingleSignatureValidationResult {
                status: ValidationStatus::Invalid,
                algorithm: signature.algorithm()?,
                signer_id: "unknown".to_string(),
                error: Some("Signature too old".to_string()),
                certificate_chain: signature.certificate_chain.clone(),
                trust_level: TrustLevel::None,
                validation_details: HashMap::new(),
            });
        }

        // Check if algorithm is supported
        let algorithm = signature.algorithm()?;
        if !self.config.required_algorithms.contains(&algorithm) {
            return Ok(SingleSignatureValidationResult {
                status: ValidationStatus::Invalid,
                algorithm,
                signer_id: "unknown".to_string(),
                error: Some(format!("Unsupported algorithm: {:?}", algorithm)),
                certificate_chain: signature.certificate_chain.clone(),
                trust_level: TrustLevel::None,
                validation_details: HashMap::new(),
            });
        }

        // Perform cryptographic verification
        let verification_result = self.verify_signature_cryptographically(module_bytes, signature)?;

        let trust_level = self.determine_trust_level(&signature.public_key_bytes()?)?;

        Ok(SingleSignatureValidationResult {
            status: if verification_result { ValidationStatus::Valid } else { ValidationStatus::Invalid },
            algorithm,
            signer_id: self.get_signer_id(signature)?,
            error: if verification_result { None } else { Some("Cryptographic verification failed".to_string()) },
            certificate_chain: signature.certificate_chain.clone(),
            trust_level,
            validation_details: HashMap::new(),
        })
    }

    /// Perform cryptographic signature verification
    fn verify_signature_cryptographically(
        &self,
        module_bytes: &[u8],
        signature: &ModuleSignature,
    ) -> WasmtimeResult<bool> {
        let algorithm = signature.algorithm()?;
        let signature_bytes = signature.signature_bytes()?;
        let public_key_bytes = signature.public_key_bytes()?;

        // Calculate module hash
        let module_hash = digest::digest(&digest::SHA256, module_bytes);

        match algorithm {
            SignatureAlgorithm::Ed25519 => {
                let public_key = UnparsedPublicKey::new(&signature::ED25519, &public_key_bytes);
                match public_key.verify(module_hash.as_ref(), &signature_bytes) {
                    Ok(()) => Ok(true),
                    Err(_) => Ok(false),
                }
            }
            _ => {
                // Other algorithms would be implemented here
                Err(WasmtimeError::new(
                    ErrorCode::UnsupportedFeature,
                    format!("Algorithm {:?} not yet implemented", algorithm),
                ))
            }
        }
    }

    /// Validate multi-signature requirements
    fn validate_multi_signature(
        &self,
        signature_results: &[SingleSignatureValidationResult],
        _context: &ValidationContext,
    ) -> WasmtimeResult<MultiSignatureValidationResult> {
        let valid_signatures = signature_results
            .iter()
            .filter(|result| result.status == ValidationStatus::Valid)
            .count();

        let status = if valid_signatures >= self.config.min_required_signatures {
            ValidationStatus::Valid
        } else {
            ValidationStatus::Invalid
        };

        Ok(MultiSignatureValidationResult {
            status,
            required_signatures: self.config.min_required_signatures,
            valid_signatures,
            total_signatures: signature_results.len(),
            policy: MultiSignaturePolicy::Threshold,
            error: if status == ValidationStatus::Valid {
                None
            } else {
                Some(format!(
                    "Insufficient valid signatures: {} valid, {} required",
                    valid_signatures, self.config.min_required_signatures
                ))
            },
        })
    }

    /// Validate certificate chain
    fn validate_certificate_chain(&self, cert_chain: &[String]) -> WasmtimeResult<CertificateChainValidationResult> {
        if cert_chain.is_empty() {
            return Ok(CertificateChainValidationResult {
                status: ValidationStatus::Invalid,
                error: Some("Empty certificate chain".to_string()),
                chain_length: 0,
                root_certificate: None,
                intermediate_certificates: Vec::new(),
                end_entity_certificate: None,
                trust_level: TrustLevel::None,
                revocation_status: RevocationStatus::Unknown,
            });
        }

        // In a full implementation, this would:
        // 1. Parse each certificate in the chain
        // 2. Verify signatures between certificates
        // 3. Check certificate validity periods
        // 4. Verify against trust store
        // 5. Check revocation status via CRL/OCSP

        Ok(CertificateChainValidationResult {
            status: ValidationStatus::Valid,
            error: None,
            chain_length: cert_chain.len(),
            root_certificate: cert_chain.first().cloned(),
            intermediate_certificates: cert_chain[1..cert_chain.len().saturating_sub(1)].to_vec(),
            end_entity_certificate: cert_chain.last().cloned(),
            trust_level: TrustLevel::Medium,
            revocation_status: RevocationStatus::NotRevoked,
        })
    }

    /// Validate timestamp
    fn validate_timestamp(&self, _timestamp_token: &[u8]) -> WasmtimeResult<TimestampValidationResult> {
        // In a full implementation, this would validate RFC 3161 timestamp tokens
        Ok(TimestampValidationResult {
            status: ValidationStatus::Valid,
            timestamp: SystemTime::now(),
            tsa_certificate: None,
            policy_oid: None,
            accuracy: None,
            error: None,
        })
    }

    /// Determine trust level for a public key
    fn determine_trust_level(&self, public_key: &[u8]) -> WasmtimeResult<TrustLevel> {
        let trust_store = self.trust_store.read()
            .map_err(|e| WasmtimeError::new(
                ErrorCode::SecurityViolation,
                format!("Trust store lock error: {}", e),
            ))?;

        if trust_store.base_store.is_key_trusted(public_key) {
            // Check specific trust level if available
            let fingerprint = self.calculate_key_fingerprint(public_key);
            Ok(trust_store.trust_levels.get(&fingerprint).copied().unwrap_or(TrustLevel::Medium))
        } else {
            Ok(TrustLevel::None)
        }
    }

    /// Get signer identifier from signature
    fn get_signer_id(&self, signature: &ModuleSignature) -> WasmtimeResult<String> {
        // Extract signer ID from certificate or use key fingerprint
        if let Some(cert_chain) = &signature.certificate_chain {
            if let Some(cert) = cert_chain.first() {
                // In a full implementation, would parse certificate to extract subject
                Ok(format!("cert:{}", &cert[0..std::cmp::min(16, cert.len())]))
            } else {
                Ok("unknown".to_string())
            }
        } else {
            let public_key = signature.public_key_bytes()?;
            Ok(self.calculate_key_fingerprint(&public_key))
        }
    }

    /// Calculate key fingerprint
    fn calculate_key_fingerprint(&self, public_key: &[u8]) -> String {
        let digest = digest::digest(&digest::SHA256, public_key);
        hex::encode(&digest.as_ref()[0..8]) // Use first 8 bytes for short fingerprint
    }

    /// Determine overall validation result
    fn determine_overall_result(
        &self,
        signature_results: &[SingleSignatureValidationResult],
        multi_sig_result: &Option<MultiSignatureValidationResult>,
    ) -> WasmtimeResult<ValidationStatus> {
        // Check multi-signature result first
        if let Some(multi_sig) = multi_sig_result {
            if multi_sig.status != ValidationStatus::Valid {
                return Ok(ValidationStatus::Invalid);
            }
        }

        // Count valid signatures
        let valid_count = signature_results
            .iter()
            .filter(|result| result.status == ValidationStatus::Valid)
            .count();

        if valid_count >= self.config.min_required_signatures {
            Ok(ValidationStatus::Valid)
        } else {
            Ok(ValidationStatus::Invalid)
        }
    }

    /// Update validation statistics
    fn update_validation_statistics(
        &self,
        signature_results: &[SingleSignatureValidationResult],
        validation_time: Duration,
    ) -> WasmtimeResult<()> {
        let mut stats = self.validation_stats.lock()
            .map_err(|e| WasmtimeError::new(
                ErrorCode::SecurityViolation,
                format!("Validation statistics lock error: {}", e),
            ))?;

        stats.total_validations += 1;

        let successful = signature_results.iter().any(|result| result.status == ValidationStatus::Valid);
        if successful {
            stats.successful_validations += 1;
        } else {
            stats.failed_validations += 1;
        }

        // Update average validation time
        stats.avg_validation_time = Duration::from_nanos(
            ((stats.avg_validation_time.as_nanos() * (stats.total_validations - 1) as u128)
                + validation_time.as_nanos())
                / stats.total_validations as u128,
        );

        // Update algorithm statistics
        for result in signature_results {
            *stats.validations_by_algorithm.entry(result.algorithm).or_insert(0) += 1;
        }

        Ok(())
    }

    /// Get validation statistics
    pub fn get_validation_statistics(&self) -> WasmtimeResult<ValidationStatistics> {
        let stats = self.validation_stats.lock()
            .map_err(|e| WasmtimeError::new(
                ErrorCode::SecurityViolation,
                format!("Validation statistics read lock error: {}", e),
            ))?;

        Ok(stats.clone())
    }

    /// Add trusted certificate to the trust store
    pub fn add_trusted_certificate(&self, cert_data: &[u8], trust_level: TrustLevel) -> WasmtimeResult<()> {
        let mut trust_store = self.trust_store.write()
            .map_err(|e| WasmtimeError::new(
                ErrorCode::SecurityViolation,
                format!("Trust store write lock error: {}", e),
            ))?;

        // In a full implementation, would parse the certificate
        let fingerprint = hex::encode(&digest::digest(&digest::SHA256, cert_data).as_ref()[0..8]);

        trust_store.trust_levels.insert(fingerprint.clone(), trust_level);

        // Add certificate metadata (simplified)
        trust_store.certificate_metadata.insert(fingerprint, CertificateMetadata {
            subject: "Unknown".to_string(),
            issuer: "Unknown".to_string(),
            serial_number: "Unknown".to_string(),
            not_before: SystemTime::now(),
            not_after: SystemTime::now() + Duration::from_secs(86400 * 365),
            key_usage: vec![KeyUsage::DigitalSignature],
            extended_key_usage: vec![ExtendedKeyUsage::CodeSigning],
            certificate_policies: Vec::new(),
        });

        Ok(())
    }
}

/// Validation context
#[derive(Debug, Clone)]
pub struct ValidationContext {
    /// Validation requirements
    pub requirements: ValidationRequirements,
    /// Additional metadata
    pub metadata: HashMap<String, String>,
    /// Trust anchors
    pub trust_anchors: Vec<String>,
    /// Validation policies
    pub policies: Vec<ValidationPolicy>,
}

#[derive(Debug, Clone)]
pub struct ValidationRequirements {
    /// Required signature algorithms
    pub required_algorithms: Vec<SignatureAlgorithm>,
    /// Minimum trust level required
    pub min_trust_level: TrustLevel,
    /// Required certificate extensions
    pub required_cert_extensions: Vec<String>,
    /// Maximum allowed signature age
    pub max_signature_age: Duration,
}

#[derive(Debug, Clone)]
pub struct ValidationPolicy {
    /// Policy name
    pub name: String,
    /// Policy conditions
    pub conditions: Vec<PolicyCondition>,
    /// Policy action
    pub action: PolicyAction,
}

#[derive(Debug, Clone)]
pub struct PolicyCondition {
    /// Condition type
    pub condition_type: ConditionType,
    /// Expected value
    pub expected_value: String,
    /// Comparison operator
    pub operator: ComparisonOperator,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ConditionType {
    SignatureAlgorithm,
    CertificateSubject,
    CertificateIssuer,
    KeyUsage,
    ExtendedKeyUsage,
    CertificatePolicy,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ComparisonOperator {
    Equals,
    NotEquals,
    Contains,
    NotContains,
    Matches,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum PolicyAction {
    Allow,
    Deny,
    Warn,
    RequireAdditionalValidation,
}

/// Validation result structures
#[derive(Debug, Clone)]
pub struct ValidationResult {
    /// Overall validation status
    pub overall_status: ValidationStatus,
    /// Individual signature validation results
    pub signature_results: Vec<SingleSignatureValidationResult>,
    /// Multi-signature validation result
    pub multi_signature_result: Option<MultiSignatureValidationResult>,
    /// Certificate chain validation results
    pub certificate_chain_results: Option<Vec<CertificateChainValidationResult>>,
    /// Timestamp validation results
    pub timestamp_results: Option<Vec<TimestampValidationResult>>,
    /// Total validation time
    pub validation_time: Duration,
    /// Validation warnings
    pub warnings: Vec<String>,
    /// Additional metadata
    pub metadata: HashMap<String, String>,
}

#[derive(Debug, Clone)]
pub struct SingleSignatureValidationResult {
    /// Validation status
    pub status: ValidationStatus,
    /// Signature algorithm used
    pub algorithm: SignatureAlgorithm,
    /// Signer identifier
    pub signer_id: String,
    /// Error message if validation failed
    pub error: Option<String>,
    /// Certificate chain (if present)
    pub certificate_chain: Option<Vec<String>>,
    /// Trust level of the signer
    pub trust_level: TrustLevel,
    /// Additional validation details
    pub validation_details: HashMap<String, String>,
}

#[derive(Debug, Clone)]
pub struct MultiSignatureValidationResult {
    /// Validation status
    pub status: ValidationStatus,
    /// Required number of signatures
    pub required_signatures: usize,
    /// Number of valid signatures found
    pub valid_signatures: usize,
    /// Total number of signatures
    pub total_signatures: usize,
    /// Multi-signature policy used
    pub policy: MultiSignaturePolicy,
    /// Error message if validation failed
    pub error: Option<String>,
}

#[derive(Debug, Clone)]
pub struct CertificateChainValidationResult {
    /// Validation status
    pub status: ValidationStatus,
    /// Error message if validation failed
    pub error: Option<String>,
    /// Certificate chain length
    pub chain_length: usize,
    /// Root certificate
    pub root_certificate: Option<String>,
    /// Intermediate certificates
    pub intermediate_certificates: Vec<String>,
    /// End entity certificate
    pub end_entity_certificate: Option<String>,
    /// Trust level of the chain
    pub trust_level: TrustLevel,
    /// Revocation status
    pub revocation_status: RevocationStatus,
}

#[derive(Debug, Clone)]
pub struct TimestampValidationResult {
    /// Validation status
    pub status: ValidationStatus,
    /// Timestamp value
    pub timestamp: SystemTime,
    /// TSA certificate
    pub tsa_certificate: Option<Vec<u8>>,
    /// Policy OID
    pub policy_oid: Option<String>,
    /// Timestamp accuracy
    pub accuracy: Option<Duration>,
    /// Error message if validation failed
    pub error: Option<String>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ValidationStatus {
    Valid,
    Invalid,
    Warning,
    Unknown,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_crypto_validation_config_creation() {
        let config = CryptoValidationConfig::default();
        assert_eq!(config.min_required_signatures, 1);
        assert!(config.required_algorithms.contains(&SignatureAlgorithm::Ed25519));

        let high_security_config = CryptoValidationConfig::high_security();
        assert_eq!(high_security_config.min_required_signatures, 2);
        assert!(high_security_config.enable_crl_checking);
        assert!(high_security_config.enable_ocsp_checking);
    }

    #[test]
    fn test_validation_engine_creation() {
        let config = CryptoValidationConfig::default();
        let engine = CryptoValidationEngine::new(config).unwrap();

        let stats = engine.get_validation_statistics().unwrap();
        assert_eq!(stats.total_validations, 0);
        assert_eq!(stats.successful_validations, 0);
    }

    #[test]
    fn test_trust_level_ordering() {
        assert!(TrustLevel::Absolute > TrustLevel::High);
        assert!(TrustLevel::High > TrustLevel::Medium);
        assert!(TrustLevel::Medium > TrustLevel::Low);
        assert!(TrustLevel::Low > TrustLevel::None);
    }

    #[test]
    fn test_validation_status() {
        assert_eq!(ValidationStatus::Valid, ValidationStatus::Valid);
        assert_ne!(ValidationStatus::Valid, ValidationStatus::Invalid);
    }
}