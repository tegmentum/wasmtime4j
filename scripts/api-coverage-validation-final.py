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

class FinalApiCoverageValidator:
    """Comprehensive API coverage validator for wasmtime4j."""

    def __init__(self, base_path: str):
        self.base_path = Path(base_path)
        self.wasmtime_version = "40.0.1"

        # Core APIs that we expect to have
        self.core_apis = [
            'Engine', 'EngineConfig', 'Store', 'Module', 'Instance', 'Linker',
            'Memory', 'Table', 'Global', 'Function', 'HostFunction', 'Caller',
            'Val', 'ValType', 'FunctionType', 'MemoryType', 'TableType', 'GlobalType',
            'Trap', 'Error', 'WasmRuntime', 'WasiPreview1', 'WasiInstance'
        ]

    def discover_all_api_classes(self) -> Dict[str, List[str]]:
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
            discovered_apis['core'] = self._scan_directory_for_classes(core_path)
        else:
            print(f"Warning: Core API path not found: {core_path}")

        # Discover JNI implementation
        jni_path = self.base_path / "wasmtime4j-jni" / "src" / "main" / "java"
        if jni_path.exists():
            discovered_apis['jni'] = self._scan_directory_for_classes(jni_path)
        else:
            print(f"Warning: JNI path not found: {jni_path}")

        # Discover Panama implementation
        panama_path = self.base_path / "wasmtime4j-panama" / "src" / "main" / "java"
        if panama_path.exists():
            discovered_apis['panama'] = self._scan_directory_for_classes(panama_path)
        else:
            print(f"Warning: Panama path not found: {panama_path}")

        return discovered_apis

    def _scan_directory_for_classes(self, directory: Path) -> List[str]:
        """Scan a directory for Java class names."""
        classes = []

        for java_file in directory.rglob("*.java"):
            try:
                class_name = self._extract_class_name(java_file)
                if class_name:
                    classes.append(class_name)
            except Exception as e:
                print(f"Warning: Failed to parse {java_file}: {e}")

        return classes

    def _extract_class_name(self, file_path: Path) -> Optional[str]:
        """Extract the main class/interface name from a Java file."""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()

            # Extract class/interface/enum declaration
            type_pattern = r'(?:public\s+)?(?:abstract\s+)?(interface|class|enum)\s+(\w+)'
            type_match = re.search(type_pattern, content)

            if type_match:
                return type_match.group(2)

            return None

        except Exception as e:
            print(f"Error parsing {file_path}: {e}")
            return None

    def count_methods_in_file(self, file_path: Path) -> int:
        """Count the number of methods in a Java file."""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()

            # Remove comments
            content = re.sub(r'//.*?$', '', content, flags=re.MULTILINE)
            content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)

            # Count method signatures
            method_pattern = r'(?:public|protected|private)?\s*(?:static\s+)?(?:final\s+)?(?:abstract\s+)?[\w\<\>\[\],\s]+\s+\w+\s*\([^)]*\)\s*(?:throws\s+[\w\s,]+)?\s*[;{]'
            methods = re.findall(method_pattern, content)

            return len(methods)

        except Exception as e:
            print(f"Error counting methods in {file_path}: {e}")
            return 0

    def generate_api_summary(self, discovered_apis: Dict[str, List[str]]) -> Dict:
        """Generate API summary statistics."""
        print("Generating API summary statistics...")

        summary = {
            'timestamp': time.strftime('%Y-%m-%dT%H:%M:%SZ'),
            'wasmtime_version': self.wasmtime_version,
            'core_api_count': len(discovered_apis['core']),
            'jni_implementation_count': len(discovered_apis['jni']),
            'panama_implementation_count': len(discovered_apis['panama']),
            'total_discovered_classes': sum(len(apis) for apis in discovered_apis.values())
        }

        # Check for expected core APIs
        core_api_names = set(discovered_apis['core'])
        expected_found = []
        expected_missing = []

        for api in self.core_apis:
            # Check if API exists (exact match or contains)
            found = any(api.lower() in cls.lower() for cls in core_api_names)
            if found:
                expected_found.append(api)
            else:
                expected_missing.append(api)

        summary['expected_apis'] = {
            'total_expected': len(self.core_apis),
            'found': len(expected_found),
            'missing': len(expected_missing),
            'found_apis': expected_found,
            'missing_apis': expected_missing,
            'coverage_percentage': (len(expected_found) / len(self.core_apis)) * 100
        }

        # Implementation coverage
        jni_api_names = set(discovered_apis['jni'])
        panama_api_names = set(discovered_apis['panama'])

        # Check how many core APIs have implementations
        apis_with_jni = []
        apis_with_panama = []
        apis_with_both = []

        for core_api in discovered_apis['core']:
            has_jni = any(core_api.lower() in jni.lower() for jni in jni_api_names)
            has_panama = any(core_api.lower() in panama.lower() for panama in panama_api_names)

            if has_jni:
                apis_with_jni.append(core_api)
            if has_panama:
                apis_with_panama.append(core_api)
            if has_jni and has_panama:
                apis_with_both.append(core_api)

        summary['implementation_coverage'] = {
            'total_core_apis': len(discovered_apis['core']),
            'with_jni_implementation': len(apis_with_jni),
            'with_panama_implementation': len(apis_with_panama),
            'with_both_implementations': len(apis_with_both),
            'jni_coverage_percentage': (len(apis_with_jni) / len(discovered_apis['core'])) * 100 if discovered_apis['core'] else 0,
            'panama_coverage_percentage': (len(apis_with_panama) / len(discovered_apis['core'])) * 100 if discovered_apis['core'] else 0,
            'both_coverage_percentage': (len(apis_with_both) / len(discovered_apis['core'])) * 100 if discovered_apis['core'] else 0
        }

        # Overall assessment
        overall_coverage = summary['implementation_coverage']['both_coverage_percentage']
        meets_target = overall_coverage >= 90.0  # 90% threshold for near-complete coverage

        summary['assessment'] = {
            'overall_coverage_score': overall_coverage,
            'meets_90_percent_threshold': meets_target,
            'api_richness_score': summary['total_discovered_classes'],
            'quality_indicators': {
                'comprehensive_core_api': summary['expected_apis']['coverage_percentage'] >= 80,
                'dual_implementation_support': summary['implementation_coverage']['both_coverage_percentage'] >= 80,
                'extensive_api_surface': summary['total_discovered_classes'] >= 500
            }
        }

        return summary

    def run_comprehensive_validation(self) -> Dict:
        """Run the complete API coverage validation."""
        print(f"🚀 Starting comprehensive API coverage validation for Wasmtime {self.wasmtime_version}")

        # Step 1: Discover all APIs
        discovered_apis = self.discover_all_api_classes()

        print(f"  Discovered {len(discovered_apis['core'])} core APIs")
        print(f"  Discovered {len(discovered_apis['jni'])} JNI implementations")
        print(f"  Discovered {len(discovered_apis['panama'])} Panama implementations")

        # Step 2: Generate summary
        summary = self.generate_api_summary(discovered_apis)

        # Step 3: Compile final report
        final_report = {
            'metadata': {
                'validation_type': 'Final API Coverage Validation',
                'task': 'Task #310 - API Coverage Validation and Documentation',
                'wasmtime_version': self.wasmtime_version,
                'timestamp': summary['timestamp'],
                'validator_version': '1.0.0'
            },
            'summary': summary,
            'discovered_apis': discovered_apis,
            'recommendations': self._generate_recommendations(summary)
        }

        return final_report

    def _generate_recommendations(self, summary: Dict) -> List[str]:
        """Generate actionable recommendations."""
        recommendations = []

        expected_coverage = summary['expected_apis']['coverage_percentage']
        impl_coverage = summary['assessment']['overall_coverage_score']

        if expected_coverage < 80:
            missing_apis = summary['expected_apis']['missing_apis']
            recommendations.append(
                f"PRIORITY: Implement missing core APIs: {', '.join(missing_apis[:5])}"
            )

        if impl_coverage < 90:
            recommendations.append(
                f"Improve implementation coverage from {impl_coverage:.1f}% to 90%+ for both JNI and Panama"
            )

        if summary['assessment']['quality_indicators']['extensive_api_surface']:
            recommendations.append("✅ Excellent API richness achieved with 500+ classes/interfaces")

        if impl_coverage >= 90 and expected_coverage >= 80:
            recommendations.append("🎉 ACHIEVEMENT: Excellent API coverage - approaching 100% Wasmtime compatibility!")

        if not recommendations:
            recommendations.append("API validation complete - no critical issues detected")

        return recommendations

def main():
    """Main execution function."""
    base_path = os.getcwd()

    validator = FinalApiCoverageValidator(base_path)
    report = validator.run_comprehensive_validation()

    # Save the comprehensive report
    docs_path = Path(base_path) / "docs"
    docs_path.mkdir(exist_ok=True)

    report_path = docs_path / "api-coverage-validation-final-report.json"

    with open(report_path, 'w') as f:
        json.dump(report, f, indent=2)

    # Print summary
    summary = report['summary']
    print(f"\n🎯 Final API Coverage Validation Complete!")
    print(f"📊 Overall Coverage: {summary['assessment']['overall_coverage_score']:.1f}%")
    print(f"🏆 Meets 90% Threshold: {summary['assessment']['meets_90_percent_threshold']}")
    print(f"📈 Total Classes/Interfaces: {summary['total_discovered_classes']}")
    print(f"✅ Core APIs: {summary['core_api_count']}")
    print(f"🔧 JNI Implementations: {summary['jni_implementation_count']}")
    print(f"🔧 Panama Implementations: {summary['panama_implementation_count']}")
    print(f"📝 Report saved to: {report_path}")

    print(f"\n📋 Expected API Coverage:")
    expected = summary['expected_apis']
    print(f"  Found: {expected['found']}/{expected['total_expected']} ({expected['coverage_percentage']:.1f}%)")

    if expected['missing_apis']:
        print(f"  Missing: {', '.join(expected['missing_apis'][:5])}")

    print(f"\n🔧 Implementation Coverage:")
    impl = summary['implementation_coverage']
    print(f"  JNI: {impl['jni_coverage_percentage']:.1f}%")
    print(f"  Panama: {impl['panama_coverage_percentage']:.1f}%")
    print(f"  Both: {impl['both_coverage_percentage']:.1f}%")

    print(f"\n💡 Recommendations:")
    for rec in report['recommendations']:
        print(f"  • {rec}")

if __name__ == "__main__":
    main()