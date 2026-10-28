# msedi-bitacora

Base tecnica del microservicio transversal de auditoria de EDI.

## Requisitos

- JDK 21
- Acceso a PostgreSQL EDI

## Configuracion minima

```text
urlConexionBd
userBd
passwordBd
jwtSecretKey
```

## Comandos

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

- Health: `http://localhost:8081/msedi-bitacora/actuator/health`
- Swagger: `http://localhost:8081/msedi-bitacora/swagger-ui/index.html`

La aplicacion no ejecuta DDL, migraciones ni carga automatica de datos.

## Endpoints

- `POST /api/v1/eventos`
- `GET /api/v1/transacciones/{idTransaccion}/eventos`
- `GET /api/v1/eventos`

Todos requieren Bearer Token y `X-Transaccion-Id`.


## Perfiles y zona horaria

El perfil \`local\` es el predeterminado. Sus valores explicitos estan en
\`config/application-local.yml\`; este archivo se carga al ejecutar desde la raiz
del repositorio, se excluye de Git y no se empaqueta dentro del JAR.

Para OpenShift se usa \`src/main/resources/application-openshift.yml\`:

\`\`\`text
SPRING_PROFILES_ACTIVE=openshift
EDI_TIME_ZONE=America/Mexico_City
TZ=America/Mexico_City
\`\`\`

\`application.yml\` configura Jackson, la JVM de la aplicacion y cada sesion
PostgreSQL con \`America/Mexico_City\`. En OpenShift tambien se recomienda
\`TZ=America/Mexico_City\` para que el proceso tenga la zona correcta desde su
arranque.


### Trazabilidad de incapacidad (2026-10-15)

El ajuste de identidad, resumenes NSSA/SIST y eventos de autorizacion requiere
`config/sql/20261015_trazabilidad_ciclo_incapacidad.sql` antes del despliegue.
Ver `documentacion/Cambios_trazabilidad_incapacidad_20261015.md` en la raiz EDI
para el detalle, el orden de despliegue y los casos de verificacion.


## Referencias de mensajes mediante enum local

`common.enums.MensajeEdi` centraliza los códigos y textos de respaldo de este
micro. Los servicios, excepciones y publicaciones a bitácora utilizan sus
referencias; los códigos públicos y sus textos actuales se conservan.

Los resolutores existentes siguen consultando primero `catalogo.edic_mensaje`.
El enum aporta un respaldo para las rutas que ya lo utilizaban y para fallos
técnicos donde la base no está disponible. Las variantes `DETALLE` conservan
los diferentes contextos técnicos de un mismo código; no son códigos nuevos.
Los mensajes recibidos de otros servicios y las búsquedas dinámicas en base
siguen aceptando sus claves; el enum no se usa como una lista restrictiva.
El catálogo y enum numérico del motor de riesgos no se migran en esta etapa.
