package com.jingxu.catmaintain;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CatMaintainApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void mysqlConnectionWorks() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void businessSchemaExists() {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN (
                      'accounts', 'merchant_stores', 'products', 'orders',
                      'order_items', 'appointments', 'maintenance_records', 'reviews'
                  )
                """, String.class);

        assertThat(tables).containsExactlyInAnyOrder(
                "accounts",
                "merchant_stores",
                "products",
                "orders",
                "order_items",
                "appointments",
                "maintenance_records",
                "reviews"
        );
    }

}
