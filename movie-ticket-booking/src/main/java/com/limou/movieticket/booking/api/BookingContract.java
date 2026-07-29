package com.limou.movieticket.booking.api;

import com.limou.movieticket.booking.domain.OrderStatus;
import com.limou.movieticket.booking.domain.PaymentStatus;
import java.time.OffsetDateTime;
import java.util.List;

public final class BookingContract {
    private BookingContract() { }
    public record LockSeatsCommand(String userId, String draftId, String showtimeId,
                                   List<String> seatIds, String idempotencyKey) { }
    public record OrderView(String id, String orderNo, String showtimeId, List<OrderItemView> items,
                            int amount, OrderStatus status, OffsetDateTime expiresAt, int version) { }
    public record OrderItemView(String id, String seatId, int unitPrice) { }
    public record SimulatePaymentCommand(String userId, String orderId, String idempotencyKey,
                                         SimulationResult result) { }
    public record PaymentView(String id, String orderId, PaymentStatus status, int amount,
                              OffsetDateTime processedAt) { }
    public enum SimulationResult { SUCCESS, FAILURE }
}
