package com.github.juanfranciscofernandezherreros.library.db.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import static org.assertj.core.api.Assertions.assertThat;

class DbAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DbAutoConfiguration.class));

    @Test
    void registersJpaVendorAdapter() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(JpaVendorAdapter.class));
    }

    @Test
    void registersHibernateJpaVendorAdapter() {
        contextRunner.run(context ->
                assertThat(context.getBean(JpaVendorAdapter.class))
                        .isInstanceOf(HibernateJpaVendorAdapter.class));
    }

    @Test
    void backsOffIfJpaVendorAdapterAlreadyDefined() {
        contextRunner
                .withBean(JpaVendorAdapter.class, HibernateJpaVendorAdapter::new)
                .run(context -> assertThat(context).hasSingleBean(JpaVendorAdapter.class));
    }

    @Test
    void registersDbPropertiesWithDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DbProperties.class);
            DbProperties props = context.getBean(DbProperties.class);
            assertThat(props.getDefaultSchema()).isEqualTo("public");
            assertThat(props.getDdlAuto()).isEqualTo("validate");
        });
    }

    @Test
    void respectsCustomDbProperties() {
        contextRunner
                .withPropertyValues(
                        "library.db.default-schema=myschema",
                        "library.db.ddl-auto=none")
                .run(context -> {
                    DbProperties props = context.getBean(DbProperties.class);
                    assertThat(props.getDefaultSchema()).isEqualTo("myschema");
                    assertThat(props.getDdlAuto()).isEqualTo("none");
                });
    }
}
