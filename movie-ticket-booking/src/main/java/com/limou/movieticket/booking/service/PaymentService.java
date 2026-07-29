package com.limou.movieticket.booking.service;
import com.limou.movieticket.booking.api.BookingContract;
public interface PaymentService { BookingContract.PaymentView simulate(BookingContract.SimulatePaymentCommand command); }
