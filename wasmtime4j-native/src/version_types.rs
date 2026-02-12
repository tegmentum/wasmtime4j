//! Basic version types for component model support
//!
//! Provides essential semantic versioning types used across the component model.

use std::cmp::Ordering;
use std::fmt;

/// Unique identifier for a component
pub type ComponentId = String;

/// Semantic version for component dependencies
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct SemanticVersion {
    pub major: u32,
    pub minor: u32,
    pub patch: u32,
}

impl SemanticVersion {
    /// Creates a new semantic version
    pub fn new(major: u32, minor: u32, patch: u32) -> Self {
        Self {
            major,
            minor,
            patch,
        }
    }
}

impl Default for SemanticVersion {
    fn default() -> Self {
        Self::new(1, 0, 0)
    }
}

impl Ord for SemanticVersion {
    fn cmp(&self, other: &Self) -> Ordering {
        match self.major.cmp(&other.major) {
            Ordering::Equal => match self.minor.cmp(&other.minor) {
                Ordering::Equal => self.patch.cmp(&other.patch),
                ord => ord,
            },
            ord => ord,
        }
    }
}

impl PartialOrd for SemanticVersion {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

impl fmt::Display for SemanticVersion {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}.{}.{}", self.major, self.minor, self.patch)
    }
}

/// Version constraint for dependency resolution
#[derive(Debug, Clone, PartialEq)]
pub enum VersionConstraint {
    /// Exact version match
    Exact(SemanticVersion),
    /// Greater than
    GreaterThan(SemanticVersion),
    /// Greater than or equal
    GreaterThanOrEqual(SemanticVersion),
    /// Less than
    LessThan(SemanticVersion),
    /// Less than or equal
    LessThanOrEqual(SemanticVersion),
    /// Compatible (^1.2.3 - same major)
    Compatible(SemanticVersion),
    /// Approximately equal (~1.2.3 - same major.minor)
    ApproximatelyEqual(SemanticVersion),
    /// Version range (inclusive)
    Range(SemanticVersion, SemanticVersion),
}

impl VersionConstraint {
    /// Checks if a version satisfies this constraint
    pub fn satisfies(&self, version: &SemanticVersion) -> bool {
        match self {
            Self::Exact(v) => version == v,
            Self::GreaterThan(v) => version > v,
            Self::GreaterThanOrEqual(v) => version >= v,
            Self::LessThan(v) => version < v,
            Self::LessThanOrEqual(v) => version <= v,
            Self::Compatible(v) => version.major == v.major && version >= v,
            Self::ApproximatelyEqual(v) => {
                version.major == v.major && version.minor == v.minor && version >= v
            }
            Self::Range(min, max) => version >= min && version <= max,
        }
    }
}

impl Default for VersionConstraint {
    fn default() -> Self {
        Self::GreaterThanOrEqual(SemanticVersion::default())
    }
}
