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
