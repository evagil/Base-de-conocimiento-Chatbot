package com.municipio.base_conocimiento;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class FlywayMigrationTest {
    @Test
    void migracionInicialCreaAreasYSubareasDesdeCero() throws Exception {
        String url = "jdbc:h2:mem:flyway_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").load().migrate();

        try (var conexion = DriverManager.getConnection(url, "sa", "");
             var consulta = conexion.createStatement().executeQuery(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
                             "WHERE TABLE_NAME IN ('AREAS', 'SUBAREAS')")) {
            assertThat(consulta.next()).isTrue();
            assertThat(consulta.getInt(1)).isEqualTo(2);
        }
    }
}
