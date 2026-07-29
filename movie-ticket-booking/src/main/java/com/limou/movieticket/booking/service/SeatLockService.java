package com.limou.movieticket.booking.service;
import com.limou.movieticket.booking.api.BookingContract;
public interface SeatLockService {
    BookingContract.OrderView lockAndCreateOrder(BookingContract.LockSeatsCommand command);
    BookingContract.OrderView cancelPendingOrder(String orderId, String userId);
    int releaseExpiredOrders();
}
