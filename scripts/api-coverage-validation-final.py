#!/usr/bin/env python3
"""
Final API Coverage Validation Framework for wasmtime4j
Task #310 - API Coverage Validation and Documentation

This comprehensive script validates 100% API coverage achievement against Wasmtime 36.0.2
and generates detailed analysis for documentation and release preparation.
"""

import os
import re
import json
import subprocess
import time
from pathlib import Path
from typing import Dict, List, Set, Tuple, Optional
from dataclasses import dataclass
import hashlib

@dataclass
class ApiMethod:
    """Represents a method in the API."""
    name: str
    signature: str
    return_type: str
    parameters: List[str]
    is_static: bool = False
    is_deprecated: bool = False
    doc_comment: Optional[str] = None

@dataclass
class ApiInterface:
    """Represents an API interface or class."""
    name: str
    package: str
    type: str  # 'interface', 'class', 'enum'
    methods: List[ApiMethod]
    extends: List[str]
    implements: List[str]
    is_public: bool = True
    doc_comment: Optional[str] = None

@dataclass
class CoverageResult:
    """Results of API coverage analysis."""
    interface_name: str
    total_methods: int
    jni_implemented: int
    panama_implemented: int
    both_implemented: int
    coverage_score: float
    missing_jni: List[str]
    missing_panama: List[str]
    extra_jni: List[str]
    extra_panama: List[str]

class FinalApiCoverageValidator:
    """Comprehensive API coverage validator for wasmtime4j."""

    def __init__(self, base_path: str):
        self.base_path = Path(base_path)
        self.wasmtime_version = "40.0.1"

        # Comprehensive list of Wasmtime 36.0.2 API categories
        self.wasmtime_api_categories = {
            # Core Engine APIs
            'core': [
                'Engine', 'EngineConfig', 'Store', 'StoreConfig', 'StoreData',
                'Module', 'ModuleConfig', 'Instance', 'InstancePre', 'Linker'
            ],

            # Memory and Tables
            'memory': [
                'Memory', 'MemoryConfig', 'MemoryType', 'Table', 'TableConfig',
                'TableType', 'Global', 'GlobalConfig', 'GlobalType'
            ],

            # Function Types and Host Functions
            'functions': [
                'Function', 'FunctionType', 'HostFunction', 'Caller',
                'TypedFunction', 'Callback', 'Trap'
            ],

            # WebAssembly Values and Types
            'values': [
                'Val', 'ValType', 'ExternType', 'ExportType', 'ImportType',
                'WasmValue', 'ReferenceType', 'ArrayType', 'StructType'
            ],

            # WASI Support
            'wasi': [
                'WasiConfig', 'WasiCtx', 'WasiCtxBuilder', 'WasiInstance',
                'WasiPreview1', 'WasiLinker', 'WasiDir', 'WasiFile'
            ],

            # Component Model (Wasmtime 36.0.2 feature)
            'component': [
                'Component', 'ComponentConfig', 'ComponentInstance',
                'ComponentLinker', 'ComponentType', 'ComponentExport'
            ],

            # Advanced Features
            'advanced': [
                'Config', 'Error', 'Trap', 'Frame', 'FrameInfo',
                'FuelConsumer', 'EpochDeadline', 'InterruptHandle'
            ],

            # SIMD and Vector Instructions
            'simd': [
                'V128', 'SimdLane', 'SimdOperation', 'VectorType'
            ],

            # Exception Handling (if supported)
            'exceptions': [
                'Exception', 'ExceptionType', 'Tag', 'TagType'
            ],

            # GC Types (preparation for GC proposal)
            'gc': [
                'GcRef', 'GcHeap', 'GcType', 'GcObject'
            ],

            # Compilation and Optimization
            'compilation': [
                'CompilationStrategy', 'OptLevel', 'Profiler',
                'CraneliftStrategy', 'WasmFeatures'
            ]
        }

        self.analysis_results = {}
        self.coverage_summary = {}

    def discover_all_api_classes(self) -> Dict[str, List[ApiInterface]]:
        """Discover all API classes, interfaces, and enums in the project."""
        print("Discovering all API classes and interfaces...")

        discovered_apis = {
            'core': [],
            'jni': [],
            'panama': []
        }

        # Discover core API
        core_path = self.base_path / "wasmtime4j" / "src" / "main" / "java"
        if core_path.exists():
            discovered_apis['core'] = self._scan_directory_for_apis(core_path)
        else:
            print(f"Warning: Core API path not found: {core_path}")

        # Discover JNI implementation
        jni_path = self.base_path / "wasmtime4j-jni" / "src" / "main" / "java"
        if jni_path.exists():
            discovered_apis['jni'] = self._scan_directory_for_apis(jni_path)
        else:
            print(f"Warning: JNI path not found: {jni_path}")

        # Discover Panama implementation
        panama_path = self.base_path / "wasmtime4j-panama" / "src" / "main" / "java"
        if panama_path.exists():
            discovered_apis['panama'] = self._scan_directory_for_apis(panama_path)
        else:
            print(f"Warning: Panama path not found: {panama_path}")

        return discovered_apis

    def _scan_directory_for_apis(self, directory: Path) -> List[ApiInterface]:
        """Scan a directory for API definitions."""
        apis = []

        for java_file in directory.rglob("*.java"):
            try:
                api_interface = self._parse_java_file(java_file)
                if api_interface:
                    apis.append(api_interface)
            except Exception as e:
                print(f"Warning: Failed to parse {java_file}: {e}")

        return apis

    def _parse_java_file(self, file_path: Path) -> Optional[ApiInterface]:
        """Parse a Java file and extract API information."""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()

            # Extract package
            package_match = re.search(r'package\s+([\w.]+);', content)
            package = package_match.group(1) if package_match else ''

            # Extract class/interface/enum declaration
            type_pattern = r'(?:public\s+)?(?:abstract\s+)?(interface|class|enum)\s+(\w+)(?:\s+extends\s+([\w,\s<>]+))?(?:\s+implements\s+([\w,\s<>]+))?'
            type_match = re.search(type_pattern, content)

            if not type_match:
                return None

            type_kind = type_match.group(1)
            class_name = type_match.group(2)
            extends = self._parse_type_list(type_match.group(3)) if type_match.group(3) else []
            implements = self._parse_type_list(type_match.group(4)) if type_match.group(4) else []

            # Extract doc comment
            doc_pattern = r'/\*\*\s*(.*?)\*/'
            doc_match = re.search(doc_pattern, content, re.DOTALL)
            doc_comment = doc_match.group(1).strip() if doc_match else None

            # Extract methods
            methods = self._extract_methods(content)

            return ApiInterface(
                name=class_name,
                package=package,
                type=type_kind,
                methods=methods,
                extends=extends,
                implements=implements,
                is_public=True,  # Assume public if we found it
                doc_comment=doc_comment
            )

        except Exception as e:
            print(f"Error parsing {file_path}: {e}")
            return None

    def _parse_type_list(self, type_string: str) -> List[str]:
        """Parse a comma-separated list of types."""
        if not type_string:
            return []
        return [t.strip() for t in type_string.split(',')]

    def _extract_methods(self, content: str) -> List[ApiMethod]:
        """Extract method signatures from Java content."""
        methods = []

        # Remove comments
        content = re.sub(r'//.*?$', '', content, flags=re.MULTILINE)
        content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)

        # Pattern for method signatures
        method_pattern = (
            r'(?:public|protected|private)?\s*'
            r'(?:static\s+)?(?:final\s+)?(?:abstract\s+)?(?:synchronized\s+)?'
            r'(?:<[^>]+>\s+)?'  # Generic type parameters
            r'([\w\[\]<>,.\s]+)\s+'  # Return type
            r'(\w+)\s*'  # Method name
            r'\(([^)]*)\)'  # Parameters
            r'(?:\s*throws\s+[\w\s,]+)?'  # Throws clause
            r'\s*[;{]'  # Ending with ; or {
        )

        for match in re.finditer(method_pattern, content):
            return_type = match.group(1).strip()
            method_name = match.group(2)
            params_str = match.group(3)

            # Skip constructors and type names
            if method_name in ['class', 'interface', 'enum'] or method_name[0].isupper():
                continue

            # Parse parameters
            parameters = self._parse_parameters(params_str)

            # Check if static
            is_static = 'static' in match.group(0)

            # Check if deprecated
            is_deprecated = '@Deprecated' in content[:match.start()]

            method = ApiMethod(
                name=method_name,
                signature=match.group(0).strip(),
                return_type=return_type,
                parameters=parameters,
                is_static=is_static,
                is_deprecated=is_deprecated
            )

            methods.append(method)

        return methods

    def _parse_parameters(self, params_str: str) -> List[str]:
        """Parse method parameters."""
        if not params_str.strip():
            return []

        params = []
        for param in params_str.split(','):
            param = param.strip()
            if param:
                # Extract just the type (remove variable name)
                parts = param.split()
                if len(parts) >= 2:
                    param_type = ' '.join(parts[:-1])
                    params.append(param_type)

        return params

    def validate_wasmtime_api_coverage(self, discovered_apis: Dict[str, List[ApiInterface]]) -> Dict[str, CoverageResult]:
        """Validate API coverage against Wasmtime 36.0.2 expectations."""
        print(f"Validating API coverage against Wasmtime {self.wasmtime_version}...")

        coverage_results = {}

        # Create lookup maps
        core_apis = {api.name: api for api in discovered_apis['core']}
        jni_apis = {api.name: api for api in discovered_apis['jni']}
        panama_apis = {api.name: api for api in discovered_apis['panama']}

        # Check coverage for each expected API category
        for category, expected_apis in self.wasmtime_api_categories.items():
            print(f"  Analyzing {category} APIs...")

            for api_name in expected_apis:
                result = self._analyze_single_api_coverage(
                    api_name, core_apis, jni_apis, panama_apis
                )
                coverage_results[f"{category}.{api_name}"] = result

        return coverage_results

    def _analyze_single_api_coverage(
        self,
        api_name: str,
        core_apis: Dict[str, ApiInterface],
        jni_apis: Dict[str, ApiInterface],
        panama_apis: Dict[str, ApiInterface]
    ) -> CoverageResult:
        """Analyze coverage for a single API."""

        # Find the core API definition
        core_api = core_apis.get(api_name)
        if not core_api:
            # Try to find by partial match
            for name, api in core_apis.items():
                if api_name.lower() in name.lower():
                    core_api = api
                    break

        if not core_api:
            return CoverageResult(
                interface_name=api_name,
                total_methods=0,
                jni_implemented=0,
                panama_implemented=0,
                both_implemented=0,
                coverage_score=0.0,
                missing_jni=[f"No core API definition found for {api_name}"],
                missing_panama=[f"No core API definition found for {api_name}"],
                extra_jni=[],
                extra_panama=[]
            )

        # Find implementations
        jni_impl = self._find_implementation(api_name, jni_apis)
        panama_impl = self._find_implementation(api_name, panama_apis)

        # Analyze method coverage
        core_methods = {m.name for m in core_api.methods}
        jni_methods = {m.name for m in jni_impl.methods} if jni_impl else set()
        panama_methods = {m.name for m in panama_impl.methods} if panama_impl else set()

        # Calculate coverage
        jni_covered = core_methods.intersection(jni_methods)
        panama_covered = core_methods.intersection(panama_methods)
        both_covered = jni_covered.intersection(panama_covered)

        missing_jni = list(core_methods - jni_methods)
        missing_panama = list(core_methods - panama_methods)
        extra_jni = list(jni_methods - core_methods) if jni_impl else []
        extra_panama = list(panama_methods - core_methods) if panama_impl else []

        total_methods = len(core_methods)
        coverage_score = (len(both_covered) / total_methods * 100) if total_methods > 0 else 0.0

        return CoverageResult(
            interface_name=api_name,
            total_methods=total_methods,
            jni_implemented=len(jni_covered),
            panama_implemented=len(panama_covered),
            both_implemented=len(both_covered),
            coverage_score=coverage_score,
            missing_jni=missing_jni,
            missing_panama=missing_panama,
            extra_jni=extra_jni,
            extra_panama=extra_panama
        )

    def _find_implementation(self, api_name: str, implementations: Dict[str, ApiInterface]) -> Optional[ApiInterface]:
        """Find implementation for an API."""
        # Direct match
        if api_name in implementations:
            return implementations[api_name]

        # Try with common prefixes
        for prefix in ['Jni', 'Panama', '']:
            impl_name = f"{prefix}{api_name}"
            if impl_name in implementations:
                return implementations[impl_name]

        # Try partial matches
        for name, impl in implementations.items():
            if api_name.lower() in name.lower():
                return impl

        return None

    def generate_coverage_summary(self, coverage_results: Dict[str, CoverageResult]) -> Dict:
        """Generate overall coverage summary."""
        total_apis = len(coverage_results)
        fully_covered = sum(1 for r in coverage_results.values() if r.coverage_score == 100.0)
        partially_covered = sum(1 for r in coverage_results.values() if 0 < r.coverage_score < 100.0)
        not_covered = sum(1 for r in coverage_results.values() if r.coverage_score == 0.0)

        total_methods = sum(r.total_methods for r in coverage_results.values())
        implemented_methods = sum(r.both_implemented for r in coverage_results.values())

        overall_coverage = (implemented_methods / total_methods * 100) if total_methods > 0 else 0.0

        # Category breakdown
        category_coverage = {}
        for category in self.wasmtime_api_categories.keys():
            category_results = [r for k, r in coverage_results.items() if k.startswith(f"{category}.")]
            if category_results:
                cat_total = sum(r.total_methods for r in category_results)
                cat_implemented = sum(r.both_implemented for r in category_results)
                cat_coverage = (cat_implemented / cat_total * 100) if cat_total > 0 else 0.0
                category_coverage[category] = {
                    'coverage_percentage': cat_coverage,
                    'total_methods': cat_total,
                    'implemented_methods': cat_implemented,
                    'api_count': len(category_results)
                }

        return {
            'timestamp': time.strftime('%Y-%m-%dT%H:%M:%SZ'),
            'wasmtime_version': self.wasmtime_version,
            'overall_coverage_percentage': overall_coverage,
            'total_apis_analyzed': total_apis,
            'fully_covered_apis': fully_covered,
            'partially_covered_apis': partially_covered,
            'not_covered_apis': not_covered,
            'total_methods': total_methods,
            'implemented_methods': implemented_methods,
            'meets_100_percent_target': overall_coverage >= 100.0,
            'category_breakdown': category_coverage,
            'top_gaps': self._identify_top_gaps(coverage_results),
            'recommendations': self._generate_coverage_recommendations(coverage_results)
        }

    def _identify_top_gaps(self, coverage_results: Dict[str, CoverageResult]) -> List[Dict]:
        """Identify the most critical coverage gaps."""
        gaps = []

        for api_key, result in coverage_results.items():
            if result.coverage_score < 100.0:
                gap_info = {
                    'api': result.interface_name,
                    'category': api_key.split('.')[0],
                    'coverage_score': result.coverage_score,
                    'missing_methods': len(result.missing_jni) + len(result.missing_panama),
                    'priority': 'critical' if result.coverage_score == 0 else 'high' if result.coverage_score < 50 else 'medium'
                }
                gaps.append(gap_info)

        # Sort by priority and missing methods
        gaps.sort(key=lambda x: (x['priority'] == 'critical', x['missing_methods']), reverse=True)
        return gaps[:10]  # Top 10 gaps

    def _generate_coverage_recommendations(self, coverage_results: Dict[str, CoverageResult]) -> List[str]:
        """Generate actionable recommendations."""
        recommendations = []

        critical_missing = [r for r in coverage_results.values() if r.coverage_score == 0]
        if critical_missing:
            recommendations.append(
                f"CRITICAL: Implement {len(critical_missing)} completely missing APIs: "
                f"{', '.join([r.interface_name for r in critical_missing[:3]])}"
            )

        partial_coverage = [r for r in coverage_results.values() if 0 < r.coverage_score < 100]
        if partial_coverage:
            total_missing = sum(len(r.missing_jni) + len(r.missing_panama) for r in partial_coverage)
            recommendations.append(
                f"Complete {total_missing} missing methods across {len(partial_coverage)} partially implemented APIs"
            )

        # Check for implementation inconsistencies
        inconsistent = [r for r in coverage_results.values() if len(r.extra_jni) > 0 or len(r.extra_panama) > 0]
        if inconsistent:
            recommendations.append(
                f"Review {len(inconsistent)} APIs with implementation inconsistencies (extra methods)"
            )

        if not recommendations:
            recommendations.append("🎉 ACHIEVEMENT: 100% API coverage validated for Wasmtime 36.0.2!")

        return recommendations

    def run_comprehensive_validation(self) -> Dict:
        """Run the complete API coverage validation."""
        print(f"🚀 Starting comprehensive API coverage validation for Wasmtime {self.wasmtime_version}")

        # Step 1: Discover all APIs
        discovered_apis = self.discover_all_api_classes()

        print(f"  Discovered {len(discovered_apis['core'])} core APIs")
        print(f"  Discovered {len(discovered_apis['jni'])} JNI implementations")
        print(f"  Discovered {len(discovered_apis['panama'])} Panama implementations")

        # Step 2: Validate coverage
        coverage_results = self.validate_wasmtime_api_coverage(discovered_apis)

        # Step 3: Generate summary
        summary = self.generate_coverage_summary(coverage_results)

        # Step 4: Compile final report
        final_report = {
            'metadata': {
                'validation_type': 'Final API Coverage Validation',
                'task': 'Task #310 - API Coverage Validation and Documentation',
                'wasmtime_version': self.wasmtime_version,
                'timestamp': summary['timestamp'],
                'validator_version': '1.0.0'
            },
            'summary': summary,
            'detailed_results': {
                api_key: {
                    'interface_name': result.interface_name,
                    'total_methods': result.total_methods,
                    'jni_implemented': result.jni_implemented,
                    'panama_implemented': result.panama_implemented,
                    'both_implemented': result.both_implemented,
                    'coverage_score': result.coverage_score,
                    'missing_jni': result.missing_jni,
                    'missing_panama': result.missing_panama,
                    'extra_jni': result.extra_jni,
                    'extra_panama': result.extra_panama
                }
                for api_key, result in coverage_results.items()
            },
            'discovered_apis': {
                'core_apis': [
                    {
                        'name': api.name,
                        'package': api.package,
                        'type': api.type,
                        'method_count': len(api.methods),
                        'extends': api.extends,
                        'implements': api.implements
                    }
                    for api in discovered_apis['core']
                ],
                'jni_implementations': [
                    {
                        'name': api.name,
                        'package': api.package,
                        'type': api.type,
                        'method_count': len(api.methods)
                    }
                    for api in discovered_apis['jni']
                ],
                'panama_implementations': [
                    {
                        'name': api.name,
                        'package': api.package,
                        'type': api.type,
                        'method_count': len(api.methods)
                    }
                    for api in discovered_apis['panama']
                ]
            }
        }

        return final_report

def main():
    """Main execution function."""
    base_path = os.getcwd()

    validator = FinalApiCoverageValidator(base_path)
    report = validator.run_comprehensive_validation()

    # Save the comprehensive report
    report_path = Path(base_path) / "docs" / "api-coverage-validation-final-report.json"
    report_path.parent.mkdir(parents=True, exist_ok=True)

    with open(report_path, 'w') as f:
        json.dump(report, f, indent=2)

    # Print summary
    summary = report['summary']
    print(f"\n🎯 Final API Coverage Validation Complete!")
    print(f"📊 Overall Coverage: {summary['overall_coverage_percentage']:.1f}%")
    print(f"🏆 Meets 100% Target: {summary['meets_100_percent_target']}")
    print(f"📈 APIs Analyzed: {summary['total_apis_analyzed']}")
    print(f"✅ Fully Covered: {summary['fully_covered_apis']}")
    print(f"⚠️  Partially Covered: {summary['partially_covered_apis']}")
    print(f"❌ Not Covered: {summary['not_covered_apis']}")
    print(f"📝 Report saved to: {report_path}")

    print(f"\n📋 Category Breakdown:")
    for category, data in summary['category_breakdown'].items():
        print(f"  {category}: {data['coverage_percentage']:.1f}% "
              f"({data['implemented_methods']}/{data['total_methods']} methods)")

    print(f"\n🔍 Top Coverage Gaps:")
    for gap in summary['top_gaps'][:5]:
        print(f"  {gap['priority'].upper()}: {gap['api']} ({gap['coverage_score']:.1f}%)")

    print(f"\n💡 Recommendations:")
    for rec in summary['recommendations']:
        print(f"  • {rec}")

if __name__ == "__main__":
    main()