# Rapture

This repository contains the historical source code for the Rapture platform from Incapture. The codebase predates many of the modern Java ecosystem conventions, so new contributors should review the [modernization plan](Doc/modernization-plan.md) for suggested upgrades to the build, dependencies, and tooling.

## Getting Started

1. Clone the repository and ensure you have a compatible JDK (the existing build targets Java 7/8-era tooling).
2. Run Gradle tasks from the `Apps` directory using the provided wrapper: `./gradlew clean build`.
3. Explore module-specific documentation in the `Apps` subdirectory.

## Modernization Guidance

To bring the platform up to current standards, follow the recommendations in [`Doc/modernization-plan.md`](Doc/modernization-plan.md). The document covers upgrading Gradle, refreshing dependencies, adopting modern testing practices, and setting up contemporary CI/CD and containerization workflows.
