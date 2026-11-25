package com.dev.cutly.suscripcion.config;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;

/** Deja el DDL de planes_suscripcion exclusivamente en manos del migrador seguro. */
public class PlanSuscripcionSchemaFilterProvider implements SchemaFilterProvider {

    private static final SchemaFilter MIGRATE_OTHER_TABLES = new SchemaFilter() {
        @Override
        public boolean includeNamespace(Namespace namespace) {
            return true;
        }

        @Override
        public boolean includeTable(Table table) {
            return !"planes_suscripcion".equalsIgnoreCase(table.getName());
        }

        @Override
        public boolean includeSequence(Sequence sequence) {
            return true;
        }
    };

    @Override
    public SchemaFilter getCreateFilter() {
        return SchemaFilter.ALL;
    }

    @Override
    public SchemaFilter getDropFilter() {
        return SchemaFilter.ALL;
    }

    @Override
    public SchemaFilter getTruncatorFilter() {
        return SchemaFilter.ALL;
    }

    @Override
    public SchemaFilter getMigrateFilter() {
        return MIGRATE_OTHER_TABLES;
    }

    @Override
    public SchemaFilter getValidateFilter() {
        return SchemaFilter.ALL;
    }
}
