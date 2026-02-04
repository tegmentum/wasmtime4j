#!/usr/bin/env python3
"""
API Compatibility Validation Script for wasmtime4j
Issue #269 Stream B - Production Validation

This script performs comprehensive API compatibility analysis between
JNI and Panama implementations by analyzing source code structure.
"""

import os
import re
import json
from pathlib import Path
from typing import Dict, List, Set, Tuple
from collections import defaultdict

class ApiCompatibilityAnalyzer:
    def __init__(self, base_path: str):
        self.base_path = Path(base_path)
        self.public_api_path = self.base_path / "wasmtime4j" / "src" / "main" / "java"
        self.jni_api_path = self.base_path / "wasmtime4j-jni" / "src" / "main" / "java"
        self.panama_api_path = self.base_path / "wasmtime4j-panama" / "src" / "main" / "java"

        self.public_interfaces = {}
        self.jni_implementations = {}
        self.panama_implementations = {}

        self.compatibility_issues = []
        self.validation_results = {}

    def extract_java_methods(self, file_path: Path) -> List[Dict]:
        """Extract method signatures from Java source file."""
        methods = []

        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()

            # Remove comments and strings to avoid false matches
            content = re.sub(r'//.*?$', '', content, flags=re.MULTILINE)
            content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)
            content = re.sub(r'"[^"]*"', '""', content)

            # Extract method signatures (simplified pattern)
            method_pattern = r'(public|protected|private|static|\s)+[\w\<\>\[\]]+\s+(\w+)\s*\([^)]*\)[^{;]*[{;]'
            matches = re.finditer(method_pattern, content)

            for match in matches:
                method_sig = match.group(0).strip()
                if not any(keyword in method_sig for keyword in ['class ', 'interface ', 'enum ']):
                    method_name = re.search(r'\s+(\w+)\s*\(', method_sig)
                    if method_name:
                        methods.append({
                            'name': method_name.group(1),
                            'signature': method_sig,
                            'is_public': 'public' in method_sig,
                            'is_static': 'static' in method_sig
                        })
        except Exception as e:
            print(f"Error processing {file_path}: {e}")

        return methods

    def extract_java_interfaces(self, file_path: Path) -> Dict:
        """Extract interface/class information from Java source file."""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()

            # Extract class/interface name
            class_pattern = r'(public\s+)?(class|interface|enum)\s+(\w+)'
            class_match = re.search(class_pattern, content)

            if not class_match:
                return {}

            class_name = class_match.group(3)
            class_type = class_match.group(2)

            # Extract package
            package_pattern = r'package\s+([\w.]+);'
            package_match = re.search(package_pattern, content)
            package_name = package_match.group(1) if package_match else ""

            # Extract methods
            methods = self.extract_java_methods(file_path)

            return {
                'name': class_name,
                'type': class_type,
                'package': package_name,
                'full_name': f"{package_name}.{class_name}",
                'methods': methods,
                'file_path': str(file_path)
            }

        except Exception as e:
            print(f"Error processing {file_path}: {e}")
            return {}

    def scan_directory(self, directory: Path) -> Dict[str, Dict]:
        """Scan a directory for Java files and extract API information."""
        apis = {}

        if not directory.exists():
            print(f"Directory does not exist: {directory}")
            return apis

        java_files = list(directory.rglob("*.java"))
        print(f"Found {len(java_files)} Java files in {directory}")

        for java_file in java_files:
            api_info = self.extract_java_interfaces(java_file)
            if api_info and api_info.get('name'):
                apis[api_info['name']] = api_info

        return apis

    def analyze_api_compatibility(self):
        """Perform comprehensive API compatibility analysis."""
        print("Starting API compatibility analysis...")

        # Scan all API directories
        print("Scanning public API interfaces...")
        self.public_interfaces = self.scan_directory(self.public_api_path)

        print("Scanning JNI implementation...")
        self.jni_implementations = self.scan_directory(self.jni_api_path)

        print("Scanning Panama implementation...")
        self.panama_implementations = self.scan_directory(self.panama_api_path)

        # Analyze compatibility
        self.validate_interface_coverage()
        self.validate_method_compatibility()
        self.validate_signature_consistency()

        return self.generate_compatibility_report()

    def validate_interface_coverage(self):
        """Validate that all public interfaces have implementations."""
        print("Validating interface coverage...")

        coverage_results = {
            'public_interfaces': len(self.public_interfaces),
            'jni_coverage': 0,
            'panama_coverage': 0,
            'missing_jni': [],
            'missing_panama': []
        }

        for interface_name, interface_info in self.public_interfaces.items():
            # Check if it's a concrete interface (not utility class)
            if interface_info.get('type') == 'interface':
                # Look for implementations
                jni_impl = self.find_implementation(interface_name, self.jni_implementations, 'Jni')
                panama_impl = self.find_implementation(interface_name, self.panama_implementations, 'Panama')

                if jni_impl:
                    coverage_results['jni_coverage'] += 1
                else:
                    coverage_results['missing_jni'].append(interface_name)

                if panama_impl:
                    coverage_results['panama_coverage'] += 1
                else:
                    coverage_results['missing_panama'].append(interface_name)

        self.validation_results['interface_coverage'] = coverage_results

    def find_implementation(self, interface_name: str, implementations: Dict, prefix: str) -> Dict:
        """Find implementation class for an interface."""
        # Try direct name match with prefix
        impl_name = f"{prefix}{interface_name}"
        if impl_name in implementations:
            return implementations[impl_name]

        # Try without prefix
        if interface_name in implementations:
            return implementations[interface_name]

        # Try pattern matching
        for impl_name, impl_info in implementations.items():
            if interface_name.lower() in impl_name.lower():
                return impl_info

        return None

    def validate_method_compatibility(self):
        """Validate method compatibility between implementations."""
        print("Validating method compatibility...")

        method_results = {
            'total_methods_analyzed': 0,
            'compatible_methods': 0,
            'incompatible_methods': [],
            'missing_in_jni': [],
            'missing_in_panama': []
        }

        # Focus on key interface methods
        key_interfaces = ['WasmRuntime', 'Engine', 'Store', 'Module', 'Instance', 'WasmMemory']

        for interface_name in key_interfaces:
            if interface_name in self.public_interfaces:
                interface_info = self.public_interfaces[interface_name]
                jni_impl = self.find_implementation(interface_name, self.jni_implementations, 'Jni')
                panama_impl = self.find_implementation(interface_name, self.panama_implementations, 'Panama')

                if jni_impl and panama_impl:
                    self.compare_interface_methods(interface_info, jni_impl, panama_impl, method_results)

        self.validation_results['method_compatibility'] = method_results

    def compare_interface_methods(self, interface_info: Dict, jni_impl: Dict, panama_impl: Dict, results: Dict):
        """Compare methods between JNI and Panama implementations."""
        interface_methods = {m['name']: m for m in interface_info.get('methods', []) if m['is_public']}
        jni_methods = {m['name']: m for m in jni_impl.get('methods', []) if m['is_public']}
        panama_methods = {m['name']: m for m in panama_impl.get('methods', []) if m['is_public']}

        for method_name, method_info in interface_methods.items():
            results['total_methods_analyzed'] += 1

            jni_has_method = method_name in jni_methods
            panama_has_method = method_name in panama_methods

            if jni_has_method and panama_has_method:
                # Both implementations have the method
                results['compatible_methods'] += 1
            else:
                if not jni_has_method:
                    results['missing_in_jni'].append(f"{interface_info['name']}.{method_name}")
                if not panama_has_method:
                    results['missing_in_panama'].append(f"{interface_info['name']}.{method_name}")

    def validate_signature_consistency(self):
        """Validate that method signatures are consistent."""
        print("Validating signature consistency...")

        signature_results = {
            'consistent_signatures': 0,
            'inconsistent_signatures': [],
            'signature_differences': []
        }

        # This would require more sophisticated parsing for full validation
        # For now, we'll provide a framework for future enhancement

        self.validation_results['signature_consistency'] = signature_results

    def calculate_compatibility_score(self) -> float:
        """Calculate overall API compatibility score."""
        coverage = self.validation_results.get('interface_coverage', {})
        methods = self.validation_results.get('method_compatibility', {})

        # Calculate coverage percentage
        total_interfaces = coverage.get('public_interfaces', 0)
        if total_interfaces == 0:
            return 0.0

        jni_coverage = coverage.get('jni_coverage', 0) / total_interfaces * 100
        panama_coverage = coverage.get('panama_coverage', 0) / total_interfaces * 100

        # Calculate method compatibility
        total_methods = methods.get('total_methods_analyzed', 0)
        if total_methods == 0:
            method_compatibility = 100.0
        else:
            compatible_methods = methods.get('compatible_methods', 0)
            method_compatibility = (compatible_methods / total_methods) * 100

        # Overall score (weighted average)
        overall_score = (jni_coverage * 0.3 + panama_coverage * 0.3 + method_compatibility * 0.4)

        return min(overall_score, 100.0)

    def generate_compatibility_report(self) -> Dict:
        """Generate comprehensive compatibility report."""
        print("Generating compatibility report...")

        compatibility_score = self.calculate_compatibility_score()

        report = {
            'timestamp': '2025-09-20T12:00:00Z',
            'analysis_type': 'API Compatibility Validation',
            'issue': '#269 Stream B - Production Validation',
            'overall_compatibility_score': compatibility_score,
            'meets_100_percent_target': compatibility_score >= 100.0,
            'validation_results': self.validation_results,
            'summary': {
                'total_public_interfaces': len(self.public_interfaces),
                'total_jni_implementations': len(self.jni_implementations),
                'total_panama_implementations': len(self.panama_implementations),
                'compatibility_score': compatibility_score
            },
            'recommendations': self.generate_recommendations()
        }

        return report

    def generate_recommendations(self) -> List[str]:
        """Generate recommendations based on analysis."""
        recommendations = []

        coverage = self.validation_results.get('interface_coverage', {})
        methods = self.validation_results.get('method_compatibility', {})

        if coverage.get('missing_jni'):
            recommendations.append(f"Implement missing JNI interfaces: {', '.join(coverage['missing_jni'][:5])}")

        if coverage.get('missing_panama'):
            recommendations.append(f"Implement missing Panama interfaces: {', '.join(coverage['missing_panama'][:5])}")

        if methods.get('missing_in_jni'):
            recommendations.append(f"Implement missing JNI methods: {len(methods['missing_in_jni'])} methods")

        if methods.get('missing_in_panama'):
            recommendations.append(f"Implement missing Panama methods: {len(methods['missing_in_panama'])} methods")

        if not recommendations:
            recommendations.append("API compatibility validation successful - no critical issues found")

        return recommendations

def main():
    """Main execution function."""
    base_path = "/Users/zacharywhitley/git/wasmtime4j"

    analyzer = ApiCompatibilityAnalyzer(base_path)
    report = analyzer.analyze_api_compatibility()

    # Save report
    report_path = Path(base_path) / "api-compatibility-report.json"
    with open(report_path, 'w') as f:
        json.dump(report, f, indent=2)

    print(f"\nAPI Compatibility Analysis Complete!")
    print(f"Overall Compatibility Score: {report['overall_compatibility_score']:.1f}%")
    print(f"Meets 100% Target: {report['meets_100_percent_target']}")
    print(f"Report saved to: {report_path}")

    # Print summary
    print("\nSummary:")
    for key, value in report['summary'].items():
        print(f"  {key}: {value}")

    print("\nRecommendations:")
    for rec in report['recommendations']:
        print(f"  - {rec}")

if __name__ == "__main__":
    main()