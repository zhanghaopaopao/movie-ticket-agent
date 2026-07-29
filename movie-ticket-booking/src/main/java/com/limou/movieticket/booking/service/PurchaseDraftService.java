package com.limou.movieticket.booking.service;
import com.limou.movieticket.booking.api.PurchaseDraftContract;
public interface PurchaseDraftService {
    PurchaseDraftContract.View create(PurchaseDraftContract.CreateCommand command);
    PurchaseDraftContract.View getForUser(String draftId, String userId);
    PurchaseDraftContract.View patch(String draftId, PurchaseDraftContract.PatchCommand command);
}
