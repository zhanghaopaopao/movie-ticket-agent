package com.limou.movieticket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class TicketingApiIntegrationTest {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedTicketingSlice() {
        jdbcTemplate.update("DELETE FROM showtime_seat");
        jdbcTemplate.update("DELETE FROM showtime");
        jdbcTemplate.update("DELETE FROM seat");
        jdbcTemplate.update("DELETE FROM hall");
        jdbcTemplate.update("DELETE FROM cinema");
        jdbcTemplate.update("DELETE FROM movie");
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        jdbcTemplate.update("""
                INSERT INTO movie(id,name,english_name,genres,duration_minutes,rating,poster_url,release_date,synopsis,cast_names,want_count,status)
                VALUES ('movie_test','深空回响','Echoes from Orbit','科幻,冒险',132,9.3,'/poster.jpg',?,'synopsis','林澈,周隼',100,'NOW_SHOWING')
                """, Date.valueOf(today.minusDays(1)));
        jdbcTemplate.update("""
                INSERT INTO cinema(id,name,brand,city,district,address,latitude,longitude,service_tags,status)
                VALUES ('cinema_test','星环影城','星环','上海','徐汇区','虹桥路168号',31.1910,121.4368,'退票,改签,IMAX','ACTIVE')
                """);
        jdbcTemplate.update("""
                INSERT INTO hall(id,cinema_id,name,hall_type,seat_template_id,status)
                VALUES ('hall_test','cinema_test','7号厅','IMAX','template','ACTIVE')
                """);
        jdbcTemplate.update("""
                INSERT INTO seat(id,hall_id,row_no,seat_no,zone,seat_type,couple_group)
                VALUES ('seat_test_1','hall_test',5,7,'CENTER','STANDARD',NULL),
                       ('seat_test_2','hall_test',5,8,'CENTER','STANDARD',NULL)
                """);
        Instant start = ZonedDateTime.of(today, LocalTime.of(19, 30), BUSINESS_ZONE).toInstant();
        jdbcTemplate.update("""
                INSERT INTO showtime(id,movie_id,hall_id,start_at,end_at,base_price,language,format,status,version)
                VALUES ('show_test','movie_test','hall_test',?,?,5600,'国语','IMAX 2D','ON_SALE',1)
                """, Timestamp.valueOf(LocalDateTime.ofInstant(start, ZoneOffset.UTC)),
                Timestamp.valueOf(LocalDateTime.ofInstant(start.plusSeconds(132 * 60L), ZoneOffset.UTC)));
        jdbcTemplate.update("""
                INSERT INTO showtime_seat(id,showtime_id,seat_id,price,status,version)
                VALUES ('inv_1','show_test','seat_test_1',5600,'AVAILABLE',2),
                       ('inv_2','show_test','seat_test_2',5600,'SOLD',3)
                """);
    }

    @Test
    void movieCinemaShowtimeAndSeatQueriesReturnSeededData() throws Exception {
        mockMvc.perform(get("/api/v1/movies").param("genre", "科幻").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value("movie_test"))
                .andExpect(jsonPath("$.data.records[0].cinemaCount").value(1));

        mockMvc.perform(get("/api/v1/cinemas").param("hallType", "IMAX")
                        .param("latitude", "31.1900").param("longitude", "121.4300").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].minimumPrice").value(5600))
                .andExpect(jsonPath("$.data.records[0].distanceKm").isNumber());

        mockMvc.perform(get("/api/v1/cinemas/cinema_test/showtimes")
                        .param("movieId", "movie_test")
                        .param("date", LocalDate.now(BUSINESS_ZONE).toString()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("show_test"))
                .andExpect(jsonPath("$.data[0].startAt").value(org.hamcrest.Matchers.endsWith("+08:00")))
                .andExpect(jsonPath("$.data[0].remainingSeats").value(1));

        mockMvc.perform(get("/api/v1/showtimes/show_test/seats").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inventoryVersion").value(3))
                .andExpect(jsonPath("$.data.seats[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.seats[1].status").value("SOLD"));
    }

    @Test
    void missingMovieUsesStableNotFoundEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/movies/missing").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void anonymousTicketingRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/movies"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
    }
}
