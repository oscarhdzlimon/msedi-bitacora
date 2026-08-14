# Spec tecnica base - Microservicios EDI

## Alcance

Esta spec define las convenciones tecnicas para los microservicios del primer alcance EDI, correspondiente a HU001-HU012.

Microservicios propuestos:

- `msedi-autenticacion`
- `msedi-bitacora`
- `msedi-catalogos`

La fuente de verdad es la base de datos Postgres disponible en el servidor. Los metadatos de Oracle y los scripts existentes son antecedentes tecnicos; los microservicios no crean, alteran ni cargan la estructura.

## Stack

- Java 21
- Spring Boot 3.5.0
- Maven
- Spring Web
- Spring Security
- JWT con `jjwt`
- MyBatis como persistencia principal
- Feign Client para servicios externos
- Postgres JDBC Driver
- Springdoc OpenAPI / Swagger
- Actuator
- Lombok

## Convenciones del proyecto

Los microservicios EDI deben seguir estas convenciones compartidas:

- `RespuestaGenerica<T>` con campos `exito`, `mensaje`, `respuesta`.
- Controllers con `ResponseEntity<RespuestaGenerica<...>>`.
- Paquetes separados en `controllers`, `services`, `services.impl`, `models.request`, `models.response`, `models.dto`, `repository`, `security`, `filters`, `exceptions`, `config`.
- Servicios definidos mediante interfaz + implementacion.
- `GlobalExceptionHandler` centralizado.
- `CorrelationIdFilter` usando header `X-Correlacion-Id`.
- Seguridad stateless.
- Swagger habilitado en `/swagger-ui`.
- Actuator habilitado en `/actuator`.
- Variables de entorno leidas desde `application.yml`.

## Respuesta generica

Formato estandar:

```json
{
  "exito": true,
  "mensaje": "Operacion exitosa",
  "respuesta": {}
}
```

Para errores funcionales controlados:

```json
{
  "exito": false,
  "mensaje": "El Sistema no se encuentra autorizado para consumir los servicios de EDI",
  "respuesta": {
    "codigo": "MSG001"
  }
}
```

Para errores tecnicos no controlados se usara `ApiErrorResponse` desde el `GlobalExceptionHandler`.

`RespuestaGenerica<T>` mantiene fijo el sobre `exito`, `mensaje` y `respuesta`; `T` queda abierto y cada endpoint define su DTO.

## Codigos HTTP generales

- `200 OK`: solicitud procesada; una validacion con `resultado = 0` tambien usa 200.
- `400 Bad Request`: headers ausentes, JSON malformado o estructura invalida.
- `401 Unauthorized`: credenciales o Bearer ausentes, invalidos o expirados.
- `403 Forbidden`: identidad valida sin permiso o transaccion distinta al claim.
- `404 Not Found`: recurso inexistente.
- `409 Conflict`: estado incompatible, como reutilizar una clave consumida.
- `410 Gone`: clave de acceso expirada.
- `500 Internal Server Error`: error no controlado propio.
- `502 Bad Gateway`: respuesta invalida de dependencia.
- `503 Service Unavailable`: base o dependencia indispensable no disponible.
- `504 Gateway Timeout`: dependencia excedio el timeout.

## Seguridad

### Entrada desde sistema convocante

No habra token inicial enviado por el Sistema Medico Convocante, porque no se cuenta con llaves o secretos para validarlo.

La autenticacion inicial se hara con:

- Identificador del sistema origen.
- Header `X-API-Key`.
- Header `X-User`.
- Header `X-Password`.
- Catalogo de sistemas autorizados.
- HTTPS/TLS.

Los tres headers de credenciales son obligatorios. La API key se leera desde variables de entorno o secrets de OpenShift. La fuente de validacion de user y password queda pendiente.

Password, API keys y JWT se excluyen de logs, trazas, bitacora y persistencia. El `X-User` validado se propaga como claim `user`.

### Bearer Token EDI

EDI generara su propio Bearer Token cuando Angular intercambie correctamente la `CVE_ACCESO` de un solo uso.

El Bearer Token no debe incluirse en la URL ni devolverse como parte de la URL entregada al Sistema Medico Convocante.

Tiempo de vida:

- 1 hora.

El token EDI se devolvera a Angular en el cuerpo HTTPS de la respuesta de intercambio y se usara para las llamadas posteriores del frontend a los servicios EDI.

El JWT debe incluir al menos:

- `jti`
- `idTransaccion`
- `cveTransaccion`
- `sistemaOrigen`
- `user`
- `iat`
- `exp`
- `aud`

Todo endpoint posterior al intercambio debe validar el Bearer Token y comprobar que el `idTransaccion` solicitado corresponda con el claim del JWT.

Para endpoints que no incluyen `idTransaccion` en el path o body, Angular lo enviara mediante el header `X-Transaccion-Id`.

### CVE_ACCESO

`CVE_ACCESO` sera un GUID/UUID generado por EDI.

Reglas:

- Se envia en la URL del frontend.
- La URL nunca debe incluir el Bearer Token.
- Es de un solo uso.
- Angular la intercambia por el payload inicial y el Bearer Token EDI.
- Se quema atomicamente durante el intercambio.
- Su vigencia es de 5 minutos.
- El Bearer Token EDI mantiene una vigencia independiente de 1 hora.
- Angular debe retirar `CVE_ACCESO` de la barra de navegacion inmediatamente despues de leerla.
- La respuesta de intercambio debe incluir `Cache-Control: no-store`.

## Base de datos

Motor vigente:

- Postgres.

Esquemas:

- `catalogo`
- `trazabilidad`
- `integracion`
- `expedicion`

Persistencia:

- MyBatis como opcion principal.
- SQL explicito para inserts, updates atomicos y consultas con joins.
- Los SQL deben usar nombres calificados por esquema.
- Los microservicios no ejecutan DDL ni inicializacion de datos.

Catalogos disponibles para el primer alcance:

- `catalogo.edic_evento`: 45 eventos con clave funcional `cve_evento`.
- `catalogo.edic_mensaje`: MSG001-MSG018.

`msedi-catalogos` expondra al frontend solamente los catalogos permitidos en su especificacion. Los demas microservicios podran consultar directamente las tablas del esquema `catalogo` que requieran, sin consumir `msedi-catalogos`.

## Gestion del esquema Postgres

- La base del servidor es la fuente de verdad.
- No se utilizara Flyway ni Liquibase.
- No se ejecutaran scripts de estructura o carga desde aplicaciones o pipelines.
- Los microservicios usaran exclusivamente consultas y operaciones DML autorizadas.
- Cualquier cambio estructural sera realizado externamente por el responsable de base.
- Despues de un cambio autorizado se actualizaran metadatos, specs, mappers y pruebas.
- La aplicacion debe deshabilitar cualquier inicializacion automatica de esquema o datos.

Campos de auditoria estandar:

- `ind_activo`
- `stp_alta`
- `stp_baja`
- `stp_modifica`
- `cve_usuario_alta`
- `cve_usuario_baja`
- `cve_usuario_modifica`

Los campos `cve_usuario_*` deben ser alfanumericos:

```sql
varchar(100)
```

Convencion sugerida para valores:

- `SISTEMA:SIMF`
- `SISTEMA:MOCE`
- `SISTEMA:PHEDS`
- `SISTEMA:EDI`
- `MEDICO:<matricula>`
- `PROCESO:<nombre_proceso>`

## Integraciones externas

Se usaran Feign Clients.

Servicios conocidos:

- SIAP empleados: `consultaCatEmpleados`.

Servicios pendientes:

- ACCEDER para derecho de incapacidad.
- Contrato final de los 28 campos del payload del sistema convocante.

Mientras no exista contrato final, EDI trabajara con modelos internos propios y mappers manuales.

## Mapper manual para payload

El sistema no debe depender directamente del contrato externo provisional.

Flujo:

```text
Solicitud externa
  -> mapper manual
  -> modelo interno EDI
  -> casos de uso
```

Cuando exista contrato final, solo debe cambiar el request externo y el mapper, no la logica de negocio.

## Trazabilidad

Todo request debe usar o generar `X-Correlacion-Id`.

`msedi-autenticacion` debe enviar eventos a `msedi-bitacora`.

La bitacora debe registrar:

- `id_transaccion`
- sistema origen
- evento
- resultado
- mensaje
- `stp_ocurrencia`
- `stp_alta`
- detalle estructurado en JSON cuando aplique

No se usara numero de secuencia fijo. Para consultas cronologicas:

```sql
order by stp_ocurrencia, id_evento_bitacora
```

