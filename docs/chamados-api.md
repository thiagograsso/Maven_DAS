# Chamados API

## Status and assignment

`PATCH /chamados/{id}/status` accepts `{"status":"EM_ATENDIMENTO"}`.
The supported transitions are:

| Current status | Next status |
|---|---|
| `ABERTO` | `EM_ATENDIMENTO` |
| `EM_ATENDIMENTO` | `AGUARDANDO_CLIENTE`, `FECHADO` |
| `AGUARDANDO_CLIENTE` | `EM_ATENDIMENTO`, `FECHADO` |
| `FECHADO` | none |

Requesting the current status is idempotent. Other transitions return HTTP 409.
Closing a ticket sets `dataFechamento` on the server. Client-provided
`dataFechamento` values are ignored. Closed tickets cannot be assigned a
different technician or updated with `PUT /chamados/{id}`; both operations
return HTTP 409. Assignment uses
`PATCH /chamados/{id}/atribuir` with `{"tecnicoId":42}`.

## Listing, filters, and paging

`GET /chamados` accepts optional `status`, `prioridade`, and `clienteId`
parameters. Filters are combined with AND; omitted filters do not restrict the
results. The response is a Spring Data page.

- Default page: `page=0`, `size=20`.
- Maximum page size: `100` (requests outside `1..100` are rejected with HTTP
  400).
- Default ordering: `dataAbertura,desc`.
- Supported sort fields: `id`, `titulo`, `dataAbertura`, `dataFechamento`,
  `prioridade`, and `status`.
- Sort direction is `asc` or `desc`; repeat `sort` to add ordering fields.

Examples:

```http
GET /chamados?status=ABERTO&prioridade=ALTA&clienteId=42&page=0&size=20&sort=dataAbertura,desc
GET /chamados?status=EM_ATENDIMENTO&page=1&size=10&sort=prioridade,asc
GET /clientes/42/chamados?page=0&size=20&sort=dataAbertura,desc
```

`GET /clientes/{id}/chamados` returns a page of tickets for that client and
returns HTTP 404 when the client does not exist.

## Comments

`POST /chamados/{id}/comentarios` accepts a nonblank `texto` (up to 2000
characters) and `autor` (up to 100 characters), for example:

```json
{"texto":"Cliente confirmou a resolução.","autor":"Ana"}
```

The server records `dataCriacao`; clients cannot set it. Comments are
append-only and can be added to closed tickets. `GET /chamados/{id}` includes a
chronological `comentarios` array. The supplied author is a display value; it
is not an authenticated-user identity.
