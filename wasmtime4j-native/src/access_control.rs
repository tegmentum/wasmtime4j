//! # Access Control and Authorization Module
//!
//! Enterprise-grade access control and authorization system including:
//! - Role-Based Access Control (RBAC)
//! - Attribute-Based Access Control (ABAC)
//! - Enterprise identity provider integration
//! - Session management and token-based authentication
//! - Resource access control for memory, compute, and I/O operations
//!
//! This module provides comprehensive authorization infrastructure for
//! enterprise deployments requiring fine-grained access control.

use crate::error::{WasmtimeError, WasmtimeResult};
use std::collections::{HashMap, HashSet};
use std::sync::{Arc, Mutex};
use std::time::{Duration, SystemTime, UNIX_EPOCH};
use serde::{Deserialize, Serialize};
use ring::hmac;
use base64::{Engine as _, engine::general_purpose};

/// User identity representation
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserIdentity {
    /// Unique user identifier
    pub user_id: String,
    /// User display name
    pub display_name: String,
    /// User email address
    pub email: Option<String>,
    /// User groups or organizational units
    pub groups: HashSet<String>,
    /// User attributes for ABAC
    pub attributes: HashMap<String, String>,
    /// Authentication timestamp
    pub authenticated_at: SystemTime,
    /// Authentication provider
    pub auth_provider: String,
}

/// Role definition for RBAC
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Role {
    /// Unique role identifier
    pub role_id: String,
    /// Human-readable role name
    pub name: String,
    /// Role description
    pub description: Option<String>,
    /// Permissions granted by this role
    pub permissions: Vec<Permission>,
    /// Parent roles (for role hierarchy)
    pub parent_roles: HashSet<String>,
    /// Role attributes
    pub attributes: HashMap<String, String>,
}

/// Permission definition
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct Permission {
    /// Resource type being protected
    pub resource_type: String,
    /// Specific resource identifier (optional)
    pub resource_id: Option<String>,
    /// Action being performed
    pub action: String,
    /// Additional conditions
    pub conditions: HashMap<String, String>,
}

impl Permission {
    /// Create a new permission
    pub fn new(resource_type: String, action: String) -> Self {
        Self {
            resource_type,
            resource_id: None,
            action,
            conditions: HashMap::new(),
        }
    }

    /// Create permission for specific resource
    pub fn for_resource(resource_type: String, resource_id: String, action: String) -> Self {
        Self {
            resource_type,
            resource_id: Some(resource_id),
            action,
            conditions: HashMap::new(),
        }
    }

    /// Add a condition to the permission
    pub fn with_condition(mut self, key: String, value: String) -> Self {
        self.conditions.insert(key, value);
        self
    }

    /// Check if this permission matches a request
    pub fn matches(&self, request: &AccessRequest) -> bool {
        // Check resource type
        if self.resource_type != "*" && self.resource_type != request.resource_type {
            return false;
        }

        // Check specific resource ID
        if let Some(ref resource_id) = self.resource_id {
            if resource_id != "*" && Some(resource_id) != request.resource_id.as_ref() {
                return false;
            }
        }

        // Check action
        if self.action != "*" && self.action != request.action {
            return false;
        }

        // Check conditions
        for (key, value) in &self.conditions {
            if let Some(request_value) = request.context.get(key) {
                if value != "*" && value != request_value {
                    return false;
                }
            } else {
                return false;
            }
        }

        true
    }
}

/// Access request for authorization
#[derive(Debug, Clone)]
pub struct AccessRequest {
    /// User making the request
    pub user: UserIdentity,
    /// Resource type being accessed
    pub resource_type: String,
    /// Specific resource identifier
    pub resource_id: Option<String>,
    /// Action being performed
    pub action: String,
    /// Request context and attributes
    pub context: HashMap<String, String>,
    /// Request timestamp
    pub timestamp: SystemTime,
}

impl AccessRequest {
    /// Create a new access request
    pub fn new(
        user: UserIdentity,
        resource_type: String,
        action: String,
    ) -> Self {
        Self {
            user,
            resource_type,
            resource_id: None,
            action,
            context: HashMap::new(),
            timestamp: SystemTime::now(),
        }
    }

    /// Set specific resource ID
    pub fn for_resource(mut self, resource_id: String) -> Self {
        self.resource_id = Some(resource_id);
        self
    }

    /// Add context attribute
    pub fn with_context(mut self, key: String, value: String) -> Self {
        self.context.insert(key, value);
        self
    }
}

/// Authorization decision
#[derive(Debug, Clone)]
pub enum AuthorizationDecision {
    /// Access granted
    Allow {
        /// Reason for allowing access
        reason: String,
        /// Applicable permissions
        permissions: Vec<Permission>,
    },
    /// Access denied
    Deny {
        /// Reason for denying access
        reason: String,
        /// Required permissions not met
        missing_permissions: Vec<Permission>,
    },
    /// Decision could not be made
    Indeterminate {
        /// Reason for indeterminate decision
        reason: String,
    },
}

impl AuthorizationDecision {
    /// Check if access is allowed
    pub fn is_allowed(&self) -> bool {
        matches!(self, AuthorizationDecision::Allow { .. })
    }

    /// Get the reason for the decision
    pub fn reason(&self) -> &str {
        match self {
            AuthorizationDecision::Allow { reason, .. } => reason,
            AuthorizationDecision::Deny { reason, .. } => reason,
            AuthorizationDecision::Indeterminate { reason } => reason,
        }
    }
}

/// Session token for authenticated users
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SessionToken {
    /// Unique token identifier
    pub token_id: String,
    /// User this token belongs to
    pub user_id: String,
    /// Token creation time
    pub issued_at: SystemTime,
    /// Token expiration time
    pub expires_at: SystemTime,
    /// Token scopes or permissions
    pub scopes: HashSet<String>,
    /// Session attributes
    pub attributes: HashMap<String, String>,
    /// Token signature for integrity
    pub signature: String,
}

impl SessionToken {
    /// Check if the token has expired
    pub fn is_expired(&self) -> bool {
        SystemTime::now() > self.expires_at
    }

    /// Check if the token has a specific scope
    pub fn has_scope(&self, scope: &str) -> bool {
        self.scopes.contains(scope)
    }

    /// Verify token signature
    pub fn verify_signature(&self, secret: &[u8]) -> WasmtimeResult<bool> {
        let key = hmac::Key::new(hmac::HMAC_SHA256, secret);
        let token_data = format!("{}:{}:{}:{}",
            self.token_id, self.user_id,
            self.issued_at.duration_since(UNIX_EPOCH).unwrap_or_default().as_secs(),
            self.expires_at.duration_since(UNIX_EPOCH).unwrap_or_default().as_secs()
        );

        let computed_signature = hmac::sign(&key, token_data.as_bytes());
        let computed_signature_b64 = general_purpose::STANDARD.encode(computed_signature.as_ref());

        Ok(computed_signature_b64 == self.signature)
    }
}

/// RBAC policy engine
#[derive(Debug, Clone)]
pub struct RbacEngine {
    /// User role assignments
    user_roles: HashMap<String, HashSet<String>>,
    /// Role definitions
    roles: HashMap<String, Role>,
    /// Role hierarchy cache
    role_hierarchy: HashMap<String, HashSet<String>>,
}

impl Default for RbacEngine {
    fn default() -> Self {
        Self::new()
    }
}

impl RbacEngine {
    /// Create a new RBAC engine
    pub fn new() -> Self {
        Self {
            user_roles: HashMap::new(),
            roles: HashMap::new(),
            role_hierarchy: HashMap::new(),
        }
    }

    /// Add a role definition
    pub fn add_role(&mut self, role: Role) {
        self.role_hierarchy.remove(&role.role_id); // Invalidate cache
        self.roles.insert(role.role_id.clone(), role);
    }

    /// Assign a role to a user
    pub fn assign_role(&mut self, user_id: String, role_id: String) {
        self.user_roles.entry(user_id).or_default().insert(role_id);
    }

    /// Remove a role from a user
    pub fn remove_role(&mut self, user_id: &str, role_id: &str) {
        if let Some(roles) = self.user_roles.get_mut(user_id) {
            roles.remove(role_id);
        }
    }

    /// Get all effective roles for a user (including inherited)
    pub fn get_user_roles(&mut self, user_id: &str) -> HashSet<String> {
        let direct_roles = self.user_roles.get(user_id).cloned().unwrap_or_default();
        let mut effective_roles = HashSet::new();

        for role_id in direct_roles {
            effective_roles.extend(self.get_role_hierarchy(&role_id));
        }

        effective_roles
    }

    /// Get role hierarchy (including parents)
    fn get_role_hierarchy(&mut self, role_id: &str) -> HashSet<String> {
        if let Some(cached) = self.role_hierarchy.get(role_id) {
            return cached.clone();
        }

        let mut hierarchy = HashSet::new();
        hierarchy.insert(role_id.to_string());

        // Extract parent role IDs to avoid borrowing conflict
        let parent_role_ids: Vec<String> = if let Some(role) = self.roles.get(role_id) {
            role.parent_roles.iter().cloned().collect()
        } else {
            Vec::new()
        };

        // Make recursive calls after extracting parent IDs
        for parent_id in parent_role_ids {
            hierarchy.extend(self.get_role_hierarchy(&parent_id));
        }

        self.role_hierarchy.insert(role_id.to_string(), hierarchy.clone());
        hierarchy
    }

    /// Get all permissions for a user
    pub fn get_user_permissions(&mut self, user_id: &str) -> Vec<Permission> {
        let roles = self.get_user_roles(user_id);
        let mut permissions = Vec::new();

        for role_id in roles {
            if let Some(role) = self.roles.get(&role_id) {
                permissions.extend(role.permissions.clone());
            }
        }

        permissions
    }

    /// Check if a user has a specific permission
    pub fn has_permission(&mut self, user_id: &str, permission: &Permission) -> bool {
        let user_permissions = self.get_user_permissions(user_id);
        user_permissions.iter().any(|p| p == permission)
    }

    /// Authorize an access request using RBAC
    pub fn authorize(&mut self, request: &AccessRequest) -> AuthorizationDecision {
        let required_permission = Permission {
            resource_type: request.resource_type.clone(),
            resource_id: request.resource_id.clone(),
            action: request.action.clone(),
            conditions: request.context.clone(),
        };

        let user_permissions = self.get_user_permissions(&request.user.user_id);

        // Check if any user permission matches the request
        for permission in &user_permissions {
            if permission.matches(request) {
                return AuthorizationDecision::Allow {
                    reason: format!("RBAC permission granted: {:?}", permission),
                    permissions: vec![permission.clone()],
                };
            }
        }

        AuthorizationDecision::Deny {
            reason: "No matching RBAC permissions found".to_string(),
            missing_permissions: vec![required_permission],
        }
    }
}

/// ABAC policy engine for attribute-based access control
#[derive(Debug, Clone)]
pub struct AbacEngine {
    /// Policy rules
    policies: Vec<AbacPolicy>,
}

/// ABAC policy rule
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AbacPolicy {
    /// Policy identifier
    pub policy_id: String,
    /// Policy description
    pub description: String,
    /// Target resources
    pub target: AbacTarget,
    /// Conditions for policy application
    pub conditions: Vec<AbacCondition>,
    /// Effect when policy matches
    pub effect: AbacEffect,
    /// Priority for policy evaluation
    pub priority: i32,
}

/// ABAC target specification
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AbacTarget {
    /// Resource types this policy applies to
    pub resource_types: Vec<String>,
    /// Actions this policy applies to
    pub actions: Vec<String>,
}

/// ABAC condition
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AbacCondition {
    /// Attribute path (e.g., "user.department", "resource.classification")
    pub attribute: String,
    /// Comparison operator
    pub operator: AbacOperator,
    /// Comparison value
    pub value: String,
}

/// ABAC comparison operators
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum AbacOperator {
    /// Equality
    Equals,
    /// Inequality
    NotEquals,
    /// String contains
    Contains,
    /// String starts with
    StartsWith,
    /// String ends with
    EndsWith,
    /// Numeric greater than
    GreaterThan,
    /// Numeric less than
    LessThan,
    /// Set membership
    In,
    /// Set non-membership
    NotIn,
}

/// ABAC policy effect
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum AbacEffect {
    /// Allow access
    Permit,
    /// Deny access
    Deny,
}

impl Default for AbacEngine {
    fn default() -> Self {
        Self::new()
    }
}

impl AbacEngine {
    /// Create a new ABAC engine
    pub fn new() -> Self {
        Self {
            policies: Vec::new(),
        }
    }

    /// Add a policy to the engine
    pub fn add_policy(&mut self, policy: AbacPolicy) {
        self.policies.push(policy);
        // Sort by priority (higher priority first)
        self.policies.sort_by(|a, b| b.priority.cmp(&a.priority));
    }

    /// Authorize an access request using ABAC
    pub fn authorize(&self, request: &AccessRequest) -> AuthorizationDecision {
        let mut applicable_policies = Vec::new();

        // Find policies that apply to this request
        for policy in &self.policies {
            if self.policy_applies(policy, request) {
                applicable_policies.push(policy);
            }
        }

        // Evaluate policies in priority order
        for policy in applicable_policies {
            if self.evaluate_conditions(&policy.conditions, request) {
                match policy.effect {
                    AbacEffect::Permit => {
                        return AuthorizationDecision::Allow {
                            reason: format!("ABAC policy {} permits access", policy.policy_id),
                            permissions: vec![], // ABAC doesn't use explicit permissions
                        };
                    }
                    AbacEffect::Deny => {
                        return AuthorizationDecision::Deny {
                            reason: format!("ABAC policy {} denies access", policy.policy_id),
                            missing_permissions: vec![],
                        };
                    }
                }
            }
        }

        AuthorizationDecision::Indeterminate {
            reason: "No applicable ABAC policies found".to_string(),
        }
    }

    /// Check if a policy applies to a request
    fn policy_applies(&self, policy: &AbacPolicy, request: &AccessRequest) -> bool {
        // Check resource type
        if !policy.target.resource_types.is_empty() &&
           !policy.target.resource_types.contains(&request.resource_type) &&
           !policy.target.resource_types.contains(&"*".to_string()) {
            return false;
        }

        // Check action
        if !policy.target.actions.is_empty() &&
           !policy.target.actions.contains(&request.action) &&
           !policy.target.actions.contains(&"*".to_string()) {
            return false;
        }

        true
    }

    /// Evaluate policy conditions
    fn evaluate_conditions(&self, conditions: &[AbacCondition], request: &AccessRequest) -> bool {
        for condition in conditions {
            if !self.evaluate_condition(condition, request) {
                return false;
            }
        }
        true
    }

    /// Evaluate a single condition
    fn evaluate_condition(&self, condition: &AbacCondition, request: &AccessRequest) -> bool {
        let attribute_value = self.get_attribute_value(&condition.attribute, request);

        match attribute_value {
            Some(value) => self.compare_values(&value, &condition.value, &condition.operator),
            None => false,
        }
    }

    /// Get attribute value from request context
    fn get_attribute_value(&self, attribute: &str, request: &AccessRequest) -> Option<String> {
        let parts: Vec<&str> = attribute.split('.').collect();
        if parts.len() < 2 {
            return None;
        }

        match parts[0] {
            "user" => match parts[1] {
                "id" => Some(request.user.user_id.clone()),
                "email" => request.user.email.clone(),
                "groups" => Some(request.user.groups.iter().cloned().collect::<Vec<_>>().join(",")),
                _ => request.user.attributes.get(parts[1]).cloned(),
            },
            "resource" => match parts[1] {
                "type" => Some(request.resource_type.clone()),
                "id" => request.resource_id.clone(),
                _ => None,
            },
            "context" => request.context.get(parts[1]).cloned(),
            _ => None,
        }
    }

    /// Compare values using the specified operator
    fn compare_values(&self, left: &str, right: &str, operator: &AbacOperator) -> bool {
        match operator {
            AbacOperator::Equals => left == right,
            AbacOperator::NotEquals => left != right,
            AbacOperator::Contains => left.contains(right),
            AbacOperator::StartsWith => left.starts_with(right),
            AbacOperator::EndsWith => left.ends_with(right),
            AbacOperator::GreaterThan => {
                if let (Ok(l), Ok(r)) = (left.parse::<f64>(), right.parse::<f64>()) {
                    l > r
                } else {
                    false
                }
            }
            AbacOperator::LessThan => {
                if let (Ok(l), Ok(r)) = (left.parse::<f64>(), right.parse::<f64>()) {
                    l < r
                } else {
                    false
                }
            }
            AbacOperator::In => {
                let values: HashSet<&str> = right.split(',').collect();
                values.contains(left)
            }
            AbacOperator::NotIn => {
                let values: HashSet<&str> = right.split(',').collect();
                !values.contains(left)
            }
        }
    }
}

/// Combined authorization engine using both RBAC and ABAC
pub struct AuthorizationEngine {
    /// RBAC engine
    rbac: RbacEngine,
    /// ABAC engine
    abac: AbacEngine,
    /// Session manager
    session_manager: Arc<Mutex<SessionManager>>,
    /// Authorization combining algorithm
    combining_algorithm: CombiningAlgorithm,
}

/// Algorithm for combining multiple authorization decisions
#[derive(Debug, Clone)]
pub enum CombiningAlgorithm {
    /// Allow if any engine allows
    PermitOverrides,
    /// Deny if any engine denies
    DenyOverrides,
    /// Require all engines to allow
    AllowOnlyIfAllPermit,
    /// First applicable decision wins
    FirstApplicable,
}

/// Session manager for token-based authentication
#[derive(Debug)]
pub struct SessionManager {
    /// Active sessions by token ID
    sessions: HashMap<String, SessionToken>,
    /// HMAC secret for token signing
    hmac_secret: Vec<u8>,
    /// Default token expiration duration
    default_expiration: Duration,
}

impl SessionManager {
    /// Create a new session manager
    pub fn new(hmac_secret: Vec<u8>) -> Self {
        Self {
            sessions: HashMap::new(),
            hmac_secret,
            default_expiration: Duration::from_secs(3600), // 1 hour
        }
    }

    /// Create a new session token
    pub fn create_token(
        &mut self,
        user_id: String,
        scopes: HashSet<String>,
    ) -> WasmtimeResult<SessionToken> {
        let token_id = format!("{}-{}", user_id,
            SystemTime::now().duration_since(UNIX_EPOCH).unwrap_or_default().as_nanos());

        let issued_at = SystemTime::now();
        let expires_at = issued_at + self.default_expiration;

        // Create token signature
        let key = hmac::Key::new(hmac::HMAC_SHA256, &self.hmac_secret);
        let token_data = format!("{}:{}:{}:{}",
            token_id, user_id,
            issued_at.duration_since(UNIX_EPOCH).unwrap_or_default().as_secs(),
            expires_at.duration_since(UNIX_EPOCH).unwrap_or_default().as_secs()
        );
        let signature = hmac::sign(&key, token_data.as_bytes());
        let signature_b64 = general_purpose::STANDARD.encode(signature.as_ref());

        let token = SessionToken {
            token_id: token_id.clone(),
            user_id,
            issued_at,
            expires_at,
            scopes,
            attributes: HashMap::new(),
            signature: signature_b64,
        };

        self.sessions.insert(token_id, token.clone());
        Ok(token)
    }

    /// Validate and retrieve a session token
    pub fn validate_token(&self, token_id: &str) -> WasmtimeResult<Option<SessionToken>> {
        if let Some(token) = self.sessions.get(token_id) {
            if token.is_expired() {
                return Ok(None);
            }

            if token.verify_signature(&self.hmac_secret)? {
                Ok(Some(token.clone()))
            } else {
                Err(WasmtimeError::Security {
                    message: "Token signature verification failed".to_string(),
                })
            }
        } else {
            Ok(None)
        }
    }

    /// Revoke a session token
    pub fn revoke_token(&mut self, token_id: &str) {
        self.sessions.remove(token_id);
    }

    /// Clean up expired tokens
    pub fn cleanup_expired(&mut self) -> usize {
        let initial_count = self.sessions.len();
        self.sessions.retain(|_, token| !token.is_expired());
        initial_count - self.sessions.len()
    }
}

impl AuthorizationEngine {
    /// Create a new authorization engine
    pub fn new(
        combining_algorithm: CombiningAlgorithm,
        hmac_secret: Vec<u8>,
    ) -> Self {
        Self {
            rbac: RbacEngine::new(),
            abac: AbacEngine::new(),
            session_manager: Arc::new(Mutex::new(SessionManager::new(hmac_secret))),
            combining_algorithm,
        }
    }

    /// Authorize an access request
    pub fn authorize(&mut self, request: &AccessRequest) -> AuthorizationDecision {
        let rbac_decision = self.rbac.authorize(request);
        let abac_decision = self.abac.authorize(request);

        match self.combining_algorithm {
            CombiningAlgorithm::PermitOverrides => {
                if rbac_decision.is_allowed() || abac_decision.is_allowed() {
                    AuthorizationDecision::Allow {
                        reason: "At least one authorization engine permits access".to_string(),
                        permissions: vec![],
                    }
                } else {
                    AuthorizationDecision::Deny {
                        reason: format!("Both engines deny: RBAC({}), ABAC({})",
                            rbac_decision.reason(), abac_decision.reason()),
                        missing_permissions: vec![],
                    }
                }
            }
            CombiningAlgorithm::DenyOverrides => {
                if matches!(rbac_decision, AuthorizationDecision::Deny { .. }) ||
                   matches!(abac_decision, AuthorizationDecision::Deny { .. }) {
                    AuthorizationDecision::Deny {
                        reason: "At least one authorization engine denies access".to_string(),
                        missing_permissions: vec![],
                    }
                } else if rbac_decision.is_allowed() || abac_decision.is_allowed() {
                    AuthorizationDecision::Allow {
                        reason: "No explicit denials and at least one permit".to_string(),
                        permissions: vec![],
                    }
                } else {
                    AuthorizationDecision::Indeterminate {
                        reason: "No explicit decisions from authorization engines".to_string(),
                    }
                }
            }
            CombiningAlgorithm::AllowOnlyIfAllPermit => {
                if rbac_decision.is_allowed() && abac_decision.is_allowed() {
                    AuthorizationDecision::Allow {
                        reason: "All authorization engines permit access".to_string(),
                        permissions: vec![],
                    }
                } else {
                    AuthorizationDecision::Deny {
                        reason: "Not all authorization engines permit access".to_string(),
                        missing_permissions: vec![],
                    }
                }
            }
            CombiningAlgorithm::FirstApplicable => {
                if !matches!(rbac_decision, AuthorizationDecision::Indeterminate { .. }) {
                    rbac_decision
                } else {
                    abac_decision
                }
            }
        }
    }

    /// Get a mutable reference to the RBAC engine
    pub fn rbac_mut(&mut self) -> &mut RbacEngine {
        &mut self.rbac
    }

    /// Get a mutable reference to the ABAC engine
    pub fn abac_mut(&mut self) -> &mut AbacEngine {
        &mut self.abac
    }

    /// Get the session manager
    pub fn session_manager(&self) -> Arc<Mutex<SessionManager>> {
        Arc::clone(&self.session_manager)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_permission_matching() {
        let permission = Permission::new("module".to_string(), "execute".to_string());

        let user = UserIdentity {
            user_id: "test_user".to_string(),
            display_name: "Test User".to_string(),
            email: None,
            groups: HashSet::new(),
            attributes: HashMap::new(),
            authenticated_at: SystemTime::now(),
            auth_provider: "test".to_string(),
        };

        let request = AccessRequest::new(user, "module".to_string(), "execute".to_string());

        assert!(permission.matches(&request));
    }

    #[test]
    fn test_rbac_authorization() {
        let mut rbac = RbacEngine::new();

        // Create a role with module execution permission
        let mut role = Role {
            role_id: "module_executor".to_string(),
            name: "Module Executor".to_string(),
            description: None,
            permissions: Vec::new(),
            parent_roles: HashSet::new(),
            attributes: HashMap::new(),
        };

        role.permissions.push(Permission::new("module".to_string(), "execute".to_string()));
        rbac.add_role(role);

        // Assign role to user
        rbac.assign_role("test_user".to_string(), "module_executor".to_string());

        // Test authorization
        let user = UserIdentity {
            user_id: "test_user".to_string(),
            display_name: "Test User".to_string(),
            email: None,
            groups: HashSet::new(),
            attributes: HashMap::new(),
            authenticated_at: SystemTime::now(),
            auth_provider: "test".to_string(),
        };

        let request = AccessRequest::new(user, "module".to_string(), "execute".to_string());
        let decision = rbac.authorize(&request);

        assert!(decision.is_allowed());
    }

    #[test]
    fn test_session_token_creation() {
        let secret = b"test_secret".to_vec();
        let mut session_manager = SessionManager::new(secret);

        let mut scopes = HashSet::new();
        scopes.insert("module:execute".to_string());

        let token = session_manager.create_token("test_user".to_string(), scopes).unwrap();

        assert_eq!(token.user_id, "test_user");
        assert!(!token.is_expired());
        assert!(token.has_scope("module:execute"));
    }

    #[test]
    fn test_abac_condition_evaluation() {
        let abac = AbacEngine::new();

        let condition = AbacCondition {
            attribute: "user.department".to_string(),
            operator: AbacOperator::Equals,
            value: "engineering".to_string(),
        };

        let mut user = UserIdentity {
            user_id: "test_user".to_string(),
            display_name: "Test User".to_string(),
            email: None,
            groups: HashSet::new(),
            attributes: HashMap::new(),
            authenticated_at: SystemTime::now(),
            auth_provider: "test".to_string(),
        };

        user.attributes.insert("department".to_string(), "engineering".to_string());

        let request = AccessRequest::new(user, "module".to_string(), "execute".to_string());

        assert!(abac.evaluate_condition(&condition, &request));
    }
}