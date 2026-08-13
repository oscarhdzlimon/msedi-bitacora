# AGENTS.md

## Fuente de verdad

Leer `docs/specs/README.md`, HU003, `spec-tecnica.md` y la matriz antes de modificar codigo.

## Flujo Spec Driven

- Cada cambio debe indicar capacidad de HU003 y evento afectado.
- Actualizar la spec antes de cambiar contratos.
- Probar registro, rechazo, error y consulta cronologica.
- Resolver eventos por `cve_evento`, no por IDs fijos.

## Restricciones

- No permitir update o delete funcional de eventos.
- No inventar numeros de secuencia.
- Ordenar por `stp_ocurrencia, id_evento_bitacora`.
- No exponer detalle sensible en consultas.
- Consultar catalogos directamente en Postgres.
- No consumir `msedi-catalogos`.
- No usar Flyway, Liquibase, DDL ni inicializacion automatica de esquema o datos.
- La base Postgres del servidor es la fuente de verdad.
