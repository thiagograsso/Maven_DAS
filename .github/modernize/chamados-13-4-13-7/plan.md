# Implementation Plan: Chamados Items 13.4–13.7

**Project**: das

## Technical Framework

- **Language**: Java 25
- **Framework**: Spring Boot 4.1.1
- **Build Tool**: Maven Wrapper
- **Database**: H2
- **Key Dependencies**: Spring Data JPA, Spring MVC, Jakarta Validation

## Overview

Extend the existing ticket (`Chamado`) API and its tests to cover the requested
business rules, filtering and pagination, comments, and focused automated test
coverage. The existing project already has ticket CRUD, status and priority
enums, a close-date field, a global business-rule exception handler, and a
Spring Boot integration test. The requested features build on that foundation;
the existing CRUD behavior should remain compatible except where closed-ticket
rules explicitly restrict mutation.

Work is sequenced because the status, query, comment, and test changes touch
shared ticket API and persistence behavior. No assessment, Azure migration,
deployment, or code changes are included in this plan.

## Current State and Scope

- Ticket CRUD is exposed through `/chamados`; `Chamado` already has
  `dataFechamento`, status, priority, and customer/technician associations.
- `ChamadoService` currently lists all tickets without filters or pagination.
- `ChamadoRepository` has no ticket-specific query methods.
- `GlobalExceptionHandler` maps `RegraNegocioException` to HTTP 409.
- There is no `ClienteController`, comment entity, ticket-comment relationship,
  or focused `@DataJpaTest` / `@WebMvcTest` suite. The existing test class uses
  `@SpringBootTest` and `MockMvc`.
- No rulebook was found under `.github/modernize/rulebook/`.

The scope covers requirements 13.4–13.7 only. Optional status-change history is
not included. Duplicate tests will verify existing uniqueness rules; this plan
does not define a new ticket-duplicate policy.

## Requirements Traceability

| Requirement | Planned task |
|---|---|
| 13.4 — Assignment/status routes and closed-ticket rules | `001-transform-chamado-status-and-assignment-rules` |
| 13.5 — Ticket filters, customer ticket listing, paging/sorting, query examples | `002-transform-chamado-filtering-pagination` |
| 13.6 — Comments, ticket association, comment creation and detail listing | `003-transform-chamado-comments` |
| 13.7 — Repository/controller tests and Maven test execution | `004-transform-chamado-test-coverage` |

## Proposed Status Rules (Needs Confirmation)

The request specifies that invalid transitions must be rejected, but does not
define the transition matrix. The following conservative proposal makes the
rules explicit for implementation and tests:

- `ABERTO` → `EM_ATENDIMENTO`
- `EM_ATENDIMENTO` → `AGUARDANDO_CLIENTE` or `FECHADO`
- `AGUARDANDO_CLIENTE` → `EM_ATENDIMENTO` or `FECHADO`
- `FECHADO` is terminal; no status change or technician reassignment is
  permitted.
- Full ticket updates are rejected when the ticket is closed. On a successful
  transition to `FECHADO`, set `dataFechamento` to the current date and time.
  Do not accept client-supplied close dates as a substitute for this rule.

This matrix is a proposal, not an already-established project rule. Confirm or
adjust it before implementation. The request does not specify whether comments
may be added after closure; treat comments as append-only and keep this policy
separate from ticket-field editing unless clarified.

## Query Documentation Examples

Document the query parameters, paging defaults/limits, and supported sort
properties alongside the API behavior. Examples to include:

```http
GET /chamados?status=ABERTO&prioridade=ALTA&clienteId=42&page=0&size=20&sort=dataAbertura,desc
GET /chamados?status=EM_ATENDIMENTO&page=1&size=10&sort=prioridade,asc
GET /clientes/42/chamados?page=0&size=20&sort=dataAbertura,desc
```

Omitted filters should not restrict results; combined filters should be applied
together. Confirm the final supported sort properties and page-size policy in
the API documentation.

## Delivery Sequence

1. Add assignment/status actions, enforce the agreed ticket state rules, and
   populate the close date.
2. Add filtered and pageable ticket queries, customer ticket listing, and
   document request examples.
3. Add ticket comments and expose them in ticket detail.
4. Add focused repository/controller tests and verify the full Maven test suite.

## Validation Goals

- Verify assignment and status endpoints for valid/invalid transitions,
  closed-ticket assignment/update restrictions, and close-date population.
- Verify every supported filter combination, sorting, paging, customer-scoped
  listing, and documented query examples.
- Verify comment persistence, author/date/ticket association, creation, and
  inclusion in ticket detail.
- Use `@DataJpaTest` for repository behavior and `@WebMvcTest` for controller
  behavior; cover creation, request validation, existing duplicate constraints,
  and 404 responses without inventing a ticket-duplicate rule.
- Run the existing and new Maven tests with the Maven Wrapper.

## Open Questions

- [ ] Confirm or revise the proposed status transition matrix above.
- [ ] Should a comment be accepted for a closed ticket?
- [ ] Which sort properties and maximum page size should the API document and
  support?
- [ ] Should optional status-change history be included in a later scope?
