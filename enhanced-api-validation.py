#!/usr/bin/env python3
"""
Enhanced API Compatibility Validation for wasmtime4j
Issue #269 Stream B - Production Validation

This script performs in-depth API compatibility analysis by examining
interface implementations and method signatures.
"""

import os
import re
import json
from pathlib import Path
from typing import Dict, List, Set, Tuple

class EnhancedApiValidator:
    def __init__(self, base_path: str):
        self.base_path = Path(base_path)
        self.core_interfaces = [
            'Engine', 'Store', 'Module', 'Instance', 'WasmMemory',
            'WasmTable', 'WasmGlobal', 'HostFunction', 'Linker',
            'WasmRuntime', 'WasiPreview1', 'WasiInstance'
        ]

        self.implementation_analysis = {}
        self.compatibility_results = {}

    def find_interface_implementations(self):
        """Find all classes that implement core interfaces."""
        print("Analyzing interface implementations...")

        for interface_name in self.core_interfaces:
            self.implementation_analysis[interface_name] = {
                'interface_file': None,
                'jni_implementation': None,
                'panama_implementation': None,
                'interface_methods': [],
                'jni_methods': [],
                'panama_methods': [],
                'compatibility_score': 0
            }

            # Find interface definition
            interface_file = self.find_file_by_name(f"{interface_name}.java", "wasmtime4j/src/main")
            if interface_file:
                self.implementation_analysis[interface_name]['interface_file'] = str(interface_file)
                self.implementation_analysis[interface_name]['interface_methods'] = self.extract_interface_methods(interface_file)

            # Find JNI implementation
            jni_impl = self.find_implementation_file(interface_name, "wasmtime4j-jni/src/main", "Jni")
            if jni_impl:
                self.implementation_analysis[interface_name]['jni_implementation'] = str(jni_impl)
                self.implementation_analysis[interface_name]['jni_methods'] = self.extract_implementation_methods(jni_impl)

            # Find Panama implementation
            panama_impl = self.find_implementation_file(interface_name, "wasmtime4j-panama/src/main", "Panama")
            if panama_impl:
                self.implementation_analysis[interface_name]['panama_implementation'] = str(panama_impl)
                self.implementation_analysis[interface_name]['panama_methods'] = self.extract_implementation_methods(panama_impl)

    def find_file_by_name(self, filename: str, base_dir: str) -> Path:
        """Find a file by name in a directory tree."""
        search_path = self.base_path / base_dir
        if search_path.exists():
            for file_path in search_path.rglob(filename):
                return file_path
        return None

    def find_implementation_file(self, interface_name: str, base_dir: str, prefix: str) -> Path:
        """Find implementation file for an interface."""
        search_path = self.base_path / base_dir

        # Try different naming patterns
        patterns = [
            f"{prefix}{interface_name}.java",
            f"{interface_name}.java",  # Some implementations might use direct names
        ]

        for pattern in patterns:
            for file_path in search_path.rglob(pattern):
                # Verify it's actually an implementation by checking for "implements"
                if self.verify_implementation(file_path, interface_name):
                    return file_path

        return None

    def verify_implementation(self, file_path: Path, interface_name: str) -> bool:
        """Verify that a file actually implements the given interface."""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()

            # Check for implements clause
            implements_pattern = rf'implements\s+.*{interface_name}'
            if re.search(implements_pattern, content):
                return True

            # Also check imports to see if the interface is used
            import_pattern = rf'import\s+.*{interface_name};'
            if re.search(import_pattern, content):
                return True

        except Exception as e:
            print(f"Error reading {file_path}: {e}")

        return False

    def extract_interface_methods(self, file_path: Path) -> List[Dict]:
        """Extract method signatures from interface file."""
        methods = []
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()

            # Remove comments
            content = re.sub(r'//.*?$', '', content, flags=re.MULTILINE)
            content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)

            # Extract method signatures from interface
            # Look for public method declarations (typically in interfaces)
            method_pattern = r'(?:public\s+)?(?:static\s+)?(?:default\s+)?[\w\<\>\[\],\s]+\s+(\w+)\s*\([^)]*\)(?:\s*throws\s+[\w\s,]+)?;'

            for match in re.finditer(method_pattern, content):
                method_name = match.group(1)
                if not method_name in ['class', 'interface', 'enum']:  # Skip type declarations
                    methods.append({
                        'name': method_name,
                        'signature': match.group(0).strip()
                    })

        except Exception as e:
            print(f"Error extracting interface methods from {file_path}: {e}")

        return methods

    def extract_implementation_methods(self, file_path: Path) -> List[Dict]:
        """Extract method signatures from implementation file."""
        methods = []
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()

            # Remove comments
            content = re.sub(r'//.*?$', '', content, flags=re.MULTILINE)
            content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)

            # Extract public method implementations
            method_pattern = r'@Override\s+public\s+[\w\<\>\[\],\s]+\s+(\w+)\s*\([^)]*\)\s*(?:throws\s+[\w\s,]+)?\s*\{'

            for match in re.finditer(method_pattern, content):
                method_name = match.group(1)
                methods.append({
                    'name': method_name,
                    'signature': match.group(0).strip(),
                    'is_override': True
                })

            # Also check for public methods without @Override
            method_pattern2 = r'public\s+[\w\<\>\[\],\s]+\s+(\w+)\s*\([^)]*\)\s*(?:throws\s+[\w\s,]+)?\s*\{'

            for match in re.finditer(method_pattern2, content):
                method_name = match.group(1)
                if not any(m['name'] == method_name for m in methods):  # Avoid duplicates
                    methods.append({
                        'name': method_name,
                        'signature': match.group(0).strip(),
                        'is_override': False
                    })

        except Exception as e:
            print(f"Error extracting implementation methods from {file_path}: {e}")

        return methods

    def calculate_method_compatibility(self, interface_name: str) -> Dict:
        """Calculate method compatibility for an interface."""
        analysis = self.implementation_analysis[interface_name]

        interface_methods = {m['name'] for m in analysis['interface_methods']}
        jni_methods = {m['name'] for m in analysis['jni_methods']}
        panama_methods = {m['name'] for m in analysis['panama_methods']}

        if not interface_methods:
            return {
                'total_methods': 0,
                'jni_coverage': 0,
                'panama_coverage': 0,
                'both_implemented': 0,
                'missing_in_jni': [],
                'missing_in_panama': [],
                'score': 0
            }

        # Calculate coverage
        jni_covered = interface_methods.intersection(jni_methods)
        panama_covered = interface_methods.intersection(panama_methods)
        both_covered = jni_covered.intersection(panama_covered)

        missing_jni = list(interface_methods - jni_methods)
        missing_panama = list(interface_methods - panama_methods)

        jni_coverage_pct = len(jni_covered) / len(interface_methods) * 100
        panama_coverage_pct = len(panama_covered) / len(interface_methods) * 100
        both_coverage_pct = len(both_covered) / len(interface_methods) * 100

        return {
            'total_methods': len(interface_methods),
            'jni_coverage': jni_coverage_pct,
            'panama_coverage': panama_coverage_pct,
            'both_implemented': both_coverage_pct,
            'missing_in_jni': missing_jni,
            'missing_in_panama': missing_panama,
            'score': both_coverage_pct
        }

    def run_comprehensive_analysis(self) -> Dict:
        """Run comprehensive API compatibility analysis."""
        print("Starting enhanced API compatibility analysis...")

        # Analyze implementations
        self.find_interface_implementations()

        # Calculate compatibility for each interface
        total_score = 0
        interfaces_with_impl = 0
        detailed_results = {}

        for interface_name in self.core_interfaces:
            analysis = self.implementation_analysis[interface_name]

            has_interface = analysis['interface_file'] is not None
            has_jni = analysis['jni_implementation'] is not None
            has_panama = analysis['panama_implementation'] is not None

            if has_interface and (has_jni or has_panama):
                method_compat = self.calculate_method_compatibility(interface_name)
                analysis['compatibility_score'] = method_compat['score']
                detailed_results[interface_name] = method_compat

                total_score += method_compat['score']
                interfaces_with_impl += 1
            else:
                analysis['compatibility_score'] = 0
                detailed_results[interface_name] = {
                    'total_methods': 0,
                    'jni_coverage': 0,
                    'panama_coverage': 0,
                    'both_implemented': 0,
                    'missing_in_jni': ['Interface or implementation not found'],
                    'missing_in_panama': ['Interface or implementation not found'],
                    'score': 0
                }

        # Calculate overall compatibility score
        overall_score = total_score / len(self.core_interfaces) if self.core_interfaces else 0

        # Generate summary
        summary = {
            'timestamp': '2025-09-20T12:00:00Z',
            'analysis_type': 'Enhanced API Compatibility Validation',
            'issue': '#269 Stream B - Production Validation',
            'overall_compatibility_score': overall_score,
            'meets_100_percent_target': overall_score >= 100.0,
            'interfaces_analyzed': len(self.core_interfaces),
            'interfaces_with_implementations': interfaces_with_impl,
            'detailed_analysis': self.implementation_analysis,
            'method_compatibility': detailed_results,
            'recommendations': self.generate_recommendations(detailed_results)
        }

        return summary

    def generate_recommendations(self, detailed_results: Dict) -> List[str]:
        """Generate specific recommendations based on analysis."""
        recommendations = []

        critical_missing = []
        minor_gaps = []

        for interface_name, results in detailed_results.items():
            score = results['score']
            if score == 0:
                critical_missing.append(interface_name)
            elif score < 100:
                missing_jni = results.get('missing_in_jni', [])
                missing_panama = results.get('missing_in_panama', [])
                if missing_jni or missing_panama:
                    minor_gaps.append(f"{interface_name} (JNI: {len(missing_jni)}, Panama: {len(missing_panama)} missing methods)")

        if critical_missing:
            recommendations.append(f"CRITICAL: Implement missing interfaces: {', '.join(critical_missing[:3])}")

        if minor_gaps:
            recommendations.append(f"Complete method implementations for: {', '.join(minor_gaps[:3])}")

        if not critical_missing and not minor_gaps:
            recommendations.append("API compatibility validation successful - all core interfaces implemented")

        return recommendations

def main():
    """Main execution function."""
    base_path = "/Users/zacharywhitley/git/wasmtime4j"

    validator = EnhancedApiValidator(base_path)
    report = validator.run_comprehensive_analysis()

    # Save detailed report
    report_path = Path(base_path) / "enhanced-api-compatibility-report.json"
    with open(report_path, 'w') as f:
        json.dump(report, f, indent=2)

    print(f"\nEnhanced API Compatibility Analysis Complete!")
    print(f"Overall Compatibility Score: {report['overall_compatibility_score']:.1f}%")
    print(f"Meets 100% Target: {report['meets_100_percent_target']}")
    print(f"Interfaces Analyzed: {report['interfaces_analyzed']}")
    print(f"Interfaces with Implementations: {report['interfaces_with_implementations']}")
    print(f"Report saved to: {report_path}")

    print("\nCore Interface Analysis:")
    for interface_name, analysis in report['detailed_analysis'].items():
        has_interface = analysis['interface_file'] is not None
        has_jni = analysis['jni_implementation'] is not None
        has_panama = analysis['panama_implementation'] is not None
        score = analysis['compatibility_score']

        status = "✅" if score == 100 else "⚠️" if score > 0 else "❌"
        print(f"  {status} {interface_name}: Interface={has_interface}, JNI={has_jni}, Panama={has_panama}, Score={score:.1f}%")

    print("\nRecommendations:")
    for rec in report['recommendations']:
        print(f"  - {rec}")

if __name__ == "__main__":
    main()