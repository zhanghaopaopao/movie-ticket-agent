package com.limou.movieticket.ticketing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.movieticket.common.api.ErrorCode;
import com.limou.movieticket.common.api.PageResult;
import com.limou.movieticket.common.exception.BusinessException;
import com.limou.movieticket.ticketing.api.CinemaSummary;
import com.limou.movieticket.ticketing.api.MovieDetail;
import com.limou.movieticket.ticketing.api.MovieSummary;
import com.limou.movieticket.ticketing.api.SeatMapResponse;
import com.limou.movieticket.ticketing.api.ShowtimeSummary;
import com.limou.movieticket.ticketing.domain.*;
import com.limou.movieticket.ticketing.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TicketingQueryService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final MovieMapper movieMapper;
    private final CinemaMapper cinemaMapper;
    private final HallMapper hallMapper;
    private final SeatMapper seatMapper;
    private final ShowtimeMapper showtimeMapper;
    private final ShowtimeSeatMapper showtimeSeatMapper;

    public TicketingQueryService(MovieMapper movieMapper, CinemaMapper cinemaMapper, HallMapper hallMapper,
                                 SeatMapper seatMapper, ShowtimeMapper showtimeMapper,
                                 ShowtimeSeatMapper showtimeSeatMapper) {
        this.movieMapper = movieMapper;
        this.cinemaMapper = cinemaMapper;
        this.hallMapper = hallMapper;
        this.seatMapper = seatMapper;
        this.showtimeMapper = showtimeMapper;
        this.showtimeSeatMapper = showtimeSeatMapper;
    }

    public PageResult<MovieSummary> findMovies(MovieStatus status, String genre, String keyword, long page, long pageSize) {
        var query = Wrappers.<Movie>lambdaQuery()
                .eq(status != null, Movie::getStatus, status)
                .like(StringUtils.hasText(genre), Movie::getGenres, genre == null ? null : genre.trim())
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(Movie::getName, keyword == null ? null : keyword.trim())
                        .or().like(Movie::getEnglishName, keyword == null ? null : keyword.trim()))
                .orderByDesc(Movie::getRating)
                .orderByAsc(Movie::getReleaseDate);
        IPage<Movie> result = movieMapper.selectPage(new Page<>(page, pageSize), query);
        return new PageResult<>(result.getRecords().stream().map(this::toMovieSummary).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public MovieDetail getMovie(String id) {
        Movie movie = require(movieMapper.selectById(id), "Movie", id);
        Coverage coverage = coverage(movie.getId());
        return new MovieDetail(movie.getId(), movie.getName(), movie.getEnglishName(), split(movie.getGenres()),
                movie.getDurationMinutes(), movie.getRating(), movie.getPosterUrl(), movie.getReleaseDate(),
                movie.getSynopsis(), split(movie.getCastNames()), valueOrZero(movie.getWantCount()), movie.getStatus(),
                coverage.cinemaCount(), coverage.showtimeCount());
    }

    public PageResult<CinemaSummary> findCinemas(String city, String district, String brand, String hallType,
                                                 String keyword, BigDecimal latitude, BigDecimal longitude,
                                                 long page, long pageSize) {
        if ((latitude == null) != (longitude == null)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "latitude and longitude must be provided together");
        }
        List<Cinema> cinemas = cinemaMapper.selectList(Wrappers.<Cinema>lambdaQuery()
                .eq(Cinema::getStatus, ResourceStatus.ACTIVE)
                .eq(StringUtils.hasText(city), Cinema::getCity, city)
                .eq(StringUtils.hasText(district), Cinema::getDistrict, district)
                .eq(StringUtils.hasText(brand), Cinema::getBrand, brand)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(Cinema::getName, keyword == null ? null : keyword.trim())
                        .or().like(Cinema::getAddress, keyword == null ? null : keyword.trim())));

        Map<String, List<Hall>> hallsByCinema = hallMapper.selectList(Wrappers.<Hall>lambdaQuery()
                        .eq(Hall::getStatus, ResourceStatus.ACTIVE))
                .stream().collect(Collectors.groupingBy(Hall::getCinemaId));
        if (StringUtils.hasText(hallType)) {
            cinemas = cinemas.stream().filter(cinema -> hallsByCinema.getOrDefault(cinema.getId(), List.of())
                    .stream().anyMatch(hall -> hall.getHallType().equalsIgnoreCase(hallType))).toList();
        }

        List<CinemaSummary> summaries = cinemas.stream()
                .map(cinema -> toCinemaSummary(cinema, hallsByCinema.getOrDefault(cinema.getId(), List.of()), latitude, longitude))
                .sorted(latitude != null && longitude != null
                        ? Comparator.comparing(CinemaSummary::distanceKm, Comparator.nullsLast(Double::compareTo))
                        : Comparator.comparing(CinemaSummary::name))
                .toList();
        int from = (int) Math.min((page - 1) * pageSize, summaries.size());
        int to = (int) Math.min(from + pageSize, summaries.size());
        return new PageResult<>(summaries.subList(from, to), summaries.size(), page, pageSize);
    }

    public List<ShowtimeSummary> findShowtimes(String cinemaId, String movieId, LocalDate date) {
        Cinema cinema = require(cinemaMapper.selectById(cinemaId), "Cinema", cinemaId);
        if (cinema.getStatus() != ResourceStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Cinema is not active");
        }
        LocalDate queryDate = date == null ? LocalDate.now(BUSINESS_ZONE) : date;
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (queryDate.isBefore(today) || queryDate.isAfter(today.plusDays(6))) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "date must be within the next 7 days");
        }
        List<Hall> halls = hallMapper.selectList(Wrappers.<Hall>lambdaQuery()
                .eq(Hall::getCinemaId, cinemaId).eq(Hall::getStatus, ResourceStatus.ACTIVE));
        if (halls.isEmpty()) {
            return List.of();
        }
        Map<String, Hall> hallById = halls.stream().collect(Collectors.toMap(Hall::getId, Function.identity()));
        LocalDateTime startUtc = utcBoundary(queryDate);
        LocalDateTime endUtc = utcBoundary(queryDate.plusDays(1));
        List<Showtime> showtimes = showtimeMapper.selectList(Wrappers.<Showtime>lambdaQuery()
                .in(Showtime::getHallId, hallById.keySet())
                .eq(StringUtils.hasText(movieId), Showtime::getMovieId, movieId)
                .ge(Showtime::getStartAt, startUtc).lt(Showtime::getStartAt, endUtc)
                .ne(Showtime::getStatus, ShowtimeStatus.STOPPED)
                .orderByAsc(Showtime::getStartAt));
        Set<String> movieIds = showtimes.stream().map(Showtime::getMovieId).collect(Collectors.toSet());
        Map<String, Movie> movieById = movieIds.isEmpty() ? Map.of() : movieMapper.selectBatchIds(movieIds).stream()
                .collect(Collectors.toMap(Movie::getId, Function.identity()));
        return showtimes.stream().map(showtime -> toShowtimeSummary(showtime, hallById.get(showtime.getHallId()),
                movieById.get(showtime.getMovieId()))).toList();
    }

    public SeatMapResponse getSeatMap(String showtimeId) {
        Showtime showtime = require(showtimeMapper.selectById(showtimeId), "Showtime", showtimeId);
        Hall hall = require(hallMapper.selectById(showtime.getHallId()), "Hall", showtime.getHallId());
        Cinema cinema = require(cinemaMapper.selectById(hall.getCinemaId()), "Cinema", hall.getCinemaId());
        Movie movie = require(movieMapper.selectById(showtime.getMovieId()), "Movie", showtime.getMovieId());
        List<ShowtimeSeat> inventory = showtimeSeatMapper.selectList(Wrappers.<ShowtimeSeat>lambdaQuery()
                .eq(ShowtimeSeat::getShowtimeId, showtimeId));
        Map<String, Seat> seatsById = inventory.isEmpty() ? Map.of() : seatMapper.selectBatchIds(
                inventory.stream().map(ShowtimeSeat::getSeatId).toList()).stream()
                .collect(Collectors.toMap(Seat::getId, Function.identity()));
        List<SeatMapResponse.SeatView> seats = inventory.stream()
                .map(item -> toSeatView(item, seatsById.get(item.getSeatId())))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(SeatMapResponse.SeatView::rowNo)
                        .thenComparingInt(SeatMapResponse.SeatView::seatNo))
                .toList();
        int inventoryVersion = inventory.stream().map(ShowtimeSeat::getVersion).filter(Objects::nonNull)
                .max(Integer::compareTo).orElse(0);
        return new SeatMapResponse(showtime.getId(), movie.getId(), movie.getName(), cinema.getId(), cinema.getName(),
                hall.getId(), hall.getName(), hall.getHallType(), toBusinessTime(showtime.getStartAt()),
                inventoryVersion, seats);
    }

    private MovieSummary toMovieSummary(Movie movie) {
        Coverage coverage = coverage(movie.getId());
        return new MovieSummary(movie.getId(), movie.getName(), movie.getEnglishName(), split(movie.getGenres()),
                movie.getDurationMinutes(), movie.getRating(), movie.getPosterUrl(), movie.getReleaseDate(),
                split(movie.getCastNames()), valueOrZero(movie.getWantCount()), movie.getStatus(),
                coverage.cinemaCount(), coverage.showtimeCount());
    }

    private Coverage coverage(String movieId) {
        List<Showtime> showtimes = showtimeMapper.selectList(Wrappers.<Showtime>lambdaQuery()
                .eq(Showtime::getMovieId, movieId)
                .ge(Showtime::getStartAt, LocalDateTime.now(ZoneOffset.UTC))
                .ne(Showtime::getStatus, ShowtimeStatus.STOPPED));
        if (showtimes.isEmpty()) return new Coverage(0, 0);
        Set<String> hallIds = showtimes.stream().map(Showtime::getHallId).collect(Collectors.toSet());
        long cinemaCount = hallMapper.selectBatchIds(hallIds).stream().map(Hall::getCinemaId).distinct().count();
        return new Coverage(cinemaCount, showtimes.size());
    }

    private CinemaSummary toCinemaSummary(Cinema cinema, List<Hall> halls, BigDecimal latitude, BigDecimal longitude) {
        List<String> hallTypes = halls.stream().map(Hall::getHallType).distinct().sorted().toList();
        Integer minimumPrice = null;
        if (!halls.isEmpty()) {
            List<Showtime> showtimes = showtimeMapper.selectList(Wrappers.<Showtime>lambdaQuery()
                    .in(Showtime::getHallId, halls.stream().map(Hall::getId).toList())
                    .ge(Showtime::getStartAt, LocalDateTime.now(ZoneOffset.UTC))
                    .eq(Showtime::getStatus, ShowtimeStatus.ON_SALE));
            minimumPrice = showtimes.stream().map(Showtime::getBasePrice).min(Integer::compareTo).orElse(null);
        }
        Double distance = latitude == null || longitude == null ? null
                : distanceKm(latitude.doubleValue(), longitude.doubleValue(), cinema.getLatitude().doubleValue(), cinema.getLongitude().doubleValue());
        return new CinemaSummary(cinema.getId(), cinema.getName(), cinema.getBrand(), cinema.getCity(), cinema.getDistrict(),
                cinema.getAddress(), cinema.getLatitude(), cinema.getLongitude(), split(cinema.getServiceTags()), hallTypes,
                minimumPrice, distance);
    }

    private ShowtimeSummary toShowtimeSummary(Showtime showtime, Hall hall, Movie movie) {
        long remaining = showtimeSeatMapper.selectCount(Wrappers.<ShowtimeSeat>lambdaQuery()
                .eq(ShowtimeSeat::getShowtimeId, showtime.getId())
                .eq(ShowtimeSeat::getStatus, SeatInventoryStatus.AVAILABLE));
        return new ShowtimeSummary(showtime.getId(), showtime.getMovieId(), movie == null ? null : movie.getName(),
                hall.getId(), hall.getName(), hall.getHallType(), toBusinessTime(showtime.getStartAt()),
                toBusinessTime(showtime.getEndAt()), showtime.getBasePrice(), showtime.getLanguage(),
                showtime.getFormat(), remaining, showtime.getStatus());
    }

    private SeatMapResponse.SeatView toSeatView(ShowtimeSeat inventory, Seat seat) {
        if (seat == null) return null;
        return new SeatMapResponse.SeatView(seat.getId(), seat.getRowNo(), seat.getSeatNo(), seat.getZone(),
                seat.getSeatType(), seat.getCoupleGroup(), inventory.getPrice(), inventory.getStatus(), inventory.getVersion());
    }

    private <T> T require(T value, String type, String id) {
        if (value == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, type + " " + id + " was not found");
        return value;
    }

    private List<String> split(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(StringUtils::hasText).toList();
    }

    private long valueOrZero(Long value) { return value == null ? 0 : value; }

    private LocalDateTime utcBoundary(LocalDate date) {
        return LocalDateTime.ofInstant(date.atStartOfDay(BUSINESS_ZONE).toInstant(), ZoneOffset.UTC);
    }

    private OffsetDateTime toBusinessTime(LocalDateTime utc) {
        return utc.toInstant(ZoneOffset.UTC).atZone(BUSINESS_ZONE).toOffsetDateTime();
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double value = 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private record Coverage(long cinemaCount, long showtimeCount) { }
}
