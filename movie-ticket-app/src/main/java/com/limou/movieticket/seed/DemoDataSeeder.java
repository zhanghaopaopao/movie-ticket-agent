package com.limou.movieticket.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Component
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DemoDataSeeder(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder,
                          @Value("${app.demo-seed.password}") String demoPassword) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUsers();
        seedMovies();
        seedCinemasAndHalls();
        seedSeats();
        seedShowtimesAndInventory();
        log.info("Demo data is ready: 6 movies, 3 cinemas, 6 halls and seven days of showtimes");
    }

    private void seedUsers() {
        upsertUser("user_demo", "user@demo.local", "USER");
        upsertUser("admin_demo", "admin@demo.local", "ADMIN");
    }

    private void upsertUser(String id, String email, String role) {
        String hash = passwordEncoder.encode(demoPassword);
        int updated = jdbcTemplate.update("""
                UPDATE app_user SET email=?,password_hash=?,role=?,status='ACTIVE',login_failure_count=0,
                    locked_until=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=?
                """, email, hash, role, id);
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO app_user(id,email,password_hash,role,status,login_failure_count,created_at,updated_at)
                    VALUES (?,?,?,?,'ACTIVE',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                    """, id, email, hash, role);
        }
    }

    private void seedMovies() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        MovieSeed[] movies = {
                new MovieSeed("movie_1001", "深空回响", "Echoes from Orbit", "科幻,冒险", 132, "9.3", today.minusDays(3), "一次失联的深空任务向地球传回神秘信号，年轻工程师必须在最后十二小时内解开信号背后的真相。", "林澈,周隼,许雯", 186000L, "NOW_SHOWING"),
                new MovieSeed("movie_1002", "夏日来信", "Letters in Summer", "剧情,爱情", 118, "9.1", today.minusDays(8), "两封跨越十年的来信，让旧友重新面对未完成的约定。", "闻溪,陈舟", 92000L, "NOW_SHOWING"),
                new MovieSeed("movie_1003", "逆风而行", "Against the Wind", "动作,犯罪", 126, "8.7", today.plusDays(18), "退役车手被卷入城市追逐，在逆风中找回自己的选择。", "韩川,程冉", 827000L, "COMING_SOON"),
                new MovieSeed("movie_1004", "月面计划", "Lunar Plan", "科幻,家庭", 109, "8.8", today.minusDays(1), "一家人参与月面实验计划，在遥远基地重新理解彼此。", "江白,周棠", 115000L, "NOW_SHOWING"),
                new MovieSeed("movie_1005", "城市漫游", "City Walkers", "喜剧,都市", 104, "8.6", today.minusDays(12), "三个陌生人在同一天迷路，意外完成一场城市寻宝。", "苏木,罗真", 73000L, "NOW_SHOWING"),
                new MovieSeed("movie_1006", "极夜信号", "Polar Night Signal", "悬疑,惊悚", 121, "8.9", today.plusDays(5), "极夜观测站收到来自废弃频道的求救信号。", "陆遥,许衡", 236000L, "COMING_SOON")
        };
        for (MovieSeed movie : movies) {
            Object[] values = movieValues(movie);
            int updated = jdbcTemplate.update("""
                    UPDATE movie SET name=?,english_name=?,genres=?,duration_minutes=?,rating=?,poster_url=?,release_date=?,
                        synopsis=?,cast_names=?,want_count=?,status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?
                    """, append(values, movie.id()));
            if (updated == 0) {
                jdbcTemplate.update("""
                        INSERT INTO movie(id,name,english_name,genres,duration_minutes,rating,poster_url,release_date,
                            synopsis,cast_names,want_count,status,created_at,updated_at)
                        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                        """, prepend(movie.id(), values));
            }
        }
    }

    private Object[] movieValues(MovieSeed movie) {
        return new Object[]{movie.name(), movie.englishName(), movie.genres(), movie.duration(),
                new BigDecimal(movie.rating()), "/assets/posters/" + movie.id() + ".jpg", Date.valueOf(movie.releaseDate()),
                movie.synopsis(), movie.cast(), movie.wantCount(), movie.status()};
    }

    private void seedCinemasAndHalls() {
        CinemaSeed[] cinemas = {
                new CinemaSeed("cinema_2001", "星环影城（徐家汇店）", "星环影城", "徐汇区", "虹桥路168号城市广场6层", "31.1910", "121.4368", "退票,改签,小吃,IMAX,杜比"),
                new CinemaSeed("cinema_2002", "光屿电影中心（港汇店）", "光屿电影", "徐汇区", "华山路2088号港汇中心5层", "31.1940", "121.4375", "退票,改签,停车,激光厅"),
                new CinemaSeed("cinema_2003", "映界影城（衡山路店）", "映界影城", "徐汇区", "衡山路890号映界剧场2层", "31.2020", "121.4430", "改签,小吃,巨幕厅,情侣座")
        };
        for (CinemaSeed cinema : cinemas) upsertCinema(cinema);

        HallSeed[] halls = {
                new HallSeed("hall_3001", "cinema_2001", "7号厅", "IMAX"),
                new HallSeed("hall_3002", "cinema_2001", "3号厅", "杜比"),
                new HallSeed("hall_3003", "cinema_2002", "5号厅", "激光厅"),
                new HallSeed("hall_3004", "cinema_2002", "2号厅", "普通厅"),
                new HallSeed("hall_3005", "cinema_2003", "8号厅", "巨幕厅"),
                new HallSeed("hall_3006", "cinema_2003", "情侣厅", "情侣厅")
        };
        for (HallSeed hall : halls) upsertHall(hall);
    }

    private void upsertCinema(CinemaSeed cinema) {
        Object[] values = {cinema.name(), cinema.brand(), "上海", cinema.district(), cinema.address(),
                new BigDecimal(cinema.latitude()), new BigDecimal(cinema.longitude()), cinema.tags(), "ACTIVE"};
        int updated = jdbcTemplate.update("""
                UPDATE cinema SET name=?,brand=?,city=?,district=?,address=?,latitude=?,longitude=?,service_tags=?,status=?,
                    updated_at=CURRENT_TIMESTAMP WHERE id=?
                """, append(values, cinema.id()));
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO cinema(id,name,brand,city,district,address,latitude,longitude,service_tags,status,created_at,updated_at)
                    VALUES (?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                    """, prepend(cinema.id(), values));
        }
    }

    private void upsertHall(HallSeed hall) {
        int updated = jdbcTemplate.update("""
                UPDATE hall SET name=?,hall_type=?,status='ACTIVE',updated_at=CURRENT_TIMESTAMP WHERE id=?
                """, hall.name(), hall.type(), hall.id());
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO hall(id,cinema_id,name,hall_type,seat_template_id,status,created_at,updated_at)
                    VALUES (?,?,?,?,?,'ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                    """, hall.id(), hall.cinemaId(), hall.name(), hall.type(), "template_7x8");
        }
    }

    private void seedSeats() {
        Set<String> existing = existingIds("seat");
        List<Object[]> rows = new ArrayList<>();
        for (int hall = 3001; hall <= 3006; hall++) {
            String hallId = "hall_" + hall;
            for (int row = 1; row <= 7; row++) {
                for (int number = 1; number <= 8; number++) {
                    String id = "seat_" + hall + "_" + row + "_" + number;
                    if (existing.contains(id)) continue;
                    String zone = row <= 2 ? "FRONT" : row <= 5 ? "CENTER" : "BACK";
                    boolean unavailable = (row == 2 && number == 2) || (row == 4 && number == 6);
                    boolean couple = row == 7 && number >= 7;
                    String type = unavailable ? "UNAVAILABLE" : couple ? "COUPLE" : "STANDARD";
                    rows.add(new Object[]{id, hallId, row, number, zone, type, couple ? "couple_" + hall + "_7" : null});
                }
            }
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO seat(id,hall_id,row_no,seat_no,zone,seat_type,couple_group) VALUES (?,?,?,?,?,?,?)
                """, rows);
    }

    private void seedShowtimesAndInventory() {
        String[][] halls = {{"hall_3001", "hall_3002"}, {"hall_3003", "hall_3004"}, {"hall_3005", "hall_3006"}};
        String[] movieIds = {"movie_1001", "movie_1002", "movie_1004", "movie_1005"};
        int[] durations = {132, 118, 109, 104};
        LocalTime[] starts = {LocalTime.of(16, 0), LocalTime.of(18, 40), LocalTime.of(20, 50)};
        Set<String> existingShowtimes = existingIds("showtime");
        Set<String> existingInventory = existingIds("showtime_seat");
        Map<String, List<SeatSeed>> seatsByHall = loadSeatsByHall();
        List<Object[]> showtimeRows = new ArrayList<>();
        List<Object[]> inventoryRows = new ArrayList<>();
        LocalDate today = LocalDate.now(BUSINESS_ZONE);

        for (int day = 0; day < 7; day++) {
            LocalDate date = today.plusDays(day);
            for (int cinema = 0; cinema < halls.length; cinema++) {
                for (int slot = 0; slot < starts.length; slot++) {
                    int movieIndex = (day + cinema + slot) % movieIds.length;
                    String hallId = halls[cinema][slot % 2];
                    String showtimeId = "show_" + date.toString().replace("-", "") + "_" + (cinema + 1) + "_" + (slot + 1);
                    ZonedDateTime localStart = ZonedDateTime.of(date, starts[slot], BUSINESS_ZONE);
                    LocalDateTime startUtc = LocalDateTime.ofInstant(localStart.toInstant(), ZoneOffset.UTC);
                    LocalDateTime endUtc = LocalDateTime.ofInstant(localStart.plusMinutes(durations[movieIndex]).toInstant(), ZoneOffset.UTC);
                    int price = cinema == 2 && slot == 0 ? 3000 : 3200 + cinema * 400 + slot * 600;
                    boolean soldOut = day == 0 && cinema == 0 && slot == 0;
                    if (!existingShowtimes.contains(showtimeId)) {
                        showtimeRows.add(new Object[]{showtimeId, movieIds[movieIndex], hallId,
                                Timestamp.valueOf(startUtc), Timestamp.valueOf(endUtc), price, "国语", formatForHall(hallId),
                                soldOut ? "SOLD_OUT" : "ON_SALE"});
                    }
                    for (SeatSeed seat : seatsByHall.getOrDefault(hallId, List.of())) {
                        String inventoryId = showtimeId + "_" + seat.id();
                        if (existingInventory.contains(inventoryId)) continue;
                        String status = "UNAVAILABLE".equals(seat.type()) ? "UNAVAILABLE" : soldOut ? "SOLD" : "AVAILABLE";
                        inventoryRows.add(new Object[]{inventoryId, showtimeId, seat.id(), price, status});
                    }
                }
            }
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO showtime(id,movie_id,hall_id,start_at,end_at,base_price,language,format,status,version,created_at,updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """, showtimeRows);
        jdbcTemplate.batchUpdate("""
                INSERT INTO showtime_seat(id,showtime_id,seat_id,price,status,version,updated_at)
                VALUES (?,?,?,?,?,0,CURRENT_TIMESTAMP)
                """, inventoryRows);
    }

    private Map<String, List<SeatSeed>> loadSeatsByHall() {
        Map<String, List<SeatSeed>> result = new HashMap<>();
        jdbcTemplate.query("SELECT id,hall_id,seat_type FROM seat", rs -> {
            result.computeIfAbsent(rs.getString("hall_id"), ignored -> new ArrayList<>())
                    .add(new SeatSeed(rs.getString("id"), rs.getString("seat_type")));
        });
        return result;
    }

    private Set<String> existingIds(String table) {
        return new HashSet<>(jdbcTemplate.queryForList("SELECT id FROM " + table, String.class));
    }

    private String formatForHall(String hallId) {
        return switch (hallId) {
            case "hall_3001" -> "IMAX 2D";
            case "hall_3002" -> "杜比 2D";
            case "hall_3003" -> "激光 2D";
            case "hall_3005" -> "巨幕 2D";
            default -> "国语 2D";
        };
    }

    private Object[] prepend(Object first, Object[] rest) {
        Object[] result = new Object[rest.length + 1];
        result[0] = first;
        System.arraycopy(rest, 0, result, 1, rest.length);
        return result;
    }

    private Object[] append(Object[] values, Object last) {
        Object[] result = Arrays.copyOf(values, values.length + 1);
        result[values.length] = last;
        return result;
    }

    private record MovieSeed(String id, String name, String englishName, String genres, int duration,
                             String rating, LocalDate releaseDate, String synopsis, String cast,
                             long wantCount, String status) { }
    private record CinemaSeed(String id, String name, String brand, String district, String address,
                              String latitude, String longitude, String tags) { }
    private record HallSeed(String id, String cinemaId, String name, String type) { }
    private record SeatSeed(String id, String type) { }
}
