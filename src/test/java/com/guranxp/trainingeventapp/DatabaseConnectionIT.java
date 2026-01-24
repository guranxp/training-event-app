package com.guranxp.trainingeventapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@ActiveProfiles("test")
class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testDatabaseConnection() throws SQLException {
        // Verify the datasource is configured
        assertThat(dataSource).isNotNull();
        
        // Test the connection
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.isValid(1)).isTrue();
        }
        
        // Test a simple query
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
    }
}
