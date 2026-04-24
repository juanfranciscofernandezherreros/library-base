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
├── starters/                          ← 3) Starters (lógica reutilizable)
│   ├── base/                          ←    starter-base: utilidades core
│   └── kafka/                         ←    starter-kafka: integración Kafka
└── starters-test/                     ← 4) Starter de testing compartido
    └── pom.xml
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

Centraliza las versiones de todos los módulos de la librería. Permite a los consumidores importarlo en `dependencyManagement` y no tener que indicar versiones de forma explícita.

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

### 4. Starter de Testing — `library-base-starter-test`

Agrupa utilidades de testing compartidas: Testcontainers, EmbeddedKafka y clase base `BaseIntegrationTest`.

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

Construye todos los módulos en el orden correcto: `parent` → `bom` → `starters/base` → `starters/kafka` → `starters-test`.