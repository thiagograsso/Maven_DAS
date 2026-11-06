# Modernization Plan: Java 25 Runtime Upgrade

**Project**: das

## Technical Framework

- **Language**: Java 21
- **Framework**: Spring Boot 4.1.1
- **Build Tool**: Maven (Maven Wrapper)
- **Database**: H2
- **Key Dependencies**: Spring Data JPA, Thymeleaf, Spring MVC

## Overview

Upgrade the application's Java runtime from Java 21 to Java 25, the latest LTS
identified by the completed assessment. This focused upgrade should retain the
current application behavior and framework while enabling the project to build
and run on the newer LTS runtime.

The scope is limited to the Java runtime and the Maven/toolchain configuration
needed for Java 25. Other assessment findings—including credentials,
containerization, and database concerns—are explicitly out of scope.

## Assessment Scope

This plan addresses only the assessment issue **“Java Version is not the latest
LTS”** (`azure-java-version-02000`), reported at `pom.xml` line 30. The
assessment found Java 21; the current Maven configuration confirms
`java.version` is 21.

## Impact Summary

| Project | Current runtime | Target runtime | Scope |
|---|---|---|---|
| das | Java 21 | Java 25 LTS | Maven build and runtime |

## Validation Goals

- Build the project with Java 25 using the Maven Wrapper.
- Run the existing unit tests with Java 25.
- Keep Spring Boot 4.1.1 and unrelated application configuration unchanged.
