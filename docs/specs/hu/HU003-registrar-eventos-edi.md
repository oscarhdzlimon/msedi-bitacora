# HU003 - Registrar y almacenar eventos EDI

## Metadatos

- Microservicio: `msedi-bitacora`
- Endpoints: `POST /api/v1/eventos`, `GET /api/v1/transacciones/{idTransaccion}/eventos`, `GET /api/v1/eventos`
- Regla: no se referencia una RN independiente
- Mensajes: no aplica

## Objetivo

Conservar de manera centralizada, consultable e inalterable para usuarios EDI los eventos funcionales, tecnicos y de integracion de HU001-HU012.

## Contrato minimo de registro

Headers obligatorios:

- `Authorization: Bearer <token-edi-o-token-de-servicio>`.
- `X-Transaccion-Id`.

Body obligatorio:

- `cveEvento`.
- `cveOperacion`.
- `resultado`.
- `stpOcurrencia`.

Body opcional:

- `codigoMensaje`.
- `objeto`.
- `error`.
- `detalle`.

Se derivan del Bearer: `user`, `jti` y `sistemaOrigen`. `idEvento`, `stpAlta`, `indActivo` y campos de auditoria se resuelven en el servicio o mediante defaults de Postgres.

## Comportamiento

1. Validar `cveEvento` contra `catalogo.edic_evento`.
2. Validar la existencia de la transaccion cuando se informe.
3. Conservar `codigoMensaje` dentro de `refDetalle`.
4. Insertar el evento sin permitir actualizacion o eliminacion desde la API.
5. Devolver `idEventoBitacora`.
6. Consultar cronologicamente por `stp_ocurrencia, id_evento_bitacora`.

## Escenarios de aceptacion

| ID | Condicion | Resultado |
| --- | --- | --- |
| HU003-CA01 | Evento valido | Registro persistido y el proceso origen continua |
| HU003-CA02 | Operacion rechazada o con error | Se conservan motivo, mensaje y trazabilidad |
| HU003-CA03 | Error de almacenamiento | Se genera alerta tecnica y se aplica la criticidad |
| HU003-CA04 | Consulta autorizada por transaccion | Historial cronologico completo |
| HU003-CA05 | Consulta avanzada autorizada | Filtros por transaccion, folio, NSS, usuario, fecha, evento y resultado |

## Persistencia

- `catalogo.edic_evento`
- `catalogo.edic_mensaje`
- `trazabilidad.edit_transaccion`
- `trazabilidad.edit_evento_bitacora`

No se usa un numero de secuencia funcional fijo. El orden se determina mediante la fecha real de ocurrencia y el identificador tecnico como desempate.

## Criticidad

- Eventos de seguridad y decisiones bloqueantes: el emisor debe confirmar el registro antes de concluir la transaccion.
- Eventos informativos: pueden evolucionar a entrega asincrona con outbox.
- Si la bitacora falla, debe emitirse una alerta operativa y conservarse evidencia local o en outbox cuando esa estrategia se habilite.

## Requerimientos no funcionales

- Registro automatico antes de concluir la transaccion.
- No afectar materialmente el tiempo de respuesta.
- Retencion conforme a politica institucional pendiente.
- Integridad e inmutabilidad para usuarios funcionales.
- Fechas con zona horaria.
- Toda consulta requiere autorizacion.

## Pruebas minimas

- Registro exitoso y fallido.
- Evento inexistente.
- Transaccion inexistente.
- Persistencia de detalle JSON.
- Orden por ocurrencia.
- Filtros de consulta.
- Error de base y alerta.
- Rechazo de modificaciones y eliminaciones.

## Pendientes

- Politica de retencion.
- Autenticacion entre microservicios.
- Sincrono u outbox para cada nivel de criticidad.
- Roles autorizados para consulta.
