# CI/CD Integration and Automation Reference

This document provides comprehensive guidance for integrating the wasmtime4j comparison test framework with CI/CD pipelines and setting up automated validation workflows.

## Overview

The CI/CD integration system provides:

- **Automated Compliance Validation**: Continuous validation against Wasmtime test suite
- **Performance Regression Detection**: Automated performance monitoring and alerting
- **Cross-Platform Validation**: Multi-platform test execution and reporting
- **Automated Reporting**: Dashboard generation and stakeholder notification
- **Issue Management**: Automated issue creation and tracking

## GitHub Actions Integration

### Core Workflows

#### 1. Main CI Pipeline (`.github/workflows/ci.yml`)

Enhanced CI pipeline with Wasmtime compliance integration:

```yaml
name: CI

on:
  push:
    branches: [ master, develop, 'epic/*', 'feature/*' ]
  pull_request:
    branches: [ master, develop ]

env:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository -Xmx1024m -XX:MaxPermSize=512m"
  WASMTIME4J_COMPARISON_TEST_SUITES: "smoke"
  WASMTIME4J_COMPARISON_RUNTIMES: "jni,panama"

jobs:
  build-and-test:
    name: Build and Test (${{ matrix.os }}, Java ${{ matrix.java }})
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        java: [8, 11, 17, 21, 23]
        include:
          - java: 23
            enable-panama: true

    steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Set up JDK ${{ matrix.java }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ matrix.java }}
        distribution: 'temurin'

    - name: Cache Maven dependencies
      uses: actions/cache@v4
      with:
        path: .m2/repository
        key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}

    - name: Install Wasmtime
      run: |
        if [[ "${{ runner.os }}" == "Linux" ]]; then
          curl -sSfL https://github.com/bytecodealliance/wasmtime/releases/download/v26.0.0/wasmtime-v26.0.0-x86_64-linux.tar.xz | tar -xJ
          sudo mv wasmtime-v26.0.0-x86_64-linux/wasmtime /usr/local/bin/
        elif [[ "${{ runner.os }}" == "macOS" ]]; then
          curl -sSfL https://github.com/bytecodealliance/wasmtime/releases/download/v26.0.0/wasmtime-v26.0.0-x86_64-macos.tar.xz | tar -xJ
          sudo mv wasmtime-v26.0.0-x86_64-macos/wasmtime /usr/local/bin/
        elif [[ "${{ runner.os }}" == "Windows" ]]; then
          curl -sSfL https://github.com/bytecodealliance/wasmtime/releases/download/v26.0.0/wasmtime-v26.0.0-x86_64-windows.zip -o wasmtime.zip
          unzip wasmtime.zip
          echo "${PWD}/wasmtime-v26.0.0-x86_64-windows" >> $GITHUB_PATH
        fi

    - name: Build project
      run: ./mvnw clean compile -B -V

    - name: Run unit tests
      run: ./mvnw test -B

    - name: Run comparison smoke tests
      run: ./mvnw test -P comparison-tests -Dwasmtime4j.comparison.test.suites=smoke

    - name: Generate comparison report
      if: always()
      run: ./mvnw exec:java -Dexec.mainClass="ai.tegmentum.wasmtime4j.comparison.ComparisonReportGenerator"

    - name: Upload comparison results
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: comparison-results-${{ matrix.os }}-java${{ matrix.java }}
        path: target/comparison-reports/

    - name: Comment PR with results
      if: github.event_name == 'pull_request'
      uses: actions/github-script@v7
      with:
        script: |
          const fs = require('fs');
          const path = 'target/comparison-reports/summary.json';

          if (fs.existsSync(path)) {
            const summary = JSON.parse(fs.readFileSync(path, 'utf8'));
            const comment = `
            ## Wasmtime Compliance Results 🔍

            **Platform**: ${{ matrix.os }} (Java ${{ matrix.java }})
            **Coverage**: ${summary.coverage}%
            **Performance**: ${summary.performance}
            **Status**: ${summary.status}

            [View detailed report](${summary.reportUrl})
            `;

            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: comment
            });
          }
```

#### 2. Wasmtime Compliance Validation (`.github/workflows/wasmtime-compliance.yml`)

Dedicated comprehensive compliance validation workflow:

```yaml
name: Wasmtime Compliance Validation

on:
  schedule:
    - cron: '0 3 * * *'  # Daily at 3 AM UTC
  workflow_dispatch:
    inputs:
      test_suite:
        description: 'Test suite to run'
        required: false
        default: 'smoke'
        type: choice
        options: [smoke, full, custom]
      target_runtimes:
        description: 'Target runtimes'
        required: false
        default: 'native,jni,panama'
      fail_on_regression:
        description: 'Fail build on performance regression'
        required: false
        default: 'true'
        type: boolean

env:
  WASMTIME_VERSION: "26.0.0"
  WASMTIME4J_COMPARISON_TEST_SUITES: ${{ github.event.inputs.test_suite || 'full' }}
  WASMTIME4J_COMPARISON_RUNTIMES: ${{ github.event.inputs.target_runtimes || 'native,jni,panama' }}

jobs:
  compliance-validation:
    name: Compliance Validation (${{ matrix.platform }})
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        include:
          - os: ubuntu-latest
            platform: linux-x86_64
          - os: macos-latest
            platform: macos-x86_64
          - os: windows-latest
            platform: windows-x86_64

    steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Set up Java 23
      uses: actions/setup-java@v4
      with:
        java-version: '23'
        distribution: 'temurin'

    - name: Download Wasmtime reference
      run: |
        # Download and install Wasmtime for comparison
        ./scripts/install-wasmtime.sh ${{ env.WASMTIME_VERSION }} ${{ matrix.platform }}

    - name: Download official Wasmtime test suite
      run: |
        # Download official test suite
        ./scripts/download-wasmtime-tests.sh ${{ env.WASMTIME_VERSION }}

    - name: Run comprehensive compliance tests
      run: |
        ./mvnw test -P compliance-tests \
          -Dwasmtime4j.comparison.test.suites=${{ env.WASMTIME4J_COMPARISON_TEST_SUITES }} \
          -Dwasmtime4j.comparison.runtimes=${{ env.WASMTIME4J_COMPARISON_RUNTIMES }} \
          -Dwasmtime4j.comparison.performance.enabled=true \
          -Dwasmtime4j.comparison.behavioral.strict.mode=true

    - name: Generate compliance dashboard
      run: |
        ./mvnw exec:java -Dexec.mainClass="ai.tegmentum.wasmtime4j.comparison.dashboard.DashboardGenerator" \
          -Dexec.args="--platform ${{ matrix.platform }} --output target/dashboard"

    - name: Check performance regressions
      if: github.event.inputs.fail_on_regression == 'true'
      run: |
        ./mvnw exec:java -Dexec.mainClass="ai.tegmentum.wasmtime4j.comparison.RegressionChecker" \
          -Dexec.args="--baseline baseline-${{ matrix.platform }}.json --current target/performance-results.json"

    - name: Upload compliance results
      uses: actions/upload-artifact@v4
      with:
        name: compliance-results-${{ matrix.platform }}
        path: |
          target/comparison-reports/
          target/dashboard/
          target/performance-results.json

    - name: Create issue on failure
      if: failure()
      uses: actions/github-script@v7
      with:
        script: |
          const title = `Compliance validation failed on ${{ matrix.platform }}`;
          const body = `
          Automated compliance validation failed for platform ${{ matrix.platform }}.

          **Workflow**: ${{ github.workflow }}
          **Run ID**: ${{ github.run_id }}
          **Platform**: ${{ matrix.platform }}
          **Test Suite**: ${{ env.WASMTIME4J_COMPARISON_TEST_SUITES }}
          **Runtimes**: ${{ env.WASMTIME4J_COMPARISON_RUNTIMES }}

          Please investigate the failure and address any compliance issues.

          [View workflow run](https://github.com/${{ github.repository }}/actions/runs/${{ github.run_id }})
          `;

          github.rest.issues.create({
            owner: context.repo.owner,
            repo: context.repo.repo,
            title: title,
            body: body,
            labels: ['compliance-failure', 'automated']
          });
```

#### 3. Performance Monitoring (`.github/workflows/performance.yml`)

Dedicated performance monitoring and baseline tracking:

```yaml
name: Performance Monitoring

on:
  push:
    branches: [master]
  schedule:
    - cron: '0 */6 * * *'  # Every 6 hours
  workflow_dispatch:

jobs:
  performance-benchmark:
    name: Performance Benchmark
    runs-on: ubuntu-latest

    steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Set up Java 23
      uses: actions/setup-java@v4
      with:
        java-version: '23'
        distribution: 'temurin'

    - name: Install Wasmtime
      run: ./scripts/install-wasmtime.sh 26.0.0 linux-x86_64

    - name: Run performance benchmarks
      run: |
        ./mvnw test -P performance-tests \
          -Dwasmtime4j.comparison.performance.benchmark.iterations=100 \
          -Dwasmtime4j.comparison.performance.statistical.confidence=99.0 \
          -Dwasmtime4j.comparison.runtime.warmup.iterations=20

    - name: Update performance baseline
      if: github.ref == 'refs/heads/master'
      run: |
        # Update baseline performance data
        cp target/performance-results.json scripts/baselines/performance-baseline-$(date +%Y%m%d).json

    - name: Detect performance regressions
      run: |
        ./mvnw exec:java -Dexec.mainClass="ai.tegmentum.wasmtime4j.comparison.RegressionDetector" \
          -Dexec.args="--threshold 5.0 --confidence 95.0"

    - name: Generate performance report
      run: |
        ./mvnw exec:java -Dexec.mainClass="ai.tegmentum.wasmtime4j.comparison.PerformanceReporter" \
          -Dexec.args="--format html --output target/performance-report.html"

    - name: Upload performance results
      uses: actions/upload-artifact@v4
      with:
        name: performance-results
        path: |
          target/performance-results.json
          target/performance-report.html
```

### Workflow Configuration

#### Environment Variables

**Global Environment Variables:**
```yaml
env:
  # Wasmtime configuration
  WASMTIME_VERSION: "26.0.0"
  WASMTIME_DOWNLOAD_URL: "https://github.com/bytecodealliance/wasmtime/releases/download"

  # Test configuration
  WASMTIME4J_COMPARISON_TEST_SUITES: "smoke,core"
  WASMTIME4J_COMPARISON_RUNTIMES: "jni,panama,native"
  WASMTIME4J_COMPARISON_PERFORMANCE_ENABLED: "true"

  # Reporting configuration
  WASMTIME4J_COMPARISON_REPORTING_FORMATS: "html,json"
  WASMTIME4J_COMPARISON_REPORTING_OUTPUT_DIRECTORY: "target/comparison-reports"

  # Performance settings
  WASMTIME4J_COMPARISON_PERFORMANCE_REGRESSION_THRESHOLD: "5.0"
  WASMTIME4J_COMPARISON_PERFORMANCE_STATISTICAL_CONFIDENCE: "95.0"
```

#### Secrets Configuration

**Required Secrets:**
```yaml
secrets:
  # GitHub token for API access
  GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

  # Optional: Performance tracking service
  PERFORMANCE_TRACKING_API_KEY: ${{ secrets.PERFORMANCE_TRACKING_API_KEY }}

  # Optional: Dashboard deployment
  DASHBOARD_DEPLOY_KEY: ${{ secrets.DASHBOARD_DEPLOY_KEY }}
```

## Jenkins Integration

### Pipeline Configuration

**Jenkinsfile for Complete Validation:**
```groovy
pipeline {
    agent any

    parameters {
        choice(
            name: 'TEST_SUITE',
            choices: ['smoke', 'core', 'full', 'performance'],
            description: 'Test suite to execute'
        )
        booleanParam(
            name: 'PERFORMANCE_ANALYSIS',
            defaultValue: true,
            description: 'Enable performance analysis'
        )
        choice(
            name: 'TARGET_PLATFORMS',
            choices: ['linux', 'windows', 'macos', 'all'],
            description: 'Target platforms for testing'
        )
    }

    environment {
        WASMTIME_VERSION = '26.0.0'
        MAVEN_OPTS = '-Xmx2048m -XX:MaxPermSize=512m'
        WASMTIME4J_COMPARISON_TEST_SUITES = "${params.TEST_SUITE}"
        WASMTIME4J_COMPARISON_PERFORMANCE_ENABLED = "${params.PERFORMANCE_ANALYSIS}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Setup Environment') {
            parallel {
                stage('Install Java') {
                    steps {
                        script {
                            // Install multiple Java versions
                            sh '''
                                # Install Java 8, 11, 17, 21, 23
                                ./scripts/install-java-versions.sh
                            '''
                        }
                    }
                }

                stage('Install Wasmtime') {
                    steps {
                        script {
                            sh '''
                                # Install Wasmtime for comparison
                                ./scripts/install-wasmtime.sh ${WASMTIME_VERSION} ${NODE_NAME}
                            '''
                        }
                    }
                }
            }
        }

        stage('Build and Test') {
            matrix {
                axes {
                    axis {
                        name 'JAVA_VERSION'
                        values '8', '11', '17', '21', '23'
                    }
                    axis {
                        name 'PLATFORM'
                        values 'linux', 'windows', 'macos'
                    }
                }
                when {
                    anyOf {
                        expression { params.TARGET_PLATFORMS == 'all' }
                        expression { params.TARGET_PLATFORMS == env.PLATFORM }
                    }
                }
                stages {
                    stage('Test') {
                        steps {
                            script {
                                withEnv(["JAVA_HOME=/opt/java/${JAVA_VERSION}"]) {
                                    sh '''
                                        # Run comparison tests
                                        ./mvnw clean test -P comparison-tests \
                                            -Dwasmtime4j.comparison.test.suites=${WASMTIME4J_COMPARISON_TEST_SUITES} \
                                            -Dwasmtime4j.comparison.performance.enabled=${WASMTIME4J_COMPARISON_PERFORMANCE_ENABLED}
                                    '''
                                }
                            }
                        }
                        post {
                            always {
                                archiveArtifacts artifacts: 'target/comparison-reports/**', fingerprint: true
                                publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                            }
                        }
                    }
                }
            }
        }

        stage('Generate Dashboard') {
            steps {
                script {
                    sh '''
                        # Generate comprehensive dashboard
                        ./mvnw exec:java -Dexec.mainClass="ai.tegmentum.wasmtime4j.comparison.dashboard.DashboardGenerator" \
                            -Dexec.args="--aggregate --output target/dashboard"
                    '''
                }
            }
            post {
                always {
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/dashboard',
                        reportFiles: 'index.html',
                        reportName: 'Comparison Dashboard'
                    ])
                }
            }
        }

        stage('Performance Analysis') {
            when {
                expression { params.PERFORMANCE_ANALYSIS }
            }
            steps {
                script {
                    sh '''
                        # Run performance regression analysis
                        ./mvnw exec:java -Dexec.mainClass="ai.tegmentum.wasmtime4j.comparison.RegressionAnalyzer" \
                            -Dexec.args="--baseline performance-baseline.json --threshold 5.0"
                    '''
                }
            }
        }
    }

    post {
        always {
            // Archive all artifacts
            archiveArtifacts artifacts: 'target/**/*.json', fingerprint: true
            archiveArtifacts artifacts: 'target/**/*.html', fingerprint: true
        }

        failure {
            script {
                // Create JIRA ticket on failure
                def issue = [
                    fields: [
                        project: [key: 'WASMTIME4J'],
                        summary: "Comparison test failure - Build ${env.BUILD_NUMBER}",
                        description: "Automated comparison test failed. See ${env.BUILD_URL} for details.",
                        issuetype: [name: 'Bug'],
                        priority: [name: 'High']
                    ]
                ]
                jiraCreateIssue issue: issue
            }
        }

        success {
            script {
                if (params.TEST_SUITE == 'full') {
                    // Send success notification for full test runs
                    slackSend(
                        color: 'good',
                        message: "✅ Full Wasmtime compliance validation passed - Build ${env.BUILD_NUMBER}"
                    )
                }
            }
        }
    }
}
```

## GitLab CI Integration

### GitLab CI Configuration

**.gitlab-ci.yml:**
```yaml
stages:
  - build
  - test
  - performance
  - report
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"
  WASMTIME_VERSION: "26.0.0"
  WASMTIME4J_COMPARISON_TEST_SUITES: "smoke,core"

# Cache Maven dependencies
cache:
  paths:
    - .m2/repository/

# Build stage
build:
  stage: build
  image: openjdk:23-jdk
  script:
    - ./mvnw clean compile -B
  artifacts:
    paths:
      - target/
    expire_in: 1 hour

# Test matrix
.test_template: &test_template
  stage: test
  image: openjdk:23-jdk
  before_script:
    - apt-get update && apt-get install -y curl
    - ./scripts/install-wasmtime.sh $WASMTIME_VERSION linux-x86_64
  script:
    - ./mvnw test -P comparison-tests -Dwasmtime4j.comparison.test.suites=$TEST_SUITE
  artifacts:
    paths:
      - target/comparison-reports/
    reports:
      junit: target/surefire-reports/*.xml
    expire_in: 1 week

test:smoke:
  <<: *test_template
  variables:
    TEST_SUITE: "smoke"

test:core:
  <<: *test_template
  variables:
    TEST_SUITE: "core"

test:advanced:
  <<: *test_template
  variables:
    TEST_SUITE: "advanced"
  only:
    - master
    - develop

# Performance testing
performance:
  stage: performance
  image: openjdk:23-jdk
  script:
    - ./mvnw test -P performance-tests
    - ./mvnw exec:java -Dexec.mainClass="ai.tegmentum.wasmtime4j.comparison.PerformanceAnalyzer"
  artifacts:
    paths:
      - target/performance-results.json
    expire_in: 1 month
  only:
    - master

# Generate reports
generate_dashboard:
  stage: report
  image: openjdk:23-jdk
  script:
    - ./mvnw exec:java -Dexec.mainClass="ai.tegmentum.wasmtime4j.comparison.dashboard.DashboardGenerator"
  artifacts:
    paths:
      - target/dashboard/
    expire_in: 1 month
  dependencies:
    - test:smoke
    - test:core
    - test:advanced

# Deploy dashboard
deploy_dashboard:
  stage: deploy
  image: alpine:latest
  before_script:
    - apk add --no-cache rsync openssh
  script:
    - rsync -avz target/dashboard/ $DEPLOY_USER@$DEPLOY_HOST:/var/www/comparison-dashboard/
  only:
    - master
  environment:
    name: production
    url: https://comparison-dashboard.example.com
```

## Azure DevOps Integration

### Azure Pipelines Configuration

**azure-pipelines.yml:**
```yaml
trigger:
  branches:
    include:
      - master
      - develop
      - feature/*

pr:
  branches:
    include:
      - master
      - develop

pool:
  vmImage: 'ubuntu-latest'

variables:
  MAVEN_CACHE_FOLDER: $(Pipeline.Workspace)/.m2/repository
  WASMTIME_VERSION: '26.0.0'

stages:
- stage: Build
  jobs:
  - job: BuildAndTest
    strategy:
      matrix:
        Java8:
          javaVersion: '1.8'
        Java11:
          javaVersion: '1.11'
        Java17:
          javaVersion: '1.17'
        Java21:
          javaVersion: '1.21'
        Java23:
          javaVersion: '1.23'

    steps:
    - task: JavaToolInstaller@0
      inputs:
        versionSpec: $(javaVersion)
        jdkArchitectureOption: 'x64'
        jdkSourceOption: 'PreInstalled'

    - task: Cache@2
      inputs:
        key: 'maven | "$(Agent.OS)" | **/pom.xml'
        restoreKeys: |
          maven | "$(Agent.OS)"
          maven
        path: $(MAVEN_CACHE_FOLDER)
      displayName: Cache Maven local repo

    - script: |
        # Install Wasmtime
        curl -sSfL https://github.com/bytecodealliance/wasmtime/releases/download/v$(WASMTIME_VERSION)/wasmtime-v$(WASMTIME_VERSION)-x86_64-linux.tar.xz | tar -xJ
        sudo mv wasmtime-v$(WASMTIME_VERSION)-x86_64-linux/wasmtime /usr/local/bin/
      displayName: 'Install Wasmtime'

    - task: Maven@3
      inputs:
        mavenPomFile: 'pom.xml'
        goals: 'clean compile'
        options: '-Dmaven.repo.local=$(MAVEN_CACHE_FOLDER)'
      displayName: 'Build project'

    - task: Maven@3
      inputs:
        mavenPomFile: 'pom.xml'
        goals: 'test'
        options: '-Dmaven.repo.local=$(MAVEN_CACHE_FOLDER) -P comparison-tests'
      displayName: 'Run comparison tests'

    - task: PublishTestResults@2
      inputs:
        testResultsFormat: 'JUnit'
        testResultsFiles: '**/surefire-reports/*.xml'
        testRunTitle: 'Comparison Tests (Java $(javaVersion))'
      condition: always()

    - task: PublishBuildArtifacts@1
      inputs:
        pathToPublish: 'target/comparison-reports'
        artifactName: 'comparison-reports-java$(javaVersion)'
      condition: always()

- stage: Performance
  dependsOn: Build
  condition: and(succeeded(), eq(variables['Build.SourceBranch'], 'refs/heads/master'))
  jobs:
  - job: PerformanceAnalysis
    steps:
    - task: JavaToolInstaller@0
      inputs:
        versionSpec: '1.23'
        jdkArchitectureOption: 'x64'
        jdkSourceOption: 'PreInstalled'

    - script: |
        # Install Wasmtime
        curl -sSfL https://github.com/bytecodealliance/wasmtime/releases/download/v$(WASMTIME_VERSION)/wasmtime-v$(WASMTIME_VERSION)-x86_64-linux.tar.xz | tar -xJ
        sudo mv wasmtime-v$(WASMTIME_VERSION)-x86_64-linux/wasmtime /usr/local/bin/
      displayName: 'Install Wasmtime'

    - task: Maven@3
      inputs:
        mavenPomFile: 'pom.xml'
        goals: 'test'
        options: '-P performance-tests -Dwasmtime4j.comparison.performance.benchmark.iterations=50'
      displayName: 'Run performance benchmarks'

    - task: PublishBuildArtifacts@1
      inputs:
        pathToPublish: 'target/performance-results.json'
        artifactName: 'performance-results'
```

## Docker Integration

### Dockerfile for CI/CD

**Dockerfile.ci:**
```dockerfile
FROM openjdk:23-jdk-slim

# Install dependencies
RUN apt-get update && \
    apt-get install -y curl unzip && \
    rm -rf /var/lib/apt/lists/*

# Install Wasmtime
ARG WASMTIME_VERSION=26.0.0
RUN curl -sSfL https://github.com/bytecodealliance/wasmtime/releases/download/v${WASMTIME_VERSION}/wasmtime-v${WASMTIME_VERSION}-x86_64-linux.tar.xz | tar -xJ && \
    mv wasmtime-v${WASMTIME_VERSION}-x86_64-linux/wasmtime /usr/local/bin/ && \
    rm -rf wasmtime-v${WASMTIME_VERSION}-x86_64-linux

# Set working directory
WORKDIR /app

# Copy Maven wrapper and dependencies
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn/

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src/
COPY wasmtime4j-* ./

# Set environment variables
ENV WASMTIME4J_COMPARISON_TEST_SUITES=smoke,core
ENV WASMTIME4J_COMPARISON_RUNTIMES=jni,panama,native
ENV WASMTIME4J_COMPARISON_PERFORMANCE_ENABLED=true

# Run tests and generate reports
CMD ["./mvnw", "test", "-P", "comparison-tests"]
```

### Docker Compose for CI/CD

**docker-compose.ci.yml:**
```yaml
version: '3.8'

services:
  comparison-tests:
    build:
      context: .
      dockerfile: Dockerfile.ci
    environment:
      - WASMTIME4J_COMPARISON_TEST_SUITES=smoke,core,advanced
      - WASMTIME4J_COMPARISON_PERFORMANCE_ENABLED=true
      - WASMTIME4J_COMPARISON_REPORTING_FORMATS=html,json
    volumes:
      - ./target:/app/target
      - ./reports:/app/reports

  performance-analysis:
    build:
      context: .
      dockerfile: Dockerfile.ci
    environment:
      - WASMTIME4J_COMPARISON_TEST_SUITES=performance
      - WASMTIME4J_COMPARISON_PERFORMANCE_BENCHMARK_ITERATIONS=100
    volumes:
      - ./target:/app/target
      - ./performance-data:/app/performance-data
    depends_on:
      - comparison-tests

  dashboard-generator:
    build:
      context: .
      dockerfile: Dockerfile.ci
    command: ["./mvnw", "exec:java", "-Dexec.mainClass=ai.tegmentum.wasmtime4j.comparison.dashboard.DashboardGenerator"]
    volumes:
      - ./target:/app/target
      - ./dashboard:/app/dashboard
    depends_on:
      - comparison-tests
      - performance-analysis
```

## Automation Scripts

### Installation Script

**scripts/install-wasmtime.sh:**
```bash
#!/bin/bash
set -euo pipefail

WASMTIME_VERSION="${1:-26.0.0}"
PLATFORM="${2:-linux-x86_64}"

echo "Installing Wasmtime v${WASMTIME_VERSION} for ${PLATFORM}..."

case "${PLATFORM}" in
  "linux-x86_64")
    URL="https://github.com/bytecodealliance/wasmtime/releases/download/v${WASMTIME_VERSION}/wasmtime-v${WASMTIME_VERSION}-x86_64-linux.tar.xz"
    curl -sSfL "${URL}" | tar -xJ
    sudo mv "wasmtime-v${WASMTIME_VERSION}-x86_64-linux/wasmtime" /usr/local/bin/
    ;;
  "macos-x86_64")
    URL="https://github.com/bytecodealliance/wasmtime/releases/download/v${WASMTIME_VERSION}/wasmtime-v${WASMTIME_VERSION}-x86_64-macos.tar.xz"
    curl -sSfL "${URL}" | tar -xJ
    sudo mv "wasmtime-v${WASMTIME_VERSION}-x86_64-macos/wasmtime" /usr/local/bin/
    ;;
  "windows-x86_64")
    URL="https://github.com/bytecodealliance/wasmtime/releases/download/v${WASMTIME_VERSION}/wasmtime-v${WASMTIME_VERSION}-x86_64-windows.zip"
    curl -sSfL "${URL}" -o wasmtime.zip
    unzip wasmtime.zip
    mv "wasmtime-v${WASMTIME_VERSION}-x86_64-windows/wasmtime.exe" /usr/local/bin/
    ;;
  *)
    echo "Unsupported platform: ${PLATFORM}"
    exit 1
    ;;
esac

echo "Wasmtime installed successfully!"
wasmtime --version
```

### Test Suite Download Script

**scripts/download-wasmtime-tests.sh:**
```bash
#!/bin/bash
set -euo pipefail

WASMTIME_VERSION="${1:-26.0.0}"
TEST_DIR="${2:-target/wasmtime-tests}"

echo "Downloading Wasmtime test suite v${WASMTIME_VERSION}..."

# Create test directory
mkdir -p "${TEST_DIR}"

# Download test suite
curl -sSfL "https://github.com/bytecodealliance/wasmtime/archive/v${WASMTIME_VERSION}.tar.gz" | \
  tar -xz -C "${TEST_DIR}" --strip-components=1

echo "Test suite downloaded to ${TEST_DIR}"
```

### Dashboard Deployment Script

**scripts/deploy-dashboard.sh:**
```bash
#!/bin/bash
set -euo pipefail

DASHBOARD_DIR="${1:-target/dashboard}"
DEPLOY_HOST="${2:-dashboard.example.com}"
DEPLOY_PATH="${3:-/var/www/comparison-dashboard}"

echo "Deploying dashboard to ${DEPLOY_HOST}:${DEPLOY_PATH}..."

# Generate dashboard
./mvnw exec:java -Dexec.mainClass="ai.tegmentum.wasmtime4j.comparison.dashboard.DashboardGenerator" \
  -Dexec.args="--output ${DASHBOARD_DIR}"

# Deploy via rsync
rsync -avz --delete "${DASHBOARD_DIR}/" "${DEPLOY_HOST}:${DEPLOY_PATH}/"

echo "Dashboard deployed successfully!"
```

## Monitoring and Alerting

### Performance Monitoring

**Performance Alert Configuration:**
```yaml
# .github/workflows/performance-alerts.yml
name: Performance Alerts

on:
  workflow_run:
    workflows: ["Performance Monitoring"]
    types: [completed]

jobs:
  check-regressions:
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    runs-on: ubuntu-latest

    steps:
    - name: Download performance results
      uses: actions/download-artifact@v4
      with:
        name: performance-results

    - name: Check for regressions
      run: |
        python scripts/check-performance-regressions.py \
          --current performance-results.json \
          --baseline baseline/performance-baseline.json \
          --threshold 5.0

    - name: Create alert on regression
      if: failure()
      uses: actions/github-script@v7
      with:
        script: |
          const title = 'Performance regression detected';
          const body = `
          Automated performance monitoring detected a regression.

          **Workflow**: ${{ github.event.workflow_run.name }}
          **Run ID**: ${{ github.event.workflow_run.id }}
          **Threshold**: 5.0%

          Please investigate and address the performance regression.
          `;

          github.rest.issues.create({
            owner: context.repo.owner,
            repo: context.repo.repo,
            title: title,
            body: body,
            labels: ['performance-regression', 'urgent']
          });
```

This comprehensive CI/CD integration and automation reference enables effective deployment and monitoring of the comparison test framework across all major CI/CD platforms while ensuring continuous validation and performance monitoring.