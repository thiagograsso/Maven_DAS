# Chamado API Extension

Extend the ticket API with pageable filtered lists, conservative status changes, and append-only comments without coupling list payloads to detail-only data.

## What Happened

For Maven_DAS task `chamados-13-4-13-7`, the existing `ChamadoResponse` was retained for list and mutation responses while a separate detail response added comments. Optional filters use a JPA `Specification`; paging is bounded and sort properties are allowlisted. Status transitions use the approved matrix, closed tickets reject reassignment and full updates, and comments carry an explicit display author because the application has no authenticated-user contract.

The project targets Java 25, but the inherited `JAVA_HOME` was invalid. An installed Temurin JDK 25 was available at `C:\Users\Usuario\.jdk\jdk-25\jdk-25.0.5+8`; setting `JAVA_HOME` to that directory enabled the complete Maven suite.

## Takeaway

For similar API extensions, retain the established response DTO where possible, put collection-only data in a detail DTO, compose optional JPA predicates, validate sort/page inputs, and generate audit timestamps server-side. Confirm the active JDK path rather than trusting inherited `JAVA_HOME`.

## History

- 2026-11-05 (Maven_DAS/chamados-13-4-13-7): initial
