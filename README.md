# library-base

Arquitectura base de librería Maven multi-módulo, reutilizable por múltiples microservicios.

---

## Estructura de módulos

```
library-base/                          ← reactor raíz (packaging: pom)
├── parent/                            ← 1) Parent POM común
│   └── pom.xml
├── bom/                               ← 2) Bill of Materials
│   └── pom.xml
└── starters/                          ← 3) Starters (lógica reutilizable)
    ├── starter-base/                  ←    utilidades core
    ├── starter-kafka/                 ←    integración Kafka
    ├── starter-db/                    ←    integración PostgreSQL / JPA
    ├── starter-openapi/               ←    generación de código desde spec OpenAPI
    └── starter-test/                  ←    utilidades de testing compartidas
```

---

## Capas de la arquitectura

### 1. Parent POM — `library-base-parent`

**Artefacto:** `com.github.juanfranciscofernandezherreros:library-base-parent`

Módulo con `packaging: pom` sin código fuente. Define:

| Configuración | Valor |
|---|---|
| Java version | 17 |
| Source encoding | UTF-8 |
| Spring Boot parent | 3.2.5 |
| `maven-compiler-plugin` | annotation processing para **Lombok + MapStruct** |
| `maven-surefire-plugin` | configuración base de tests unitarios |
| `maven-failsafe-plugin` | ejecución de tests de integración |
| `maven-source-plugin` | adjunta fuentes al artefacto |
| `maven-javadoc-plugin` | adjunta Javadoc al artefacto |
| `spring-boot-maven-plugin` | deshabilitado (`skip: true`) — librerías, no ejecutables |
| `distributionManagement` | Nexus releases + snapshots (URLs configurables por propiedad) |

#### Uso como parent en un microservicio

```xml
<parent>
    <groupId>com.github.juanfranciscofernandezherreros</groupId>
    <artifactId>library-base-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
```

#### Sobrescribir URLs de Nexus/Artifactory

Las URLs se parametrizan para poder sobreescribirse sin modificar el POM:

```xml
<!-- En el pom.xml del microservicio o vía -D en CI -->
<properties>
    <nexus.releases.url>https://mi-nexus/repository/maven-releases/</nexus.releases.url>
    <nexus.snapshots.url>https://mi-nexus/repository/maven-snapshots/</nexus.snapshots.url>
</properties>
```

O directamente en el pipeline de CI:

```bash
mvn deploy \
  -Dnexus.releases.url=https://mi-nexus/repository/maven-releases/ \
  -Dnexus.snapshots.url=https://mi-nexus/repository/maven-snapshots/
```

---

### 2. Bill of Materials — `library-base-bom`

**Artefacto:** `com.github.juanfranciscofernandezherreros:library-base-bom`

Centraliza las versiones de todos los módulos de la librería. Se mantiene por compatibilidad con proyectos que no usen `library-base-parent` como parent. Los microservicios que **sí** usen `library-base-parent` no necesitan importarlo: la gestión de versiones ya está incluida en el parent.

#### Uso en un microservicio sin `library-base-parent`

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.github.juanfranciscofernandezherreros</groupId>
            <artifactId>library-base-bom</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Tras importar el BOM, las dependencias individuales no necesitan versión:

```xml
<dependencies>
    <dependency>
        <groupId>com.github.juanfranciscofernandezherreros</groupId>
        <artifactId>library-base-starter-base</artifactId>
    </dependency>
</dependencies>
```

---

### 3. Starters

#### 3.1 `library-base-starter-base` — Utilidades core

Proporciona auto-configuración base y el bean `BaseService`.

**Dependencia:**

```xml
<dependency>
    <groupId>com.github.juanfranciscofernandezherreros</groupId>
    <artifactId>library-base-starter-base</artifactId>
</dependency>
```

**Propiedades configurables** (`application.yml`):

```yaml
library:
  base:
    application-name: mi-servicio   # nombre expuesto por el bean (default: library-base)
    verbose-logging: true            # logging detallado (default: false)
```

**Bean disponible:**

```java
@Autowired
BaseService baseService;

baseService.getApplicationName();   // → "mi-servicio"
baseService.isVerboseLogging();     // → true
```

---

#### 3.2 `library-base-starter-kafka` — Integración Kafka

Proporciona un productor Kafka genérico (`LibraryKafkaProducer`) con auto-configuración. Se activa únicamente si `KafkaTemplate` está en el classpath.

**Dependencia:**

```xml
<dependency>
    <groupId>com.github.juanfranciscofernandezherreros</groupId>
    <artifactId>library-base-starter-kafka</artifactId>
</dependency>
```

**Propiedades configurables** (`application.yml`):

```yaml
library:
  kafka:
    default-topic: mis-eventos       # topic por defecto (default: library-events)
    client-id-prefix: mi-servicio    # prefijo del clientId productor (default: library)
```

**Bean disponible:**

```java
@Autowired
LibraryKafkaProducer<MiEvento> producer;

// Enviar al topic por defecto
producer.send(miEvento);

// Enviar con clave de partición
producer.send("clave", miEvento);

// Enviar a un topic explícito
producer.send("otro-topic", "clave", miEvento);
```

---

#### 3.3 `library-base-starter-db` — Integración PostgreSQL / JPA

Proporciona auto-configuración de Spring Data JPA con PostgreSQL. Incluye H2 en scope `test` para pruebas en memoria.

**Dependencia:**

```xml
<dependency>
    <groupId>com.github.juanfranciscofernandezherreros</groupId>
    <artifactId>library-base-starter-db</artifactId>
</dependency>
```

**Propiedades configurables** (`application.yml`):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: myuser
    password: secret
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

library:
  db:
    default-schema: public   # schema Hibernate por defecto (default: public)
    ddl-auto: validate       # estrategia DDL (default: validate)
```

---

#### 3.4 `library-base-starter-openapi` — Generación de código desde spec OpenAPI

Lee un fichero de especificación OpenAPI 3 (YAML o JSON) y genera automáticamente DTOs e interfaces de controlador al arrancar la aplicación. Las fuentes se escriben en `target/generated-sources/openapi` por defecto.

**Dependencia:**

```xml
<dependency>
    <groupId>com.github.juanfranciscofernandezherreros</groupId>
    <artifactId>library-base-starter-openapi</artifactId>
</dependency>
```

**Propiedades configurables** (`application.yml`):

```yaml
library:
  openapi:
    spec-path: src/main/resources/openapi.yaml   # ruta al fichero YAML/JSON (obligatorio)
    output-dir: target/generated-sources/openapi  # directorio de salida (default)
    base-package: com.example.api                 # paquete raíz del código generado (default: generated.api)
    enabled: true                                 # activa/desactiva la generación (default: true)
```

El valor de `spec-path` acepta:
- Ruta relativa o absoluta al sistema de ficheros: `src/main/resources/openapi.yaml`
- Recurso del classpath: `classpath:openapi.yaml`
- URL HTTP/HTTPS: `https://raw.githubusercontent.com/.../openapi.yaml`

**Salida generada** (para `base-package: com.example.api`):

```
target/generated-sources/openapi/
  com/example/api/
    dto/           ← un *Dto.java por cada schema definido en components/schemas
    controller/    ← un *Controller.java por cada tag (o "Default" si no hay tag)
```

Consulta el apartado [Guía: usar como parent + OpenAPI YAML](#guía-usar-como-parent--openapi-yaml) para ver un ejemplo completo de integración.

---

#### 3.5 `library-base-starter-test` — Utilidades de testing compartidas

Agrupa utilidades de testing: EmbeddedKafka y clase base `BaseIntegrationTest`.

**Dependencia** (scope `test`):

```xml
<dependency>
    <groupId>com.github.juanfranciscofernandezherreros</groupId>
    <artifactId>library-base-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Uso:**

```java
@ExtendWith(SpringExtension.class)
class MiServicioIntegrationTest extends BaseIntegrationTest {

    @Autowired
    MiServicio miServicio;

    @Test
    void deberiaFuncionar() {
        // perfil "test" activo automáticamente
    }
}
```

---

## Configuración de Lombok + MapStruct

El `parent` configura `annotationProcessorPaths` en el orden correcto:

1. `lombok` — genera getters/setters antes de que MapStruct los lea
2. `mapstruct-processor` — genera implementaciones de mappers
3. `lombok-mapstruct-binding` — garantiza el orden de procesamiento

No es necesario ninguna configuración adicional en los módulos consumidores.

---

## Guía: usar como parent + OpenAPI YAML

Esta sección describe cómo crear un microservicio que:
1. use `library-base-parent` como POM padre,
2. importe el BOM para gestionar versiones,
3. active el starter OpenAPI para generar código a partir de un fichero YAML.

### Paso 1 — `pom.xml` del microservicio

El microservicio sólo necesita declarar el parent y sus dependencias. La gestión de versiones (dependencyManagement) ya está incluida en `library-base-parent`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Heredar la configuración de plugins, Lombok/MapStruct, versiones y distribución -->
    <parent>
        <groupId>com.github.juanfranciscofernandezherreros</groupId>
        <artifactId>library-base-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>mi-microservicio</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Starter OpenAPI — genera DTOs y controllers desde el YAML -->
        <dependency>
            <groupId>com.github.juanfranciscofernandezherreros</groupId>
            <artifactId>library-base-starter-openapi</artifactId>
        </dependency>
    </dependencies>
</project>
```

### Paso 2 — Añadir el fichero de especificación

Coloca tu fichero OpenAPI 3 en `src/main/resources/`:

```
src/main/resources/
  openapi.yaml
```

Ejemplo mínimo de `openapi.yaml`:

```yaml
openapi: "3.0.3"
info:
  title: Mi API
  version: "1.0.0"

paths:
  /productos:
    get:
      tags: [productos]
      operationId: listarProductos
      summary: Lista todos los productos
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/Producto'

components:
  schemas:
    Producto:
      type: object
      properties:
        id:
          type: integer
          format: int64
        nombre:
          type: string
        precio:
          type: number
          format: double
```

### Paso 3 — Configurar `application.yml`

```yaml
library:
  openapi:
    spec-path: src/main/resources/openapi.yaml
    output-dir: target/generated-sources/openapi
    base-package: com.example.miservicio.api
    enabled: true
```

### Paso 4 — Compilar

Al ejecutar `mvn spring-boot:run` o `mvn compile`, el `ApplicationRunner` del starter lee el YAML y escribe los ficheros generados bajo `target/generated-sources/openapi/`:

```
target/generated-sources/openapi/
  com/example/miservicio/api/
    dto/
      ProductoDto.java
    controller/
      ProductosController.java
```

> **Nota:** para que el compilador vea las clases generadas en un `mvn package` normal, añade el directorio de salida como source root con el plugin `build-helper-maven-plugin`:
>
> ```xml
> <plugin>
>     <groupId>org.codehaus.mojo</groupId>
>     <artifactId>build-helper-maven-plugin</artifactId>
>     <executions>
>         <execution>
>             <id>add-generated-sources</id>
>             <phase>generate-sources</phase>
>             <goals><goal>add-source</goal></goals>
>             <configuration>
>                 <sources>
>                     <source>target/generated-sources/openapi</source>
>                 </sources>
>             </configuration>
>         </execution>
>     </executions>
> </plugin>
> ```

### Resumen del flujo

```
pom.xml
  └─ parent: library-base-parent       ← hereda plugins, Java 17, Lombok/MapStruct y versiones
  └─ dep: library-base-starter-openapi ← activa la generación

src/main/resources/openapi.yaml        ← tu especificación OpenAPI 3

application.yml
  library.openapi.spec-path: ...       ← apunta al YAML

mvn compile / spring-boot:run
  → ApplicationRunner genera DTOs + Controllers en target/generated-sources/openapi/
```

---

## Publicación

```bash
# Publicar snapshot
mvn deploy \
  -Dnexus.snapshots.url=https://mi-nexus/repository/maven-snapshots/

# Publicar release
mvn deploy \
  -Dnexus.releases.url=https://mi-nexus/repository/maven-releases/
```

Las credenciales se configuran en `~/.m2/settings.xml`:

```xml
<servers>
    <server>
        <id>nexus-releases</id>
        <username>usuario</username>
        <password>contraseña</password>
    </server>
    <server>
        <id>nexus-snapshots</id>
        <username>usuario</username>
        <password>contraseña</password>
    </server>
</servers>
```

---

## Build completo

```bash
mvn clean install
```

Construye todos los módulos en el orden correcto: `parent` → `bom` → `starters/starter-base` → `starters/starter-kafka` → `starters/starter-db` → `starters/starter-openapi` → `starters/starter-test`.