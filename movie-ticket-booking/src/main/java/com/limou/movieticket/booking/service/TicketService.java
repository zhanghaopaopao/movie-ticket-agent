package com.limou.movieticket.booking.service;
import com.limou.movieticket.booking.domain.Ticket;
import java.util.List;
public interface TicketService {
    List<Ticket> findByOrderForUser(String orderId, String userId);
    List<Ticket> issueForPaidOrder(String orderId);
}
