package com.dev.cutly.suscripcion.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/** Ejecuta el preflight antes de que Hibernate inicie su propio schema update. */
@Component("planSuscripcionSchemaBootstrap")
public class PlanSuscripcionSchemaBootstrap implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(PlanSuscripcionSchemaBootstrap.class);

    private final PlanSuscripcionSchemaMigrator migrator;
    private final PlanSuscripcionReadiness readiness;

    public PlanSuscripcionSchemaBootstrap(
            PlanSuscripcionSchemaMigrator migrator,
            PlanSuscripcionReadiness readiness
    ) {
        this.migrator = migrator;
        this.readiness = readiness;
    }

    @Bean
    static BeanFactoryPostProcessor planSchemaBeforeEntityManagerFactory() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
                throw new IllegalStateException("No se encontró entityManagerFactory para ordenar la migración de planes");
            }
            var definition = beanFactory.getBeanDefinition("entityManagerFactory");
            String[] dependencies = definition.getDependsOn();
            if (dependencies == null) {
                definition.setDependsOn("planSuscripcionSchemaBootstrap");
            } else if (Arrays.stream(dependencies).noneMatch("planSuscripcionSchemaBootstrap"::equals)) {
                String[] ordered = Arrays.copyOf(dependencies, dependencies.length + 1);
                ordered[dependencies.length] = "planSuscripcionSchemaBootstrap";
                definition.setDependsOn(ordered);
            }
        };
    }

    @Override
    public void afterPropertiesSet() {
        try {
            migrator.migrar();
        } catch (RuntimeException e) {
            String diagnostic = e.getMessage() == null
                    ? "La migración de planes falló; revisar los logs de arranque"
                    : e.getMessage();
            logger.error("El preflight del esquema de planes falló antes de Hibernate; readiness queda bloqueada", e);
            readiness.markBlocked(List.of(diagnostic));
        }
    }
}
