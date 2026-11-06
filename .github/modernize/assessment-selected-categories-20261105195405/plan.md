# Modernization Plan: Selected Assessment Categories

**Project**: das

## Technical Framework

- **Language**: Java 25 (current Maven configuration)
- **Framework**: Spring Boot 4.1.1
- **Build Tool**: Maven Wrapper
- **Database**: H2 in-memory
- **Key Dependencies**: Spring Data JPA, Thymeleaf, Spring MVC

## Overview

This plan addresses the selected assessment scope that remains applicable to the
current repository: modernizing the Jakarta Persistence data layer for an Azure
target and preparing the Java application for container use. It also includes
the required dependency security review. No Azure resource provisioning or
application deployment is included.

The work is phased to preserve application behavior while the persistence
target is established, then secure and prepare the resulting application for
container execution.

## Assessment Scope and Revalidation

- The Jakarta Persistence finding is confirmed: the project has the Spring Data
  JPA dependency and uses `jakarta.persistence` entity APIs.
- The containerization finding is confirmed: no `Dockerfile` is present.
- The Java-version finding was not carried forward: the report records Java 21,
  but the current `pom.xml` sets `java.version` to 25, already the latest stable
  version stated in the planning guidance.
- The plaintext-credential finding was not carried forward: the current
  `spring.datasource.password` property is empty, so no non-empty plaintext
  password is present to migrate.

## Migration Impact Summary

| Application | Original Service | New Azure Service | Authentication | Comments |
|-------------|------------------|-------------------|----------------|----------|
| das | H2/Jakarta JPA | Azure persistence target TBD | To be decided | Target service not specified |
| das | No container definition | Container-ready Java app | Not applicable | Dockerfile to be created |

## Phases

1. Modernize persistence for an Azure-supported target after the target is
   confirmed.
2. Review and remediate dependency vulnerabilities.
3. Prepare the application for container execution.

## Validation Goals

- Build the project and run its existing unit tests after the persistence
  changes.
- Confirm persistence behaviors remain functional for the application's
  existing entities and repositories.
- Confirm dependency remediation leaves the project build and tests passing.
- Build the container image and verify that the application can start from it.

## Open Questions & Questionnaire

- [ ] Which Azure persistence service should replace the current H2/JPA
  persistence? The selected solution does not identify a target service.

## Blockers and Scope Notes

- The persistence modernization cannot be made service-specific until the Azure
  database target is identified. No target is assumed in this plan.
- The report's Java upgrade and plaintext-credential categories were excluded
  after checking the current `pom.xml` and `application.properties`.
- No integration-test task, Azure infrastructure, or deployment target was
  requested or added.
