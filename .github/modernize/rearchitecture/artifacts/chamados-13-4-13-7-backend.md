# Chamados 13.4–13.7 — Backend Implementation

## Summary

Implemented ticket assignment and status actions, closed-ticket protections,
filtering and pagination, customer ticket listing, ticket comments, and
repository, service, controller, and live HTTP tests. Added API usage
documentation at `docs/chamados-api.md`.

The approved status transitions are:

- `ABERTO` → `EM_ATENDIMENTO`
- `EM_ATENDIMENTO` → `AGUARDANDO_CLIENTE` or `FECHADO`
- `AGUARDANDO_CLIENTE` → `EM_ATENDIMENTO` or `FECHADO`
- `FECHADO` → no other status

Repeating the current status is idempotent. Closing sets `dataFechamento` on
the server; client-supplied close dates are ignored. Closed tickets reject
technician reassignment and full updates. Comments remain appendable after
closure; the request supplies a display author because no authentication
identity contract exists.

Ticket list filters (`status`, `prioridade`, `clienteId`) combine with AND.
Paging defaults to 20 items, caps page size at 100, and supports documented
sorting on `id`, `titulo`, `dataAbertura`, `dataFechamento`, `prioridade`, and
`status`.

## Upstream Artifacts Consumed

- `.github/modernize/chamados-13-4-13-7/plan.md` — implementation scope,
  proposed status matrix, query examples, and validation goals.
- `.github/modernize/chamados-13-4-13-7/.metadata/tasks.json` — acceptance
  criteria and dependencies for items 001–004.

## Evidence Mapping

- `tasks.json#001` / plan § Proposed Status Rules → `ChamadoService`,
  assignment/status endpoints, service tests, and `ChamadosHttpIntegrationTests`.
- `tasks.json#002` / plan § Query Documentation Examples → repository
  specifications, pageable/customer listing controllers, `docs/chamados-api.md`,
  repository tests, and live HTTP filter/list probes.
- `tasks.json#003` / plan § Comments → `Comentario` persistence, creation route,
  detail response, and repository/controller/live HTTP tests.
- `tasks.json#004` / plan § Validation Goals → `@DataJpaTest`,
  `@WebMvcTest`, service tests, preserved existing integration tests, and the
  full Maven test run below.

## Changed Files

- `src/main/java/br/com/sistemas/das/controller/ChamadoController.java`
- `src/main/java/br/com/sistemas/das/controller/ClienteChamadoController.java`
- `src/main/java/br/com/sistemas/das/service/ChamadoService.java`
- `src/main/java/br/com/sistemas/das/repository/ChamadoRepository.java`
- `src/main/java/br/com/sistemas/das/repository/ChamadoSpecifications.java`
- `src/main/java/br/com/sistemas/das/repository/ComentarioRepository.java`
- `src/main/java/br/com/sistemas/das/entity/Chamado.java`
- `src/main/java/br/com/sistemas/das/entity/Comentario.java`
- `src/main/java/br/com/sistemas/das/dto/ChamadoDetalheResponse.java`
- `src/main/java/br/com/sistemas/das/dto/AtribuirTecnicoRequest.java`
- `src/main/java/br/com/sistemas/das/dto/AlterarStatusRequest.java`
- `src/main/java/br/com/sistemas/das/dto/ComentarioRequest.java`
- `src/main/java/br/com/sistemas/das/dto/ComentarioResponse.java`
- `src/test/java/br/com/sistemas/das/ChamadosHttpIntegrationTests.java`
- `src/test/java/br/com/sistemas/das/controller/ChamadoControllerTests.java`
- `src/test/java/br/com/sistemas/das/repository/ChamadoRepositoryTests.java`
- `src/test/java/br/com/sistemas/das/service/ChamadoServiceTests.java`
- `docs/chamados-api.md`

## Test Results

- Command: `$env:JAVA_HOME='C:\Users\Usuario\.jdk\jdk-25\jdk-25.0.5+8'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; .\mvnw.cmd -B test`
- Java runtime: Temurin 25.0.5+8
- Passed: 17
- Failed: 0
- Errors: 0
- Skipped: 0
- Runtime endpoint verification: passed through a random-port HTTP integration
  test, including list/filter/client-list, detail, assignment, status, comment,
  closed-ticket restrictions, page-size limit, and unsupported sort handling.
- `git diff --check`: passed.

## Blockers and Notes

- No implementation or test blockers.
- The inherited `JAVA_HOME` was invalid and pointed to a `.exe` path. Tests
  used the installed Java 25 JDK by setting `JAVA_HOME` explicitly.
- No project Java instruction files or screenshots were present in the
  workspace. The supplied plan directory was left untouched.
- No commit was created.
