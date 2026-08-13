# Matriz de trazabilidad - msedi-bitacora

| Capacidad | Endpoint | Tablas | Cobertura |
| --- | --- | --- | --- |
| Registrar evento | `POST /api/v1/eventos` | `catalogo.evento`, `catalogo.mensaje`, `trazabilidad.evento_bitacora` | HU003 y auditoria HU001-HU012 |
| Consultar por transaccion | `GET /api/v1/transacciones/{idTransaccion}/eventos` | `trazabilidad.transaccion`, `trazabilidad.evento_bitacora` | HU003 escenario 4 |
| Consulta avanzada | `GET /api/v1/eventos` | Bitacora y joins funcionales | HU003 RNF de consulta |

Los 45 valores de `cveEvento` disponibles en `catalogo.evento` cubren exito, rechazo y error de HU001-HU012.
