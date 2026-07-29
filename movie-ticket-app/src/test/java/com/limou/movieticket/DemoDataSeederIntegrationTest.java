package com.limou.movieticket;

import com.limou.movieticket.seed.DemoDataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "app.demo-seed.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:movie_ticket_seed;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
class DemoDataSeederIntegrationTest {
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired DemoDataSeeder seeder;

    @Test
    void createsCompleteSevenDayDatasetAndCanRunAgain() throws Exception {
        assertSeedCounts();
        seeder.run(new DefaultApplicationArguments(new String[0]));
        assertSeedCounts();
        Integer soldOut = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM showtime WHERE status='SOLD_OUT'", Integer.class);
        Integer lowerPrice = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM showtime WHERE base_price=3000", Integer.class);
        assertThat(soldOut).isGreaterThanOrEqualTo(1);
        assertThat(lowerPrice).isGreaterThanOrEqualTo(1);
    }

    private void assertSeedCounts() {
        assertThat(count("movie")).isEqualTo(6);
        assertThat(count("cinema")).isEqualTo(3);
        assertThat(count("hall")).isEqualTo(6);
        assertThat(count("seat")).isEqualTo(336);
        assertThat(count("showtime")).isEqualTo(63);
        assertThat(count("showtime_seat")).isEqualTo(3528);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
