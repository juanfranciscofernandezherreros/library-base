package com.github.juanfranciscofernandezherreros.library.db.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import jakarta.persistence.EntityManagerFactory;

/**
 * Auto-configuration for the DB library starter.
 * <p>
 * Activates only when {@link EntityManagerFactory} is present on the classpath
 * (i.e. {@code spring-boot-starter-data-jpa} is a dependency).
 * Registers a pre-configured {@link JpaVendorAdapter} for PostgreSQL with
 * settings derived from {@link DbProperties} unless a custom adapter already exists.
 * <p>
 * DataSource and EntityManagerFactory beans are provided by
 * Spring Boot's own JPA auto-configuration; this class only enriches the
 * vendor-specific defaults.
 */
@AutoConfiguration
@ConditionalOnClass(EntityManagerFactory.class)
@EnableConfigurationProperties(DbProperties.class)
public class DbAutoConfiguration {

    /**
     * Provides a {@link HibernateJpaVendorAdapter} targeting PostgreSQL.
     * <p>
     * The adapter sets {@code showSql} to {@code false} and targets the
     * {@link Database#POSTGRESQL} dialect. The DDL strategy is intentionally
     * left to Spring Boot's {@code spring.jpa.hibernate.ddl-auto} property so
     * that the consuming application retains full control.
     *
     * @return a PostgreSQL-specific JPA vendor adapter
     */
    @Bean
    @ConditionalOnMissingBean(JpaVendorAdapter.class)
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setDatabase(Database.POSTGRESQL);
        adapter.setShowSql(false);
        adapter.setGenerateDdl(false);
        return adapter;
    }
}
