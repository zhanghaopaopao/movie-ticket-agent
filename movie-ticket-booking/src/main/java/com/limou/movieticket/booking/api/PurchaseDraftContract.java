package com.limou.movieticket.booking.api;

import com.limou.movieticket.booking.domain.PurchaseDraftStatus;
import com.limou.movieticket.booking.domain.SourceMode;
import java.time.OffsetDateTime;
import java.util.List;

public final class PurchaseDraftContract {
    private PurchaseDraftContract() { }
    public record CreateCommand(String userId, SourceMode sourceMode) { }
    public record PatchCommand(String userId, String movieId, String cinemaId, TimeRange dateTime,
                               String showtimeId, Integer ticketCount, Budget budget, List<String> seatIds,
                               SourceMode sourceMode, int expectedVersion) { }
    public record View(String draftId, String movieId, String cinemaId, TimeRange dateTime,
                       String showtimeId, int ticketCount, Budget budget, List<String> seatIds,
                       SourceMode sourceMode, PurchaseDraftStatus status, int version, String orderId) { }
    public record TimeRange(OffsetDateTime start, OffsetDateTime end) { }
    public record Budget(BudgetType type, int amount, String currency) { }
    public enum BudgetType { PER_TICKET, TOTAL }
}
