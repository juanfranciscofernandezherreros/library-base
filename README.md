# library-base

Arquitectura base de librería Maven multi-módulo, reutilizable por múltiples microservicios.

---

## Estructura de módulos

```
library-base/                          ← reactor raíz — library-base-build (packaging: pom)
├── parent/                            ← 1) Parent POM común
│   └── pom.xml
├── bom/                               ← 2) Bill of Materials
│   └── pom.xml
└── starters/                          ← 3) Starters (lógica reutilizable)
    ├── starter-base/                  ←    utilidades core
    ├── starter-kafka/                 ←    integración Kafka + Avro
    ├── starter-db/                    ←    integración PostgreSQL / JPA
    ├── starter-test/                  ←    utilidades de testing compartidas
    └── starter-openapi/               ←    generación de código desde spec OpenAPI
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
| `maven-compiler-plugin` | annotation processing para **Lombok + MapStruct** (ver `annotationProcessorPaths`) |
| `maven-surefire-plugin` | configuración base de tests unitarios |
| `maven-failsafe-plugin` | ejecución de tests de integración (`integration-test` + `verify`) |
| `maven-source-plugin` | adjunta fuentes al artefacto (`jar-no-fork`) |
| `maven-javadoc-plugin` | adjunta Javadoc al artefacto |
| `spring-boot-maven-plugin` | deshabilitado (`skip: true`) — librerías, no ejecutables |
| `openapi-generator-maven-plugin` | v7.4.0 — generación de código en fase `generate-sources` (activar declarando en `<plugins>`) |
| `build-helper-maven-plugin` | registra `${openapi.output-dir}/src/main/java` como source root |
| `distributionManagement` | Nexus releases + snapshots (URLs configurables por propiedad) |

**Propiedades OpenAPI sobreescribibles** (en el POM del microservicio o vía `-D` en CI):

| Propiedad | Valor por defecto |
|---|---|
| `openapi.spec-path` | `${project.basedir}/src/main/resources/openapi.yaml` |
| `openapi.output-dir` | `${project.build.directory}/generated-sources/openapi` |
| `openapi.api-package` | `${project.groupId}.api` |
| `openapi.model-package` | `${project.groupId}.api.model` |

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

Centraliza las versiones de todos los módulos de la librería y sus dependencias transitivas. Permite a los consumidores importarlo en `dependencyManagement` sin tener que indicar versiones de forma explícita.

**Versiones gestionadas:**

| Librería | Versión |
|---|---|
| Spring Boot BOM | 3.2.5 |
| Lombok | 1.18.32 |
| MapStruct | 1.5.5.Final |
| Springdoc OpenAPI | 2.3.0 |
| PostgreSQL driver | 42.7.3 |
| H2 (tests) | 2.2.224 |
| Apache Kafka | 3.9.2 |
| Apache Avro | 1.11.4 |
| JUnit Jupiter | 5.10.2 |
| Mockito | 5.7.0 |
| Zookeeper | 3.8.6 |
| swagger-parser | 2.1.22 |
| jackson-databind-nullable | 0.2.6 |

#### Uso en un microservicio

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

Proporciona auto-configuración base y el bean `BaseService`. Se activa automáticamente al incluir la dependencia (Spring Boot auto-configuration).

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

Proporciona un productor Kafka genérico (`LibraryKafkaProducer`), un consumidor base (`LibraryKafkaConsumer`) y configuración de Kafka Streams (`LibraryKafkaStreamsConfig`). Se activa únicamente si `KafkaTemplate` está en el classpath (`@ConditionalOnClass(KafkaTemplate.class)`). Incluye soporte para serialización **Apache Avro** (el serializer de Confluent Schema Registry debe añadirse en el proyecto consumidor).

**Dependencia:**

```xml
<dependency>
    <groupId>com.github.juanfranciscofernandezherreros</groupId>
    <artifactId>library-base-starter-kafka</artifactId>
</dependency>
```

**Propiedades configurables** (`application.yml`):

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: mi-consumer-group
      auto-offset-reset: earliest

library:
  kafka:
    default-topic: mis-eventos           # topic por defecto (default: library-events)
    client-id-prefix: mi-servicio        # prefijo del clientId productor (default: library)
    zookeeper-connect: localhost:2181    # conexión ZooKeeper para clústeres pre-KRaft (default: localhost:2181)
    schema-registry-url: http://localhost:8081  # URL del Schema Registry Confluent (default: http://localhost:8081)
    streams:
      application-id: mi-streams-app    # ID de la topología Kafka Streams (default: library-streams-app)
      state-dir: /tmp/kafka-streams     # directorio de estado local (default: /tmp/kafka-streams)

# Propiedad de entorno independiente — activa el bean LibraryKafkaConsumer (default: false)
library.kafka.consumer.enabled: true
```

> **Nota sobre `library.kafka.consumer.enabled`:** esta propiedad no forma parte del tipo `KafkaLibraryProperties`; se evalúa directamente desde el entorno Spring mediante `@ConditionalOnProperty`. Esto significa que no aparece en el autocompletado del IDE a través de los metadatos de configuración del starter, pero sí funciona correctamente si se define en `application.yml` o como variable de entorno.

**Productor — `LibraryKafkaProducer<V>`:**

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

Los tres métodos devuelven `CompletableFuture<SendResult<String, V>>`.

**Consumidor — `LibraryKafkaConsumer<V>`:**

El bean se registra sólo cuando la propiedad de entorno `library.kafka.consumer.enabled=true`. La clase escucha el topic configurado en `library.kafka.default-topic` usando el `group-id` de `spring.kafka.consumer.group-id`. Para personalizar el procesamiento, extiende la clase y sobreescribe `process()`:

```java
@Component
public class MiConsumer extends LibraryKafkaConsumer<MiEvento> {

    @Override
    protected void process(MiEvento payload) {
        // lógica de negocio
    }
}
```

**Kafka Streams — `LibraryKafkaStreamsConfig`:**

Proporciona la configuración por defecto de Kafka Streams (application ID, bootstrap servers, Serde String por defecto, state dir). Para activarlo, importa la clase en tu aplicación:

```java
@Import(LibraryKafkaStreamsConfig.class)
@SpringBootApplication
public class MiApplication { }
```

El bean `libraryKStream` registra una topología de paso directo (pass-through) sobre `library.kafka.default-topic`. Sobreescribe el bean en tu aplicación para añadir lógica de streaming propia.

---

#### 3.3 `library-base-starter-db` — Integración PostgreSQL / JPA

Proporciona auto-configuración de Spring Data JPA con PostgreSQL. Registra un `HibernateJpaVendorAdapter` preconfigurado para PostgreSQL. La DataSource y el `EntityManagerFactory` son gestionados por la auto-configuración estándar de Spring Boot. Incluye H2 en scope `test` para pruebas en memoria.

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

Lee un fichero de especificación OpenAPI 3 (YAML o JSON) y genera automáticamente DTOs e interfaces de controlador al arrancar la aplicación mediante un `ApplicationRunner`. Las fuentes se escriben en `target/generated-sources/openapi` por defecto.

> **Generación en tiempo de compilación:** el `parent` también declara en `pluginManagement` el `openapi-generator-maven-plugin` v7.4.0. Si necesitas que el código generado esté disponible para el compilador en la fase `compile`, actívalo en la sección `<plugins>` de tu POM (ver [Guía: usar como parent + OpenAPI YAML](#guía-usar-como-parent--openapi-yaml)).

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

Agrupa utilidades de testing: `EmbeddedKafkaTestConfig` (broker Kafka en memoria con KRaft), clase base `BaseIntegrationTest` y soporte para **Testcontainers**.

**Dependencia** (scope `test`):

```xml
<dependency>
    <groupId>com.github.juanfranciscofernandezherreros</groupId>
    <artifactId>library-base-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Dependencias transitivas incluidas:**
- `spring-boot-starter-test`
- `spring-kafka-test`
- `spring-boot-testcontainers`
- `org.testcontainers:junit-jupiter`
- `org.testcontainers:kafka`
- `com.h2database:h2`

**`BaseIntegrationTest`** — clase base abstracta que activa `@SpringBootTest` y el perfil `test`:

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

**`EmbeddedKafkaTestConfig`** — arranca un broker Kafka en memoria (KRaft, modo single-node) para tests que requieren Kafka sin un broker externo. La auto-configuración del starter (`TestAutoConfiguration`) lo importa automáticamente cuando `spring-kafka-test` está en el classpath, por lo que normalmente no es necesaria ninguna acción adicional. Si necesitas registrarlo de forma explícita en una clase de test concreta, impórtalo directamente:

```java
@Import(EmbeddedKafkaTestConfig.class)
class MiKafkaTest extends BaseIntegrationTest {

    @Autowired
    EmbeddedKafkaBroker broker;

    @Test
    void deberiaPublicarMensaje() {
        // broker disponible en el contexto
    }
}
```

---

## Configuración de Lombok + MapStruct

El `parent` configura `annotationProcessorPaths` en el orden correcto:

1. `lombok` — genera getters/setters antes de que MapStruct los lea
2. `mapstruct-processor` — genera implementaciones de mappers
3. `lombok-mapstruct-binding` — garantiza el orden de procesamiento

No es necesaria ninguna configuración adicional en los módulos consumidores.

---

## Guía: usar como parent + OpenAPI YAML

Esta sección describe cómo crear un microservicio que:
1. use `library-base-parent` como POM padre,
2. importe el BOM para gestionar versiones,
3. active el starter OpenAPI para generar código a partir de un fichero YAML.

### Paso 1 — `pom.xml` del microservicio

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 1) Heredar la configuración de plugins, Lombok/MapStruct y distribución -->
    <parent>
        <groupId>com.github.juanfranciscofernandezherreros</groupId>
        <artifactId>library-base-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>mi-microservicio</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <dependencyManagement>
        <!-- 2) Importar el BOM para no repetir versiones en cada dependencia -->
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

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 3) Starter OpenAPI — genera DTOs y controllers desde el YAML en tiempo de ejecución -->
        <dependency>
            <groupId>com.github.juanfranciscofernandezherreros</groupId>
            <artifactId>library-base-starter-openapi</artifactId>
        </dependency>
    </dependencies>

    <!-- 4) Opcional: activar generación en tiempo de compilación con el plugin Maven -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.openapitools</groupId>
                <artifactId>openapi-generator-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>build-helper-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
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

### Paso 4 — Ejecutar la aplicación

Al arrancar la aplicación (`mvn spring-boot:run`), el `ApplicationRunner` del starter lee el YAML y escribe los ficheros generados bajo `target/generated-sources/openapi/`:

```
target/generated-sources/openapi/
  com/example/miservicio/api/
    dto/
      ProductoDto.java
    controller/
      ProductosController.java
```

> **Nota:** cuando se activa el `openapi-generator-maven-plugin` en `<plugins>` (paso 1, opción 4), la generación ocurre en la fase `generate-sources` (es decir, durante `mvn compile` o cualquier fase posterior) y el `build-helper-maven-plugin` registra automáticamente `${openapi.output-dir}/src/main/java` como source root, de modo que el compilador ve las clases generadas en un `mvn package` normal. La generación en tiempo de ejecución vía `ApplicationRunner` (sin el plugin Maven) sólo ocurre al arrancar la aplicación Spring Boot y **no** durante `mvn compile`.

### Resumen del flujo

```
pom.xml
  └─ parent: library-base-parent       ← hereda plugins, Java 17, Lombok/MapStruct
  └─ BOM import: library-base-bom      ← gestiona versiones de todos los starters
  └─ dep: library-base-starter-openapi ← activa la generación en tiempo de ejecución

src/main/resources/openapi.yaml        ← tu especificación OpenAPI 3

application.yml
  library.openapi.spec-path: ...       ← apunta al YAML

mvn spring-boot:run (arranque de la aplicación)
  → ApplicationRunner genera DTOs + Controllers en target/generated-sources/openapi/

mvn compile (con plugin activado en <plugins>)
  → openapi-generator-maven-plugin genera en fase generate-sources
  → build-helper-maven-plugin registra el directorio como source root
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

Construye todos los módulos en el orden correcto: `parent` → `bom` → `starters/starter-base` → `starters/starter-kafka` → `starters/starter-db` → `starters/starter-test` → `starters/starter-openapi`.