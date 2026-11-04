# Matriz de trazabilidad - msedi-bitacora

| Capacidad | Endpoint | Tablas | Cobertura |
| --- | --- | --- | --- |
| Registrar evento | `POST /api/v1/eventos` | `catalogo.edic_evento`, `catalogo.edic_mensaje`, `trazabilidad.edit_evento_bitacora` | HU003 y auditoria HU001-HU012 |
| Consultar por transaccion | `GET /api/v1/transacciones/{idTransaccion}/eventos` | `trazabilidad.edit_transaccion`, `trazabilidad.edit_evento_bitacora` | HU003 escenario 4 |
| Consulta avanzada | `GET /api/v1/eventos` | Bitacora y joins funcionales | HU003 RNF de consulta |

Los 45 valores de `cveEvento` disponibles en `catalogo.edic_evento` cubren exito, rechazo y error de HU001-HU012.

## HU003 / HU043-HU062 — Motor y DPES
La incapacidad conserva su transacción de origen. El actor conserva la transacción validada del acceso o de la ejecución batch. Se persisten eventos en outbox en la misma transacción funcional y se publican con credencial SERVICIO_BITACORA de msedi-evaluacion-riesgos, scope edi:bitacora:riesgos. Bitácora verifica UUID y contenido contra la evidencia durable; deduplica por UUID. No se almacenan tokens. Las decisiones confirman evidencia durable antes de responder y la entrega central se reintenta.
