# Despliegue de MiniSuper-Hub en Tomcat como servicio de Windows

## Configuración descubierta en el proyecto

- Java declarado: **17** (`pom.xml`); la compilación verificada en este equipo se ejecutó con OpenJDK **25.0.4.1**, usando `--release 17`.
- Spring Boot: **4.1.1**.
- Compilación: Maven Wrapper (`mvnw.cmd`, Maven 3.9.16).
- Aplicación web MVC con Thymeleaf y Spring Security.
- Base de datos: PostgreSQL; la configuración de desarrollo usa `localhost:5432/MiniSuper-Hub`.
- El proyecto no tenía empaquetado WAR ni inicializador de contenedor Servlet.
- La aplicación usa rutas Thymeleaf context-aware (`@{...}`), por lo que puede desplegarse bajo `/minisuper-hub`.
- Spring Boot 4.1.x requiere un contenedor Servlet 6.1 compatible. El WAR construido resuelve Tomcat embebido **11.0.24**; para despliegue externo use **Apache Tomcat 11.0.x** actualizado y Java 17 o posterior. No use Tomcat 9 ni Tomcat 10.1 para esta aplicación.

## Cambios realizados

- El artefacto Maven ahora es `target/minisuper-hub.war`.
- `MiniSuperHubApplication` extiende `SpringBootServletInitializer`, permitiendo el arranque por el contenedor.
- `spring-boot-starter-tomcat` se declara con alcance `provided`: el Tomcat instalado ejecuta el WAR desplegado. El plugin de Spring Boot permite que el mismo WAR siga siendo ejecutable con `java -jar` para Docker/desarrollo.
- `Dockerfile` se actualizó para construir y ejecutar el WAR.
- Se agregó `application-prod.properties`: requiere `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`, valida el esquema sin alterarlo y desactiva la creación de usuarios de demostración.
- `empleadosUsersInitializer` ahora se puede desactivar por propiedad. En producción se desactiva porque además de crear cuentas demo migra/elimina una columna antigua del esquema.
- La aplicación escucha `PORT` cuando está definido (Render lo proporciona con valor predeterminado `10000`) y conserva `8080` en desarrollo local.

## Alternativa: desplegar el Dockerfile en Render

Este modo es distinto al servicio Tomcat de Windows: Render ejecuta el WAR como aplicación Spring Boot con su Tomcat embebido. Si necesitas específicamente el servicio Windows, no uses el formulario de Render.

- Elige Docker, deja Root Directory vacío si el repositorio está en su raíz y usa una región igual a la de PostgreSQL para conexión privada.
- Render despliega una rama remota, no los cambios sin commit del equipo local. Sube los cambios a Git y selecciona la rama que los contiene; si los integras en `main`, selecciona `main`.
- Configura `SPRING_PROFILES_ACTIVE=prod`, `DB_USERNAME`, `DB_PASSWORD` y `DB_URL`. `DB_URL` debe tener formato JDBC (`jdbc:postgresql://<host-completo>:5432/minisuperhub`), usando el hostname interno completo de la URL de conexión de Render; no pegues la URL `postgresql://` entera en `DB_URL`.
- No necesitas definir `PORT` manualmente salvo que quieras cambiar el valor automático de Render. El plan Free puede suspender el servicio cuando no recibe tráfico; usa un plan activo si necesitas disponibilidad continua.
- La base debe tener ya el esquema validado y una cuenta administrativa; el perfil `prod` no crea tablas ni usuarios.

## 1. Requisitos del equipo Windows

1. Instale un JDK/JRE de 64 bits, versión 17 o posterior, y compruebe `java -version`.
2. Instale Apache Tomcat **11.0.x** usando el instalador de Windows con servicio o registre la distribución ZIP como servicio. Seleccione el Java instalado y deje el servicio con inicio **Automático (inicio retrasado)**.
3. Instale PostgreSQL y asegúrese de que la base `MiniSuper-Hub` existe, acepta conexiones desde el servicio Tomcat y tiene un usuario de aplicación con permisos mínimos necesarios.
4. Configure PostgreSQL para iniciar automáticamente antes que Tomcat. Si ambos son servicios locales, revise el nombre real del servicio PostgreSQL y establezca el orden de inicio/dependencia de acuerdo con la política del servidor.

El servicio Tomcat inicia la aplicación al arrancar Windows, aunque nadie haya iniciado sesión. No se debe iniciar esta aplicación ejecutando un JAR ni abrir una sesión de usuario para arrancarla.

## 2. Preparar base de datos y cuentas

Antes de desplegar, haga un respaldo y confirme que el esquema de PostgreSQL coincide con las entidades actuales. El perfil `prod` usa `spring.jpa.hibernate.ddl-auto=validate`: si faltan tablas/columnas o no coinciden los tipos, la aplicación fallará al arrancar en vez de modificar datos automáticamente.

El proyecto no incluye Flyway/Liquibase ni scripts versionados de migración. Por tanto, la estructura de tablas y roles debe prepararse/migrarse explícitamente antes del despliegue. En particular, si la base aún conserva la columna antigua `usuarios.rol`, revise y ejecute la migración de roles de forma controlada, con respaldo; el inicializador que hacía esa migración se desactiva en producción.

También debe existir al menos una cuenta administrativa válida en las tablas de usuarios/roles. El perfil de producción no crea `admin/admin` ni `empleado/empleado`. Compruebe el login en una copia de la base antes de poner el servicio en producción.

## 3. Configurar el perfil y credenciales del servicio

En **Variables de entorno del sistema** (no solo variables del usuario que compila), agregue:

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://127.0.0.1:5432/MiniSuper-Hub
DB_USERNAME=<usuario PostgreSQL de la aplicación>
DB_PASSWORD=<secreto fuerte de PostgreSQL>
```

Cambie host/puerto si PostgreSQL está en otro equipo. No incluya la contraseña en el repositorio ni en este documento. Para un servidor productivo, limite los permisos NTFS de configuración/logs y valore un mecanismo de secretos administrado en vez de una variable persistente.

Después de cambiar variables del sistema, **reinicie el servicio Tomcat** (o Windows) para que el proceso del servicio herede los nuevos valores. La cuenta de servicio de Tomcat debe tener permiso para leer los recursos del servidor y conectarse a PostgreSQL.

## 4. Compilar el WAR

Desde PowerShell, en la carpeta raíz del repositorio:

```powershell
java -version
.\mvnw.cmd -DskipTests clean package
```

El resultado esperado es:

```text
target\minisuper-hub.war
```

No copie el archivo `*.war.original` a Tomcat. Para revisar el archivo localmente puede usar:

```powershell
jar tf target\minisuper-hub.war
```

Compruebe que contiene `WEB-INF\web.xml` o las clases bajo `WEB-INF\classes` y dependencias bajo `WEB-INF\lib`. Las dependencias del contenedor marcadas como `provided` pueden aparecer en `WEB-INF\lib-provided` para conservar la modalidad ejecutable del WAR.

## 5. Desplegar en Tomcat

1. Identifique `CATALINA_BASE` de la instalación/servicio Tomcat. En una instalación estándar suele estar bajo `C:\Program Files\Apache Software Foundation\Tomcat 11.0`, pero use la ruta real del equipo.
2. Detenga el servicio Tomcat desde `services.msc`.
3. Copie `target\minisuper-hub.war` a `%CATALINA_BASE%\webapps\minisuper-hub.war`.
4. Inicie o reinicie el servicio Tomcat. Tomcat expandirá el WAR y desplegará la aplicación.
5. Desde el propio servidor, compruebe `http://localhost:8080/minisuper-hub/login`. Desde otra máquina use el nombre/IP del servidor y abra el puerto correspondiente solo para las redes necesarias.

El contexto será `/minisuper-hub` por el nombre del WAR. Si se necesita que la aplicación responda en `/`, el archivo debe desplegarse como `ROOT.war`; no cambie el nombre sin actualizar el procedimiento de operación.

Para cada actualización, detenga Tomcat, respalde el WAR y la carpeta expandida `webapps\minisuper-hub`, reemplace el WAR y vuelva a iniciar el servicio. No borre otras aplicaciones de `webapps`.

## 6. Verificación de arranque automático y diagnóstico

- En `services.msc`, confirme que el servicio Apache Tomcat está en ejecución y configurado como **Automático (inicio retrasado)**.
- Reinicie el equipo y compruebe el endpoint de login sin iniciar sesión en Windows.
- Revise `%CATALINA_BASE%\logs\catalina*.log` y `localhost*.log` si no despliega. Busque errores de conexión PostgreSQL, validación Hibernate, versión Java o carga de clases.
- Si Tomcat inicia pero el WAR falla, confirme que está usando Java 17+, Tomcat 11.0.x, que el perfil `prod` se heredó y que las tres variables `DB_*` están disponibles para el proceso del servicio.
- Evite habilitar la aplicación Manager de Tomcat en una interfaz pública; restrinja el acceso administrativo al host/red de mantenimiento.

## Nota sobre verificación en este entorno

El empaquetado debe verificarse en un Windows con JDK 17+ ejecutando el comando anterior. El terminal de este entorno de desarrollo no pudo iniciar porque le falta `libasound.so.2`; por eso no se afirma que el WAR se haya compilado aquí.
