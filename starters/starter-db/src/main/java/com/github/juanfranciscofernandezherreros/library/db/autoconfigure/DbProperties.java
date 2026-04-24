package com.github.juanfranciscofernandezherreros.library.db.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the DB library starter.
 * <p>
 * All properties are prefixed with {@code library.db}.
 *
 * <p>Example {@code application.yml}:
 * <pre>
 * spring:
 *   datasource:
 *     url: jdbc:postgresql://localhost:5432/mydb
 *     username: myuser
 *     password: secret
 *   jpa:
 *     hibernate:
 *       ddl-auto: validate
 *     show-sql: false
 * library:
 *   db:
 *     default-schema: public
 *     ddl-auto: validate
 * </pre>
 */
@ConfigurationProperties(prefix = "library.db")
public class DbProperties {

    /**
     * Default schema used for entity mappings when no explicit schema is specified.
     * Maps to the Hibernate {@code hibernate.default_schema} property.
     */
    private String defaultSchema = "public";

    /**
     * DDL auto strategy applied to Hibernate.
     * Accepted values: {@code none}, {@code validate}, {@code update}, {@code create},
     * {@code create-drop}.
     * Defaults to {@code validate} (safe for production).
     */
    private String ddlAuto = "validate";

    public String getDefaultSchema() {
        return defaultSchema;
    }

    public void setDefaultSchema(String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }

    public String getDdlAuto() {
        return ddlAuto;
    }

    public void setDdlAuto(String ddlAuto) {
        this.ddlAuto = ddlAuto;
    }
}
