# Spec tecnica - msedi-bitacora

## Responsabilidad

`msedi-bitacora` es el microservicio transversal para registrar y consultar eventos de auditoria de EDI.

Cubre:

- HU003 - Registrar y almacenar eventos del Ecosistema Digital de Incapacidades.

Tambien soporta la trazabilidad requerida por HU001-HU012.

## Context path

```yaml
server:
  servlet:
    context-path: /msedi-bitacora
```

## Endpoints

### Registrar evento

```http
POST /api/v1/eventos
```

Uso:

- Lo consumen otros microservicios EDI.
- Registra eventos funcionales, tecnicos y de integracion.

Headers:

```http
Authorization: Bearer <token-edi-o-token-de-servicio>
X-Transaccion-Id: 12
X-Correlacion-Id: <opcional>
```

Request minimo:

```json
{
  "cveEvento": "TOKEN_EDI_GENERADO",
  "cveOperacion": "proceso",
  "resultado": "EXITOSA",
  "stpOcurrencia": "2026-08-11T18:30:00-06:00"
}
```

Campos opcionales:

```json
{
  "codigoMensaje": null,
  "objeto": "ACCESO_EDI",
  "error": null,
  "detalle": {
    "grupo": "AUTENTICACION"
  }
}
```

Mapeo derivado:

- `id_transaccion`: `X-Transaccion-Id`, validado contra claim `idTransaccion`.
- `id_sistema_origen`: claim `sistemaOrigen` o transaccion.
- `ref_nombre_usuario`: claim `user`.
- `ref_sesion`: claim `jti`.
- `ref_terminal`: IP confiable propagada por gateway.
- `cve_usuario_alta`: claim `user`; para servicio, `SERVICIO:<sub>`.
- `id_evento`: resolucion de `cveEvento`.
- `ref_detalle`: JSON con `codigoMensaje` y `detalle`.
- `stp_alta`: default de Postgres.
- `cve_usuario` y `ref_perfil_usuario`: nulos mientras no existan en JWT.

Obligatorios del body: `cveEvento`, `cveOperacion`, `resultado` y `stpOcurrencia`. Los demas son opcionales.

Response:

```json
{
  "exito": true,
  "mensaje": "Evento registrado correctamente",
  "respuesta": {
    "idEventoBitacora": 12345
  }
}
```

### Consultar eventos por transaccion

```http
GET /api/v1/transacciones/{idTransaccion}/eventos
```

Response:

```json
{
  "exito": true,
  "mensaje": "Consulta exitosa",
  "respuesta": [
    {
      "idEventoBitacora": 1,
      "idTransaccion": 12,
      "cveEvento": "CVE_ACCESO_GENERADA",
      "resultado": "EXITOSA",
      "stpOcurrencia": "2026-08-11T18:30:00-06:00",
      "stpAlta": "2026-08-11T18:30:01-06:00"
    }
  ]
}
```

Orden de consulta:

```sql
order by stp_ocurrencia, id_evento_bitacora
```

### Consulta avanzada

```http
GET /api/v1/eventos
```

Filtros sugeridos:

- `idTransaccion`
- `cveTransaccion`
- `nss`
- `folioIncapacidad`
- `cveUsuario`
- `fechaInicio`
- `fechaFin`
- `cveEvento`
- `resultado`

Nota: NSS y folio pueden resolverse mediante joins con tablas funcionales cuando existan.

## Persistencia requerida

Tablas principales:

- `catalogo.evento`
- `catalogo.mensaje`
- `trazabilidad.evento_bitacora`
- `trazabilidad.transaccion`

`cveEvento` se resuelve contra `catalogo.evento.cve_evento` para obtener `id_evento`. Los consumidores no deben depender del identificador numerico del catalogo.

`codigoMensaje` se valida contra `catalogo.mensaje.cve_mensaje` y se conserva dentro de `ref_detalle`, porque `trazabilidad.evento_bitacora` no tiene una relacion directa con el catalogo de mensajes.

Campos clave en `trazabilidad.evento_bitacora`:

- `id_evento_bitacora`
- `id_evento`
- `id_transaccion`
- `id_sistema_origen`
- `ref_nombre_usuario`
- `ref_perfil_usuario`
- `ref_error`
- `ref_detalle`
- `ref_sesion`
- `ref_terminal`
- `ref_objeto`
- `cve_operacion`
- `ref_resultado`
- `stp_ocurrencia`
- `stp_alta`
- campos de auditoria estandar

## Catalogo de eventos

`catalogo.evento` contiene 45 eventos para HU001-HU012. La clave de integracion es `cve_evento`; los consumidores no deben depender de `id_evento`.

La fuente de verdad del catalogo es `catalogo.evento` en la base Postgres del servidor.

Grupos de eventos:

- Acceso: `SISTEMA_ORIGEN_VALIDADO`, `SISTEMA_ORIGEN_RECHAZADO`, `SISTEMA_ORIGEN_VALIDACION_ERROR`, `API_KEY_VALIDADA`, `API_KEY_RECHAZADA`, `INICIO_EXPEDICION_ACEPTADO`, `OPCION_EXPEDICION_RECHAZADA`, `INICIO_EXPEDICION_ERROR`.
- Token EDI: `TOKEN_EDI_GENERADO`, `TOKEN_EDI_VALIDADO`, `TOKEN_EDI_RECHAZADO`, `TOKEN_EDI_EXPIRADO`.
- Clave de acceso: `CVE_ACCESO_GENERADA`, `CVE_ACCESO_CONSUMIDA`, `CVE_ACCESO_REUSO_RECHAZADO`, `CVE_ACCESO_EXPIRADA`, `PAYLOAD_RECUPERACION_ERROR`.
- Bitacora: `BITACORA_CONSULTADA`, `BITACORA_CONSULTA_RECHAZADA`, `BITACORA_ALMACENAMIENTO_ERROR`.
- Derecho: `DERECHO_INCAPACIDAD_VALIDADO`, `DERECHO_INCAPACIDAD_RECHAZADO`, `DERECHO_INCAPACIDAD_ERROR`, `DERECHO_INCAPACIDAD_NSS_INVALIDO`.
- Medico: `ELEGIBILIDAD_MEDICO_VALIDADA`, `ELEGIBILIDAD_MEDICO_RECHAZADA`, `ELEGIBILIDAD_MEDICO_ERROR`, `DATOS_MEDICO_VALIDADOS`, `DATOS_MEDICO_INCONSISTENTES`, `PERFIL_MEDICO_INVALIDO`, `DATOS_MEDICO_RECEPCION_ERROR`.
- Datos institucionales: `DATOS_INSTITUCIONALES_RECIBIDOS`, `DATOS_INSTITUCIONALES_INCOMPLETOS`, `DATOS_INSTITUCIONALES_RECEPCION_ERROR`.
- Unidad: `DATOS_UNIDAD_VALIDADOS`, `DATOS_UNIDAD_INCOMPLETOS`.
- Asegurado: `DATOS_ASEGURADO_VALIDADOS`, `DATOS_ASEGURADO_INCONSISTENTES`, `DATOS_ASEGURADO_RECEPCION_ERROR`.
- SIAP laboral: `TRABAJADOR_IMSS_IDENTIFICADO`, `NO_TRABAJADOR_IMSS_IDENTIFICADO`, `INFORMACION_LABORAL_SIAP_ERROR`.
- Diagnostico: `DATOS_DIAGNOSTICO_VALIDADOS`, `DATOS_DIAGNOSTICO_INCONSISTENTES`, `DATOS_DIAGNOSTICO_RECEPCION_ERROR`.

`cveOperacion` se normaliza a minusculas antes de persistirse. Valores admitidos: `insert`, `update`, `delete`, `select`, `login`, `proceso`, `error`.

## Paquetes sugeridos

```text
mx.gob.imss.edi.bitacora
  config
  controllers
  exceptions
  filters
  security
  services
  services.impl
  repository
  models.request
  models.response
  models.dto
  common.constants
  common.response
```

## MyBatis mappers

Mappers iniciales:

- `EventoMapper`
- `EventoBitacoraMapper`
- `TransaccionConsultaMapper`

## Sincrono vs asincrono

Para el arranque puede consumirse por REST sincrono con timeout corto.

Evolucion recomendada:

- Publicacion asincrona.
- Outbox o cola institucional.
- Registro ordenado por `stp_ocurrencia`, no por llegada.

## Seguridad

Todos los endpoints funcionales requieren Bearer Token.

- Angular usa el JWT EDI emitido durante el intercambio.
- Los micros propagan el JWT EDI cuando ya existe.
- Para HU001/HU002 antes del intercambio, `msedi-autenticacion` usa un Bearer de servicio corto.
- El token de servicio usa `sub = msedi-autenticacion` y audiencia de bitacora.
- Incluye `idTransaccion`, `sistemaOrigen` y el `user` validado como identidad delegada.
- Bitacora valida firma, expiracion, audiencia y `X-Transaccion-Id`.
- Password, API key y JWT nunca forman parte del evento.

## Configuracion yml esperada

```yaml
server:
  port: 8080
  servlet:
    context-path: /msedi-bitacora

spring:
  application:
    name: msedi-bitacora
  datasource:
    url: ${urlConexionBd}
    username: ${userBd}
    password: ${passwordBd}
    driver-class-name: org.postgresql.Driver

jwt:
  secret-key: ${jwtSecretKey}
  expiration-ms: ${EDI_JWT_EXPIRATION_MS:3600000}
```

## Decisiones pendientes

- Definir la infraestructura outbox o mensajeria para una evolucion asincrona.

- Definir permisos de consulta transversal para personal de auditoria y soporte.
- Definir los joins de NSS y folio cuando exista una incapacidad persistida.



## Decisiones implementadas

- En el primer sprint el registro se consume mediante REST sincrono.
- El JWT ordinario solo permite consultar la transaccion indicada en su claim `idTransaccion`.
- Los filtros `nss` y `folioIncapacidad` devuelven `FILTRO_FUNCIONAL_NO_DISPONIBLE` mientras no exista la relacion funcional persistida.
- Se admiten las operaciones `insert`, `update`, `delete`, `select`, `login`, `proceso` y `error`.
- Password, API key, Bearer, JWT, NSS y CURP se redactan o enmascaran tanto en `detalle` como en el texto de error.
- La API no expone endpoints de modificacion o eliminacion de eventos.
