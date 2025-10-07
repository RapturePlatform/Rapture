# Modernization Plan for the Rapture Platform

This document outlines recommended steps to bring the Rapture platform up to modern Java and infrastructure best practices while preserving the existing functionality. The proposals are grouped to allow staged adoption.

## 1. Update the Build and Dependency Toolchain

* **Upgrade Gradle and the build plugins.** The current root build uses Gradle 3.2.1 and legacy plugins such as the `maven` plugin and an early build scan version.【F:Apps/build.gradle†L23-L56】 Move to a recent Gradle LTS (8.x) and migrate to the modern `maven-publish` and `java-library` plugins. This will also enable configuration caching, version catalogs, and other modern features.
* **Adopt dependency management best practices.** Replace the hard-coded dependency versions scattered across subprojects with a Gradle version catalog or dependency constraints, and remove the deprecated `compile` and `testCompile` configurations in favor of `implementation` and `testImplementation`.
* **Introduce automated dependency updates.** Configure Renovate or Dependabot to keep Gradle, plugins, and libraries current.

## 2. Refresh Libraries and Runtime Versions

* **Move to the latest LTS JDK.** Validate the codebase on JDK 17 or 21, and adjust source/target compatibility accordingly.
* **Replace end-of-life logging and utility libraries.** For example, upgrade from `log4j:1.2.14` to Log4j 2 or SLF4J with Logback, and migrate the Apache HttpComponents, Jackson, and testing stack to their latest supported versions.【F:Apps/build.gradle†L70-L87】
* **Audit for transitive vulnerabilities.** Use tools such as OWASP Dependency-Check or Gradle's `dependencyCheckAnalyze` to identify and remediate CVEs.

## 3. Improve Project Structure and Observability

* **Convert legacy web modules.** Evaluate whether the `RaptureWebServer` and REST components can be migrated to a Spring Boot or Jakarta EE stack with embedded servers, simplifying deployment and testing.
* **Standardize configuration.** Move the generated `.cfg` files into externalized configuration (e.g., YAML/JSON) managed by Spring Config or similar, reducing custom build logic that copies resources into `build/config`.【F:Apps/build.gradle†L57-L69】
* **Introduce consistent logging and metrics.** Adopt SLF4J with structured logging and integrate metrics (Micrometer/Prometheus) for critical services.

## 4. Modern Development Workflow

* **Establish continuous integration.** Add GitHub Actions or another CI pipeline to run unit tests, static analysis, and packaging on each commit.
* **Add automated code quality checks.** Integrate tools such as Spotless for formatting, Error Prone or SpotBugs for static analysis, and Jacoco for coverage.
* **Containerize services.** Provide Dockerfiles or Compose manifests for the primary servers (API, REST, Watch, etc.) to streamline local development and deployment.

## 5. Developer Experience Enhancements

* **Improve documentation.** Expand the top-level `README.md` with setup instructions, module overviews, and developer onboarding steps.
* **Adopt modern testing practices.** Introduce JUnit 5, AssertJ, and Testcontainers for integration tests, replacing legacy PowerMock/EasyMock usages where possible.【F:Apps/build.gradle†L80-L87】
* **Enable observability in tests.** Provide sample scripts or Gradle tasks to run test suites and integration scenarios locally with clear reporting.

## 6. Deployment and Security Hardening

* **Secrets management.** Replace any hard-coded credentials in configuration with references to environment variables or a secrets manager.
* **Runtime packaging.** Publish container images or modern distribution bundles instead of custom ZIPs created with Ant checksums during the Gradle build.【F:Apps/build.gradle†L88-L97】
* **Security scanning in CI/CD.** Add SAST/DAST tools (e.g., SonarCloud, Snyk) and infrastructure-as-code scanning if deployment definitions are introduced.

Implementing these changes incrementally will keep the platform maintainable while ensuring compatibility with current Java ecosystems and operational tooling.
